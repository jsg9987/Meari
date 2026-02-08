# 억양 분석 개선 이력

**프로젝트**: 억양 점수 정확도 개선
**목표**: 억양 점수 0점 문제 해결 → 정상 점수 분포 (20~90점)
**기간**: 2026-02-09 ~

---

## 📊 베이스라인 (Phase 1 완료 후)

**측정 일시**: 2026-02-09 00:30 (pytest 결과)
**Phase 1 개선 완료**: ✅
- sample_rate 버그 수정
- DTW 점수 계산 공식 개선
- Pitch 유효성 검증
- 로깅 강화

### 현재 성능

```
score: 11점
reference_pitch: [125.629, 125.629, 125.629, ...]  # 같은 값 반복!
user_pitch: [120.651, 120.651, 120.651, ...]       # 같은 값 반복!
feedback: "억양에 더 많은 연습이 필요합니다. 천천히 따라해보세요."
```

### 문제점 분석

#### 1. Pitch 추출 품질 문제 (Critical) 🔥

**증상:**
- 같은 값이 연속으로 반복됨
- 평탄한 pitch 배열

**원인:**
- `librosa.pyin()`이 대부분 **NaN** 반환
- `interpolate_zeros()`가 평균값으로 채움
- 결과적으로 평탄한 배열 생성

**근본 원인:**
- pyin 파라미터가 부적절
  - fmin=65Hz, fmax=2093Hz (너무 넓음)
  - frame_length=2048, hop_length=512 (너무 성김)
- 오디오 전처리 부족

#### 2. 점수는 나왔지만 낮음 (11점)

**분석:**
- 평탄한 pitch끼리 비교 → DTW distance 큼
- 우리가 개선한 공식 덕분에 0점은 피함
- 하지만 실제 억양 패턴을 반영하지 못함

#### 3. 유효성 검증 통과 이유

```python
# 현재 검증
if pitch_std < 5.0:  # 표준편차 < 5Hz
    return None
```

- 시작(125.629) → 끝(112.571) 변화로 std ≥ 5Hz
- 하지만 구간별로는 같은 값 반복
- **검증이 너무 약함**

---

## 🔄 개선 작업 계획

### [계획] #1: librosa.pyin() 파라미터 튜닝

**우선순위**: 🔥 Critical
**소요 시간**: 30분
**예상 효과**: Valid pitch ratio 30% → 70%+

#### 현재 설정
```python
pitch, _, _ = librosa.pyin(
    audio_np,
    fmin=librosa.note_to_hz('C2'),  # 65Hz
    fmax=librosa.note_to_hz('C7'),  # 2093Hz
    sr=sample_rate,
    frame_length=2048,
    hop_length=512
)
```

#### 개선 설정
```python
pitch, _, _ = librosa.pyin(
    audio_np,
    fmin=80,    # 남성 최저음 (더 높게)
    fmax=400,   # 한국어 대화 범위 (더 좁게)
    sr=sample_rate,
    frame_length=1024,  # 더 짧게 (시간 해상도 향상)
    hop_length=256      # 더 촘촘하게
)
```

#### 파라미터 설명

| 파라미터 | 현재 | 개선 | 이유 |
|---------|------|------|------|
| **fmin** | 65Hz (C2) | 80Hz | 한국어 최저음 더 높음 |
| **fmax** | 2093Hz (C7) | 400Hz | 일반 대화는 400Hz 이하 |
| **frame_length** | 2048 | 1024 | 더 짧은 윈도우 = 시간 해상도 향상 |
| **hop_length** | 512 | 256 | 더 촘촘한 샘플링 |

#### 예상 효과

- Valid pitch ratio: 30% → 70%+
- 평탄한 배열 → 실제 억양 패턴
- 점수: 11점 → 40~60점

---

### [계획] #2: interpolate_zeros() 로직 개선

**우선순위**: 🔥 High
**소요 시간**: 20분
**예상 효과**: 잘못된 보간 방지

#### 현재 문제
```python
# non_zero_indices가 2개 미만이면
# 평균값으로 전체를 채움 → 평탄한 배열!
if len(non_zero_indices) < 2:
    mean_pitch = np.mean(pitch[non_zero_indices])
    pitch[zero_indices] = mean_pitch  # 💣 문제!
    return pitch
```

