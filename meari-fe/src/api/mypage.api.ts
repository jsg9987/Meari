import type { AxiosResponse } from 'axios'
import type { ApiResponse } from './auth.api'
import axiosInstance from './axiosInstance'

// 목업/실제 API 전환 플래그 (true: 목업, false: 실제 API)
const USE_MOCK_MYPAGE = false

export interface MyPageProfile {
  nickname: string
  email: string
  profile_image_url: string
  native_language: 'KR' | 'VN' | 'EN'
}

export type MyPageProfileResponse = AxiosResponse<ApiResponse<MyPageProfile>>

export const getMyPageProfile = async (): Promise<MyPageProfileResponse> => {
  const response = await axiosInstance.get<ApiResponse<MyPageProfile>>('/mypage')
  return response
}

export interface UpdateMyPageProfilePayload {
  nickname: string
  native_language: 'KR' | 'VN' | 'EN'
}

export type UpdateMyPageProfileResponse = AxiosResponse<ApiResponse<null>>

export const updateMyPageProfile = async (
  payload: UpdateMyPageProfilePayload
): Promise<UpdateMyPageProfileResponse> => {
  const response = await axiosInstance.patch<ApiResponse<null>>('/mypage/change', payload)
  return response
}

export interface CheckPasswordPayload {
  password: string
}

export type CheckPasswordResponse = AxiosResponse<ApiResponse<null>>

export const checkMyPagePassword = async (
  payload: CheckPasswordPayload
): Promise<CheckPasswordResponse> => {
  const response = await axiosInstance.post<ApiResponse<null>>('/mypage/check-password', payload)
  return response
}

export interface ChangePasswordPayload {
  new_password: string
}

export type ChangePasswordResponse = AxiosResponse<ApiResponse<null>>

export const changeMyPagePassword = async (
  payload: ChangePasswordPayload
): Promise<ChangePasswordResponse> => {
  const response = await axiosInstance.patch<ApiResponse<null>>('/mypage/change/pw', payload)
  return response
}

export interface UploadProfileImagePayload {
  fileName: string
  contentType: string
  fileSize: number
  width: number
  height: number
}

export interface UploadProfileImageData {
  uploadUrl: string
  s3Key: string
  profileUrl: string
  expiresIn: number
}

export type UploadProfileImageResponse = AxiosResponse<ApiResponse<UploadProfileImageData>>

export const requestProfileImageUploadUrl = async (
  payload: UploadProfileImagePayload
): Promise<UploadProfileImageResponse> => {
  const response = await axiosInstance.post<ApiResponse<UploadProfileImageData>>(
    '/member/profile-image/upload-url',
    payload
  )
  return response
}

export interface ConfirmProfileImagePayload {
  profileUrl: string
}

export type ConfirmProfileImageResponse = AxiosResponse<ApiResponse<null>>

export const confirmProfileImage = async (
  payload: ConfirmProfileImagePayload
): Promise<ConfirmProfileImageResponse> => {
  const response = await axiosInstance.post<ApiResponse<null>>('/member/profile-image', payload)
  return response
}

export interface GetProfileImageData {
  profileUrl: string
}

export type GetProfileImageResponse = AxiosResponse<ApiResponse<GetProfileImageData>>

export const getProfileImage = async (memberId: number): Promise<GetProfileImageResponse> => {
  const response = await axiosInstance.get<ApiResponse<GetProfileImageData>>(
    `/member/${memberId}/profile-image`
  )
  return response
}

// 쉐도잉 연습 기록 타입
export interface ShadowingPracticeRecord {
  idx: number
  date: string // YYYY-MM-DD 형식
  accuracy: number // 정확도 (0-100)
  intonation: number // 억양 (0-100)
  errorsCount: number // 오류 개수
}

export type ShadowingPracticeHistoryResponse = AxiosResponse<
  ApiResponse<ShadowingPracticeRecord[]>
>

// 목업 데이터
const mockPracticeHistory: ShadowingPracticeRecord[] = [
  { idx: 1, date: '2026-02-02', accuracy: 85, intonation: 90, errorsCount: 1 },
  { idx: 2, date: '2026-02-01', accuracy: 82, intonation: 88, errorsCount: 0 },
  { idx: 3, date: '2026-01-31', accuracy: 79, intonation: 86, errorsCount: 3 },
  { idx: 4, date: '2026-01-30', accuracy: 75, intonation: 83, errorsCount: 4 },
  { idx: 5, date: '2026-01-29', accuracy: 72, intonation: 80, errorsCount: 5 }
]

