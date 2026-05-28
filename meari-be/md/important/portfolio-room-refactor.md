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
2. **저장소 경계 평가**: Redis(`RoomSessionService`) 키를 영속성·동시성·빈도로 분류. 동시성 제어가 필요한 데이터(역할 선점 HSETNX, 분석요청 SETNX)는 Redis가 정당, 단순 상태(GamePhase·content)는 Room 엔티티가 더 적합.
3. **트랜잭션·동시성 검토**: `recordingComplete`의 race condition, `getRoomList` readOnly+dirty checking 미반영 등 잠재 버그 식별.

## 학습한 개념

- **단일 책임 원칙(SRP)**과 응집도(cohesion)
- **CQRS** (Query/Command 분리) — 조회는 `@Transactional(readOnly=true)`, 명령은 쓰기 트랜잭션
- **도메인 모델 패턴(Domain Model Pattern)** — 검증·규칙을 Service가 아닌 Entity 안에
- **Spring 트랜잭션 전파**: `REQUIRES_NEW`로 readOnly 안에서 쓰기 분리, `@TransactionalEventListener(AFTER_COMMIT)`로 외부 I/O 분리
- **Hibernate Dirty Checking과 readOnly의 관계** — readOnly에서는 flush 안 됨
- **Extract Class / Extract Method** 리팩터링과 그 한계 (라인 감소 ≠ 클래스 분리)
- **God Service / God Object 안티패턴**과 그 재발 방지(Facade 보류)

## 수정 (After)

### 1) 책임별 클래스 분리
| 새 컴포넌트 | 책임 |
|---|---|
| `RoomBroadcastService` | STOMP 송신 전담 (`messagingTemplate` 12개 호출 일원화) |
| `RoomQueryService` | 조회 전용 (`@Transactional(readOnly=true)`) |
| `RoomCleanupService` | 좀비방 종료 (`@Transactional(propagation=REQUIRES_NEW)`) |

### 2) 도메인 메서드 강화 (Entity 내부로 규칙 이동)
`Room` 엔티티에 추가:
- `checkOwnerOrThrow(memberId)`
- `checkWaitingOrThrow()`
- `checkInProgressOrThrow()`

→ Service 검증부가 `if (...) throw ...` 6줄 → `room.checkXxxOrThrow()` 2줄. **7개 phase 메서드에서 재사용.**

### 3) 부수 버그 수정
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

### 정량 지표
- RoomService: 1316 LOC → ~1100 LOC (조회·브로드캐스트 제거)
- `messagingTemplate.convertAndSend` 호출: 12곳 → 1곳 (`RoomBroadcastService`)
- 검증 코드 중복: 8곳 이상 → Room 도메인 메서드 3개로 일원화
- 잠재 버그 1건 해결 (getRoomList readOnly + dirty checking)

## 작성 시 한 줄 모범 표현 (예시)

> "1316 LOC의 God Service였던 `RoomService`를 책임별로 분리(`Broadcast`/`Query`/`Cleanup`)하고, 반복되던 검증 로직을 `Room` 엔티티의 도메인 메서드로 응집시켜 **CQRS와 도메인 모델 패턴을 준수하는 구조**로 리팩터링. 부수적으로 `@Transactional(readOnly)` 안에서 dirty checking이 동작하지 않아 좀비방이 닫히지 않던 잠재 버그를 `REQUIRES_NEW` 분리로 해결."
