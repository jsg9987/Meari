# 쉐도잉 분석 시스템 빠른 시작 가이드

## 🚀 5분 안에 시작하기

### 1️⃣ RabbitMQ 실행

```bash
docker run -d --name rabbitmq \
  -p 5672:5672 \
  -p 15672:15672 \
  -e RABBITMQ_DEFAULT_USER=ssafy \
  -e RABBITMQ_DEFAULT_PASS=ssafy \
  rabbitmq:3-management
```

확인: http://localhost:15672 (ID: ssafy, PW: ssafy)

### 2️⃣ Spring Boot 실행

```bash
cd meari-be
./gradlew bootRun
```

### 3️⃣ FastAPI 설정 및 실행

```bash
cd meari-ai

# 가상환경 생성
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate

# 의존성 설치
pip install -r requirements.txt

# 환경변수 설정
cp .env.example .env
# .env 파일 편집 (AWS Credentials 설정)

# 서버 시작
python -m app.main
```

### 4️⃣ 동작 확인

#### Spring Boot
```bash
curl http://localhost:8080/api/v1/reports/room/1
```

#### FastAPI
```bash
curl http://localhost:8000/health
```

#### RabbitMQ
- http://localhost:15672/#/queues
- `analysis.requests`, `analysis.results` 큐 확인

---

## 📋 전체 플로우 테스트

### 1. 녹음 완료 트리거

WebSocket으로 녹음 완료 메시지 전송:

```javascript
// Frontend
stompClient.send("/app/room/123/recording/complete", {}, JSON.stringify({
  member_id: 1,
  sentence_id: 1,
  audio_url: "s3://meari-bucket/recordings/room123/round1/member1/sentence1.wav"
}));
```

### 2. 로그 확인

**Spring Boot:**
```
멤버 1 모든 녹음 완료, 분석 요청
발음 분석 요청 발행: roomId=123, round=1, memberId=1
```

**RabbitMQ UI:**
- `analysis.requests` 큐에 메시지 1개 추가됨

**FastAPI:**
```
분석 요청 수신: roomId=123, round=1, memberId=1
멤버 분석 시작...
S3 다운로드...
ASR 결과: ...
분석 완료 및 ACK: memberId=1
```

**RabbitMQ UI:**
- `analysis.results` 큐에 메시지 1개 추가됨

**Spring Boot:**
```
발음 분석 결과 수신: roomId=123, memberId=1, accuracy=85, intonation=90
ShadowingReport 업데이트 완료: reportId=1, status=COMPLETED
```

### 3. 결과 조회

```bash
curl http://localhost:8080/api/v1/reports/1
```

**응답:**
```json
{
  "status": "success",
  "data": {
    "shadowing_report_id": 1,
    "member_id": 1,
    "accuracy": 85,
    "intonation": 90,
    "status": "COMPLETED",
    "detailed_analysis": "{...}"
  }
}
```

---

## ⚠️ 트러블슈팅

### RabbitMQ 연결 안됨
```bash
# RabbitMQ 실행 확인
docker ps | grep rabbitmq

# 재시작
docker restart rabbitmq
```

### FastAPI 모델 로드 실패
```bash
# 모델 경로 확인
ls -la meari-ai/models/wav2vec2
ls -la meari-ai/models/mdd

# Placeholder 모드로 실행 (개발용)
# MDD 모델 없이도 ASR은 동작
```

### Spring Boot DB 연결 실패
```bash
# PostgreSQL 실행 확인
docker ps | grep postgres

# application.yml 확인
# - datasource.url
# - datasource.username/password
```

---

## 📚 다음 단계

1. **실제 MDD 모델 통합**: `meari-ai/app/models/model_loader.py` 수정
2. **억양 분석 구현**: `meari-ai/app/services/analysis_service.py` 수정
3. **성능 테스트**: 4명 동시 녹음 → 멤버별 분석 시간 측정
4. **프론트엔드 연동**: Report API 호출

상세 문서: `meari-be/md/SHADOWING_ANALYSIS_IMPLEMENTATION.md`
