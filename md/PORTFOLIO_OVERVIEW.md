# Meari 프로젝트 포트폴리오 정리

> 담당: Backend (Spring Boot) + AI Server (FastAPI)
> 기간: SSAFY 1기 프로젝트
> 도메인: i14c207.p.ssafy.io

---

## 1. 프로젝트 한 줄 요약

**AI 기반 한국어 발음 교정 플랫폼** - WebRTC 실시간 화상 연습 + Wav2Vec2 발음 분석을 결합한 한국어 학습 서비스

---

## 2. 내가 만든 것 (담당 영역 정리)

### 2.1 meari-be (Spring Boot 백엔드)

| 영역 | 주요 작업 | 핵심 기술 |
|------|----------|-----------|
| **인증/인가** | JWT 기반 로그인, 회원가입, 토큰 갱신/블랙리스트 | Spring Security, Redis, BCrypt |
| **실시간 방 관리** | 쉐도잉 방 생성/참가/퇴장, 라운드 진행, 상태 동기화 | WebSocket(STOMP), Redis |
| **WebRTC 화상** | OpenVidu 세션/연결 관리 (최대 4인) | OpenVidu 2.29.0 |
| **발음 분석 파이프라인** | 비동기 분석 요청/결과 수신, 다중 AI Provider 연동 | RabbitMQ, OpenAI/Claude/Gemini API |
| **콘텐츠 관리** | 테마/영상/문장/역할 CRUD, CSV 일괄 업로드 | JPA, Apache Commons CSV |
| **KOPIC 모의고사** | 랜덤 문제 출제, AI 채점, 종합 리포트 | JPA, JSONB |
| **대시보드** | GitHub 스타일 활동 히트맵, 학습 통계 | JPA, 커서 기반 페이지네이션 |
| **파일 업로드** | S3 Presigned URL 발급 (클라이언트 직접 업로드) | AWS S3 SDK |
| **리포트** | 쉐도잉/KOPIC 분석 결과 조회 및 집계 | JSONB, DTO 매핑 |

**API 엔드포인트 총 30+개**, **엔티티 14개**, **테스트 19개 파일**

### 2.2 meari-ai (FastAPI AI 서버)

| 영역 | 주요 작업 | 핵심 기술 |
|------|----------|-----------|
| **음성 인식 (ASR)** | Wav2Vec2 모델로 한국어 음성 → 자모 변환 | HuggingFace Transformers, PyTorch |
| **정확도 분석** | 편집 거리 기반 음절 단위 정확도 계산 | Levenshtein Distance |
| **억양 분석** | Praat 기반 피치 추출 + DTW 비교 | Parselmouth, fastdtw |
| **비동기 메시징** | RabbitMQ Consumer/Producer 구현 | pika |
| **오디오 처리** | S3 비동기 다운로드, 16kHz 리샘플링, 포맷 변환 | aioboto3, librosa, ffmpeg |
| **한국어 NLP** | 자모 → 음절 재조합, 형태소 분석 | 커스텀 korean_utils |

---

## 3. 기술 스택 전체 정리

### Backend
```
Java 21 / Spring Boot 3.5.9 / Spring Security 6.x / Spring Data JPA
PostgreSQL 15 / Redis / RabbitMQ 3
WebSocket(STOMP) / OpenVidu 2.29.0
AWS S3 SDK / Cloudinary
SpringDoc OpenAPI 2.8.4
Gradle 8.7 / JUnit 5 / Mockito
```

### AI Server
```
Python 3.11 / FastAPI 0.115.5
PyTorch 2.5.1 / Transformers 4.46.3 (Wav2Vec2)
Parselmouth (Praat) / librosa / fastdtw
pika (RabbitMQ) / aioboto3 (S3)
```

### Infra/DevOps
```
Docker / Docker Compose
Jenkins CI/CD + GitLab Webhook
Nginx (리버스 프록시 + SSL)
Let's Encrypt (HTTPS)
```

---

## 4. 아키텍처 요약

```
[React Frontend] ──HTTP/WS──> [Nginx :80,:443]
                                    │
                    ┌───────────────┼───────────────┐
                    ▼               ▼               ▼
            [Spring Boot]    [OpenVidu]      [Static Files]
             :8080               (WebRTC)
                │
        ┌───────┼───────┬──────────┐
        ▼       ▼       ▼          ▼
    [PostgreSQL] [Redis] [S3]  [RabbitMQ]
                                   │
                                   ▼
                            [FastAPI :8000]
                             Wav2Vec2 + Praat
```

