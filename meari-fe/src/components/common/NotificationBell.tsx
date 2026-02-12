import { useState, useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { Bell } from 'lucide-react'
import { getShadowingReportsAPI } from '../../api/mypage.api'
import type { ShadowingReport } from '../../api/mypage.api'

const NotificationBell = () => {
  const navigate = useNavigate()
  const [unreadReports, setUnreadReports] = useState<ShadowingReport[]>([])
  const [isOpen, setIsOpen] = useState(false)
  const bellRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const fetchReports = async () => {
      try {
        const response = await getShadowingReportsAPI()
        const reports = response.data.data?.contents ?? []
        const unread = reports.filter((report) => !report.is_read)
        setUnreadReports(unread)
      } catch {
        // 실패 시 무시
      }
    }

    fetchReports()
  }, [])

  // 드롭다운 외부 클릭 감지
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (bellRef.current && !bellRef.current.contains(event.target as Node)) {
        setIsOpen(false)
      }
    }

    document.addEventListener('mousedown', handleClickOutside)
    return () => {
      document.removeEventListener('mousedown', handleClickOutside)
    }
  }, [])

  const formatTime = (dateString: string) => {
    const date = new Date(dateString)
    const now = new Date()
    const diffMs = now.getTime() - date.getTime()
    const diffMin = Math.floor(diffMs / 60000)
    const diffHour = Math.floor(diffMin / 60)
    const diffDay = Math.floor(diffHour / 24)

    if (diffMin < 1) return '방금 전'
    if (diffMin < 60) return `${diffMin}분 전`
    if (diffHour < 24) return `${diffHour}시간 전`
    if (diffDay < 7) return `${diffDay}일 전`
    return date.toLocaleDateString('ko-KR', { month: 'short', day: 'numeric' })
  }

  const handleGoToMyPage = () => {
    setIsOpen(false)
    navigate('/mypage', { state: { menu: 'report' } })
  }

  return (
    <div className='relative' ref={bellRef}>
      <button
        type='button'
        onClick={() => setIsOpen(!isOpen)}
        className='relative flex items-center justify-center w-10 h-10 rounded-full hover:bg-gray-100 transition-colors cursor-pointer'
        title='알림'
      >
        <Bell size={21} />
        {unreadReports.length > 0 && (
          <span className='absolute top-1 right-1 w-3 h-3 bg-blue-500 rounded-full border-2 border-white' />
        )}
      </button>

      {isOpen && (
        <div className='absolute top-full right-0 mt-2 w-80 bg-white rounded-lg shadow-lg z-10 overflow-hidden'>
          <div className='px-4 py-3 border-b border-gray-200'>
            <h3 className='font-semibold text-gray-900 text-sm'>알림</h3>
          </div>

          <div className='max-h-80 overflow-y-auto'>
            {unreadReports.length === 0 ? (
              <div className='px-4 py-8 text-center text-gray-400 text-sm'>
                새로운 알림이 없습니다.
              </div>
            ) : (
              unreadReports.map((report) => (
                <div
                  key={report.shadowing_report_id}
                  className='flex items-start gap-3 px-4 py-3 hover:bg-gray-50 transition-colors border-b border-gray-100 last:border-b-0'
                >
                  <img
                    src={report.thumbnail_url}
                    alt={report.content_title}
                    className='w-10 h-10 rounded object-cover flex-shrink-0 mt-0.5'
                  />
                  <div className='flex-1 min-w-0'>
                    <p className='text-sm font-medium text-gray-900 truncate'>
                      {report.content_title}
                    </p>
                    <p className='text-xs text-gray-500 mt-0.5'>
                      리포트가 생성되었습니다!
                    </p>
                    <p className='text-xs text-gray-400 mt-1'>
                      {formatTime(report.created_at)}
                    </p>
                  </div>
                </div>
              ))
            )}
          </div>

          <div className='px-4 py-2.5 border-t border-gray-200'>
            <button
              onClick={handleGoToMyPage}
              className='w-full text-center text-sm text-blue-600 font-medium hover:text-blue-700 transition-colors py-1 rounded-md hover:bg-blue-50 cursor-pointer'
            >
              마이페이지에서 확인하기
            </button>
          </div>
        </div>
      )}
    </div>
  )
}

export default NotificationBell
