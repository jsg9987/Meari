import { create } from 'zustand';
import { login, type LoginCredentials, type UserInfo, getUserInfo } from '../api/auth.api';

interface AuthState {
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
    userInfo: null,
    isAuthenticated: false,
    isLoading: false,
    error: null,

    login: async (credentials) => {
        set({ isLoading: true, error: null });
        try {
            const response = await login(credentials) as any;

            const token = response.data?.access_token ||
                response.data?.data?.access_token ||
                response.access_token ||
                (typeof response.data === 'string' ? response.data : null);

            if (!token) {
                throw new Error('토큰을 찾을 수 없습니다.');
            }

            localStorage.setItem('access_token', token);

            set({
                isAuthenticated: true,
                isLoading: false
            });

            try {
                const userResponse = await getUserInfo() as any;
                const userInfo = userResponse.data?.data || userResponse.data;
                if (userInfo && (userInfo.nickname || userInfo.email)) {
                    set({ userInfo });
                }
            } catch (err) {
                console.warn('[AuthStore] Background user info fetch failed', err);
            }
        } catch (err: any) {
            console.error('[AuthStore] Login failed', err);
            const errorMessage = err.response?.data?.error?.message || err.message || '로그인에 실패했습니다.';
            set({
                isAuthenticated: false,
                error: errorMessage,
                isLoading: false
            });
        }
    },

    logout: () => {
        localStorage.removeItem('access_token');
        localStorage.removeItem('last_active_at');
        set({ userInfo: null, isAuthenticated: false, error: null });
    },

    checkAuth: () => {
        const token = localStorage.getItem('access_token');
        if (token) {
            set({ isAuthenticated: true });
        }
    },

    setUserInfo: (userInfo) => {
        set({ userInfo });
    },

    fetchUserInfo: async () => {
        const token = localStorage.getItem('access_token');
        if (!token) return;

        try {
            const response = await getUserInfo();
            if (response.data.success && response.data.data) {
                set({ userInfo: response.data.data });
            }
        } catch (error: any) {
            console.error('[AuthStore] Failed to fetch user info:', error);
            if (error.response?.status === 401) {
                localStorage.removeItem('access_token');
                localStorage.removeItem('last_active_at');
                set({ userInfo: null, isAuthenticated: false });
            }
        }
    }
}));
