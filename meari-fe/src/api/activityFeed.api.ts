import axiosInstance from './axiosInstance'
import { apiConfig } from './apiConfig'
import type { AxiosResponse } from 'axios'
import type { ApiResponse } from './auth.api'

export type ActivityType = 'DAILY_LEARNING' | 'COPIC' | 'SHADOWING'

export type DailyLearningCategory = 'WORD' | 'SENTENCE'
export type DailyLearningStatus = 'COMPLETED' | 'IN_PROGRESS'

export type CopicEvent = 'EXAM_COMPLETED' | 'GRADING_COMPLETED'
export type CopicStatus = 'IN_PROGRESS' | 'COMPLETED'

export type ShadowingStatus = 'STARTED' | 'COMPLETED'

export interface DailyLearningPayload {
  dailyLearningId: number
  category: DailyLearningCategory
  status: DailyLearningStatus
}

export interface CopicPayload {
  copicId: number
  theme: string
  event: CopicEvent
  status: CopicStatus
}

export interface ShadowingPayload {
  shadowingId: number
  theme: string
  contentName: string
  status: ShadowingStatus
}

export type ActivityItem =
  | {
      activityId: string
      type: 'DAILY_LEARNING'
      occurredAt: string
      title: string
      dateLabel: string
      timeLabel: string
      payload: DailyLearningPayload
      deeplink?: string
    }
  | {
      activityId: string
      type: 'COPIC'
      occurredAt: string
      title: string
      dateLabel: string
      timeLabel: string
      payload: CopicPayload
      deeplink?: string
    }
  | {
      activityId: string
      type: 'SHADOWING'
      occurredAt: string
      title: string
      dateLabel: string
      timeLabel: string
      payload: ShadowingPayload
      deeplink?: string
    }

export interface ActivityFeedData {
  items: ActivityItem[]
  nextCursor: string | null
  hasNext: boolean
}

export interface ActivityFeedRequest {
  cursor?: string
  size?: number
  types?: ActivityType[] | string
}

export type ActivityFeedResponse = AxiosResponse<ApiResponse<ActivityFeedData>>

