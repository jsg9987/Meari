# RabbitMQ 학습 노트 + Meari 코드 검수

> 목적: Meari의 발음 분석 파이프라인을 HTTP에서 RabbitMQ로 재전환하기 전, 면접 답변과 트러블슈팅의 베이스가 될 핵심 개념을 정리하고, 현재 코드를 그 잣대로 검수한다.

---

## 1. 핵심 개념 (면접 빈출 + Meari 직결)

### 1.1 AMQP 0-9-1 모델

```
Producer ──publish──> Exchange ──(binding+routing key)──> Queue ──> Consumer
```

- **Producer**: 메시지를 만들어 Exchange로 보내는 쪽 (Queue를 직접 모름)
- **Exchange**: 메시지의 라우터. Queue와 Binding으로 연결됨
- **Binding**: Exchange ↔ Queue 연결 규칙. Routing key가 매칭되면 해당 Queue로 메시지 복사
- **Queue**: 메시지가 쌓이는 버퍼. Consumer가 빼감
- **Channel**: TCP 연결 위에 다중화된 가상 연결 (스레드별로 만드는 게 보통)

핵심 포인트: **Producer는 Queue를 모른다.** Exchange와 Routing key만 안다. 이게 "Producer/Consumer 분리"의 실체.

### 1.2 Exchange 4종류

| 종류 | 라우팅 방식 | 용례 |
|---|---|---|
| **direct** | routing key가 정확히 일치하는 binding | 1:1 작업 큐, RPC |
| **topic** | routing key 패턴 매칭 (`order.*`, `log.#`) | 이벤트 분류 |
| **fanout** | 모든 바인딩된 큐에 브로드캐스트 (key 무시) | pub/sub, 알림 |
| **headers** | 메시지 헤더로 라우팅 | 거의 안 씀 |

→ Meari는 **direct** 사용. 이유: 분석 요청은 1개 Consumer 그룹(FastAPI)만 받으면 되고, 결과도 마찬가지. 패턴/브로드캐스트가 필요 없음.

### 1.3 Queue 속성

- **durable=true**: 브로커 재시작해도 큐 정의가 살아남음 (큐 메타데이터 영속화)
- **exclusive=true**: 해당 connection만 사용 가능, connection 끊기면 삭제 (RPC reply 큐 용도)
- **auto-delete=true**: 마지막 consumer가 떠나면 자동 삭제
- **TTL** (`x-message-ttl`): 큐에 들어온 메시지가 N ms 후 자동 만료 → DLQ로 라우팅 가능
- **max-length**: 큐 최대 메시지 수, 초과 시 가장 오래된 것부터 drop or DLQ

### 1.4 메시지 영속성 — durable과 헷갈리지 말 것

- **Queue durable**: 큐 자체의 메타데이터가 디스크에 저장
- **Message persistent (`delivery_mode=2`)**: 메시지 본문이 디스크에 저장

**둘 다 켜야** 브로커 재시작 시 메시지가 살아남는다. 한쪽만 켜면 의미 없음.

→ Meari Spring 측은 `RabbitTemplate.convertAndSend`가 기본적으로 PERSISTENT로 보내고, AI 측은 명시적으로 `delivery_mode=2`. Queue도 양쪽 다 durable=true. ✅

### 1.5 메시지 손실 방지 3종 세트

발행 단계에서 메시지가 사라지지 않도록 막는 메커니즘.

1. **Publisher Confirm**: 브로커가 "메시지 받았음" ack를 Producer에게 비동기로 회신. ack 못 받으면 재발행. Spring에서는 `publisher-confirm-type=correlated` + `ConfirmCallback`.
2. **Publisher Returns + mandatory=true**: 라우팅할 Queue가 없으면 Producer에게 메시지 반환. `ReturnsCallback`으로 처리.
3. **Transactional channel**: tx로 묶지만 성능 저하가 커서 거의 안 씀. Confirm으로 대체.

핵심: **Confirm은 "브로커까지 도달", Returns는 "큐에 라우팅"** — 다른 단계를 지킨다.

### 1.6 Consumer Ack 모드

| 모드 | 동작 | 위험 |
|---|---|---|
| **auto-ack** | 메시지 받자마자 ack | Consumer가 처리 도중 죽으면 메시지 소실 |
| **manual ack** | 처리 성공 후 `basic_ack` | 누락 시 unacked로 영원히 남음 |

