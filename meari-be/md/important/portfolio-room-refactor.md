# Room 도메인 리팩터링 — 포트폴리오 정리 자료

## 문제 (Before)

`RoomService` 1316 LOC 단일 클래스가 다음 6가지 책임을 모두 떠안고 있었다:
- 방 생성·조회 (CRUD)
- 입퇴장·강퇴·방장 위임
- 게임 진행 상태 전이 (WATCHING → ROLE_PICK → ROUND_1/2)
- STOMP 브로드캐스트 (`messagingTemplate` 직접 호출 12곳에 분산)
- 분석 요청 트리거
- 검증 로직 (방장 권한·방 상태 등) 중복 8~10곳

의존성 11개 주입, 트랜잭션 경계와 readOnly가 메서드마다 일관되지 않음.

### 발견한 구체적 문제
1. **`getRoomList`의 잠재 버그**: 클래스 레벨 `@Transactional(readOnly=true)` 안에서 `room.updateStatus(COMPLETED)`를 시도. Hibernate readOnly에서는 Dirty Checking flush가 동작하지 않아 status가 DB에 반영되지 않음 → 좀비방이 영영 목록에 남음.
2. **외부 I/O가 트랜잭션 안**: STOMP 브로드캐스트가 `@Transactional` 안에서 호출되어, DB 롤백 시 "DB는 없는데 클라이언트엔 메시지가 나간" 불일치 가능.
3. **검증 코드 중복**: `if (!room.getOwner().getMemberId().equals(memberId)) throw ...` 같은 두 줄짜리가 8곳 이상 반복.

## 분석

1. **메서드별 책임 매핑표 작성**: 각 메서드가 ① DB ② Redis ③ 브로드캐스트 ④ 분석 트리거 중 무엇을 건드리는지 표로 정리 → 5가지 책임 그룹이 한 클래스에 섞여 있음을 가시화.
2. **저장소 경계 평가**: Redis(`RoomSessionService`) 키를 영속성·동시성·빈도로 분류. 동시성 제어가 필요한 데이터(역할 선점 HSETNX, 분석요청 SETNX)는 Redis가 정당.
3. **트랜잭션·동시성 검토**: `recordingComplete`의 race condition, `getRoomList` readOnly+dirty checking 미반영 등 잠재 버그 식별.

### 의사결정 시행착오 (포트폴리오 가치 — "왜 안 한 것"도 결과의 일부)

- **저장소 재설계 시도 후 되돌림**: GamePhase·content를 Redis→DB(Room 컬럼) 이전 시도. 진행하며 `RoomTimeoutScheduler`가 `redisTemplate.keys("room:*:phase")` 패턴으로 Redis를 스캔하는 등 **Redis 전제 코드가 광범위**함을 발견 → 침습이 커서 동작 보존 어려움. **현재 저장소 경계 유지**가 더 안전하다고 판단해 롤백. 책임 분리(아래)에 집중.
- **MemberRoom 제거 보류**: 참여자 진실은 Redis라 DB 중복 의심됐으나, 방장 위임의 입장 순서가 `MemberRoom.createdAt`에 의존 → Redis Sorted Set 이전이 추가 작업이라 분해 우선으로 보류.
- **Facade 도입 검토 후 회피**: 분리된 module 위에 `RoomService` Facade 층을 더 두려다가, **1:1 위임 메서드만 늘면 의미 없는 "투명한 Facade"** 안티패턴 → 각 module(Query/Phase/Command) 자체가 이미 작은 Facade 역할이고 Controller가 직접 호출하는 게 더 단순하다고 결론. **2층 layer 회피.**
- **Extract Method의 한계 자가비판**: 검증 묶음을 `validateGameStart` private helper로 추출했다가, 1회용·재사용 0이라는 사실을 인지하고 **Room 엔티티의 도메인 메서드(`checkOwnerOrThrow` 등)로 옮김** — 실제 라인 감소(6줄 → 2줄) + 7곳에서 재사용.

## 학습한 개념

- **단일 책임 원칙(SRP)**과 응집도(cohesion)
- **CQRS** (Query/Command 분리) — 조회는 `@Transactional(readOnly=true)`, 명령은 쓰기 트랜잭션
- **도메인 모델 패턴(Domain Model Pattern)** — 검증·규칙을 Service가 아닌 Entity 안에
- **Spring 트랜잭션 전파**: `REQUIRES_NEW`로 readOnly 안에서 쓰기 분리, `@TransactionalEventListener(AFTER_COMMIT)`로 외부 I/O 분리
- **Hibernate Dirty Checking과 readOnly의 관계** — readOnly에서는 flush 안 됨
- **Extract Class / Extract Method** 리팩터링과 그 한계 (라인 감소 ≠ 클래스 분리)
- **God Service / God Object 안티패턴**과 그 재발 방지(Facade 보류)

## 수정 (After)

