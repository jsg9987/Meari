import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store/auth.store';

// Assets
import logoDark from '../assets/images/common/logo-dark.svg';
import loginIllustration from '../assets/images/auth/login-illustration.svg';

const Login = () => {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');

    const navigate = useNavigate();

    // Store에서 상태와 액션 가져오기
    const { login, isLoading, error } = useAuthStore();

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        await login({ email, password });
    };

    const labelClass = 'text-sm font-medium text-[#1a1a1a]';

    const inputClass =
        'h-12 w-full rounded-lg border border-[#e5e5e5] bg-white ' +
        'px-4 text-sm text-[#1a1a1a] ' +
        'placeholder:text-[#bebebe] ' +
        'focus:outline-none focus:ring-2 focus:ring-[#1a1a1a]/20';

    const buttonClass =
        'mt-4 h-12 w-full rounded-lg ' +
        'bg-[#1a1a1a] text-white font-semibold ' +
        'transition hover:bg-[#333] ' +
        'disabled:bg-[#ccc] disabled:cursor-not-allowed disabled:hover:bg-[#ccc]';

    return (
        <div className="min-h-screen bg-white flex">
            <div className="mx-auto w-full max-w-[1280px] px-6 lg:px-10 flex flex-col">

                {/* Logo (Top) */}
                <div className="pt-10">
                    <img
                        src={logoDark}
                        alt="MEARI Logo"
                        className="w-24 sm:w-28 h-auto"
                    />
                </div>

                {/* Center Area */}
                <div className="flex-1 flex flex-col lg:flex-row items-center justify-center gap-16">

                    {/* 로그인 카드 */}
                    <div
                        className={
                            'w-full max-w-[400px] rounded-xl border border-[#bebebe] bg-white p-6 sm:p-10 ' +
                            'shadow-[0_1px_2px_rgba(0,0,0,0.25)]'
                        }
                    >
                        <form onSubmit={handleSubmit} className="space-y-5">

                            {/* 헤더 */}
                            <div className="mb-6 space-y-2">
                                <h2 className="text-2xl font-bold font-pretendard text-[#1a1a1a]">
                                    로그인
                                </h2>
                                <p className="text-xs font-pretendard text-[#666]">
                                    로그인을 수행하고, 나만의 학습 리포트를 확인하세요.
                                </p>
                            </div>

                            {/* 이메일 */}
                            <div className="space-y-2">
                                <label htmlFor="email" className={labelClass}>
                                    이메일
                                </label>
                                <input
                                    id="email"
                                    type="email"
                                    className={inputClass}
                                    placeholder="이메일을 입력 해주세요."
                                    value={email}
                                    onChange={(e) => setEmail(e.target.value)}
                                    autoComplete="email"
                                />
                            </div>

                            {/* 비밀번호 */}
                            <div className="space-y-2">
                                <label htmlFor="password" className={labelClass}>
                                    비밀번호
                                </label>
                                <input
                                    id="password"
                                    type="password"
                                    className={inputClass}
                                    placeholder="비밀번호를 입력 해주세요."
                                    value={password}
                                    onChange={(e) => setPassword(e.target.value)}
                                    autoComplete="current-password"
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
