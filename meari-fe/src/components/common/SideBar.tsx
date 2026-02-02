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

const menuItems = [
  { id: 'dashboard' as SideBarMenu, icon: LayoutDashboard, label: '대시보드' },
  { id: 'profile' as SideBarMenu, icon: User, label: '프로필' },
  { id: 'report' as SideBarMenu, icon: FileText, label: '리포트' },
  { id: 'settings' as SideBarMenu, icon: Settings, label: '설정' }
]

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
      className='h-screen w-64 flex flex-col py-6 px-4 fixed left-0 top-0'
      style={{ backgroundColor: 'var(--color-bg-root)' }}
    >
      {/* 로고 */}
      <button
        type='button'
        onClick={handleLogoClick}
        className='pt-8 pb-12 cursor-pointer hover:opacity-80 transition-opacity'
        aria-label='홈으로 이동'
      >
        <img src={logoWhite} alt='Meari' className='h-5' />
      </button>

      {/* 메뉴 그룹 */}
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
                isActive
                  ? 'text-white'
                  : 'text-white/60 hover:text-white hover:bg-white/10'
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

      {/* 프로필 이미지 */}
      <div className='mt-auto'>
        <div className='flex items-center gap-3 px-4 py-3 bg-white/10 rounded-xl'>
          <div className='flex items-center justify-center w-10 h-10 rounded-full bg-blue-500 text-white font-semibold text-sm'>
            {userInitial.charAt(0).toUpperCase()}
          </div>
          <div className='text-white text-sm font-medium'>
            {userInitial}
          </div>
        </div>
      </div>
    </div>
  )
}

export default SideBar