### 발음 분석 흐름
```
사용자 녹음 → S3 업로드(Presigned URL)
  → Spring Boot가 RabbitMQ에 분석 요청 발행
  → FastAPI Consumer가 수신
  → S3에서 오디오 다운로드
  → Wav2Vec2 ASR + 피치 분석
  → 결과를 RabbitMQ로 반환
  → Spring Boot가 결과 수신 → DB 저장
  → WebSocket으로 클라이언트에 결과 푸시
```

---

## 5. DB 설계 (핵심 엔티티)

| 엔티티 | 설명 | 주요 컬럼 |
|--------|------|-----------|
| Member | 사용자 | email(UK), nickname(UK), nativeLanguage |
| Room | 쉐도잉 방 | status(WAITING/IN_PROGRESS/COMPLETED), maxPeople, password |
| Content | 학습 영상 | theme_id, videoUrl, thumbnailUrl, totalDuration |
| Role | 등장인물 | content_id, characterName |
| Sentence | 대사 | content_id, role_id, textKo, textVn, startTime, endTime |
| ShadowingReport | 쉐도잉 결과 | accuracy, intonation, detailedAnalysis(JSONB) |
| KopicReport | KOPIC 결과 | totalScore, accuracy, intonation, detailedAnalysis(JSONB) |
| DailyRecord | 학습 기록 | recordDate, completedCount |

JSONB 컬럼을 활용해 AI 분석 상세 데이터를 유연하게 저장

---

## 6. 설계 패턴 및 아키텍처적 특징

### Spring Boot
- **도메인 기반 패키지 구조**: domain/ 하위에 14개 도메인 모듈
- **계층형 아키텍처**: Controller → Service → Repository → Entity
- **전략 패턴**: AnalysisService 인터페이스 + RabbitMQ/HTTP 구현체 교체
- **글로벌 예외 처리**: @RestControllerAdvice + 커스텀 ErrorCode 체계
- **통일 응답 포맷**: ApiResponse<T> 래퍼
- **커서 기반 페이지네이션**: 방 목록 조회 등
- **JWT 필터 체인**: JwtExceptionFilter → JwtAuthenticationFilter → DefaultAuthenticationFilter

### FastAPI
- **싱글턴 모델 로딩**: 앱 시작 시 Wav2Vec2 모델 1회 로드
- **비동기 병렬 처리**: asyncio.gather()로 S3 다운로드 병렬화
- **문장별 독립 에러 처리**: 하나의 문장 분석 실패가 전체에 영향 안 줌
- **Pydantic 스키마 검증**: 요청/응답 자동 검증

---

## 7. 현재 상태 진단

### 잘 된 점
- 도메인 분리가 깔끔하고 일관된 패턴 적용
- JWT + Redis 블랙리스트 인증 체계
- RabbitMQ 비동기 분석 파이프라인
- WebSocket + WebRTC 실시간 통신
- Multi-stage Docker 빌드 + Jenkins CI/CD
- 19개 테스트 파일 (Given-When-Then 패턴)
- SpringDoc API 문서화 기반 존재

### 개선이 필요한 점

| 우선순위 | 영역 | 현재 상태 | 목표 |
|---------|------|----------|------|
| **P0** | 테스트 커버리지 | BE 19개 파일, AI 테스트 없음 | BE 80%+, AI pytest 도입 |
| **P0** | API 문서화 | Swagger 설정만 존재, @Operation 미비 | 전 엔드포인트 문서화 |
| **P1** | 모니터링 | 없음 | Actuator + Prometheus + Grafana |
| **P1** | 로깅 | 기본 로깅만 | 구조화된 로깅 (Logback JSON) |
| **P1** | 캐싱 | Redis를 세션/블랙리스트로만 사용 | 콘텐츠/통계 캐싱 적용 |
| **P2** | 보안 | CORS 전체 허용, Rate Limiting 없음 | CORS 제한, Rate Limit 추가 |
| **P2** | DB 최적화 | N+1 잠재적 이슈 | Fetch Join, 쿼리 최적화 |
| **P2** | AI 테스트 | 수동 테스트 스크립트만 | pytest + 커버리지 |
| **P3** | 코드 품질 | 수동 리뷰만 | JaCoCo, SonarQube |

---

## 8. 주간 개선 계획

### 1주차: 문서화 + 테스트 기반

| 일 | 작업 | 예상 시간 |
|----|------|----------|
| Day 1 | Spring Boot 전체 Controller에 @Operation, @Tag 추가 | 3h |
| Day 2 | DTO에 @Schema 설명 + 요청/응답 예시 추가 | 3h |
| Day 3 | JaCoCo 플러그인 추가, 기존 테스트 실행 확인 | 2h |
| Day 4 | RoomService 테스트 보강 (엣지 케이스) | 3h |
| Day 5 | AnalysisService 테스트 추가 (Mock 활용) | 3h |
| Day 6 | FastAPI pytest 환경 설정 + 첫 테스트 작성 | 3h |
| Day 7 | 정리 및 커밋 | 1h |

