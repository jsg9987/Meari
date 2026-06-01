# 포트폴리오 — 동시성: 녹음 완료 중복으로 인한 발음 분석 중복 요청 제거

> 키워드: Race Condition, Check-Then-Act, Redis SETNX, 원자성, Testcontainers 동시성 테스트
> 발견 경위: 실시간 방 도메인 리팩토링을 위해 코드를 점검하던 중 동시성 취약점을 발견하고, 재현 테스트로 입증 후 수정.

---

## 한 줄 요약
실시간 쉐도잉에서 마지막 문장 녹음완료 메시지가 동시에 중복 도착하면 **발음 분석(Wav2Vec2)이 멤버당 여러 번 요청**되던 race condition을, Redis SETNX 기반 원자 가드로 **정확히 1회**로 보장. 실제 Redis(Testcontainers) 동시성 테스트로 재현·검증.

---

## 배경 & 문제 상황

메아리는 4~6명이 한 방에서 영상 대사를 문장 단위로 따라 녹음하고, 한 멤버의 **모든 문장 녹음이 끝나면 발음 분석을 1회 요청**한다. 분석은 FastAPI에서 Wav2Vec2로 수행되는 **무거운 작업(문장당 수 초)**이다.

녹음 완료 처리(`RoomService.recordingComplete`)의 흐름:
```java
roomSessionService.markRecordingComplete(...);          // Redis Set(SADD)에 완료 문장 기록
if (roomSessionService.isMemberRecordingsComplete(...)) // 완료 문장수 == 예상 문장수 ?
    analysisService.requestMemberAnalysis(...);          // 분석 요청
```

**문제 1 — check-then-act 비원자성**
"완료됐는지 확인(isMemberRecordingsComplete)"과 "분석 요청(requestMemberAnalysis)"이 **분리된 두 연산**이다. 그 사이에 다른 스레드가 끼어들 수 있다. (Tomcat/WebSocket은 요청마다 스레드를 할당하고, `RoomService`는 싱글톤이라 동시 요청이 같은 Redis 상태를 공유한다.)

**문제 2 — Set 자료구조가 오히려 모든 중복을 통과시킴**
완료 문장은 Redis **Set**(`SADD`)으로 관리된다. Set은 중복 원소를 허용하지 않으므로 같은 마지막 문장이 N번 도착해도 크기는 그대로(예: 5). 그 결과 **모든 중복 메시지가 "5==5 완료"로 판정**되어 각자 분석을 요청한다.

**문제 3 — 중복 도착 경로가 실재**
마지막 문장 메시지는 (a) 같은 계정 다중 탭, (b) 네트워크 재전송(at-least-once), (c) 클라이언트 버그로 동시에 여러 번 도착할 수 있다.

**문제 4 — 비용**
중복 요청 = **비싼 Wav2Vec2 추론이 N배 낭비** + 분석 큐 중복 메시지 + 동일 `ShadowingReport` 행에 대한 중복 UPDATE.

---

## 해결 과정

**1. 재현 테스트 먼저 작성 (TDD, 실제 Redis)**
Mockito 단위 테스트로는 Redis의 원자성을 진짜로 검증할 수 없다고 판단해, **Testcontainers로 실제 Redis를 띄운 통합 테스트**를 작성했다.
- `phase=ROUND_1`, 멤버 1명, 총 5문장 중 1~4번 미리 완료 세팅
- `ExecutorService(10)` + `CountDownLatch`로 **마지막 5번 문장 녹음완료를 10개 스레드가 동시 발사**
- `requestMemberAnalysis` 호출 횟수를 검증

→ 수정 전 결과(재현 성공):
```
멤버 100 모든 녹음 완료, 분석 요청   ← 10개 스레드 전부 로그
Wanted 1 time: But was 10 times: analysisService.requestMemberAnalysis(1, 1, 100)
```
타이밍 운에 기대지 않고 **결정적으로 10회 재현**됨(Set 멱등성 때문에 모든 스레드가 완료로 판정).

**2. Redis SETNX 원자 가드 도입**
"분석 요청을 이미 보냈는가"를 **단일 원자 연산**으로 판정하는 메서드를 추가했다.
```java
public boolean tryMarkAnalysisRequested(Long roomId, Integer round, Long memberId) {
    String key = String.format(KEY_ANALYSIS_REQUESTED, roomId, round, memberId);
    Boolean firstRequest = redisTemplate.opsForValue()
            .setIfAbsent(key, "1", SESSION_TTL_HOURS, TimeUnit.HOURS); // SET NX
    return Boolean.TRUE.equals(firstRequest);
}
```
`SET ... NX`는 "키가 없을 때만 세팅하고 true 반환"을 **Redis 단일 명령**으로 처리한다. Redis는 명령을 단일 스레드로 순차 실행하므로, 동시 호출이라도 **첫 호출만 true**를 받는다.

