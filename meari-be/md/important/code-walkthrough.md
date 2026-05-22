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

| | 단위 | 다루는 것 |
|---|---|---|
| [x] | **0A** | 요청 한 번이 응답이 되기까지의 전체 여정 (Tomcat → Filter Chain → DispatcherServlet → Interceptor → Controller → Service → Repository → DB → 역순) |
| [ ] | **0B** | Spring IoC/DI — `@Component`/`@Service`/`@Configuration`/`@Bean` 가족, ApplicationContext, Bean lifecycle, 생성자 주입 |
| [ ] | **0C** | Layered Architecture — Controller/Service/Repository/Entity 책임 분리, 의존 방향, DTO가 왜 있는가 |
| [ ] | **0D** | Spring MVC + Spring Security 동거 방식 — 두 영역의 경계, `@RestControllerAdvice`가 닿는 범위 vs 안 닿는 범위 |
| [ ] | **0E** | JPA로 객체와 DB 잇기 — Entity/Repository/EntityManager, 트랜잭션, Dirty Checking, Lazy/Eager Loading, N+1 문제 |
| [ ] | **0F** | Meari 프로젝트의 큰 그림 — 무엇을 만든 것이며 도메인끼리 어떻게 엮여 있나 (사용자 시나리오: 회원가입 → 방 만들기 → 영상 따라하기 → 분석 → 리포트) |

---

## 섹션 1 — 인증·보안 시스템 (~760 LOC)

`global/auth/`, `global/config/SecurityConfig`. refactor.md A2 항목과 직결.

| | 단위 | 파일 | 주요 도입 개념 |
|---|---|---|---|
| [x] | **1A** | `SecurityConfig` (149L) | Spring Security FilterChain, @EnableWebSecurity, AuthenticationManager, BCrypt |
| [ ] | **1B** | `JwtUtil` (97L) | JWT 구조 (header.payload.signature), 서명 알고리즘(HS256), Claims, ExpiredJwtException |
| [ ] | **1C** | `JwtAuthenticationFilter` (98L) | OncePerRequestFilter, Authentication, SecurityContext, UserDetails |
| [ ] | **1D** | `JwtExceptionFilter` (55L) | 필터에서 예외 잡는 패턴, ObjectMapper로 JSON 직접 작성 |
| [ ] | **1E** | `DefaultAuthenticationFilter` (107L) | AbstractAuthenticationProcessingFilter, RequestMatcher, ResponseCookie, Set-Cookie |
| [ ] | **1F** | `RefreshTokenService` + `TokenBlacklistService` | Redis 기본, RedisTemplate, TTL, 키 네임스페이싱 |
| [ ] | **1G** | `UserDetailsImpl` + `UserDetailsServiceImpl` | Spring Security 어댑터 패턴, GrantedAuthority |
| [ ] | **1H** | `AuthService` (74L) | Service 레이어의 책임, 예외 변환 |
| [ ] | **1I** | `AuthController` (96L) | @AuthenticationPrincipal, REST 엔드포인트 매핑 |
| [ ] | **1J** | Auth DTOs (Login/Signup/RefreshToken/AccessToken/EmailCheck/NicknameCheck) | record vs class, @Valid, Bean Validation |
| [ ] | **1K** | `CorsConfig` | CORS 동작 원리, origin/methods/headers, preflight |

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

가장 복잡한 도메인. 1316L의 RoomService를 메서드 그룹으로 쪼개서 다룸.

