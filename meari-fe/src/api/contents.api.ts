import axiosInstance from './axiosInstance';
import type { AxiosResponse } from 'axios';
import type { ApiResponse } from './auth.api';

export interface Theme {
  theme_id: number;
  name: string;
  description: string;
  theme_url: string;
}

export interface Content {
  content_id: number;
  theme_id: number;
  title: string;
  description: string;
  video_url: string;
  thumbnail_url: string;
  duration: number;
}

export type GetThemesResponse = AxiosResponse<ApiResponse<Theme[]>>;

export type GetThemeContentsResponse = AxiosResponse<ApiResponse<Content[]>>;

export const getThemesMock = async (): Promise<GetThemesResponse> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({
        success: true,
        data: [
          {
            theme_id: 1,
            name: '일상회화',
            description: '일상에서 자주 사용하는 회화 표현을 중심으로 자연스럽게 말하는 연습',
            theme_url: 'https://images.unsplash.com/photo-1554118811-1e0d58224f24?w=400',
          },
          {
            theme_id: 2,
            name: '비즈니스',
            description: '회사에서 사용하는 업무 관련 대화를 상황별로 익히는 실전 연습',
            theme_url: 'https://images.unsplash.com/photo-1542744173-8e7e53415bb0?w=400',
          },
          {
            theme_id: 3,
            name: '뉴스',
            description: '시사 뉴스 리포트를 따라하며 발음과 억양을 함께 다듬는 연습',
            theme_url: 'https://images.unsplash.com/photo-1488646953014-85cb44e25828?w=400',
          },
          {
            theme_id: 4,
            name: '여행',
            description: '가이드와 관광객의 대화를 통해 다양한 여행 상황 회화를 연습',
            theme_url: 'https://images.unsplash.com/photo-1441986300917-64674bd600d8?w=400',
          },
        ],
        error: null,
      });
    }, 400);
  });
};

export const getThemes = async (): Promise<GetThemesResponse> => {
  const useMock = import.meta.env.VITE_USE_MOCK_CONTENTS === 'true';
  if (useMock) {
    return await getThemesMock();
  }
  const response = await axiosInstance.get<ApiResponse<Theme[]>>('/contents/themes');
  return response;
};

export const getThemeContentsMock = async (themeId: number): Promise<GetThemeContentsResponse> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      const mockContents: Record<number, Content[]> = {
        1: [
          {
            content_id: 1,
            theme_id: 1,
            title: '그래서 쪼끔은 후회해?',
            description: '한국어 일상 대화 쉐도잉 연습',
            video_url: '/src/assets/video/[ Kor & Eng Sub ] [ Korean Shadowing ] 그래서 쪼끔은 후회해？ - So do you regret it？ Even a little？ [7kpGMG0H4pM].f137.mp4',
            thumbnail_url: 'https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=400',
            duration: 167,
          },
          {
            content_id: 2,
            theme_id: 1,
            title: '식당에서 예약하기',
            description: '전화로 식당 예약하는 대화 연습',
            video_url: 'https://example.com/video2.mp4',
            thumbnail_url: 'https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=400',
            duration: 240,
          },
          {
            content_id: 3,
            theme_id: 1,
            title: '메뉴 추천 받기',
            description: '식당에서 직원에게 메뉴 추천을 받는 상황',
            video_url: 'https://example.com/video3.mp4',
            thumbnail_url: 'https://images.unsplash.com/photo-1559339352-11d035aa65de?w=400',
            duration: 200,
          },
        ],
        2: [
          {
            content_id: 4,
            theme_id: 2,
            title: '회의 시작하기',
            description: '비즈니스 미팅을 시작하는 인사말과 소개',
            video_url: 'https://example.com/video4.mp4',
            thumbnail_url: 'https://images.unsplash.com/photo-1556761175-5973dc0f32e7?w=400',
            duration: 300,
          },
          {
            content_id: 5,
            theme_id: 2,
            title: '프레젠테이션하기',
            description: '제품이나 서비스를 소개하는 프레젠테이션',
            video_url: 'https://example.com/video5.mp4',
            thumbnail_url: 'https://images.unsplash.com/photo-1560439514-4e9645039924?w=400',
            duration: 360,
          },
        ],
        3: [
          {
            content_id: 6,
            theme_id: 3,
            title: '호텔 체크인',
            description: '호텔에서 체크인하는 대화',
            video_url: 'https://example.com/video6.mp4',
            thumbnail_url: 'https://images.unsplash.com/photo-1566073771259-6a8506099945?w=400',
            duration: 220,
          },
          {
            content_id: 7,
            theme_id: 3,
            title: '길 물어보기',
            description: '여행지에서 길을 물어보는 상황',
            video_url: 'https://example.com/video7.mp4',
            thumbnail_url: 'https://images.unsplash.com/photo-1488646953014-85cb44e25828?w=400',
            duration: 150,
          },
        ],
        4: [
          {
            content_id: 8,
            theme_id: 4,
            title: '옷 구매하기',
            description: '옷가게에서 사이즈와 색상 확인하기',
            video_url: 'https://example.com/video8.mp4',
            thumbnail_url: 'https://images.unsplash.com/photo-1441984904996-e0b6ba687e04?w=400',
            duration: 190,
          },
          {
            content_id: 9,
            theme_id: 4,
            title: '환불 요청하기',
            description: '구매한 제품을 환불하는 대화',
            video_url: 'https://example.com/video9.mp4',
            thumbnail_url: 'https://images.unsplash.com/photo-1472851294608-062f824d29cc?w=400',
            duration: 210,
          },
        ],
      };

      resolve({
        data: {
          success: true,
          data: mockContents[themeId] || [],
          error: null,
        }
      } as GetThemeContentsResponse);
    }, 600);
  });
};

export const getThemeContents = async (themeId: number): Promise<GetThemeContentsResponse> => {
  const useMock = import.meta.env.VITE_USE_MOCK_CONTENTS === 'true';
  if (useMock) {
    return await getThemeContentsMock(themeId);
  }
  const response = await axiosInstance.get<ApiResponse<Content[]>>(`/themes/${themeId}/contents`);
  return response;
};

// --- Select Room Content (방 컨텐츠 선택) ---
export interface SelectRoomContentData {
  message: string;
}

export type SelectRoomContentResponse = AxiosResponse<ApiResponse<SelectRoomContentData>>;

// TODO: 실제 구현시 선택된 content의 content_id 사용
export const selectRoomContent = async (
  roomId: number
): Promise<SelectRoomContentResponse> => {
  const response = await axiosInstance.post<ApiResponse<SelectRoomContentData>>(
    `/rooms/${roomId}/content`,
    { content_id: 1 }
  );
  return response;
};
