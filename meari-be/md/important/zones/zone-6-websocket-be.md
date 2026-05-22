# Zone 6 — WebSocket 실시간 통신 (BE + nginx, FE 제외) 실행 계획

> 마스터 플랜의 Z6 상세본. STOMP 메시지가 사용자에게 안전하게 도달하는가 — nginx 프록시 타임아웃, STOMP heartbeat, BE 인터셉터 + 이벤트 리스너 흐름 점검.

## 목표

다음 6개 명제를 면접에서 1인칭으로 답할 수 있게 만들기:

1. **nginx 60초 기본 타임아웃** — `/ws` location에서도 `proxy_read_timeout 60s` → 1분 idle 후 STOMP 끊김. **사용자가 화상 통화 중 갑자기 채팅 안 가는 UX 버그** 직접 재현
2. **STOMP heartbeat 미활성화** — `SimpleBroker`에 heartbeat 설정 없으면 nginx idle 타임아웃을 피할 수단이 없음. 활성화 후 끊김 회피
3. **`JwtChannelInterceptor` 인증 우회 시도** — 토큰 없이 CONNECT, 잘못된 토큰, 만료 토큰으로 시도 시 동작
4. **`RoomSessionMappingInterceptor` 책임 경계** — sessionId↔roomId 매핑이 어느 시점에 만들어지고 정리되는가
5. **`WebSocketEventListener` 예외 삼킴** — `handleGracePeriodExpired`에서 NOT_FOUND_MEMBER_ROOM 외 비즈니스 예외는 error 로그만 → 어떤 비즈니스 예외가 묻히는지 검증
6. **SockJS vs raw WebSocket 이중 등록** — `WebSocketConfig:39-45`에 `/ws` 엔드포인트가 SockJS와 raw 양쪽으로 등록 — 의도인지 실수인지 확인

## 현재 코드 상태 (Pre-Z6 베이스라인)

### 핵심 위치

| 파일 | 라인 | 책임 | 위험 |
|---|---|---|---|
| `meari-fe/nginx/nginx.conf` | 55~78 | `/ws` location의 proxy 설정 | `proxy_read_timeout 60s` (line 77) → 60초 idle 후 끊김 |
| `meari-fe/nginx/nginx.conf` | 58~59 | `Upgrade`, `Connection "upgrade"` 헤더 | ✅ WebSocket 핸드셰이크 자체는 OK |
| `WebSocketConfig.java` | 28 | `enableSimpleBroker("/topic", "/queue")` | heartbeat 미설정 |
| `WebSocketConfig.java` | 39~41 | `registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS()` | 모든 origin 허용 |
| `WebSocketConfig.java` | 44~45 | 같은 `/ws` 엔드포인트가 SockJS 없이도 등록 | 중복 등록 의문 |
| `WebSocketConfig.java` | 51~57 | inbound channel 인터셉터 — JWT → SessionMapping 순서 | 순서 의존 |
| `JwtChannelInterceptor.java` | (전체) | CONNECT 프레임에서 JWT 검증 | 인증 우회 가능 여부 검증 필요 |
| `RoomSessionMappingInterceptor.java` | (전체) | SUBSCRIBE/SEND 시 sessionId↔roomId 매핑 | 매핑 누락 가능성 |
| `WebSocketEventListener.java` | 82~93 | `handleGracePeriodExpired` try-catch | NOT_FOUND_MEMBER_ROOM 외 비즈니스 예외는 error 로그만 |
| `WebSocketEventListener.java` | 34 | `Executors.newScheduledThreadPool(2)` | (Z2 SC6 중복) 스레드 누수 |

### `/topic` `/queue` `/app` prefix
- `/topic/...` — 1:N 브로드캐스트 (방 상태 메시지 등)
- `/queue/...` — 1:1 (사용자별 — `RoomWebSocketController`의 `/queue/errors` TODO와 연결)
- `/app/...` — 클라이언트가 BE로 보낼 때 prefix

---

## Pre-flight 체크리스트

