import axiosInstance from './axiosInstance';
import { apiConfig } from './apiConfig';

export interface LoginCredentials {
    email: string;
    password: string;
}

export interface LoginResponse {
    access_token: string;
}

// Mock API
const loginMock = async ({ email, password }: LoginCredentials) => {
    console.log('[API] Mock Login Requested:', { email, password });
    return new Promise((resolve, reject) => {
        setTimeout(() => {
            if (email === "user@gmail.com" && password === "1234") {
                resolve({
                    data: {
                        access_token: "mock-jwt-access-token-eyJhbGciOi-mock",
                    }
                });
                return;
            }

            reject({
                response: {
                    status: 401,
                    data: {
                        message: "이메일 또는 비밀번호가 올바르지 않습니다.",
                    },
                },
            });
        }, 700);
    });
};

// Real API
const loginReal = async ({ email, password }: LoginCredentials) => {
    const response = await axiosInstance.post<LoginResponse>('/auth/login', {
        email,
        password,
    });
    return response;
};

// --- Signup ---
export interface SignupCredentials {
    email: string;
    password: string;
    nickname: string;
    sex: string; // 'M' | 'F'
    native_language: string; // 'TOPIC_KR' ... (DB Schema: VARCHAR(10))
}

export interface SignupResponse {
    success: boolean;
    data: any | null;
    error: {
        code: string;
        message: string;
    } | null;
}

const signupMock = async (credentials: SignupCredentials) => {
    console.log('[API] Mock Signup Requested:', credentials);
    return new Promise((resolve, reject) => {
        setTimeout(() => {
            // 중복 이메일 시뮬레이션
            if (credentials.email === "user@gmail.com") {
                reject({
                    response: {
                        status: 409,
                        data: {
                            success: false,
                            data: null,
                            error: {
                                code: "AUTH_EMAIL_DUPLICATED",
                                message: "이미 사용 중인 이메일입니다.",
                            },
                        },
                    },
                });
                return;
            }

            // 성공
            resolve({
                data: {
                    success: true,
                    data: { message: "회원가입 성공" },
                    error: null,
                }
            });
        }, 700);
    });
};

const signupReal = async (credentials: SignupCredentials) => {
    const response = await axiosInstance.post<SignupResponse>('/auth/signup', credentials);
    return response;
};

// --- Get User Info ---
export interface UserInfo {
    email: string;
    profile_url: string;
    nickname: string;
}

export interface UserInfoResponse {
    success: boolean;
    data: UserInfo | null;
    error: {
        code: string;
        message: string;
    } | null;
}

const getUserInfoMock = async () => {
    console.log('[API] Mock Get User Info Requested');
    return new Promise((resolve) => {
        setTimeout(() => {
            resolve({
                data: {
                    success: true,
                    data: {
                        email: "user@gmail.com",
                        profile_url: "http://",
                        nickname: "김싸피"
                    },
                    error: null
                }
            });
        }, 300);
    });
};

const getUserInfoReal = async () => {
    const response = await axiosInstance.get<UserInfoResponse>('/members/me');
    return response;
};

const useMock = apiConfig.shouldMock('AUTH');

export const login = useMock ? loginMock : loginReal;
export const signup = useMock ? signupMock : signupReal;
export const getUserInfo = useMock ? getUserInfoMock : getUserInfoReal;

console.log(`[AuthAPI] Initialized. Mode: ${useMock ? 'MOCK' : 'REAL'}`);
