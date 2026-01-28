import { create } from 'zustand';
import { login, type LoginCredentials, type UserInfo, getUserInfo } from '../api/auth.api';

interface AuthState {
    user: { email: string } | null;
    userInfo: UserInfo | null;
    isAuthenticated: boolean;
    isLoading: boolean;
    error: string | null;
    login: (credentials: LoginCredentials) => Promise<void>;
    logout: () => void;
    checkAuth: () => void;
    setUserInfo: (userInfo: UserInfo | null) => void;
    fetchUserInfo: () => Promise<void>;
}

export const useAuthStore = create<AuthState>((set) => ({
    user: null,
    userInfo: null,
    isAuthenticated: false,
    isLoading: false,
    error: null,

    login: async (credentials) => {
        set({ isLoading: true, error: null });
        try {
            const response = await login(credentials);
            console.log(response);

            const token = response.access_token;
            localStorage.setItem('accessToken', token);

            set({
                isAuthenticated: true,
                user: { email: credentials.email },
                isLoading: false
            });
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        } catch (err: any) {
            console.error('[AuthStore] Login failed', err);
            const errorMessage = err.response?.data?.message || '로그인에 실패했습니다.';
            set({
                isAuthenticated: false,
                user: null,
                error: errorMessage,
                isLoading: false
            });
        }
    },

    logout: () => {
        localStorage.removeItem('accessToken');
        set({ user: null, userInfo: null, isAuthenticated: false, error: null });
    },

    checkAuth: () => {
        const token = localStorage.getItem('accessToken');
        if (token) {
            // TODO: 토큰 유효성 검증 API 호출 필요. 현재는 존재 여부로만 판단.
            set({ isAuthenticated: true, user: { email: 'user@gmail.com' } }); // 임시 사용자 정보 복원
        }
    },

    setUserInfo: (userInfo) => {
        set({ userInfo });
    },

    fetchUserInfo: async () => {
        try {
            const response = await getUserInfo();
            if (response.success && response.data) {
                set({ userInfo: response.data });
            }
        } catch (error) {
            console.error('[AuthStore] Failed to fetch user info:', error);
        }
    }
}));
