# RabbitMQ 전환 트러블 raw 노트

> Phase 2 진행 중 발생하는 모든 트러블의 1차 기록. Phase 3에서 정리해 `portfolio_features.md`로 통합.

---

## 사전 변경 사항

- `meari-be/src/main/resources/application.yml`: `analysis.mode: rabbitmq` 추가
- `meari-ai/.env`: `ENABLE_RABBITMQ=true` 추가
- 의도적 미수정: 신뢰성 이슈는 일부러 재현

---

## TS-1. 매 라운드 첫 멤버 분석 결과 publish 실패 (Heisenbug)

### 증상
RabbitMQ 모드로 실제 운영 시, **매 라운드 첫 번째로 분석 완료되는 멤버의 결과 메시지가 publish 되지 않음**. Consumer는 예외 발생 후 `nack(requeue=False)`로 메시지를 drop → BE는 해당 결과를 영영 받지 못함. 두 번째 멤버부터는 재연결된 상태에서 정상 동작.

재현 빈도: **매 라운드 첫 publish마다 100% 재현**. 초기엔 간헐적으로 성공해서 Heisenbug처럼 보였으나, 원인을 특정한 뒤엔 결정론적으로 재현됨.

### 핵심 로그 (3회차 동일 패턴)

```
14:28:43 멤버 56 분석 완료
14:28:43 ConnectionAbortedError: [WinError 10053] 현재 연결은 사용자의 호스트 시스템의 소프트웨어의 의해 중단되었습니다
14:28:43 pika.exceptions.StreamLostError: Stream connection lost
14:28:44 멤버 1 분석 시작 (nack 후 다음 메시지)
14:28:49 RabbitMQ 연결이 끊어짐, 재연결 시도 → 성공
14:28:49 ✅ 멤버 1 분석 결과 발행 성공
```

즉 **첫 번째 publish가 `StreamLostError`로 실패, Consumer는 nack → 해당 결과 drop. 재연결 후 두 번째부터 정상**.

### 원인 추적

**1차 가설**: Windows 방화벽/백신의 TCP keepalive 간섭 → 장시간 idle 연결 강제 종료
- 부분적으로 맞지만, 간격이 일정하게 짧아도 재현되어 주 원인 아님

**2차 가설 (확정)**: **pika의 `BlockingConnection`이 thread-unsafe한데 Producer를 Main 스레드에서 초기화 후 Consumer 스레드에서 사용**

근거:
- `meari-ai/app/services/rabbitmq_producer.py` 하단: `producer = RabbitMQProducer()` — 모듈 import 시점에 **Main 스레드**에서 connection 생성
- `meari-ai/app/main.py:208`: Consumer는 **별도 스레드**(`consumer_thread`)에서 실행
- `meari-ai/app/services/rabbitmq_consumer.py:91`: Consumer callback이 해당 별도 스레드에서 `producer.publish_result` 호출 → **다른 스레드 소유 connection을 사용**

pika 공식 문서 (Connection class docs):
> "BlockingConnection and its channels are **not thread-safe**. Each connection/channel must be used by one thread only."

Windows 10053은 이 스레드 안전성 위반이 OS 레벨에서 TCP RST로 노출된 증상.

### 해결

**Thread-local Producer 패턴**:
- 모듈 레벨 전역 인스턴스 제거
- `threading.local()`을 사용한 `get_producer()` 팩토리 함수
- Consumer 콜백 스레드가 최초 호출 시점에 해당 스레드 소유의 connection 생성

**추가 방어 코드**:
- `publish_result`에 재시도 루프 (최대 2회). `StreamLostError` / `ConnectionClosed` 발생 시 재연결 후 1회 더 시도.

### 변경된 파일

- `meari-ai/app/services/rabbitmq_producer.py` — thread-local + retry
- `meari-ai/app/services/rabbitmq_consumer.py` — `get_producer()` 사용
- `meari-ai/app/main.py` — 전역 producer 참조 제거

### 회고 / 연결되는 RabbitMQ 개념

- **pika의 스레드 모델**: pika의 `BlockingConnection`은 하나의 스레드가 독점적으로 써야 함. 비동기 환경에선 `SelectConnection` 또는 `aio-pika`가 정석
- **Publisher Confirm / Connection Recovery**: 메시지 발행 신뢰성은 "브로커 확인"만으로 충분하지 않고 **클라이언트 측 연결 상태 관리**까지 결합돼야 함
- **Heisenbug → Deterministic Bug**: 처음엔 간헐적으로 보였으나, 원인을 특정하자 결정론적으로 재현됨. 가설 검증의 반례로서의 가치
- **인프라를 도입했다고 끝이 아님**: RabbitMQ 자체는 내구성 있게 메시지를 보관하지만, **Consumer/Producer의 연결 관리 부실**로 오히려 HTTP보다 유실 가능성이 커질 수 있었음. 큐는 도구, 신뢰성은 설계의 몫.

---

### TS-1 검증 결과
Thread-local + retry 패턴 적용 후 AI 서버 재기동. 브라우저에서 라운드 진행 시 매 라운드 첫 멤버도 정상적으로 publish 완료. **StreamLostError 재발 없음**. 해결 확정.

---

## TS-2. (Phase A 진행 중 추가 예정)
