import { useState } from 'react'
import SideBar, { type SideBarMenu } from '../components/common/SideBar'

const MyPage = () => {
  const [activeMenu, setActiveMenu] = useState<SideBarMenu>('dashboard')

  const renderContent = () => {
    switch (activeMenu) {
      case 'dashboard':
        return (
          <div className='p-8'>
            <h1 className='text-3xl font-bold text-gray-900 mb-6'>대시보드</h1>
            <p className='text-gray-600'>대시보드 화면입니다.</p>
          </div>
        )
      case 'profile':
        return (
          <div className='p-8'>
            <h1 className='text-3xl font-bold text-gray-900 mb-6'>프로필</h1>
            <p className='text-gray-600'>프로필 정보를 확인하고 수정할 수 있습니다.</p>
          </div>
        )
      case 'report':
        return (
          <div className='p-8'>
            <h1 className='text-3xl font-bold text-gray-900 mb-6'>리포트</h1>
            <p className='text-gray-600'>학습 리포트를 확인할 수 있습니다.</p>
          </div>
        )
      case 'settings':
        return (
          <div className='p-8'>
            <h1 className='text-3xl font-bold text-gray-900 mb-6'>설정</h1>
            <p className='text-gray-600'>설정을 변경할 수 있습니다.</p>
          </div>
        )
      default:
        return null
    }
  }

  return (
    <div className='flex min-h-screen w-full'>
      <aside>
        <SideBar activeMenu={activeMenu} onMenuChange={setActiveMenu} userInitial='U' />
      </aside>

      <main className='flex-1 bg-white'>
        {renderContent()}
      </main>
    </div>
  )
}

export default MyPage