// 목업 API - 쉐도잉 연습 기록 조회
export const getShadowingPracticeHistoryMock = async (): Promise<
  ShadowingPracticeHistoryResponse
> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({
        data: {
          success: true,
          data: mockPracticeHistory,
          error: null
        }
      } as ShadowingPracticeHistoryResponse)
    }, 300)
  })
}

// 실제 API - 쉐도잉 연습 기록 조회
export const getShadowingPracticeHistoryAPI = async (): Promise<
  ShadowingPracticeHistoryResponse
> => {
  const response = await axiosInstance.get<ApiResponse<ShadowingPracticeRecord[]>>(
    '/dashboard/me/shadowing-practice-history'
  )
  return response
}

// 목업/실제 API 전환
export const getShadowingPracticeHistory = USE_MOCK_MYPAGE
  ? getShadowingPracticeHistoryMock
  : getShadowingPracticeHistoryAPI

// KOPIC 요약 데이터 타입
export interface KopicSentenceScore {
  kopic_sentence_id: number
  score: number
}

export interface KopicSentenceAvgScore {
  kopic_sentence_id: number
  avg_score: number
}

export interface KopicBestScore {
  exam_date: string // YYYY-MM-DD 형식
  total_avg_score: number
  sentences: KopicSentenceScore[]
}

export interface KopicAverageScore {
  total_avg_score: number
  sentences: KopicSentenceAvgScore[]
}

export interface KopicSummaryData {
  best: KopicBestScore
  average: KopicAverageScore
}

export type KopicSummaryResponse = AxiosResponse<ApiResponse<KopicSummaryData>>

// 목업 데이터 - KOPIC 요약
const mockKopicSummary: KopicSummaryData = {
  best: {
    exam_date: '2026-02-01',
    total_avg_score: 82,
    sentences: [
      { kopic_sentence_id: 1, score: 80 },
      { kopic_sentence_id: 2, score: 85 },
      { kopic_sentence_id: 3, score: 78 },
      { kopic_sentence_id: 4, score: 88 },
      { kopic_sentence_id: 5, score: 79 }
    ]
  },
  average: {
    total_avg_score: 75,
    sentences: [
      { kopic_sentence_id: 1, avg_score: 72 },
      { kopic_sentence_id: 2, avg_score: 76 },
      { kopic_sentence_id: 3, avg_score: 74 },
      { kopic_sentence_id: 4, avg_score: 77 },
      { kopic_sentence_id: 5, avg_score: 76 }
    ]
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

// 실제 API - KOPIC 요약 데이터 조회
export const getKopicSummaryAPI = async (): Promise<KopicSummaryResponse> => {
  const response = await axiosInstance.get<ApiResponse<KopicSummaryData>>(
    '/dashboard/me/kopic-summary'
  )
  return response
}

// 목업/실제 API 전환
export const getKopicSummary = USE_MOCK_MYPAGE ? getKopicSummaryMock : getKopicSummaryAPI

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

// 일일 활동 기록 (새 API) 타입
export type DailyRecordPeriod = 'yearly' | 'weekly' | 'monthly'

export interface DailyRecordActivity {
  date: string // YYYY-MM-DD 형식
  completed_count: number
}

export interface DailyRecordsData {
  startDate: string // YYYY-MM-DD 형식
  endDate: string // YYYY-MM-DD 형식
  activities: DailyRecordActivity[]
}

export type DailyRecordsResponse = AxiosResponse<ApiResponse<DailyRecordsData>>

// 목업 데이터 - 일일 활동 기록
const generateMockDailyRecords = (period: DailyRecordPeriod): DailyRecordsData => {
  const today = new Date()
  let startDate: Date
  let endDate: Date = new Date(today)

  switch (period) {
    case 'yearly':
      startDate = new Date(today.getFullYear(), 0, 1)
      endDate = new Date(today.getFullYear(), 11, 31)
      break
    case 'monthly':
      startDate = new Date(today.getFullYear(), today.getMonth(), 1)
      endDate = new Date(today.getFullYear(), today.getMonth() + 1, 0)
      break
    case 'weekly': {
      const dayOfWeek = today.getDay()
      startDate = new Date(today)
      startDate.setDate(today.getDate() - dayOfWeek)
      endDate = new Date(startDate)
      endDate.setDate(startDate.getDate() + 6)
      break
    }
  }

  const activities: DailyRecordActivity[] = []

  // 랜덤으로 일부 날짜에 활동 추가
  const currentDate = new Date(startDate)
  while (currentDate <= endDate) {
    const rand = Math.random()
    if (rand > 0.3) { // 70% 확률로 활동 기록
      activities.push({
        date: currentDate.toISOString().split('T')[0],
        completed_count: Math.floor(Math.random() * 5) + 1 // 1-5개
      })
    }
    currentDate.setDate(currentDate.getDate() + 1)
  }

  return {
    startDate: startDate.toISOString().split('T')[0],
    endDate: endDate.toISOString().split('T')[0],
    activities
  }
}

// 목업 API - 일일 활동 기록 조회
export const getDailyRecordsMock = async (
  period: DailyRecordPeriod = 'yearly'
): Promise<DailyRecordsResponse> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({
        data: {
          success: true,
          data: generateMockDailyRecords(period),
          error: null
        }
      } as DailyRecordsResponse)
    }, 300)
  })
}

