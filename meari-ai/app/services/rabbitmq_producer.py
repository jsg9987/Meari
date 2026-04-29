"""
RabbitMQ Producer
FastAPI에서 Spring Boot로 분석 결과 전송

스레드 안전성 주의: pika.BlockingConnection은 생성 스레드에서만 사용해야 함.
Consumer 콜백 스레드와 Main 스레드가 달라 발생하던 StreamLostError(10053)를
thread-local 패턴으로 해결. get_producer()를 통해 접근할 것.
"""
import json
import logging
import threading
import pika
from app.config import settings
from app.schemas.response import AnalysisResultMessage

logger = logging.getLogger(__name__)


class RabbitMQProducer:
    """RabbitMQ 결과 발행자"""

    def __init__(self):
        self.connection = None
        self.channel = None
        self.connect()

    def connect(self):
        """RabbitMQ 연결"""
        try:
            credentials = pika.PlainCredentials(
                settings.RABBITMQ_USERNAME,
                settings.RABBITMQ_PASSWORD
            )
            parameters = pika.ConnectionParameters(
                host=settings.RABBITMQ_HOST,
                port=settings.RABBITMQ_PORT,
                virtual_host=settings.RABBITMQ_VIRTUAL_HOST,
                credentials=credentials,
                heartbeat=600,
                blocked_connection_timeout=300
            )
            self.connection = pika.BlockingConnection(parameters)
            self.channel = self.connection.channel()

            # Publisher Confirm 활성화 (메시지가 실제로 라우팅되었는지 확인)
            self.channel.confirm_delivery()

            # Exchange 선언 (이미 Spring Boot에서 생성되었지만 멱등성 보장)
            self.channel.exchange_declare(
                exchange=settings.ANALYSIS_EXCHANGE,
                exchange_type='direct',
                durable=True
            )

            # RESULT_QUEUE도 선언하고 바인딩 (Consumer가 먼저 시작 안 할 수도 있으므로)
            self.channel.queue_declare(queue=settings.RESULT_QUEUE, durable=True)
            self.channel.queue_bind(
                exchange=settings.ANALYSIS_EXCHANGE,
                queue=settings.RESULT_QUEUE,
                routing_key=settings.RESULT_ROUTING_KEY
            )

            logger.info(
                f"RabbitMQ Producer 연결 성공 "
                f"(Exchange: {settings.ANALYSIS_EXCHANGE}, "
                f"Result Queue: {settings.RESULT_QUEUE})"
            )
        except Exception as e:
            logger.error(f"RabbitMQ 연결 실패: {e}")
            raise

    def publish_result(self, result: AnalysisResultMessage, max_retries: int = 2):
        """
        분석 결과 발행 (재시도 포함)

        StreamLostError / ConnectionClosed 발생 시 재연결 후 1회 더 시도.
        라우팅 실패(UnroutableError)는 재시도해도 소용없으므로 즉시 예외.
        """
        message_body = result.model_dump_json()
        last_err = None

        for attempt in range(1, max_retries + 1):
            try:
                if self.connection is None or self.connection.is_closed:
                    logger.warning(f"RabbitMQ 연결 끊어짐, 재연결 시도 (attempt={attempt})")
                    self.connect()

                self.channel.basic_publish(
                    exchange=settings.ANALYSIS_EXCHANGE,
                    routing_key=settings.RESULT_ROUTING_KEY,
                    body=message_body,
                    properties=pika.BasicProperties(
                        delivery_mode=2,
                        content_type='application/json'
                    ),
                    mandatory=True
                )

                logger.info(
                    f"✅ 분석 결과 발행 성공 (attempt={attempt}): "
                    f"roomId={result.room_id}, round={result.round}, "
                    f"memberId={result.member_id}"
                )
                return

            except pika.exceptions.UnroutableError:
                logger.error(
                    f"❌ 라우팅 실패 — Exchange '{settings.ANALYSIS_EXCHANGE}' / "
                    f"RoutingKey '{settings.RESULT_ROUTING_KEY}'에 바인딩된 Queue 없음. 재시도 무의미."
                )
                raise

            except (pika.exceptions.StreamLostError,
                    pika.exceptions.ConnectionClosed,
                    pika.exceptions.ChannelClosed,
                    ConnectionError) as e:
                last_err = e
                logger.warning(
                    f"publish 실패 (attempt={attempt}/{max_retries}): {type(e).__name__}. "
                    f"연결 재설정 후 재시도."
                )
                # 강제 reset — is_closed 체크로는 Windows TCP RST를 감지 못하는 경우 있음
                try:
                    if self.connection and not self.connection.is_closed:
                        self.connection.close()
                except Exception:
                    pass
                self.connection = None
                self.channel = None

            except Exception as e:
                logger.error(f"❌ 분석 결과 발행 실패 (비복구성): {e}", exc_info=True)
                raise

        logger.error(f"❌ 분석 결과 발행 최종 실패 ({max_retries}회 재시도 소진): {last_err}")
        raise last_err

    def close(self):
        """연결 종료"""
        if self.connection and not self.connection.is_closed:
            self.connection.close()
            logger.info("RabbitMQ Producer 연결 종료")


# ──────────────────────────────────────────────────────────────
# Thread-local Producer 팩토리
# pika.BlockingConnection은 생성 스레드에서만 사용 가능.
# Consumer 콜백 스레드가 최초 호출 시 해당 스레드 소유의 connection 생성.
# ──────────────────────────────────────────────────────────────
_tl = threading.local()


def get_producer() -> RabbitMQProducer:
    """현재 스레드 소유의 Producer 인스턴스 반환 (최초 호출 시 생성)"""
    if not hasattr(_tl, 'producer') or _tl.producer is None:
        logger.info(f"Thread {threading.get_ident()}: RabbitMQProducer 최초 생성")
        _tl.producer = RabbitMQProducer()
    return _tl.producer


def close_producer():
    """현재 스레드 Producer 연결 종료"""
    if hasattr(_tl, 'producer') and _tl.producer is not None:
        _tl.producer.close()
        _tl.producer = None
