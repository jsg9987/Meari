import { useState } from 'react'
import { useNavigate } from 'react-router-dom'

// 설명 카드 이미지 (SVG)
import newsClickImg from '../../assets/images/copik/learning-preparation.svg'
import questionFlowImg from '../../assets/images/copik/question-speaking.svg'
import resultCheckImg from '../../assets/images/copik/result-check.svg'
import progressChartImg from '../../assets/images/copik/mypage-chart.svg'

// 테마 더미 썸네일
const dummyThemeImg = 'https://images.unsplash.com/photo-1557804506-669a67965ba0?w=400&h=300&fit=crop'

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

// 테마 데이터 (더미)
const themes = [
  { id: 1, name: '생활', description: '테마 설명', duration: '2 minute', sections: 16, thumbnail: dummyThemeImg },
  { id: 2, name: '생활', description: '테마 설명', duration: '2 minute', sections: 16, thumbnail: dummyThemeImg },
  { id: 3, name: '생활', description: '테마 설명', duration: '2 minute', sections: 16, thumbnail: dummyThemeImg },
  { id: 4, name: '생활', description: '테마 설명', duration: '2 minute', sections: 16, thumbnail: dummyThemeImg },
]

const KopicPanel = () => {
  const navigate = useNavigate()
  const [selectedTheme, setSelectedTheme] = useState<number | null>(null)

  const handleStartEvaluation = () => {
    if (selectedTheme) {
      navigate(`/kopic/evaluation/${selectedTheme}`)
    } else {
      alert('테마를 선택해주세요.')
    }
  }

  return (
    <section>
      {/* 타이틀 및 시작 버튼 */}
      <div className='flex items-start justify-between mb-8'>
        <div>
          <h1 className='text-2xl font-bold text-gray-900 mb-2'>
            코픽 (K-OPIC)
          </h1>
          <p className='text-sm text-gray-600'>
            보다 정확한 검증을 통해 현재의 실력을 평가 받아보세요.
          </p>
        </div>
        <button
          type='button'
          onClick={handleStartEvaluation}
          className='px-8 py-3 bg-[#2D9CDB] text-white font-medium rounded-lg hover:bg-[#2789c2] transition-colors cursor-pointer'
        >
          평가 시작하기
        </button>
      </div>

      {/* 설명 카드 영역 - 높이 고정 및 이미지 절대 배치 */}
      <div className='grid grid-cols-4 gap-4 mb-9'>
        {stepCards.map((card) => (
          <div
            key={card.id}
            className='bg-white rounded-lg p-2 shadow-sm border border-gray-100 relative overflow-hidden h-40'
          >
            {/* 배지 (시안 스타일: 어두운 배경 + 전구 아이콘) */}
            <div className='inline-flex items-center gap-1.5 mb-3 px-2 py-1 rounded bg-[#001C27] relative z-10'>
              <span className='text-[14px] font-bold text-[#38bdf8]'>
                {card.badge}
              </span>
              <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="#fbbf24" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                <path d="M12 2a6 6 0 0 1 6 6c0 2.97-1 3.5-2.5 4.5-.37.26-1.5.5-1.5 2.5a2 2 0 0 1-4 0c0-2-1.13-2.25-1.5-2.5A6 6 0 0 1 12 2z"></path>
                <path d="M9 18h6"></path>
                <path d="M10 22h4"></path>
              </svg>
            </div>

            {/* 텍스트 */}
            <div className='relative z-10 max-w-[60%]'>
              <p className='text-[15px] text-gray-500 leading-tight'>{card.title}</p>
              <p className='text-[15px] font-semibold text-gray-900 leading-tight'>{card.subtitle}</p>
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
        <h2 className='text-lg font-bold text-gray-900 mb-4'>테마 선택</h2>
        <div className='grid grid-cols-4 gap-4'>
          {themes.map((theme) => (
            <div
              key={theme.id}
              onClick={() => setSelectedTheme(theme.id)}
              className={`bg-white rounded-lg overflow-hidden shadow-md cursor-pointer transition-all duration-300 hover:shadow-xl hover:scale-[1.02] ${selectedTheme === theme.id
                ? 'ring-2 ring-[#2D9CDB] ring-offset-2'
                : ''
                }`}
            >
              {/* 썸네일 */}
              <div className='aspect-square bg-gray-100'>
                <img
                  src={theme.thumbnail}
                  alt={theme.name}
                  className='w-full h-full object-cover'
                />
              </div>

              {/* 정보 */}
              <div className='p-4'>
                <h3 className='text-base font-semibold text-gray-900 mb-1'>
                  {theme.name}
                </h3>
                <p className='text-sm text-gray-500 mb-2'>{theme.description}</p>
                <p className='text-xs text-gray-400'>
                  {theme.duration} · {theme.sections} section
                </p>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}

export default KopicPanel
