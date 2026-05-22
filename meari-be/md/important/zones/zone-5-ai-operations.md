# Zone 5 — AI 서버 운영성 (asyncio · 모델 lifecycle · 헬스체크 · 크로스플랫폼) 실행 계획

> 마스터 플랜의 Z5 상세본. AI 서버(meari-ai, FastAPI)가 운영 환경에서 실제로 견디는가를 검증.

## 목표

다음 6개 명제를 면접에서 1인칭으로 답할 수 있게 만들기:

1. **Wav2Vec2 콜드 스타트** — 첫 분석 요청이 1초+, 후속은 100ms대. 워밍업 inference 도입으로 콜드/웜 격차 X→Y로 단축
2. **헬스체크가 거짓말한다** — `/health`가 항상 200을 반환하는 하드코딩 → 모델 로드 실패 후에도 "healthy" → LB가 부적격 인스턴스로 트래픽 라우팅 → 의존성 실측 헬스체크 도입
3. **Windows 절대경로** — `/tmp/reference_audio_cache` 가정 → Windows에서는 cache_dir 생성 실패. `pathlib + tempfile.gettempdir()`로 크로스플랫폼화
4. **S3 부분 실패 무시** — 10개 문장 중 1개 다운로드 실패 → broad except + 로그만 → 사용자는 그 문장 0점 받음. exponential backoff retry 도입
5. **event loop 낭비** — 매 분석마다 `asyncio.run()`으로 새 loop 생성/파괴 → CPU 오버헤드 측정 가능. global loop 또는 thread-local 패턴 도입
6. **Pydantic 검증 빈약** — `start_time` 음수 허용, `audio_url` URL 형식 검증 없음 → 잘못된 요청이 깊은 곳까지 흘러들어가 의문의 에러로 표출. `Field(ge=0)`, `HttpUrl`, `min_length` 명시화

## 현재 코드 상태 (Pre-Z5 베이스라인)

### 핵심 위치

| 파일 | 라인 | 책임 | 위험 |
|---|---|---|---|
| `main.py` | 253~260 | `/health` 엔드포인트 | 하드코딩 `{"status":"healthy", "rabbitmq":"connected", "models":"loaded"}` |
| `main.py` | 277~283 | HTTP `/analyze` 핸들러 — `loop.run_in_executor`로 동기 메서드 호출 | 매 요청마다 thread pool 스레드 점유 |
| `analysis_service.py` | 63 | `analyze_member` 안에서 `asyncio.run(...)` | 매 호출마다 new event loop 생성/파괴 |
| `analysis_service.py` | 226~252 | `asyncio.gather(..., return_exceptions=True)` | 부분 실패 시 인덱스 매핑 위험 (Z4와 일부 중첩) |
| `analysis_service.py` | 300~313 | `download_audio_from_s3_async` — broad except | 부분 실패 재시도 없음, 로그만 |
| `model_loader.py` | 25~37 | Wav2Vec2 startup_event eager load | 워밍업 inference 부재 |
| `reference_audio_manager.py` | 21, 39~64 | `cache_dir = "/tmp/reference_audio_cache"` | UNIX 절대경로, Windows fail |
| `schemas/request.py` | 8~25 | `SentenceAnalysisInfo` | `start_time`/`end_time` 음수 허용, `audio_url` URL 검증 없음, `text_ko` 빈 문자열 가능 |
| `rabbitmq_consumer.py` | 78, 88 | pydantic validation | extra field 처리 미정 (Z3과 결합) |

### 코드 단편 — 헬스체크 하드코딩 (`main.py:253~260`)
```python
@app.get("/health")
async def health_check():
    """상세 헬스체크"""
    return {
        "status": "healthy",
        "rabbitmq": "connected",
        "models": "loaded"
    }
```
실제 의존성 상태와 무관하게 200 반환.

### 코드 단편 — `asyncio.run` per-request (`analysis_service.py:63`)
```python
user_audios, ref_audios = asyncio.run(
    self.download_all_audios_parallel(message.sentences)
)
```
`analyze_member`가 동기 메서드라 매번 새 loop. RabbitMQ Consumer 스레드 + HTTP `/analyze` thread pool 양쪽에서 호출됨.

---

## Pre-flight 체크리스트

