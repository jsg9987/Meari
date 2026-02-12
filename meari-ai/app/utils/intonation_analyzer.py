"""
억양 분석 유틸리티
Pitch Contour 추출 및 DTW 비교
Parselmouth (Praat) 기반 Pitch 추출
"""
import logging
import numpy as np
import librosa
import torch
import parselmouth
from typing import Dict, Any, List, Tuple, Optional
from fastdtw import fastdtw
from scipy.spatial.distance import euclidean
from scipy.interpolate import interp1d

logger = logging.getLogger(__name__)


def align_pitch_for_frontend(
    ref_pitch: np.ndarray,
    user_pitch: np.ndarray,
    sample_rate: int = 16000,
    num_points: int = 100
) -> Dict[str, List[float]]:
    """
    프론트엔드 차트용으로 pitch 데이터를 같은 길이로 정렬

    Args:
        ref_pitch: 정답 pitch 배열
        user_pitch: 사용자 pitch 배열
        sample_rate: 샘플 레이트
        num_points: 통일할 데이터 포인트 수 (기본 100개)

    Returns:
        {
            "reference": [float, ...],  # num_points개
            "user": [float, ...],       # num_points개
            "time_points": [float, ...]  # num_points개 (초 단위)
        }
    """
    # 1. 각 pitch를 num_points로 보간
    # Reference pitch
    ref_x = np.linspace(0, 1, len(ref_pitch))
    ref_interp = interp1d(ref_x, ref_pitch, kind='linear')
    ref_aligned = ref_interp(np.linspace(0, 1, num_points))

    # User pitch
    user_x = np.linspace(0, 1, len(user_pitch))
    user_interp = interp1d(user_x, user_pitch, kind='linear')
    user_aligned = user_interp(np.linspace(0, 1, num_points))

    # 2. 시간 축 생성 (긴 쪽 기준)
    max_frames = max(len(ref_pitch), len(user_pitch))
    hop_length = 512
    max_time = librosa.frames_to_time(max_frames, sr=sample_rate, hop_length=hop_length)
    time_points = np.linspace(0, max_time, num_points)

    return {
        "reference": ref_aligned.tolist(),
        "user": user_aligned.tolist(),
        "time_points": time_points.tolist()
    }


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

        # ✅ None 체크 (품질 낮은 오디오)
        if ref_pitch is None or user_pitch is None:
            logger.warning("Pitch 추출 실패 또는 품질 낮음")
            return {
                "score": -1,  # -1로 분석 불가 표시
                "feedback": "오디오 품질이 낮아 억양 분석이 어렵습니다. 더 크고 명확하게 발음해주세요.",
                "pitch_data": {
                    "reference": [],
                    "user": [],
                    "time_points": []
                },
                "statistics": {},
                "raw_data": {
                    "reference_pitch": [],
                    "user_pitch": [],
                    "reference_frames": 0,
                    "user_frames": 0
                }
            }

        # 2. Z-score 정규화 (화자 독립적)
        ref_mean = np.mean(ref_pitch)
        ref_std = np.std(ref_pitch)
        user_mean = np.mean(user_pitch)
        user_std = np.std(user_pitch)

        # 표준편차가 0이면 정규화 불가 (평탄한 pitch)
        if ref_std < 1e-6 or user_std < 1e-6:
            logger.warning("Pitch 표준편차가 너무 작음 (평탄)")
            return {
                "score": -1,
                "feedback": "오디오 품질이 낮아 억양 분석이 어렵습니다.",
                "pitch_data": {
                    "reference": [],
                    "user": [],
                    "time_points": []
                },
                "statistics": {},
                "raw_data": {
                    "reference_pitch": [],
                    "user_pitch": [],
                    "reference_frames": 0,
                    "user_frames": 0
                }
            }

        ref_pitch_norm = (ref_pitch - ref_mean) / ref_std
        user_pitch_norm = (user_pitch - user_mean) / user_std

        logger.info(
            f"Z-score 정규화: "
            f"ref(mean={ref_mean:.1f}Hz, std={ref_std:.1f}Hz), "
            f"user(mean={user_mean:.1f}Hz, std={user_std:.1f}Hz)"
        )

        # 3. DTW로 시간 정렬 및 유사도 계산 (정규화된 pitch 사용)
        distance, path = compute_dtw(ref_pitch_norm, user_pitch_norm)

        # 4. 억양 점수 계산 (0-100)
        # 원본 pitch로 통계 계산 (max_distance에 사용)
        score = calculate_intonation_score(distance, len(ref_pitch), ref_pitch, user_pitch)

        # 5. 프론트엔드 차트용 데이터 생성 (길이 통일)
        aligned_data = align_pitch_for_frontend(ref_pitch, user_pitch, sample_rate)

        # 6. 통계 정보 계산
        statistics = {
            "reference": {
                "mean": float(ref_mean),
                "std": float(ref_std),
                "min": float(np.min(ref_pitch)),
                "max": float(np.max(ref_pitch))
            },
            "user": {
                "mean": float(user_mean),
                "std": float(user_std),
                "min": float(np.min(user_pitch)),
                "max": float(np.max(user_pitch))
            },
            "pitch_difference": float(abs(ref_mean - user_mean))
        }

        # 7. 피드백 생성
        feedback = generate_feedback(ref_pitch, user_pitch, path, score)

        # 8. 프론트엔드 친화적 응답 구조
        return {
            "score": int(score),
            "feedback": feedback,

            # 차트 렌더링용 (길이 통일)
            "pitch_data": aligned_data,

            # 통계 정보
            "statistics": statistics,

            # 원본 데이터 (상세 분석/디버깅용)
            "raw_data": {
                "reference_pitch": ref_pitch.tolist(),
                "user_pitch": user_pitch.tolist(),
                "reference_frames": len(ref_pitch),
                "user_frames": len(user_pitch)
            }
        }

    except Exception as e:
        logger.error(f"억양 분석 실패: {e}", exc_info=True)
        return {
            "score": 0,
            "feedback": "억양 분석 실패",
            "pitch_data": {
                "reference": [],
                "user": [],
                "time_points": []
            },
            "statistics": {},
            "raw_data": {
                "reference_pitch": [],
                "user_pitch": [],
                "reference_frames": 0,
                "user_frames": 0
            }
        }


