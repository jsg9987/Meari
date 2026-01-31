import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { signup } from '../api/auth.api';

// Assets
import logoDark from '../assets/images/common/logo-dark.svg';
import signupIllustration from '../assets/images/auth/Signup-illustaration.svg';

const Signup = () => {
    const navigate = useNavigate();
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const [formData, setFormData] = useState({
        email: '',
        nickname: '',
        password: '',
        confirmPassword: '',
        native_language: 'KR'
    });

    const [showPassword, setShowPassword] = useState(false);
    const [showConfirmPassword, setShowConfirmPassword] = useState(false);

    const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError(null);

        // 유효성 검사
        if (!formData.email || !formData.nickname || !formData.password) {
            setError('모든 필수 항목을 입력해주세요.');
            return;
        }

        if (formData.password.length < 8) {
            setError('비밀번호는 8자 이상이어야 합니다.');
            return;
        }

        if (formData.password !== formData.confirmPassword) {
            setError('비밀번호가 일치하지 않습니다.');
            return;
        }

        try {
            setIsLoading(true);
            // API 호출
            await signup({
                email: formData.email,
                password: formData.password,
                nickname: formData.nickname,
                sex: 'M', // 성별 필드 제거로 인해 임시 기본값 설정 (API 요구사항에 따라 조정 필요)
                native_language: formData.native_language
            });

            // 성공 시 로그인 페이지로 이동
            alert('회원가입이 완료되었습니다. 로그인해주세요.');
            navigate('/login');
        } catch (err: any) {
            const msg = err.response?.data?.error?.message || '회원가입에 실패했습니다.';
            setError(msg);
        } finally {
            setIsLoading(false);
        }
    };

    const labelClass = 'text-sm font-medium text-[#001C27]';

    const inputClass =
        'h-12 w-full rounded-lg border border-[#e5e5e5] bg-white ' +
        'px-4 text-sm text-[#001C27] ' +
        'placeholder:text-[#bebebe] ' +
        'focus:outline-none focus:ring-2 focus:ring-[#001C27]/20';

    const checkButtonClass =
        'text-[10px] bg-[#e5e5e5] text-[#666] px-2 py-1 rounded font-pretendard transition ' +
        'hover:bg-[#d5d5d5] shadow-[0_1px_1px_rgba(0,0,0,0.1)] active:shadow-none active:translate-y-[1px]';

    return (
        <div className="min-h-screen bg-white flex">
            <div className="mx-auto w-full max-w-[1280px] px-6 lg:px-10 min-h-screen flex flex-col">

                {/* Header Logo */}
                <div className="w-[200px] h-[80px] p-[10px] flex items-center justify-center">
                    <img src={logoDark} alt="MEARI Logo" className="w-full h-full object-contain" />
                </div>

                {/* Content Area */}
                <div className="flex-1 flex flex-col lg:flex-row items-center justify-center pl-15">

                    {/* Signup Card */}
                    <div
                        className={
                            'w-full max-w-[450px] rounded-xl border border-[#bebebe] bg-white p-8 sm:p-10 ' +
                            'shadow-[0_1px_2px_rgba(0,0,0,0.25)]' +
                            'transition-opacity duration-200'
                        }
                    >
                        <form onSubmit={handleSubmit} className="space-y-4">
                            <div className="mb-6 space-y-2">
                                <h2 className="text-2xl font-bold font-pretendard text-[#001C27]">회원가입</h2>
                                <p className="text-xs font-pretendard text-[#666]">개인정보는 Meari에서 안전하게 보호됩니다.</p>
                            </div>

                            {/* Email */}
                            <div className="space-y-2">
                                <div className="flex justify-between items-center">
                                    <label htmlFor="email" className={labelClass}>이메일</label>
                                    <button type="button" className={checkButtonClass}>중복확인</button>
                                </div>
                                <input
                                    id="email"
                                    type="email"
                                    name="email"
                                    className={inputClass}
                                    placeholder="이메일을 입력 해주세요."
                                    value={formData.email}
                                    onChange={handleChange}
                                />
                            </div>

                            {/* 닉네임 */}
                            <div className="space-y-2">
                                <div className="flex justify-between items-center">
                                    <label htmlFor="nickname" className={labelClass}>
                                        닉네임
                                    </label>
                                    <button type="button" className={checkButtonClass}>중복확인</button>
                                </div>
                                <input
                                    id="nickname"
                                    type="text"
                                    name="nickname"
                                    className={inputClass}
                                    placeholder="닉네임을 입력 해주세요."
                                    value={formData.nickname}
                                    onChange={handleChange}
                                />
                            </div>

                            {/* Password */}
                            <div className="space-y-2">
                                <label htmlFor="password" className={labelClass}>비밀번호 입력</label>
                                <div className="relative">
                                    <input
                                        id="password"
                                        type={showPassword ? "text" : "password"}
                                        name="password"
                                        className={inputClass}
                                        placeholder="비밀번호를 입력 해주세요."
                                        value={formData.password}
                                        onChange={handleChange}
                                    />
                                    <button
                                        type="button"
                                        className="absolute right-4 top-1/2 -translate-y-1/2 text-[#bebebe]"
                                        onClick={() => setShowPassword(!showPassword)}
                                    >
                                        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-5 h-5">
                                            <path strokeLinecap="round" strokeLinejoin="round" d="M2.036 12.322a1.012 1.012 0 010-.644C3.301 8.844 6.533 6.25 10 6.25c3.467 0 6.7 2.594 7.964 5.428a1.012 1.012 0 010 .644C16.699 15.156 13.467 17.75 10 17.75c-3.467 0-6.7-2.594-7.964-5.428z" />
                                            <path strokeLinecap="round" strokeLinejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                                        </svg>
                                    </button>
                                </div>
                            </div>

                            {/* Confirm Password */}
                            <div className="space-y-2">
                                <label htmlFor="confirmPassword" className={labelClass}>비밀번호 확인</label>
                                <div className="relative">
                                    <input
                                        id="confirmPassword"
                                        type={showConfirmPassword ? "text" : "password"}
                                        name="confirmPassword"
                                        className={inputClass}
                                        placeholder="비밀번호를 입력 해주세요."
                                        value={formData.confirmPassword}
                                        onChange={handleChange}
                                    />
                                    <button
                                        type="button"
                                        className="absolute right-4 top-1/2 -translate-y-1/2 text-[#bebebe]"
                                        onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                                    >
                                        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-5 h-5">
                                            <path strokeLinecap="round" strokeLinejoin="round" d="M2.036 12.322a1.012 1.012 0 010-.644C3.301 8.844 6.533 6.25 10 6.25c3.467 0 6.7 2.594 7.964 5.428a1.012 1.012 0 010 .644C16.699 15.156 13.467 17.75 10 17.75c-3.467 0-6.7-2.594-7.964-5.428z" />
                                            <path strokeLinecap="round" strokeLinejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                                        </svg>
                                    </button>
                                </div>
                            </div>

                            {/* Native Language */}
                            <div className="space-y-2">
                                <label htmlFor="native_language" className={labelClass}>모국어 선택</label>
                                <div className="relative">
                                    <select
                                        id="native_language"
                                        name="native_language"
                                        className={inputClass}
                                        value={formData.native_language}
                                        onChange={handleChange}
                                    >
                                        <option value="KR">한국어</option>
                                        <option value="VN">Vietnam</option>
                                    </select>
                                </div>
                            </div>

                            {/* Submit Button */}
                            <button
                                type="submit"
                                disabled={isLoading}
                                className="mt-8 h-12 w-full rounded-lg bg-[#2D9CDB] text-white font-semibold shadow-sm transition hover:bg-[#1B85C4] disabled:bg-[#ccc]"
                            >
                                {isLoading ? '가입 중...' : '회원가입'}
                            </button>

                            {/* Login Redirect */}
                            <p className="mt-4 text-center text-[13px] text-[#bebebe]">
                                이미 계정이 있으신가요?
                                <button
                                    type="button"
                                    className="ml-1 font-bold text-[#2D9CDB] hover:underline"
                                    onClick={() => navigate('/login')}
                                >
                                    로그인하기
                                </button>
                            </p>

                            {/* Error Message */}
                            {error && (
                                <div className="text-red-500 text-xs text-center mt-2">
                                    {error}
                                </div>
                            )}
                        </form>
                    </div>

                    {/* Illustration */}
                    <div className="hidden lg:flex items-center justify-center">
                        <img src={signupIllustration} className="w-full max-w-[600px]" alt="Signup Illustration" />
                    </div>

                </div>
            </div>
        </div>
    );
};

export default Signup; 