처리 실패 시:
- `basic_nack(requeue=true)` → 다시 큐 앞으로. **DLQ 없으면 무한 사이클**
- `basic_nack(requeue=false)` → drop. DLQ가 바인딩돼 있으면 거기로

Spring AMQP `@RabbitListener` 기본은 AUTO와 비슷하지만, 메서드에서 예외 던지면 ack 하지 않고 재처리. `listener.simple.retry`가 활성화되면 인메모리 N회 재시도 후 reject.

### 1.7 Prefetch (QoS)

`channel.basic_qos(prefetch_count=N)` — 한 Consumer가 ack 안 한 메시지를 동시에 N개까지만 보유.

- N 무제한(기본): 한 Consumer가 큐의 메시지를 다 빨아당김. **다른 Consumer 굶음(load imbalance)**
- N=1: 한 번에 하나만. 처리 시간 편차 큰 작업(=AI 분석)에 적합
- 일반 웹 작업: 10~50

→ Meari AI 쪽: `prefetch_count=1` ✅ (Wav2Vec2 분석은 5~30초 걸리므로 정답)
→ Meari BE 쪽 (결과 큐): 미설정 → 기본값. 결과 처리는 가벼우니 큰 문제 아니지만 명시하는 게 좋음.

### 1.8 DLX / DLQ — 실패 메시지 격리

- **DLX (Dead Letter Exchange)**: 처리 실패/만료된 메시지가 라우팅되는 별도 Exchange
- **DLQ (Dead Letter Queue)**: DLX에 바인딩된 큐. 운영자가 보고 분석/재처리

큐 선언 시 `x-dead-letter-exchange`, `x-dead-letter-routing-key` 인자로 지정.

DLQ가 없으면:
- nack(requeue=false) → 메시지 그냥 사라짐
- nack(requeue=true) → 무한 사이클 (poison message)

→ Meari **양쪽 다 DLQ 없음.** Phase 2에서 잡을 핵심 개선점.

### 1.9 멱등성 (Idempotent Consumer)

RabbitMQ는 "at-least-once" 보장 — 같은 메시지가 두 번 이상 처리될 수 있다.
- Consumer가 처리는 했지만 ack 직전에 죽음 → 브로커가 재전송
- Network partition으로 인한 중복

따라서 Consumer는 멱등이어야 한다. 흔한 방법:
- 메시지에 `messageId` 포함, 처리한 ID를 Redis Set에 저장. 다시 오면 skip
- 비즈니스 키(예: `(roomId, round, memberId)`) 기반 upsert

→ Meari `ShadowingReport`는 같은 `(room, round, member)`로 두 번 분석 결과 받으면 update가 아닌 새 행이 생기는 구조. **오늘 발생한 `NonUniqueResultException`과 같은 부류** — 멱등성 미보장 사례. 별도 이슈.

### 1.10 Spring AMQP 직렬화 — `__TypeId__`의 함정

Jackson2JsonMessageConverter는 메시지 헤더 `__TypeId__`에 클래스명을 박아서 보낸다.
- 같은 Spring↔Spring이면 Consumer가 그 클래스를 자동으로 역직렬화 ✅
- 다른 언어(FastAPI)에서 받으면 그냥 무시 가능 (json.loads로 처리) ✅
- 반대로 **FastAPI가 보낸 메시지엔 `__TypeId__` 헤더 없음** → Spring이 받을 때:
  - `@RabbitListener` 메서드 파라미터 타입을 보고 `DefaultClassMapper`가 추론해줌 (대부분 동작)
  - 하지만 안전하지 않음. `Jackson2JsonMessageConverter`에 `DefaultJackson2JavaTypeMapper.setTrustedPackages` + `setIdClassMapping`을 명시하는 게 정석

→ Meari는 양쪽 다 명시적 매핑 없음. 메서드 시그니처(`AnalysisResultMessage`) 기반 추론에 의존. 동작은 하지만 fragile.

### 1.11 Spring `SimpleRabbitListenerContainerFactory` 동시성

```yaml
spring.rabbitmq.listener.simple:
  concurrency: 3            # 시작 컨슈머 스레드 수
  max-concurrency: 10       # 최대
  prefetch: 5
  acknowledge-mode: manual  # auto | manual | none
  retry:
    enabled: true
    max-attempts: 3
```

→ Meari는 retry만 설정, concurrency/prefetch/ack-mode 미설정.

### 1.12 (확장) Quorum Queue / Mirrored Queue

