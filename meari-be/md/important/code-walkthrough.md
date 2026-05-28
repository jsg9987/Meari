# Meari BE 코드 워크스루 — 전체 목차

## 이 문서가 왜 있나

프로젝트 전체 코드(~14,000 LOC, ~200 파일)를 혼자 일일이 읽으면 너무 오래 걸리고, 큰 그림이 안 잡힘. 그래서 AI 어시스턴트와 함께 **파일 단위로 깊게 (라인까지) + 프레임워크 개념과 함께** 읽어 나가기 위한 목차 문서.

목표: 워크스루가 끝나면
1. 어떤 파일이 어디서 무엇을 하는지 머릿속에 지도가 그려짐
2. Spring 웹 앱이 어떻게 돌아가는지 (Filter, MVC, JPA, 트랜잭션) 자기 언어로 설명 가능
3. `refactor.md`의 B1(RoomService 분해), B2(RoomWebSocketController 정리)를 자신 있게 설계 가능

## 진행 원칙

1. **한 응답 = 작은 단위 1개** (파일 1개, 또는 매우 작은 2~3개 묶음)
2. **각 단위 시작 시 개념 도입** — 이 코드가 의존하는 프레임워크/패턴 1~3개 짧게
3. **그 다음 코드 라인별 설명** — 어노테이션, 필드, 메서드 모두. 보일러플레이트(import 등)만 생략
4. **끝에 문제점/리팩터 후보** 짚음 (refactor.md와 연결되는 부분 표시)
5. **다음 단위로 갈 때 사용자 승인** — "다음 (X) 진행할까요?" 물어보고 OK 받기 전엔 안 넘어감
6. 사용자가 "잠깐 X 더 깊이" 하면 그 단위에서 더 파고듦

## 진행 표시 규칙

- `[ ]` 아직
- `[~]` 진행 중
- `[x]` 완료

---

## 섹션 0 — Spring 웹 애플리케이션 기초 (개념)

코드 들어가기 전에 큰 그림과 핵심 개념. 사용자가 "왜 이 코드를 적는지" 모른 채로 가면 무의미.

|     | 단위 | 다루는 것 |
|-----|---|---|
| [x] | **0A** | 요청 한 번이 응답이 되기까지의 전체 여정 (Tomcat → Filter Chain → DispatcherServlet → Interceptor → Controller → Service → Repository → DB → 역순) |
| [x] | **0B** | Spring IoC/DI — `@Component`/`@Service`/`@Configuration`/`@Bean` 가족, ApplicationContext, Bean lifecycle, 생성자 주입 |
| [x] | **0C** | Layered Architecture — Controller/Service/Repository/Entity 책임 분리, 의존 방향, DTO가 왜 있는가 |
| [x] | **0D** | Spring MVC + Spring Security 동거 방식 — 두 영역의 경계, `@RestControllerAdvice`가 닿는 범위 vs 안 닿는 범위 |
| [x] | **0E** | JPA로 객체와 DB 잇기 — Entity/Repository/EntityManager, 트랜잭션, Dirty Checking, Lazy/Eager Loading, N+1 문제 |
| [~] | **0F** | Meari 프로젝트의 큰 그림 — 무엇을 만든 것이며 도메인끼리 어떻게 엮여 있나 (사용자 시나리오: 회원가입 → 방 만들기 → 영상 따라하기 → 분석 → 리포트) |

---

## 섹션 1 — 인증·보안 시스템 (~760 LOC)

`global/auth/`, `global/config/SecurityConfig`. refactor.md A2 항목과 직결.

> **학습 순서 재배치(2026-05-26)**: 기존 1A~1K는 색인 순서라 첫 학습엔 부적합 → "필요 기반" 순서로 재배치. 회원가입(Security 거의 불필요)에서 출발해 필요가 쌓이는 순서로 가고, 전체 배선인 SecurityConfig는 조각을 다 본 뒤 ⑧에서 재조립. (대괄호 안은 원래 색인 라벨)
>
> **첫 절반 = "로그인해서 토큰을 받기까지"(①~⑤), 두 번째 절반 = "그 토큰으로 매 요청 인증 + 배선 + 부가"(⑥~⑪).**

