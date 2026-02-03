import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { LayoutDashboard, User, FileText, Settings, LogOut } from 'lucide-react'
import logoWhite from '../../assets/images/common/logo-white.svg'

export type SideBarMenu = 'dashboard' | 'profile' | 'report' | 'settings'

type SideBarProps = {
  activeMenu?: SideBarMenu
  onMenuChange?: (menu: SideBarMenu) => void
  userInitial?: string
  userName?: string
  userEmail?: string
  onLogout?: () => void
}

const topMenuItem = { id: 'dashboard' as SideBarMenu, icon: LayoutDashboard, label: '대시보드' }

const menuItems = [
  { id: 'profile' as SideBarMenu, icon: User, label: '프로필' },
  { id: 'settings' as SideBarMenu, icon: Settings, label: '설정' },
  { id: 'report' as SideBarMenu, icon: FileText, label: '리포트' }
]

const SideBar = ({
  activeMenu = 'dashboard',
  onMenuChange,
  userInitial = 'U',
  userName = 'User',
  userEmail = 'user@example.com',
  onLogout
}: SideBarProps) => {
  const [selectedMenu, setSelectedMenu] = useState<SideBarMenu>(activeMenu)
  const navigate = useNavigate()

  const handleMenuClick = (menuId: SideBarMenu) => {
    setSelectedMenu(menuId)
    onMenuChange?.(menuId)
  }

  const handleLogoClick = () => {
    navigate('/')
  }

  const handleLogout = () => {
    onLogout?.()
  }

  return (
    <div
      className='h-screen w-64 flex flex-col py-6 px-4 fixed left-0 top-0'
      style={{ backgroundColor: 'var(--color-bg-root)' }}
    >
      {/* 로고 */}
      <button
        type='button'
        onClick={handleLogoClick}
        className='pt-8 pb-12 cursor-pointer hover:opacity-80 transition-opacity'
        aria-label='메인으로 이동'
      >
        <img src={logoWhite} alt='Meari' className='h-5' />
      </button>

      {/* 대시보드 메뉴 */}
      <div className='flex flex-col gap-2 mb-6'>
        {(() => {
          const Icon = topMenuItem.icon
          const isActive = selectedMenu === topMenuItem.id

          return (
            <button
              key={topMenuItem.id}
              type='button'
              onClick={() => handleMenuClick(topMenuItem.id)}
              className={`flex items-center gap-3 px-4 py-3 rounded-xl transition-all ${
                isActive ? 'text-white' : 'text-white/60 hover:text-white hover:bg-white/10'
              }`}
              style={isActive ? { backgroundColor: 'var(--color-tab-active)' } : undefined}
              title={topMenuItem.label}
              aria-label={topMenuItem.label}
            >
              <Icon size={22} />
              <span className='text-sm font-medium'>{topMenuItem.label}</span>
            </button>
          )
        })()}
      </div>

      {/* 바뀌 메뉴 */}
      <div className='flex flex-col gap-2'>
        {menuItems.map((item) => {
          const Icon = item.icon
          const isActive = selectedMenu === item.id

          return (
            <button
              key={item.id}
              type='button'
              onClick={() => handleMenuClick(item.id)}
              className={`flex items-center gap-3 px-4 py-3 rounded-xl transition-all ${
                isActive ? 'text-white' : 'text-white/60 hover:text-white hover:bg-white/10'
              }`}
              style={isActive ? { backgroundColor: 'var(--color-tab-active)' } : undefined}
              title={item.label}
              aria-label={item.label}
            >
              <Icon size={22} />
              <span className='text-sm font-medium'>{item.label}</span>
            </button>
          )
        })}
      </div>

      {/* 로그아웃 및 프로필 */}
      <div className='mt-auto flex flex-col gap-2'>
        <button
          type='button'
          onClick={handleLogout}
          className='flex items-center gap-3 px-4 py-3 rounded-xl text-white/60 hover:text-white hover:bg-white/10 transition-all'
          aria-label='로그아웃'
        >
          <LogOut size={22} />
          <span className='text-sm font-medium'>로그아웃</span>
        </button>

        <div className='flex items-center gap-3 px-4 py-3 bg-white/10 rounded-xl'>
          <div className='flex items-center justify-center w-10 h-10 rounded-full bg-blue-500 text-white font-semibold text-sm shrink-0'>
            {userInitial.charAt(0).toUpperCase()}
          </div>
          <div className='flex flex-col overflow-hidden'>
            <div className='text-white text-sm font-medium truncate'>{userName}</div>
            <div className='text-white/60 text-xs truncate'>{userEmail}</div>
          </div>
        </div>
      </div>
    </div>
  )
}

export default SideBar