- [ ] WebSocket 클라이언트 도구 — Postman의 WebSocket / `wscat` / 브라우저 콘솔에서 `new WebSocket(...)` 또는 STOMP 클라이언트
- [ ] Chrome DevTools "Offline" / Throttling 사용 가능 (재연결 시뮬)
- [ ] 자체 STOMP 클라이언트 스크립트 (Node.js `@stomp/stompjs` 같은 거)
- [ ] BE 로그 + nginx 로그 동시 확인 (nginx access/error log 마운트되어 있나?)
- [ ] `application.yml`에 STOMP heartbeat 관련 설정 있는지 확인 — Spring 기본은 disabled
- [ ] 직접 nginx 설정 변경하고 재기동 가능 (`docker-compose restart nginx` 또는 reload)
- [ ] 인증 토큰 발급 흐름 가능 (Postman으로 로그인 후 AT 추출)

---

## 시나리오 — 직접 수행 순서

각 시나리오 결과를 `meari-be/md/_websocket_ts_raw.md`에 누적.

### SC1. 베이스라인 — 정상 STOMP 연결 + 메시지
- [ ] Hypothesis: 정상 환경에서 토큰 가진 사용자가 `/ws` 연결 → `/topic/room/{roomId}/state` 구독 → 메시지 수신
- Setup: BE 운영, 사용자 1명 로그인 후 AT 받음, 방 1개 입장
- Execute:
  1. 브라우저 콘솔 또는 Node 스크립트로 STOMP 클라이언트 연결:
     ```js
     const client = new StompJs.Client({
       brokerURL: 'ws://localhost/ws',
       connectHeaders: { Authorization: `Bearer ${AT}` }
     });
     ```
  2. 연결 성공 로그 확인 (BE 로그 + 클라이언트 콘솔)
  3. `/topic/room/${roomId}/state` 구독
  4. 다른 사용자가 같은 방에 입장 → MEMBER_JOIN 메시지 수신 확인
  5. 연결 시점부터 메시지 수신까지 latency 측정
- Observe: 정상 흐름 시퀀스
- Conclude: 베이스라인 latency, 메시지 수신 정상

### SC2. ⭐ nginx 60초 idle 끊김 재현
- [ ] Hypothesis: STOMP 클라이언트가 60초 이상 메시지 안 보내면 nginx가 idle로 판단해 connection 끊음 → 사용자에게 노출되는 모습 확인
- Setup: SC1 환경
- Execute:
  1. STOMP 클라이언트 연결 + 구독
  2. 65초 대기 (nginx default 60s를 넘김)
  3. 그 시점에 다른 사용자가 메시지 발생 → 연결 끊긴 클라이언트가 받는지
  4. 클라이언트 측 onDisconnect 콜백 호출 시점 측정
  5. nginx 로그에서 connection close 이벤트 확인
- Observe: 60초 ± 정도에서 끊김
- Conclude: 실측 데이터 → "사용자가 화상 통화 중 갑자기 채팅 안 가는" 시나리오 직접 재현

### SC3. STOMP heartbeat 미활성화 영향
- [ ] Hypothesis: SimpleBroker에 heartbeat 미설정이라 nginx idle을 피할 수단 없음. heartbeat 켜면 60초 이내에 ping/pong이 흘러서 끊김 회피
- Setup: SC2와 동일
- Execute:
  1. 현재 코드 그대로(`WebSocketConfig.java:28`) idle 65초 → 끊김 (SC2 결과)
  2. WebSocketConfig에 heartbeat 추가 (PR2 미리 적용):
     ```java
     registry.enableSimpleBroker("/topic", "/queue")
         .setHeartbeatValue(new long[]{10000, 10000})
         .setTaskScheduler(taskScheduler);
     ```
  3. 같은 65초 idle 시나리오 재현 → 끊기지 않는지
- Observe: heartbeat 활성 전후 비교
- Conclude: heartbeat의 효과 정량화

### SC4. JwtChannelInterceptor 인증 우회 시도
- [ ] Hypothesis: 다음 케이스에서 CONNECT 거부되어야 함
  - 토큰 없음 → 거부
  - 잘못된 서명 토큰 → 거부
  - 만료 토큰 → 거부
- Setup: BE 운영
- Execute:
  1. `connectHeaders` 없이 `/ws` 연결 시도 → 응답
  2. 임의 문자열을 Authorization으로 → 응답
  3. 만료된 AT로 → 응답
  4. 정상 AT로 → 성공
- Observe: 각 케이스의 응답 코드/메시지, BE 로그
- Conclude: 인증 흐름이 실제로 막는지 확인