| | 단위 (학습 순서) | 파일 | 주요 도입 개념 |
|---|---|---|---|
| [x] | **① Auth DTO + 검증** [1J] | `LoginRequestDto`/`SignupRequestDto` | record, @Email/@NotBlank/@Size/@NotNull, @Valid→MethodArgumentNotValidException(0D 연결), @JsonProperty. A2 TODO(비번 패턴) |
| [x] | **② 회원가입 + 비번 암호화** [1J/1I 일부] | `MemberServiceImpl.signup` | BCryptPasswordEncoder, 단방향 해싱, salt, matches |
| [x] | **③ JWT + JwtUtil** [1B] | `JwtUtil` (97L) | JWT 구조(header.payload.signature), HS256, Claims(subject/type/exp), @Value, parseSignedClaims 예외 |
| [x] | **④ UserDetails 어댑터** [1G] | `UserDetailsImpl` + `UserDetailsServiceImpl` | Adapter 패턴, UserDetails 규격, loadUserByUsername, DaoAuthenticationProvider 연결, 빈 authorities |
| [x] | **⑤ 로그인 흐름** [1E + SecurityConfig 일부] | `DefaultAuthenticationFilter` + `AuthenticationManager` | AbstractAuthenticationProcessingFilter, RequestMatcher, attempt/success/unsuccess, ResponseCookie(httpOnly), Set-Cookie |
| [x] | **⑥ 매 요청 검증** [1C] | `JwtAuthenticationFilter` (98L) | OncePerRequestFilter, SecurityContextHolder(ThreadLocal), 인증정보 주입, 스킵경로 중복 냄새 |
| [x] | **⑦ 필터 예외 처리** [1D] | `JwtExceptionFilter` (55L) | 0D에서 봄, ⑥보다 바깥에 위치해 try-catch로 감쌈 |
| [x] | **⑧ SecurityConfig 재조립** [1A] | `SecurityConfig` (149L) | AuthenticationManager, 기본값 disable(STATELESS), authorizeHttpRequests, addFilterBefore 3개, WebSecurityCustomizer(ignoring vs permitAll) |
| [x] | **⑨ AuthController 종합** [1I/1H] | `AuthController` + `AuthService` | @AuthenticationPrincipal(⑥의 결실), refresh/logout, 예외 변환 패턴 |
| [x] | **⑩ RefreshToken + Blacklist** [1F] | `RefreshTokenService` + `TokenBlacklistService` | Redis, RedisTemplate, TTL=토큰수명(자동삭제), 키 네임스페이싱, access/refresh 분리, stateless 로그아웃 |
| [x] | **⑪ CorsConfig** [1K] | `CorsConfig` | SOP, preflight(OPTIONS), allowedOrigins/methods/headers, allowCredentials+"*" 금지 |

---

## 섹션 2 — 독립 도메인 (~440 LOC)

가장 단순한 CRUD 도메인. 프로젝트 표준 패턴 학습. refactor.md B3와 연결.

| | 단위 | 파일 | 주요 도입 개념 |
|---|---|---|---|
| [ ] | **2A** | Member Entity + BaseEntity 재참조 | JPA @Entity/@Id/@GeneratedValue, 엔티티 매핑, 빌더 패턴 적용 |
| [ ] | **2B** | `MemberRepository` + 관련 인터페이스 메서드들 | JpaRepository, 메서드 이름 쿼리(findBy/existsBy), Optional |
| [ ] | **2C** | `MemberServiceImpl` + `MemberMapper` | @Transactional readOnly, BCrypt 적용, 도메인 메서드(`Member.deactivate()` 등) 사용 |
| [ ] | **2D** | `SignupRequestDto`/`PasswordUpdateRequestDto` 검증 | @NotBlank/@Email/@Pattern, 검증 실패 흐름 (refactor.md A2 TODO 연결) |
| [ ] | **2E** | `theme/` 도메인 (30L) | 가장 작은 도메인. enum성 데이터를 어떻게 다루나 |
| [ ] | **2F** | `word/` Entity (Word, Sentence) | Room/분석에서 참조될 단어·문장 모델 |
| [ ] | **2G** | `s3/` 도메인 + `global/util/S3Service` | Presigned URL 패턴, AWS SDK, 보안 (refactor.md A1 — AWS 키 점검) |

---

## 섹션 3 — 콘텐츠·KOPIC 도메인 (~1,800 LOC)

학습 콘텐츠와 외부 AI(Gemini) 연동의 첫 사례. refactor.md B5 연결.

| | 단위 | 파일 | 주요 도입 개념 |
|---|---|---|---|
| [ ] | **3A** | content Entity (Content, Script, Role) | 1:N 매핑(@OneToMany), Cascade, FetchType |
| [ ] | **3B** | `ContentService` + `ContentPreprocessService`(206L) | 외부 데이터 전처리 패턴 |
| [ ] | **3C** | `ContentController` + DTO들 | REST 페이지네이션, Cursor vs Offset |
| [ ] | **3D** | kopic Entity (KopicSentence, KopicSentenceLevel) | 다국어/난이도 데이터 |
| [ ] | **3E** | `KopicEvaluateService` | 채점 도메인 흐름 |
| [ ] | **3F** | ⭐ `GeminiAnalysisService` (270L) | 외부 LLM 호출 패턴, RestClient, JSON 파싱, 리트라이 부재 |
| [ ] | **3G** | `KopicReportService` + 통합 리포트 | 점수 집계 |
| [ ] | **3H** | `KopicController` | 모의고사 시작/제출 흐름 |
| [ ] | **3I** | admin Entity + `CsvScriptParser`(266L) | CSV 파싱, OpenCSV, 책임 비대 (refactor.md C4) |
| [ ] | **3J** | admin Service + Controller | MultipartFile 업로드, 트랜잭션 경계 |

