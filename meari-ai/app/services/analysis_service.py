"""
발음 분석 서비스
Wav2Vec2 + MDD를 사용한 발음 오류 탐지 및 점수 계산
"""
import json
import logging
import io
import torch
import torchaudio
import boto3
from typing import List, Dict, Any
from app.config import settings
from app.schemas.request import AnalysisRequestMessage, SentenceAnalysisInfo
from app.schemas.response import AnalysisResultMessage
from app.models.model_loader import (
    get_processor, get_asr_model, get_mdd_model, get_device
)

logger = logging.getLogger(__name__)


class AnalysisService:
    """발음 분석 서비스"""

    def __init__(self):
        self.s3_client = boto3.client(
            's3',
            aws_access_key_id=settings.AWS_ACCESS_KEY,
            aws_secret_access_key=settings.AWS_SECRET_KEY,
            region_name=settings.AWS_REGION
        )
        logger.info("AnalysisService 초기화 완료")

    def analyze_member(self, message: AnalysisRequestMessage) -> AnalysisResultMessage:
        """
        멤버의 모든 문장 분석

        Args:
            message: 분석 요청 메시지

        Returns:
            분석 결과 메시지
        """
        logger.info(
            f"멤버 분석 시작: roomId={message.room_id}, "
            f"memberId={message.member_id}, sentences={len(message.sentences)}"
        )

        sentence_errors = []

        for sentence_info in message.sentences:
            try:
                # 1. S3에서 오디오 다운로드
                audio_waveform = self.download_audio_from_s3(sentence_info.audio_url)

                # 2. 음성 전처리 (16kHz 변환)
                audio_waveform = self.preprocess_audio(audio_waveform)

                # 3. ASR + MDD 추론
                errors = self.detect_pronunciation_errors(
                    audio_waveform,
                    sentence_info.text_ko
                )

                sentence_errors.append({
                    "sentence_id": sentence_info.sentence_id,
                    "text": sentence_info.text_ko,
                    "errors": errors
                })

                logger.debug(f"문장 {sentence_info.sentence_id} 분석 완료")

            except Exception as e:
                logger.error(
                    f"문장 {sentence_info.sentence_id} 분석 실패: {e}",
                    exc_info=True
                )
                sentence_errors.append({
                    "sentence_id": sentence_info.sentence_id,
                    "text": sentence_info.text_ko,
                    "errors": [],
                    "error_message": str(e)
                })

        # 4. 점수 계산
        accuracy = self.calculate_accuracy(sentence_errors)
        intonation = self.calculate_intonation(sentence_errors)

        # 5. 결과 생성
        result = AnalysisResultMessage(
            room_id=message.room_id,
            round=message.round,
            member_id=message.member_id,
            accuracy=accuracy,
            intonation=intonation,
            detailed_analysis=json.dumps(sentence_errors, ensure_ascii=False)
        )

        logger.info(
            f"멤버 분석 완료: memberId={message.member_id}, "
            f"accuracy={accuracy}, intonation={intonation}"
        )

        return result

    def download_audio_from_s3(self, audio_url: str) -> torch.Tensor:
        """
        S3에서 오디오 파일 다운로드

        Args:
            audio_url: S3 URL (s3://bucket/key 형식)

        Returns:
            오디오 waveform (Tensor)
        """
        try:
            # s3://bucket/key 파싱
            if audio_url.startswith("s3://"):
                parts = audio_url.replace("s3://", "").split("/", 1)
                bucket = parts[0]
                key = parts[1]
            else:
                raise ValueError(f"Invalid S3 URL format: {audio_url}")

            logger.debug(f"S3 다운로드: bucket={bucket}, key={key}")

            # S3 객체 다운로드
            response = self.s3_client.get_object(Bucket=bucket, Key=key)
            audio_bytes = response['Body'].read()

            # 오디오 로드
            waveform, sample_rate = torchaudio.load(io.BytesIO(audio_bytes))

            logger.debug(f"오디오 로드 완료: sample_rate={sample_rate}, shape={waveform.shape}")

            return waveform

        except Exception as e:
            logger.error(f"S3 다운로드 실패: {e}")
            raise

    def preprocess_audio(self, waveform: torch.Tensor) -> torch.Tensor:
        """
        오디오 전처리 (16kHz 변환)

        Args:
            waveform: 원본 오디오

        Returns:
            전처리된 오디오
        """
        # 스테레오 → 모노 변환
        if waveform.shape[0] > 1:
            waveform = torch.mean(waveform, dim=0, keepdim=True)

        # 리샘플링 (현재 sample rate를 알 수 없으므로 가정 필요)
        # TODO: 실제 sample rate를 메타데이터로 전달받거나 파일에서 추출
        # 여기서는 16kHz로 이미 변환되어 있다고 가정

        return waveform

    def detect_pronunciation_errors(
        self,
        audio_waveform: torch.Tensor,
        text_ko: str
    ) -> List[Dict[str, Any]]:
        """
        발음 오류 탐지

        Args:
            audio_waveform: 오디오 waveform
            text_ko: 정답 텍스트

        Returns:
            발음 오류 목록
        """
        try:
            processor = get_processor()
            asr_model = get_asr_model()
            mdd_model = get_mdd_model()
            device = get_device()

            # 1. ASR: 음성 → 텍스트 변환
            inputs = processor(
                audio_waveform.squeeze().numpy(),
                sampling_rate=settings.SAMPLE_RATE,
                return_tensors="pt",
                padding=True
            )
            inputs = {k: v.to(device) for k, v in inputs.items()}

            with torch.no_grad():
                logits = asr_model(**inputs).logits
                predicted_ids = torch.argmax(logits, dim=-1)
                transcription = processor.batch_decode(predicted_ids)[0]

            logger.debug(f"ASR 결과: {transcription} (정답: {text_ko})")

            # 2. MDD: 발음 오류 탐지
            # TODO: 실제 MDD 모델 추론 로직 구현
            # Placeholder: 간단한 문자열 비교
            errors = []
            if transcription.lower().strip() != text_ko.lower().strip():
                errors.append({
                    "type": "pronunciation_error",
                    "expected": text_ko,
                    "actual": transcription,
                    "position": 0
                })

            return errors

        except Exception as e:
            logger.error(f"발음 오류 탐지 실패: {e}")
            return []

    def calculate_accuracy(self, sentence_errors: List[Dict[str, Any]]) -> int:
        """
        정확도 점수 계산 (0-100)

        Args:
            sentence_errors: 문장별 오류 정보

        Returns:
            정확도 점수
        """
        if not sentence_errors:
            return 0

        total_sentences = len(sentence_errors)
        error_count = sum(
            1 for s in sentence_errors if len(s.get("errors", [])) > 0
        )

        accuracy = int((total_sentences - error_count) / total_sentences * 100)
        return max(0, min(100, accuracy))

    def calculate_intonation(self, sentence_errors: List[Dict[str, Any]]) -> int:
        """
        억양 점수 계산 (0-100)

        Args:
            sentence_errors: 문장별 오류 정보

        Returns:
            억양 점수
        """
        # TODO: 실제 억양 분석 로직 구현
        # Placeholder: 정확도와 유사하게 계산
        return self.calculate_accuracy(sentence_errors)
