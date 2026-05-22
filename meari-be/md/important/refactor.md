# Meari BE 리팩터링 계획

> 배경: 프로젝트 초기 구현은 AI 보조(vibe coding)로 진행되어 구조·코드 품질 재점검이 필요함.
> 본 문서는 **코드 전체 독해 완료 후** 착수한다는 전제 하에 작성됨.
> 독해 가이드는 이 문서 맨 아래 "부록 A. 코드 읽기 순서" 참조.

---

## 📐 전체 원칙

1. **테스트 먼저** — 현재 테스트 19개 / 소스 161 파일. 리팩터 전에 행동 보존용 테스트부터 확보.
2. **한 번에 하나씩** — 특히 Room 도메인은 각 단계가 독립된 PR이 되도록 분할.
3. **엔티티 불변성 유지** — Setter 금지, 도메인 메서드 + Dirty Checking (CLAUDE.md 원칙 준수).
4. **Layered Architecture 엄수** — Controller는 변환·흐름, Service는 트랜잭션, Entity는 도메인 로직.
5. **Phase 단위 브랜치/PR** — 각 Phase 끝에 `/review`로 자체 코드 리뷰.

---

## Phase A. 🚨 긴급 / 안전성 (1~2주)

### A1. 보안·비밀값 점검
- [ ] `Dockerfile`의 빌드 타임 ARG(Gemini, AWS, OpenVidu, JWT 키) → 런타임 환경변수로 이동
- [ ] `global/config/CorsConfig` origin 허용 범위 축소 (현재 전체 허용 의심 — refactor.md 기존 메모)
- [ ] repo 잔재 파일 정리: `data_backup.sql`, `nul`, `gradle-wrapper.jar` 등 `.gitignore` 재정비 후 제거 검토
- [ ] `env.example`에 누락된 키 확인 및 실제 환경과 일치시키기

### A2. Auth 취약점
- [ ] `global/auth/jwt/JwtUtil` TODO: 멀티스레드 lazy init 제거 → `@PostConstruct`에서 key 1회 초기화
- [ ] `SignupRequestDto` / `PasswordUpdateRequestDto` TODO: 비밀번호 패턴 검증 Bean Validation `@Pattern` 추가
- [ ] `TokenBlacklistService` / `RefreshTokenService` Redis 키 TTL·네임스페이스 점검

### A3. 인증/인가 테스트 추가 (현재 0개)
- [ ] `@WebMvcTest` + MockMvc로 로그인 성공/실패/만료/블랙리스트 시나리오
- [ ] JWT 필터 단위 테스트
- [ ] 회원가입 중복 검증(email/nickname) 테스트

---

## Phase B. 🏗️ 구조 개선 (2~4주) — 가장 큰 효과

### B1. ⭐ `RoomService` 분해 (1316 LOC → 4~5개 컴포넌트)
현재 단일 클래스가 상태 머신 + 세션 조정 + WebSocket 브로드캐스트 + 페이즈 로직을 모두 담당.

**제안 구조**
```
domain/room/
├── service/
│   ├── RoomCommandService        # 생성/입장/퇴장 (CRUD)
│   ├── RoomQueryService          # 조회 전용 (@Transactional(readOnly=true))
│   ├── RoomPhaseService          # WAITING→ROLE_SELECT→IN_PROGRESS→COMPLETE 전이
│   ├── RoomBroadcastService      # STOMP 송신 책임만
│   └── RoomMembershipService     # MemberRoom 조인/탈퇴
└── phase/
    ├── PhaseHandler (interface)
    ├── WaitingPhaseHandler
    ├── RoleSelectPhaseHandler
    ├── InProgressPhaseHandler
    └── CompletePhaseHandler
```
- 패턴: **Strategy/State** — `GamePhase` enum ↔ `PhaseHandler` 매핑
- 작업 순서: ① 테스트 커버 확보 → ② 읽기 전용 `RoomQueryService` 먼저 분리 → ③ Broadcast 분리 → ④ Phase 전략화 → ⑤ Command 축소

### B2. `RoomWebSocketController` 비즈니스 로직 제거 (255 LOC)
- [ ] 상태 전이·에러 브로드캐스트 로직을 Service로 이동
- [ ] TODO로 남은 `/user/queue/errors` (개인 에러 큐) 구현
- [ ] 컨트롤러는 메시지 파싱 + 서비스 호출만 담당

### B3. DTO ↔ Entity 변환 일관화
- [ ] 현재 `dashboard`만 `DashboardMapper` 존재, 나머지는 Service 인라인 변환
- [ ] 선택: **MapStruct 도입** 또는 **DTO에 `static from(Entity)` 관례 통일**
- [ ] Controller에서 DTO 변환하는 케이스 제거 (CLAUDE.md 계층 규칙)

### B4. 분석 파이프라인 회복력
- [ ] `HttpAnalysisService` TODO: 재시도 로직 → Spring Retry `@Retryable`
- [ ] RabbitMQ 장애 시 HTTP 폴백 경로 명확화 (현재 모호)
- [ ] DLQ(Dead Letter Queue) 추가 — 현재 TTL 10분만 존재
- [ ] `AnalysisConsumer` 실패 시 재처리 정책 정의

### B5. 외부 API 호출 추상화
- [ ] `GeminiAnalysisService`, `AnthropicService`, `OpenAiService` REST 호출 패턴 중복 → 공통 `ExternalApiClient` 추상화
- [ ] 사용되지 않는 `OpenAiService` DTO 정리 (사용 여부 확정)

