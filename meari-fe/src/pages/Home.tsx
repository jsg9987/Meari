import { useState } from 'react'
import Header, { type HomeTab } from '../components/common/Header'
import ShadowingPanel from '../components/home/ShadowingPanel'
import CopikPanel from '../components/home/KopicPanel'
import DailyStudyPanel from '../components/home/DailyStudyPanel'

const Home = () => {
  const [activeTab, setActiveTab] = useState<HomeTab>('shadowing')

  const renderContent = () => {
    if (activeTab === 'shadowing') {
      return <ShadowingPanel />
    }
    if (activeTab === 'copik') {
      return <CopikPanel />
    }
    return <DailyStudyPanel />
  }

  return (
    <div className='min-h-screen w-full'>
      <header className='w-full relative z-60'>
        <Header activeTab={activeTab} onTabChange={setActiveTab} />
      </header>

      <main className='mx-auto w-full py-11'>
        {renderContent()}
      </main>
    </div>
  )
}

export default Home
