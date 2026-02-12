"""
한글 자모 처리 유틸리티
Wav2Vec2 ASR 출력(자모)을 음절로 변환하는 기능 제공
"""
import re
import numpy as np
from typing import List, Tuple, Optional

# 한글 유니코드 상수
HANGUL_BASE = 0xAC00  # '가'
HANGUL_END = 0xD7A3   # '힣'

# 초성 (19개)
CHOSEONG = [
    'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ',
    'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
]

# 중성 (21개)
JUNGSEONG = [
    'ㅏ', 'ㅐ', 'ㅑ', 'ㅒ', 'ㅓ', 'ㅔ', 'ㅕ', 'ㅖ', 'ㅗ', 'ㅘ',
    'ㅙ', 'ㅚ', 'ㅛ', 'ㅜ', 'ㅝ', 'ㅞ', 'ㅟ', 'ㅠ', 'ㅡ', 'ㅢ', 'ㅣ'
]

# 종성 (28개, 첫 번째는 종성 없음)
JONGSEONG = [
    '', 'ㄱ', 'ㄲ', 'ㄳ', 'ㄴ', 'ㄵ', 'ㄶ', 'ㄷ', 'ㄹ', 'ㄺ',
    'ㄻ', 'ㄼ', 'ㄽ', 'ㄾ', 'ㄿ', 'ㅀ', 'ㅁ', 'ㅂ', 'ㅄ', 'ㅅ',
    'ㅆ', 'ㅇ', 'ㅈ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
]

# 인덱스 매핑
CHO2I = {c: i for i, c in enumerate(CHOSEONG)}
JUNG2I = {c: i for i, c in enumerate(JUNGSEONG)}
JONG2I = {c: i for i, c in enumerate(JONGSEONG)}


def is_hangul_syllable(ch: str) -> bool:
    """완성형 한글 음절인지 확인"""
    if len(ch) != 1:
        return False
    code = ord(ch)
    return HANGUL_BASE <= code <= HANGUL_END


def is_jamo(ch: str) -> bool:
    """한글 자모(초성/중성/종성)인지 확인"""
    return ch in CHO2I or ch in JUNG2I or ch in JONG2I


def compose_syllable(cho: str, jung: str, jong: str = '') -> Optional[str]:
    """
    초성, 중성, 종성을 조합하여 한글 음절 생성

    Args:
        cho: 초성
        jung: 중성
        jong: 종성 (없으면 빈 문자열)

    Returns:
        조합된 음절 또는 None (조합 불가능한 경우)
    """
    if cho not in CHO2I or jung not in JUNG2I:
        return None

    cho_idx = CHO2I[cho]
    jung_idx = JUNG2I[jung]
    jong_idx = JONG2I.get(jong, 0)  # 종성 없으면 0

    # 유니코드 조합 공식
    code = HANGUL_BASE + (cho_idx * 21 + jung_idx) * 28 + jong_idx
    return chr(code)


def decompose_syllable(syllable: str) -> Optional[Tuple[str, str, str]]:
    """
    한글 음절을 초성, 중성, 종성으로 분해

    Args:
        syllable: 한글 음절 1개

    Returns:
        (초성, 중성, 종성) 튜플 또는 None
    """
    if not is_hangul_syllable(syllable):
        return None

    code = ord(syllable) - HANGUL_BASE
    jong_idx = code % 28
    code //= 28
    jung_idx = code % 21
    cho_idx = code // 21

    return (CHOSEONG[cho_idx], JUNGSEONG[jung_idx], JONGSEONG[jong_idx])


def clean_asr_text(text: str) -> str:
    """ASR 출력 텍스트 정리"""
    if not text:
        return ""
    # 연속 공백 제거, 앞뒤 공백 제거
    text = re.sub(r'\s+', ' ', text).strip()
    return text


def strip_spaces(text: str) -> str:
    """모든 공백 제거"""
    return re.sub(r'\s+', '', (text or '').strip())


