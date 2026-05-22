# Zone 3 — 분석 파이프라인 (BE↔AI 통합) 실행 계획

> 마스터 플랜의 Z3 상세본. 각 시나리오를 사용자가 직접 재현하고 결과를 raw 노트에 기록 → portfolio 갱신.

## 목표

**면접 1인칭 답변 가능 상태로 만들기.** 다음 7개 명제를 직접 본 적 있다고 답할 수 있어야 함:

1. AI 서버가 다운돼도 RabbitMQ가 메시지를 보존하고, 재기동 시 자동 처리됨 (큐의 가치)
2. 멱등성 미보장 → 같은 메시지 2회 처리 시 DB 중복 행 발생 → UNIQUE + UPSERT로 해결
3. Publisher Confirm 콜백 미구현 시 routing 실패가 silently drop → 콜백 구현으로 발견 가능
4. BE Consumer auto-ack + DLQ 부재 → 처리 실패 메시지가 어떻게 사라지는지 → manual ack + DLQ로 격리
5. AI graceful shutdown 부재 → 처리 중 SIGTERM 시 메시지 ACK 안 되고 손실 → 비-daemon thread + signal trap
6. Pydantic 검증이 너무 관대해 typo 메시지가 영구 손실 → `extra='forbid'` + 명시 검증
7. JSON alias 호환성 — BE가 camelCase 기대할 때 AI가 snake_case 보내면 어떻게 깨지는가

## 현재 코드 상태 (Pre-Z3 베이스라인)

### RabbitMQ 토폴로지 (`RabbitMQConfig.java`)
| 항목 | 값 |
|---|---|
| Exchange | `analysis.exchange` (Direct, durable, auto-delete=false) |
| Request Queue | `analysis.requests` (durable=true) — BE → AI |
| Result Queue | `analysis.results` (durable=true) — AI → BE |
| Request Routing Key | `analysis.request` |
| Result Routing Key | `analysis.result` |
| DLX/DLQ | ❌ 없음 |
| TTL 상수 | 600,000ms (선언되지만 사용 안 함) |

### 신뢰성 메커니즘 현황 (`rabbitmq_study.md` 2.4 + 신규)
| # | 항목 | 상태 |
|---|---|---|
| 1 | Queue durable | ✅ |
| 2 | Message persistent (delivery_mode=2) | ✅ (Spring 기본 PERSISTENT, AI 명시) |
| 3 | Publisher Confirm 활성화 | ⚠️ yml에 `correlated` 설정됐지만 콜백 미구현 |
| 4 | Publisher Returns + mandatory | ⚠️ `publisher-returns: true`만, mandatory 미설정 |
| 5 | BE Consumer ack 모드 | ❌ AUTO (예외 시 retry 후 reject → drop, DLQ 없음) |
| 6 | AI Consumer ack 모드 | ✅ manual ack/nack (이미 구현) |
| 7 | DLQ | ❌ 양쪽 다 없음 |
| 8 | BE prefetch | ❌ 미설정 |
| 9 | AI prefetch | ✅ `prefetch_count=1` |
| 10 | 멱등성 | ❌ `messageId` 미사용, ShadowingReport 비멱등 |
| 11 | BE retry 정책 | ⚠️ yml `max-attempts: 3, multiplier: 2` (DLQ 없이 단독) |
| 12 | 직렬화 BE→AI | ✅ 동작 (Jackson `__TypeId__` AI에서 무시) |
| 13 | 직렬화 AI→BE | ⚠️ fragile (시그니처 추론 의존) |
| 14 | AI Producer 재연결 | ✅ Heisenbug 대응 시 추가 |
| 15 | AI Consumer 재연결 | ❌ start_consuming drop 시 재연결 없음 |
| 16 | 헬스체크 정확성 | ❌ AI `/health` 항상 healthy 하드코딩 |

### 신규 발견 (3개 Explore 에이전트, 47개 중 Z3 관련)
- BE `AnalysisConsumer.java:29~55` — `@RabbitListener`에 `@Transactional` 없음
- BE `HttpAnalysisService.java:34~79` — `@Async` 트랜잭션 컨텍스트 손실
- BE `HttpAnalysisService.java:77` — 재시도 로직 TODO
- AI `rabbitmq_consumer.py:78,88` — pydantic `extra` 처리 미정 → typo 메시지 영구 손실
- AI `analysis_service.py:165-184` — JSON 직렬화 alias 호환성
- AI `main.py:226-240` — graceful shutdown 부재 (consumer thread daemon=True)
- AI `schemas/request.py:8-25` — pydantic 검증 미흡 (start_time 음수 허용 등)

