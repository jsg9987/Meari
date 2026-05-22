# Zone 2 — Room 동시성 / 상태 머신 / 스케줄러 (BE) 실행 계획

> 마스터 플랜의 Z2 상세본. 여러 사용자/스레드가 동시에 같은 방을 만질 때 어디서 race가 터지는가를 직접 재현하고 수정.

## 목표

다음 6개 명제를 면접에서 1인칭으로 답할 수 있게 만들기:

1. 같은 멤버가 두 클라이언트에서 마지막 문장을 동시 전송하면 **분석이 2번 호출**되는 race를 직접 재현. Redis SETNX 또는 DB 멱등성 제약으로 해결
2. `tryBroadcastRecordingsComplete`의 "체크 → 마킹" 2단계 사이 race로 **`RECORDINGS_COMPLETE` 메시지가 두 번 브로드캐스트** 가능. `RoomTimeoutScheduler`와 정상 경로가 동시 발동
3. 방장이 나가는 동시에 다른 멤버가 역할 선택 → DB-Redis 정합성 일시 깨짐
4. `RoomTimeoutScheduler`의 **5초 fixedRate** 때문에 정확한 타임아웃 시점이 **±5초 편차**. 사용자가 본 화면 종료 시점이 들쭉날쭉
5. `RoomService:850`의 `40000L` 영상 버퍼 타임이 **하드코딩** — 환경별 조정 불가
6. `WebSocketEventListener`의 `ScheduledExecutorService` **종료 로직 부재** → 장시간 운영 시 스레드 누수

추가:
7. `RoomWebSocketController` TODO `/queue/errors` 미구현 — 한 명의 에러가 전체 방에 브로드캐스트 (UX 버그). B2 리팩터와 일부 묶임

## 현재 코드 상태 (Pre-Z2 베이스라인)

### 핵심 위치

| 파일 | 라인 | 책임 | 위험 |
|---|---|---|---|
| `RoomService.java` | 911~954 | `recordingComplete()` `@Transactional` | 939 line `isMemberRecordingsComplete` + 944 `requestMemberAnalysis` 비원자 |
| `RoomService.java` | 982~997 | `tryBroadcastRecordingsComplete()` | 986 `isRoundCompleted` + 994 `markRoundCompleted` 사이 race |
| `RoomService.java` | 348~389 | `leaveRoom()` `@Transactional` | 372 owner 체크 + 373 `handleOwnerLeave` |
| `RoomService.java` | 850 | `timeoutMillis = playStartTime + (videoDuration*1000) + 40000L` | 매직 넘버 |
| `RoomService.java` | 1233~1248 | `checkAndHandleRecordingTimeout()` 동기 체크 | 정상 경로에서 호출 |
| `RoomTimeoutScheduler.java` | 29 | `@Scheduled(fixedRate = 5000)` | 5초 주기 polling — 편차 ±5초 |
| `RoomTimeoutScheduler.java` | 80~91 | `currentTime > timeoutMillis` 체크 후 `markRoundCompleted` + 브로드캐스트 | 정상 경로(`tryBroadcastRecordingsComplete`)와 동시 발동 가능 |
| `WebSocketEventListener.java` | 34 | `Executors.newScheduledThreadPool(2)` | `@PreDestroy` 없음 |
| `WebSocketEventListener.java` | 36, 66 | `GRACE_PERIOD_SECONDS = 30`, `scheduler.schedule(...)` | 30초 후 자동 퇴장 시도 |
| `RoomWebSocketController.java` | 80, 113 | TODO `/queue/errors` | 미구현 |

### 핵심 의존 — `RoomSessionService` (Redis 추상화)

다음 메서드의 **원자성**이 race condition 분석의 핵심:
- `isMemberRecordingsComplete(roomId, round, memberId)` — Redis SET 카운트 확인
- `markRecordingComplete(roomId, round, memberId, sentenceId)` — SET에 add (atomic)
- `isRoundCompleted(roomId, round)` — STRING 존재 확인
- `markRoundCompleted(roomId, round)` — **GET-then-SET이면 race, SETNX이면 OK**
- `isAllRecordingsComplete`, `isAllWatchingComplete` — 카운트 비교

→ Z2 분석 시 가장 먼저 확인할 것: **`markRoundCompleted` 구현이 SETNX인가?** 답에 따라 SC2의 시나리오가 다름.

---

## Pre-flight 체크리스트 (시작 전 환경 준비)