// 실제 API - 일일 활동 기록 조회
export const getDailyRecordsAPI = async (
  period: DailyRecordPeriod = 'yearly'
): Promise<DailyRecordsResponse> => {
  const response = await axiosInstance.get<ApiResponse<DailyRecordsData>>(
    '/dashboard/me/daily-records',
    { params: { period } }
  )
  return response
}

// 목업/실제 API 전환
export const getDailyRecords = USE_MOCK_MYPAGE ? getDailyRecordsMock : getDailyRecordsAPI

// 최근 활동 내역 타입
export type RecentActivityType = 'DAILY' | 'SHADOWING' | 'KOPIC'
export type RecentActivityStatus = 'COMPLETED' | 'PROCESSING'

export interface RecentActivity {
  activity_type: RecentActivityType
  title: string
  status: RecentActivityStatus
  created_at: string // ISO 8601 형식
  theme?: string // SHADOWING, KOPIC에만 존재
  content?: string // SHADOWING에만 존재
}

export type RecentActivitiesResponse = AxiosResponse<ApiResponse<RecentActivity[]>>

// 목업 데이터 - 최근 활동 내역
const mockRecentActivities: RecentActivity[] = [
  {
    activity_type: 'DAILY',
    title: '일일학습 완료했습니다!',
    status: 'COMPLETED',
    created_at: '2026-02-01T20:10:00'
  },
  {
    activity_type: 'SHADOWING',
    theme: '공공장소',
    content: '카페에서 커피 주문하기',
    title: '쉐도잉(공공장소) - 카페에서 커피 주문하기를 완료했습니다!',
    status: 'COMPLETED',
    created_at: '2026-02-01T19:40:00'
  },
  {
    activity_type: 'KOPIC',
    theme: '비즈니스',
    title: 'KOPIC(비즈니스) 채점이 완료되었습니다!',
    status: 'COMPLETED',
    created_at: '2026-02-01T18:05:00'
  },
  {
    activity_type: 'DAILY',
    title: '일일학습 완료했습니다!',
    status: 'COMPLETED',
    created_at: '2026-01-31T15:30:00'
  },
  {
    activity_type: 'SHADOWING',
    theme: '비즈니스',
    content: '협상 기초',
    title: '쉐도잉(비즈니스) - 협상 기초를 완료했습니다!',
    status: 'COMPLETED',
    created_at: '2026-01-31T13:15:00'
  },
  {
    activity_type: 'KOPIC',
    theme: '일상생활',
    title: 'KOPIC(일상생활) 채점이 완료되었습니다!',
    status: 'COMPLETED',
    created_at: '2026-01-30T18:20:00'
  }
]

// 목업 API - 최근 활동 내역 조회
export const getRecentActivitiesMock = async (): Promise<RecentActivitiesResponse> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({
        data: {
          success: true,
          data: mockRecentActivities,
          error: null
        }
      } as RecentActivitiesResponse)
    }, 300)
  })
}

// 실제 API - 최근 활동 내역 조회
export const getRecentActivitiesAPI = async (): Promise<RecentActivitiesResponse> => {
  const response = await axiosInstance.get<ApiResponse<RecentActivity[]>>(
    '/dashboard/me/activities'
  )
  return response
}

// 목업/실제 API 전환
export const getRecentActivities = USE_MOCK_MYPAGE
  ? getRecentActivitiesMock
  : getRecentActivitiesAPI

// 쉐도잉 리포트 타입
export interface ShadowingReport {
  shadowing_report_id: number
  thumbnail_url: string
  room_title: string
  content_title: string
  total_score: number
  is_read: boolean
  created_at: string // ISO 8601 형식
}

