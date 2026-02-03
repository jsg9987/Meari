import type { AxiosResponse } from 'axios'
import type { ApiResponse } from './auth.api'

// 쉐도잉 연습 기록 타입
export interface ShadowingPracticeRecord {
  idx: number
  date: string // YYYY-MM-DD 형식
  accuracy: number // 정확도 (0-100)
  intonation: number // 억양 (0-100)
  errorsCount: number // 오류 개수
}

export interface ShadowingPracticeHistoryData {
  last5: ShadowingPracticeRecord[]
}

export type ShadowingPracticeHistoryResponse = AxiosResponse<
  ApiResponse<ShadowingPracticeHistoryData>
>

// 목업 데이터
const mockPracticeHistory: ShadowingPracticeRecord[] = [
  { idx: 1, date: '2026-02-02', accuracy: 85, intonation: 90, errorsCount: 1 },
  { idx: 2, date: '2026-02-01', accuracy: 82, intonation: 88, errorsCount: 0 },
  { idx: 3, date: '2026-01-31', accuracy: 79, intonation: 86, errorsCount: 3 },
  { idx: 4, date: '2026-01-30', accuracy: 75, intonation: 83, errorsCount: 4 },
  { idx: 5, date: '2026-01-29', accuracy: 72, intonation: 80, errorsCount: 5 }
]

// 목업 API - 쉐도잉 연습 기록 조회 (최근 5개)
export const getShadowingPracticeHistoryMock = async (): Promise<
  ShadowingPracticeHistoryResponse
> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({
        data: {
          success: true,
          data: {
            last5: mockPracticeHistory
          },
          error: null
        }
      } as ShadowingPracticeHistoryResponse)
    }, 300)
  })
}

// 실제 API는 나중에 구현
// export const getShadowingPracticeHistory = async (): Promise<ShadowingPracticeHistoryResponse> => {
//   const response = await axiosInstance.get<ApiResponse<ShadowingPracticeHistoryData>>('/mypage/shadowing-practice-history')
//   return response
// }

// 현재는 목업 사용
export const getShadowingPracticeHistory = getShadowingPracticeHistoryMock

// KOPIC 요약 데이터 타입
export interface KopicQuestionScores {
  q1: number
  q2: number
  q3: number
  q4: number
  q5: number
}

export interface KopicMaxScore {
  exam_id: number
  taken_at: string // YYYY-MM-DD 형식
  total_average_score: number
  question_scores: KopicQuestionScores
}

export interface KopicAverageScore {
  total_average_score: number
  question_average_scores: KopicQuestionScores
}

export interface KopicLatestScore {
  exam_id: number
  taken_at: string // YYYY-MM-DD 형식
  total_average_score: number
  question_scores: KopicQuestionScores
}

export interface KopicSummaryData {
  copick_summary: {
    max_score: KopicMaxScore
    average_score: KopicAverageScore
    latest_score: KopicLatestScore
  }
}

export type KopicSummaryResponse = AxiosResponse<ApiResponse<KopicSummaryData>>

// 목업 데이터 - KOPIC 요약
const mockKopicSummary: KopicSummaryData = {
  copick_summary: {
    max_score: {
      exam_id: 3021,
      taken_at: '2026-02-01',
      total_average_score: 82,
      question_scores: {
        q1: 80,
        q2: 85,
        q3: 78,
        q4: 88,
        q5: 79
      }
    },
    average_score: {
      total_average_score: 75,
      question_average_scores: {
        q1: 72,
        q2: 76,
        q3: 74,
        q4: 78,
        q5: 75
      }
    },
    latest_score: {
      exam_id: 3050,
      taken_at: '2026-02-02',
      total_average_score: 79,
      question_scores: {
        q1: 75,
        q2: 80,
        q3: 77,
        q4: 82,
        q5: 81
      }
    }
  }
}

// 목업 API - KOPIC 요약 데이터 조회
export const getKopicSummaryMock = async (): Promise<KopicSummaryResponse> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({
        data: {
          success: true,
          data: mockKopicSummary,
          error: null
        }
      } as KopicSummaryResponse)
    }, 300)
  })
}

// 실제 API는 나중에 구현
// export const getKopicSummary = async (): Promise<KopicSummaryResponse> => {
//   const response = await axiosInstance.get<ApiResponse<KopicSummaryData>>('/mypage/kopic-summary')
//   return response
// }

// 현재는 목업 사용
export const getKopicSummary = getKopicSummaryMock

// 일일 활동 타입
export type DailyActivityStatus = 'NONE' | 'WORD' | 'SENTENCE' | 'BOTH'

export interface DailyActivity {
  date: string // YYYY-MM-DD 형식
  status: DailyActivityStatus
}

export interface DailyActivityData {
  activities: DailyActivity[]
}

export type DailyActivityResponse = AxiosResponse<ApiResponse<DailyActivityData>>

// 목업 데이터 - 일일 활동 (최근 1년)
const generateMockDailyActivities = (): DailyActivity[] => {
  const activities: DailyActivity[] = []
  const today = new Date()

  // 최근 365일 데이터 생성
  for (let i = 364; i >= 0; i--) {
    const date = new Date(today)
    date.setDate(date.getDate() - i)
    const dateString = date.toISOString().split('T')[0]

    // 랜덤으로 상태 할당 (학습 X는 30% 확률)
    const rand = Math.random()
    let status: DailyActivityStatus
    if (rand < 0.3) {
      status = 'NONE'
    } else if (rand < 0.5) {
      status = 'WORD'
    } else if (rand < 0.7) {
      status = 'SENTENCE'
    } else {
      status = 'BOTH'
    }

    activities.push({ date: dateString, status })
  }

  return activities
}

const mockDailyActivities: DailyActivityData = {
  activities: generateMockDailyActivities()
}

// 목업 API - 일일 활동 데이터 조회
export const getDailyActivityMock = async (): Promise<DailyActivityResponse> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({
        data: {
          success: true,
          data: mockDailyActivities,
          error: null
        }
      } as DailyActivityResponse)
    }, 300)
  })
}

// 실제 API는 나중에 구현
// export const getDailyActivity = async (): Promise<DailyActivityResponse> => {
//   const response = await axiosInstance.get<ApiResponse<DailyActivityData>>('/mypage/daily-activity')
//   return response
// }

// 현재는 목업 사용
export const getDailyActivity = getDailyActivityMock