- [ ] 로컬 docker-compose 띄움 (PG, Redis, RabbitMQ, BE)
- [ ] 브라우저 탭 4개 띄울 수 있는 멀티 계정 시나리오 (회원 4명)
- [ ] Redis CLI 또는 RedisInsight (`redis-cli` `keys "room:*"`)
- [ ] DB 도구 (room, member_room 테이블 즉시 조회 가능)
- [ ] BE 로그 실시간 확인 (IDE 콘솔)
- [ ] `jstack`/JVisualVM 사용 가능 (Z2-SC6 스레드 누수)
- [ ] (선택) k6 또는 같은 메서드를 빠르게 N회 호출하는 짧은 스크립트
- [ ] `RoomSessionService`의 `markRoundCompleted` 등 핵심 메서드 구현 직접 확인 (SETNX vs GET-SET 결정)

---

## 시나리오 — 직접 수행 순서

각 시나리오마다 결과를 `meari-be/md/_room_concurrency_ts_raw.md`에 누적 (Z3의 `_rabbitmq_ts_raw.md` 패턴).

### SC1. 베이스라인 — 정상 라운드 1회 완주
- [ ] Hypothesis: 정상 환경에서 4명 방이 영상 보고 녹음 완료까지 race 없이 끝남
- Setup: 4명 회원 + 방 1개 + 짧은 영상 콘텐츠(30초 권장)
- Execute:
  1. 4명 입장 → 역할 선택 → 라운드 시작
  2. 영상 끝나고 4명 모두 녹음 완료 메시지 발송
  3. 분석 요청 4번 발생 확인 (BE 로그)
  4. `RECORDINGS_COMPLETE` 메시지 1번 브로드캐스트 확인
  5. DB: `shadowing_report` 4행, Redis: round completed 마킹
- Observe: 정상 흐름의 시퀀스 다이어그램 작성
- Conclude: 베이스라인 확보

### SC2. 분석 중복 호출 race 재현
- [ ] Hypothesis: 한 멤버 X의 마지막 문장 메시지를 두 클라이언트에서 거의 동시에 보내면 `analysisService.requestMemberAnalysis()`가 **2번 호출**됨
- Setup: SC1 끝난 상태에서 새 라운드 진행 중. 멤버 X의 모든 문장 중 마지막 1개만 남은 상태
- Execute:
  1. X의 클라이언트를 두 탭으로 동시 로그인
  2. 두 탭에서 동일한 마지막 문장 RECORDING_COMPLETE 메시지를 거의 동시에 발송 (수동 클릭 또는 짧은 스크립트)
  3. BE 로그에서 `analysisService.requestMemberAnalysis(...) 호출` 횟수 확인
  4. RabbitMQ UI: `analysis.requests` 큐에 메시지 2개 들어왔는지 확인
  5. DB: `shadowing_report` 행 1개 vs 2개 (Z3와 결합)
- Observe: race 재현 빈도, 발생 조건
- Conclude: 동시성 결함 재현 — Z3의 멱등성 부재와 결합된 영향 정량화

### SC3. 브로드캐스트 중복 race
- [ ] Hypothesis: `tryBroadcastRecordingsComplete()`의 `isRoundCompleted` 체크 + `markRoundCompleted` 사이에 `RoomTimeoutScheduler`가 깨어나면 두 호출자가 각자 브로드캐스트
- Setup: SC1 환경. 영상 끝나기 5초 전부터 마지막 멤버 한 명이 응답 안 하게 함
- Execute:
  1. 정상 멤버 3명은 녹음 완료
  2. 마지막 멤버 1명 의도적으로 누락 → 타임아웃 대기
  3. 타임아웃 직전(±2초)에 마지막 멤버가 RECORDING_COMPLETE 메시지 보냄
  4. `RoomTimeoutScheduler` 5초 주기와 정상 경로가 거의 동시 발동
  5. `RECORDINGS_COMPLETE` 메시지가 2번 브로드캐스트되는지 BE 로그 + 클라이언트 수신 측에서 확인
- Observe: 메시지 발생 횟수, 클라이언트가 받는 영향
- Conclude: 두 경로의 동시 발동을 시각적으로 확인

### SC4. 방장 위임 + Redis 정합성 race
- [ ] Hypothesis: 방장이 나가는 트랜잭션과 다른 멤버의 역할 변경이 동시에 일어나면 짧은 시간 동안 Redis와 DB의 owner가 다를 수 있음
- Setup: WAITING 상태 방, 멤버 4명
- Execute:
  1. 방장 A의 leaveRoom 트랜잭션 시작 (의도적으로 디버거 또는 sleep으로 지연)
  2. 트랜잭션 진행 중에 다른 멤버 B가 역할 선택 요청
  3. Redis 상태(owner, roles) vs DB 상태 비교 — 일시적으로 불일치 발견되는지
  4. 트랜잭션 종료 후 최종 일관성 확인