def jamo_to_syllables_with_probs(
    raw_jamo: str,
    token_probs: List[float]
) -> Tuple[str, List[str], List[float]]:
    """
    자모 문자열과 토큰별 확률을 음절 단위로 변환

    Wav2Vec2 CTC 디코딩 결과 (자모 단위)를 음절로 조합하고,
    각 음절의 confidence를 구성 자모들의 평균으로 계산

    Args:
        raw_jamo: CTC 디코딩 결과 자모 문자열 (예: "ㅇㅏㄴㄴㅕㅇ")
        token_probs: 각 토큰(자모)의 confidence 리스트

    Returns:
        (음절 문자열, 음절 리스트, 음절별 confidence 리스트)
    """
    s = (raw_jamo or '').strip().replace('|', ' ')
    probs = list(token_probs or [])

    out_syl = []
    out_prob = []

    i = 0
    pi = 0  # probs index

    def take_prob() -> float:
        nonlocal pi
        if pi < len(probs):
            p = float(probs[pi])
        else:
            p = 1.0  # 확률이 없으면 기본값
        pi += 1
        return p

    while i < len(s):
        ch = s[i]

        # 공백은 그대로
        if ch.isspace():
            out_syl.append(' ')
            i += 1
            continue

        # 이미 음절이면 그대로 + prob 1개 소비
        if is_hangul_syllable(ch):
            out_syl.append(ch)
            out_prob.append(take_prob())
            i += 1
            continue

        # 초성 + 중성 (+종성) 조합 시도
        if ch in CHO2I and (i + 1) < len(s) and s[i + 1] in JUNG2I:
            cho = s[i]
            jung = s[i + 1]
            p1 = take_prob()
            p2 = take_prob()
            jong = ''
            p3 = None
            step = 2

            # 종성 확인
            if (i + 2) < len(s):
                cand = s[i + 2]
                # 다음이 중성이면 현재 자음은 다음 음절의 초성
                if cand in JONG2I and cand != '':
                    # 다다음이 중성이면 cand는 다음 초성
                    if (i + 3) < len(s) and s[i + 3] in JUNG2I:
                        jong = ''
                        step = 2
                    else:
                        jong = cand
                        p3 = take_prob()
                        step = 3

            syl = compose_syllable(cho, jung, jong)
            if syl is not None:
                out_syl.append(syl)
                parts = [p1, p2] + ([] if p3 is None else [p3])
                out_prob.append(float(np.mean(parts)))
                i += step
                continue

        # 조합 불가능한 문자는 그대로
        out_syl.append(ch)
        out_prob.append(take_prob())
        i += 1

    # 결과 정리
    syll_text = re.sub(r'\s+', ' ', ''.join(out_syl)).strip()
    syll_list = [c for c in syll_text if not c.isspace()]

    # 공백 제외한 음절 수와 prob 수 맞추기
    syll_probs = out_prob[:len(syll_list)]

    # 부족한 경우 1.0으로 채우기
    while len(syll_probs) < len(syll_list):
        syll_probs.append(1.0)

    return syll_text, syll_list, syll_probs


def calculate_accuracy_from_syllables(answer: str, predicted: str) -> int:
    """
    정답과 예측 음절을 비교하여 정확도 계산 (0-100)

    Args:
        answer: 정답 텍스트
        predicted: 예측 텍스트

    Returns:
        정확도 점수 (0-100)
    """
    from difflib import SequenceMatcher

    ans = strip_spaces(answer)
    hyp = strip_spaces(predicted)

    if len(ans) == 0:
        return 100 if len(hyp) == 0 else 0

    sm = SequenceMatcher(a=list(ans), b=list(hyp))
    ops = sm.get_opcodes()

    diff_cnt = 0
    for tag, i1, i2, j1, j2 in ops:
        if tag == 'equal':
            continue
        if tag == 'replace':
            diff_cnt += max(i2 - i1, j2 - j1)
        elif tag == 'delete':
            diff_cnt += (i2 - i1)
        elif tag == 'insert':
            diff_cnt += (j2 - j1)

    accuracy = int(max(0, min(100, round((1 - diff_cnt / max(1, len(ans))) * 100))))
    return accuracy
