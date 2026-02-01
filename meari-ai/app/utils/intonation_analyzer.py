"""
억양 분석 유틸리티
Pitch Contour 추출 및 DTW 비교
"""
import logging
import numpy as np
import librosa
import torch
from typing import Dict, Any, List, Tuple
from fastdtw import fastdtw
from scipy.spatial.distance import euclidean

logger = logging.getLogger(__name__)


def analyze_intonation(
    reference_audio: torch.Tensor,
    user_audio: torch.Tensor,
    sample_rate: int = 16000
) -> Dict[str, Any]:
    """
    정답 오디오와 사용자 오디오의 억양 비교

    Args:
        reference_audio: 정답 오디오 waveform
        user_audio: 사용자 오디오 waveform
        sample_rate: 샘플 레이트

    Returns:
        억양 분석 결과 딕셔너리
    """
    try:
        # 1. Pitch 추출
        ref_pitch = extract_pitch(reference_audio, sample_rate)
        user_pitch = extract_pitch(user_audio, sample_rate)

        # 2. DTW로 시간 정렬 및 유사도 계산
        distance, path = compute_dtw(ref_pitch, user_pitch)

        # 3. 억양 점수 계산 (0-100)
        score = calculate_intonation_score(distance, len(ref_pitch))

        # 4. 시간 프레임 생성
        hop_length = 512
        time_frames = librosa.frames_to_time(
            np.arange(len(ref_pitch)),
            sr=sample_rate,
            hop_length=hop_length
        )

        # 5. 피드백 생성
        feedback = generate_feedback(ref_pitch, user_pitch, path, score)

        return {
            "score": int(score),
            "reference_pitch": ref_pitch.tolist(),
            "user_pitch": user_pitch.tolist(),
            "time_frames": time_frames.tolist(),
            "dtw_path": path,
            "feedback": feedback
        }

    except Exception as e:
        logger.error(f"억양 분석 실패: {e}", exc_info=True)
        return {
            "score": 0,
            "reference_pitch": [],
            "user_pitch": [],
            "time_frames": [],
            "dtw_path": [],
            "feedback": "억양 분석 실패"
        }


def extract_pitch(
    audio: torch.Tensor,
    sample_rate: int = 16000
) -> np.ndarray:
    """
    오디오에서 Pitch (F0) 추출

    Args:
        audio: 오디오 waveform
        sample_rate: 샘플 레이트

    Returns:
        Pitch 배열 (Hz)
    """
    # Tensor → NumPy 변환
    if isinstance(audio, torch.Tensor):
        audio_np = audio.squeeze().numpy()
    else:
        audio_np = audio

    # pyin 알고리즘으로 pitch 추출
    pitch, voiced_flag, voiced_probs = librosa.pyin(
        audio_np,
        fmin=librosa.note_to_hz('C2'),  # 최소 주파수: ~65Hz
        fmax=librosa.note_to_hz('C7'),  # 최대 주파수: ~2093Hz
        sr=sample_rate,
        frame_length=2048,
        hop_length=512
    )

    # NaN 처리 (무성음 구간)
    # 무성음 구간은 0으로 채우거나 선형 보간
    pitch = np.nan_to_num(pitch, nan=0.0)

    # 0이 너무 많으면 선형 보간으로 채우기
    pitch = interpolate_zeros(pitch)

    return pitch


def interpolate_zeros(pitch: np.ndarray) -> np.ndarray:
    """
    Pitch 배열의 0 값을 선형 보간

    Args:
        pitch: Pitch 배열

    Returns:
        보간된 Pitch 배열
    """
    pitch = pitch.copy()
    zero_indices = np.where(pitch == 0)[0]

    if len(zero_indices) == 0:
        return pitch

    non_zero_indices = np.where(pitch != 0)[0]

    if len(non_zero_indices) < 2:
        # 0이 아닌 값이 거의 없으면 평균값으로 채우기
        mean_pitch = np.mean(pitch[non_zero_indices]) if len(non_zero_indices) > 0 else 150.0
        pitch[zero_indices] = mean_pitch
        return pitch

    # 선형 보간
    pitch[zero_indices] = np.interp(
        zero_indices,
        non_zero_indices,
        pitch[non_zero_indices]
    )

    return pitch


def compute_dtw(
    ref_pitch: np.ndarray,
    user_pitch: np.ndarray
) -> Tuple[float, List[List[int]]]:
    """
    DTW (Dynamic Time Warping)로 두 pitch 시퀀스 비교

    Args:
        ref_pitch: 정답 pitch 배열
        user_pitch: 사용자 pitch 배열

    Returns:
        (distance, path) 튜플
        - distance: DTW 거리
        - path: 매핑 경로 [[ref_idx, user_idx], ...]
    """
    # DTW 실행
    distance, path = fastdtw(
        ref_pitch.reshape(-1, 1),
        user_pitch.reshape(-1, 1),
        dist=euclidean
    )

    return distance, path


def calculate_intonation_score(
    dtw_distance: float,
    sequence_length: int
) -> float:
    """
    DTW 거리를 0-100 점수로 변환

    Args:
        dtw_distance: DTW 거리
        sequence_length: Pitch 시퀀스 길이

    Returns:
        억양 점수 (0-100)
    """
    # 정규화: 거리를 시퀀스 길이로 나누기
    normalized_distance = dtw_distance / sequence_length if sequence_length > 0 else dtw_distance

    # 거리 → 점수 변환
    # 경험적으로 normalized_distance가 0-50 범위
    # 0이면 100점, 50이면 0점
    max_distance = 50.0
    score = max(0, 100 - (normalized_distance / max_distance * 100))

    return float(score)


def generate_feedback(
    ref_pitch: np.ndarray,
    user_pitch: np.ndarray,
    dtw_path: List[List[int]],
    score: float
) -> str:
    """
    억양 점수 기반 피드백 생성

    Args:
        ref_pitch: 정답 pitch
        user_pitch: 사용자 pitch
        dtw_path: DTW 매핑 경로
        score: 억양 점수

    Returns:
        피드백 문자열
    """
    if score >= 90:
        return "억양이 매우 자연스럽습니다!"
    elif score >= 80:
        return "억양이 좋습니다. 조금만 더 연습하면 완벽합니다!"
    elif score >= 70:
        return "억양이 괜찮지만, 문장의 높낮이 변화에 주의해주세요."
    elif score >= 60:
        return "억양 연습이 필요합니다. 정답 음성을 들으며 따라해보세요."
    else:
        return "억양에 더 많은 연습이 필요합니다. 천천히 따라해보세요."
