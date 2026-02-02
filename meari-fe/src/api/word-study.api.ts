import axiosInstance from './axiosInstance'
import type { AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import type { ApiResponse } from './auth.api'

export type WordStudyWord = {
  wordId: number
  wordKr: string
  definitionKr: string
  wordVn: string
  definitionVn: string
}

export type GetWordStudyResponse = AxiosResponse<ApiResponse<WordStudyWord[]>>

const WORD_POOL: WordStudyWord[] = [
  {
    wordId: 1,
    wordKr: '예약',
    definitionKr: '미리 약속하여 자리나 시간을 확보하는 것',
    wordVn: 'đặt chỗ',
    definitionVn: 'yêu cầu giữ chỗ hoặc thời gian trước'
  },
  {
    wordId: 2,
    wordKr: '환불',
    definitionKr: '지불한 돈을 다시 돌려받는 것',
    wordVn: 'hoàn tiền',
    definitionVn: 'nhận lại số tiền đã thanh toán'
  },
  {
    wordId: 3,
    wordKr: '영수증',
    definitionKr: '결제 사실을 증명하는 종이',
    wordVn: 'biên lai',
    definitionVn: 'giấy chứng nhận thanh toán'
  },
  {
    wordId: 4,
    wordKr: '분실물',
    definitionKr: '잃어버린 물건',
    wordVn: 'đồ thất lạc',
    definitionVn: 'vật dụng bị mất'
  },
  {
    wordId: 5,
    wordKr: '포장',
    definitionKr: '물건을 싸서 들고 가기 좋게 함',
    wordVn: 'đóng gói',
    definitionVn: 'gói lại để mang đi'
  },
  {
    wordId: 6,
    wordKr: '현금',
    definitionKr: '지폐와 동전으로 된 돈',
    wordVn: 'tiền mặt',
    definitionVn: 'tiền giấy và tiền xu'
  },
  {
    wordId: 7,
    wordKr: '할인',
    definitionKr: '가격을 내려 파는 것',
    wordVn: 'giảm giá',
    definitionVn: 'hạ giá bán'
  },
  {
    wordId: 8,
    wordKr: '교환',
    definitionKr: '산 물건을 다른 것으로 바꾸는 것',
    wordVn: 'đổi hàng',
    definitionVn: 'đổi sang sản phẩm khác'
  },
  {
    wordId: 9,
    wordKr: '영업시간',
    definitionKr: '가게가 문을 여는 시간',
    wordVn: 'giờ mở cửa',
    definitionVn: 'thời gian cửa hàng hoạt động'
  },
  {
    wordId: 10,
    wordKr: '좌석',
    definitionKr: '앉는 자리',
    wordVn: 'chỗ ngồi',
    definitionVn: 'nơi để ngồi'
  },
  {
    wordId: 11,
    wordKr: '결제',
    definitionKr: '돈을 지불하는 것',
    wordVn: 'thanh toán',
    definitionVn: 'trả tiền'
  },
  {
    wordId: 12,
    wordKr: '추천',
    definitionKr: '좋은 것을 골라 권함',
    wordVn: 'gợi ý',
    definitionVn: 'đề xuất điều tốt'
  },
  {
    wordId: 13,
    wordKr: '알레르기',
    definitionKr: '특정 물질에 대한 과민 반응',
    wordVn: 'dị ứng',
    definitionVn: 'phản ứng quá mẫn với một chất'
  },
  {
    wordId: 14,
    wordKr: '반찬',
    definitionKr: '밥과 함께 먹는 음식',
    wordVn: 'món ăn kèm',
    definitionVn: 'món ăn dùng với cơm'
  },
  {
    wordId: 15,
    wordKr: '면세',
    definitionKr: '세금을 내지 않는 것',
    wordVn: 'miễn thuế',
    definitionVn: 'không phải nộp thuế'
  }
]

export const getWordStudyMock = async (): Promise<GetWordStudyResponse> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({
        data: {
          success: true,
          data: WORD_POOL,
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
  const response = await axiosInstance.get<ApiResponse<WordStudyWord[]>>('/contents/words/random')
  return response
}

const useMock =
  import.meta.env.VITE_USE_MOCK_API === 'true' ||
  import.meta.env.VITE_USE_MOCK_WORD_STUDY === 'true'

export const getWordStudy = useMock ? getWordStudyMock : getWordStudyReal