- [ ] AI 서버를 직접 컨트롤 가능 (Docker 또는 `python -m app.main`)
- [ ] 분석 요청을 BE 거치지 않고도 발사 가능: `POST /analyze` (HTTP 직접 호출 — Postman)
- [ ] AI 컨테이너 로그 실시간 확인 (`docker logs -f meari-ai`)
- [ ] 측정 도구: `time` 명령 또는 Postman의 응답 시간, 또는 `curl -w "%{time_total}\n"`
- [ ] (SC4용) Windows 환경에서 직접 `python -m app.main` 실행 가능
- [ ] (SC5용) S3 임시로 다운 시키거나 잘못된 URL 주입할 수 있는 방법
- [ ] (SC6용) `htop` 또는 `docker stats`로 CPU 관찰 가능

---

## 시나리오 — 직접 수행 순서

각 시나리오 결과를 `meari-be/md/_ai_operations_ts_raw.md`에 누적.

### SC1. 베이스라인 — 정상 분석 1회
- [ ] Hypothesis: 정상 환경에서 1문장 분석이 N초 안에 끝남
- Setup: AI 서버 운영 모드 시작, S3 정상
- Execute:
  1. 짧은 음성(2~3초) 1문장으로 `/analyze` HTTP 호출
  2. 응답 시간 측정
  3. 같은 요청 5번 반복 — 평균 시간
- Observe: 정상 latency 분포
- Conclude: 베이스라인 확보

### SC2. Wav2Vec2 콜드 스타트 측정
- [ ] Hypothesis: 첫 요청은 모델 워밍업 때문에 1000ms+ 걸리지만 후속은 100ms대
- Setup: AI 서버 방금 재기동
- Execute:
  1. 재기동 직후 첫 요청 응답 시간 측정 (T_first)
  2. 곧바로 100번 더 요청 — 각 응답 시간 측정 (T_2 ~ T_101)
  3. 분포: T_first vs 평균(T_2..T_101)
- Observe: latency 분포 그래프
- Conclude: 콜드 스타트 격차 정량화 → 워밍업의 가치 증명

### SC3. 헬스체크 거짓 양성
- [ ] Hypothesis: 모델 로드를 의도적으로 실패시켜도 `/health`는 200을 반환
- Setup: 정상 AI 서버
- Execute:
  1. 정상 시 `/health` 호출 → 200 + `{"models":"loaded"}` 확인
  2. 모델 파일을 일시 이동 또는 망가뜨림 (`mv app/models/wav2vec2 /tmp/`)
  3. AI 재기동 — startup에서 모델 로드 실패해야 함
  4. AI 로그에서 모델 로드 에러 확인
  5. `/health` 다시 호출 → 응답 확인 (현재는 여전히 200 + "loaded")
  6. 실제 `/analyze` 요청 → 500 에러 ("모델이 로드되지 않았습니다" 류)
- Observe: 헬스체크 응답 vs 실제 처리 가능성 불일치
- Conclude: LB가 이 인스턴스로 트래픽 보내는 위험 명확화. 모델 파일 원복 후 정상화

### SC4. Windows 절대경로 호환성
- [ ] Hypothesis: Windows에서 AI 서버 시작하면 `/tmp/reference_audio_cache` 생성 시도가 실패하거나 의도와 다른 경로에 만들어짐
- Setup: Windows 환경 (이미 사용자 환경)
- Execute:
  1. 컨테이너 아닌 직접 실행 — `python -m app.main` 또는 `uvicorn app.main:app`
  2. `reference_audio_manager.py:21`의 `cache_dir = "/tmp/reference_audio_cache"` 경로가 어디에 만들어지는지 확인
     - PowerShell `dir C:\tmp\reference_audio_cache` 또는 시스템 root에서 검색
     - Git Bash 환경이면 `/tmp/`가 다른 경로로 매핑될 수도
  3. 분석 요청 → 캐시 동작 정상인지 또는 mkdir permission denied
- Observe: Windows에서 동작 여부, 어디에 캐시되는지
- Conclude: 크로스플랫폼 호환성 결함 직접 확인

