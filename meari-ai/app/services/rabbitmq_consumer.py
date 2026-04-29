"""
RabbitMQ Consumer
Spring Boot로부터 분석 요청 수신
"""
import json
import logging
import pika
from app.config import settings
from app.schemas.request import AnalysisRequestMessage
from app.services.analysis_service import AnalysisService
from app.services.rabbitmq_producer import get_producer

logger = logging.getLogger(__name__)


class RabbitMQConsumer:
    """RabbitMQ 요청 수신자"""

    def __init__(self, analysis_service: AnalysisService):
        self.analysis_service = analysis_service
        self.connection = None
        self.channel = None
        self.connect()

    def connect(self):
        """RabbitMQ 연결 및 큐 설정"""
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
            
            # 1. Exchange 선언
            self.channel.exchange_declare(
                exchange=settings.ANALYSIS_EXCHANGE,
                exchange_type='direct',
                durable=True
            )

            # 2. Queue 선언 (요청/결과 큐 모두)
            self.channel.queue_declare(queue=settings.REQUEST_QUEUE, durable=True)
            self.channel.queue_declare(queue=settings.RESULT_QUEUE, durable=True)

            # 3. Queue와 Exchange 바인딩
            self.channel.queue_bind(
                exchange=settings.ANALYSIS_EXCHANGE,
                queue=settings.REQUEST_QUEUE,
                routing_key=settings.REQUEST_ROUTING_KEY
            )
            self.channel.queue_bind(
                exchange=settings.ANALYSIS_EXCHANGE,
                queue=settings.RESULT_QUEUE,
                routing_key=settings.RESULT_ROUTING_KEY
            )

            # 4. QoS 설정: 한 번에 하나의 메시지만 처리
            self.channel.basic_qos(prefetch_count=1)

            logger.info("RabbitMQ Consumer 연결 및 토폴로지 설정 성공")
        except Exception as e:
            logger.error(f"RabbitMQ 연결 실패: {e}")
            raise

    def callback(self, ch, method, properties, body):
        """메시지 수신 콜백"""
        try:
            # JSON 파싱
            message_dict = json.loads(body.decode('utf-8'))
            message = AnalysisRequestMessage(**message_dict)

            logger.info(
                f"분석 요청 수신: roomId={message.room_id}, "
                f"round={message.round}, memberId={message.member_id}, "
                f"sentences={len(message.sentences)}"
            )

            # 분석 수행
            result = self.analysis_service.analyze_member(message)

            # 결과 발행 (thread-local producer — 이 consumer 스레드가 소유한 connection 사용)
            get_producer().publish_result(result)

            # ACK
            ch.basic_ack(delivery_tag=method.delivery_tag)
            logger.info(f"분석 완료 및 ACK: memberId={message.member_id}")

        except Exception as e:
            logger.error(f"메시지 처리 실패: {e}", exc_info=True)
            # NACK: 재시도 큐로 이동 (Dead Letter Queue 설정 필요)
            ch.basic_nack(delivery_tag=method.delivery_tag, requeue=False)

    def start_consuming(self):
        """메시지 수신 시작"""
        try:
            self.channel.basic_consume(
                queue=settings.REQUEST_QUEUE,
                on_message_callback=self.callback
            )

            logger.info("RabbitMQ 메시지 수신 대기 중...")
            self.channel.start_consuming()
        except KeyboardInterrupt:
            logger.info("Consumer 중단")
            self.stop_consuming()
        except Exception as e:
            logger.error(f"Consumer 오류: {e}")
            raise

    def stop_consuming(self):
        """메시지 수신 중단"""
        if self.channel:
            self.channel.stop_consuming()
        if self.connection and not self.connection.is_closed:
            self.connection.close()
        logger.info("RabbitMQ Consumer 연결 종료")
