import { useEffect, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import SideBar, { type SideBarMenu } from '../components/common/SideBar'
import Dashboard from '../components/mypage/Dashboard'
import SettingsPanel from '../components/mypage/SettingsPanel'
import ReportTab from '../components/mypage/ReportTab'
import {
  changeMyPagePassword,
  checkMyPagePassword,
  confirmProfileImage,
  getProfileImage,
  getMyPageProfile,
  requestProfileImageUploadUrl,
  updateMyPageProfile
} from '../api/mypage.api'
import { useAuthStore } from '../store/auth.store'

const MyPage = () => {
  const location = useLocation()
  const navigate = useNavigate()
  const { logout } = useAuthStore()
  const [activeMenu, setActiveMenu] = useState<SideBarMenu>('dashboard')
  const [autoPlay, setAutoPlay] = useState(false)
  const [fontSize, setFontSize] = useState<'small' | 'medium' | 'large'>('medium')
  const [theme, setTheme] = useState<'light' | 'dark'>('light')
  const [profileStep, setProfileStep] = useState<'view' | 'verify' | 'edit' | 'password'>('view')
  const [verifyPassword, setVerifyPassword] = useState('')
  const [nicknameInput, setNicknameInput] = useState('')
  const [email, setEmail] = useState('')
  const [nativeLanguage, setNativeLanguage] = useState<'KR' | 'VN' | 'EN'>('KR')
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const [_profileImageName, setProfileImageName] = useState('')
  const [profileImageUrl, setProfileImageUrl] = useState('')
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [profileBanner, setProfileBanner] = useState('')
  const { userInfo, fetchUserInfo, isAuthenticated } = useAuthStore()

  const displayNickname = nicknameInput || '사용자'
  const displayEmail = email || 'user@email.com'

  const profileBannerPool = [
    'linear-gradient(120deg, #c2d7ff 0%, #f7d1ff 100%)',
    'linear-gradient(120deg, #c9f2ff 0%, #ffd5c2 100%)',
    'linear-gradient(120deg, #d8ffe5 0%, #d4e2ff 100%)'
  ]

  useEffect(() => {
    const nextMenu = (location.state as { menu?: SideBarMenu } | null)?.menu
    if (nextMenu) {
      setActiveMenu(nextMenu)
    }
  }, [location.state])

  useEffect(() => {
    const fetchProfile = async () => {
      try {
        const response = await getMyPageProfile()
        const data = response.data?.data
        if (!data) return
        setNicknameInput(data.nickname ?? '')
        setEmail(data.email ?? '')
        setNativeLanguage((data.native_language as 'KR' | 'VN' | 'EN') ?? 'KR')
        setProfileImageUrl(data.profile_image_url ?? '')
      } catch (error) {
        console.error('[MyPage] Failed to load profile', error)
      }
    }
    fetchProfile()
  }, [])

  useEffect(() => {
    if (isAuthenticated && !userInfo) {
      fetchUserInfo()
    }
  }, [isAuthenticated, userInfo, fetchUserInfo])

  useEffect(() => {
    if (!userInfo?.memberId) return
    const fetchProfileImage = async () => {
      try {
        const response = await getProfileImage(userInfo.memberId)
        const data = response.data?.data
        if (data?.profileUrl) {
          setProfileImageUrl(data.profileUrl)
        }
      } catch (error) {
        console.error('[MyPage] Failed to load profile image', error)
      }
    }
    fetchProfileImage()
  }, [userInfo?.memberId])

  const getLanguageLabel = (value: 'KR' | 'VN' | 'EN') => {
    if (value === 'KR') return '한국어'
    if (value === 'VN') return '베트남어'
    return '영어'
  }

  const getImageDimensions = (file: File) =>
    new Promise<{ width: number; height: number }>((resolve, reject) => {
      const img = new Image()
      const url = URL.createObjectURL(file)
      img.onload = () => {
        resolve({ width: img.width, height: img.height })
        URL.revokeObjectURL(url)
      }
      img.onerror = () => {
        URL.revokeObjectURL(url)
        reject(new Error('이미지 로딩 실패'))
      }
      img.src = url
    })
  useEffect(() => {
    const nextIndex = Math.floor(Math.random() * profileBannerPool.length)
    setProfileBanner(profileBannerPool[nextIndex])
  }, [])

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

  const handleLogout = () => {
    logout()
    navigate('/main')
  }

  const handleVerifySubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!verifyPassword.trim()) return
    checkMyPagePassword({ password: verifyPassword })
      .then(() => setProfileStep('edit'))
      .catch(() => alert('비밀번호가 올바르지 않습니다.'))
  }

  const handleProfileSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    updateMyPageProfile({
      nickname: nicknameInput.trim(),
      native_language: nativeLanguage
    })
      .then(() => setProfileStep('view'))
      .catch(() => alert('프로필 저장에 실패했습니다.'))
  }

  const handlePasswordSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!newPassword.trim() || newPassword !== confirmPassword) {
      alert('새 비밀번호를 확인해주세요.')
      return
    }
    changeMyPagePassword({ new_password: newPassword })
      .then(() => setProfileStep('view'))
      .catch(() => alert('비밀번호 변경에 실패했습니다.'))
  }

  // Profile header
  const profileHeader = (
    <div className='mb-6'>
      <h1 className='text-3xl font-bold text-gray-900'>{'\uD504\uB85C\uD544'}</h1>
      <p className='text-sm text-gray-500 mt-2'>
        {'\uD504\uB85C\uD544 \uC815\uBCF4\uB97C \uD655\uC778\uD558\uACE0 \uD544\uC694\uD560 \uB54C \uC218\uC815\uD558\uC138\uC694.'}
      </p>
    </div>
  )

  // Profile banner
  const profileBannerSection = (
    <div
      className='h-[200px] w-full rounded-2xl border border-gray-200'
      style={{ backgroundImage: profileBanner }}
    />
  )

  // Profile info
  const profileInfoGrid = (
    <div className='mt-6 border-t border-gray-200 pt-6'>
      <div className='grid gap-4 sm:grid-cols-2'>
        <div className='flex flex-col gap-2'>
          <label className='text-sm font-medium text-gray-700'>{'\uB2C9\uB124\uC784'}</label>
          <input
            type='text'
            value={displayNickname}
            disabled
            readOnly
            className='rounded border border-gray-200 bg-gray-50 px-3 py-2 text-gray-500'
          />
        </div>
        <div className='flex flex-col gap-2'>
          <label className='text-sm font-medium text-gray-700'>{'\uBAA8\uAD6D\uC5B4'}</label>
          <input
            type='text'
            value={getLanguageLabel(nativeLanguage)}
            disabled
            readOnly
            className='rounded border border-gray-200 bg-gray-50 px-3 py-2 text-gray-500'
          />
        </div>
        <div className='flex flex-col gap-2'>
          <label className='text-sm font-medium text-gray-700'>
            {'\uC5F0\uB77D \uC774\uBA54\uC77C'}
          </label>
          <input
            type='text'
            value={displayEmail}
            disabled
            readOnly
            className='rounded border border-gray-200 bg-gray-50 px-3 py-2 text-gray-500'
          />
        </div>
        <div className='flex flex-col gap-2'>
          <label className='text-sm font-medium text-gray-700'>{'\uAC00\uC785\uC77C'}</label>
          <input
            type='text'
            value={'2026-02-02'}
            disabled
            readOnly
            className='rounded border border-gray-200 bg-gray-50 px-3 py-2 text-gray-500'
          />
        </div>
      </div>
    </div>
  )

  // Profile view
  const profileViewSection = (
    <div className='space-y-6 text-gray-800'>
      {profileBannerSection}
      <div className='rounded-2xl border border-gray-200 bg-white p-6'>
        <div className='flex flex-wrap items-center justify-between gap-4'>
          <div className='flex items-center gap-10'>
            <div className='flex h-28 w-28 items-center justify-center rounded-full border border-gray-300 bg-white text-gray-600'>
              {profileImageUrl ? (
                <img
                  src={profileImageUrl}
                  alt='프로필'
                  className='h-28 w-28 rounded-full object-cover'
                />
              ) : (
                'IMG'
              )}
            </div>
            <div>
              <p className='text-2xl font-semibold text-gray-900 pb-2'>{displayNickname}</p>
              <p className='text-md text-gray-600'>{displayEmail}</p>
            </div>
          </div>
          <div className='flex flex-wrap gap-3'>
            <button
              type='button'
              onClick={() => setProfileStep('verify')}
              className='rounded bg-blue-500 px-3 py-2 text-white text-[14px]'
            >
              {'\uAC1C\uC778\uC815\uBCF4 \uC218\uC815'}
            </button>
            <button
              type='button'
              onClick={() => setProfileStep('password')}
              className='rounded border border-gray-300 px-3 py-2 text-[14px]'
            >
              {'\uBE44\uBC00\uBC88\uD638 \uBCC0\uACBD'}
            </button>
          </div>
        </div>

        {profileInfoGrid}

        <div className='mt-6 flex flex-wrap gap-3'>
          <button
            type='button'
            className='rounded border border-gray-200 px-4 py-2 text-gray-400'
          >
            {'\uD68C\uC6D0\uD0C8\uD1F4'}
          </button>
        </div>
      </div>
    </div>
  )

  // Profile verify
  const profileVerifySection = (
    <div className='min-h-[calc(100vh-280px)] flex items-center justify-center text-gray-800 w-full'>
      <div className='w-full max-w-md rounded-2xl border border-gray-200 bg-white p-6 shadow-sm'>
        <div className='mb-4'>
          <h2 className='text-md font-semibold text-gray-900'>{'\uBE44\uBC00\uBC88\uD638 \uD655\uC778'}</h2>
        </div>
        <form onSubmit={handleVerifySubmit} className='space-y-4'>
          <div className='flex flex-col gap-2'>
            <input
              type='password'
              value={verifyPassword}
              onChange={(event) => setVerifyPassword(event.target.value)}
              className='rounded border border-gray-300 px-3 py-2 text-[12px]'
              placeholder={'\uBE44\uBC00\uBC88\uD638\uB97C \uC785\uB825\uD558\uC138\uC694'}
            />
          </div>
          <div className='flex gap-4'>
            <button type='submit' className='rounded bg-blue-500 px-4 py-1 text-white text-[14px]'>
              {'\uD655\uC778'}
            </button>
            <button
              type='button'
              onClick={() => setProfileStep('view')}
              className='rounded border border-gray-200 px-4 py-1 text-gray-600 text-[14px]'
            >
              {'\uCDE8\uC18C'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )

  // Profile edit
  const profileEditSection = (
    <div className='space-y-6 text-gray-800'>
      {profileBannerSection}
      <div className='rounded-2xl border border-gray-200 bg-white p-6'>
        <form onSubmit={handleProfileSubmit} className='space-y-5'>
          <div className='flex flex-wrap items-center justify-between gap-4'>
            <div className='flex items-center gap-10'>
              <div className='relative'>
                <label
                  htmlFor='profile-image-input'
                  className='cursor-pointer'
                  aria-label='\uD504\uB85C\uD544 \uC0AC\uC9C4 \uBCC0\uACBD'
                >
                  <div className='flex h-28 w-28 items-center justify-center rounded-full border border-gray-300 bg-white text-gray-600'>
                    {profileImageUrl ? (
                      <img
                        src={profileImageUrl}
                        alt='프로필'
                        className='h-28 w-28 rounded-full object-cover'
                      />
                    ) : (
                      'IMG'
                    )}
                  </div>
                  <div className='absolute -bottom-2 -right-2 flex h-7 w-7 items-center justify-center rounded-full bg-gray-900 text-white text-[12px]'>
                    {'\u270E'}
                  </div>
                </label>
                <input
                  id='profile-image-input'
                  type='file'
                  accept='image/*'
                  onChange={async (event) => {
                    const file = event.target.files?.[0]
                    if (!file) return
                    setProfileImageName(file.name)
                    try {
                      const { width, height } = await getImageDimensions(file)
                      const uploadResponse = await requestProfileImageUploadUrl({
                        fileName: file.name,
                        contentType: file.type,
                        fileSize: file.size,
                        width,
                        height
                      })
                      const uploadData = uploadResponse.data?.data
                      if (!uploadData) throw new Error('업로드 URL 발급 실패')
                      await fetch(uploadData.uploadUrl, {
                        method: 'PUT',
                        headers: { 'Content-Type': file.type },
                        body: file
                      })
                      await confirmProfileImage({ profileUrl: uploadData.profileUrl })
                      setProfileImageUrl(uploadData.profileUrl)
                    } catch (error) {
                      console.error('[MyPage] Profile image upload failed', error)
                      alert('프로필 이미지 업로드에 실패했습니다.')
                    }
                  }}
                  className='hidden'
                />
              </div>
              <div>
                <p className='text-2xl font-semibold text-gray-900 pb-2'>
                  {'\uAC1C\uC778\uC815\uBCF4 \uC218\uC815'}
                </p>
                <p className='text-md text-gray-600'>{displayEmail}</p>
              </div>
            </div>
            <div className='flex flex-wrap gap-3'>
              <button type='submit' className='rounded bg-blue-500 px-3 py-2 text-white text-[14px]'>
                {'\uC800\uC7A5'}
              </button>
              <button
                type='button'
                onClick={() => setProfileStep('view')}
                className='rounded border border-gray-200 px-3 py-2 text-gray-600 text-[14px]'
              >
                {'\uCDE8\uC18C'}
              </button>
              <button
                type='button'
                onClick={() => setProfileStep('password')}
                className='rounded border border-gray-300 px-3 py-2 text-[14px]'
              >
                {'\uBE44\uBC00\uBC88\uD638 \uBCC0\uACBD'}
              </button>
            </div>
          </div>

          <div className='mt-2 border-t border-gray-200 pt-6 space-y-5'>
            <div className='grid gap-4 sm:grid-cols-2'>
              <div className='flex flex-col gap-2'>
                <label className='text-sm font-medium text-gray-700'>{'\uB2C9\uB124\uC784'}</label>
                <input
                  type='text'
                  value={nicknameInput}
                  onChange={(event) => setNicknameInput(event.target.value)}
                  className='rounded border border-gray-300 px-3 py-2'
                  placeholder={'\uB2C9\uB124\uC784\uC744 \uC785\uB825\uD574\uC8FC\uC138\uC694'}
                />
              </div>
              <div className='flex flex-col gap-2'>
                <label className='text-sm font-medium text-gray-700'>{'\uBAA8\uAD6D\uC5B4'}</label>
                <select
                  value={nativeLanguage}
                  onChange={(event) => setNativeLanguage(event.target.value as 'KR' | 'VN' | 'EN')}
                  className='rounded border border-gray-300 px-3 py-2'
                >
                  <option value='KR'>{'\uD55C\uAD6D\uC5B4'}</option>
                  <option value='VN'>{'\uBCA0\uD2B8\uB0A8\uC5B4'}</option>
                  <option value='EN'>{'\uC601\uC5B4'}</option>
                </select>
              </div>
              <div className='flex flex-col gap-2'>
                <label className='text-sm font-medium text-gray-700'>
                  {'\uC5F0\uB77D \uC774\uBA54\uC77C'}
                </label>
                <input
                  type='text'
                  value={displayEmail}
                  disabled
                  readOnly
                  className='rounded border border-gray-200 bg-gray-50 px-3 py-2 text-gray-500'
                />
              </div>
            </div>
          </div>
        </form>
      </div>
    </div>
  )

  // Password change
  const profilePasswordSection = (
    <div className='max-w-lg space-y-4 text-gray-800'>
      <div className='rounded-2xl border border-gray-200 bg-white p-5'>
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
    </div>
  )

  const renderContent = () => {
    switch (activeMenu) {
      case 'dashboard':
        return <Dashboard />
      case 'profile':
        return (
          <div className='p-8'>
            {profileHeader}
            {profileStep === 'view' && (
              profileViewSection
            )}

            {profileStep === 'verify' && (
              profileVerifySection
            )}

            {profileStep === 'edit' && (
              profileEditSection
            )}

            {profileStep === 'password' && (
              profilePasswordSection
            )}
          </div>
        )
      case 'report':
        return <ReportTab />
      case 'settings':
        return (
          <SettingsPanel
            autoPlay={autoPlay}
            onAutoPlayChange={setAutoPlay}
            fontSize={fontSize}
            onFontSizeChange={setFontSize}
            theme={theme}
            onThemeChange={setTheme}
            onReset={handleResetLocalSettings}
          />
        )
      default:
        return null
    }
  }

  return (
    <div className='flex min-h-screen w-full'>
      <SideBar
        activeMenu={activeMenu}
        onMenuChange={setActiveMenu}
        userInitial='U'
        onLogout={handleLogout}
      />

      <main className='flex-1 bg-white ml-64 min-w-0'>
        {renderContent()}
      </main>
    </div>
  )
}

export default MyPage
