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
  const [profileStep, setProfileStep] = useState<'view' | 'verify' | 'edit' | 'password'>('view')
  const [verifyPassword, setVerifyPassword] = useState('')
  const [nicknameInput, setNicknameInput] = useState('')
  const [nativeLanguage, setNativeLanguage] = useState<'KR' | 'VN' | 'EN'>('KR')
  const [profileImageName, setProfileImageName] = useState('')
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')

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

  const handleVerifySubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!verifyPassword.trim()) return
    setProfileStep('edit')
  }

  const handleProfileSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setProfileStep('view')
  }

  const handlePasswordSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setProfileStep('view')
  }

  const renderContent = () => {
    switch (activeMenu) {
      case 'dashboard':
        return <Dashboard />
      case 'profile':
        return (
          <div className='p-8'>
            <h1 className='text-3xl font-bold text-gray-900 mb-6'>{'\uD504\uB85C\uD544'}</h1>
            {profileStep === 'view' && (
              <div className='max-w-2xl space-y-6 text-gray-800'>
                <div className='flex items-center gap-4'>
                  <div className='flex h-16 w-16 items-center justify-center rounded-full bg-gray-200 text-gray-600'>
                    {'IMG'}
                  </div>
                  <div className='space-y-2'>
                    <label className='block text-sm font-medium text-gray-700'>
                      {'\uD504\uB85C\uD544 \uC0AC\uC9C4'}
                    </label>
                    <p className='text-xs text-gray-500'>{profileImageName || '-'}</p>
                  </div>
                </div>

                <div className='grid gap-4 sm:grid-cols-2'>
                  <div className='flex flex-col gap-2'>
                    <label className='text-sm font-medium text-gray-700'>
                      {'\uB2C9\uB124\uC784'}
                    </label>
                    <input
                      type='text'
                      value={nicknameInput}
                      disabled
                      readOnly
                      className='rounded border border-gray-200 bg-gray-50 px-3 py-2 text-gray-500'
                    />
                  </div>
                  <div className='flex flex-col gap-2'>
                    <label className='text-sm font-medium text-gray-700'>
                      {'\uBAA8\uAD6D\uC5B4'}
                    </label>
                    <input
                      type='text'
                      value={nativeLanguage}
                      disabled
                      readOnly
                      className='rounded border border-gray-200 bg-gray-50 px-3 py-2 text-gray-500'
                    />
                  </div>
                </div>

                <div className='flex flex-wrap gap-3'>
                  <button
                    type='button'
                    onClick={() => setProfileStep('verify')}
                    className='rounded bg-blue-500 px-4 py-2 text-white'
                  >
                    {'\uAC1C\uC778\uC815\uBCF4 \uC218\uC815'}
                  </button>
                  <button
                    type='button'
                    onClick={() => setProfileStep('password')}
                    className='rounded border border-gray-300 px-4 py-2'
                  >
                    {'\uBE44\uBC00\uBC88\uD638 \uBCC0\uACBD'}
                  </button>
                  <button
                    type='button'
                    className='rounded border border-gray-200 px-4 py-2 text-gray-400'
                  >
                    {'\uD68C\uC6D0\uD0C8\uD1F4'}
                  </button>
                </div>
              </div>
            )}

            {profileStep === 'verify' && (
              <div className='max-w-lg space-y-4 text-gray-800'>
                <p className='text-sm text-gray-600'>
                  {'\uD504\uB85C\uD544 \uC815\uBCF4 \uC218\uC815\uC744 \uC704\uD574 \uBE44\uBC00\uBC88\uD638\uB97C \uD655\uC778\uD574\uC8FC\uC138\uC694.'}
                </p>
                <form onSubmit={handleVerifySubmit} className='space-y-3'>
                  <div className='flex flex-col gap-2'>
                    <label className='text-sm font-medium text-gray-700'>
                      {'\uBE44\uBC00\uBC88\uD638'}
                    </label>
                    <input
                      type='password'
                      value={verifyPassword}
                      onChange={(event) => setVerifyPassword(event.target.value)}
                      className='rounded border border-gray-300 px-3 py-2'
                    />
                  </div>
                  <button type='submit' className='rounded bg-blue-500 px-4 py-2 text-white'>
                    {'\uD655\uC778'}
                  </button>
                </form>
              </div>
            )}

            {profileStep === 'edit' && (
              <div className='max-w-2xl space-y-6 text-gray-800'>
                <form onSubmit={handleProfileSubmit} className='space-y-5'>
                  <div className='flex items-center gap-4'>
                    <div className='flex h-16 w-16 items-center justify-center rounded-full bg-gray-200 text-gray-600'>
                      {'IMG'}
                    </div>
                    <div className='space-y-2'>
                      <label className='block text-sm font-medium text-gray-700'>
                        {'\uD504\uB85C\uD544 \uC0AC\uC9C4 \uBCC0\uACBD'}
                      </label>
                      <input
                        type='file'
                        accept='image/*'
                        onChange={(event) => {
                          const file = event.target.files?.[0]
                          setProfileImageName(file?.name ?? '')
                        }}
                        className='text-sm'
                      />
                      {profileImageName && (
                        <p className='text-xs text-gray-500'>{profileImageName}</p>
                      )}
                    </div>
                  </div>

                  <div className='grid gap-4 sm:grid-cols-2'>
                    <div className='flex flex-col gap-2'>
                      <label className='text-sm font-medium text-gray-700'>
                        {'\uB2C9\uB124\uC784'}
                      </label>
                      <input
                        type='text'
                        value={nicknameInput}
                        onChange={(event) => setNicknameInput(event.target.value)}
                        className='rounded border border-gray-300 px-3 py-2'
                        placeholder={'\uB2C9\uB124\uC784\uC744 \uC785\uB825\uD574\uC8FC\uC138\uC694'}
                      />
                    </div>
                    <div className='flex flex-col gap-2'>
                      <label className='text-sm font-medium text-gray-700'>
                        {'\uBAA8\uAD6D\uC5B4'}
                      </label>
                      <select
                        value={nativeLanguage}
                        onChange={(event) =>
                          setNativeLanguage(event.target.value as 'KR' | 'VN' | 'EN')
                        }
                        className='rounded border border-gray-300 px-3 py-2'
                      >
                        <option value='KR'>{'\uD55C\uAD6D\uC5B4'}</option>
                        <option value='VN'>{'\uBCA0\uD2B8\uB0A8\uC5B4'}</option>
                        <option value='EN'>{'\uC601\uC5B4'}</option>
                      </select>
                    </div>
                  </div>

                  <div className='flex flex-wrap gap-3'>
                    <button type='submit' className='rounded bg-blue-500 px-4 py-2 text-white'>
                      {'\uC800\uC7A5'}
                    </button>
                    <button
                      type='button'
                      onClick={() => setProfileStep('password')}
                      className='rounded border border-gray-300 px-4 py-2'
                    >
                      {'\uBE44\uBC00\uBC88\uD638 \uBCC0\uACBD'}
                    </button>
                    <button
                      type='button'
                      onClick={() => setProfileStep('view')}
                      className='rounded border border-gray-200 px-4 py-2 text-gray-600'
                    >
                      {'\uCDE8\uC18C'}
                    </button>
                  </div>
                </form>
              </div>
            )}

            {profileStep === 'password' && (
              <div className='max-w-lg space-y-4 text-gray-800'>
                <form onSubmit={handlePasswordSubmit} className='space-y-4'>
                  <div className='flex flex-col gap-2'>
                    <label className='text-sm font-medium text-gray-700'>
                      {'\uD604\uC7AC \uBE44\uBC00\uBC88\uD638'}
                    </label>
                    <input
                      type='password'
                      value={currentPassword}
                      onChange={(event) => setCurrentPassword(event.target.value)}
                      className='rounded border border-gray-300 px-3 py-2'
                    />
                  </div>
                  <div className='flex flex-col gap-2'>
                    <label className='text-sm font-medium text-gray-700'>
                      {'\uC0C8 \uBE44\uBC00\uBC88\uD638'}
                    </label>
                    <input
                      type='password'
                      value={newPassword}
                      onChange={(event) => setNewPassword(event.target.value)}
                      className='rounded border border-gray-300 px-3 py-2'
                    />
                  </div>
                  <div className='flex flex-col gap-2'>
                    <label className='text-sm font-medium text-gray-700'>
                      {'\uC0C8 \uBE44\uBC00\uBC88\uD638 \uD655\uC778'}
                    </label>
                    <input
                      type='password'
                      value={confirmPassword}
                      onChange={(event) => setConfirmPassword(event.target.value)}
                      className='rounded border border-gray-300 px-3 py-2'
                    />
                  </div>
                  <div className='flex gap-3'>
                    <button type='submit' className='rounded bg-blue-500 px-4 py-2 text-white'>
                      {'\uBCC0\uACBD'}
                    </button>
                    <button
                      type='button'
                      onClick={() => setProfileStep('view')}
                      className='rounded border border-gray-200 px-4 py-2 text-gray-600'
                    >
                      {'\uB4A4\uB85C'}
                    </button>
                  </div>
                </form>
              </div>
            )}
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
