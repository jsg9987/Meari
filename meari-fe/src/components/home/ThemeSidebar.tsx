import { useState } from 'react'
import { Mic, FileText, Calendar, ChevronDown, LayoutGrid } from 'lucide-react'
import ThemeIcon from './ThemeIcon'
import type { Theme } from '../../api/contents.api'
import type { HomeTab } from '../common/Header'


type ThemeSidebarProps = {
  themes: Theme[]
  selectedTheme: string
  onThemeSelect: (themeName: string) => void
  activeTab: HomeTab
  onTabChange: (tab: HomeTab) => void
}

const tabs = [
  { id: 'shadowing', label: '쉐도잉', icon: Mic },
  { id: 'copik', label: '코픽', icon: FileText },
  { id: 'daily', label: '일일 학습', icon: Calendar },
] as const

const ThemeSidebar = ({ themes, selectedTheme, onThemeSelect, activeTab, onTabChange }: ThemeSidebarProps) => {
  const [isShadowingOpen, setIsShadowingOpen] = useState(true)

  const handleTabClick = (tabId: HomeTab) => {
    if (tabId === 'shadowing') {
      // 쉐도잉 클릭 시: 이미 쉐도잉이 활성화되어 있으면 토글, 아니면 열기
      if (activeTab === 'shadowing') {
        setIsShadowingOpen(!isShadowingOpen)
      } else {
        setIsShadowingOpen(true)
        onTabChange(tabId)
      }
    } else {
      // 다른 탭 클릭 시: 쉐도잉 메뉴 닫기
      setIsShadowingOpen(false)
      onTabChange(tabId)
    }
  }

  return (
    <div className='w-64 h-full flex flex-col gap-2 py-6 px-4 bg-white border-r border-gray-200'>
      {/* 탭 메뉴 */}
      {tabs.map((tab) => {
        const Icon = tab.icon
        const isActive = tab.id === activeTab
        const isShadowing = tab.id === 'shadowing'

        return (
          <div key={tab.id}>
            <button
              type='button'
              onClick={() => handleTabClick(tab.id)}
              className={`flex items-center justify-between w-full gap-3 px-5 py-3 rounded-lg transition-all ${
                isActive
                  ? 'bg-(--color-bg-button) text-white'
                  : 'text-gray-700 hover:bg-gray-50'
              }`}
            >
              <div className='flex items-center gap-3'>
                <Icon size={20} className='shrink-0' />
                <span className='text-base font-semibold'>{tab.label}</span>
              </div>
              {isShadowing && (
                <ChevronDown
                  size={18}
                  className={`transition-transform ${isShadowingOpen && isActive ? 'rotate-180' : ''}`}
                />
              )}
            </button>

            {/* 쉐도잉 하위 테마들 */}
            {isShadowing && isActive && (
              <div
                className={`overflow-hidden transition-all duration-300 ease-in-out ${
                  isShadowingOpen ? 'max-h-125 opacity-100 mt-2' : 'max-h-0 opacity-0 mt-0'
                }`}
              >
                <div className='ml-4 flex flex-col gap-1'>
                  {/* 전체 버튼 */}
                  <button
                    type='button'
                    onClick={() => onThemeSelect('전체')}
                    className={`flex items-center gap-3 px-4 py-2.5 rounded-lg transition-all ${
                      selectedTheme === '전체'
                        ? 'bg-(--color-bg-button) text-white'
                        : 'text-gray-700 hover:bg-gray-50'
                    }`}
                  >
                    <LayoutGrid className='w-4.5 h-4.5 shrink-0' />
                    <span className='text-sm font-medium'>전체</span>
                  </button>

                  {/* 테마 목록 */}
                  {themes.map((theme) => (
                    <button
                      key={theme.theme_id}
                      type='button'
                      onClick={() => onThemeSelect(theme.name)}
                      className={`flex items-center gap-3 px-4 py-2.5 rounded-lg transition-all ${
                        selectedTheme === theme.name
                          ? 'bg-(--color-bg-button) text-white'
                          : 'text-gray-700 hover:bg-gray-50'
                      }`}
                    >
                      <ThemeIcon themeName={theme.name} className='w-4.5 h-4.5 shrink-0' />
                      <span className='text-sm font-medium'>{theme.name}</span>
                    </button>
                  ))}
                </div>
              </div>
            )}
          </div>
        )
      })}
    </div>
  )
}

export default ThemeSidebar
