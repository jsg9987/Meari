# test_publish.py
import pika
import json
import sys
import os

# 프로젝트 루트 경로를 sys.path에 추가
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..')))

from app.config import settings

# RabbitMQ 연결
credentials = pika.PlainCredentials(settings.RABBITMQ_USERNAME, settings.RABBITMQ_PASSWORD)
connection = pika.BlockingConnection(
    pika.ConnectionParameters(
        host=settings.RABBITMQ_HOST,
        port=settings.RABBITMQ_PORT,
        virtual_host=settings.RABBITMQ_VIRTUAL_HOST,
        credentials=credentials
    )
)
channel = connection.channel()

# 메시지 발행 전 Exchange와 Queue 설정
channel.exchange_declare(
    exchange=settings.ANALYSIS_EXCHANGE,
    exchange_type='direct',
    durable=True
)

# Request Queue 선언 및 바인딩
channel.queue_declare(queue=settings.REQUEST_QUEUE, durable=True)
channel.queue_bind(
    exchange=settings.ANALYSIS_EXCHANGE,
    queue=settings.REQUEST_QUEUE,
    routing_key=settings.REQUEST_ROUTING_KEY
)

print(f"Exchange: {settings.ANALYSIS_EXCHANGE}")
print(f"Queue: {settings.REQUEST_QUEUE}")
print(f"Routing Key: {settings.REQUEST_ROUTING_KEY}")

# 테스트 메시지
test_message = {
    "room_id": 1,
    "round": 1,
    "content_id": 1,
    "member_id": 1,
    "role_id": 1,
    "sentences": [
        {
            "sentence_id": 1,
            "audio_url": "s3://meari-bucket/recordings/audio.wav",
            "text_ko": "안녕하세요",
            "start_time": 0.0,
            "end_time": 2.0
        }
    ]
}

print(f"\n발행할 메시지:")
print(json.dumps(test_message, indent=2, ensure_ascii=False))

channel.basic_publish(
    exchange=settings.ANALYSIS_EXCHANGE,
    routing_key=settings.REQUEST_ROUTING_KEY,
    body=json.dumps(test_message),
    properties=pika.BasicProperties(
        delivery_mode=2,  # 메시지 영속성
        content_type='application/json'
    )
)
print(f"\n✅ 테스트 메시지 발행 완료!")
print(f"   → Exchange: {settings.ANALYSIS_EXCHANGE}")
print(f"   → Routing Key: {settings.REQUEST_ROUTING_KEY}")
connection.close()