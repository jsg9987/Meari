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
    wordKr: 'Order',
    definitionKr: 'A request to buy something',
    wordVn: 'dat hang',
    definitionVn: 'yeu cau mua hang'
  },
  {
    wordId: 2,
    wordKr: 'Warm',
    definitionKr: 'Having a moderately high temperature',
    wordVn: 'am',
    definitionVn: 'co nhiet do cao vua phai'
  },
  {
    wordId: 3,
    wordKr: 'Receipt',
    definitionKr: 'A paper that proves payment',
    wordVn: 'hoa don',
    definitionVn: 'giay chung nhan thanh toan'
  },
  {
    wordId: 4,
    wordKr: 'Takeout',
    definitionKr: 'Food packaged to go',
    wordVn: 'mang di',
    definitionVn: 'dong goi mang di'
  },
  {
    wordId: 5,
    wordKr: 'Refund',
    definitionKr: 'Getting paid money back',
    wordVn: 'hoan tien',
    definitionVn: 'tra lai tien da thanh toan'
  },
  {
    wordId: 6,
    wordKr: 'Add',
    definitionKr: 'To include one more',
    wordVn: 'them',
    definitionVn: 'them vao'
  },
  {
    wordId: 7,
    wordKr: 'Recommend',
    definitionKr: 'Suggest something good',
    wordVn: 'goi y',
    definitionVn: 'de xuat dieu tot'
  },
  {
    wordId: 8,
    wordKr: 'Discount',
    definitionKr: 'A price reduction',
    wordVn: 'giam gia',
    definitionVn: 'ha gia'
  },
  {
    wordId: 9,
    wordKr: 'Exchange',
    definitionKr: 'Swap for something else',
    wordVn: 'doi',
    definitionVn: 'doi sang cai khac'
  },
  {
    wordId: 10,
    wordKr: 'Seat',
    definitionKr: 'A place to sit',
    wordVn: 'cho ngoi',
    definitionVn: 'noi ngoi'
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
