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
