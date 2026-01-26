import axiosInstance from './axiosInstance';

export interface Theme {
  theme_id: number;
  name: string;
  description: string;
  theme_url: string;
}

export interface GetThemesResponse {
  success: boolean;
  data: Theme[];
  error: {
    code: string;
    message: string;
  } | null;
}

export const getThemesMock = async (): Promise<GetThemesResponse> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({
        success: true,
        data: [
          {
            theme_id: 1,
            name: '식당/카페',
            description: '식당이나 카페에서 사용할 수 있는 일상 대화 연습',
            theme_url: 'https://images.unsplash.com/photo-1554118811-1e0d58224f24?w=400',
          },
          {
            theme_id: 2,
            name: '비즈니스 미팅',
            description: '회사에서 사용하는 업무 관련 대화 연습',
            theme_url: 'https://images.unsplash.com/photo-1542744173-8e7e53415bb0?w=400',
          },
          {
            theme_id: 3,
            name: '여행',
            description: '여행지에서 사용할 수 있는 회화 연습',
            theme_url: 'https://images.unsplash.com/photo-1488646953014-85cb44e25828?w=400',
          },
          {
            theme_id: 4,
            name: '쇼핑',
            description: '쇼핑할 때 필요한 표현 연습',
            theme_url: 'https://images.unsplash.com/photo-1441986300917-64674bd600d8?w=400',
          },
        ],
        error: null,
      });
    }, 400);
  });
};

export const getThemes = async (): Promise<GetThemesResponse> => {
  const response = await axiosInstance.get('/contents/themes');
  const responseData = (response as { data?: GetThemesResponse }).data ?? response;
  return responseData as GetThemesResponse;
};