---

## Phase C. 🎨 품질 / 성능 (1~2주)

### C1. N+1 예방
- [ ] `RoomController.getRoomList` 커서 페이지네이션에서 Room→Member/Content 지연로딩 N+1 검증
- [ ] 필요 지점에 `@EntityGraph` / fetch join / Projection DTO 적용
- [ ] Report/Dashboard 리스트 조회 경로도 점검

### C2. 테스트 보강 (핵심 도메인 커버리지 60%+ 목표)
- [ ] Room 상태 전이 시나리오 테스트
- [ ] Auth / Security 필터 통합 테스트
- [ ] OpenVidu 세션 라이프사이클 테스트 (Mocking)
- [ ] Gemini / Anthropic API 호출 WireMock 테스트
- [ ] Entity 도메인 메서드 단위 테스트

### C3. 로깅 / 관찰성
- [ ] CLAUDE.md 규칙대로 주요 상태 전이에만 `log.debug`, 단순 확인 로그 제거
- [ ] MDC에 `roomId`, `memberId` 추가하여 분산 추적성 향상
- [ ] 예외 발생 지점 로그 레벨 일관화

### C4. 코드 중복 / 단순화 (`/simplify` 스킬 활용)
- [ ] CSV 파서(`CsvScriptParser` 266L)와 `ScriptSavingService` 책임 재분리
- [ ] 중복 Validation 로직 공통 유틸화

---

## Phase D. 🧹 정리 (선택, 여유 시간)

- [ ] Cloudinary vs S3 이중 구조 정리 — 하나로 통일
- [ ] 미사용 의존성 점검 (build.gradle)
- [ ] Swagger(`@Tag`, `@Operation`, `@Schema`) 누락분 보강
- [ ] `build/` 디렉터리, `meari-be.iml` 등 repo 잔재 정리
- [ ] `HELP.md`, `md/todo.md`, `md/todo2.md` 등 오래된 문서 아카이브

---

## 🗓️ 추정 일정

| Phase | 기간 | 우선순위 | 비고 |
|---|---|---|---|
| A | 1~2주 | 🔴 필수 | 보안 먼저 |
| B | 2~4주 | 🟠 권장 | B1이 가장 큼 — 설계 문서 선행 |
| C | 1~2주 | 🟡 권장 | 테스트·성능 |
| D | 상시 | 🟢 선택 | 틈틈이 |

---

## ✅ 진행 체크리스트 규칙

1. 각 항목 착수 시 브랜치 분리 (`refactor/A1-secrets`, `refactor/B1-room-split` 등)
2. PR 단위를 작게 — 1 PR = 1 목적
3. 리팩터 PR에는 반드시 관련 테스트 동반
4. B1 RoomService 분해는 **설계 문서(별도 md)** 작성 후 착수
5. 매 Phase 종료 시 `/review` 로 자체 리뷰

---

## 부록 A. 코드 읽기 순서 (리팩터 착수 전 필수 이해)

### Step 0. 사전 준비
- `CLAUDE.md` (컨벤션) / `md/plan.md` / `md/api.md` / `md/erd.md` / `md/common_response.md`
- `build.gradle` 의존성 훑기

### Step 1. 글로벌 기반 (45분)
`MeariBeApplication` → `global/common/ApiResponse` → `global/error/ErrorCode` → `BusinessException` + `GlobalExceptionHandler` → `global/entity/BaseEntity` → `global/config/SecurityConfig` → `global/auth/jwt/*` → `global/auth/service/*` → `global/auth/controller/AuthController`

### Step 2. 독립 도메인 (30분)
`member` → `theme` → `word` → `s3` + `global/util/S3Service`

### Step 3. 콘텐츠 도메인 (30분)
`content` → `kopic` (+ `GeminiAnalysisService`) → `admin` (+ `CsvScriptParser`)

### Step 4. ⭐ Room / WebRTC — 핵심 (60분)
`room/entity` → `RoomController` → `RoomService`(1316L) → `RoomSessionService`(772L) → `global/config/WebSocketConfig` + `JwtChannelInterceptor` + `RoomSessionMappingInterceptor` → `RoomWebSocketController` → `webrtc/config` → `OpenViduService`(313L) → `WebRtcRoomService`

### Step 5. AI 분석 파이프라인 (45분)
`global/config/RabbitMQConfig` → `analysis/AnalysisRequestBuilder` → `AnalysisProducer` → `AnalysisConsumer` → `HttpAnalysisService` → `solo_practice` → `global/pipeline/videosaving/ScriptSavingService` → `NlpServiceImpl`(KOMORAN) → `HomonymDisambiguationService` → `AnthropicService`

### Step 6. 결과 도메인 (20분)
`report` (ShadowingReport/KopicReport, JSONB) → `dashboard` (+ `DashboardMapper`) → `mypage`

### 독해 시 체크 포인트
- Entity: public setter 여부 / 도메인 메서드 유무
- Service: `@Transactional` / `readOnly` 구분
- Controller: DTO 변환 위치
- Repository: `@EntityGraph` / fetch join 사용 여부 (N+1)
- TODO / FIXME: 발견 시 즉시 본 문서에 추가

---

**작성일**: 2026-04-20
**버전**: 1.0
