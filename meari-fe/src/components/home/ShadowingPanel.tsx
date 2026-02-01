import { useState, useEffect, useRef, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import RoomCard from './RoomCard'
import PasswordModal from '../webrtc/PasswordModal'
import CreateRoomButton from './CreateRoomButton'
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

  return (
    <section>
      {/* 타이틀 및 방 생성 버튼 */}
      <div className='flex items-end justify-between mb-10'>
        <div>
          <h1 className='text-[1.65rem] font-bold text-gray-900 mb-2'>
            쉐도잉 (Shadowing)
          </h1>
          <p className='text-[16px] text-gray-600'>
            한국인의 음성을 실시간으로 따라하며 발음 정확도를 교정 받으세요.
          </p>
        </div>
        <CreateRoomButton />
      </div>

      {/* 필터 및 검색 */}
      <div className='flex items-center justify-between mb-4'>
        {/* 테마 필터 */}
        <div className='flex items-center gap-[8px]'>
          {themes.map((theme) => (
            <button
              key={theme}
              type='button'
              onClick={() => setSelectedTheme(theme)}
              className={`px-[18px] py-[7px] rounded-full text-[13px] font-medium transition-colors ${selectedTheme === theme
                ? 'border-2 border-[#2D9CDB] text-[#2D9CDB] bg-white'
                : 'border border-gray-300 text-gray-600 bg-white hover:border-[#2D9CDB] hover:text-[#2D9CDB]'
                }`}
            >
              {theme}
            </button>
          ))}
        </div>

        {/* 검색창 */}
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
              contentTitle={room.content_title}
              currentPeople={room.current_people}
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
