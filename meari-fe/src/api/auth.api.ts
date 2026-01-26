import axiosInstance from './axiosInstance';

export interface LoginCredentials {
    email: string;
    password: string;
}

export interface LoginResponse {
    success: boolean;
    data: {
        access_token: string;
    } | null;
    error: {
        code: string;
        message: string;
    } | null;
}

// Mock API
const loginMock = async ({ email, password }: LoginCredentials): Promise<LoginResponse> => {
    console.log('[API] Mock Login Requested:', { email, password });
    return new Promise((resolve, reject) => {
        setTimeout(() => {
            if (email === "user@gmail.com" && password === "1234") {
                resolve({
                    success: true,
                    data: {
                        access_token: "mock-jwt-access-token-eyJhbGciOi-mock",
                    },
                    error: null,
                });
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
                        },
                    },
                },
            });
        }, 700);
    });
};

// Real API
const loginReal = async ({ email, password }: LoginCredentials): Promise<LoginResponse> => {
    const response = await axiosInstance.post('/auth/login', {
        email,
        password,
    });
    return response.data;
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
                success: true,
                data: { message: "회원가입 성공" },
                error: null,
            });
        }, 700);
    });
};

const signupReal = async (credentials: SignupCredentials): Promise<SignupResponse> => {
    const response = await axiosInstance.post('/auth/signup', credentials);
    return response.data;
};

const useMock = import.meta.env.VITE_USE_MOCK_API === 'true';

export const login = useMock ? loginMock : loginReal;
export const signup = useMock ? signupMock : signupReal;

console.log(`[AuthAPI] Initialized. Mode: ${useMock ? 'MOCK' : 'REAL'}`);