export interface ShadowingReportListData {
  contents: ShadowingReport[]
  next_cursor: number | null
  has_next: boolean
  size: number
}

export type ShadowingReportListResponse = AxiosResponse<ApiResponse<ShadowingReportListData>>

// 목업 데이터 - 쉐도잉 리포트 목록
const generateMockShadowingReports = (page: number, limit: number): ShadowingReport[] => {
  const contentTitles = [
    'Grocery inflation',
    'Business meeting etiquette',
    'Daily conversation',
    'Restaurant reservation',
    'Shopping at the mall'
  ]
  const roomTitles = ['초보만', '비즈니스반', 'Level 3 그룹', '프리토킹 룸', '초급반']

  const reports: ShadowingReport[] = []
  const startIdx = page * limit

  for (let i = 0; i < limit; i++) {
    const idx = startIdx + i
    if (idx >= 50) break // 총 50개의 데이터만 생성

    const date = new Date()
    date.setDate(date.getDate() - idx)
    const dateString = date.toISOString()

    const titleIdx = idx % contentTitles.length
    reports.push({
      shadowing_report_id: 1000 + idx + 1,
      thumbnail_url: `https://picsum.photos/seed/${idx}/400/300`,
      content_title: contentTitles[titleIdx],
      room_title: `${roomTitles[titleIdx]} - ${Math.floor(idx / 5) + 1}차`,
      total_score: Math.floor(Math.random() * 30) + 70,
      is_read: Math.random() > 0.5,
      created_at: dateString
    })
  }

  return reports
}

// 목업 API - 쉐도잉 리포트 목록 조회 (무한 스크롤)
export const getShadowingReportsMock = async (
  page: number = 0,
  limit: number = 10
): Promise<ShadowingReportListResponse> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      const contents = generateMockShadowingReports(page, limit)

      const hasNext = (page + 1) * limit < 50
      const nextCursor = hasNext ? page + 1 : null

      resolve({
        data: {
          success: true,
          data: {
            contents,
            next_cursor: nextCursor,
            has_next: hasNext,
            size: contents.length
          },
          error: null
        }
      } as ShadowingReportListResponse)
    }, 300)
  })
}

// 실제 API - 쉐도잉 리포트 목록 조회
export const getShadowingReportsAPI = async (
  cursor?: number,
  size: number = 10
): Promise<ShadowingReportListResponse> => {
  const params = new URLSearchParams()
  if (cursor !== undefined) {
    params.append('cursor', cursor.toString())
  }
  params.append('size', size.toString())

  const response = await axiosInstance.get<ApiResponse<ShadowingReportListData>>(
    `/api/v1/shadowing/reports?${params.toString()}`
  )
  return response
}

// 환경 변수에 따라 목업 또는 실제 API 사용
export const getShadowingReports = USE_MOCK_MYPAGE
  ? getShadowingReportsMock
  : getShadowingReportsAPI

// 코픽(OPIc) 리포트 타입
export interface KopicReport {
  kopic_report_id: number
  thumbnail_url: string
  theme: string
  total_score: number
  is_read: boolean
  created_at: string // ISO 8601 형식
}

export interface KopicReportListData {
  contents: KopicReport[]
  next_cursor: number | null
  has_next: boolean
  size: number
}

export type KopicReportListResponse = AxiosResponse<ApiResponse<KopicReportListData>>

// 목업 데이터 - 코픽 리포트 목록
const generateMockKopicReports = (page: number, limit: number): KopicReport[] => {
  const themes = ['news', 'business', 'daily', 'travel', 'shopping']
  const reports: KopicReport[] = []
  const startIdx = page * limit

  for (let i = 0; i < limit; i++) {
    const idx = startIdx + i
    if (idx >= 30) break // 총 30개의 데이터만 생성

    const date = new Date()
    date.setDate(date.getDate() - idx)
    const dateString = date.toISOString()

    const themeIdx = idx % themes.length
    reports.push({
      kopic_report_id: 2000 + idx + 1,
      thumbnail_url: `https://picsum.photos/seed/kopic${idx}/400/300`,
      theme: themes[themeIdx],
      total_score: Math.floor(Math.random() * 30) + 70,
      is_read: Math.random() > 0.5,
      created_at: dateString
    })
  }

  return reports
}

