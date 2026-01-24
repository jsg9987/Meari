// =========================================================================================
// [Step 3] Login UI Implementation
// - Auth Store(Zustand)와 연동하여 로그인 처리
// =========================================================================================

import { useState } from 'react';
import { useAuthStore } from '../store/auth.store';

const Login = () => {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');

    // Store에서 상태와 액션 가져오기
    const { login, isLoading, error } = useAuthStore();

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        await login({ email, password });
    };

    return (
        <div>
            <h2>로그인</h2>
            <form onSubmit={handleSubmit}>
                <div>
                    <label htmlFor="email">이메일: </label>
                    <input
                        type="email"
                        id="email"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        placeholder="이메일을 입력해주세요."
                    />
                </div>
                <div>
                    <label htmlFor="password">비밀번호: </label>
                    <input
                        type="password"
                        id="password"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        placeholder="비밀번호를 입력해주세요."
                    />
                </div>

                {/* 에러 메시지 표시 */}
                {error && (
                    <div style={{ color: 'red' }}>
                        {error}
                    </div>
                )}

                <button type="submit" disabled={isLoading}>
                    {isLoading ? '로그인 중...' : '로그인'}
                </button>
            </form>
            <div>
                <p>테스트 계정: user@gmail.com / 1234</p>
            </div>
        </div>
    );
};

export default Login;
/**
 * =========================================================================================
 * End of Login UI Implementation
 * =========================================================================================
 */