def extract_pitch(
    audio: torch.Tensor,
    sample_rate: int = 16000
) -> Optional[np.ndarray]:
    """
    Parselmouth (Praat)로 Pitch (F0) 추출 + 유효성 검증

    Args:
        audio: 오디오 waveform
        sample_rate: 샘플 레이트

    Returns:
        Pitch 배열 (Hz) 또는 None (품질 낮음)
    """
    try:
        # Tensor → NumPy 변환
        if isinstance(audio, torch.Tensor):
            audio_np = audio.squeeze().numpy()
        else:
            audio_np = audio

        # ✅ Parselmouth는 numpy array 직접 지원!
        # 임시 파일 저장 불필요
        snd = parselmouth.Sound(audio_np, sampling_frequency=sample_rate)

        # Praat의 pitch 추출
        pitch_obj = snd.to_pitch(
            time_step=0.01,  # 10ms (hop_length=160 at 16kHz와 유사)
            pitch_floor=75.0,  # 최소 주파수
            pitch_ceiling=400.0  # 최대 주파수 (한국어 대화 범위)
        )

        # Pitch 값 추출
        pitch_values = pitch_obj.selected_array['frequency']

        # 0(무음) 값은 NaN으로
        pitch_values = pitch_values.copy()
        pitch_values[pitch_values == 0] = np.nan

        # NaN 처리 및 보간
        pitch = interpolate_zeros(pitch_values)

        if pitch is None:
            logger.warning("Pitch 보간 실패")
            return None

        # ✅ 유효성 검증
        # 1. 표준편차 체크 (평탄한 pitch 감지)
        pitch_std = np.std(pitch)
        if pitch_std < 5.0:  # 표준편차가 5Hz 미만이면 거의 평탄
            logger.warning(f"Pitch 표준편차 너무 낮음: {pitch_std:.2f}Hz (평탄한 발화)")
            return None

        # 2. 유효 비율 체크 (원본 NaN 비율)
        valid_ratio = np.sum(~np.isnan(pitch_values)) / len(pitch_values)
        if valid_ratio < 0.3:  # 30% 미만이면 실패
            logger.warning(f"유효 Pitch 비율 낮음: {valid_ratio:.1%}")
            return None

        # 3. Pitch 통계 로그
        logger.info(
            f"Pitch Extracted (Parselmouth): "
            f"mean={np.mean(pitch):.1f}Hz, "
            f"std={pitch_std:.1f}Hz, "
            f"valid_ratio={valid_ratio:.1%}"
        )

        return pitch

    except Exception as e:
        logger.error(f"Parselmouth Pitch 추출 실패: {e}")
        return None


