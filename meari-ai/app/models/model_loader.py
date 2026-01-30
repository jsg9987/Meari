"""
ML 모델 로더
Wav2Vec2 ASR 모델을 전역으로 로드
"""
import logging
import torch
from transformers import Wav2Vec2Processor, Wav2Vec2ForCTC
from app.config import settings

logger = logging.getLogger(__name__)

# 전역 모델 변수
processor = None
asr_model = None
mdd_model = None
device = None


def load_models():
    """모델 로드 (서버 시작 시 1회 실행)"""
    global processor, asr_model, mdd_model, device

    try:
        # 디바이스 설정
        device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        logger.info(f"사용 디바이스: {device}")

        # Wav2Vec2 Processor 로드 (HuggingFace Hub에서 자동 다운로드)
        logger.info(f"Wav2Vec2 Processor 로드 중... ({settings.WAV2VEC2_MODEL_PATH})")
        processor = Wav2Vec2Processor.from_pretrained(settings.WAV2VEC2_MODEL_PATH)
        logger.info("Wav2Vec2 Processor 로드 완료")

        # Wav2Vec2 ASR 모델 로드
        logger.info(f"Wav2Vec2 ASR 모델 로드 중... ({settings.WAV2VEC2_MODEL_PATH})")
        asr_model = Wav2Vec2ForCTC.from_pretrained(settings.WAV2VEC2_MODEL_PATH)
        asr_model.to(device)
        asr_model.eval()
        logger.info("Wav2Vec2 ASR 모델 로드 완료")

        # MDD 모델 로드 (선택사항 - 실패해도 ASR 기반 분석은 가능)
        try:
            logger.info("MDD 모델 로드 시도 중...")
            mdd_model = load_mdd_model(settings.MDD_MODEL_PATH)
            if mdd_model is not None:
                logger.info("MDD 모델 로드 완료")
            else:
                logger.warning("MDD 모델 없음 - ASR 기반 분석만 사용")
        except Exception as mdd_err:
            logger.warning(f"MDD 모델 로드 실패 (ASR만 사용): {mdd_err}")
            mdd_model = None

        logger.info("모델 로드 완료 (ASR 분석 준비됨)")

    except Exception as e:
        logger.error(f"필수 모델 로드 실패: {e}", exc_info=True)
        raise


def load_mdd_model(model_path: str):
    """
    MDD 모델 로드 (Placeholder)

    실제 구현:
    - Jupyter notebook의 finetuned_mdd_model을 로드
    - torch.load() 또는 model.load_state_dict() 사용

    TODO: 실제 MDD 모델 구조에 맞게 구현 필요
    """
    try:
        # Placeholder: 실제로는 커스텀 모델 클래스 정의 후 로드
        # Example:
        # model = MDDModel(...)
        # model.load_state_dict(torch.load(f"{model_path}/model.pth"))
        # model.to(device)
        # model.eval()
        # return model

        logger.warning("MDD 모델은 Placeholder입니다. 실제 모델 로드 로직 구현 필요")
        return None

    except Exception as e:
        logger.error(f"MDD 모델 로드 실패: {e}")
        raise


def get_processor():
    """Processor 반환"""
    if processor is None:
        raise RuntimeError("Processor가 로드되지 않았습니다.")
    return processor


def get_asr_model():
    """ASR 모델 반환"""
    if asr_model is None:
        raise RuntimeError("ASR 모델이 로드되지 않았습니다.")
    return asr_model


def get_mdd_model():
    """MDD 모델 반환"""
    if mdd_model is None:
        logger.warning("MDD 모델이 로드되지 않았습니다.")
    return mdd_model


def get_device():
    """디바이스 반환"""
    if device is None:
        raise RuntimeError("디바이스가 설정되지 않았습니다.")
    return device