#### 개선 방안
```python
# 1. 유효 비율 체크
valid_ratio = len(non_zero_indices) / len(pitch)
if valid_ratio < 0.5:  # 50% 미만이면 실패
    return None  # 보간 불가

# 2. 연속된 0이 너무 많으면 실패
max_consecutive_zeros = max_consecutive_count(pitch)
if max_consecutive_zeros > 20:  # 20프레임 이상 연속 0
    return None

# 3. 선형 보간만 수행
pitch[zero_indices] = np.interp(...)
```

---

### [계획] #3: 유효성 검증 강화

**우선순위**: 🟡 Medium
**소요 시간**: 15분
**예상 효과**: 품질 낮은 pitch 걸러내기

#### 추가 검증
```python
# 1. 연속된 같은 값 체크
consecutive_same = count_consecutive_same(pitch)
if consecutive_same > 10:  # 10개 이상 같은 값 연속
    logger.warning("연속된 같은 값 감지")
    return None

# 2. 변동 계수 (Coefficient of Variation) 체크
cv = std / mean
if cv < 0.05:  # 5% 미만이면 너무 평탄
    logger.warning(f"변동 계수 낮음: {cv:.3f}")
    return None
```

---

### [계획] #4: 오디오 전처리 (Pitch 추출용)

**우선순위**: 🟡 Medium
**소요 시간**: 40분
**예상 효과**: Pitch 추출 품질 향상 +10~15%

#### 전처리 항목

```python
def preprocess_for_pitch(audio: np.ndarray, sr: int) -> np.ndarray:
    # 1. RMS 정규화 (볼륨 통일)
    rms = np.sqrt(np.mean(audio**2))
    if rms > 0:
        target_rms = 0.1
        audio = audio * (target_rms / rms)

    # 2. Bandpass filter (80Hz ~ 400Hz)
    # Pitch 범위만 남기고 노이즈 제거
    from scipy.signal import butter, sosfilt
    sos = butter(4, [80, 400], btype='bandpass', fs=sr, output='sos')
    audio = sosfilt(sos, audio)

    return audio
```

**적용 위치:**
- `intonation_analyzer.py`의 `extract_pitch()` 함수
- pyin 호출 전에 전처리

---

### [계획] #5: Z-score 정규화 추가

**우선순위**: 🟢 Low
**소요 시간**: 30분
**예상 효과**: 화자 간 pitch 범위 차이 보정

#### 개선 내용

```python
def normalize_pitch(pitch: np.ndarray) -> np.ndarray:
    """Z-score 정규화 (화자 독립적)"""
    mean = np.mean(pitch)
    std = np.std(pitch)
    if std > 0:
        normalized = (pitch - mean) / std
    else:
        normalized = pitch - mean
    return normalized
```

#### DTW 계산 시 적용
```python
# 정규화 후 DTW
ref_pitch_norm = normalize_pitch(ref_pitch)
user_pitch_norm = normalize_pitch(user_pitch)
distance, path = compute_dtw(ref_pitch_norm, user_pitch_norm)
```

---

### [계획] #6: Correlation 보완 점수 (Optional)

**우선순위**: 🟢 Low
**소요 시간**: 40분
**예상 효과**: DTW 보완, 더 안정적인 점수

#### 구현
```python
def compute_correlation_score(ref_pitch, user_pitch):
    # 길이 맞추기 (100 포인트)
    from scipy.interpolate import interp1d

    ref_aligned = interp1d(
        np.linspace(0, 1, len(ref_pitch)),
        ref_pitch
    )(np.linspace(0, 1, 100))

    user_aligned = interp1d(
        np.linspace(0, 1, len(user_pitch)),
        user_pitch
    )(np.linspace(0, 1, 100))

    # Pearson correlation
    correlation = np.corrcoef(ref_aligned, user_aligned)[0, 1]
    score = max(0, correlation * 100)
    return score

# 최종 점수 = DTW 60% + Correlation 40%
final_score = 0.6 * dtw_score + 0.4 * corr_score
```

---

### [대안] #7: crepe 라이브러리 (장기)

**우선순위**: 🔵 Future
**소요 시간**: 2시간
**예상 효과**: Pitch 추출 품질 대폭 향상

#### crepe 특징
- Deep Learning 기반 pitch 추출
- librosa.pyin()보다 robust
- 더 정확하지만 느림

```python
import crepe

# crepe 사용
time, frequency, confidence, activation = crepe.predict(
    audio,
    sr,
    viterbi=True
)
```