// 목업 API - 코픽 리포트 목록 조회
export const getKopicReportsMock = async (
  page: number = 0,
  limit: number = 10
): Promise<KopicReportListResponse> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      const contents = generateMockKopicReports(page, limit)
      const hasNext = (page + 1) * limit < 30
      const nextCursor = hasNext ? page + 1 : null

      resolve({
        data: {
          success: true,
          data: {
            contents,
            next_cursor: nextCursor,
            has_next: hasNext,
            size: contents.length
          },
          error: null
        }
      } as KopicReportListResponse)
    }, 300)
  })
}

// 실제 API - 코픽 리포트 목록 조회
export const getKopicReportsAPI = async (
  cursor?: number,
  size: number = 10
): Promise<KopicReportListResponse> => {
  const params = new URLSearchParams()
  if (cursor !== undefined) {
    params.append('cursor', cursor.toString())
  }
  params.append('size', size.toString())

  const response = await axiosInstance.get<ApiResponse<KopicReportListData>>(
    `/api/v1/kopic/reports?${params.toString()}`
  )
  return response
}

export const getKopicReports = USE_MOCK_MYPAGE ? getKopicReportsMock : getKopicReportsAPI

// 쉐도잉 리포트 상세 타입
export interface SyllableError {
  type: string // "replace" | "insert" | "delete"
  position: number
  expected: string
  actual: string
  confidence: number
  description: string
}

export interface IntonationData {
  score: number
  reference_pitch: number[]
  user_pitch: number[]
  time_frames: number[]
  dtw_path: number[][]
  feedback: string
}

export interface SentenceAnalysis {
  sentence_id: number
  text_expected: string
  text_recognized: string
  accuracy: number
  mean_confidence: number
  syllables: string[]
  syllable_confidences: number[]
  errors: SyllableError[]
  intonation: IntonationData
}

export interface DetailedAnalysisSummary {
  total_sentences: number
  analyzed_sentences: number
  average_accuracy: number
  average_confidence: number
  average_intonation: number
}

export interface DetailedAnalysis {
  sentences: SentenceAnalysis[]
  summary: DetailedAnalysisSummary
}

export interface ShadowingReportDetail {
  shadowing_report_id: number
  member_id: number
  member_nickname: string
  room_id: number
  room_title: string
  content_id: number
  content_title: string
  role_id: number
  role_name: string
  accuracy: number
  intonation: number
  total_score: number
  detailed_analysis: DetailedAnalysis
  status: string // "COMPLETED" | "PROCESSING"
  created_at: string // ISO 8601 형식
  updated_at: string // ISO 8601 형식
}

export type ShadowingReportDetailResponse = AxiosResponse<ApiResponse<ShadowingReportDetail>>

// 목업 데이터 - 쉐도잉 리포트 상세
const mockShadowingReportDetail: ShadowingReportDetail = {
  shadowing_report_id: 1001,
  member_id: 1,
  member_nickname: '홍길동',
  room_id: 123,
  room_title: '한국어 연습방',
  content_id: 101,
  content_title: '일상 대화',
  role_id: 1,
  role_name: '손님',
  accuracy: 85,
  intonation: 90,
  total_score: 87,
  detailed_analysis: {
    sentences: [
      {
        sentence_id: 1,
        text_expected: '안녕하세요',
        text_recognized: '안영하세요',
        accuracy: 80,
        mean_confidence: 0.9739,
        syllables: ['안', '영', '하', '세', '요'],
        syllable_confidences: [0.95, 0.88, 0.98, 0.99, 0.97],
        errors: [
          {
            type: 'replace',
            position: 1,
            expected: '녕',
            actual: '영',
            confidence: 0.8879,
            description: "'녕' → '영' 대체"
          }
        ],
        intonation: {
          score: 85,
          reference_pitch: [120.5, 125.3, 130.2, 128.9, 122.1],
          user_pitch: [118.2, 123.8, 131.5, 127.3, 120.8],
          time_frames: [0.0, 0.1, 0.2, 0.3, 0.4],
          dtw_path: [[0, 0], [1, 1], [2, 2], [3, 3], [4, 4]],
          feedback: '억양이 매우 자연스럽습니다!'
        }
      },
      {
        sentence_id: 2,
        text_expected: '저는 학생입니다',
        text_recognized: '저는 학생입니다',
        accuracy: 100,
        mean_confidence: 0.9856,
        syllables: ['저', '는', '학', '생', '입', '니', '다'],
        syllable_confidences: [0.99, 0.98, 0.99, 0.98, 0.97, 0.99, 0.98],
        errors: [],
        intonation: {
          score: 95,
          reference_pitch: [115.2, 118.5, 125.8, 128.2, 120.5, 118.3, 115.8],
          user_pitch: [114.8, 118.9, 126.2, 127.8, 120.2, 118.5, 115.5],
          time_frames: [0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6],
          dtw_path: [[0, 0], [1, 1], [2, 2], [3, 3], [4, 4], [5, 5], [6, 6]],
          feedback: '완벽한 억양입니다!'
        }
      },
      {
        sentence_id: 3,
        text_expected: '오늘 날씨가 좋네요',
        text_recognized: '오늘 날시가 조네요',
        accuracy: 70,
        mean_confidence: 0.8234,
        syllables: ['오', '늘', '날', '시', '가', '조', '네', '요'],
        syllable_confidences: [0.95, 0.93, 0.88, 0.65, 0.89, 0.75, 0.92, 0.94],
        errors: [
          {
            type: 'replace',
            position: 3,
            expected: '씨',
            actual: '시',
            confidence: 0.6523,
            description: "'씨' → '시' 대체"
          },
          {
            type: 'replace',
            position: 5,
            expected: '좋',
            actual: '조',
            confidence: 0.7489,
            description: "'좋' → '조' 대체"
          }
        ],
        intonation: {
          score: 78,
          reference_pitch: [122.3, 124.5, 128.9, 132.1, 130.5, 125.8, 123.2, 120.5],
          user_pitch: [120.8, 123.2, 127.5, 130.8, 129.2, 124.5, 122.8, 119.8],
          time_frames: [0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7],
          dtw_path: [[0, 0], [1, 1], [2, 2], [3, 3], [4, 4], [5, 5], [6, 6], [7, 7]],
          feedback: '약간 평평한 억양입니다. 감정을 좀 더 넣어보세요.'
        }
      }
    ],
    summary: {
      total_sentences: 10,
      analyzed_sentences: 10,
      average_accuracy: 85,
      average_confidence: 0.9234,
      average_intonation: 85
    }
  },
  status: 'COMPLETED',
  created_at: '2025-01-30T10:00:00',
  updated_at: '2025-01-30T10:30:00'
}

