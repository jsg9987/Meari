# app/test/debug_rabbitmq.py
"""RabbitMQ 바인딩 상태 및 메시지 확인 스크립트"""
import pika
import sys
import os
import json

sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..')))
from app.config import settings

def main():
    print("=" * 70)
    print("RabbitMQ 디버깅 - Exchange/Queue/Binding 확인")
    print("=" * 70)

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

    print(f"\n✅ RabbitMQ 연결 성공\n")

    # 1. Exchange 확인
    print(f"📦 Exchange: {settings.ANALYSIS_EXCHANGE}")
    try:
        channel.exchange_declare(
            exchange=settings.ANALYSIS_EXCHANGE,
            exchange_type='direct',
            durable=True,
            passive=True  # 확인만
        )
        print(f"   ✅ Exchange 존재함\n")
    except Exception as e:
        print(f"   ❌ Exchange 없음: {e}\n")

    # 2. REQUEST_QUEUE 확인
    print(f"📥 Request Queue: {settings.REQUEST_QUEUE}")
    try:
        result = channel.queue_declare(
            queue=settings.REQUEST_QUEUE,
            durable=True,
            passive=True
        )
        print(f"   ✅ Queue 존재함")
        print(f"   - 메시지 수: {result.method.message_count}")
        print(f"   - Consumer 수: {result.method.consumer_count}\n")
    except Exception as e:
        print(f"   ❌ Queue 없음: {e}\n")

    # 3. RESULT_QUEUE 확인 및 메시지 읽기
    print(f"📤 Result Queue: {settings.RESULT_QUEUE}")
    try:
        result = channel.queue_declare(
            queue=settings.RESULT_QUEUE,
            durable=True,
            passive=True
        )
        print(f"   ✅ Queue 존재함")
        print(f"   - 메시지 수: {result.method.message_count}")
        print(f"   - Consumer 수: {result.method.consumer_count}")

        # 메시지가 있으면 하나 읽어보기 (소비하지 않고 peek)
        if result.method.message_count > 0:
            print(f"\n   🔍 메시지 내용 확인 (첫 번째 메시지):")
            method_frame, header_frame, body = channel.basic_get(
                queue=settings.RESULT_QUEUE,
                auto_ack=False
            )
            if method_frame:
                print(f"      Routing Key: {method_frame.routing_key}")
                print(f"      Content Type: {header_frame.content_type}")
                try:
                    msg = json.loads(body.decode())
                    print(f"      내용:\n{json.dumps(msg, indent=6, ensure_ascii=False)}")
                except:
                    print(f"      원본: {body.decode()[:200]}")

                # 메시지 다시 돌려놓기 (NACK with requeue)
                channel.basic_nack(delivery_tag=method_frame.delivery_tag, requeue=True)
                print(f"\n   ℹ️ 메시지를 다시 큐에 돌려놓았습니다.")
        else:
            print(f"\n   ℹ️ 큐에 메시지가 없습니다.")
    except Exception as e:
        print(f"   ❌ Queue 없음 또는 오류: {e}")

    print("\n" + "=" * 70)
    connection.close()


if __name__ == '__main__':
    main()