---

## 섹션 4 — ⭐ Room 도메인: REST + 상태 머신 (~1,900 LOC) — **B1의 대상**

가장 복잡한 도메인. 1316L의 RoomService가 핵심.

| | 단위 (학습 순서) | 다루는 것 | 주요 개념 |
|---|---|---|---|
| [ ] | **4-0 상태 모델** [4A+4B] | Room / RoomStatus / GamePhase / MemberRoom (+MemberRound) | 방 생애주기 지도(WAITING→IN_PROGRESS{WATCHING→ROLE_PICK→ROUND}), @Enumerated(STRING), 조인 엔티티, **RoomStatus(DB) vs GamePhase(Redis)** 이원 관리 |
| [ ] | **4-1 흐름: 방 생성·조회** [4D+4E+4C] | RoomController(해당부분) → createRoom/createQuickRoom/getRoomList/getRoomDetail → Repository | REST 매핑, @Transactional(readOnly), DTO 변환, 커서 페이지네이션, N+1 점검(C1) |
| [ ] | **4-2 흐름: 입장·퇴장** [4F] | enterRoom/leaveRoom/kickMember/handleAbnormalDisconnect (+ RoomSessionService 세션) | 정원·권한 체크, 방장 위임(handleOwnerLeave), Redis 세션, **섹션 5와 연결점** |
| [ ] | **4-3 흐름: 게임 진행 ★** [4G+4H+4I] | startGame→selectContent→finishWatching→watchingComplete→confirmRoles→startRound→recordingComplete→finishRound→finishGame | Phase 전이 패턴(**반복되는 상태검증**), 브로드캐스트(**트랜잭션 밖 원칙**), 분석 트리거, 세그먼트 빌드. 1316L의 대부분 |
| [ ] | **4-4 종합 → B1 설계** [4J] | 메서드 → 책임 분류표 + 분해 설계 | Query/Command/Phase/Broadcast/Membership 매핑, **트랜잭션 경계·커밋 후 브로드캐스트**, PhaseHandler 필요성 판단, **Facade는 보류(과설계 — 모듈화부터)** |

---

## 섹션 5 — WebSocket·WebRTC (~1,700 LOC) — **B2의 대상**

실시간 통신 영역. 0A/0D에서 본 "MVC 영역"이 아닌 별도 영역.

> 인프라(설정) → **연결 생애주기(연결→메시지→종료)를 세로 관통** → 세션 모델 종합 → WebRTC 레이어 순. `RoomSessionService`(772L)는 흐름 중 등장 시 참조하고 끝에 한 번 전체를 묶음.

| | 단위 (학습 순서) | 다루는 것 | 주요 개념 |
|---|---|---|---|
| [ ] | **5-0 인프라** [5A] | `WebSocketConfig` + STOMP 기본 | STOMP, /app vs /topic vs /queue, MessageBroker, MVC 밖 영역(0D 연결) |
| [ ] | **5-1 연결 흐름** [5B+5C] | `JwtChannelInterceptor`(CONNECT 인증) → `RoomSessionMappingInterceptor`(session↔room) | ChannelInterceptor, 핸드셰이크/CONNECT 인증, sessionId↔roomId 매핑 |
| [ ] | **5-2 메시지 흐름** [5D+5E/5F] | `RoomWebSocketController`(@MessageMapping) → `RoomSessionService`(세션상태) → 브로드캐스트(/topic) | @MessageMapping, ready/role/chat, **컨트롤러에 박힌 비즈니스 로직(B2 식별)** |
| [ ] | **5-3 종료 흐름** [5G] | `SessionDisconnectEvent` → 세션 정리 (handleWebSocketDisconnect + RoomSessionService) | EventListener, Grace Period, **Redis 정리 순서가 컨트롤러에 있는 문제** |
| [ ] | **5-4 세션 모델 종합** [5E~5G] | `RoomSessionService`(772L) 전체 | Redis 기반 세션/ready/role/phase 상태 모델 한 그림 |
| [ ] | **5-5 WebRTC 레이어** [5H+5I+5J] | WebRTC/OpenVidu 개념 → `OpenViduService`(313L) → `WebRtcRoomService`+컨트롤러 | SFU/MCU, ICE/STUN/TURN, 외부 SaaS, Room lifecycle↔OpenVidu 세션 |
| [ ] | **5-6 종합 → B2 설계** [5K] | RoomWebSocketController 비즈니스 로직 식별 → 매핑표 | Service 이동 대상, **`/user/queue/errors` 구현** 포함 |

