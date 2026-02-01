"""
발음 분석 서비스
Wav2Vec2 ASR을 사용한 한국어 발음 분석 및 점수 계산

주요 기능:
- S3에서 오디오 병렬 다운로드 (aioboto3)
- Wav2Vec2 ASR 추론 (자모 단위 인식)
- 자모 → 음절 변환 및 confidence 계산
- 정답 대비 정확도 계산 (edit distance 기반)
"""
import json
import logging
import io
import asyncio
import torch
import torch.nn.functional as F
import torchaudio
import numpy as np
import aioboto3
from typing import List, Dict, Any, Tuple, Optional
from app.config import settings
from app.schemas.request import AnalysisRequestMessage, SentenceAnalysisInfo
from app.schemas.response import AnalysisResultMessage
from app.models.model_loader import (
    get_processor, get_asr_model, get_mdd_model, get_device
)
from app.utils.korean_utils import (
    jamo_to_syllables_with_probs,
    calculate_accuracy_from_syllables,
    strip_spaces
)

logger = logging.getLogger(__name__)


class AnalysisService:
    """발음 분석 서비스"""

    def __init__(self):
        logger.info("AnalysisService 초기화 완료")

    def analyze_member(self, message: AnalysisRequestMessage) -> AnalysisResultMessage:
        """
        멤버의 모든 문장 분석

        Spring Boot에서 받은 분석 요청을 처리하고 결과 반환

        Args:
            message: 분석 요청 메시지 (roomId, memberId, sentences 등)

        Returns:
            분석 결과 메시지 (accuracy, intonation, detailed_analysis)
        """
        logger.info(
            f"멤버 분석 시작: roomId={message.room_id}, "
            f"memberId={message.member_id}, sentences={len(message.sentences)}"
        )

        # 1. S3에서 모든 오디오 병렬 다운로드
        logger.debug(f"S3 병렬 다운로드 시작: {len(message.sentences)}개 파일")
        audio_data_list = asyncio.run(
            self.download_all_audios_parallel(message.sentences)
        )
        logger.debug("S3 병렬 다운로드 완료")

        # 2. 각 문장 분석 (순차 처리)
        sentence_results = []
        total_accuracy = 0
        total_confidence = 0
        valid_count = 0

        for idx, sentence_info in enumerate(message.sentences):
            try:
                audio_waveform, sample_rate = audio_data_list[idx]

                if audio_waveform is None:
                    raise Exception("오디오 다운로드 실패")

                # 음성 전처리 (16kHz 모노 변환)
                audio_waveform = self.preprocess_audio(audio_waveform, sample_rate)

                # ASR 추론 + 발음 분석
                analysis = self.detect_pronunciation_errors(
                    audio_waveform,
                    sentence_info.text_ko
                )

                # 문장 결과 저장
                sentence_result = {
                    "sentence_id": sentence_info.sentence_id,
                    "text_expected": sentence_info.text_ko,
                    "text_recognized": analysis.get("transcription", ""),
                    "accuracy": analysis.get("accuracy", 0),
                    "mean_confidence": analysis.get("mean_confidence", 0.0),
                    "syllables": analysis.get("syllables", []),
                    "syllable_confidences": analysis.get("syllable_confidences", []),
                    "errors": analysis.get("errors", [])
                }

                # 에러 메시지 있으면 추가
                if "error_message" in analysis:
                    sentence_result["error_message"] = analysis["error_message"]

                sentence_results.append(sentence_result)

                # 통계 누적
                total_accuracy += analysis.get("accuracy", 0)
                total_confidence += analysis.get("mean_confidence", 0.0)
                valid_count += 1

                logger.debug(
                    f"문장 {sentence_info.sentence_id} 분석 완료: "
                    f"accuracy={analysis.get('accuracy', 0)}, "
                    f"confidence={analysis.get('mean_confidence', 0.0):.3f}"
                )

            except Exception as e:
                logger.error(
                    f"문장 {sentence_info.sentence_id} 분석 실패: {e}",
                    exc_info=True
                )
                sentence_results.append({
                    "sentence_id": sentence_info.sentence_id,
                    "text_expected": sentence_info.text_ko,
                    "text_recognized": "",
                    "accuracy": 0,
                    "mean_confidence": 0.0,
                    "syllables": [],
                    "syllable_confidences": [],
                    "errors": [],
                    "error_message": str(e)
                })

        # 3. 전체 점수 계산
        if valid_count > 0:
            accuracy = int(round(total_accuracy / valid_count))
            avg_confidence = total_confidence / valid_count
        else:
            accuracy = 0
            avg_confidence = 0.0

        # 억양 점수 (현재는 confidence 기반으로 계산)
        intonation = self.calculate_intonation_score(sentence_results)

        # 4. detailed_analysis JSON 구성
        detailed = {
            "sentences": sentence_results,
            "summary": {
                "total_sentences": len(message.sentences),
                "analyzed_sentences": valid_count,
                "average_accuracy": accuracy,
                "average_confidence": round(avg_confidence, 4)
            }
        }

        # 5. 결과 생성
        result = AnalysisResultMessage(
            room_id=message.room_id,
            round=message.round,
            member_id=message.member_id,
            accuracy=accuracy,
            intonation=intonation,
            detailed_analysis=json.dumps(detailed, ensure_ascii=False)
        )

        logger.info(
            f"멤버 분석 완료: memberId={message.member_id}, "
            f"accuracy={accuracy}, intonation={intonation}, "
            f"analyzed={valid_count}/{len(message.sentences)}"
        )

        return result

    async def download_all_audios_parallel(
        self,
        sentences: List[SentenceAnalysisInfo]
    ) -> List[Tuple[Optional[torch.Tensor], int]]:
        """
        모든 문장의 오디오를 S3에서 병렬로 다운로드

        Args:
            sentences: 문장 정보 리스트

        Returns:
            (waveform, sample_rate) 튜플 리스트
            다운로드 실패 시 (None, 0) 반환
        """
        tasks = [
            self.download_audio_from_s3_async(sentence.audio_url, sentence.sentence_id)
            for sentence in sentences
        ]

        results = await asyncio.gather(*tasks, return_exceptions=True)

        # 결과 처리 (예외는 None으로 변환)
        audio_data_list = []
        for idx, result in enumerate(results):
            if isinstance(result, Exception):
                logger.error(
                    f"문장 {sentences[idx].sentence_id} 오디오 다운로드 실패: {result}"
                )
                audio_data_list.append((None, 0))
            else:
                audio_data_list.append(result)

        return audio_data_list

    async def download_audio_from_s3_async(
        self,
        audio_url: str,
        sentence_id: int
    ) -> Tuple[torch.Tensor, int]:
        """
        S3에서 오디오 파일 비동기 다운로드

        Args:
            audio_url: S3 URL (s3://bucket/key 형식) 또는 HTTP URL
            sentence_id: 문장 ID (로깅용)

        Returns:
            (오디오 waveform, sample_rate) 튜플
        """
        try:
            # s3://bucket/key 파싱
            if audio_url.startswith("s3://"):
                parts = audio_url.replace("s3://", "").split("/", 1)
                bucket = parts[0]
                key = parts[1]
            elif audio_url.startswith("https://") and ".s3." in audio_url:
                from urllib.parse import urlparse
                parsed = urlparse(audio_url)
                bucket = parsed.netloc.split('.')[0]
                key = parsed.path.lstrip('/')
            else:
                raise ValueError(f"Invalid S3 URL format: {audio_url}")

            # 파일 확장자 추출
            file_extension = key.split('.')[-1].lower()
            if file_extension not in ['wav', 'mp3', 'flac', 'ogg']:
                format_hint = None
            else:
                format_hint = file_extension

            # aioboto3로 비동기 다운로드
            session = aioboto3.Session()
            async with session.client(
                's3',
                aws_access_key_id=settings.AWS_ACCESS_KEY,
                aws_secret_access_key=settings.AWS_SECRET_KEY,
                region_name=settings.AWS_REGION
            ) as s3_client:
                response = await s3_client.get_object(Bucket=bucket, Key=key)
                audio_bytes = await response['Body'].read()

            # 오디오 로드 (동기 작업)
            waveform, sample_rate = torchaudio.load(
                io.BytesIO(audio_bytes),
                format=format_hint
            )

            return waveform, sample_rate

        except Exception as e:
            logger.error(f"문장 {sentence_id} S3 비동기 다운로드 실패: {e}")
            raise

    def preprocess_audio(
        self,
        waveform: torch.Tensor,
        original_sample_rate: int = 16000
    ) -> torch.Tensor:
        """
        오디오 전처리 (16kHz 모노 변환)

        Args:
            waveform: 원본 오디오
            original_sample_rate: 원본 샘플 레이트

        Returns:
            전처리된 오디오 (16kHz, 모노)
        """
        # 스테레오 → 모노 변환
        if waveform.shape[0] > 1:
            waveform = torch.mean(waveform, dim=0, keepdim=True)

        # 리샘플링 (16kHz가 아닌 경우)
        if original_sample_rate != settings.SAMPLE_RATE:
            resampler = torchaudio.transforms.Resample(
                orig_freq=original_sample_rate,
                new_freq=settings.SAMPLE_RATE
            )
            waveform = resampler(waveform)
            logger.debug(
                f"리샘플링: {original_sample_rate}Hz → {settings.SAMPLE_RATE}Hz"
            )

        return waveform

    def asr_with_confidence(
        self,
        audio_waveform: torch.Tensor
    ) -> Tuple[str, List[str], List[float]]:
        """
        Wav2Vec2 ASR 추론 + Greedy Decode with Confidence

        CTC logits에서 greedy decoding하면서 각 토큰의 probability 추출

        Args:
            audio_waveform: 전처리된 오디오 (16kHz, 모노)

        Returns:
            (raw_jamo: 자모 문자열, tokens: 토큰 리스트, probs: 토큰별 확률)
        """
        processor = get_processor()
        asr_model = get_asr_model()
        device = get_device()

        # 입력 텐서 생성
        inputs = processor(
            audio_waveform.squeeze().numpy(),
            sampling_rate=settings.SAMPLE_RATE,
            return_tensors="pt",
            padding=True
        )
        input_values = inputs.input_values.to(device)

        # 추론
        with torch.no_grad():
            logits = asr_model(input_values).logits  # (1, T, vocab_size)

        # Softmax로 확률 변환
        probs = F.softmax(logits, dim=-1)  # (1, T, vocab_size)

        # Greedy decoding
        predicted_ids = torch.argmax(logits, dim=-1)  # (1, T)
        predicted_ids = predicted_ids.squeeze(0).cpu().numpy()  # (T,)
        probs = probs.squeeze(0).cpu().numpy()  # (T, vocab_size)

        # CTC blank token (보통 0번)
        blank_id = processor.tokenizer.pad_token_id or 0

        # 토큰과 확률 추출 (blank 제거, 연속 중복 제거)
        tokens = []
        token_probs = []
        prev_id = None

        for t, token_id in enumerate(predicted_ids):
            if token_id == blank_id:
                continue
            if token_id == prev_id:
                continue

            # 토큰 문자 변환
            token_str = processor.tokenizer.decode([int(token_id)])
            token_prob = float(probs[t, token_id])

            tokens.append(token_str)
            token_probs.append(token_prob)
            prev_id = token_id

        # 전체 문자열
        raw_jamo = ''.join(tokens)

        logger.debug(f"ASR 자모 출력: {raw_jamo}")
        logger.debug(f"토큰 수: {len(tokens)}, 평균 confidence: {np.mean(token_probs) if token_probs else 0:.3f}")

        return raw_jamo, tokens, token_probs

    def detect_pronunciation_errors(
        self,
        audio_waveform: torch.Tensor,
        text_ko: str
    ) -> Dict[str, Any]:
        """
        발음 오류 탐지 및 상세 분석

        ASR 추론 → 자모→음절 변환 → 정답 비교 → 음절별 분석 결과

        Args:
            audio_waveform: 전처리된 오디오 waveform
            text_ko: 정답 텍스트 (한국어)

        Returns:
            상세 분석 결과 딕셔너리:
            {
                "transcription": 인식된 음절 텍스트,
                "syllables": 음절 리스트,
                "syllable_confidences": 음절별 confidence,
                "accuracy": 정확도 (0-100),
                "mean_confidence": 평균 confidence,
                "errors": 오류 상세 리스트
            }
        """
        try:
            # 1. ASR 추론 (자모 + confidence)
            raw_jamo, tokens, token_probs = self.asr_with_confidence(audio_waveform)

            # 2. 자모 → 음절 변환
            syll_text, syll_list, syll_probs = jamo_to_syllables_with_probs(
                raw_jamo, token_probs
            )

            logger.debug(f"음절 변환 결과: {syll_text} (정답: {text_ko})")

            # 3. 정확도 계산 (edit distance 기반)
            accuracy = calculate_accuracy_from_syllables(text_ko, syll_text)

            # 4. 평균 confidence 계산
            mean_conf = float(np.mean(syll_probs)) if syll_probs else 1.0

            # 5. 음절별 오류 분석
            errors = self.analyze_syllable_errors(text_ko, syll_text, syll_probs)

            return {
                "transcription": syll_text,
                "syllables": syll_list,
                "syllable_confidences": [round(p, 4) for p in syll_probs],
                "accuracy": accuracy,
                "mean_confidence": round(mean_conf, 4),
                "errors": errors
            }

        except Exception as e:
            logger.error(f"발음 오류 탐지 실패: {e}", exc_info=True)
            return {
                "transcription": "",
                "syllables": [],
                "syllable_confidences": [],
                "accuracy": 0,
                "mean_confidence": 0.0,
                "errors": [],
                "error_message": str(e)
            }

    def analyze_syllable_errors(
        self,
        answer: str,
        predicted: str,
        predicted_probs: List[float]
    ) -> List[Dict[str, Any]]:
        """
        정답과 예측 결과를 음절 단위로 비교하여 오류 분석

        Args:
            answer: 정답 텍스트
            predicted: ASR 인식 결과
            predicted_probs: 인식된 음절별 confidence

        Returns:
            오류 상세 리스트
        """
        from difflib import SequenceMatcher

        ans = strip_spaces(answer)
        hyp = strip_spaces(predicted)

        errors = []
        sm = SequenceMatcher(a=list(ans), b=list(hyp))
        ops = sm.get_opcodes()

        for tag, i1, i2, j1, j2 in ops:
            if tag == 'equal':
                continue

            error_info = {
                "type": tag,
                "position": i1,
                "expected": ans[i1:i2] if i1 < i2 else "",
                "actual": hyp[j1:j2] if j1 < j2 else ""
            }

            # 해당 위치의 confidence (있으면)
            if j1 < len(predicted_probs):
                error_info["confidence"] = round(predicted_probs[j1], 4)

            # 오류 유형 상세화
            if tag == 'replace':
                error_info["description"] = f"'{ans[i1:i2]}' → '{hyp[j1:j2]}' 대체"
            elif tag == 'delete':
                error_info["description"] = f"'{ans[i1:i2]}' 누락"
            elif tag == 'insert':
                error_info["description"] = f"'{hyp[j1:j2]}' 삽입"

            errors.append(error_info)

        return errors

    def calculate_intonation_score(
        self,
        sentence_results: List[Dict[str, Any]]
    ) -> int:
        """
        억양 점수 계산 (0-100)

        현재 구현: ASR confidence를 억양 점수의 proxy로 사용
        (confidence가 높으면 발화가 명확하고 자연스러웠다고 가정)

        TODO: 실제 억양 분석을 위해서는 prosody 분석 필요
        - pitch contour 분석
        - duration/rhythm 분석
        - energy 패턴 분석

        Args:
            sentence_results: 문장별 분석 결과

        Returns:
            억양 점수 (0-100)
        """
        if not sentence_results:
            return 0

        confidences = []
        for result in sentence_results:
            conf = result.get("mean_confidence", 0.0)
            if conf > 0:
                confidences.append(conf)

        if not confidences:
            return 50  # 기본값

        # confidence를 0-100 점수로 변환
        # confidence는 보통 0.5~1.0 범위이므로 스케일 조정
        avg_conf = float(np.mean(confidences))

        # [0.5, 1.0] → [0, 100] 변환 (0.5 미만은 0점, 1.0은 100점)
        # 더 관대한 스케일: [0.3, 0.95] → [0, 100]
        min_conf = 0.3
        max_conf = 0.95

        if avg_conf <= min_conf:
            score = 0
        elif avg_conf >= max_conf:
            score = 100
        else:
            score = int((avg_conf - min_conf) / (max_conf - min_conf) * 100)

        return max(0, min(100, score))
