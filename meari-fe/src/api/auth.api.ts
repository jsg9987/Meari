import axiosInstance from './axiosInstance';
import { apiConfig } from './apiConfig';
import type { AxiosResponse } from 'axios';

// ?? API ?? ??
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
            if (email == 'user@gmail.com' && password == '1234') {
                resolve({
                    data: {
                        success: true,
                        data: {
                            access_token: 'mock-jwt-access-token-eyJhbGciOi-mock'
                        },
                        error: null
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
                            code: 'AUTH_INVALID_CREDENTIALS',
                            message: '??? ?? ????? ???? ????.'
                        }
                    }
                }
            });
        }, 700);
    });
};

// Real API
const loginReal = async ({ email, password }: LoginCredentials): Promise<LoginResponse> => {
    const response = await axiosInstance.post<ApiResponse<LoginData>>('/auth/login', {
        email,
        password
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

// --- Email/Nickname Check ---
export interface EmailCheckResponse {
    success: boolean;
    data: { has_email: boolean } | null;
    error: { code: string; message: string } | null;
}

export interface NicknameCheckResponse {
    success: boolean;
    data: { has_nickname: boolean } | null;
    error: { code: string; message: string } | null;
}

const checkEmailReal = async (email: string) => {
    const response = await axiosInstance.get<EmailCheckResponse>('/auth/email/check', {
        params: { email }
    });
    return response;
};

const checkNicknameReal = async (nickname: string) => {
    const response = await axiosInstance.get<NicknameCheckResponse>('/auth/nickname/check', {
        params: { nickname }
    });
    return response;
};

const signupMock = async (credentials: SignupCredentials): Promise<SignupResponse> => {
    console.log('[API] Mock Signup Requested:', credentials);
    return new Promise((resolve, reject) => {
        setTimeout(() => {
            // ??? ??
            if (credentials.email === 'user@gmail.com') {
                reject({
                    response: {
                        status: 409,
                        data: {
                            success: false,
                            data: null,
                            error: {
                                code: 'AUTH_EMAIL_DUPLICATED',
                                message: '?? ?? ?? ??????.'
                            }
                        }
                    }
                });
                return;
            }

            // ??
            resolve({
                data: {
                    success: true,
                    data: { message: '???? ??' },
                    error: null
                }
            } as SignupResponse);
        }, 700);
    });
};

const signupReal = async (credentials: SignupCredentials): Promise<SignupResponse> => {
    const response = await axiosInstance.post<ApiResponse<SignupData>>('/auth/signup', credentials);
    return response;
};

const checkEmailMock = async (email: string) => {
    return new Promise((resolve) => {
        setTimeout(() => {
            resolve({
                data: {
                    success: true,
                    data: { has_email: email === 'user@gmail.com' },
                    error: null
                }
            });
        }, 300);
    });
};

const checkNicknameMock = async (nickname: string) => {
    return new Promise((resolve) => {
        setTimeout(() => {
            resolve({
                data: {
                    success: true,
                    data: { has_nickname: nickname === '???' },
                    error: null
                }
            });
        }, 300);
    });
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
                        email: 'user@gmail.com',
                        profile_url: 'http://',
                        nickname: '???'
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
export const checkEmail = useMock ? checkEmailMock : checkEmailReal;
export const checkNickname = useMock ? checkNicknameMock : checkNicknameReal;

console.log(`[AuthAPI] Initialized. Mode: ${useMock ? 'MOCK' : 'REAL'}`);
