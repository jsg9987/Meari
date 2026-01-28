import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import illustratorCard from '../../assets/images/daily/Illustrator-card.svg'
import illustratorCard1 from '../../assets/images/daily/Illustrator-card-1.svg'

// 카드 데이터
const CARDS = [
  {
    id: 1,
    title: '일일 단어 학습',
    subtitle: '10개로 구성된 학습을 진행하세요.',
    badge: '10 quizes',
    themeColor: 'white',
    image: illustratorCard,
    path: '/study/word',
    isLocked: false
  },
  {
    id: 2,
    title: '문장 순서 맞추기',
    subtitle: '랜덤으로 배치 된 문장의 순서를 맞춰보세요.',
    badge: '10 section',
    themeColor: 'gradient-teal',
    image: illustratorCard1,
    path: '/study/sentence',
    isLocked: false
  },
  {
    id: 3,
    title: 'Coming Soon',
    subtitle: '추후 컨텐츠 추가 예정',
    badge: '20 section',
    themeColor: 'dark',
    image: illustratorCard1,
    path: '#',
    isLocked: true
  }
]

// 월 이름
const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']

const DailyStudyPanel = () => {
  const navigate = useNavigate()
  const [activeCard, setActiveCard] = useState<number>(0)

  const handleCardClick = (index: number) => {
    setActiveCard(index)
  }

  const handleStart = (path: string, locked: boolean, e: React.MouseEvent) => {
    e.stopPropagation()
    if (!locked) navigate(path)
  }

  // 카드 스타일 결정
  const getCardStyle = (card: typeof CARDS[0], isActive: boolean) => {
    if (isActive) {
      return 'bg-white border border-gray-100 shadow-sm'
    }
    if (card.themeColor === 'white') {
      return 'bg-[#001C27] text-white shadow-sm'
    }
    if (card.themeColor === 'gradient-teal') {
      return 'bg-gradient-to-br from-[#56CCF2] to-[#2F80ED] text-white shadow-sm'
    }
    if (card.themeColor === 'dark') {
      return 'bg-[#4A4A4A] text-white shadow-sm'
    }
    return 'bg-white border border-gray-100 text-gray-900 shadow-sm'
  }

  return (
    <section>
      {/* 타이틀 */}
      <div className='flex items-end justify-between mb-10'>
        <div>
          <h1 className='text-[1.65rem] font-bold text-gray-900 mb-2'>
            일일 학습 (Daily)
          </h1>
          <p className='text-base text-gray-600'>
            매일 10분, 필수 문장 5개를 마스터하여 자연스러운 한국어 습관을 만드세요.
          </p>
        </div>
      </div>

      {/* Grass Grid Section */}
      <div className="bg-white rounded-lg p-2 mb-4.5 border border-gray-100 shadow-sm">
        {/* 월 레이블 */}
        <div className="flex mb-2 pl-8">
          {MONTHS.map((month) => (
            <div key={month} className="flex-1 text-xs text-gray-400 font-medium">
              {month}
            </div>
          ))}
        </div>

        {/* 잔디 그리드 (구획만 설정, 비어있음) */}
        <div className="flex gap-1 mb-2">
          {/* 요일 레이블 */}
          <div className="flex flex-col justify-around text-[10px] text-gray-400 pr-2">
            <span>M</span>
            <span>W</span>
            <span>F</span>
            <span>S</span>
          </div>

          {/* 빈 잔디 그리드 영역 */}
          <div className="flex-1 h-[150px] bg-gray-50 rounded-lg border border-dashed border-gray-200 flex items-center justify-center">
            <span className="text-gray-300 text-sm">잔디 영역</span>
          </div>
        </div>

        {/* 토글 및 범례 */}
        <div className="flex justify-between items-center px-2 pl-6">
          <div className="flex gap-2">
            <button className="px-2 py-1 text-xs rounded-full bg-gray-100 text-gray-600">month</button>
            <button className="px-2 py-1 text-xs rounded-full text-gray-400">date</button>
          </div>
          <div className="flex items-center gap-1 text-[10px] text-gray-400">
            <span>less</span>
            <div className="size-3 rounded-sm bg-[#E8F4FD]"></div>
            <div className="size-3 rounded-sm bg-[#B3D9F7]"></div>
            <div className="size-3 rounded-sm bg-[#6BB8F0]"></div>
            <div className="size-3 rounded-sm bg-[#2F80ED]"></div>
            <span>more</span>
          </div>
        </div>
      </div>

      {/* 컨텐츠 선택 */}
      <div>
        <h2 className='text-xl font-bold text-gray-900 mb-[18px]'>컨텐츠 선택</h2>
      </div>

      {/* 카드 그리드 */}
      <div className="flex gap-2">
        {CARDS.map((card, index) => {
          const isActive = activeCard === index
          return (
            <div
              key={card.id}
              onClick={() => handleCardClick(index)}
              className={`
                relative rounded-2xl overflow-hidden cursor-pointer
                transition-all duration-500 ease-out min-h-[347px]
                ${getCardStyle(card, isActive)}
                ${isActive ? 'flex-2' : 'flex-1'}
              `}
            >
              {/* 활성 카드 (확장된 상태) */}
              {isActive ? (
                <div className="p-5 h-full flex flex-col">
                  {/* 아이콘 & 배지 */}
                  <div className="flex justify-between items-start mb-8">
                    <div className="w-10 h-10 bg-[#56CCF2] rounded-full flex items-center justify-center">
                      <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
                        <path d="M9 12L11 14L15 10M21 12C21 16.9706 16.9706 21 12 21C7.02944 21 3 16.9706 3 12C3 7.02944 7.02944 3 12 3C16.9706 3 21 7.02944 21 12Z" stroke="white" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                      </svg>
                    </div>
                    <span className="text-sm text-gray-400 font-medium">{card.badge}</span>
                  </div>

                  {/* 서브타이틀 */}
                  <p className="text-gray-400 text-sm mb-2">{card.subtitle}</p>

                  {/* 타이틀 */}
                  <h3 className="text-2xl font-bold text-[#1A1A1A] mb-10">{card.title}</h3>

                  {/* 시작 버튼 */}
                  <button
                    onClick={(e) => handleStart(card.path, card.isLocked, e)}
                    className="w-fit px-8 py-3 bg-[#56CCF2] text-white font-semibold rounded-xl
                               hover:bg-[#4AB8DD] transition-colors"
                  >
                    단어 학습 시작하기
                  </button>

                  {/* 유저 정보 */}
                  <div className="flex items-center gap-2 mt-auto">
                    <div className="w-6 h-6 bg-gray-300 rounded-full flex items-center justify-center">
                      <svg width="12" height="12" viewBox="0 0 12 12" fill="none">
                        <path d="M6 6C7.38071 6 8.5 4.88071 8.5 3.5C8.5 2.11929 7.38071 1 6 1C4.61929 1 3.5 2.11929 3.5 3.5C3.5 4.88071 4.61929 6 6 6Z" fill="#666" />
                        <path d="M6 7C3.79086 7 2 8.79086 2 11H10C10 8.79086 8.20914 7 6 7Z" fill="#666" />
                      </svg>
                    </div>
                    <span className="text-gray-500 text-sm">김선엽</span>
                  </div>

                  {/* 일러스트 이미지 */}
                  <img
                    src={card.image}
                    alt="illustration"
                    className="absolute right-3 bottom-3 w-[100px] pointer-events-none"
                  />
                </div>
              ) : (
                /* 비활성 카드 (축소된 상태) */
                <div className="p-5 h-full flex flex-col">
                  {/* 아이콘 & 배지 */}
                  <div className="flex justify-between items-start mb-8">
                    <div className={`w-10 h-10 ${card.themeColor === 'dark' ? 'bg-white/20' : 'bg-white/30'} rounded-full flex items-center justify-center backdrop-blur-sm`}>
                      <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
                        <path d="M9 12L11 14L15 10M21 12C21 16.9706 16.9706 21 12 21C7.02944 21 3 16.9706 3 12C3 7.02944 7.02944 3 12 3C16.9706 3 21 7.02944 21 12Z" stroke="white" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                      </svg>
                    </div>
                    <span className="text-sm font-medium opacity-90">{card.badge}</span>
                  </div>

                  {/* 서브타이틀 */}
                  <p className="text-sm opacity-80 mb-2">{card.subtitle}</p>

                  {/* 타이틀 */}
                  <h3 className="text-xl font-bold mb-auto">{card.title}</h3>

                  {/* 유저 정보 */}
                  <div className="flex items-center gap-2 mt-4">
                    <div className={`w-6 h-6 ${card.themeColor === 'dark' ? 'bg-white/20' : 'bg-white/40'} rounded-full flex items-center justify-center`}>
                      <svg width="12" height="12" viewBox="0 0 12 12" fill="none">
                        <path d="M6 6C7.38071 6 8.5 4.88071 8.5 3.5C8.5 2.11929 7.38071 1 6 1C4.61929 1 3.5 2.11929 3.5 3.5C3.5 4.88071 4.61929 6 6 6Z" fill="white" />
                        <path d="M6 7C3.79086 7 2 8.79086 2 11H10C10 8.79086 8.20914 7 6 7Z" fill="white" />
                      </svg>
                    </div>
                    <span className="text-sm opacity-90">김선엽</span>
                  </div>

                  {/* 일러스트 이미지 */}
                  <img
                    src={card.image}
                    alt="illustration"
                    className="absolute right-3 bottom-3 w-[100px] pointer-events-none opacity-90"
                  />
                </div>
              )}
            </div>
          )
        })}
      </div>
    </section>
  )
}

export default DailyStudyPanel