const mockActivities: ActivityItem[] = [
  {
    activityId: 'act_1008',
    type: 'DAILY_LEARNING',
    occurredAt: '2026-02-01T20:10:00+09:00',
    title: 'Daily learning (word) completed.',
    dateLabel: '1st Feb.',
    timeLabel: '8:10 pm',
    payload: {
      dailyLearningId: 12348,
      category: 'WORD',
      status: 'COMPLETED'
    },
    deeplink: '/daily-learning/12348'
  },
  {
    activityId: 'act_1007',
    type: 'SHADOWING',
    occurredAt: '2026-02-01T19:40:00+09:00',
    title: 'Shadowing (Public Place) - Ordering Coffee at a Cafe started.',
    dateLabel: '1st Feb.',
    timeLabel: '7:40 pm',
    payload: {
      shadowingId: 557,
      theme: 'PUBLIC_PLACE',
      contentName: 'Ordering Coffee at a Cafe',
      status: 'STARTED'
    },
    deeplink: '/shadowing/557'
  },
  {
    activityId: 'act_1006',
    type: 'COPIC',
    occurredAt: '2026-02-01T18:05:12+09:00',
    title: 'KOPIC (Business) grading completed.',
    dateLabel: '1st Feb.',
    timeLabel: '6:05 pm',
    payload: {
      copicId: 989,
      theme: 'BUSINESS',
      event: 'GRADING_COMPLETED',
      status: 'COMPLETED'
    },
    deeplink: '/copic/989'
  },
  {
    activityId: 'act_1005',
    type: 'DAILY_LEARNING',
    occurredAt: '2026-02-01T15:30:00+09:00',
    title: 'Daily learning (sentence) completed.',
    dateLabel: '1st Feb.',
    timeLabel: '3:30 pm',
    payload: {
      dailyLearningId: 12347,
      category: 'SENTENCE',
      status: 'COMPLETED'
    },
    deeplink: '/daily-learning/12347'
  },
  {
    activityId: 'act_1004',
    type: 'SHADOWING',
    occurredAt: '2026-02-01T13:15:05+09:00',
    title: 'Shadowing (Business) - Negotiation Basics completed.',
    dateLabel: '1st Feb.',
    timeLabel: '1:15 pm',
    payload: {
      shadowingId: 556,
      theme: 'BUSINESS',
      contentName: 'Negotiation Basics',
      status: 'COMPLETED'
    },
    deeplink: '/shadowing/556'
  },
  {
    activityId: 'act_1003',
    type: 'DAILY_LEARNING',
    occurredAt: '2026-02-01T11:00:00+09:00',
    title: 'Daily learning (word) completed.',
    dateLabel: '1st Feb.',
    timeLabel: '11:00 am',
    payload: {
      dailyLearningId: 12345,
      category: 'WORD',
      status: 'COMPLETED'
    },
    deeplink: '/daily-learning/12345'
  },
  {
    activityId: 'act_1002',
    type: 'COPIC',
    occurredAt: '2026-02-01T10:40:12+09:00',
    title: 'KOPIC (Business) grading completed.',
    dateLabel: '1st Feb.',
    timeLabel: '10:40 am',
    payload: {
      copicId: 987,
      theme: 'BUSINESS',
      event: 'GRADING_COMPLETED',
      status: 'COMPLETED'
    },
    deeplink: '/copic/987'
  },
  {
    activityId: 'act_1001',
    type: 'SHADOWING',
    occurredAt: '2026-02-01T09:10:05+09:00',
    title: 'Shadowing (Public Place) - Ordering Coffee at a Cafe completed.',
    dateLabel: '1st Feb.',
    timeLabel: '9:10 am',
    payload: {
      shadowingId: 555,
      theme: 'PUBLIC_PLACE',
      contentName: 'Ordering Coffee at a Cafe',
      status: 'COMPLETED'
    },
    deeplink: '/shadowing/555'
  }
]

const normalizeTypes = (types?: ActivityType[] | string): ActivityType[] | null => {
  if (!types) return null
  if (Array.isArray(types)) return types
  return types.split(',').map((type) => type.trim() as ActivityType).filter(Boolean)
}

export const getActivityFeedMock = async (
  params: ActivityFeedRequest = {}
): Promise<ActivityFeedResponse> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      const size = params.size ?? 20
      const startIndex = Number.isFinite(Number(params.cursor)) ? Number(params.cursor) : 0
      const requestedTypes = normalizeTypes(params.types)

      const filtered = requestedTypes?.length
        ? mockActivities.filter((item) => requestedTypes.includes(item.type))
        : [...mockActivities]

      const items = filtered.slice(startIndex, startIndex + size)
      const nextIndex = startIndex + size
      const hasNext = nextIndex < filtered.length

      resolve({
        data: {
          success: true,
          data: {
            items,
            nextCursor: hasNext ? String(nextIndex) : null,
            hasNext
          },
          error: null
        }
      } as ActivityFeedResponse)
    }, 400)
  })
}

const toTypesParam = (types?: ActivityType[] | string): string | undefined => {
  if (!types) return undefined
  if (Array.isArray(types)) return types.join(',')
  return types
}

export const getActivityFeedReal = async (
  params: ActivityFeedRequest = {}
): Promise<ActivityFeedResponse> => {
  const { cursor, size, types } = params
  const response = await axiosInstance.get<ApiResponse<ActivityFeedData>>('/activity-feed', {
    params: {
      cursor,
      size,
      types: toTypesParam(types)
    }
  })
  return response
}

const useMock = apiConfig.shouldMock('ACTIVITY_FEED')

export const getActivityFeed = useMock ? getActivityFeedMock : getActivityFeedReal

console.log(`[ActivityFeedAPI] Initialized. Mode: ${useMock ? 'MOCK' : 'REAL'}`)
