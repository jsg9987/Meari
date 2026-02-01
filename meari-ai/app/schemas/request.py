"""
RabbitMQ 요청 메시지 스키마
"""
from typing import List
from pydantic import BaseModel


class SentenceAnalysisInfo(BaseModel):
    """문장별 분석 정보"""
    sentence_id: int
    audio_url: str
    text_ko: str
    start_time: float
    end_time: float
    reference_audio_key: str  # S3 key for reference audio


class AnalysisRequestMessage(BaseModel):
    """멤버별 발음 분석 요청 메시지"""
    room_id: int
    round: int
    content_id: int
    member_id: int
    role_id: int
    sentences: List[SentenceAnalysisInfo]
