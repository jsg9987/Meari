"""
RabbitMQ Producer
FastAPI에서 Spring Boot로 분석 결과 전송
"""
import json
import logging
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

    def publish_result(self, result: AnalysisResultMessage):
        """분석 결과 발행"""
        try:
            # 재연결 확인
            if self.connection is None or self.connection.is_closed:
                logger.warning("RabbitMQ 연결이 끊어짐, 재연결 시도")
                self.connect()

            message_body = result.model_dump_json()

            logger.debug(
                f"메시지 발행 시도: Exchange={settings.ANALYSIS_EXCHANGE}, "
                f"RoutingKey={settings.RESULT_ROUTING_KEY}, "
                f"BodySize={len(message_body)} bytes"
            )

            # basic_publish는 confirm_delivery()가 설정되어 있으면
            # 메시지가 라우팅되지 않으면 UnroutableError 예외 발생
            self.channel.basic_publish(
                exchange=settings.ANALYSIS_EXCHANGE,
                routing_key=settings.RESULT_ROUTING_KEY,
                body=message_body,
                properties=pika.BasicProperties(
                    delivery_mode=2,  # 메시지 영속성
                    content_type='application/json'
                ),
                mandatory=True  # 라우팅 실패 시 에러 발생
            )

            logger.info(
                f"✅ 분석 결과 발행 성공 (라우팅 확인됨): "
                f"roomId={result.room_id}, round={result.round}, "
                f"memberId={result.member_id}"
            )
        except pika.exceptions.UnroutableError:
            logger.error(
                f"❌ 메시지 라우팅 실패! Exchange '{settings.ANALYSIS_EXCHANGE}'에서 "
                f"RoutingKey '{settings.RESULT_ROUTING_KEY}'로 라우팅할 Queue가 없습니다."
            )
            raise
        except Exception as e:
            logger.error(f"❌ 분석 결과 발행 실패: {e}", exc_info=True)
            raise

    def close(self):
        """연결 종료"""
        if self.connection and not self.connection.is_closed:
            self.connection.close()
            logger.info("RabbitMQ Producer 연결 종료")


# 전역 Producer 인스턴스
producer = RabbitMQProducer()