### 1) 책임별 클래스 분리 (외부 호출자는 module 직접 주입·호출)
| 새 컴포넌트 | 책임 |
|---|---|
| `RoomBroadcastService` | STOMP 송신 전담 (`messagingTemplate` 12개 호출 일원화) |
| `RoomQueryService` | 조회 전용 (`@Transactional(readOnly=true)`) — `RoomController`가 직접 호출 |
| `RoomCleanupService` | 좀비방 종료 (`@Transactional(propagation=REQUIRES_NEW)`) |
| (예정) `RoomPhaseService`, `RoomCommandService` | 게임 진행 / 생성·입퇴장 |

→ 외부(Controller·Scheduler·WSController)가 필요한 module을 직접 주입. **추가 Facade 층 없음.**

### 2) 도메인 메서드 강화 (Entity 내부로 규칙 이동)
`Room` 엔티티에 추가:
- `checkOwnerOrThrow(memberId)` / `checkWaitingOrThrow()` / `checkInProgressOrThrow()`

→ Service 검증부 `if (...) throw ...` 6줄 → `room.checkXxxOrThrow()` 2줄. **7곳에서 재사용.**

### 3) 중복 추출
- `createRoom` / `createQuickRoom` 80% 중복 → `createRoomWithOwner(...)` 공통 메서드 (Room 생성 + 방장 자동 입장 + Redis 세션 등록)
- `leaveRoom` / `handleAbnormalDisconnect`의 "마지막 사람 퇴장 시 방 종료" 처리 → `closeRoomIfEmpty(...)` helper (2곳 재사용)

### 4) 의미 단위 helper 추출 (긴 메서드 가독성)
- `startRound`: `validateRoundStartPhase`, `initializeRoundSessionState`로 흐름 단순화 (70줄 → 30줄)

### 5) 부수 버그 수정
- `getRoomList`의 readOnly + dirty checking 미반영 → `RoomCleanupService.closeAsCompleted()` `REQUIRES_NEW` 트랜잭션으로 분리. 좀비방 status가 실제로 DB 반영.

## 결과 (성과 표현 후보)

각 항목에 쓸 만한 표현들 — 글 쓸 때 골라서 조합:

### 코드 품질
- 응집도(cohesion)가 높은 / 단일 책임 원칙(SRP)을 만족하는
- 책임이 명확히 분리된 / 관심사 분리(separation of concerns)
- 도메인 모델 패턴 적용 / 도메인 로직이 엔티티에 응집된
- 재사용 가능한 도메인 메서드 / 중복 제거 (DRY)
- CQRS의 Query/Command가 분리된

### 안전성
- 트랜잭션 경계가 명확한 / readOnly와 쓰기 트랜잭션이 분리된
- Dirty Checking이 정상 동작하는 안전한 상태로 / 데이터 일관성을 보장하는
- 동시성 제어가 정당한 위치에 배치된 (Redis SETNX·HSETNX 유지)

### 유지보수성
- 변경 영향이 격리된 / 한 책임 수정이 다른 책임에 새지 않는
- 테스트 단위가 작아진 (의존성 축소)
- "긴 코드"가 아니라 "작은 협력 객체들"로 구성된
- 추가 layer 없이 module을 외부 호출자가 직접 주입하는 단순한 구조 (2층 Facade 안티패턴 회피)
- 비판적 검토를 통해 과한 추출을 도메인 메서드로 대체 (재사용성 확보)

### 정량 지표 (진행 중 — Phase/Command 분리 미완료)
- RoomService: 1316 LOC → 진행 중 (조회·브로드캐스트·중복부 제거 완료, 게임 진행·입퇴장 분리 예정)
- `messagingTemplate.convertAndSend` 호출: 12곳 → 1곳 (`RoomBroadcastService`)
- 검증 코드 중복: 8곳 이상 → Room 도메인 메서드 3개로 일원화 (7곳 재사용)
- `createRoom`/`createQuickRoom` 중복: 80% → 공통 메서드 추출 (-33줄)
- `leaveRoom`/`disconnect` "마지막 퇴장" 중복: 2곳 → 1곳
- 잠재 버그 1건 해결 (`getRoomList` readOnly + dirty checking)

## 작성 시 한 줄 모범 표현 (예시)

> "1316 LOC의 God Service였던 `RoomService`를 책임별 module(`Broadcast`/`Query`/`Cleanup`/`Phase`/`Command`)로 분해하고, 반복되던 검증 로직을 `Room` 엔티티의 도메인 메서드로 응집시켜 **CQRS와 도메인 모델 패턴을 준수**하는 구조로 리팩터링. **module 위에 또 다른 Facade 층을 두는 안티패턴을 인지하고 회피**해 외부 호출자가 module을 직접 사용하는 단순 구조 채택. 부수적으로 `@Transactional(readOnly)` 안에서 dirty checking이 동작하지 않아 좀비방이 닫히지 않던 잠재 버그를 `REQUIRES_NEW` 분리로 해결."

## 진행 현황 메모 (포트폴리오엔 빼도 됨)

- ✅ Broadcast / Query / Cleanup 분리, 도메인 메서드 추가·적용, createRoom·startRound·leave 중복/긴 메서드 정리, Controller가 RoomQueryService 직접 호출
- ⏳ RoomPhaseService 분리, RoomCommandService 분리, RoomService 제거, 브로드캐스트 AFTER_COMMIT, WebSocket B2 (disconnect 이중 핸들러 + `/queue/errors`)
