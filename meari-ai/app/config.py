"""
FastAPI 설정 파일
환경변수 및 전역 설정 관리
"""
import os
from dotenv import load_dotenv

load_dotenv()


class Settings:
    """애플리케이션 설정"""

    # 모드 설정 (HTTP 모드일 때는 RabbitMQ 불필요)
    ENABLE_RABBITMQ: bool = os.getenv("ENABLE_RABBITMQ", "false").lower() == "true"

    # RabbitMQ 설정 (ENABLE_RABBITMQ=true일 때만 필요)
    RABBITMQ_HOST: str = os.getenv("RABBITMQ_HOST", "localhost")
    RABBITMQ_PORT: int = int(os.getenv("RABBITMQ_PORT", "5672"))
    RABBITMQ_USERNAME: str = os.getenv("RABBITMQ_USERNAME", "")
    RABBITMQ_PASSWORD: str = os.getenv("RABBITMQ_PASSWORD", "")
    RABBITMQ_VIRTUAL_HOST: str = os.getenv("RABBITMQ_VIRTUAL_HOST", "/")

    # Queue 설정
    ANALYSIS_EXCHANGE: str = "analysis.exchange"
    REQUEST_QUEUE: str = "analysis.requests"
    RESULT_QUEUE: str = "analysis.results"
    REQUEST_ROUTING_KEY: str = "analysis.request"
    RESULT_ROUTING_KEY: str = "analysis.result"

    # AWS S3 설정
    AWS_ACCESS_KEY: str = os.getenv("AWS_ACCESS_KEY")  # 환경변수 필수
    AWS_SECRET_KEY: str = os.getenv("AWS_SECRET_KEY")  # 환경변수 필수
    AWS_REGION: str = os.getenv("AWS_REGION", "ap-northeast-2")
    AWS_S3_BUCKET: str = os.getenv("AWS_S3_BUCKET")  # 환경변수 필수

    # 모델 경로 (HuggingFace 모델명 또는 로컬 경로)
    WAV2VEC2_MODEL_PATH: str = os.getenv("WAV2VEC2_MODEL_PATH", "Kkonjeong/wav2vec2-base-korean")
    MDD_MODEL_PATH: str = os.getenv("MDD_MODEL_PATH", "./models/mdd")  # MDD는 선택사항

    # 추론 설정
    INFERENCE_TIMEOUT: int = int(os.getenv("INFERENCE_TIMEOUT", "300"))  # 5분
    SAMPLE_RATE: int = 16000  # 16kHz

    # 로깅
    LOG_LEVEL: str = os.getenv("LOG_LEVEL", "INFO")

    def validate(self):
        """필수 환경변수 검증"""
        missing = []

        # RabbitMQ 설정 검증 (ENABLE_RABBITMQ=true일 때만)
        if self.ENABLE_RABBITMQ:
            if not self.RABBITMQ_USERNAME:
                missing.append("RABBITMQ_USERNAME")
            if not self.RABBITMQ_PASSWORD:
                missing.append("RABBITMQ_PASSWORD")

        # AWS 설정 검증 (항상 필요)
        if not self.AWS_ACCESS_KEY:
            missing.append("AWS_ACCESS_KEY")
        if not self.AWS_SECRET_KEY:
            missing.append("AWS_SECRET_KEY")
        if not self.AWS_S3_BUCKET:
            missing.append("AWS_S3_BUCKET")

        if missing:
            raise ValueError(
                f"다음 환경변수가 설정되지 않았습니다: {', '.join(missing)}\n"
                f".env 파일을 확인하거나 환경변수를 설정해주세요."
            )


settings = Settings()
# 애플리케이션 시작 시 환경변수 검증
settings.validate()
