import ThemeIcon from './ThemeIcon'
import type { Theme } from '../../api/contents.api'

type ThemeSidebarProps = {
  themes: Theme[]
  selectedTheme: string
  onThemeSelect: (themeName: string) => void
}

const ThemeSidebar = ({ themes, selectedTheme, onThemeSelect }: ThemeSidebarProps) => {
  return (
    <div className='w-64 h-full flex flex-col gap-2 py-6 px-4 bg-white'>
      {/* 전체 버튼 */}
      <button
        type='button'
        onClick={() => onThemeSelect('전체')}
        className={`flex items-center gap-4 px-5 py-3 rounded-lg transition-all ${
          selectedTheme === '전체'
            ? 'bg-[oklch(0.63_0.12_232)] text-white'
            : 'text-gray-700 hover:bg-gray-50'
        }`}
      >
        <svg className='w-6 h-6 flex-shrink-0' viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <rect x="3" y="3" width="7" height="7"></rect>
          <rect x="14" y="3" width="7" height="7"></rect>
          <rect x="14" y="14" width="7" height="7"></rect>
          <rect x="3" y="14" width="7" height="7"></rect>
        </svg>
        <span className='text-base font-semibold'>전체</span>
      </button>

      {/* 구분선 */}
      <div className='my-2 border-t border-gray-200'></div>

      {/* 테마 목록 */}
      {themes.map((theme) => (
        <button
          key={theme.theme_id}
          type='button'
          onClick={() => onThemeSelect(theme.name)}
          className={`flex items-center gap-4 px-5 py-3 rounded-lg transition-all ${
            selectedTheme === theme.name
              ? 'bg-[oklch(0.63_0.12_232)] text-white'
              : 'text-gray-700 hover:bg-gray-50'
          }`}
        >
          <ThemeIcon themeName={theme.name} className='w-6 h-6 flex-shrink-0' />
          <span className='text-base font-semibold'>{theme.name}</span>
        </button>
      ))}
    </div>
  )
}

export default ThemeSidebar
