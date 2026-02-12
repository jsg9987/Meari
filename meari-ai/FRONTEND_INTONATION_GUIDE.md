# 억양 분석 API - 프론트엔드 사용 가이드

## 📋 API 응답 구조

### 정상 응답 (score >= 0)

```json
{
  "score": 94,
  "feedback": "억양이 매우 자연스럽습니다!",

  "pitch_data": {
    "reference": [216.5, 218.3, 220.1, ..., 215.8],  // 100개 (Hz)
    "user": [195.2, 197.8, 199.5, ..., 194.3],       // 100개 (Hz)
    "time_points": [0.0, 0.05, 0.10, ..., 4.95]     // 100개 (초)
  },

  "statistics": {
    "reference": {
      "mean": 216.5,      // 평균 주파수 (Hz)
      "std": 12.3,        // 표준편차
      "min": 180.0,       // 최소값
      "max": 250.0        // 최대값
    },
    "user": {
      "mean": 195.2,
      "std": 15.8,
      "min": 160.0,
      "max": 230.0
    },
    "pitch_difference": 21.3  // 평균 주파수 차이 (Hz)
  },

  "raw_data": {
    "reference_pitch": [216.5, 218.3, ...],  // 292개 (원본)
    "user_pitch": [195.2, 197.8, ...],       // 371개 (원본)
    "reference_frames": 292,
    "user_frames": 371
  }
}
```

### 분석 불가 응답 (score = -1)

품질이 낮은 오디오인 경우:

```json
{
  "score": -1,
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
```

---

## 🎨 프론트엔드 구현 가이드

### 1. 기본 처리 로직

```typescript
interface IntonationResult {
  score: number;
  feedback: string;
  pitch_data: {
    reference: number[];
    user: number[];
    time_points: number[];
  };
  statistics: {
    reference?: {
      mean: number;
      std: number;
      min: number;
      max: number;
    };
    user?: {
      mean: number;
      std: number;
      min: number;
      max: number;
    };
    pitch_difference?: number;
  };
  raw_data: {
    reference_pitch: number[];
    user_pitch: number[];
    reference_frames: number;
    user_frames: number;
  };
}

// API 응답 처리
function handleIntonationResult(result: IntonationResult) {
  if (result.score === -1) {
    // 분석 불가 처리
    showErrorMessage(result.feedback);
    return;
  }

  // 점수 표시
  showScore(result.score);

  // 피드백 표시
  showFeedback(result.feedback);

  // 차트 렌더링
  renderIntonationChart(result.pitch_data);

  // 통계 표시 (선택적)
  showStatistics(result.statistics);
}
```

---

### 2. 차트 렌더링 (Chart.js 예시)

```javascript
import { Line } from 'react-chartjs-2';

function IntonationChart({ pitchData }) {
  const chartData = {
    labels: pitchData.time_points.map(t => t.toFixed(2)),  // X축: 시간 (초)
    datasets: [
      {
        label: '정답',
        data: pitchData.reference,
        borderColor: 'rgb(75, 192, 192)',
        backgroundColor: 'rgba(75, 192, 192, 0.2)',
        tension: 0.4,  // 곡선 부드럽게
        borderWidth: 2
      },
      {
        label: '사용자',
        data: pitchData.user,
        borderColor: 'rgb(255, 99, 132)',
        backgroundColor: 'rgba(255, 99, 132, 0.2)',
        tension: 0.4,
        borderWidth: 2
      }
    ]
  };

  const options = {
    responsive: true,
    plugins: {
      legend: {
        position: 'top',
      },
      title: {
        display: true,
        text: '억양 패턴 비교'
      }
    },
    scales: {
      x: {
        title: {
          display: true,
          text: '시간 (초)'
        }
      },
      y: {
        title: {
          display: true,
          text: '주파수 (Hz)'
        },
        beginAtZero: false  // 0부터 시작하지 않음
      }
    }
  };

  return <Line data={chartData} options={options} />;
}
```

---

### 3. Recharts 예시

```jsx
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend } from 'recharts';

function IntonationChart({ pitchData }) {
  // 데이터 변환 (Recharts 형식)
  const chartData = pitchData.time_points.map((time, index) => ({
    time: time.toFixed(2),
    정답: pitchData.reference[index],
    사용자: pitchData.user[index]
  }));

  return (
    <LineChart width={800} height={400} data={chartData}>
      <CartesianGrid strokeDasharray="3 3" />
      <XAxis
        dataKey="time"
        label={{ value: '시간 (초)', position: 'insideBottom', offset: -5 }}
      />
      <YAxis
        label={{ value: '주파수 (Hz)', angle: -90, position: 'insideLeft' }}
      />
      <Tooltip />
      <Legend />
      <Line
        type="monotone"
        dataKey="정답"
        stroke="#4BC0C0"
        strokeWidth={2}
        dot={false}  // 점 표시 안함
      />
      <Line
        type="monotone"
        dataKey="사용자"
        stroke="#FF6384"
        strokeWidth={2}
        dot={false}
      />
    </LineChart>
  );
}
```

