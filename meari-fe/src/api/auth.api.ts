import axiosInstance from './axiosInstance';
import { apiConfig } from './apiConfig';
import type { AxiosResponse } from 'axios';

// 공통 API 응답 타입
export interface ApiResponse<T> {
    success: boolean;
    data: T | null;
    error: {
        code: string;
        message: string;
    } | null;
}

export interface LoginCredentials {
    email: string;
    password: string;
}

export interface LoginData {
    access_token: string;
}

export type LoginResponse = AxiosResponse<ApiResponse<LoginData>>;

// Mock API
const loginMock = async ({ email, password }: LoginCredentials): Promise<LoginResponse> => {
    console.log('[API] Mock Login Requested:', { email, password });
    return new Promise((resolve, reject) => {
        setTimeout(() => {
            if (email === "user@gmail.com" && password === "1234") {
                resolve({
                    data: {
                        success: true,
                        data: {
                            access_token: "mock-jwt-access-token-eyJhbGciOi-mock",
                        },
                        error: null,
                    }
                } as LoginResponse);
                return;
            }

            reject({
                response: {
                    status: 401,
                    data: {
                        success: false,
                        data: null,
                        error: {
                            code: "AUTH_INVALID_CREDENTIALS",
                            message: "이메일 또는 비밀번호가 올바르지 않습니다.",
                        }
                    },
                },
            });
        }, 700);
    });
};

// Real API
const loginReal = async ({ email, password }: LoginCredentials): Promise<LoginResponse> => {
    const response = await axiosInstance.post<ApiResponse<LoginData>>('/auth/login', {
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

export interface SignupData {
    message: string;
}

export type SignupResponse = AxiosResponse<ApiResponse<SignupData>>;

const signupMock = async (credentials: SignupCredentials): Promise<SignupResponse> => {
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
            } as SignupResponse);
        }, 700);
    });
};

const signupReal = async (credentials: SignupCredentials): Promise<SignupResponse> => {
    const response = await axiosInstance.post<ApiResponse<SignupData>>('/auth/signup', credentials);
    return response;
};

// --- Get User Info ---
export interface UserInfo {
    memberId: number;
    email: string;
    profile_url: string;
    nickname: string;
}

export type UserInfoResponse = AxiosResponse<ApiResponse<UserInfo>>;

const getUserInfoMock = async (): Promise<UserInfoResponse> => {
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
            } as UserInfoResponse);
        }, 300);
    });
};

const getUserInfoReal = async (): Promise<UserInfoResponse> => {
    const response = await axiosInstance.get<ApiResponse<UserInfo>>('/members/me');
    return response;
};

const useMock = apiConfig.shouldMock('AUTH');
    
export const login = useMock ? loginMock : loginReal;
export const signup = useMock ? signupMock : signupReal;
export const getUserInfo = useMock ? getUserInfoMock : getUserInfoReal;

console.log(`[AuthAPI] Initialized. Mode: ${useMock ? 'MOCK' : 'REAL'}`);