- Observe: 일시 불일치의 노출 윈도우, 사용자에게 영향 있는 윈도우인지
- Conclude: 트랜잭션-Redis 결합의 한계 — 진짜 문제인지 이론적 위험인지 판단

### SC5. 타임아웃 정확도 ±5초 편차
- [ ] Hypothesis: 사용자가 본 라운드 종료 시점이 정확히 `playStartTime + videoDuration + 40s`가 아니라 **0~5초 더 늦음** (스케줄러 주기 때문)
- Setup: 짧은 영상(10초) + 마지막 멤버가 응답 안 함
- Execute:
  1. 라운드 시작 시간 기록 (T0)
  2. 정확한 expected timeout = T0 + 2 + 10 + 40 = T0 + 52초
  3. 실제 클라이언트가 `RECORDINGS_COMPLETE` 받는 시점 측정
  4. 5번 반복하여 편차 분포 확인 (52, 53, 54, 56, 55 같은 분포 예상)
- Observe: 편차 통계
- Conclude: 정량적 편차 데이터 → 5초 주기를 1초로 줄이거나 이벤트 기반 타임아웃으로 전환의 근거

### SC6. ScheduledExecutorService 스레드 누수
- [ ] Hypothesis: `WebSocketEventListener`의 `Executors.newScheduledThreadPool(2)`이 application context 종료 시 정리 안 됨 → JVM은 종료되지만 정상 shutdown 안 됨. 또한 reload 환경(devtools restart)에서 누적
- Setup: BE를 IDE devtools(자동 reload) 환경에서 실행
- Execute:
  1. `jcmd <pid> Thread.print | grep -i "scheduled"` 또는 VisualVM Threads 탭으로 베이스라인 측정
  2. 코드 변경 → devtools가 context 재로드
  3. 다시 스레드 카운트 측정 — 증가했는지
  4. 5번 반복 후 누수량 측정
- Observe: 스레드 카운트 추이 그래프
- Conclude: `@PreDestroy`로 `scheduler.shutdown()` 추가 필요성 증명

### SC7. /queue/errors 미구현으로 인한 UX 버그
- [ ] Hypothesis: 멤버 X가 역할 선택 시 에러 발생 → 현재는 전체 `/topic/room/{roomId}/state`로 에러 브로드캐스트 → 다른 3명 화면에도 X의 에러가 노출
- Setup: 4명 방, 역할 선택 단계
- Execute:
  1. 멤버 X가 의도적으로 잘못된 roleId로 역할 선택 시도
  2. 다른 3명 화면에 에러 메시지 떠오르는지 확인
  3. 또는 현재는 어떻게 처리되는지 코드 추적 (`RoomWebSocketController.java:80, 113` TODO 부근)
- Observe: 사용자 노출 흐름
- Conclude: 개인 에러 큐 도입 필요성 — B2 리팩터와 묶음

---

## 발견 후 수정 계획 (PR 분할)

각 PR은 별도 브랜치. 이미 Z3에서 다루는 항목은 중복 회피.

### PR1: 분석 중복 호출 멱등성
- 옵션 A: `RoomSessionService`에 `tryMarkAnalysisRequested(roomId, round, memberId)` SETNX 메서드 추가, true일 때만 `analysisService.requestMemberAnalysis(...)` 호출
- 옵션 B: Z3 PR3의 ShadowingReport DB UNIQUE 제약 + UPSERT (이걸로도 사실상 멱등)
- 권장: **A + B 둘 다** — Redis 단에서 막고, DB에서도 안전망
- 검증: SC2 재현 안 됨, BE 로그에 `requestMemberAnalysis` 1회만

### PR2: 브로드캐스트 원자성 — `markRoundCompleted` SETNX 보장
- `RoomSessionService.markRoundCompleted` 내부 구현이 GET-SET이면 SETNX로 변경
- `tryBroadcastRecordingsComplete()` 코드를 다음 패턴으로:
  ```java
  if (!roomSessionService.tryMarkRoundCompleted(roomId, round)) {
      return; // 이미 다른 호출자가 마킹함
  }
  // 브로드캐스트
  ```
- `RoomTimeoutScheduler.java:84` 도 같은 패턴 적용
- 검증: SC3 재현 안 됨, `RECORDINGS_COMPLETE` 메시지 1회만

