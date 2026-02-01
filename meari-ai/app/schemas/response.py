"""
RabbitMQ 응답 메시지 스키마
"""
from pydantic import BaseModel


class AnalysisResultMessage(BaseModel):
    """발음 분석 결과 메시지"""
    room_id: int
    round: int
    member_id: int
    accuracy: int
    intonation: int
    detailed_analysis: str  # JSON string