---

### 4. 통계 정보 표시

```jsx
function StatisticsPanel({ statistics }) {
  if (!statistics.reference || !statistics.user) {
    return null;
  }

  return (
    <div className="statistics-panel">
      <h3>억양 통계</h3>

      <div className="stat-row">
        <div className="stat-col">
          <h4>정답</h4>
          <p>평균: {statistics.reference.mean.toFixed(1)} Hz</p>
          <p>표준편차: {statistics.reference.std.toFixed(1)} Hz</p>
          <p>범위: {statistics.reference.min.toFixed(1)} ~ {statistics.reference.max.toFixed(1)} Hz</p>
        </div>

        <div className="stat-col">
          <h4>사용자</h4>
          <p>평균: {statistics.user.mean.toFixed(1)} Hz</p>
          <p>표준편차: {statistics.user.std.toFixed(1)} Hz</p>
          <p>범위: {statistics.user.min.toFixed(1)} ~ {statistics.user.max.toFixed(1)} Hz</p>
        </div>
      </div>

      <div className="pitch-diff">
        <p>평균 주파수 차이: {statistics.pitch_difference.toFixed(1)} Hz</p>
      </div>
    </div>
  );
}
```

---

### 5. 점수 표시 UI

```jsx
function ScoreDisplay({ score, feedback }) {
  // 점수에 따른 색상
  const getScoreColor = (score) => {
    if (score >= 90) return '#4CAF50';  // 초록
    if (score >= 80) return '#8BC34A';  // 연두
    if (score >= 70) return '#FFC107';  // 노랑
    if (score >= 60) return '#FF9800';  // 주황
    return '#F44336';  // 빨강
  };

  return (
    <div className="score-container">
      <div
        className="score-circle"
        style={{ borderColor: getScoreColor(score) }}
      >
        <span className="score-number">{score}</span>
        <span className="score-label">점</span>
      </div>

      <p className="feedback-text">{feedback}</p>
    </div>
  );
}
```

---

## 🔍 주요 필드 설명

### 1. `pitch_data` (차트용)

**목적**: 프론트엔드에서 바로 차트 그리기

- ✅ **길이 통일**: 100개로 고정 (차트 렌더링 쉬움)
- ✅ **보간 완료**: 원본 길이가 달라도 알아서 처리됨
- ✅ **시간 축**: `time_points`로 X축 표시

**사용법**:
```javascript
// Chart.js
data: {
  labels: pitchData.time_points,  // X축
  datasets: [
    { data: pitchData.reference },  // Y축 (정답)
    { data: pitchData.user }        // Y축 (사용자)
  ]
}
```

---

### 2. `statistics` (정보 표시용)

**목적**: 통계 정보를 UI에 표시

- `mean`: 평균 주파수 (Hz) - 목소리 높이
- `std`: 표준편차 - 억양 변화 정도
- `min/max`: 최소/최대 주파수 - 억양 범위
- `pitch_difference`: 정답과 사용자의 평균 차이

**활용**:
- 사용자에게 "목소리가 너무 낮아요" 같은 구체적 피드백
- 억양 변화가 적으면 "더 생동감 있게 읽어보세요"

---

### 3. `raw_data` (상세 분석용)

**목적**: 원본 데이터 (디버깅, 상세 분석)

- 일반적으로 프론트에서 사용 안함
- 관리자 모드나 디버그 모드에서 활용
- 길이가 다름 (292 vs 371개)

**예시**:
```javascript
// 디버그 모드
if (debugMode) {
  console.log('원본 프레임 수:',
    result.raw_data.reference_frames,
    result.raw_data.user_frames
  );
}
```

---

## ⚠️ 주의사항

### 1. `score = -1` 처리

```javascript
if (result.score === -1) {
  // 오디오 품질 낮음 → 차트 표시 안함
  showErrorMessage(result.feedback);
  hideChart();
  return;
}
```

### 2. 빈 배열 체크

```javascript
if (result.pitch_data.reference.length === 0) {
  // 데이터 없음 → 차트 표시 안함
  return;
}
```

