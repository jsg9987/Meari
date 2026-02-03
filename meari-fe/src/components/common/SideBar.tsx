import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { LayoutDashboard, User, FileText, Settings } from 'lucide-react'
import logoWhite from '../../assets/images/common/logo-white.svg'

export type SideBarMenu = 'dashboard' | 'profile' | 'report' | 'settings'

type SideBarProps = {
  activeMenu?: SideBarMenu
  onMenuChange?: (menu: SideBarMenu) => void
  userInitial?: string
}

const SideBar = ({ activeMenu = 'dashboard', onMenuChange, userInitial = 'U' }: SideBarProps) => {
  const [selectedMenu, setSelectedMenu] = useState<SideBarMenu>(activeMenu)
  const navigate = useNavigate()

  const handleMenuClick = (menuId: SideBarMenu) => {
    setSelectedMenu(menuId)
    onMenuChange?.(menuId)
  }

  const handleLogoClick = () => {
    navigate('/')
  }

  return (
    <div
      className='h-full w-34 flex flex-col items-center py-10'
      style={{ backgroundColor: 'var(--color-bg-root)' }}
    >
      {/* 로고 */}
      <div className='pt-2 pb-14'>
        <button
          type='button'
          onClick={handleLogoClick}
          className='cursor-pointer hover:opacity-80 transition-opacity'
          aria-label='메인으로 이동'
        >
          <img src={logoWhite} alt='Meari' className='h-4' />
        </button>
      </div>

      {/* 대시보드 */}
      <div className=''>
        <button
          type='button'
          onClick={() => handleMenuClick('dashboard')}
          className={`flex items-center justify-center w-16 h-16 rounded-xl transition-all ${
            selectedMenu === 'dashboard'
              ? 'text-white'
              : 'text-white/60 hover:text-white hover:bg-white/10'
          }`}
          style={selectedMenu === 'dashboard' ? { backgroundColor: 'var(--color-tab-active)' } : undefined}
          title='대시보드'
          aria-label='대시보드'
        >
          <LayoutDashboard size={26} />
        </button>
      </div>

      {/* 메뉴 */}
      <div className='pt-14'>
        <div className='flex flex-col items-center gap-8'>
          <button
            type='button'
            onClick={() => handleMenuClick('profile')}
            className={`flex items-center justify-center w-16 h-16 rounded-xl transition-all ${
              selectedMenu === 'profile'
                ? 'text-white'
                : 'text-white/60 hover:text-white hover:bg-white/10'
            }`}
            style={selectedMenu === 'profile' ? { backgroundColor: 'var(--color-tab-active)' } : undefined}
            title='프로필'
            aria-label='프로필'
          >
            <User size={26} />
          </button>

          <button
            type='button'
            onClick={() => handleMenuClick('report')}
            className={`flex items-center justify-center w-16 h-16 rounded-xl transition-all ${
              selectedMenu === 'report'
                ? 'text-white'
                : 'text-white/60 hover:text-white hover:bg-white/10'
            }`}
            style={selectedMenu === 'report' ? { backgroundColor: 'var(--color-tab-active)' } : undefined}
            title='리포트'
            aria-label='리포트'
          >
            <FileText size={26} />
          </button>

          <button
            type='button'
            onClick={() => handleMenuClick('settings')}
            className={`flex items-center justify-center w-16 h-16 rounded-xl transition-all ${
              selectedMenu === 'settings'
                ? 'text-white'
                : 'text-white/60 hover:text-white hover:bg-white/10'
            }`}
            style={selectedMenu === 'settings' ? { backgroundColor: 'var(--color-tab-active)' } : undefined}
            title='설정'
            aria-label='설정'
          >
            <Settings size={26} />
          </button>
        </div>
      </div>

      <div className='flex-1' />

      {/* 프로필 */}
      <div className='flex flex-col items-center'>
        <div className='flex items-center justify-center w-16 h-16 rounded-full bg-blue-500 text-white font-semibold text-xl'>
          {userInitial.charAt(0).toUpperCase()}
        </div>
      </div>
    </div>
  )
}

export default SideBar