### SC5. S3 다운로드 부분 실패
- [ ] Hypothesis: 10개 문장 중 5번째만 다운로드 실패해도 나머지는 정상 처리되지만, 5번째는 0점 처리되고 사용자에게 별도 알림 없음
- Setup: 정상 AI 서버 + 정상 S3
- Execute:
  1. 10문장 분석 요청을 만들되, 5번째 문장의 `audio_url`을 의도적으로 잘못된 값으로 주입 (BE 수정 또는 Postman 직접 호출)
  2. AI 로그: download 실패 위치 확인
  3. 분석 결과 응답 — 5번째 문장 score 확인 (0점일 것)
  4. 사용자(또는 BE)가 이 부분 실패를 알 수 있는 신호가 응답에 있는지
- Observe: 부분 실패 처리 흐름
- Conclude: 재시도 부재의 영향 — 일시 네트워크 단절도 한 문장 0점

### SC6. Per-request event loop 오버헤드
- [ ] Hypothesis: 매 분석마다 새 event loop를 만드는 비용이 있음. 동시 100 요청 시 CPU 오버헤드 또는 메모리 증가 측정 가능
- Setup: 정상 AI 서버
- Execute:
  1. 베이스라인: idle 상태 `docker stats meari-ai` CPU/Memory 기록
  2. 10 요청 직렬 발사 → CPU 추이
  3. 100 요청 동시 발사 (Postman runner 또는 짧은 스크립트) → CPU/메모리 추이
  4. 같은 분석을 1번만 돌리는 경우 vs 10번 돌리는 경우의 1회당 평균 latency 비교
- Observe: 처리량 / CPU 곡선
- Conclude: per-request loop 비용을 정량화. 글로벌 loop로 바꾸면 X% 개선될 것이라는 근거

### SC7. Pydantic 검증 미흡
- [ ] Hypothesis: `start_time = -1`, `audio_url = "not-a-url"`, `text_ko = ""` 같은 잘못된 값이 그대로 들어와 깊은 곳에서 의문의 에러로 표출
- Setup: 정상 AI 서버
- Execute:
  1. Postman으로 `/analyze`에 다음 메시지 발사:
     ```json
     {
       "room_id": 1, "round": 1, "member_id": 1,
       "sentences": [{
         "sentence_id": 1, "text_ko": "",
         "start_time": -1, "end_time": -2,
         "audio_url": "not-a-url"
       }]
     }
     ```
  2. AI가 어디서 깨지는지 stack trace 확인
  3. 빈 문자열, 음수, 잘못된 URL 각각 분리해서 시도
- Observe: 어느 시점에 어떤 에러가 표출되는지
- Conclude: 입력 검증 부재의 비용 — 깊은 레이어에서의 에러는 디버깅이 더 어려움

---

## 발견 후 수정 계획 (PR 분할)

### PR1: Wav2Vec2 워밍업
- `app/main.py`의 `startup_event`에서 모델 로드 직후 dummy 입력으로 1회 inference:
  ```python
  @app.on_event("startup")
  async def startup_event():
      load_models()
      # 워밍업 inference
      dummy = torch.zeros(1, 16000)  # 1초 무음
      with torch.no_grad():
          model(dummy)
      logger.info("Model warmup 완료")
  ```
- 검증: SC2 재현 시 첫 요청 latency가 후속과 비슷

### PR2: 의존성 실측 헬스체크
- 새 함수 `_check_dependencies()` 작성:
  ```python
  @app.get("/health")
  async def health_check():
      checks = {
          "rabbitmq": _check_rabbitmq(),
          "models": _check_models(),
          "s3": _check_s3()
      }
      all_ok = all(checks.values())
      status_code = 200 if all_ok else 503
      return JSONResponse(
          status_code=status_code,
          content={"status": "healthy" if all_ok else "degraded", **checks}
      )
  ```
- `/ready`(준비됨)와 `/health`(살아있음) 분리 권장 — Kubernetes liveness/readiness probe 패턴
- 검증: SC3 재현 시 503 + 정확한 실패 사유 반환

### PR3: 크로스플랫폼 경로
- `reference_audio_manager.py`:
  ```python
  from pathlib import Path
  import tempfile
  
  cache_dir = Path(tempfile.gettempdir()) / "meari_reference_audio_cache"
  cache_dir.mkdir(parents=True, exist_ok=True)
  ```
