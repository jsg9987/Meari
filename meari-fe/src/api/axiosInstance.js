import axios from 'axios';

// baseUrl, timeout, withCredentials 같은 반복되는 코드의 
// 재사용을 줄이는 인스턴스입니다.
const axiosInstance = axios.create({
  baseURL: import.meta.env.VITE_BASE_SERVER_URL,
  timeout: 10000,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 요청을 보내기 전에 탈취해서 accessToken을 추가하는 함수
axiosInstance.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// 요청을 받기 전에 탈취해서 처리하는 로직
// ex) accessToken이 만료되어 재요청을 보내는 로직 or 로그아웃
axiosInstance.interceptors.response.use(
  (response) => response.data,
  (error) => {
    if (error.response?.status === 401) {
      // 로그아웃 or 토큰 재발급 처리
    }
    return Promise.reject(error);
  }
);

export default axiosInstance;