---

## Pre-flight 체크리스트 (시작 전 환경 준비)

- [ ] 로컬 `docker-compose up -d` 또는 배포 환경 접근 가능
- [ ] RabbitMQ Management UI 접속 가능 (`http://localhost:15672`, ssafy/ssafy)
- [ ] PostgreSQL 직접 쿼리 도구 (DBeaver / DataGrip / `psql`)
- [ ] Redis 도구 (RedisInsight / `redis-cli`)
- [ ] BE 로그 실시간 확인 가능 (IDE 콘솔 또는 `docker logs -f spring-api`)
- [ ] AI 로그 실시간 확인 가능 (`docker logs -f meari-ai`)
- [ ] BE `application.yml`에 `analysis.mode: rabbitmq` 설정 (HTTP 모드라면 RabbitMQ 경로 안 탐)
- [ ] AI `.env`에 `ENABLE_RABBITMQ=true`
- [ ] FE 또는 Postman으로 분석 트리거 가능한 사용자 시나리오 한 개 확보 (가장 단순한 건 solo_practice — 1:1 연습)

---

## 시나리오 — 직접 수행 순서

각 시나리오마다 **Hypothesis → Setup → Execute → Observe → Conclude**. 결과는 `meari-be/md/_rabbitmq_ts_raw.md`의 TS-2~ 자리에 누적 (TS-1은 Heisenbug, 완료).

### SC1. 베이스라인 — 정상 분석 흐름 확인
- [ ] Hypothesis: 정상 환경에서 분석 1회 끝까지 성공 (큐 length는 처리 후 0)
- Setup: 모든 컨테이너 ready, ShadowingReport 테이블 비어있음 또는 알려진 상태
- Execute:
  1. solo_practice로 분석 1회 트리거 (가장 단순한 시나리오)
  2. RabbitMQ UI: `analysis.requests` 큐 length 추이 관찰 (1 → 0)
  3. AI 로그: 메시지 수신/처리 확인
  4. RabbitMQ UI: `analysis.results` 큐 length 추이
  5. BE 로그: result consume 확인
  6. DB: `shadowing_report` 신규 행 확인
- Observe: 각 단계 시간(ms 단위) 기록, 큐 length 그래프
- Conclude: 정상 경로 latency 베이스라인 확보

### SC2. AI 서버 다운 → 큐 잔존
- [ ] Hypothesis: AI 다운 중 발행한 메시지는 큐에 보존, AI 재기동 시 자동 처리
- Setup: SC1 끝난 상태
- Execute:
  1. `docker stop meari-ai` (또는 AI 프로세스 종료)
  2. solo_practice로 분석 트리거 5번
  3. RabbitMQ UI에서 `analysis.requests` 큐 length 확인 (5여야 함)
  4. 30초 대기
  5. `docker start meari-ai` (또는 AI 재기동)
  6. 큐 length가 0으로 감소하는 시간 측정
- Observe: 큐에 5개 잔존 시간, 자동 소비 시간
- Conclude: 큐의 내구성 직접 확인 — 면접 답변 가능

### SC3. 멱등성 부재 → DB 중복 행
- [ ] Hypothesis: 같은 분석 메시지를 2회 발행하면 `shadowing_report`에 중복 행 발생
- Setup: 특정 (room, round, member) 조합으로 1회 분석 완료한 상태
- Execute:
  1. RabbitMQ UI의 "Publish message" 기능으로 `analysis.results` 큐에 동일한 결과 메시지 1번 더 발행
     - 또는 BE에서 강제로 같은 결과를 재처리하게 만듦
  2. DB 조회: `SELECT * FROM shadowing_report WHERE room_id=X AND round=Y AND member_id=Z`
  3. 행 개수 확인 — 1개 vs 2개?
  4. 만약 2개면: 어느 함수가 insert vs update를 결정하는가 코드 추적
  5. 만약 NonUniqueResultException 같은 게 떠서 BE 깨지면 그 트레이스 캡처
- Observe: 중복 행 여부, 또는 예외 트레이스
- Conclude: 멱등성 미보장의 구체적 증거 확보

