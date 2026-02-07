import { useState, useEffect } from 'react'
import Header, { type HomeTab } from '../components/common/Header'
import ShadowingPanel from '../components/home/ShadowingPanel'
import CopikPanel from '../components/home/KopicPanel'
import DailyStudyPanel from '../components/home/DailyStudyPanel'
import ThemeSidebar from '../components/home/ThemeSidebar'
import { getThemes, type Theme } from '../api/contents.api'

const Home = () => {
  const [activeTab, setActiveTab] = useState<HomeTab>('shadowing')
  const [themes, setThemes] = useState<Theme[]>([])
  const [selectedTheme, setSelectedTheme] = useState<string>('전체')

  // 테마 로드
  useEffect(() => {
    const loadThemes = async () => {
      try {
        const response = await getThemes()
        if (response.data.success && response.data.data) {
          setThemes(response.data.data)
        }
      } catch (error) {
        console.error('Failed to load themes:', error)
      }
    }
    loadThemes()
  }, [])

  const renderContent = () => {
    if (activeTab === 'shadowing') {
      return <ShadowingPanel selectedTheme={selectedTheme} />
    }
    if (activeTab === 'copik') {
      return <CopikPanel />
    }
    return <DailyStudyPanel />
  }

  return (
    <div className='min-h-screen w-full bg-gray-50'>
      <header className='w-full relative z-60'>
        <Header />
      </header>

      <main className='mx-auto w-full relative'>
        {/* 왼쪽 사이드바 - 고정 */}
        <div className='fixed left-0 top-(--header-height) h-[calc(100vh-var(--header-height))] overflow-y-auto z-50 bg-white'>
          <ThemeSidebar
            themes={themes}
            selectedTheme={selectedTheme}
            onThemeSelect={setSelectedTheme}
            activeTab={activeTab}
            onTabChange={setActiveTab}
          />
        </div>

        {/* 오른쪽 콘텐츠 - 전환 애니메이션 */}
        <div className='ml-64 transition-opacity duration-300'>
          {renderContent()}
        </div>
      </main>
    </div>
  )
}

export default Home
