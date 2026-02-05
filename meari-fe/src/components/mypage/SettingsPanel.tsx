type SettingsPanelProps = {
  autoPlay: boolean
  onAutoPlayChange: (value: boolean) => void
  fontSize: 'small' | 'medium' | 'large'
  onFontSizeChange: (value: 'small' | 'medium' | 'large') => void
  theme: 'light' | 'dark'
  onThemeChange: (value: 'light' | 'dark') => void
  onReset: () => void
}

const SettingsPanel = ({
  autoPlay,
  onAutoPlayChange,
  fontSize,
  onFontSizeChange,
  theme,
  onThemeChange,
  onReset
}: SettingsPanelProps) => (
  <div className='p-8'>
    <h1 className='text-3xl font-bold text-gray-900 mb-6'>Settings</h1>
    <div className='space-y-6 text-gray-800'>
      <div>
        <label className='flex items-center gap-2'>
          <input
            type='checkbox'
            checked={autoPlay}
            onChange={(event) => onAutoPlayChange(event.target.checked)}
          />
          {'자동 재생'}
        </label>
      </div>

      <div>
        <label className='block mb-2'>{'글자 크기'}</label>
        <select
          value={fontSize}
          onChange={(event) => onFontSizeChange(event.target.value as 'small' | 'medium' | 'large')}
        >
          <option value='small'>{'작게'}</option>
          <option value='medium'>{'보통'}</option>
          <option value='large'>{'크게'}</option>
        </select>
      </div>

      <div>
        <label className='block mb-2'>{'테마'}</label>
        <select value={theme} onChange={(event) => onThemeChange(event.target.value as 'light' | 'dark')}>
          <option value='light'>{'라이트'}</option>
          <option value='dark'>{'다크'}</option>
        </select>
      </div>

      <div>
        <button type='button' onClick={onReset}>
          {'캐시/데이터 초기화'}
        </button>
      </div>
    </div>
  </div>
)

export default SettingsPanel
