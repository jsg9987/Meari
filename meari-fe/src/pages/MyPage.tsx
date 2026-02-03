import { useState } from 'react'
import SideBar, { type SideBarMenu } from '../components/common/SideBar'
import Dashboard from '../components/mypage/Dashboard'

const MyPage = () => {
  const [activeMenu, setActiveMenu] = useState<SideBarMenu>('dashboard')

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
            <p className='text-gray-600'>Manage account and notification settings.</p>
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