def interpolate_zeros(pitch: np.ndarray) -> Optional[np.ndarray]:
    """
    Pitch 배열의 0/NaN 값을 선형 보간 (개선 버전)

    Args:
        pitch: Pitch 배열 (0 또는 NaN 포함 가능)

    Returns:
        보간된 Pitch 배열 또는 None (보간 불가능)
    """
    pitch = pitch.copy()

    # NaN과 0을 모두 처리
    nan_mask = np.isnan(pitch) | (pitch == 0)

    if np.all(nan_mask):
        # 전부 NaN/0이면 실패
        return None

    # 유효한 값의 인덱스
    valid_indices = np.where(~nan_mask)[0]
    valid_values = pitch[valid_indices]

    if len(valid_indices) < 2:
        # 유효 값이 2개 미만이면 보간 불가
        return None

    # 유효 비율 체크
    valid_ratio = len(valid_indices) / len(pitch)
    if valid_ratio < 0.3:
        # 30% 미만이면 보간해도 품질 낮음
        logger.warning(f"유효 Pitch 비율 너무 낮음: {valid_ratio:.1%}")
        return None

    # 선형 보간
    nan_indices = np.where(nan_mask)[0]
    pitch[nan_indices] = np.interp(
        nan_indices,
        valid_indices,
        valid_values
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
    sequence_length: int,
    ref_pitch: np.ndarray,
    user_pitch: np.ndarray
) -> float:
    """
    개선된 DTW 점수 계산 (0점 방지)

    Args:
        dtw_distance: DTW 거리
        sequence_length: Pitch 시퀀스 길이
        ref_pitch: 정답 pitch 배열
        user_pitch: 사용자 pitch 배열

    Returns:
        억양 점수 (0-100)
    """
    if sequence_length == 0:
        return 0.0

    # 1. 기본 정규화
    normalized_distance = dtw_distance / sequence_length

    # 2. Pitch 통계 기반 적응형 스케일링
    ref_mean = np.mean(ref_pitch)
    user_mean = np.mean(user_pitch)
    pitch_diff = abs(ref_mean - user_mean)

    ref_std = np.std(ref_pitch)
    user_std = np.std(user_pitch)
    avg_std = (ref_std + user_std) / 2

    # 3. 적응형 max_distance
    # 기본 50 + pitch 차이의 10% + 표준편차 반영
    max_distance = 50.0 + (pitch_diff * 0.1) + (avg_std * 0.05)

    # 4. 단계별 점수 계산 (0점 방지)
    if normalized_distance < 1:
        score = 100 - normalized_distance * 5  # 100~95
    elif normalized_distance < 2:
        score = 95 - (normalized_distance - 1) * 10  # 95~85
    elif normalized_distance < 5:
        score = 85 - (normalized_distance - 2) * 15  # 85~40
    elif normalized_distance < 10:
        score = 40 - (normalized_distance - 5) * 5   # 40~15
    else:
        score = max(10, 15 - (normalized_distance - 10) * 1)  # 15~10 (최소 10점)

    # 5. 디버깅 로그
    logger.info(
        f"Intonation Score Calculation: "
        f"dtw_distance={dtw_distance:.2f}, "
        f"normalized={normalized_distance:.2f}, "
        f"max_distance={max_distance:.2f}, "
        f"ref_mean={ref_mean:.1f}Hz, "
        f"user_mean={user_mean:.1f}Hz, "
        f"pitch_diff={pitch_diff:.1f}Hz, "
        f"score={score:.1f}"
    )

    return max(0, min(100, float(score)))


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