- 또는 환경변수로 외부화: `os.environ.get("MEARI_CACHE_DIR", default)`
- 검증: SC4 재현 시 Windows에서 정상 동작

### PR4: S3 부분 실패 재시도
- `download_audio_from_s3_async`에 exponential backoff:
  ```python
  for attempt in range(3):
      try:
          return await _download(...)
      except (ClientError, ConnectionError) as e:
          if attempt == 2: raise
          await asyncio.sleep(2 ** attempt)  # 1, 2, 4초
  ```
- 또는 `tenacity` 라이브러리 사용:
  ```python
  @retry(stop=stop_after_attempt(3), wait=wait_exponential(min=1, max=10))
  async def download_audio(...):
  ```
- 부분 실패는 응답에 명시적으로 표시 — `sentence_results[idx].error = "DOWNLOAD_FAILED"` 등
- 검증: SC5 재현 시 일시 실패가 자동 회복

### PR5: Event loop 재사용
- 옵션 A: 글로벌 loop — `loop = asyncio.get_event_loop()` 한 번만 만들고 재사용
- 옵션 B: 동기 메서드 자체를 async로 전환하고 FastAPI의 main loop 사용 — 큰 리팩터
- 옵션 C: thread-local loop — Heisenbug 패턴 따라
- 권장: **B가 정석**이지만 시간 들음. 단기는 A 또는 C
- 검증: SC6 재현 시 CPU 사용률 감소

### PR6: Pydantic 강화
- `schemas/request.py`:
  ```python
  from pydantic import BaseModel, Field, HttpUrl
  
  class SentenceAnalysisInfo(BaseModel):
      sentence_id: int = Field(gt=0)
      text_ko: str = Field(min_length=1, max_length=500)
      start_time: float = Field(ge=0)
      end_time: float = Field(gt=0)
      audio_url: HttpUrl  # URL 형식 검증
      
      class Config:
          extra = "forbid"  # Z3과 결합
  ```
- ValidationError → HTTP 422 (FastAPI 자동) + RabbitMQ Consumer는 nack(requeue=False) + DLQ (Z3 PR1)
- 검증: SC7 재현 시 즉시 422 + 구체적 에러 메시지

### PR7: AI Pydantic + DLQ 결합 (Z3 의존)
- Z3 PR5(extra='forbid' + DLQ)와 통합. Z3 실행 시 함께 진행

---

## 측정 지표 표 (Before/After 채우기)

| 지표 | Before | After (목표) |
|---|---|---|
| 첫 요청 latency (SC2) | (측정) ms | 후속과 ±10% 이내 |
| `/health` 정확도 (SC3) | 거짓 양성 100% | 100% 정확 |
| Windows 실행 가능 (SC4) | (Yes/No) | Yes |
| S3 일시 실패 회복률 (SC5) | 0% | 95%+ |
| 100 동시 요청 시 CPU 평균 (SC6) | (측정) % | -X% |
| 잘못된 입력의 422 즉시 응답율 (SC7) | 0% | 100% |

---

## 정리 산출물

- `meari-be/md/_ai_operations_ts_raw.md` — 시나리오별 raw 노트
- `meari-be/md/portfolio_features.md` 신규 섹션:
  - "AI 서버 운영성 — 콜드 스타트, 헬스체크 거짓 양성, 크로스플랫폼"
- `portfolio_features.md` #2 (Wav2Vec2 파이프라인)에 워밍업 단락 추가

---

## 진행 체크리스트

- [ ] Pre-flight 환경 준비
- [ ] SC1 베이스라인
- [ ] SC2 콜드 스타트 측정
- [ ] SC3 헬스체크 거짓 양성
- [ ] SC4 Windows 호환
- [ ] SC5 S3 부분 실패
- [ ] SC6 event loop 오버헤드
- [ ] SC7 Pydantic 검증 미흡
- [ ] PR1 워밍업
- [ ] PR2 헬스체크 의존성 실측
- [ ] PR3 크로스플랫폼 경로
- [ ] PR4 S3 재시도
- [ ] PR5 event loop 재사용
- [ ] PR6 Pydantic 강화
- [ ] 측정 지표 표 채우기
- [ ] portfolio_features.md 갱신

---

**작성일**: 2026-04-29
**다음 액션**: Pre-flight + AI 서버 재기동 → SC1 베이스라인