### SC5. RoomSessionMappingInterceptor 책임 경계
- [ ] Hypothesis: sessionId↔roomId 매핑이 SUBSCRIBE 시점에 만들어지고 DISCONNECT 시점에 정리. 매핑 누락 시 disconnect 처리에 영향
- Setup: BE 운영
- Execute:
  1. 정상 입장 → `/topic/room/X/state` 구독
  2. Redis에서 sessionId↔roomId 매핑 키 확인 (어떤 키 패턴인지 탐색)
  3. 의도적으로 SUBSCRIBE 안 하고 SEND만 시도 → 매핑이 만들어지는지
  4. 비정상 disconnect (브라우저 강제 종료) → Redis 매핑이 정리되는지
- Observe: 매핑 lifecycle
- Conclude: WebSocketEventListener의 disconnect 처리 정확성

### SC6. WebSocketEventListener 예외 삼킴
- [ ] Hypothesis: `handleGracePeriodExpired`에서 RoomService.leaveRoom 호출 시 NOT_FOUND_MEMBER_ROOM 외 비즈니스 예외(예: ROOM_NOT_WAITING, INVALID_PHASE)가 발생하면 error 로그만 + 사용자에게 알림 없음
- Setup: 의도적으로 disconnect → grace period 30초 대기 → 자동 leaveRoom 트리거. 이때 leaveRoom이 실패하도록 환경 조작
- Execute:
  1. 사용자 disconnect 후 30초 안에 다른 트랜잭션이 방을 INVALID 상태로 만듦 (예: 라운드 시작)
  2. grace period 만료 → leaveRoom 호출 → BusinessException 가능성
  3. BE 로그 확인 — error 로그만 남고 사용자 측 영향
- Observe: 어떤 예외가 묻히는지
- Conclude: 무시되는 예외가 있는지 + 영향

### SC7. SockJS vs raw WebSocket 이중 등록
- [ ] Hypothesis: 같은 `/ws` 경로에 SockJS와 raw 둘 다 등록 — 클라이언트가 어느 쪽을 사용하는가? 의도된 것인가?
- Setup: BE 운영
- Execute:
  1. Raw WebSocket 클라이언트로 `ws://localhost/ws` 연결 → 성공 여부
  2. SockJS 클라이언트로 `http://localhost/ws` 연결 → 성공 여부
  3. WebSocketConfig 코드 추적: line 39-41 (`withSockJS`) + line 44-45 (raw) — 두 빈이 충돌하지 않는가
  4. Spring 로그에 등록된 endpoint 확인
- Observe: 실제 동작
- Conclude: 둘 다 등록한 이유 파악, 한쪽으로 통일하는 게 맞는지

---

## 발견 후 수정 계획 (PR 분할)

### PR1: ⭐ nginx WebSocket 프록시 타임아웃 연장
- `meari-fe/nginx/nginx.conf` `/ws` location에:
  ```nginx
  location /ws {
      proxy_pass http://meari-spring:8080;
      proxy_http_version 1.1;
      proxy_set_header Upgrade $http_upgrade;
      proxy_set_header Connection "upgrade";
      proxy_set_header Host $host;
      proxy_set_header X-Real-IP $remote_addr;
      
      # WebSocket idle 시간 충분히 — 1시간
      proxy_read_timeout 3600s;
      proxy_send_timeout 3600s;
      
      # Buffering disable for streaming
      proxy_buffering off;
  }
  ```
- 검증: SC2 재현 시 1시간 이내 끊김 없음

### PR2: STOMP heartbeat 활성화
- `WebSocketConfig.java`:
  ```java
  @Bean
  public TaskScheduler heartBeatScheduler() {
      ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
      scheduler.setPoolSize(1);
      scheduler.setThreadNamePrefix("ws-heartbeat-");
      scheduler.initialize();
      return scheduler;
  }
  
  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
      registry.enableSimpleBroker("/topic", "/queue")
              .setHeartbeatValue(new long[]{10000, 10000})  // server, client 10초
              .setTaskScheduler(heartBeatScheduler());
      registry.setApplicationDestinationPrefixes("/app");
  }
  ```
- 클라이언트 측에서도 `heartbeatIncoming/Outgoing` 설정 필요 (FE는 Z6 범위 밖이라 문서로 가이드)
- 검증: SC3 재현 시 heartbeat가 nginx idle 회피

