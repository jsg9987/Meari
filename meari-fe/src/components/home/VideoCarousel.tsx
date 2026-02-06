import { useState, useEffect, useCallback } from 'react'

// 목업 비디오 데이터
const MOCK_VIDEOS = [
  {
    id: 1,
    title: '그래서 쪼끔은 후회해?',
    description: '한국어 일상 대화 쉐도잉 연습',
    thumbnail: 'https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=800',
    duration: '2:47',
    themeName: '일상회화',
  },
  {
    id: 2,
    title: '식당에서 예약하기',
    description: '전화로 식당 예약하는 대화 연습',
    thumbnail: 'https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=800',
    duration: '4:00',
    themeName: '일상회화',
  },
  {
    id: 3,
    title: '메뉴 추천 받기',
    description: '식당에서 직원에게 메뉴 추천을 받는 상황',
    thumbnail: 'https://images.unsplash.com/photo-1559339352-11d035aa65de?w=800',
    duration: '3:20',
    themeName: '여행',
  },
  {
    id: 4,
    title: '회의 시작하기',
    description: '비즈니스 미팅을 시작하는 인사말과 소개',
    thumbnail: 'https://images.unsplash.com/photo-1556761175-5973dc0f32e7?w=800',
    duration: '5:00',
    themeName: '비즈니스',
  },
  {
    id: 5,
    title: '호텔 체크인',
    description: '호텔에서 체크인하는 대화',
    thumbnail: 'https://images.unsplash.com/photo-1566073771259-6a8506099945?w=800',
    duration: '3:40',
    themeName: '여행',
  },
]

type VideoCarouselProps = {
  onVideoSelect?: (videoId: number) => void
}

const VideoCarousel = ({ onVideoSelect }: VideoCarouselProps) => {
  const [currentIndex, setCurrentIndex] = useState(0)
  const [isAutoPlaying, setIsAutoPlaying] = useState(true)

  const nextSlide = useCallback(() => {
    setCurrentIndex((prevIndex) => (prevIndex + 1) % MOCK_VIDEOS.length)
  }, [])

  const prevSlide = () => {
    setCurrentIndex((prevIndex) => (prevIndex - 1 + MOCK_VIDEOS.length) % MOCK_VIDEOS.length)
  }

  const goToSlide = (index: number) => {
    setCurrentIndex(index)
    setIsAutoPlaying(false)
  }

  const handleVideoClick = (videoId: number) => {
    onVideoSelect?.(videoId)
  }

  // 자동 슬라이드
  useEffect(() => {
    if (!isAutoPlaying) return

    const interval = setInterval(() => {
      nextSlide()
    }, 5000)

    return () => clearInterval(interval)
  }, [isAutoPlaying, nextSlide])

  const getVisibleVideos = () => {
    const prev = (currentIndex - 1 + MOCK_VIDEOS.length) % MOCK_VIDEOS.length
    const next = (currentIndex + 1) % MOCK_VIDEOS.length
    return [
      { video: MOCK_VIDEOS[prev], position: 'left' as const },
      { video: MOCK_VIDEOS[currentIndex], position: 'center' as const },
      { video: MOCK_VIDEOS[next], position: 'right' as const },
    ]
  }

  return (
    <div className='relative w-[1400px] py-4'>
      {/* 캐러셀 컨테이너 */}
      <div className='relative h-[420px] flex items-center justify-center gap-0 overflow-visible'>
        {getVisibleVideos().map(({ video, position }) => (
          <div
            key={`${video.id}-${position}`}
            className={`relative transition-all duration-1000 ease-out cursor-pointer ${
              position === 'center'
                ? 'w-[800px] h-[380px] z-20 mx-auto'
                : position === 'left'
                ? 'w-[200px] h-[300px] z-10 -mr-40 opacity-60'
                : 'w-[200px] h-[300px] z-10 -ml-40 opacity-60'
            }`}
            onClick={() => position === 'center' && handleVideoClick(video.id)}
          >
            {/* 이미지 */}
            <div className='relative w-full h-full rounded-xl overflow-hidden shadow-2xl'>
              <img
                src={video.thumbnail}
                alt={video.title}
                className='w-full h-full object-cover transition-transform duration-700'
              />

              {/* 양 옆 어둡게 gradient */}
              {position !== 'center' && (
                <div className='absolute inset-0 bg-black/40 backdrop-blur-[1px]'></div>
              )}

              {/* 중앙 비디오 오버레이 */}
              {position === 'center' && (
                <>
                  <div className='absolute inset-0 bg-gradient-to-t from-black/90 via-black/10 to-black/50'></div>

                  {/* 비디오 정보 */}
                  <div className='absolute bottom-0 left-0 right-0 p-8 text-white'>
                    <div className='flex items-center gap-3 mb-4'>
                      <span className='px-5 py-2 bg-[oklch(0.63_0.12_232)] rounded-full text-3xl font-bold text-white shadow-lg'>
                        {video.themeName}
                      </span>
                      <span className='px-3 py-1.5 bg-white/20 backdrop-blur-sm rounded-full text-sm font-medium'>
                        {video.duration}
                      </span>
                    </div>
                    <h3 className='text-4xl font-extrabold mb-2 drop-shadow-lg'>{video.title}</h3>
                    <p className='text-lg text-gray-100 font-medium'>{video.description}</p>
                  </div>
                </>
              )}
            </div>
          </div>
        ))}

        {/* 이전/다음 버튼 */}
        <button
          type='button'
          onClick={prevSlide}
          className='absolute left-4 z-30 w-12 h-12 rounded-full bg-black/80 hover:bg-black shadow-lg flex items-center justify-center transition-all'
        >
          <svg className='w-6 h-6 text-white' viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M15 18l-6-6 6-6" />
          </svg>
        </button>
        <button
          type='button'
          onClick={nextSlide}
          className='absolute right-4 z-30 w-12 h-12 rounded-full bg-black/80 hover:bg-black shadow-lg flex items-center justify-center transition-all'
        >
          <svg className='w-6 h-6 text-white' viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M9 18l6-6-6-6" />
          </svg>
        </button>
      </div>
    </div>
  )
}

export default VideoCarousel
