import { useState, useEffect, useRef, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import RoomCard from './RoomCard'
import PasswordModal from '../webrtc/PasswordModal'
import CreateRoomButton from './CreateRoomButton'
import ThemeSidebar from './ThemeSidebar'
import { getRooms, joinRoom, type RoomItem } from '../../api/rooms.api'
import { getThemes, type Theme } from '../../api/contents.api'

// 테마 목록은 API를 통해 가져옵니다.

// TODO: useEffect 4번 호출 버그 수정
const ShadowingPanel = () => {
  const navigate = useNavigate()
  const [themes, setThemes] = useState<string[]>(['전체'])
  const [themeData, setThemeData] = useState<Theme[]>([])
  const [selectedTheme, setSelectedTheme] = useState<string>('전체')
  const [searchKeyword, setSearchKeyword] = useState('')
  const [debouncedSearchKeyword, setDebouncedSearchKeyword] = useState('')
  const [rooms, setRooms] = useState<RoomItem[]>([])
  const [isInitialLoading, setIsInitialLoading] = useState(true)
  const [isFetchingNextPage, setIsFetchingNextPage] = useState(false)
  const [nextCursor, setNextCursor] = useState<number | null>(null)
  const [hasNext, setHasNext] = useState(true)

  const observerTarget = useRef<HTMLDivElement>(null)
  const minLoadingTimeRef = useRef<number | null>(null)

  // 비밀번호 모달 상태
  const [isPasswordModalOpen, setIsPasswordModalOpen] = useState(false)
  const [selectedRoomId, setSelectedRoomId] = useState<number | string | null>(null)
  const [selectedRoomTitle, setSelectedRoomTitle] = useState<string>('')

  // 방 목록 불러오기 함수
  const fetchRooms = useCallback(async (isFirstPage: boolean = false) => {
    if (isFirstPage) {
      setIsInitialLoading(true)
      // 스켈레톤 최소 노출 시간 시작
      minLoadingTimeRef.current = Date.now()
    } else {
      if (!hasNext || isFetchingNextPage) return
      setIsFetchingNextPage(true)
    }

    try {
      // 선택된 테마 이름에 맞는 theme_id 찾기
      const targetTheme = themeData.find(t => t.name === selectedTheme);
      const themeId = selectedTheme === '전체' ? undefined : targetTheme?.theme_id;

      const response = await getRooms({
        themeId: themeId,
        cursor: isFirstPage ? undefined : (nextCursor ?? undefined),
        size: 16
      });

      if (response.data.success && response.data.data) {
        const newRooms = response.data.data.contents;

        // 첫 페이지 로딩 시 최소 300ms 보장
        if (isFirstPage && minLoadingTimeRef.current) {
          const elapsedTime = Date.now() - minLoadingTimeRef.current
          const remainingTime = Math.max(0, 300 - elapsedTime)

          if (remainingTime > 0) {
            await new Promise(resolve => setTimeout(resolve, remainingTime))
          }
        }

        if (isFirstPage) {
          setRooms(newRooms);
        } else {
          setRooms(prev => [...prev, ...newRooms]);
        }
        setNextCursor(response.data.data.next_cursor);
        setHasNext(response.data.data.has_next);
      }
    } catch (error) {
      console.error('Failed to fetch rooms:', error)
    } finally {
      setIsInitialLoading(false)
      setIsFetchingNextPage(false)
      minLoadingTimeRef.current = null
    }
  }, [selectedTheme, themeData, nextCursor, hasNext, isFetchingNextPage])

  // 초기 테마 로드
  useEffect(() => {
    const loadThemes = async () => {
      try {
        const response = await getThemes()
        if (response.data.success && response.data.data) {
          const fetchedThemes = response.data.data
          setThemeData(fetchedThemes)
          setThemes(['전체', ...fetchedThemes.map(t => t.name)])
        }
      } catch (error) {
        console.error('Failed to load themes:', error)
      }
    }
    loadThemes()
  }, [])

  // 검색어 디바운싱 (500ms)
  useEffect(() => {
    const timeoutId = setTimeout(() => {
      setDebouncedSearchKeyword(searchKeyword)
    }, 500)

    return () => clearTimeout(timeoutId)
  }, [searchKeyword])

  // 테마/디바운스된 검색어 변경 시 초기화
  useEffect(() => {
    setNextCursor(null)
    setHasNext(true)
    fetchRooms(true)
  }, [selectedTheme, debouncedSearchKeyword])

  // 무한 스크롤 Observer 설정
  useEffect(() => {
    if (!observerTarget.current || !hasNext || isFetchingNextPage || isInitialLoading) {
      return
    }

    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting) {
          fetchRooms()
        }
      },
      { threshold: 0.1 }
    )

    observer.observe(observerTarget.current)
    return () => {
      observer.disconnect()
    }
  }, [fetchRooms, hasNext, isFetchingNextPage, isInitialLoading])

  // 방 클릭 핸들러
  const handleRoomClick = (room: RoomItem) => {
    if (room.has_password) {
      setSelectedRoomId(room.room_id)
      setSelectedRoomTitle(room.title)
      setIsPasswordModalOpen(true)
    } else {
      navigate(`/shadowing/${room.room_id}`)
    }
  }

  // 비밀번호 제출 핸들러
  const handlePasswordSubmit = async (password: string) => {
    if (!selectedRoomId) return

    const response = await joinRoom({
      room_id: selectedRoomId as number,
      password,
    })
    if (response.data.success) {
      setIsPasswordModalOpen(false)
      navigate(`/shadowing/${selectedRoomId}`)
    } else {
      throw new Error('비밀번호가 일치하지 않습니다.')
    }
  }

    // 추천 콘텐츠 목업 데이터
  const featuredContents = [
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
  ]

  return (
    <section className='relative'>
      {/* 왼쪽 사이드바 - 페이지 왼쪽에 고정 */}
      <div className='fixed left-0 top-[var(--header-height)] h-[calc(100vh-var(--header-height))] overflow-y-auto z-50'>
        <ThemeSidebar
          themes={themeData}
          selectedTheme={selectedTheme}
          onThemeSelect={setSelectedTheme}
        />
      </div>

      {/* 메인 콘텐츠 */}
      <div className='ml-64 px-8'>
        {/* 방 생성 버튼 */}
        <div className='flex items-end justify-end mb-6'>
          <CreateRoomButton />
        </div>

        {/* 추천 콘텐츠 섹션 */}
        <div className='mb-8'>
          <h2 className='text-xl font-bold text-gray-900 mb-4'>추천 쉐도잉 콘텐츠</h2>
          <div className='flex gap-4 h-[400px]'>
            {/* 왼쪽 큰 사진 */}
            <div
              className='flex-1 relative rounded-xl overflow-hidden shadow-lg cursor-pointer group'
              onClick={() => console.log('Selected:', featuredContents[0].id)}
            >
              <img
                src={featuredContents[0].thumbnail}
                alt={featuredContents[0].title}
                className='w-full h-full object-cover transition-transform duration-300 group-hover:scale-105'
              />
              <div className='absolute inset-0 bg-gradient-to-t from-black/90 via-black/20 to-black/50'></div>
              <div className='absolute bottom-0 left-0 right-0 p-6 text-white'>
                <div className='flex items-center gap-3 mb-3'>
                  <span className='px-4 py-1.5 bg-[oklch(0.63_0.12_232)] rounded-full text-sm font-bold'>
                    {featuredContents[0].themeName}
                  </span>
                  <span className='px-3 py-1 bg-white/20 backdrop-blur-sm rounded-full text-sm font-medium'>
                    {featuredContents[0].duration}
                  </span>
                </div>
                <h3 className='text-3xl font-extrabold mb-2'>{featuredContents[0].title}</h3>
                <p className='text-lg text-gray-100'>{featuredContents[0].description}</p>
              </div>
            </div>

            {/* 오른쪽 세로 두 개 */}
            <div className='flex flex-col gap-4 w-[380px]'>
              {featuredContents.slice(1, 3).map((content) => (
                <div
                  key={content.id}
                  className='flex-1 relative rounded-xl overflow-hidden shadow-lg cursor-pointer group'
                  onClick={() => console.log('Selected:', content.id)}
                >
                  <img
                    src={content.thumbnail}
                    alt={content.title}
                    className='w-full h-full object-cover transition-transform duration-300 group-hover:scale-105'
                  />
                  <div className='absolute inset-0 bg-gradient-to-t from-black/90 via-black/20 to-black/50'></div>
                  <div className='absolute bottom-0 left-0 right-0 p-4 text-white'>
                    <div className='flex items-center gap-2 mb-2'>
                      <span className='px-3 py-1 bg-[oklch(0.63_0.12_232)] rounded-full text-xs font-bold'>
                        {content.themeName}
                      </span>
                      <span className='px-2 py-0.5 bg-white/20 backdrop-blur-sm rounded-full text-xs font-medium'>
                        {content.duration}
                      </span>
                    </div>
                    <h3 className='text-lg font-bold mb-1'>{content.title}</h3>
                    <p className='text-sm text-gray-200 line-clamp-1'>{content.description}</p>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* 검색창 */}
        <div className='flex items-center justify-between mb-6'>
          <h2 className='text-xl font-bold text-gray-900'>쉐도잉 방 목록</h2>
          <div className='relative'>
            <input
              type='text'
              value={searchKeyword}
              onChange={(e) => setSearchKeyword(e.target.value)}
              placeholder='search'
              className='w-[250px] px-[18px] py-[7px] pr-11 border border-gray-300 rounded-full text-[13px] focus:outline-none focus:ring-2 focus:ring-[#2D9CDB] focus:border-transparent'
            />
            <svg
              className='absolute right-3 top-1/2 -translate-y-1/2 text-gray-400'
              width="18"
              height="18"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
            >
              <circle cx="11" cy="11" r="8"></circle>
              <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
            </svg>
          </div>
        </div>

        {/* 방 목록 그리드 */}
        {isInitialLoading ? (
          <div className='grid grid-cols-4 gap-x-[18px] gap-y-[24px]'>
            {[...Array(4)].map((_, index) => (
              <div key={index} className='bg-white rounded-lg overflow-hidden shadow-md border border-gray-100'>
                {/* 썸네일 스켈레톤 */}
                <div className='aspect-video animate-shimmer' />
                {/* 정보 스켈레톤 */}
                <div className='p-4 space-y-3'>
                  <div className='h-5 animate-shimmer rounded' />
                  <div className='h-4 animate-shimmer rounded w-4/5' />
                  <div className='flex items-center justify-between pt-2'>
                    <div className='h-4 animate-shimmer rounded w-16' />
                    <div className='h-4 animate-shimmer rounded w-12' />
                  </div>
                </div>
              </div>
            ))}
          </div>
        ) : rooms.length === 0 ? (
          <div className='text-center py-20 text-gray-500'>방이 없습니다.</div>
        ) : (
          <div className='grid grid-cols-4 gap-x-[18px] gap-y-[24px]'>
            {rooms.map((room, index) => (
              <RoomCard
                key={`${room.room_id}-${index}`}
                roomId={room.room_id}
                title={room.title}
                themeName={room.theme_name}
                contentTitle={room.content_title}
                currentPeople={room.current_people}
                maxPeople={room.max_people}
                status={room.status}
                hasPassword={room.has_password}
                onClick={() => handleRoomClick(room)}
              />
            ))}
            {/* 무한 스크롤 타겟 및 하단 로딩 표시 */}
            <div ref={observerTarget} className="h-10 w-full col-span-4 flex items-center justify-center">
              {isFetchingNextPage && <div className="text-gray-400 text-sm">추가 방 불러오는 중...</div>}
            </div>
          </div>
        )}
      </div>

      {/* 비밀번호 모달 */}
      {isPasswordModalOpen && (
        <PasswordModal
          roomTitle={selectedRoomTitle}
          onCancel={() => setIsPasswordModalOpen(false)}
          onSubmit={handlePasswordSubmit}
        />
      )}
    </section>
  )
}

export default ShadowingPanel