- **Mirrored Queue (deprecated)**: 마스터-슬레이브 복제, split-brain 약함
- **Quorum Queue**: Raft 기반, 3노드 이상에서 강한 일관성. 운영 환경 권장

Meari는 단일 노드라 미적용. 면접에서 "HA는 어떻게?" 물으면 "Quorum Queue + 3노드 클러스터" 정도 답변.

---

## 2. 현재 Meari 코드 검수 결과

### 2.1 인프라 구성

| 항목 | 값 | 비고 |
|---|---|---|
| RabbitMQ 컨테이너 | `meari-rabbitmq` (rabbitmq:3-management-alpine) | management UI 포함 |
| 호스트 포트 매핑 | `5673:5672` | **비표준 (충돌 회피)** |
| Management UI | `localhost:15672` | docker-compose에 포트 매핑 누락 — 추가 필요 |
| 기본 계정 | `ssafy` / `ssafy` | docker-compose 환경변수 |

### 2.2 BE/AI 설정 일치성

| 항목 | BE (`application.yml`) | AI (`config.py` 기본값) | 일치 |
|---|---|---|---|
| host | `localhost` | `localhost` | ✅ |
| port | `5673` | `5672` | ❌ **트러블 1순위** |
| username | `ssafy` | `""` (빈 문자열) | ❌ |
| password | `ssafy` | `""` (빈 문자열) | ❌ |
| virtual-host | `/` | `/` | ✅ |

→ AI `.env`에 `RABBITMQ_PORT=5673`, `RABBITMQ_USERNAME=ssafy`, `RABBITMQ_PASSWORD=ssafy` 명시하지 않으면 즉시 연결 실패.

### 2.3 토폴로지 (Exchange/Queue/Binding)

| 항목 | 값 | 평가 |
|---|---|---|
| Exchange | `analysis.exchange` (Direct, durable) | ✅ |
| Request Queue | `analysis.requests` (durable) | ✅ |
| Result Queue | `analysis.results` (durable) | ✅ |
| Routing Key (req) | `analysis.request` | ✅ |
| Routing Key (res) | `analysis.result` | ✅ |
| 양쪽에서 모두 declare | BE/AI 모두 `exchange_declare`, `queue_declare` | ✅ 멱등 안전 |

### 2.4 신뢰성 메커니즘 검수

| # | 항목 | 상태 | 위치 | 영향 |
|---|---|---|---|---|
| 1 | Queue durable | ✅ | `RabbitMQConfig:44,53` / `rabbitmq_consumer.py:51-52` | OK |
| 2 | Message persistent | ✅ | Spring 기본 PERSISTENT, AI는 `delivery_mode=2` | OK |
| 3 | Publisher Confirm 활성화 | ⚠️ 부분 | `application.yml:56` `correlated` 설정 | 콜백 미구현 → 발행 실패해도 알 수 없음 |
| 4 | Publisher Returns + mandatory | ⚠️ | yml에 `publisher-returns: true`만, mandatory 미설정 | 큐 없을 때 메시지 silently drop |
| 5 | BE Consumer ack 모드 | ❌ | `AnalysisConsumer.java:28` `@RabbitListener` 기본 (AUTO) | 예외 시 retry 후 reject → drop (DLQ 없음) |
| 6 | AI Consumer ack 모드 | ✅ | `rabbitmq_consumer.py:94/100` manual ack/nack | OK |
| 7 | DLQ | ❌ | 양쪽 모두 미설정 | 실패 메시지 영구 소실 / 무한 루프 위험 |
| 8 | BE prefetch | ❌ 미설정 | yml에 `listener.simple.prefetch` 없음 | 결과 큐는 가벼워서 큰 문제 아님 |
| 9 | AI prefetch | ✅ | `rabbitmq_consumer.py:67` `prefetch_count=1` | AI 분석 시간 편차 고려 — 정답 |
| 10 | 멱등성 | ❌ | `messageId` 미사용, `ShadowingReport`도 비멱등 | 중복 처리 시 DB 중복 행 (오늘 발생한 이슈와 동형) |
| 11 | BE retry 정책 | ⚠️ | yml에 `max-attempts: 3, multiplier: 2` | DLQ 없이 단독 사용 → 3회 후 메시지 사라짐 |
| 12 | 직렬화 BE→AI | ✅ 동작 | Jackson `__TypeId__` 헤더 추가 → AI는 `json.loads`로 무시 | snake_case 변환은 `AnalysisRequestMessage` Pydantic 필드 alias 확인 필요 |
| 13 | 직렬화 AI→BE | ⚠️ fragile | AI는 순수 JSON, `__TypeId__` 없음 → Spring은 `@RabbitListener` 시그니처로 추론 | 메서드 시그니처 변경 시 깨짐. `Jackson2JavaTypeMapper` 명시 권장 |
| 14 | AI 연결 복구 (publisher) | ⚠️ | `publish_result`에 재연결 체크 있음 (`rabbitmq_producer.py:71`) | OK (수동) |
| 15 | AI 연결 복구 (consumer) | ❌ | `start_consuming` 중 connection drop 시 재연결 없음 | RabbitMQ 재시작 시 AI 재기동 필요 |
| 16 | 헬스체크 정확성 | ❌ | `main.py:258` `"rabbitmq": "connected"` 하드코딩 | 실제 상태 미반영 |

