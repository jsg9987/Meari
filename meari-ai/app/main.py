"""
FastAPI 메인 애플리케이션
발음 분석 서비스 엔트리포인트
"""
import logging
import signal
import sys
from fastapi import FastAPI, HTTPException
from app.config import settings
from app.models.model_loader import load_models
from app.services.analysis_service import AnalysisService
from app.services.rabbitmq_consumer import RabbitMQConsumer
from app.services.rabbitmq_producer import producer
from app.schemas.request import AnalysisRequestMessage
from app.schemas.response import AnalysisResultMessage

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

# 전역 변수
consumer = None
analysis_service = None


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
        global analysis_service
        analysis_service = AnalysisService()
        logger.info("Analysis Service 초기화 완료")

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


@app.post("/analyze", response_model=AnalysisResultMessage)
async def analyze_pronunciation(request: AnalysisRequestMessage):
    """
    직접 HTTP 호출용 발음 분석 엔드포인트
    Spring Boot에서 직접 호출
    """
    logger.info(f"[HTTP] 분석 요청 수신: roomId={request.room_id}, round={request.round}, memberId={request.member_id}")

    if not analysis_service:
        logger.error("Analysis Service가 초기화되지 않음")
        raise HTTPException(status_code=500, detail="Analysis Service not initialized")

    try:
        # 분석 수행 (동기 메서드를 별도 스레드에서 실행)
        import asyncio
        loop = asyncio.get_event_loop()
        result = await loop.run_in_executor(
            None,
            analysis_service.analyze_member,
            request
        )
        logger.info(f"[HTTP] 분석 완료: roomId={request.room_id}, memberId={request.member_id}, accuracy={result.accuracy}")
        return result

    except Exception as e:
        logger.error(f"[HTTP] 분석 실패: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"분석 실패: {str(e)}")


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