// 목업 API - 쉐도잉 리포트 상세 조회
export const getShadowingReportDetailMock = async (
  reportId: number
): Promise<ShadowingReportDetailResponse> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({
        data: {
          success: true,
          data: {
            ...mockShadowingReportDetail,
            shadowing_report_id: reportId
          },
          error: null
        }
      } as ShadowingReportDetailResponse)
    }, 500)
  })
}

// 실제 API - 쉐도잉 리포트 상세 조회
export const getShadowingReportDetailAPI = async (
  reportId: number
): Promise<ShadowingReportDetailResponse> => {
  const response = await axiosInstance.get<ApiResponse<ShadowingReportDetail>>(
    `/api/v1/reports/shadowing/${reportId}`
  )
  return response
}

// 환경 변수에 따라 목업 또는 실제 API 사용
export const getShadowingReportDetail = USE_MOCK_MYPAGE
  ? getShadowingReportDetailMock
  : getShadowingReportDetailAPI

// 사전 학습 타입
export interface PreStudyItem {
  id: number
  contentName: string
  isCompleted: boolean
  completedAt?: string // YYYY-MM-DD 형식
}

export interface PreStudyData {
  items: PreStudyItem[]
}

export type PreStudyResponse = AxiosResponse<ApiResponse<PreStudyData>>

// 목업 데이터 - 사전 학습 목록
const mockPreStudyItems: PreStudyItem[] = [
  {
    id: 1,
    contentName: '기초 문법 - 현재시제',
    isCompleted: true,
    completedAt: '2026-02-01'
  },
  {
    id: 2,
    contentName: '기초 문법 - 과거시제',
    isCompleted: true,
    completedAt: '2026-02-02'
  },
  {
    id: 3,
    contentName: '기초 문법 - 미래시제',
    isCompleted: false
  },
  {
    id: 4,
    contentName: '필수 단어 100개',
    isCompleted: true,
    completedAt: '2026-01-30'
  },
  {
    id: 5,
    contentName: '발음 연습 - 모음',
    isCompleted: false
  }
]

// 목업 API - 사전 학습 목록 조회
export const getPreStudyItemsMock = async (): Promise<PreStudyResponse> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({
        data: {
          success: true,
          data: {
            items: mockPreStudyItems
          },
          error: null
        }
      } as PreStudyResponse)
    }, 300)
  })
}

// 현재는 목업 사용
export const getPreStudyItems = getPreStudyItemsMock
// export const getShadowingReports = getShadowingReportsAPI