### SC4. Publisher Confirm 콜백 미구현 → silently drop
- [ ] Hypothesis: 라우팅 안 되는 routing key로 발행하면 BE는 정상 종료, 실제로는 메시지 사라짐
- Setup: 정상 환경
- Execute:
  1. 임시로 BE 어딘가에 `rabbitTemplate.convertAndSend("analysis.exchange", "analysis.NONEXISTENT", message);` 호출 추가
  2. 또는 RabbitMQ UI에서 `analysis.exchange`에 routing key `analysis.NONEXISTENT`로 발행
  3. BE 로그: 에러 없는지 확인
  4. RabbitMQ UI: 어느 큐에도 도착 안 함 확인
- Observe: 메시지가 사라졌다는 사실을 BE는 모름
- Conclude: 콜백 미구현의 실측 증거

### SC5. AnalysisConsumer 의도적 예외 → DLQ 부재
- [ ] Hypothesis: BE Consumer 안에 RuntimeException 발생 시, retry 정책에 따라 N회 시도 후 메시지가 silently 사라짐 (DLQ 없음)
- Setup: 정상 환경
- Execute:
  1. `AnalysisConsumer.java`에 의도적 예외 추가 (테스트용 임시 코드):
     ```java
     if (Math.random() < 0.5) throw new RuntimeException("intentional");
     ```
  2. 분석 메시지 10개 발행
  3. RabbitMQ UI: unacked, ready 카운트 추이 관찰
  4. application.yml의 `max-attempts: 3`이 어떻게 동작하는지 — 3번 시도 후 메시지 어디로?
  5. DB: 결과적으로 처리된 메시지 vs 발행한 메시지 비교
- Observe: 손실된 메시지 수, retry 동작
- Conclude: DLQ 부재 + auto-ack 단점 직접 확인. **테스트 후 임시 코드 반드시 제거**

### SC6. AI Pydantic extra field → 영구 손실
- [ ] Hypothesis: 의도적 typo 필드(`text_k0` 등) 메시지를 발행하면, AI가 ValidationError → NACK requeue=False로 영구 손실
- Setup: 정상 환경
- Execute:
  1. RabbitMQ UI에서 `analysis.requests` 큐로 잘못된 JSON 발행:
     ```json
     {"room_id": 1, "round": 1, "member_id": 1, "text_k0": "typo!", "sentences": []}
     ```
  2. AI 로그: ValidationError 또는 처리 어떻게?
  3. 큐 length 변화 — 1 → 0인지 (drop), 1 → 1인지 (requeue)
- Observe: 손실 vs 무한 루프
- Conclude: pydantic 정책의 실측 영향

### SC7. AI Graceful Shutdown 부재
- [ ] Hypothesis: AI가 분석 처리 중(5~30초) SIGTERM 받으면 ACK 못 하고 메시지 손실 또는 재처리
- Setup: 정상 환경
- Execute:
  1. 분석 트리거 (긴 음성 — 30초짜리 권장)
  2. AI 로그에서 "분석 시작" 보이면 즉시 `docker stop meari-ai` (Ctrl+C)
  3. RabbitMQ UI: 해당 메시지가 unacked 상태로 남는지 확인 (consumer 끊기면 다시 ready로 돌아감)
  4. AI 재기동
  5. 같은 메시지가 재처리되는지 확인 (메시지가 이미 처리 중이었다면 결과는 어떻게)
- Observe: 메시지 운명, ShadowingReport 행 상태
- Conclude: graceful shutdown 부재의 영향

---

## 발견 후 수정 계획 (PR 분할)

각 PR은 별도 브랜치로. 검증 시나리오 통과 후 머지.

### PR1: BE Consumer manual ack + DLQ 도입
- `application.yml`: `acknowledge-mode: manual`
- `RabbitMQConfig`: DLX/DLQ 빈 추가 (`analysis.requests.dlq`, `analysis.results.dlq`)
- `AnalysisConsumer`: `Channel`/`@Header(AmqpHeaders.DELIVERY_TAG)` 받아 명시적 ack/nack
- 검증: SC5 재현 안 됨, 실패 메시지가 DLQ로 격리

### PR2: Publisher Confirm/Return 콜백
- `RabbitTemplate`에 `setConfirmCallback`, `setReturnsCallback`
- routing 실패 시 로그 + 알림
- `mandatory=true` 명시
- 검증: SC4 재현 시 BE에서 발견 가능

