import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store/auth.store';

// Assets
import logoDark from '../assets/images/common/logo-dark-2.svg';
import loginIllustration from '../assets/images/auth/login-illustration.svg';

const Login = () => {
    const navigate = useNavigate();
    const { login, isLoading, error } = useAuthStore();
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        await login({ email, password });
        // 로그인 에러가 없으면 메인으로 이동
        if (!useAuthStore.getState().error) {
            navigate('/main');
        }
    };

    const labelClass = 'text-sm font-medium text-[#001C27]';

    const inputClass =
        'h-12 w-full rounded-lg border border-[#e5e5e5] bg-white ' +
        'px-4 text-sm text-[#001C27] ' +
        'placeholder:text-[#bebebe] ' +
        'focus:outline-none focus:ring-2 focus:ring-[#001C27]/20';

    const buttonClass =
        'mt-4 h-12 w-full rounded-lg ' +
        'bg-[#001C27] text-white font-semibold ' +
        'transition hover:bg-[#002D3F] ' +
        'disabled:bg-[#ccc] disabled:cursor-not-allowed disabled:hover:bg-[#ccc]';

    return (
        <div className="min-h-screen bg-white flex">
            <div className="mx-auto w-full max-w-[1280px] px-6 lg:px-10 min-h-screen flex flex-col">

                {/* Header Logo */}
                <div className="w-[200px] h-[80px] p-[10px] flex items-center justify-center">
                    <img src={logoDark} alt="MEARI Logo" className="w-full h-full object-contain" />
                </div>

                {/* Content Area */}
                <div className="flex-1 flex flex-col lg:flex-row items-center justify-center pl-20">

                    {/* Login Card */}
                    <div
                        className={
                            'w-full max-w-[400px] rounded-xl border border-[#bebebe] bg-white p-8 sm:p-10' +
                            'shadow-[0_1px_2px_rgba(0,0,0,0.25)]' +
                            'transition-opacity duration-200'
                        }
                    >
                        <form onSubmit={handleSubmit} className="space-y-5">
                            <div className="mb-6 space-y-2">
                                <h2 className="text-2xl font-bold font-pretendard text-[#001C27]">로그인</h2>
                                <p className="text-xs font-pretendard text-[#666]">로그인을 수행하고, 나만의 학습 리포트를 확인하세요.</p>
                            </div>

                            {/* Email */}

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
                            <div className="space-y-2 mb-12">
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

                            {/* 로그인 버튼 */}
                            <button
                                type="submit"
                                disabled={isLoading}
                                className={buttonClass}
                            >
                                {isLoading ? '로그인 중...' : '로그인'}
                            </button>

                            {/* 가입 링크 */}
                            <p className="mt-4 text-center text-sm text-[#666]">
                                아직 계정이 없으신가요?
                                <button
                                    type="button"
                                    className="ml-1 font-bold text-[#001C27] hover:underline"
                                    onClick={() => navigate('/signup')}
                                >
                                    가입하기
                                </button>
                            </p>

                            {/* 에러 메시지 */}
                            {error && (
                                <div className="text-red-500 text-sm">
                                    {error}
                                </div>
                            )}
                        </form>
                    </div>

                    {/* Illustration */}
                    <div className="hidden lg:flex items-center justify-center">
                        <img src={loginIllustration} className="w-full max-w-[600px]" alt="Login Illustration" />
                    </div>

                </div>
            </div>
        </div>
    );
};

export default Login;
