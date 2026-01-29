"""
FastAPI 설정 파일
환경변수 및 전역 설정 관리
"""
import os
from dotenv import load_dotenv

load_dotenv()


class Settings:
    """애플리케이션 설정"""

    # RabbitMQ 설정
    RABBITMQ_HOST: str = os.getenv("RABBITMQ_HOST", "localhost")
    RABBITMQ_PORT: int = int(os.getenv("RABBITMQ_PORT", "5672"))
    RABBITMQ_USERNAME: str = os.getenv("RABBITMQ_USERNAME", "ssafy")
    RABBITMQ_PASSWORD: str = os.getenv("RABBITMQ_PASSWORD", "ssafy")
    RABBITMQ_VIRTUAL_HOST: str = os.getenv("RABBITMQ_VIRTUAL_HOST", "/")

    # Queue 설정
    ANALYSIS_EXCHANGE: str = "analysis.exchange"
    REQUEST_QUEUE: str = "analysis.requests"
    RESULT_QUEUE: str = "analysis.results"
    REQUEST_ROUTING_KEY: str = "analysis.request"
    RESULT_ROUTING_KEY: str = "analysis.result"

    # AWS S3 설정
    AWS_ACCESS_KEY: str = os.getenv("AWS_ACCESS_KEY", "")
    AWS_SECRET_KEY: str = os.getenv("AWS_SECRET_KEY", "")
    AWS_REGION: str = os.getenv("AWS_REGION", "ap-northeast-2")
    AWS_S3_BUCKET: str = os.getenv("AWS_S3_BUCKET", "meari-bucket")

    # 모델 경로
    WAV2VEC2_MODEL_PATH: str = os.getenv("WAV2VEC2_MODEL_PATH", "./models/wav2vec2")
    MDD_MODEL_PATH: str = os.getenv("MDD_MODEL_PATH", "./models/mdd")

    # 추론 설정
    INFERENCE_TIMEOUT: int = int(os.getenv("INFERENCE_TIMEOUT", "300"))  # 5분
    SAMPLE_RATE: int = 16000  # 16kHz

    # 로깅
    LOG_LEVEL: str = os.getenv("LOG_LEVEL", "INFO")


settings = Settings()