호출부를 "완료 체크 + 원자 가드"로 결합:
```java
if (isMemberRecordingsComplete(...) && tryMarkAnalysisRequested(...)) {
    analysisService.requestMemberAnalysis(...);
}
```
방장이 라운드를 강제 종료하는 부분완료 경로(`requestPartialCompletionAnalysis`)에도 동일 가드를 적용해, **finishRound와 recordingComplete가 같은 멤버를 중복 요청**하는 경우까지 차단했다.

**3. 기존 패턴과의 일관성**
역할 선점(`tryAssignRole`)은 이미 `HSETNX`로 원자성을 보장하고 있었다. **같은 원자성 패턴을 분석 트리거에도 일관 적용**해, 일부만 동시성이 고려돼 있던 불일치를 정리했다.

**4. 상태 정리**
라운드 재진행/게임 종료 시 `analysis_requested` 플래그를 함께 정리(키 TTL 24h + 명시 삭제)해 재시작 시 정상 동작하도록 했다.

---

## 결과

1. **동시 10회 요청 → 분석 정확히 1회.** 같은 재현 테스트가 수정 후 GREEN(`times(1)`).
2. **무거운 Wav2Vec2 추론 중복 낭비 제거** (N배 → 1배), 분석 큐 중복 메시지 및 중복 DB UPDATE 제거.
3. **영구 회귀 가드 확보** — 실제 Redis 동시성 테스트가 CI에 남아 향후 회귀를 자동 감지.
4. 동시성 고려가 누락돼 있던 지점을 기존 원자성 패턴으로 통일.

---

## 배운 점 / 면접 답변 포인트

- **동시성 안전 = check-then-act를 하나의 원자 연산으로 합치는 것.** "읽고→판단→행동"이 분리되면 그 틈으로 race가 들어온다.
- **Redis 명령의 원자성**은 Redis가 단일 스레드로 명령을 순차 처리하기 때문이다. 개별 명령(SADD, SET NX, HSETNX)은 쪼개지지 않지만, **여러 명령의 조합은 원자적이지 않다.**
- **분산 환경에선 JVM `synchronized`/락이 무의미**(서버 여러 대). 그래서 Redis SETNX·DB UNIQUE 제약 같은 **외부 저장소 레벨의 원자성**이 정답.
- **자료구조 선택이 버그 양상을 바꾼다** — Set이라 count가 멱등이라 오히려 모든 중복이 "완료"로 통과했다. (List였다면 다른 양상)
- **동시성 버그는 Testcontainers + Executorの + CountDownLatch로 결정적으로 재현·검증**할 수 있다.

### 예상 꼬리 질문
- *"왜 DB UNIQUE 제약이 아니라 Redis SETNX?"* → 분석 결과 행(`ShadowingReport`)은 라운드 시작 시 미리 생성되므로 "중복 행"이 아니라 "중복 요청(추론)"이 문제다. 요청 단계에서 막아야 비싼 추론 낭비를 없앨 수 있어 Redis 가드가 1차 방어. 메시지 큐/AI 측 멱등(messageId 기반)을 2차 방어로 둘 수 있다.
- *"SETNX 키에 TTL을 왜?"* → 방 세션은 휘발성이라 영구 보존 불필요 + 키 누수 방지. 라운드 재진행 시엔 명시 삭제로 즉시 정합성 확보.
- *"이 버그를 운영 중 겪었나?"* → 아니다. 도메인 리팩토링을 위해 코드를 점검하던 중 발견했고, 재현 테스트로 실제 발생을 입증한 뒤 수정했다.

---

## 변경 파일
- `RoomSessionService` — `tryMarkAnalysisRequested()` 추가 + 라운드/게임 상태 정리에 플래그 제거
- `RoomService` — `recordingComplete`, `requestPartialCompletionAnalysis` 2곳에 가드 적용
- `RoomRecordingConcurrencyTest` (신규) — Testcontainers 실제 Redis 동시성 재현·검증 테스트
- `build.gradle` — Testcontainers 테스트 의존성 추가