### 2.5 Dead Code / 구조적 이슈

- **`AnalysisProducer.java`** — Dead code 확정. 프로덕션 코드 어디서도 호출하지 않고 `RoomServiceWebSocketTest`에서만 mock 주입. 동일 로직이 `AnalysisRequestBuilder`(빌드) + `RabbitMQAnalysisService`(publish)로 분해돼 있어 중복. **삭제 대상** (사용자 직접 처리 예정).
- **`AnalysisRequestBuilder` vs `AnalysisProducer`**: 빌드 로직 중복. `AnalysisProducer` 삭제로 해소.

### 2.6 환경 분리 미흡 (오늘 발견과 동형)

- `RestClientConfig`의 `http://fastapi:8000` 사례처럼, RabbitMQ 호스트도 docker 환경/로컬 환경에서 다른 값이 필요한데 yml에 단일 값만 박혀있음. → profile 분리 또는 env override 정착 필요.

---

## 3. Phase 2 진입 전 의사결정

**보강할 것 (필수, Phase 2 진입 전):**
- 없음. 일부러 현 상태로 돌려서 트러블이 어디서 터지는지 직접 본다 (포폴 소재 가치).

**즉시 환경 보정만 (트러블이 아닌 단순 미스):**
- AI `.env` 또는 `meari-ai/.env`에 RabbitMQ 접속 정보 4개 명시 (포트/계정).
- docker-compose에 management UI 포트 매핑 추가 (`15672:15672`).

**문서화 시 강조할 보강 항목 (Phase 2 진행 중 발견되면 같이 손봄):**
- DLQ 추가 (`analysis.requests.dlq`)
- BE Consumer manual ack 전환
- Publisher Confirm/Return 콜백 구현
- 멱등성: `ShadowingReport` 조회 → 있으면 update / 없으면 insert (UNIQUE 제약 + UPSERT 또는 lock)
- AI Consumer 자동 재연결 루프
- `Jackson2JavaTypeMapper` 명시 매핑

---

## 4. 면접 답변 매핑 (각 개념 → Meari 코드 위치)

| 면접 질문 | 답변 시 가리킬 코드 |
|---|---|
| "RabbitMQ에서 메시지 손실 막는 방법?" | `application.yml:56-57` Publisher Confirm/Returns 설정 (다만 콜백 미구현은 한계로 인정) |
| "Consumer ack 모드?" | `rabbitmq_consumer.py:94/100` manual ack/nack |
| "DLQ 써보셨나요?" | "현재는 미적용. Phase 2 트러블슈팅 결과 도입 — 코드 위치" |
| "왜 Direct Exchange?" | `RabbitMQConfig.java:33` — 1:1 작업 큐 패턴, 패턴/브로드캐스트 불필요 |
| "Prefetch 왜 1?" | `rabbitmq_consumer.py:67` — Wav2Vec2 분석 5~30초 편차, load balancing 위해 |
| "메시지 직렬화 호환?" | Jackson `__TypeId__` 회피 위해 양쪽 모두 순수 JSON 사용 (BE→AI는 Pydantic alias, AI→BE는 시그니처 추론) |
| "HTTP 대신 왜 RabbitMQ?" | `portfolio_features.md` 1번 참조 — 동기 블로킹 → @Async → 메시지 내구성 |

---

다음 단계: Phase 2 (실제 RabbitMQ 모드 전환). raw 트러블 노트는 `_rabbitmq_ts_raw.md`에 누적, 정리 후 `portfolio_features.md` 1번 섹션으로 통합.