| | 단위 | 파일/메서드 그룹 | 주요 도입 개념 |
|---|---|---|---|
| [ ] | **4A** | Room Entity + GamePhase 등 enum | 상태 머신 모델링, JPA enum 매핑, @Enumerated(STRING) |
| [ ] | **4B** | MemberRoom, MemberRound 조인 엔티티 | N:M 관계 풀어내기, 복합키 vs 별도 ID |
| [ ] | **4C** | `RoomRepository` 등 모든 Repository | 커서 페이지네이션 쿼리, fetch join, projection |
| [ ] | **4D** | `RoomController` (193L) — REST API | 입장/퇴장/조회 엔드포인트 매핑 |
| [ ] | **4E** | `RoomService` 부분 1: 생성·조회 (createRoom, createQuickRoom, getRoomList, getRoomDetail) | @Transactional, DTO 변환 |
| [ ] | **4F** | `RoomService` 부분 2: 입장·퇴장 (enterRoom, leaveRoom, kickMember, handleAbnormalDisconnect) | 동시성 고려, 정원 체크 |
| [ ] | **4G** | `RoomService` 부분 3: Phase 전이 (startGame, finishWatching, recordingComplete, finishGame) | 상태 머신 전이 트리거, 브로드캐스트 호출 |
| [ ] | **4H** | `RoomService` 부분 4: 역할 (selectRole, confirmRoles, selectContent) | 협상 패턴 |
| [ ] | **4I** | `RoomService` 부분 5: 라운드/녹화 (startRound, watchingComplete) | 라운드 단위 진행 |
| [ ] | **4J** | RoomService 종합 — 책임 분류표 정리 | **B1 분해 직전 매핑표 작성** |

---

## 섹션 5 — WebSocket·WebRTC (~1,700 LOC) — **B2의 대상**

실시간 통신 영역. 0A에서 본 "MVC 영역"이 아닌 별도 영역.

| | 단위 | 파일 | 주요 도입 개념 |
|---|---|---|---|
| [ ] | **5A** | `WebSocketConfig` + STOMP 기본 | STOMP 프로토콜, /topic vs /queue vs /app, MessageBroker |
| [ ] | **5B** | `JwtChannelInterceptor` | ChannelInterceptor, CONNECT 프레임 인증 |
| [ ] | **5C** | `RoomSessionMappingInterceptor` | sessionId ↔ roomId 매핑 |
| [ ] | **5D** | `RoomWebSocketController` (255L) | @MessageMapping, @SendTo, SimpMessagingTemplate |
| [ ] | **5E** | `RoomSessionService` 부분 1: 세션 추적 | ConcurrentHashMap, sessionId 관리 |
| [ ] | **5F** | `RoomSessionService` 부분 2: 브로드캐스트 | STOMP 메시지 발행 |
| [ ] | **5G** | `RoomSessionService` 부분 3: 정리 (disconnect 처리) | EventListener, SessionDisconnectEvent |
| [ ] | **5H** | WebRTC 개념 + OpenVidu 도입 | SFU/MCU, ICE/STUN/TURN, OpenVidu의 역할 |
| [ ] | **5I** | `OpenViduService` (313L) | 외부 SaaS 클라이언트, 세션·토큰 발급, 녹화 |
| [ ] | **5J** | `WebRtcRoomService` + 컨트롤러 | Room lifecycle ↔ OpenVidu 세션 연결 |
| [ ] | **5K** | RoomWebSocketController 종합 — 비즈니스 로직 식별 | **B2 정리 직전 매핑표 작성** |

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

전체 진행률: **2 / 64 완료** (0A, 1A)

## 사용 팁

- 한 자리에서 1~3개 단위씩 진행이 무난함. 너무 많이 몰아서 들으면 머리에 안 남음
- 각 단위 끝나면 그 단위에서 무엇을 배웠는지 짧게 자기 언어로 요약 (이게 학습에 가장 효과적)
- 모르는 개념이 나오면 그 자리에서 깊이 파고듦. "다음으로 넘어가자"는 사용자 의지가 명확할 때만 진행

---

**작성일**: 2026-04-29
**작성 맥락**: refactor.md Phase B(B1, B2) 진입을 위한 사전 코드 이해 작업
Z3 → Z2 → Z5 → Z7 → Z1 → Z6 → Z10 → Z4 → Z9 → Z8