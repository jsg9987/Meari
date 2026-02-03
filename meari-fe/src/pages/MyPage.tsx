import { useEffect, useState } from 'react'
import { useLocation } from 'react-router-dom'
import SideBar, { type SideBarMenu } from '../components/common/SideBar'
import Dashboard from '../components/mypage/Dashboard'

const MyPage = () => {
  const location = useLocation()
  const [activeMenu, setActiveMenu] = useState<SideBarMenu>('dashboard')
  const [autoPlay, setAutoPlay] = useState(false)
  const [fontSize, setFontSize] = useState<'small' | 'medium' | 'large'>('medium')
  const [theme, setTheme] = useState<'light' | 'dark'>('light')

  useEffect(() => {
    const nextMenu = (location.state as { menu?: SideBarMenu } | null)?.menu
    if (nextMenu) {
      setActiveMenu(nextMenu)
    }
  }, [location.state])

  useEffect(() => {
    const storedAutoPlay = localStorage.getItem('settings.autoPlay')
    const storedFontSize = localStorage.getItem('settings.fontSize') as
      | 'small'
      | 'medium'
      | 'large'
      | null
    const storedTheme = localStorage.getItem('settings.theme') as 'light' | 'dark' | null

    if (storedAutoPlay !== null) {
      setAutoPlay(storedAutoPlay === 'true')
    }
    if (storedFontSize) {
      setFontSize(storedFontSize)
    }
    if (storedTheme) {
      setTheme(storedTheme)
    }
  }, [])

  useEffect(() => {
    localStorage.setItem('settings.autoPlay', String(autoPlay))
    document.documentElement.setAttribute('data-auto-play', autoPlay ? 'on' : 'off')
  }, [autoPlay])

  useEffect(() => {
    localStorage.setItem('settings.fontSize', fontSize)
    document.documentElement.setAttribute('data-font-size', fontSize)
  }, [fontSize])

  useEffect(() => {
    localStorage.setItem('settings.theme', theme)
    document.documentElement.setAttribute('data-theme', theme)
  }, [theme])

  const handleResetLocalSettings = () => {
    localStorage.removeItem('settings.autoPlay')
    localStorage.removeItem('settings.fontSize')
    localStorage.removeItem('settings.theme')
    setAutoPlay(false)
    setFontSize('medium')
    setTheme('light')
  }

  const renderContent = () => {
    switch (activeMenu) {
      case 'dashboard':
        return <Dashboard />
      case 'profile':
        return (
          <div className='p-8'>
            <h1 className='text-3xl font-bold text-gray-900 mb-6'>Profile</h1>
            <p className='text-gray-600'>View and update your profile details.</p>
          </div>
        )
      case 'report':
        return (
          <div className='p-8'>
            <h1 className='text-3xl font-bold text-gray-900 mb-6'>Reports</h1>
            <p className='text-gray-600'>Check your learning reports here.</p>
          </div>
        )
      case 'settings':
        return (
          <div className='p-8'>
            <h1 className='text-3xl font-bold text-gray-900 mb-6'>Settings</h1>
            <div className='space-y-6 text-gray-800'>
              <div>
                <label className='flex items-center gap-2'>
                  <input
                    type='checkbox'
                    checked={autoPlay}
                    onChange={(event) => setAutoPlay(event.target.checked)}
                  />
                  자동 재생
                </label>
              </div>

              <div>
                <label className='block mb-2'>글자 크기</label>
                <select
                  value={fontSize}
                  onChange={(event) =>
                    setFontSize(event.target.value as 'small' | 'medium' | 'large')
                  }
                >
                  <option value='small'>작게</option>
                  <option value='medium'>보통</option>
                  <option value='large'>크게</option>
                </select>
              </div>

              <div>
                <label className='block mb-2'>테마</label>
                <select
                  value={theme}
                  onChange={(event) => setTheme(event.target.value as 'light' | 'dark')}
                >
                  <option value='light'>라이트</option>
                  <option value='dark'>다크</option>
                </select>
              </div>

              <div>
                <button type='button' onClick={handleResetLocalSettings}>
                  캐시/데이터 초기화
                </button>
              </div>
            </div>
          </div>
        )
      default:
        return null
    }
  }

  return (
    <div className='flex min-h-screen w-full'>
      <SideBar activeMenu={activeMenu} onMenuChange={setActiveMenu} userInitial='U' />

      <main className='flex-1 bg-white ml-64'>
        {renderContent()}
      </main>
    </div>
  )
}

export default MyPage