### PR3: 타임아웃 정확도 — 이벤트 기반 또는 주기 단축
- 옵션 A: `@Scheduled(fixedRate = 1000)` (1초)로 단축 — 단순
- 옵션 B: 이벤트 기반 — Redis ZADD에 timeoutMillis 저장하고 가장 빠른 expire 시간에 `scheduler.schedule(...)` (정밀)
- 옵션 C: ROUND_START 시 Spring `TaskScheduler.schedule(runnable, Date(timeoutMillis))` (가장 정밀)
- 권장: **C** — 사용자가 본 시점과 코드 시점이 일치
- 검증: SC5 편차 < 1초

### PR4: `40000L` 외부화
- `application.yml`:
  ```yaml
  room:
    recording-timeout-buffer-ms: 40000
  ```
- `RoomService`에 `@Value("${room.recording-timeout-buffer-ms}") private long recordingTimeoutBufferMs;`
- 검증: 환경별로 yaml 변경만으로 조정 가능

### PR5: `WebSocketEventListener` 스레드 정리
- 변경:
  ```java
  @PreDestroy
  public void shutdown() {
      scheduler.shutdown();
      try {
          if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
              scheduler.shutdownNow();
          }
      } catch (InterruptedException e) {
          scheduler.shutdownNow();
          Thread.currentThread().interrupt();
      }
  }
  ```
- 또는 Spring의 `ThreadPoolTaskScheduler` 빈으로 교체 (`@Bean` + `@RequiredArgsConstructor`로 주입)
- 검증: SC6 재현 안 됨, devtools reload 시 스레드 정리됨

### PR6: 방장 위임 트랜잭션 경계 명확화
- `handleOwnerLeave(room)` 내부 작업이 DB 변경 후 Redis 변경 순서인지 확인
- 옵션: `leaveRoom()` 트랜잭션 종료 후(After Commit) Redis 동기화 — `TransactionSynchronizationManager.registerSynchronization(...)`
- 검증: SC4의 일시 불일치 윈도우 0 또는 사용자 영향 없음

### PR7: /queue/errors 개인 에러 큐 (B2와 통합)
- `RoomWebSocketController.java:80, 113` TODO 해소
- 사용자별 `/user/queue/errors` 구독으로 전환 — `SimpMessagingTemplate.convertAndSendToUser(memberId.toString(), "/queue/errors", error)`
- B2 RoomWebSocketController 정리 시 함께 진행 권장
- 검증: SC7 재현 안 됨, 다른 멤버 화면 영향 없음

---

## 측정 지표 표 (Before/After 채우기)

| 지표 | Before | After (목표) |
|---|---|---|
| 분석 중복 호출 (SC2) | (측정) | 0 |
| 브로드캐스트 중복 (SC3) | (측정) | 0 |
| 방장 위임 일시 불일치 윈도우 | (SC4 측정 ms) | 0 또는 트랜잭션 commit 이후만 |
| 타임아웃 편차 (SC5) | ±5초 | ±1초 이하 |
| 1시간 운영 후 누수 스레드 (SC6) | (측정) | 0 |
| 한 멤버 에러의 다른 멤버 화면 노출 (SC7) | (Yes/No) | No |

---

## 정리 산출물

- `meari-be/md/_room_concurrency_ts_raw.md` — TS-1~ 시나리오별 raw 노트 누적
- `meari-be/md/portfolio_features.md` 신규 섹션 또는 #3/#6/#10 보강:
  - "Race condition 직접 재현 후 Redis SETNX + DB 멱등성 이중 안전망"
  - "스케줄러 정확도 개선 — 5초 주기에서 이벤트 기반으로"
  - "Spring 빈 lifecycle — `@PreDestroy`로 스레드 누수 차단"

---

## 진행 체크리스트

- [ ] Pre-flight 환경 준비
- [ ] `RoomSessionService.markRoundCompleted` 구현 확인 (SETNX vs GET-SET)
- [ ] SC1 베이스라인
- [ ] SC2 분석 중복 호출 race
- [ ] SC3 브로드캐스트 중복 race
- [ ] SC4 방장 위임 정합성
- [ ] SC5 타임아웃 정확도
- [ ] SC6 스레드 누수
- [ ] SC7 /queue/errors UX
- [ ] PR1 분석 멱등성 (Redis SETNX)
- [ ] PR2 markRoundCompleted SETNX
- [ ] PR3 타임아웃 이벤트 기반
- [ ] PR4 40000L 외부화
- [ ] PR5 @PreDestroy
- [ ] PR6 방장 위임 commit 후 Redis 동기화
- [ ] PR7 /queue/errors (B2 통합)
- [ ] 측정 지표 표 채우기
- [ ] portfolio_features.md 갱신

---

**작성일**: 2026-04-29
**다음 액션**: Pre-flight + `RoomSessionService.markRoundCompleted` 구현 확인 → SC1 베이스라인
