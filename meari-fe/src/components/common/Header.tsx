export type HomeTab = 'shadowing' | 'copik' | 'daily'

type HeaderProps = {
  activeTab: HomeTab
  onTabChange: (tab: HomeTab) => void
}

const tabs = [
  { id: 'shadowing', label: '쉐도잉' },
  { id: 'copik', label: '코픽' },
  { id: 'daily', label: '일일 학습' },
] as const

const Header = ({ activeTab, onTabChange }: HeaderProps) => {
  const activeIndex = tabs.findIndex((tab) => tab.id === activeTab)

  return (
    <div className='w-full h-[var(--header-height)] bg-[var(--color-bg-root)]'>
      <div className='mx-auto grid h-full w-full max-w-[1200px] grid-cols-[1fr_auto_1fr] items-center px-6'>
        <div className='flex items-center gap-3'>
          <span className='text-lg font-semibold tracking-[0.25em] text-white'>Meari</span>
        </div>

        <nav className='flex items-center justify-center' aria-label='Main'>
          <div className='relative w-full max-w-[420px] rounded-full border border-white/15 bg-white/5'>
            <div
              className='absolute inset-0 w-1/3 rounded-full bg-white/90 transition-transform duration-300 ease-out'
              style={{ transform: `translateX(${activeIndex * 100}%)` }}
              aria-hidden
            />
            <div className='relative z-10 grid grid-cols-3 text-sm font-medium text-white/80' role='tablist'>
              {tabs.map((tab) => (
                <button
                  key={tab.id}
                  type='button'
                  role='tab'
                  aria-selected={tab.id === activeTab}
                  className={`py-2 transition-colors ${
                    tab.id === activeTab ? 'text-slate-900' : 'text-white/80'
                  }`}
                  onClick={() => onTabChange(tab.id)}
                >
                  {tab.label}
                </button>
              ))}
            </div>
          </div>
        </nav>

        <div className='flex items-center justify-end gap-3 text-sm text-white/80'>
          <button type='button' className='rounded-full border border-white/20 px-3 py-1.5'>
            한국어
          </button>
          <button type='button' className='rounded-full border border-white/20 px-3 py-1.5'>
            설정
          </button>
          <button type='button' className='rounded-full border border-white/20 px-3 py-1.5'>
            프로필
          </button>
        </div>
      </div>
    </div>
  )
}

export default Header
