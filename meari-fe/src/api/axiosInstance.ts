import axios from 'axios';
import type { InternalAxiosRequestConfig, AxiosError, AxiosResponse } from 'axios';

const axiosInstance = axios.create({
  baseURL: import.meta.env.VITE_BASE_SERVER_URL,
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' },
});

// 요청 인터셉터: 모든 요청에 토큰 추가 (로그인/회원가입 제외)
axiosInstance.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const isAuthFree =
      config.url?.includes('/auth/login') ||
      config.url?.includes('/auth/signup') ||
      config.url?.includes('/auth/email/check') ||
      config.url?.includes('/auth/nickname/check');

    if (isAuthFree) return config;

    const token = localStorage.getItem('access_token');
    if (token) config.headers.Authorization = `Bearer ${token}`;
    return config;
  },
  (error: AxiosError) => Promise.reject(error)
);

// 응답 인터셉터: 401 에러(세션 만료) 처리
axiosInstance.interceptors.response.use(
  (response: AxiosResponse) => response,
  (error: AxiosError) => {
    if (error.response?.status === 401) {
      const requestUrl = error.config?.url || '';
      // /members/me 요청은 로그인 직후 발생할 수 있으므로 예외 처리
      if (!requestUrl.includes('/members/me')) {
        localStorage.removeItem('access_token');
        const currentPath = window.location.pathname;
        if (currentPath !== '/login' && currentPath !== '/signup') {
          window.location.href = '/login';
        }
      }
    }
    return Promise.reject(error);
  }
);

export default axiosInstance;