### 2주차: 테스트 심화 + 모니터링

| 일 | 작업 | 예상 시간 |
|----|------|----------|
| Day 1 | FastAPI analysis_service 단위 테스트 (모델 모킹) | 3h |
| Day 2 | FastAPI intonation_analyzer 테스트 | 3h |
| Day 3 | Spring Actuator + Prometheus 설정 | 2h |
| Day 4 | Grafana 대시보드 생성 (Spring Boot 메트릭) | 3h |
| Day 5 | Logback JSON 구조화 로깅 적용 | 2h |
| Day 6 | RabbitMQ 모니터링 대시보드 | 2h |
| Day 7 | 정리 및 커밋 | 1h |

### 3주차: 성능 최적화 + 보안

| 일 | 작업 | 예상 시간 |
|----|------|----------|
| Day 1 | N+1 쿼리 점검 및 Fetch Join 적용 | 3h |
| Day 2 | Redis 캐싱 적용 (@Cacheable - 콘텐츠, 대시보드) | 3h |
| Day 3 | CORS 정책 수정 (운영환경 도메인 제한) | 1h |
| Day 4 | Rate Limiting 도입 (Bucket4j) | 2h |
| Day 5 | k6 부하 테스트 스크립트 작성 + 실행 | 3h |
| Day 6 | 부하 테스트 결과 분석 및 문서화 | 2h |
| Day 7 | 정리 및 커밋 | 1h |

### 4주차: 포트폴리오 마무리

| 일 | 작업 | 예상 시간 |
|----|------|----------|
| Day 1 | README.md 최종 정리 (아키텍처 다이어그램 확인) | 2h |
| Day 2 | 기술적 챌린지 문서 작성 (면접 대비) | 3h |
| Day 3 | 성능 지표 정리 (테스트 결과, 응답 시간 등) | 2h |
| Day 4 | SonarQube 로컬 실행 + 코드 품질 리포트 | 3h |
| Day 5 | 발견된 코드 스멜 수정 | 3h |
| Day 6 | 최종 검토 + GitHub/GitLab 정리 | 2h |
| Day 7 | 이력서 프로젝트 설명 문구 확정 | 1h |

---

## 9. 포트폴리오 어필 포인트 (면접 대비)

### 기술적 깊이를 보여주는 키워드

1. **비동기 메시지 큐 아키텍처**
   - "발음 분석이 5~30초 걸리는 문제를 RabbitMQ 비동기 처리로 해결. 사용자에게 즉시 응답 후 WebSocket으로 결과 푸시"

2. **마이크로서비스 통신**
   - "Spring Boot ↔ FastAPI를 RabbitMQ로 연결. 서비스 간 느슨한 결합 + 독립적 스케일링 가능"

3. **실시간 상태 동기화**
   - "4명의 사용자가 동시에 진행하는 쉐도잉 세션에서 Redis + WebSocket으로 녹음 완료 상태 동기화"

4. **JWT 인증 체계**
   - "Access/Refresh 토큰 패턴 + Redis 블랙리스트로 안전한 로그아웃 구현"

5. **S3 Presigned URL**
   - "서버 부하 없이 클라이언트가 S3에 직접 업로드. 서버 메모리 90% 절감"

6. **AI 모델 서빙**
   - "Wav2Vec2 ASR + Praat 피치 분석을 FastAPI로 서빙. 문장별 독립 에러 처리로 안정성 확보"

7. **도메인 주도 설계**
   - "14개 도메인 모듈로 분리. 각 도메인은 Controller/Service/Repository/Entity/DTO 계층 보유"

### 예상 면접 질문 + 답변 키포인트

| 질문 | 답변 핵심 |
|------|----------|
| RabbitMQ를 왜 선택? | 발음 분석 지연 문제 → 비동기 처리로 UX 개선. Kafka 대비 라우팅 유연성 |
| WebSocket vs SSE? | 양방향 통신 필요 (방 상태 + 채팅). SSE는 단방향이라 부적합 |
| N+1 문제 경험? | 대시보드 통계 쿼리에서 발견 → Fetch Join + 복합 인덱스로 해결 |
| Redis 활용? | 세션, JWT 블랙리스트, 녹음 상태 관리, 캐싱 (4가지 용도) |
| 에러 핸들링? | 글로벌 예외 처리 + 커스텀 ErrorCode + 통일 응답 포맷 |
| 배포 프로세스? | GitLab → Jenkins Webhook → Docker Build → Compose Deploy → Mattermost 알림 |
| 테스트 전략? | 단위 테스트(Mockito) + Given-When-Then 패턴. 비즈니스 로직 중심 |

