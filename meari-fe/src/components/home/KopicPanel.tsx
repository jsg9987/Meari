import { useState, useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { getThemes, type Theme } from '../../api/contents.api'
import ThemeConfirmModal from './ThemeConfirmModal'

// 설명 카드 이미지 (SVG)
import newsClickImg from '../../assets/images/copik/learning-preparation.svg'
import questionFlowImg from '../../assets/images/copik/question-speaking.svg'
import resultCheckImg from '../../assets/images/copik/result-check.svg'
import progressChartImg from '../../assets/images/copik/mypage-chart.svg'


// 설명 카드 데이터
const stepCards = [
  {
    id: 1,
    badge: '학습 준비',
    title: '테마 목록에서 원하는',
    subtitle: '테마를 선택하여 클릭',
    image: newsClickImg,
  },
  {
    id: 2,
    badge: '질문 및 발화',
    title: '질문을 듣고, 알맞는',
    subtitle: '발화를 진행하세요',
    image: questionFlowImg,
  },
  {
    id: 3,
    badge: '결과 확인',
    title: '결과 리포트가',
    subtitle: '출력되면 확인 하세요',
    image: resultCheckImg,
  },
  {
    id: 4,
    badge: '마이페이지',
    title: '마이페이지에서',
    subtitle: '학습 내역을 조회하세요',
    image: progressChartImg,
    isChart: true,
  },
]

// 테마 데이터는 API를 통해 동적으로 가져옵니다.

const KopicPanel = () => {
  const navigate = useNavigate()
  const [themes, setThemes] = useState<Theme[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [selectedTheme, setSelectedTheme] = useState<number | null>(null)
  const [isConfirmModalOpen, setIsConfirmModalOpen] = useState(false)
  const minLoadingTimeRef = useRef<number | null>(null)

  useEffect(() => {
    const loadThemes = async () => {
      try {
        setIsLoading(true)
        // 스켈레톤 최소 노출 시간 시작
        minLoadingTimeRef.current = Date.now()

        const response = await getThemes()

        if (response.data.success && response.data.data) {
          // 최소 300ms 보장
          const elapsedTime = Date.now() - minLoadingTimeRef.current
          const remainingTime = Math.max(0, 300 - elapsedTime)

          if (remainingTime > 0) {
            await new Promise(resolve => setTimeout(resolve, remainingTime))
          }

          setThemes(response.data.data)
        }
      } catch (error) {
        console.error('Failed to load themes:', error)
      } finally {
        setIsLoading(false)
        minLoadingTimeRef.current = null
      }
    }
    loadThemes()
  }, [])

  const handleStartEvaluation = () => {
    if (selectedTheme) {
      setIsConfirmModalOpen(true)
    } else {
      alert('테마를 선택해주세요.')
    }
  }

  const handleThemeClick = (themeId: number) => {
    setSelectedTheme(themeId)
  }

  const handleConfirmStart = () => {
    if (selectedTheme) {
      navigate(`/kopic/evaluation/${selectedTheme}`)
    }
    setIsConfirmModalOpen(false)
  }

  const handleCloseModal = () => {
    setIsConfirmModalOpen(false)
  }

  const currentThemeName = themes.find(t => t.theme_id === selectedTheme)?.name || ''

  return (
    <section className='px-8 py-6'>
      {/* 타이틀 및 시작 버튼 */}
      <div className='flex items-end justify-between mb-10'>
        <div>
          <h1 className='text-[1.65rem] font-bold text-gray-900 mb-2'>
            코픽 (K-OPIC)
          </h1>
          <p className='text-[16px] text-gray-600'>
            보다 정확한 검증을 통해 현재의 실력을 평가 받아보세요.
          </p>
        </div>
        <button
          type='button'
          onClick={handleStartEvaluation}
          className='px-9 py-2.5 bg-[#2D9CDB] text-white font-medium rounded-full hover:bg-[#2789c2] transition-colors cursor-pointer'
        >
          평가 시작하기
        </button>
      </div>

      {/* 설명 카드 영역 - 높이 고정 및 이미지 절대 배치 */}
      <div className='grid grid-cols-4 gap-4.5 mb-10'>
        {stepCards.map((card) => (
          <div
            key={card.id}
            className='bg-white rounded-lg p-2.25 shadow-sm border border-gray-100 relative overflow-hidden h-48'
          >
            {/* 배지 (시안 스타일: 어두운 배경 + 전구 아이콘) */}
            <div className='inline-flex items-center gap-1.5 mb-3 px-[9px] py-[4px] rounded bg-[#001C27] relative z-10'>
              <span className='text-[14px] font-bold text-[#38bdf8]'>
                {card.badge}
              </span>
              <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="#fbbf24" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                <path d="M12 2a6 6 0 0 1 6 6c0 2.97-1 3.5-2.5 4.5-.37.26-1.5.5-1.5 2.5a2 2 0 0 1-4 0c0-2-1.13-2.25-1.5-2.5A6 6 0 0 1 12 2z"></path>
                <path d="M9 18h6"></path>
                <path d="M10 22h4"></path>
              </svg>
            </div>

            {/* 텍스트 */}
            <div className='relative z-10 max-w-[60%]'>
              <p className='text-[14px] text-gray-500 leading-tight'>{card.title}</p>
              <p className='text-[14px] font-semibold text-gray-900 leading-tight'>{card.subtitle}</p>
            </div>

            {/* 이미지 영역 - 절대 배치로 우측 하단 고정 및 확대 */}
            <div className='absolute bottom-0 right-0'>
              <img
                src={card.image}
                alt={card.badge}
                className='object-contain transition-transform w-36 h-36 translate-x-4 translate-y-4'
              />
            </div>
          </div>
        ))}
      </div>

      {/* 테마 선택 영역 */}
      <div>
        <h2 className='text-xl font-bold text-gray-900 mb-[18px]'>테마 선택</h2>
        {isLoading ? (
          <div className='grid grid-cols-4 gap-[18px]'>
            {[1, 2, 3, 4].map((index) => (
              <div key={index} className='bg-white rounded-lg overflow-hidden shadow-md'>
                {/* 썸네일 스켈레톤 */}
                <div className='aspect-4/3 animate-shimmer' />
                {/* 정보 스켈레톤 */}
                <div className='px-3 py-4 space-y-3'>
                  <div className='h-[18px] animate-shimmer rounded' />
                  <div className='space-y-2'>
                    <div className='h-[13px] animate-shimmer rounded' />
                    <div className='h-[13px] animate-shimmer rounded w-4/5' />
                  </div>
                  <div className='h-[13px] animate-shimmer rounded w-3/5' />
                </div>
              </div>
            ))}
          </div>
        ) : themes.length === 0 ? (
          <div className='text-center py-20 text-gray-500'>테마 정보를 불러올 수 없습니다.</div>
        ) : (
          <div className='grid grid-cols-4 gap-[18px]'>
            {themes.map((theme) => (
              <div
                key={theme.theme_id}
                onClick={() => handleThemeClick(theme.theme_id)}
                className={`bg-white rounded-lg overflow-hidden cursor-pointer transition-all duration-300 hover:scale-[1.02] ${selectedTheme === theme.theme_id
                  ? 'shadow-[0_0_35px_rgba(45,156,219,0.8)] z-10 scale-[1.02]'
                  : 'shadow-md hover:shadow-xl'
                  }`}
              >
                {/* 썸네일 */}
                <div className='aspect-4/3 bg-gray-100'>
                  <img
                    src={theme.theme_url}
                    alt={theme.name}
                    className='w-full h-full object-cover'
                  />
                </div>

                {/* 정보 */}
                <div className='px-3 py-4'>
                  <h3 className='text-[18px] font-semibold text-gray-900 mb-4'>
                    {theme.name}
                  </h3>
                  <p className='text-[13px] text-gray-500 mb-4'>{theme.description}</p>
                  <p className='text-[13px] text-gray-400'>
                    2 minute · 16 section
                  </p>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* 확인 모달 */}
      <ThemeConfirmModal
        isOpen={isConfirmModalOpen}
        themeName={currentThemeName}
        onClose={handleCloseModal}
        onConfirm={handleConfirmStart}
      />
    </section>
  )
}

export default KopicPanel
