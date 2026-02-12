# Meari 발음 분석 서비스 (FastAPI)

Wav2Vec2 + MDD 기반 비동기 발음 분석 시스템

## 아키텍처

```
[Spring Boot] → [RabbitMQ] → [FastAPI] → [ML Models]
                                ↓
                          [RabbitMQ]
                                ↓
                          [Spring Boot]
```

## 주요 기능

- **RabbitMQ 기반 비동기 처리**: Spring Boot와 메시지 큐를 통한 통신
- **Wav2Vec2 ASR**: 음성 → 텍스트 변환
- **MDD (Mispronunciation Detection)**: 발음 오류 탐지
- **S3 통합**: 녹음 파일 자동 다운로드
- **자동 점수 계산**: 정확도 및 억양 점수

## 설치

### 1. Python 환경 설정

```bash
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate
pip install -r requirements.txt
```

### 2. 환경 변수 설정

`.env` 파일 생성 (`.env.example` 참고):

```env
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=ssafy
RABBITMQ_PASSWORD=ssafy

AWS_ACCESS_KEY=your_key
AWS_SECRET_KEY=your_secret
AWS_S3_BUCKET=meari-bucket

WAV2VEC2_MODEL_PATH=./models/wav2vec2
MDD_MODEL_PATH=./models/mdd
```

### 3. 모델 준비

Jupyter notebook에서 학습한 모델을 다음 경로에 배치:

```
meari-ai/
├── models/
│   ├── wav2vec2/
│   │   ├── config.json
│   │   ├── pytorch_model.bin
│   │   └── preprocessor_config.json
│   └── mdd/
│       └── model.pth
```

## 실행

### 개발 모드

```bash
cd meari-ai
python -m app.main
```

또는

```bash
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

### Production 모드

```bash
uvicorn app.main:app --host 0.0.0.0 --port 8000 --workers 4
```

## 엔드포인트

- `GET /` - 기본 정보
- `GET /health` - 헬스체크

## 메시지 포맷

### 입력 (Spring Boot → FastAPI)

```json
{
  "room_id": 123,
  "round": 1,
  "content_id": 101,
  "member_id": 1,
  "role_id": 1,
  "sentences": [
    {
      "sentence_id": 1,
      "audio_url": "s3://meari-bucket/recordings/...",
      "text_ko": "안녕하세요",
      "start_time": 0.5,
      "end_time": 2.3
    }
  ]
}
```

### 출력 (FastAPI → Spring Boot)

```json
{
  "room_id": 123,
  "round": 1,
  "member_id": 1,
  "accuracy": 85,
  "intonation": 90,
  "detailed_analysis": "{...}"
}
```

## 개발 가이드

### 모델 업데이트

`app/models/model_loader.py`의 `load_mdd_model()` 함수를 실제 MDD 모델에 맞게 구현:

```python
def load_mdd_model(model_path: str):
    model = YourMDDModel(...)
    model.load_state_dict(torch.load(f"{model_path}/model.pth"))
    model.to(device)
    model.eval()
    return model
```

### 분석 로직 커스터마이징

`app/services/analysis_service.py`의 `detect_pronunciation_errors()` 수정

## 트러블슈팅

### RabbitMQ 연결 실패

- RabbitMQ 서버 실행 확인: `docker ps | grep rabbitmq`
- 포트 및 인증 정보 확인

### 모델 로드 실패

- 모델 파일 경로 확인
- GPU 메모리 부족 시 CPU 사용: `CUDA_VISIBLE_DEVICES=-1`

### S3 다운로드 실패

- AWS Credentials 확인
- S3 버킷 권한 확인

## TODO

- [ ] 실제 MDD 모델 통합
- [ ] 억양 분석 로직 구현
- [ ] 배치 추론 최적화
- [ ] Dead Letter Queue 설정
- [ ] 모니터링 (Prometheus)
