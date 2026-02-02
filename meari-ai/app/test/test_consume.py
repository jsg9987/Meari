# app/test/test_consume.py
import pika
import json
import sys
import os

# 프로젝트 루트 경로를 sys.path에 추가
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..')))

from app.config import settings

def main():
    """RabbitMQ에서 분석 결과 메시지를 수신하여 출력합니다."""
    print("=" * 60)
    print("RabbitMQ 결과 메시지 Consumer 시작")
    print("=" * 60)

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

    # Exchange와 Queue를 선언하고 바인딩합니다. (멱등성이 보장되므로 안전)
    channel.exchange_declare(exchange=settings.ANALYSIS_EXCHANGE, exchange_type='direct', durable=True)
    channel.queue_declare(queue=settings.RESULT_QUEUE, durable=True)
    channel.queue_bind(
        exchange=settings.ANALYSIS_EXCHANGE,
        queue=settings.RESULT_QUEUE,
        routing_key=settings.RESULT_ROUTING_KEY
    )

    # 큐에 있는 메시지 개수 확인
    queue_state = channel.queue_declare(queue=settings.RESULT_QUEUE, durable=True, passive=True)
    message_count = queue_state.method.message_count

    print(f"\n연결 정보:")
    print(f"  - Host: {settings.RABBITMQ_HOST}:{settings.RABBITMQ_PORT}")
    print(f"  - Exchange: {settings.ANALYSIS_EXCHANGE}")
    print(f"  - Queue: {settings.RESULT_QUEUE}")
    print(f"  - Routing Key: {settings.RESULT_ROUTING_KEY}")
    print(f"  - 대기 중인 메시지: {message_count}개")
    print(f"\n[*] 메시지를 기다립니다. 종료하려면 CTRL+C를 누르세요.")
    print("=" * 60)

    def callback(ch, method, properties, body):
        """메시지 수신 시 호출되는 콜백 함수"""
        print("\n" + "=" * 60)
        print("📩 분석 결과 메시지 수신")
        print("=" * 60)

        try:
            message_data = json.loads(body)

            print(f"\n✅ 메시지 파싱 성공!")
            print(f"\n📊 분석 결과 요약:")
            print(f"  - Room ID: {message_data.get('room_id')}")
            print(f"  - Round: {message_data.get('round')}")
            print(f"  - Member ID: {message_data.get('member_id')}")
            print(f"  - Accuracy: {message_data.get('accuracy')}/100")
            print(f"  - Intonation: {message_data.get('intonation')}/100")

            print(f"\n📝 전체 메시지:")
            print(json.dumps(message_data, indent=2, ensure_ascii=False))

            # detailed_analysis가 있으면 파싱해서 출력
            if 'detailed_analysis' in message_data:
                try:
                    detailed = json.loads(message_data['detailed_analysis'])
                    print(f"\n🔍 상세 분석 (detailed_analysis):")
                    print(json.dumps(detailed, indent=2, ensure_ascii=False))
                except:
                    print(f"\n⚠️ detailed_analysis 파싱 실패 (문자열 그대로 출력):")
                    print(message_data['detailed_analysis'][:500])  # 처음 500자만

        except json.JSONDecodeError as e:
            print(f"\n❌ JSON 파싱 실패: {e}")
            print(f"수신된 원본 메시지:")
            print(body.decode())
        except Exception as e:
            print(f"\n❌ 메시지 처리 중 오류: {e}")
            import traceback
            traceback.print_exc()

        # 메시지를 정상적으로 처리했음을 RabbitMQ에 알림 (ACK)
        ch.basic_ack(delivery_tag=method.delivery_tag)

        # 메시지를 하나 처리한 후 소비를 중단하고 프로그램을 종료
        print("\n" + "=" * 60)
        print("✅ 메시지 처리 완료 - 프로그램 종료")
        print("=" * 60)
        ch.stop_consuming()

    # 소비 시작
    channel.basic_consume(
        queue=settings.RESULT_QUEUE,
        on_message_callback=callback
    )

    try:
        channel.start_consuming()
    except KeyboardInterrupt:
        print("\n사용자에 의해 프로그램이 중단되었습니다.")
    finally:
        if connection.is_open:
            connection.close()
            print("[*] RabbitMQ 연결이 종료되었습니다.")


if __name__ == '__main__':
    try:
        main()
    except pika.exceptions.ProbableAuthenticationError:
        print("\n[!] RabbitMQ 인증에 실패했습니다.")
        print("    .env 파일에 RABBITMQ_USERNAME과 RABBITMQ_PASSWORD가 정확한지 확인해주세요.")
    except Exception as e:
        print(f"\n[!] 오류가 발생했습니다: {e}")