### 3. 시간 축 포맷

```javascript
// 시간 축 레이블 (소수점 2자리)
labels: pitchData.time_points.map(t => t.toFixed(2) + 's')
```

### 4. Y축 범위 자동 조정

```javascript
// Chart.js 옵션
scales: {
  y: {
    beginAtZero: false,  // 0부터 시작 안함
    suggestedMin: 100,    // 최소 100Hz
    suggestedMax: 400     // 최대 400Hz
  }
}
```

---

## 📊 완성된 컴포넌트 예시 (React + Chart.js)

```jsx
import React from 'react';
import { Line } from 'react-chartjs-2';

function IntonationAnalysis({ result }) {
  // 분석 불가 처리
  if (result.score === -1) {
    return (
      <div className="error-state">
        <p className="error-message">{result.feedback}</p>
        <p className="error-hint">더 크고 명확하게 발음해주세요.</p>
      </div>
    );
  }

  // 데이터 없음
  if (result.pitch_data.reference.length === 0) {
    return <p>데이터가 없습니다.</p>;
  }

  // 차트 데이터
  const chartData = {
    labels: result.pitch_data.time_points.map(t => t.toFixed(2)),
    datasets: [
      {
        label: '정답',
        data: result.pitch_data.reference,
        borderColor: '#4BC0C0',
        backgroundColor: 'rgba(75, 192, 192, 0.2)',
        tension: 0.4,
        borderWidth: 2
      },
      {
        label: '나의 발음',
        data: result.pitch_data.user,
        borderColor: '#FF6384',
        backgroundColor: 'rgba(255, 99, 132, 0.2)',
        tension: 0.4,
        borderWidth: 2
      }
    ]
  };

  const chartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'top',
      },
      title: {
        display: true,
        text: '억양 패턴 비교'
      }
    },
    scales: {
      x: {
        title: {
          display: true,
          text: '시간 (초)'
        }
      },
      y: {
        title: {
          display: true,
          text: '주파수 (Hz)'
        },
        beginAtZero: false
      }
    }
  };

  // 점수 색상
  const scoreColor = result.score >= 90 ? '#4CAF50' :
                    result.score >= 80 ? '#8BC34A' :
                    result.score >= 70 ? '#FFC107' :
                    result.score >= 60 ? '#FF9800' : '#F44336';

  return (
    <div className="intonation-analysis">
      {/* 점수 표시 */}
      <div className="score-section">
        <div
          className="score-circle"
          style={{ borderColor: scoreColor }}
        >
          <span className="score-number">{result.score}</span>
          <span className="score-label">점</span>
        </div>
        <p className="feedback">{result.feedback}</p>
      </div>

      {/* 차트 */}
      <div className="chart-section" style={{ height: '400px' }}>
        <Line data={chartData} options={chartOptions} />
      </div>

      {/* 통계 정보 */}
      {result.statistics.reference && (
        <div className="statistics-section">
          <h4>상세 분석</h4>
          <div className="stat-grid">
            <div className="stat-item">
              <span className="stat-label">평균 주파수 차이</span>
              <span className="stat-value">
                {result.statistics.pitch_difference.toFixed(1)} Hz
              </span>
            </div>
            <div className="stat-item">
              <span className="stat-label">나의 평균 주파수</span>
              <span className="stat-value">
                {result.statistics.user.mean.toFixed(1)} Hz
              </span>
            </div>
            <div className="stat-item">
              <span className="stat-label">정답 평균 주파수</span>
              <span className="stat-value">
                {result.statistics.reference.mean.toFixed(1)} Hz
              </span>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default IntonationAnalysis;
```

---

## 🎯 핵심 요약

### 프론트엔드가 할 일

1. ✅ **`pitch_data` 사용** - 차트 그리기 (100개 고정)
2. ✅ **`score` 표시** - 큰 숫자로 표시
3. ✅ **`feedback` 표시** - 사용자에게 조언
4. ✅ **`score = -1` 처리** - 에러 메시지 표시
5. 선택: `statistics` 표시 (상세 정보)

### 백엔드가 해준 것

1. ✅ 길이 통일 (100개)
2. ✅ 보간 완료
3. ✅ 시간 축 생성
4. ✅ 통계 계산

---

## 📞 문의

프론트엔드 구현 중 문제 발생 시:
1. `score = -1` 확인 (분석 불가 케이스)
2. `pitch_data` 배열이 비어있지 않은지 확인
3. 차트 라이브러리 설치 확인 (Chart.js, Recharts 등)
