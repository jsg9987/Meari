"""
정답 오디오 관리
S3에서 다운로드 + 로컬 캐싱
"""
import os
import logging
import aioboto3
import torchaudio
import io
from typing import Tuple
import torch
from app.config import settings

logger = logging.getLogger(__name__)


class ReferenceAudioManager:
    """정답 오디오 다운로드 및 캐싱 관리"""

    def __init__(self):
        self.cache_dir = "/tmp/reference_audio_cache"
        os.makedirs(self.cache_dir, exist_ok=True)
        logger.info(f"ReferenceAudioManager 초기화: cache_dir={self.cache_dir}")

    async def get_reference_audio(
        self,
        s3_key: str
    ) -> Tuple[torch.Tensor, int]:
        """
        S3에서 정답 오디오 다운로드 (로컬 캐싱)

        Args:
            s3_key: S3 key (예: "reference_audio/1/123.wav")

        Returns:
            (waveform, sample_rate) 튜플
        """
        # 1. 로컬 캐시 확인
        cache_filename = s3_key.replace('/', '_')
        cache_path = os.path.join(self.cache_dir, cache_filename)

        if os.path.exists(cache_path):
            # 캐시 히트
            logger.debug(f"정답 오디오 캐시 히트: {s3_key}")
            return torchaudio.load(cache_path)

        # 2. S3에서 다운로드
        logger.debug(f"정답 오디오 S3 다운로드: {s3_key}")
        session = aioboto3.Session()
        async with session.client(
            's3',
            aws_access_key_id=settings.AWS_ACCESS_KEY,
            aws_secret_access_key=settings.AWS_SECRET_KEY,
            region_name=settings.AWS_REGION
        ) as s3:
            response = await s3.get_object(
                Bucket=settings.AWS_S3_BUCKET,
                Key=s3_key
            )
            audio_bytes = await response['Body'].read()

        # 3. 로컬에 저장 (캐싱)
        with open(cache_path, 'wb') as f:
            f.write(audio_bytes)

        # 4. 로드
        waveform, sample_rate = torchaudio.load(io.BytesIO(audio_bytes))

        logger.debug(f"정답 오디오 로드 완료: {s3_key}, sr={sample_rate}, shape={waveform.shape}")

        return waveform, sample_rate


# 싱글톤 인스턴스
reference_audio_manager = ReferenceAudioManager()
