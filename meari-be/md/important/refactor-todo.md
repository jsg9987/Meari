# 개선 또는 리팩터링 TODO list

## 4/29일
- [ ] ExceptionDto에 message 뿐만 아니라 code도 추가 고려
- [x] ApiResponse의 Fail 메서드 리팩터링: 하나로 합치기

---

# walkthrough 발견 사항 (2026-05-27~, 섹션 0~4 진행 중)

> code-walkthrough 진행하며 발견한 구체적 개선 항목. 상위 계획은 `refactor.md`(Phase A~D).
> **원칙: 코드 전체 독해 + 행동 보존 테스트 확보 후 착수.**

## ✅ 이미 적용 (커밋됨, refactor/domain-code)
- /auth `permitAll` 낱개화(logout·delete는 인증 필요), 죽은 `members/login` 설정·chat-test 주석 제거
- CORS 허용 출처 yml 외부화(@ConfigurationProperties)
- 잔재 파일 `nul` 삭제 / 환경 의존 통합테스트 `@Disabled` 해제(격리는 미적용)

## 인증·보안 (섹션 1) — refactor.md A/B
- [ ] **개선 B (인증 단일 진실원화)**: `JwtAuthenticationFilter`를 "토큰 없으면 통과"로 + `AuthenticationEntryPoint`/`AccessDeniedHandler` 커스텀(ApiResponse 401/403) → 필터 스킵목록 제거, SecurityConfig가 인가 단일 진실원. 행동 변경(테스트 4개 재작성·에러포맷 변경·프론트 영향) → A3 후
- [ ] 🔴 **AWS/Gemini/OpenAI/Anthropic API 키 평문 하드코딩** → 키 폐기+환경변수+히스토리 정리 (A1, 긴급)
- [ ] 비밀번호 패턴 검증 `@Pattern` (`SignupRequestDto`/`PasswordUpdateRequestDto`) (A2)
- [ ] `JwtUtil` 멀티스레드 lazy init → `@PostConstruct` 1회 초기화 (A2)
- [ ] Auth/Member 경계: 회원 엔드포인트를 `MemberController`로 이전

## Room 도메인 (섹션 4) — refactor.md B1
### ★ 저장소/상태 재설계 (4-0~4-1 식별 — 기준: 영속성/동시성/빈도)
- [ ] **GamePhase Redis → DB(Room 컬럼) 이동 검토**: 전이 드묾(게임당 ~4회)·저빈도 read·Room과 함께 조회 → Redis 이점 작음. RoomStatus와 모아 상태 이원화 해소
- [ ] content 선택값도 DB(Room) 가능성 검토
- [ ] Redis는 **실시간 협상 전용**으로: 역할 선점(동시성)·ready·접속자·세션매핑
- [ ] **MemberRoom 제거 검토(의견: 제거 쪽)**: 이력 안 남김(퇴장 시 삭제)+실시간 진실은 Redis(이중관리)+참여 이력은 ShadowingReport 보유. 제거 시 방장위임(입장순서)·정원·중복입장을 Redis로 이전, `WebRtcRoomService` 의존 정리 필요 → B1과 묶어 결정

### RoomService(1316L) 분해 (B1)
- [ ] 모듈화: RoomQuery / RoomCommand / RoomPhase / RoomBroadcast Service. **Facade 보류**(과설계). 조합은 RoomPhaseService가 broadcast·analysis 주입해 조율
- [ ] 트랜잭션 경계는 진입 메서드 1곳, 조회는 readOnly
- [ ] **외부 I/O(브로드캐스트·RabbitMQ·Redis)는 커밋 후**(@TransactionalEventListener AFTER_COMMIT) — DB 롤백 시 불일치 방지
- [ ] createRoom/createQuickRoom 중복(80%) → 공통 추출
- [ ] **getRoomList 좀비방 정리(조회의 쓰기 부작용)** → 스케줄러로 분리. ★ 잠재 버그: readOnly 트랜잭션이라 `room.updateStatus` dirty checking 미반영 가능 → 검증 필요
- [ ] getRoomList N+1(방마다 Redis/content) → 배치 (C1)
- [ ] 조회/명령 분리(CQRS)
- [ ] **leave/disconnect/kick 3개 중복**(멤버 제거+방장위임+방종료체크+브로드캐스트) → 공통 메서드 추출
- [ ] **입장 정원 체크 race condition**(count→save 무락; 동시 마지막자리 입장) + **인원 세는 기준 불일치**(enterRoom은 DB count, getRoomList는 Redis)
- [ ] DB·Redis 보정 코드(enterRoom 잔존 데이터 삭제, leaveRoom null 방어) — 이중 관리 비용, 단일 진실원화로 해소

## WebSocket (섹션 5, B2)
- [ ] `RoomWebSocketController` 비즈니스 로직(toggleReady/selectRole/releaseRole 세션조작+브로드캐스트) → Service로
- [ ] `handleWebSocketDisconnect` Redis 정리 순서 로직 → Service로
- [ ] `/user/queue/errors` 개인 에러 큐 구현 (TODO 2곳)

## 엔티티 컨벤션
- [ ] `@AllArgsConstructor` 사용 엔티티(Content/Role/Sentence/Theme 등) → 명시적 `@Builder` 생성자 통일. **전체 동시 + 테스트 id 부여를 `ReflectionTestUtils.setField`로** (3개만 바꾸면 불일치 — 이번 세션 롤백함)

## 테스트 인프라 (refactor.md A3/C2)
- [ ] 환경 의존 통합테스트 격리: `application-test.yml` + H2/Testcontainers + 외부 `@MockBean` (현재 @Disabled만 해제, 격리 미적용)
- [ ] `SoloPracticeControllerTest`: Security 우회(`addFilters=false`) 후 재활성화
- [ ] `RoomServiceTest`/`RoomServiceWebSocketTest`: B1 분해와 함께 재작성
- [ ] `CsvScriptParserTest`: 변경된 검증 로직에 맞춰 재작성