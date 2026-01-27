import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import RoomCard from './RoomCard'
import PasswordModal from './PasswordModal'
import { getRooms, joinRoom, type RoomItem } from '../../api/rooms.api'

const themes = ['전체', '생활', '비즈니스', '뉴스', '공공행정'] as const

const ShadowingPanel = () => {
  const navigate = useNavigate()
  const [selectedTheme, setSelectedTheme] = useState<string>('전체')
  const [searchKeyword, setSearchKeyword] = useState('')
  const [rooms, setRooms] = useState<RoomItem[]>([])
  const [isLoading, setIsLoading] = useState(true)

  // 비밀번호 모달 상태
  const [isPasswordModalOpen, setIsPasswordModalOpen] = useState(false)
  const [selectedRoomId, setSelectedRoomId] = useState<string | null>(null)

  // 방 목록 불러오기
  useEffect(() => {
    const fetchRooms = async () => {
      setIsLoading(true)
      try {
        const response = await getRooms({
          theme: selectedTheme,
          keyword: searchKeyword,
        })
        if (response.success && response.data) {
          setRooms(response.data.rooms)
        }
      } catch (error) {
        console.error('Failed to fetch rooms:', error)
      } finally {
        setIsLoading(false)
      }
    }

    fetchRooms()
  }, [selectedTheme, searchKeyword])

  // 방 클릭 핸들러
  const handleRoomClick = (room: RoomItem) => {
    if (room.has_password) {
      setSelectedRoomId(room.room_id)
      setIsPasswordModalOpen(true)
    } else {
      navigate(`/shadowing/${room.room_id}`)
    }
  }

  // 비밀번호 제출 핸들러
  const handlePasswordSubmit = async (password: string) => {
    if (!selectedRoomId) return

    try {
      const response = await joinRoom({
        room_id: selectedRoomId,
        password,
      })
      if (response.success) {
        setIsPasswordModalOpen(false)
        navigate(`/shadowing/${selectedRoomId}`)
      }
    } catch (error: unknown) {
      const err = error as { response?: { data?: { error?: { message?: string } } } }
      alert(err.response?.data?.error?.message || '입장에 실패했습니다.')
    }
  }

  return (
    <section>
      {/* 타이틀 */}
      <div className='mb-6'>
        <h1 className='text-2xl font-bold text-gray-900 mb-2'>
          쉐도잉 (Shadowing)
        </h1>
        <p className='text-sm text-gray-600'>
          한국인의 음성을 실시간으로 따라하며 발음 정확도를 교정 받으세요.
        </p>
      </div>

      {/* 필터 및 검색 */}
      <div className='flex items-center justify-between mb-6'>
        {/* 테마 필터 */}
        <div className='flex items-center gap-2'>
          {themes.map((theme) => (
            <button
              key={theme}
              type='button'
              onClick={() => setSelectedTheme(theme)}
              className={`px-4 py-1.5 rounded-full text-sm font-medium transition-colors ${selectedTheme === theme
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
            className='w-48 px-4 py-2 pr-10 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-[#2D9CDB] focus:border-transparent'
          />
          <svg
            className='absolute right-3 top-1/2 -translate-y-1/2 text-gray-400'
            width="16"
            height="16"
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
      {isLoading ? (
        <div className='text-center py-10 text-gray-500'>로딩 중...</div>
      ) : rooms.length === 0 ? (
        <div className='text-center py-10 text-gray-500'>방이 없습니다.</div>
      ) : (
        <div className='grid grid-cols-4 gap-4'>
          {rooms.map((room) => (
            <RoomCard
              key={room.room_id}
              roomId={room.room_id}
              title={room.title}
              currentPeople={room.current_people}
              hasPassword={room.has_password}
              onClick={() => handleRoomClick(room)}
            />
          ))}
        </div>
      )}

      {/* 비밀번호 모달 */}
      <PasswordModal
        isOpen={isPasswordModalOpen}
        onClose={() => setIsPasswordModalOpen(false)}
        onSubmit={handlePasswordSubmit}
      />
    </section>
  )
}

export default ShadowingPanel
