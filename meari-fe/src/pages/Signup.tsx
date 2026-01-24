/**
 * =========================================================================================
 * [Step 5] Signup UI Implementation
 * - 스타일(CSS) 적용 금지 (User Rule)
 * - Pure HTML 태그로 기능 위주 구현
 * - 필수 필드: 이메일, 닉네임, 비밀번호, 비밀번호 확인, 성별, 모국어
 * =========================================================================================
 */
import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { signup } from '../api/auth.api';

const Signup = () => {
    const navigate = useNavigate();

    // 입력 상태 관리
    const [formData, setFormData] = useState({
        email: '',
        nickname: '',
        password: '',
        confirmPassword: '',
        sex: '',
        native_language: 'KR' // 기본값 'KR'
    });

    // 비밀번호 보이기/숨기기 토글 상태
    const [showPassword, setShowPassword] = useState(false);
    const [showConfirmPassword, setShowConfirmPassword] = useState(false);

    // UI 상태
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError(null);

        // 유효성 검사
        if (!formData.email || !formData.nickname || !formData.password || !formData.sex) {
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
                sex: formData.sex,
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

    return (
        <div>
            <h2>회원가입</h2>
            <form onSubmit={handleSubmit}>
                {/* 이메일 */}
                <div>
                    <label>이메일:</label>
                    <br />
                    <input
                        type="email"
                        name="email"
                        value={formData.email}
                        onChange={handleChange}
                        placeholder="이메일을 입력해주세요."
                    />
                </div>
                <br />

                {/* 닉네임 */}
                <div>
                    <label>닉네임:</label>
                    <br />
                    <input
                        type="text"
                        name="nickname"
                        value={formData.nickname}
                        onChange={handleChange}
                        placeholder="닉네임을 입력해주세요."
                    />
                </div>
                <br />

                {/* 비밀번호 */}
                <div>
                    <label>비밀번호 입력:</label>
                    <br />
                    <input
                        type={showPassword ? "text" : "password"}
                        name="password"
                        value={formData.password}
                        onChange={handleChange}
                        placeholder="비밀번호를 입력해주세요."
                    />
                    <button type="button" onClick={() => setShowPassword(!showPassword)}>
                        {showPassword ? "숨기기" : "보기"}
                    </button>
                </div>
                <br />

                {/* 비밀번호 확인 */}
                <div>
                    <label>비밀번호 확인:</label>
                    <br />
                    <input
                        type={showConfirmPassword ? "text" : "password"}
                        name="confirmPassword"
                        value={formData.confirmPassword}
                        onChange={handleChange}
                        placeholder="비밀번호를 다시 입력해주세요."
                    />
                    <button type="button" onClick={() => setShowConfirmPassword(!showConfirmPassword)}>
                        {showConfirmPassword ? "숨기기" : "보기"}
                    </button>
                </div>
                <br />

                {/* 성별 (DB Schema Essential) */}
                <div>
                    <label>성별:</label>
                    <label>
                        <input
                            type="radio"
                            name="sex"
                            value="M"
                            onChange={handleChange}
                        /> 남성
                    </label>
                    <label>
                        <input
                            type="radio"
                            name="sex"
                            value="F"
                            onChange={handleChange}
                        /> 여성
                    </label>
                </div>
                <br />

                {/* 모국어 (DB Schema Essential) */}
                <div>
                    <label>모국어:</label>
                    <select name="native_language" value={formData.native_language} onChange={handleChange}>
                        <option value="KR">한국어</option>
                        <option value="EN">English</option>
                        <option value="JP">Japanese</option>
                        <option value="CN">Chinese</option>
                    </select>
                </div>
                <br />

                {/* 에러 메시지 */}
                {error && <div style={{ color: 'red' }}>{error}</div>}

                <button type="submit" disabled={isLoading}>
                    {isLoading ? '가입 중...' : '회원가입'}
                </button>
            </form>

            <br />
            <div>
                이미 계정이 있으신가요? <Link to="/login">로그인하기</Link>
            </div>
        </div>
    );
};

export default Signup;
