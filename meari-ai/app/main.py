"""
FastAPI 메인 애플리케이션
발음 분석 서비스 엔트리포인트
"""
import logging
import signal
import sys
from fastapi import FastAPI
from app.config import settings
from app.models.model_loader import load_models
from app.services.analysis_service import AnalysisService
from app.services.rabbitmq_consumer import RabbitMQConsumer
from app.services.rabbitmq_producer import producer

# 로깅 설정
logging.basicConfig(
    level=getattr(logging, settings.LOG_LEVEL),
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# FastAPI 앱
app = FastAPI(
    title="Meari 발음 분석 API",
    description="Wav2Vec2 + MDD 기반 발음 분석 서비스",
    version="1.0.0"
)

# 전역 Consumer
consumer = None


@app.on_event("startup")
async def startup_event():
    """서버 시작 시 실행"""
    logger.info("=" * 50)
    logger.info("Meari 발음 분석 서비스 시작")
    logger.info("=" * 50)

    try:
        # 1. 모델 로드
        logger.info("ML 모델 로드 중...")
        load_models()
        logger.info("ML 모델 로드 완료")

        # 2. Analysis Service 초기화
        analysis_service = AnalysisService()

        # 3. RabbitMQ Consumer 시작 (별도 스레드)
        logger.info("RabbitMQ Consumer 시작 중...")
        global consumer
        consumer = RabbitMQConsumer(analysis_service)

        # Consumer를 별도 스레드에서 실행
        import threading
        consumer_thread = threading.Thread(
            target=consumer.start_consuming,
            daemon=True
        )
        consumer_thread.start()
        logger.info("RabbitMQ Consumer 시작 완료")

        logger.info("=" * 50)
        logger.info("서비스 준비 완료 - 분석 요청 대기 중")
        logger.info("=" * 50)

    except Exception as e:
        logger.error(f"서버 시작 실패: {e}", exc_info=True)
        sys.exit(1)


@app.on_event("shutdown")
async def shutdown_event():
    """서버 종료 시 실행"""
    logger.info("서버 종료 중...")

    try:
        # Consumer 종료
        if consumer:
            consumer.stop_consuming()

        # Producer 종료
        producer.close()

        logger.info("서버 종료 완료")
    except Exception as e:
        logger.error(f"서버 종료 실패: {e}")


@app.get("/")
async def root():
    """헬스체크 엔드포인트"""
    return {
        "service": "Meari 발음 분석 API",
        "status": "running",
        "version": "1.0.0"
    }


@app.get("/health")
async def health_check():
    """상세 헬스체크"""
    return {
        "status": "healthy",
        "rabbitmq": "connected",
        "models": "loaded"
    }


def signal_handler(sig, frame):
    """시그널 핸들러 (Ctrl+C)"""
    logger.info("\n시그널 수신 - 서버 종료 중...")
    if consumer:
        consumer.stop_consuming()
    producer.close()
    sys.exit(0)


# 시그널 등록
signal.signal(signal.SIGINT, signal_handler)
signal.signal(signal.SIGTERM, signal_handler)


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(
        "app.main:app",
        host="0.0.0.0",
        port=8000,
        reload=False,  # Production에서는 False
        log_level=settings.LOG_LEVEL.lower()
    )
