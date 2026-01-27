import { useState, useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { Mic, FileText, Calendar, Settings, ChevronDown, User, LogOut } from 'lucide-react'
import logoWhite from '../../assets/images/common/logo-white.svg'

export type HomeTab = 'shadowing' | 'copik' | 'daily'
export type Language = 'ko' | 'vi' | 'en'

type HeaderProps = {
  activeTab: HomeTab
  onTabChange: (tab: HomeTab) => void
}

const tabs = [
  { id: 'shadowing', label: '쉐도잉', icon: Mic },
  { id: 'copik', label: '코픽', icon: FileText },
  { id: 'daily', label: '일일 학습', icon: Calendar },
] as const

const languages = [
  { code: 'ko', label: '한국어', nativeLabel: '한국어' },
  { code: 'vi', label: '베트남어', nativeLabel: 'Tiếng Việt' },
  { code: 'en', label: '영어', nativeLabel: 'English' },
] as const

const Header = ({ activeTab, onTabChange }: HeaderProps) => {
  const activeIndex = tabs.findIndex((tab) => tab.id === activeTab)
  const navigate = useNavigate()
  const [selectedLanguage, setSelectedLanguage] = useState<Language>('ko')
  const [isLanguageOpen, setIsLanguageOpen] = useState(false)
  const [isProfileOpen, setIsProfileOpen] = useState(false)
  const languageRef = useRef<HTMLDivElement>(null)
  const profileRef = useRef<HTMLDivElement>(null)

  // Mock 사용자 정보
  const userInfo = {
    nickname: 'User',
    email: 'user@example.com',
    profileImage: null // null이면 이니셜 표시
  }

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

  const currentLanguage = languages.find((lang) => lang.code === selectedLanguage)

  const handleMyPageClick = () => {
    setIsProfileOpen(false)
    navigate('/mypage')
  }

  const handleLogoutClick = () => {
    setIsProfileOpen(false)
    // TODO: 로그아웃 처리
    console.log('Logout')
  }

  return (
    <div className='w-full h-(--header-height) bg-(--color-bg-root)'>
      <div className='mx-auto grid h-full w-full max-w-300 grid-cols-[1fr_auto_1fr] items-center px-6'>
        {/* 로고 */}
        <div className='flex items-center gap-3'>
          <img src={logoWhite} alt='Meari' className='h-5' />
        </div>

        {/* 메뉴 탭 */}
        <nav className='flex items-center justify-center' aria-label='Main'>
          <div className='relative flex gap-3'>
            <div
              className='absolute top-0 left-0 h-full w-30 rounded-lg transition-transform duration-300 ease-out'
              style={{
                transform: `translateX(${activeIndex * 132}px)`,
                backgroundColor: 'var(--color-tab-active)'
              }}
              aria-hidden
            />
            <div className='relative z-10 grid grid-cols-3 text-sm font-medium text-white/80' role='tablist'>
              {tabs.map((tab) => (
                <button
                  key={tab.id}
                  type='button'
                  role='tab'
                  aria-selected={tab.id === activeTab}
                  className={`py-2 transition-colors ${tab.id === activeTab ? 'text-slate-900' : 'text-white/80'
                    }`}
                  onClick={() => onTabChange(tab.id)}
                >
                  {tab.label}
                </button>
              ))}
            </div>
          </div>
        </nav>

        {/* 오른쪽 메뉴 */}
        <div className='flex items-center justify-end gap-3 text-sm text-white/80'>
          {/* 언어 선택 드롭다운 */}
          <div className='relative' ref={languageRef}>
            <button
              type='button'
              onClick={() => setIsLanguageOpen(!isLanguageOpen)}
              className='flex items-center gap-1.5 rounded-full border border-white/20 px-3 py-1.5 hover:bg-white/5 transition-colors cursor-pointer'
            >
              {currentLanguage?.nativeLabel}
              <ChevronDown size={14} className={`transition-transform ${isLanguageOpen ? 'rotate-180' : ''}`} />
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

          {/* 설정 버튼 */}
          <button
            type='button'
            className='flex items-center justify-center w-9 h-9 rounded-full border border-white/20 hover:bg-white/5 transition-colors cursor-pointer'
            title='설정'
          >
            <Settings size={18} />
          </button>

          {/* 프로필 버튼 */}
          <div className='relative' ref={profileRef}>
            <button
              type='button'
              onClick={() => setIsProfileOpen(!isProfileOpen)}
              className='flex items-center justify-center w-9 h-9 rounded-full bg-blue-500 text-white font-semibold hover:bg-blue-600 transition-colors cursor-pointer'
              title='프로필'
            >
              {userInfo.profileImage ? (
                <img src={userInfo.profileImage} alt='Profile' className='w-full h-full rounded-full object-cover' />
              ) : (
                userInfo.nickname.charAt(0).toUpperCase()
              )}
            </button>

            {/* 프로필 드롭다운 메뉴 */}
            {isProfileOpen && (
              <div className='absolute top-full right-0 mt-2 w-64 bg-white rounded-lg shadow-lg z-10 p-2'>
                {/* 사용자 정보 */}
                <div className='px-3 py-2.5 border-b border-gray-200 mb-1'>
                  <div className='flex items-center gap-3'>
                    <div className='flex items-center justify-center w-12 h-12 rounded-full bg-blue-500 text-white font-semibold text-lg'>
                      {userInfo.profileImage ? (
                        <img src={userInfo.profileImage} alt='Profile' className='w-full h-full rounded-full object-cover' />
                      ) : (
                        userInfo.nickname.charAt(0).toUpperCase()
                      )}
                    </div>
                    <div className='flex-1'>
                      <p className='font-semibold text-gray-900'>{userInfo.nickname}</p>
                      <p className='text-sm text-gray-500'>{userInfo.email}</p>
                    </div>
                  </div>
                </div>

                {/* 메뉴 버튼들 */}
                <div className='space-y-1'>
                  <button
                    onClick={handleMyPageClick}
                    className='flex items-center gap-3 px-3 py-2 w-full hover:bg-gray-100 transition-colors text-left text-gray-700 rounded-md cursor-pointer'
                  >
                    <User size={18} />
                    <span>마이페이지</span>
                  </button>
                  <button
                    onClick={handleLogoutClick}
                    className='flex items-center gap-3 px-3 py-2 w-full hover:bg-gray-100 transition-colors text-left text-red-600 rounded-md cursor-pointer'
                  >
                    <LogOut size={18} />
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
