import axiosInstance from './axiosInstance'
import type { AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import type { ApiResponse } from './auth.api'

export type WordStudySentence = {
  sentence_id: number
  sequence: number
  speaker_role: string
  text_ko: string
  text_vi: string
}

export type GetWordStudyResponse = AxiosResponse<ApiResponse<WordStudySentence[]>>

const SENTENCE_POOL: WordStudySentence[] = [
  {
    sentence_id: 1,
    sequence: 1,
    speaker_role: 'USER',
    text_ko: '예약',
    text_vi: 'Đặt chỗ hoặc thời gian trước.'
  },
  {
    sentence_id: 2,
    sequence: 2,
    speaker_role: 'USER',
    text_ko: '환불',
    text_vi: 'Nhận lại tiền đã thanh toán.'
  },
  {
    sentence_id: 3,
    sequence: 3,
    speaker_role: 'USER',
    text_ko: '영수증',
    text_vi: 'Giấy tờ chứng minh việc thanh toán.'
  },
  {
    sentence_id: 4,
    sequence: 4,
    speaker_role: 'USER',
    text_ko: '분실물',
    text_vi: 'Đồ vật bị mất.'
  },
  {
    sentence_id: 5,
    sequence: 5,
    speaker_role: 'USER',
    text_ko: '포장',
    text_vi: 'Đóng gói món ăn để mang đi.'
  },
  {
    sentence_id: 6,
    sequence: 6,
    speaker_role: 'USER',
    text_ko: '현금',
    text_vi: 'Tiền mặt, không phải thẻ.'
  },
  {
    sentence_id: 7,
    sequence: 7,
    speaker_role: 'USER',
    text_ko: '할인',
    text_vi: 'Giảm giá.'
  },
  {
    sentence_id: 8,
    sequence: 8,
    speaker_role: 'USER',
    text_ko: '교환',
    text_vi: 'Đổi sản phẩm đã mua sang sản phẩm khác.'
  },
  {
    sentence_id: 9,
    sequence: 9,
    speaker_role: 'USER',
    text_ko: '영업시간',
    text_vi: 'Thời gian cửa hàng mở cửa.'
  },
  {
    sentence_id: 10,
    sequence: 10,
    speaker_role: 'USER',
    text_ko: '좌석',
    text_vi: 'Chỗ ngồi.'
  },
  {
    sentence_id: 11,
    sequence: 11,
    speaker_role: 'USER',
    text_ko: '결제',
    text_vi: 'Thanh toán.'
  },
  {
    sentence_id: 12,
    sequence: 12,
    speaker_role: 'USER',
    text_ko: '추천',
    text_vi: 'Giới thiệu điều tốt/đáng dùng.'
  },
  {
    sentence_id: 13,
    sequence: 13,
    speaker_role: 'USER',
    text_ko: '알레르기',
    text_vi: 'Phản ứng dị ứng với một chất.'
  },
  {
    sentence_id: 14,
    sequence: 14,
    speaker_role: 'USER',
    text_ko: '반찬',
    text_vi: 'Món ăn kèm với cơm.'
  },
  {
    sentence_id: 15,
    sequence: 15,
    speaker_role: 'USER',
    text_ko: '면세',
    text_vi: 'Miễn thuế.'
  }
]

export const getWordStudyMock = async (): Promise<GetWordStudyResponse> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({
        data: {
          success: true,
          data: SENTENCE_POOL,
          error: null
        },
        status: 200,
        statusText: 'OK',
        headers: {},
        config: {} as InternalAxiosRequestConfig
      } as GetWordStudyResponse)
    }, 400)
  })
}

const getWordStudyReal = async (): Promise<GetWordStudyResponse> => {
  const response = await axiosInstance.post<ApiResponse<WordStudySentence[]>>(
    '/contents/1/words'
  )
  return response
}

const useMock =
  import.meta.env.VITE_USE_MOCK_API === 'true' ||
  import.meta.env.VITE_USE_MOCK_WORD_STUDY === 'true'

export const getWordStudy = useMock ? getWordStudyMock : getWordStudyReal
