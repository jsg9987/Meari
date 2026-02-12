import axiosInstance from './axiosInstance'
import type { AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import type { ApiResponse } from './auth.api'

export type QuizWord = {
  text: string
  index: number
}

export type QuizSentence = {
  sentence_id: number
  text_vn: string
  words: QuizWord[]
}

export type GetQuizResponse = AxiosResponse<ApiResponse<QuizSentence[]>>

const MOCK_QUIZ: QuizSentence[] = [
  {
    sentence_id: 501,
    text_vn: 'Xin chào, tôi có thể giúp gì cho bạn?',
    words: [
      { text: '도와드릴까요', index: 2 },
      { text: '어서오세요', index: 0 },
      { text: '주문', index: 1 }
    ]
  },
  {
    sentence_id: 502,
    text_vn: 'Cho tôi một ly americano nóng',
    words: [
      { text: '주세요', index: 4 },
      { text: '한', index: 2 },
      { text: '따뜻한', index: 0 },
      { text: '잔', index: 3 },
      { text: '아메리카노', index: 1 }
    ]
  },
  {
    sentence_id: 503,
    text_vn: 'Tôi muốn thêm một miếng bánh phô mai',
    words: [
      { text: '추가할게요', index: 4 },
      { text: '치즈케이크', index: 1 },
      { text: '한', index: 2 },
      { text: '조각', index: 3 },
      { text: '더', index: 0 }
    ]
  },
  {
    sentence_id: 504,
    text_vn: 'Tôi muốn mang đi',
    words: [
      { text: '테이크아웃', index: 0 },
      { text: '으로', index: 1 },
      { text: '부탁드립니다', index: 2 }
    ]
  },
  {
    sentence_id: 505,
    text_vn: 'Làm ơn cho tôi biên lai',
    words: [
      { text: '영수증', index: 0 },
      { text: '도', index: 1 },
      { text: '같이', index: 2 },
      { text: '주세요', index: 3 }
    ]
  }
]

export const getQuizMock = async (count = 5): Promise<GetQuizResponse> => {
  const limited = MOCK_QUIZ.slice(0, count)
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({
        data: {
          success: true,
          data: limited,
          error: null
        },
        status: 200,
        statusText: 'OK',
        headers: {},
        config: {} as InternalAxiosRequestConfig
      } as GetQuizResponse)
    }, 400)
  })
}

const getQuizReal = async (count = 5): Promise<GetQuizResponse> => {
  const response = await axiosInstance.get<ApiResponse<QuizSentence[]>>('/contents/quiz', {
    params: { count }
  })
  return response
}

const useMock =
  import.meta.env.VITE_USE_MOCK_API === 'true' || import.meta.env.VITE_USE_MOCK_QUIZ === 'true'

export const getQuiz = useMock ? getQuizMock : getQuizReal