### PR3: `WebSocketEventListener` 예외 처리 정교화
- `handleGracePeriodExpired` catch:
  ```java
  } catch (BusinessException e) {
      switch (e.getErrorCode()) {
          case NOT_FOUND_MEMBER_ROOM -> log.debug("...");
          case ROOM_NOT_WAITING, INVALID_PHASE -> log.warn("자동 퇴장 우회 — 상태 변경: ...");
          default -> log.error("자동 퇴장 실패: ...");
      }
  }
  ```
- 또는 어떤 예외든 사용자에게 STOMP `/queue/errors`로 알림 (B2 통합)
- 검증: SC6 재현 시 무시되던 예외가 분류됨

### PR4: JwtChannelInterceptor 검증 강화 (필요 시)
- SC4 결과에 따라 보강
- CONNECT 시 토큰 없으면 명확한 ERROR 프레임 반환
- 만료 토큰 시 `EXPIRED_TOKEN_ERROR` 명확화
- 검증: SC4 재현 시 모든 우회 케이스가 거부됨

### PR5: SockJS vs raw 이중 등록 정리
- SC7 결과에 따라:
  - 클라이언트가 raw만 쓴다면 line 39-41 제거 (SockJS 의존성 제거)
  - SockJS만 쓴다면 line 44-45 제거
  - 둘 다 필요하면 주석으로 의도 명시
- 검증: 단순화 + 의도 명확

### PR6: SecurityConfig WebSocket 정책
- `/ws/**` permitAll (현재) — handshake는 인증 없이, CONNECT 프레임에서 JWT 검증 → OK
- 단 운영 환경에서 `setAllowedOriginPatterns("*")`은 보안 취약 — 화이트리스트로 변경
  ```java
  registry.addEndpoint("/ws")
      .setAllowedOriginPatterns("https://meari.example.com");
  ```
- 검증: 다른 origin에서 CONNECT 시 거부

---

## 측정 지표 표 (Before/After 채우기)

| 지표 | Before | After (목표) |
|---|---|---|
| nginx idle 끊김 시간 (SC2) | 60s | 3600s (1시간) |
| heartbeat 활성 후 1시간 idle 유지 (SC3) | 끊김 | 유지 |
| 인증 우회 가능 케이스 (SC4) | (측정) | 0 |
| sessionId↔roomId 매핑 누락 (SC5) | (측정) | 0 |
| 무시되는 예외 케이스 (SC6) | (측정) | 분류·로깅·사용자 알림 |
| `/ws` 등록 endpoint 수 (SC7) | 2 | 1 (정리 후) |

---

## 정리 산출물

- `meari-be/md/_websocket_ts_raw.md` — 시나리오별 raw 노트
- `meari-be/md/portfolio_features.md` 신규 또는 #3(WebSocket Redis)에 보강:
  - "nginx 60초 기본 타임아웃으로 WebSocket 끊기던 UX 버그 — 직접 재현 후 `proxy_read_timeout` + STOMP heartbeat 도입"
  - "WebSocket 인터셉터 체인의 책임 분리 — JWT 인증 → 세션 매핑 순서 보장"
  - "WebSocketEventListener 예외 처리 정교화 — 무시되던 케이스 분류"

---

## 진행 체크리스트

- [ ] Pre-flight (STOMP 클라이언트, nginx 로그 마운트, AT)
- [ ] SC1 베이스라인
- [ ] SC2 ⭐ nginx 60초 끊김 재현
- [ ] SC3 STOMP heartbeat 효과
- [ ] SC4 JwtChannelInterceptor 우회 시도
- [ ] SC5 RoomSessionMappingInterceptor 책임
- [ ] SC6 WebSocketEventListener 예외 삼킴
- [ ] SC7 SockJS vs raw 이중 등록
- [ ] PR1 ⭐ nginx 타임아웃 연장
- [ ] PR2 STOMP heartbeat
- [ ] PR3 WebSocketEventListener 예외 정교화
- [ ] PR4 JwtChannelInterceptor 보강
- [ ] PR5 endpoint 등록 정리
- [ ] PR6 origin 화이트리스트
- [ ] 측정 지표 표
- [ ] portfolio 갱신

---

**작성일**: 2026-04-29
**다음 액션**: STOMP 클라이언트 셋업 → SC1 베이스라인 → SC2 nginx 끊김 재현 (가장 빠른 수확)