---

## 10. 이력서 프로젝트 설명 (예시)

```
[Meari - AI 기반 한국어 발음 교정 플랫폼]

프로젝트 개요: WebRTC 실시간 화상 연습 + AI 발음 분석 학습 플랫폼
팀 구성: 6인 (BE 2, FE 2, AI 1, Infra 1)
담당 역할: Backend + AI Server 개발

주요 성과:
- Spring Boot 기반 RESTful API 30+ 엔드포인트 설계/개발
- RabbitMQ 비동기 파이프라인으로 발음 분석 처리 (동기 대비 UX 99% 개선)
- Wav2Vec2 ASR + Praat 피치 분석 FastAPI 서버 구축
- JWT + Redis 인증 체계, WebSocket/WebRTC 실시간 통신
- Jenkins + Docker CI/CD 파이프라인 구축
- 단위 테스트 19개 파일 (Given-When-Then 패턴)

기술 스택:
BE: Java 21, Spring Boot 3.5, JPA, Spring Security, PostgreSQL, Redis, RabbitMQ
AI: Python 3.11, FastAPI, PyTorch, Wav2Vec2, Parselmouth
Infra: Docker, Jenkins, AWS S3, Nginx, OpenVidu
```

---

## 11. 추가 개선 아이디어 (여유 있을 때)

| 아이디어 | 포트폴리오 가치 | 난이도 |
|---------|---------------|--------|
| Dead Letter Queue 설정 | 메시지 유실 방지 운영 경험 | 낮음 |
| Custom Health Indicator | RabbitMQ/S3 연결 상태 체크 | 낮음 |
| Spring Profiles 분리 | dev/prod 환경 분리 경험 | 낮음 |
| API 버전 관리 전략 문서화 | 설계 사고력 어필 | 낮음 |
| Testcontainers 통합 테스트 | 실제 DB/MQ로 테스트 | 중간 |
| Blue-Green 배포 | 무중단 배포 경험 | 중간 |
| Prometheus + Grafana 대시보드 스크린샷 | 모니터링 경험 시각화 | 중간 |
| k6 부하 테스트 결과 보고서 | 성능 측정 능력 증명 | 중간 |
| Kubernetes 매니페스트 작성 | K8s 경험 추가 (이력서 키워드) | 높음 |

---

## 부록: 파일 구조 맵

### meari-be 핵심 파일
```
src/main/java/com/ssafy/meari/
├── domain/
│   ├── room/          ← 핵심 비즈니스 로직 (방 관리 + 게임 진행)
│   ├── analysis/      ← AI 분석 파이프라인 (RabbitMQ/HTTP 전략)
│   ├── kopic/         ← KOPIC 모의고사
│   ├── dashboard/     ← 학습 대시보드
│   ├── content/       ← 콘텐츠 관리
│   ├── report/        ← 리포트 조회
│   ├── member/        ← 회원 관리
│   ├── s3/            ← 파일 업로드
│   ├── solo_practice/ ← 개인 연습
│   ├── webrtc/        ← OpenVidu 연동
│   ├── mypage/        ← 마이페이지
│   ├── theme/         ← 테마 관리
│   ├── word/          ← 어휘 관리
│   └── admin/         ← 관리자 (CSV 업로드)
└── global/
    ├── auth/          ← JWT 인증 체계
    ├── config/        ← Security, WebSocket, RabbitMQ, S3, Redis 설정
    ├── error/         ← 글로벌 예외 처리 + ErrorCode
    ├── common/        ← ApiResponse, 공통 DTO
    └── pipeline/      ← 비디오 처리 파이프라인
```

### meari-ai 핵심 파일
```
app/
├── main.py            ← FastAPI 엔트리포인트
├── config.py          ← 환경변수 관리
├── models/
│   └── model_loader.py ← Wav2Vec2 모델 로딩 (싱글턴)
├── services/
│   ├── analysis_service.py    ← 핵심: ASR + 정확도 + 억양 분석
│   ├── rabbitmq_consumer.py   ← MQ 메시지 수신
│   └── rabbitmq_producer.py   ← MQ 결과 발행
├── utils/
│   ├── korean_utils.py        ← 자모 → 음절 변환
│   ├── intonation_analyzer.py ← 피치 분석 (Praat + DTW)
│   └── reference_audio_manager.py ← S3 오디오 캐싱
└── schemas/
    ├── request.py     ← Pydantic 요청 모델
    └── response.py    ← Pydantic 응답 모델
```
