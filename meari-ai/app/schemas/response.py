"""
RabbitMQ 응답 메시지 스키마
"""
from typing import List, Dict, Any, Optional
from pydantic import BaseModel, Field


# Intonation 관련 상세 스키마
class PitchStatistics(BaseModel):
    """Pitch 통계 정보"""
    mean: float = Field(..., description="평균 주파수 (Hz)")
    std: float = Field(..., description="표준편차 (Hz)")
    min: float = Field(..., description="최소 주파수 (Hz)")
    max: float = Field(..., description="최대 주파수 (Hz)")


class IntonationPitchData(BaseModel):
    """프론트엔드 차트용 Pitch 데이터 (길이 통일)"""
    reference: List[float] = Field(..., description="정답 pitch (100개, Hz)")
    user: List[float] = Field(..., description="사용자 pitch (100개, Hz)")
    time_points: List[float] = Field(..., description="시간 축 (100개, 초)")


class IntonationStatistics(BaseModel):
    """억양 통계 정보"""
    reference: PitchStatistics = Field(..., description="정답 음성 통계")
    user: PitchStatistics = Field(..., description="사용자 음성 통계")
    pitch_difference: float = Field(..., description="평균 주파수 차이 (Hz)")


class IntonationRawData(BaseModel):
    """원본 Pitch 데이터 (디버깅/상세 분석용)"""
    reference_pitch: List[float] = Field(..., description="정답 원본 pitch 배열")
    user_pitch: List[float] = Field(..., description="사용자 원본 pitch 배열")
    reference_frames: int = Field(..., description="정답 프레임 수")
    user_frames: int = Field(..., description="사용자 프레임 수")


class IntonationAnalysis(BaseModel):
    """억양 분석 결과"""
    score: int = Field(..., ge=-1, le=100, description="억양 점수 (0~100, -1=분석불가)")
    feedback: str = Field(..., description="억양 평가 피드백")
    pitch_data: IntonationPitchData = Field(..., description="차트용 pitch 데이터")
    statistics: IntonationStatistics = Field(..., description="통계 정보")
    raw_data: IntonationRawData = Field(..., description="원본 데이터")


# Pronunciation Error 스키마
class PronunciationError(BaseModel):
    """발음 오류 정보"""
    type: str = Field(..., description="오류 유형 (replace | delete | insert)")
    position: int = Field(..., description="오류 위치 인덱스")
    expected: str = Field(..., description="기대 발음")
    actual: str = Field(..., description="실제 인식된 발음")
    confidence: Optional[float] = Field(None, description="해당 구간 신뢰도")
    description: str = Field(..., description="오류 상세 설명")


class SentenceAnalysis(BaseModel):
    """문장별 분석 결과"""
    sentence_id: int = Field(..., description="문장 고유 ID")
    text_expected: str = Field(..., description="정답 텍스트")
    text_recognized: str = Field(..., description="인식된 텍스트")
    accuracy: int = Field(..., ge=0, le=100, description="발음 정확도 (%)")
    mean_confidence: float = Field(..., ge=0, le=1, description="평균 인식 신뢰도")
    syllables: List[str] = Field(..., description="인식된 음절 리스트")
    syllable_confidences: List[float] = Field(..., description="음절별 신뢰도")
    errors: List[PronunciationError] = Field(..., description="발음 오류 리스트")
    intonation: IntonationAnalysis = Field(..., description="억양 분석 결과")
    error_message: Optional[str] = Field(None, description="에러 메시지 (에러 발생 시)")


class AnalysisSummary(BaseModel):
    """분석 요약 정보"""
    total_sentences: int = Field(..., description="전체 문장 수")
    analyzed_sentences: int = Field(..., description="분석 완료된 문장 수")
    average_accuracy: int = Field(..., ge=0, le=100, description="평균 발음 정확도 (%)")
    average_confidence: float = Field(..., ge=0, le=1, description="평균 인식 신뢰도")
    average_intonation: int = Field(..., ge=-1, le=100, description="평균 억양 점수")


class DetailedAnalysis(BaseModel):
    """상세 분석 결과 (detailed_analysis JSON 구조)"""
    summary: AnalysisSummary = Field(..., description="분석 요약")
    sentences: List[SentenceAnalysis] = Field(..., description="문장별 상세 분석")


class AnalysisResultMessage(BaseModel):
    """발음 분석 결과 메시지"""
    room_id: int = Field(..., description="방 ID")
    round: int = Field(..., description="라운드")
    member_id: int = Field(..., description="멤버 ID")
    accuracy: int = Field(..., ge=0, le=100, description="전체 발음 정확도 (%)")
    intonation: int = Field(..., ge=-1, le=100, description="전체 억양 점수")
    detailed_analysis: str = Field(
        ...,
        description="상세 분석 결과 (JSON 문자열)",
        example='{"summary": {"total_sentences": 3, "analyzed_sentences": 3, "average_accuracy": 85, "average_confidence": 0.92, "average_intonation": 94}, "sentences": [...]}'
    )