---

## 섹션 6 — AI 분석 파이프라인 + 1:1 연습 (~1,700 LOC)

비동기 메시지 큐와 NLP/LLM. refactor.md B4/B5 연결.

| | 단위 | 파일 | 주요 도입 개념 |
|---|---|---|---|
| [ ] | **6A** | RabbitMQ 개념 + `RabbitMQConfig`(105L) | Exchange/Queue/Binding/RoutingKey, AMQP, DLQ 부재 |
| [ ] | **6B** | `AnalysisRequestBuilder` | 메시지 페이로드 빌드 |
| [ ] | **6C** | `AnalysisProducer` | RabbitTemplate, publish 패턴 |
| [ ] | **6D** | `AnalysisConsumer` | @RabbitListener, ack/nack, 재시도 |
| [ ] | **6E** | `HttpAnalysisService` + 동기 vs 비동기 분기 | 두 갈래 경로의 의미, 폴백 전략 부재 |
| [ ] | **6F** | `solo_practice` 도메인 전체 | 분석 트리거 진입점 |
| [ ] | **6G** | `ScriptSavingService` (345L) — 영상 저장 후 처리 | 파이프라인 패턴 |
| [ ] | **6H** | NLP 개념 + `NlpServiceImpl` (358L) | 형태소 분석(KOMORAN), POS 태깅 |
| [ ] | **6I** | `AnthropicService` (110L) | Claude API 호출, 동음이의어 판단 |
| [ ] | **6J** | `HomonymDisambiguationService` (247L) | 후처리 로직 |

---

## 섹션 7 — 결과 도메인 (~2,400 LOC)

학습 결과를 어떻게 저장·집계·노출하는가.

| | 단위 | 파일 | 주요 도입 개념 |
|---|---|---|---|
| [ ] | **7A** | report Entity (ShadowingReport, KopicReport, KopicTotalReport) + JSONB | PostgreSQL JSONB 컬럼, Hibernate JsonType, 정규화 vs 비정규화 트레이드오프 |
| [ ] | **7B** | `ShadowingReportService` (170L) + DetailedAnalysis DTO(215L) | 분석 결과 저장·조회 |
| [ ] | **7C** | KopicReport 흐름 점검 (3에서 미진했던 부분) | 통합 리포트 |
| [ ] | **7D** | dashboard Entity (DailyRecord 등) | 일별 통계 모델 |
| [ ] | **7E** | `DashboardServiceImpl` (249L) — 통계 쿼리 | 집계 쿼리, 성능 점검 (refactor.md C1) |
| [ ] | **7F** | ⭐ `DashboardMapper` — 프로젝트 유일 Mapper | MapStruct 도입 가능성 (refactor.md B3) |
| [ ] | **7G** | mypage 도메인 전체 (150L) | 프로필·비밀번호 변경 |

---

## 섹션 후 — 리팩터 작업 진입

워크스루 완료 시점에 별도 플랜 파일을 만들어 다음을 짠다:

- **B1 — RoomService 분해 상세 계획**: 4J에서 만든 책임 분류표를 기반으로 어떤 메서드가 어느 새 컴포넌트로 가는지 매핑. 단계별 PR 분리 (refactor.md 명시 순서: Query → Broadcast → Phase Strategy → Command 축소)
- **B2 — RoomWebSocketController 정리**: 5K에서 만든 매핑표를 기반으로 어떤 줄이 어느 서비스로 가는지 정확히. `/user/queue/errors` 구현도 같이.
- 행동 보존 테스트 부족분 식별 후 보강 (refactor.md A3 후속)

---

## 진행 상황 기록 방식

각 단위 끝나면 이 문서 표의 `[ ]` → `[x]`로 바꿈. 사용자가 직접 체크해도 되고, AI에게 부탁해도 됨.

전체 진행률: **17 / 64 완료** (섹션 0 전체: 0A~0F, 섹션 1 전체: ①~⑪). 섹션 1은 학습 순서 재배치됨(위 표 참고). 다음: 섹션 2(독립 도메인) — 2A Member Entity.

## 사용 팁

- 한 자리에서 1~3개 단위씩 진행이 무난함. 너무 많이 몰아서 들으면 머리에 안 남음
- 각 단위 끝나면 그 단위에서 무엇을 배웠는지 짧게 자기 언어로 요약 (이게 학습에 가장 효과적)
- 모르는 개념이 나오면 그 자리에서 깊이 파고듦. "다음으로 넘어가자"는 사용자 의지가 명확할 때만 진행

---

**작성일**: 2026-04-29
**작성 맥락**: refactor.md Phase B(B1, B2) 진입을 위한 사전 코드 이해 작업
Z3 → Z2 → Z5 → Z7 → Z1 → Z6 → Z10 → Z4 → Z9 → Z8