### PR3: ShadowingReport 멱등성
- DB UNIQUE 제약: `(room_id, round, member_id)`
- ResultService를 upsert 패턴으로:
  ```java
  Optional<ShadowingReport> existing = repo.findByX();
  if (existing.isPresent()) existing.get().updateAnalysis(...);
  else repo.save(new ShadowingReport(...));
  ```
- 또는 Spring Data의 `@Modifying` UPSERT 쿼리
- 검증: SC3 재현 시 행 1개 유지

### PR4: AI Graceful Shutdown
- consumer thread `daemon=False`로 변경
- `signal.signal(SIGTERM, handler)` — handler는 `consumer.stop_consuming()` 호출 후 timeout 대기
- shutdown_event에서 처리 완료 대기 로직
- 검증: SC7 재현 시 메시지 ACK 정상 또는 깔끔한 재처리

### PR5: AI Pydantic 강화
- `AnalysisRequestMessage` Config: `extra='forbid'`
- 필드별 `Field(ge=0)`, `min_length=1` 등 추가
- ValidationError → manual nack(requeue=False) + DLQ 적재
- 검증: SC6 재현 시 명시 에러 + DLQ 적재

### PR6: BE @Async + 트랜잭션 정합성
- `HttpAnalysisService` 안의 트랜잭션 경계 점검
- `analysisResultService.updateShadowingReport(...)`가 외부 빈인지 확인 (이미 그럼)
- @Async 메서드의 예외 처리 보강 (`AsyncUncaughtExceptionHandler`)

### PR7: BE Consumer 자동 재연결 + 트랜잭션
- `@RabbitListener`에 `@Transactional` 추가 (DB 처리 묶음)
- AI에 따로 `start_consuming` 끊김 시 재연결 루프 (Spring AMQP는 기본 자동 재연결)

---

## 측정 지표 표 (Before/After 채우기)

| 지표 | Before | After (목표) |
|---|---|---|
| 정상 1회 분석 latency (ms) | (SC1 측정) | 변화 없음 (오버헤드 측정) |
| AI 30초 다운 후 자동 처리율 | (SC2) | 100% |
| 같은 메시지 2회 후 DB 중복 행 | (SC3) | 0 |
| Routing 실패 발견 가능 여부 | ❌ silently drop | ✅ 콜백 알림 |
| Consumer 예외 시 메시지 손실률 | (SC5) | 0 (DLQ로 격리) |
| Typo 메시지 손실률 | (SC6) | 0 (DLQ + 명시 ValidationError) |
| AI shutdown 시 처리 중 메시지 운명 | (SC7) | ACK 후 종료 또는 깨끗한 재처리 |

---

## 정리 산출물

- `_rabbitmq_ts_raw.md` (기존) — TS-2~ 시나리오별 raw 노트 누적
- `portfolio_features.md` 1번 섹션의 **심화 3** 신설:
  - "메시지 큐 신뢰성 검증 — 의도적 결함 노출 후 DLQ/멱등성/graceful shutdown 도입 사례"
- 또는 별도 섹션 #16 신설 (별점 ⭐⭐⭐⭐⭐ 가능)

---

## 진행 체크리스트

- [ ] Pre-flight 환경 준비
- [ ] SC1 베이스라인 측정
- [ ] SC2 AI 다운 시뮬
- [ ] SC3 멱등성 부재 재현
- [ ] SC4 Publisher Confirm silently drop
- [ ] SC5 Consumer 예외 + DLQ 부재
- [ ] SC6 Pydantic typo 영구 손실
- [ ] SC7 AI graceful shutdown 부재
- [ ] PR1 manual ack + DLQ
- [ ] PR2 Publisher Confirm 콜백
- [ ] PR3 ShadowingReport 멱등성
- [ ] PR4 AI graceful shutdown
- [ ] PR5 AI Pydantic forbid
- [ ] PR6 BE @Async 트랜잭션 보강
- [ ] PR7 BE Consumer 자동 재연결 + 트랜잭션
- [ ] 측정 지표 표 채우기
- [ ] portfolio_features.md 심화 3 작성

---

**작성일**: 2026-04-29
**다음 액션**: Pre-flight 환경 준비 → SC1 베이스라인 측정