---

## 📈 개선 추이 예상

```
억양 점수
100 ┤
 90 ┤                                    [목표]
 80 ┤
 70 ┤                         [#4+#5]
 60 ┤                [#3]
 50 ┤         [#2]
 40 ┤  [#1]
 30 ┤
 20 ┤
 11 ┤ ●
  0 ┼─┴─────┴────┴────┴────┴─────────────
   Base  #1   #2   #3   #4   #5
        pyin  보간  검증  전처리  Z-score
```

---

## 🎯 실행 우선순위

### 즉시 실행 (1시간)
1. **#1: pyin 파라미터 튜닝** (30분) 🔥
2. **#2: interpolate 로직 개선** (20분) 🔥
3. **#3: 유효성 검증 강화** (15분)

### 시간 여유 시 (1.5시간)
4. **#4: 오디오 전처리** (40분)
5. **#5: Z-score 정규화** (30분)
6. **#6: Correlation 점수** (40분)

### 장기 과제
7. **#7: crepe 라이브러리** (2시간+)

---

## ✅ 체크리스트

### Phase 1 완료
- [x] sample_rate 버그 수정
- [x] DTW 점수 계산 공식 개선
- [x] Pitch 유효성 검증
- [x] 로깅 강화

### Phase 2 계획
- [ ] #1: pyin 파라미터 튜닝
- [ ] #2: interpolate 로직 개선
- [ ] #3: 유효성 검증 강화
- [ ] #4: 오디오 전처리
- [ ] #5: Z-score 정규화
- [ ] #6: Correlation 점수

### 대기
- [ ] #7: crepe 라이브러리 (장기)
- [ ] 최종 벤치마크
- [ ] 발표 자료 작성

---

## 📝 참고 자료

### 관련 문서
- **긴급 수정 보고서**: `md/report/2026-02-09_intonation_score_emergency_fix.md`
- **테스트 결과**: pytest 로그 (score: 11점)

### 디버깅 도구
- **debug_pitch.py**: Pitch 추출 품질 분석 스크립트
- **test_intonation_analyzer.py**: 억양 분석 테스트

---

## 🔄 최근 개선 작업 (2026-02-09 02:30)

### ✅ Parselmouth (Praat) 통합 완료!

**소요 시간**: 30분
**우선순위**: 🔥 Critical

#### 개선 내용

1. **Pitch 추출 엔진 교체**
   - Before: `librosa.pyin()` (Valid ratio 60.8%)
   - After: `Parselmouth` (Valid ratio 63.9%, +3.1%p)

2. **코드 변경**
   - `intonation_analyzer.py`: extract_pitch() 함수 Parselmouth 버전으로 교체
   - `interpolate_zeros()`: 유효성 검증 강화 (30% 미만 시 실패)
   - Phase 1 점수 계산 공식 유지 (적응형 max_distance, 0점 방지)

3. **의존성 추가**
   - `requirements.txt`: `praat-parselmouth==0.4.3`, `scipy>=1.7.0`

#### 벤치마크 결과

**같은 파일 테스트 (test_parselmouth.py):**
```
점수: 100/100 ✅
DTW distance: 0.00
Valid ratio: 63.9%
Pitch: mean=150.4Hz, std=55.5Hz
```

**다른 파일 테스트 (257 vs 257_mine):**
- AI 기본 공식: 0.5점 (점수 계산 공식 문제)
- 우리 Phase 1 공식 적용 예상: 40~60점

#### 예상 효과

| 지표 | Before | After (예상) | 개선 |
|------|--------|--------------|------|
| Valid ratio | 60.8% | 63.9% | +3.1%p |
| 점수 (11점에서) | 11점 | 40~60점 | +300~450% |

#### 다음 단계

- [ ] 실제 벤치마크 측정 (`test_parselmouth_integration.py`)
- [ ] 결과에 따라 추가 개선 검토

---

### ❌ librosa.pyin() 파라미터 튜닝 시도 (실패)

**시도 일시**: 2026-02-09 02:00
**결과**: Valid ratio 60.8% → 47.2% (악화)

**교훈**: 파라미터 튜닝으로는 한계, Parselmouth 통합이 정답

---

**마지막 업데이트**: 2026-02-09 02:40
**다음 작업**: Parselmouth 통합 결과 측정
