# app/test/check_queues.py
"""RabbitMQ 큐 상태 확인 스크립트"""
import pika
import sys
import os

# 프로젝트 루트 경로를 sys.path에 추가
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..')))

from app.config import settings

def check_queues():
    """RabbitMQ 큐 상태를 확인합니다."""
    print("=" * 60)
    print("RabbitMQ 큐 상태 확인")
    print("=" * 60)

    try:
        credentials = pika.PlainCredentials(
            settings.RABBITMQ_USERNAME,
            settings.RABBITMQ_PASSWORD
        )
        connection = pika.BlockingConnection(
            pika.ConnectionParameters(
                host=settings.RABBITMQ_HOST,
                port=settings.RABBITMQ_PORT,
                virtual_host=settings.RABBITMQ_VIRTUAL_HOST,
                credentials=credentials
            )
        )
        channel = connection.channel()

        print(f"\n✅ RabbitMQ 연결 성공")
        print(f"   Host: {settings.RABBITMQ_HOST}:{settings.RABBITMQ_PORT}")
        print(f"   Virtual Host: {settings.RABBITMQ_VIRTUAL_HOST}")

        # Exchange 확인
        print(f"\n📦 Exchange: {settings.ANALYSIS_EXCHANGE}")

        # REQUEST_QUEUE 상태
        try:
            queue_state = channel.queue_declare(
                queue=settings.REQUEST_QUEUE,
                durable=True,
                passive=True
            )
            print(f"\n📥 Request Queue: {settings.REQUEST_QUEUE}")
            print(f"   - 대기 중인 메시지: {queue_state.method.message_count}개")
            print(f"   - Consumer 수: {queue_state.method.consumer_count}개")
        except pika.exceptions.ChannelClosedByBroker:
            print(f"\n⚠️ Request Queue '{settings.REQUEST_QUEUE}' 없음")
            channel = connection.channel()  # 채널 재생성

        # RESULT_QUEUE 상태
        try:
            queue_state = channel.queue_declare(
                queue=settings.RESULT_QUEUE,
                durable=True,
                passive=True
            )
            print(f"\n📤 Result Queue: {settings.RESULT_QUEUE}")
            print(f"   - 대기 중인 메시지: {queue_state.method.message_count}개")
            print(f"   - Consumer 수: {queue_state.method.consumer_count}개")
        except pika.exceptions.ChannelClosedByBroker:
            print(f"\n⚠️ Result Queue '{settings.RESULT_QUEUE}' 없음")

        connection.close()
        print("\n" + "=" * 60)

    except pika.exceptions.ProbableAuthenticationError:
        print("\n❌ RabbitMQ 인증 실패")
        print("   .env 파일의 RABBITMQ_USERNAME과 RABBITMQ_PASSWORD를 확인하세요.")
    except Exception as e:
        print(f"\n❌ 오류 발생: {e}")
        import traceback
        traceback.print_exc()


if __name__ == '__main__':
    check_queues()
