import { useState, useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { Settings, ChevronDown, User, LogOut } from 'lucide-react'
import logoDark from '../../assets/images/common/logo-dark-2.svg'
import { useAuthStore } from '../../store/auth.store'
import NotificationBell from './NotificationBell'

export type HomeTab = 'shadowing' | 'copik' | 'daily'
export type Language = 'ko' | 'vi' | 'en'

const languages = [
  { code: 'ko', label: '한국어', nativeLabel: '한국어' },
  { code: 'vi', label: '베트남어', nativeLabel: 'Tiếng Việt' },
  { code: 'en', label: '영어', nativeLabel: 'English' },
] as const

const Header = () => {
  const navigate = useNavigate()
  const [selectedLanguage, setSelectedLanguage] = useState<Language>('ko')
  const [isLanguageOpen, setIsLanguageOpen] = useState(false)
  const [isProfileOpen, setIsProfileOpen] = useState(false)
  const languageRef = useRef<HTMLDivElement>(null)
  const profileRef = useRef<HTMLDivElement>(null)

  // Zustand store에서 사용자 정보 및 로그아웃 가져오기
  const userInfo = useAuthStore((state) => state.userInfo)
  const logout = useAuthStore((state) => state.logout)

  // 드롭다운 외부 클릭 감지
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (languageRef.current && !languageRef.current.contains(event.target as Node)) {
        setIsLanguageOpen(false)
      }
      if (profileRef.current && !profileRef.current.contains(event.target as Node)) {
        setIsProfileOpen(false)
      }
    }

    document.addEventListener('mousedown', handleClickOutside)
    return () => {
      document.removeEventListener('mousedown', handleClickOutside)
    }
  }, [])

  // 표시용 사용자 정보 (로딩 중 기본값 처리)
  const displayInfo = {
    nickname: userInfo?.nickname || (useAuthStore.getState().isAuthenticated ? 'Loading...' : 'Guest'),
    email: userInfo?.email || '',
    profileImage: userInfo?.profile_url && userInfo.profile_url !== 'http://' ? userInfo.profile_url : null
  }

  const currentLanguage = languages.find((lang) => lang.code === selectedLanguage)

  const handleMyPageClick = () => {
    setIsProfileOpen(false)
    navigate('/mypage')
  }

  const handleLogoutClick = () => {
    setIsProfileOpen(false)
    logout()
    navigate('/login')
  }

  return (
    <div className='w-full h-(--header-height) bg-white border-b border-gray-200'>
      <div className='mx-auto flex h-full w-full items-center justify-between px-6'>
        {/* 로고 */}
        <div className='flex items-center gap-3'>
          <img src={logoDark} alt='Meari' className='h-7' />
        </div>

        {/* 오른쪽 메뉴 */}
        <div className='flex items-center gap-2 text-[17px] text-gray-700'>
          {/* 언어 선택 드롭다운 */}
          <div className='relative' ref={languageRef}>
            <button
              type='button'
              onClick={() => setIsLanguageOpen(!isLanguageOpen)}
              className='flex items-center gap-1.5 rounded-full px-3 py-1.5 hover:bg-gray-100 transition-colors cursor-pointer'
            >
              {currentLanguage?.nativeLabel}
              <ChevronDown size={17} className={`transition-transform ${isLanguageOpen ? 'rotate-180' : ''}`} />
            </button>

            {/* 언어 드롭다운 메뉴 */}
            {isLanguageOpen && (
              <div className='absolute flex flex-col gap-1 top-full right-0 mt-2 bg-white rounded-lg shadow-lg z-10 min-w-40 p-2'>
                {languages.map((lang) => (
                  <button
                    key={lang.code}
                    onClick={() => {
                      setSelectedLanguage(lang.code)
                      setIsLanguageOpen(false)
                    }}
                    className={`flex flex-col items-start px-3 py-2 w-full hover:bg-gray-100 transition-colors text-left rounded-md cursor-pointer ${selectedLanguage === lang.code ? 'bg-blue-50 text-blue-600' : 'text-gray-700'
                      }`}
                  >
                    <span className='font-medium'>{lang.nativeLabel}</span>
                    <span className='text-xs text-gray-500'>{lang.label}</span>
                  </button>
                ))}
              </div>
            )}
          </div>

          {/* 알림 벨 */}
          <NotificationBell />

          {/* 설정 버튼 */}
          <button
            type='button'
            className='flex items-center justify-center w-10 h-10 rounded-full hover:bg-gray-100 transition-colors cursor-pointer'
            title='설정'
            onClick={() => navigate('/mypage', { state: { menu: 'settings' } })}
          >
            <Settings size={21} />
          </button>

          {/* 프로필 버튼 */}
          <div className='relative ml-3' ref={profileRef}>
            <button
              type='button'
              onClick={() => setIsProfileOpen(!isProfileOpen)}
              className='flex items-center justify-center w-10 h-10 rounded-full bg-[var(--color-bg-button)] text-white font-semibold hover:opacity-90 transition-opacity cursor-pointer'
              title='프로필'
            >
              {displayInfo.profileImage ? (
                <img src={displayInfo.profileImage} alt='Profile' className='w-full h-full rounded-full object-cover' />
              ) : (
                displayInfo.nickname.charAt(0).toUpperCase()
              )}
            </button>

            {/* 프로필 드롭다운 메뉴 */}
            {isProfileOpen && (
              <div className='absolute top-full right-0 mt-2 w-64 bg-white rounded-lg shadow-lg z-10 p-2'>
                {/* 사용자 정보 */}
                <div className='px-3 py-2.5 border-b border-gray-200 mb-1'>
                  <div className='flex items-center gap-3'>
                    <div className='flex items-center justify-center w-[52px] h-[52px] rounded-full bg-blue-500 text-white font-semibold text-xl'>
                      {displayInfo.profileImage ? (
                        <img src={displayInfo.profileImage} alt='Profile' className='w-full h-full rounded-full object-cover' />
                      ) : (
                        displayInfo.nickname.charAt(0).toUpperCase()
                      )}
                    </div>
                    <div className='flex-1'>
                      <p className='font-semibold text-gray-900'>{userInfo?.nickname || '사용자'}</p>
                      <p className='text-sm text-gray-500'>{userInfo?.email || ''}</p>
                    </div>
                  </div>
                </div>

                {/* 메뉴 버튼들 */}
                <div className='space-y-1'>
                  <button
                    onClick={handleMyPageClick}
                    className='flex items-center gap-3 px-3 py-2 w-full hover:bg-gray-100 transition-colors text-left text-gray-700 rounded-md cursor-pointer'
                  >
                    <User size={20} />
                    <span>마이페이지</span>
                  </button>
                  <button
                    onClick={handleLogoutClick}
                    className='flex items-center gap-3 px-3 py-2 w-full hover:bg-gray-100 transition-colors text-left text-red-600 rounded-md cursor-pointer'
                  >
                    <LogOut size={20} />
                    <span>로그아웃</span>
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}

export default Header
