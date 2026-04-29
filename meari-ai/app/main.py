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
from app.schemas.request import AnalysisRequestMessage
from app.schemas.response import AnalysisResultMessage

# RabbitMQ는 선택적으로 import (ENABLE_RABBITMQ=true일 때만)
if settings.ENABLE_RABBITMQ:
    from app.services.rabbitmq_consumer import RabbitMQConsumer
    # Producer는 thread-local이라 여기서 인스턴스화하지 않음 (Consumer 스레드에서 get_producer()로 최초 생성)

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


# OpenAPI schema 커스터마이징 (Swagger UI 개선)
def custom_openapi():
    """Swagger UI에 detailed_analysis JSON 구조 표시"""
    if app.openapi_schema:
        return app.openapi_schema

    from fastapi.openapi.utils import get_openapi

    openapi_schema = get_openapi(
        title=app.title,
        version=app.version,
        description=app.description,
        routes=app.routes,
    )

    # detailed_analysis JSON 구조 상세 정의
    openapi_schema["components"]["schemas"]["DetailedAnalysisExample"] = {
        "type": "object",
        "description": "detailed_analysis JSON 파싱 후 구조",
        "properties": {
            "summary": {
                "type": "object",
                "properties": {
                    "total_sentences": {"type": "integer", "example": 3},
                    "analyzed_sentences": {"type": "integer", "example": 3},
                    "average_accuracy": {"type": "integer", "example": 85},
                    "average_confidence": {"type": "number", "example": 0.92},
                    "average_intonation": {"type": "integer", "example": 94}
                }
            },
            "sentences": {
                "type": "array",
                "items": {
                    "type": "object",
                    "properties": {
                        "sentence_id": {"type": "integer"},
                        "text_expected": {"type": "string"},
                        "text_recognized": {"type": "string"},
                        "accuracy": {"type": "integer"},
                        "mean_confidence": {"type": "number"},
                        "syllables": {"type": "array", "items": {"type": "string"}},
                        "syllable_confidences": {"type": "array", "items": {"type": "number"}},
                        "errors": {
                            "type": "array",
                            "items": {
                                "type": "object",
                                "properties": {
                                    "type": {"type": "string", "example": "replace"},
                                    "position": {"type": "integer"},
                                    "expected": {"type": "string"},
                                    "actual": {"type": "string"},
                                    "confidence": {"type": "number"},
                                    "description": {"type": "string"}
                                }
                            }
                        },
                        "intonation": {
                            "type": "object",
                            "properties": {
                                "score": {"type": "integer", "minimum": -1, "maximum": 100, "example": 94},
                                "feedback": {"type": "string", "example": "억양이 매우 자연스럽습니다!"},
                                "pitch_data": {
                                    "type": "object",
                                    "description": "차트용 데이터 (100개 고정)",
                                    "properties": {
                                        "reference": {
                                            "type": "array",
                                            "items": {"type": "number"},
                                            "description": "정답 pitch (100개, Hz)",
                                            "example": [216.5, 218.3, 220.1]
                                        },
                                        "user": {
                                            "type": "array",
                                            "items": {"type": "number"},
                                            "description": "사용자 pitch (100개, Hz)",
                                            "example": [195.2, 197.8, 199.5]
                                        },
                                        "time_points": {
                                            "type": "array",
                                            "items": {"type": "number"},
                                            "description": "시간 축 (100개, 초)",
                                            "example": [0.0, 0.05, 0.10]
                                        }
                                    }
                                },
                                "statistics": {
                                    "type": "object",
                                    "properties": {
                                        "reference": {
                                            "type": "object",
                                            "properties": {
                                                "mean": {"type": "number", "example": 216.5},
                                                "std": {"type": "number", "example": 12.3},
                                                "min": {"type": "number", "example": 180.0},
                                                "max": {"type": "number", "example": 250.0}
                                            }
                                        },
                                        "user": {
                                            "type": "object",
                                            "properties": {
                                                "mean": {"type": "number", "example": 195.2},
                                                "std": {"type": "number", "example": 15.8},
                                                "min": {"type": "number", "example": 160.0},
                                                "max": {"type": "number", "example": 230.0}
                                            }
                                        },
                                        "pitch_difference": {"type": "number", "example": 21.3}
                                    }
                                },
                                "raw_data": {
                                    "type": "object",
                                    "description": "원본 데이터 (디버깅용)",
                                    "properties": {
                                        "reference_pitch": {
                                            "type": "array",
                                            "items": {"type": "number"},
                                            "description": "정답 원본 pitch (가변 길이)"
                                        },
                                        "user_pitch": {
                                            "type": "array",
                                            "items": {"type": "number"},
                                            "description": "사용자 원본 pitch (가변 길이)"
                                        },
                                        "reference_frames": {"type": "integer", "example": 292},
                                        "user_frames": {"type": "integer", "example": 371}
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    app.openapi_schema = openapi_schema
    return app.openapi_schema


app.openapi = custom_openapi

# 전역 변수
consumer = None
analysis_service = None


@app.on_event("startup")
async def startup_event():
    """서버 시작 시 실행"""
    logger.info("=" * 50)
    logger.info("Meari 발음 분석 서비스 시작")
    logger.info(f"모드: {'RabbitMQ' if settings.ENABLE_RABBITMQ else 'HTTP'}")
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

        # 3. RabbitMQ Consumer 시작 (ENABLE_RABBITMQ=true일 때만)
        if settings.ENABLE_RABBITMQ:
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
        else:
            logger.info("RabbitMQ 비활성화 (HTTP 모드)")

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
        # RabbitMQ 종료 (활성화된 경우만)
        # Producer는 thread-local이라 consumer_thread(daemon=True)와 함께 자연스럽게 정리됨
        if settings.ENABLE_RABBITMQ:
            if consumer:
                consumer.stop_consuming()

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
    if settings.ENABLE_RABBITMQ:
        if consumer:
            consumer.stop_consuming()
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
