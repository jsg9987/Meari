import { useEffect, useState } from 'react'
import { Clock } from 'lucide-react'
import {
  getRecentActivities,
  type RecentActivity,
  type RecentActivityType
} from '../../api/mypage.api'

const typeBadgeClass: Record<RecentActivityType, string> = {
  DAILY: 'bg-blue-50 text-blue-700 border-blue-200',
  KOPIC: 'bg-amber-50 text-amber-700 border-amber-200',
  SHADOWING: 'bg-emerald-50 text-emerald-700 border-emerald-200'
}

const getTypeLabel = (type: RecentActivityType) => {
  const labels = {
    DAILY: '일일 학습',
    KOPIC: 'KOPIC',
    SHADOWING: '쉐도잉'
  }
  return labels[type]
}

const getDateTimeLabel = (activity: RecentActivity) => {
  const date = new Date(activity.created_at)
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${month}/${day} ${hours}:${minutes}`
}

const getActivitySummary = (activity: RecentActivity) => {
  switch (activity.activity_type) {
    case 'DAILY':
      return activity.status === 'COMPLETED' ? '완료' : '진행 중'
    case 'SHADOWING':
      return activity.theme && activity.content
        ? `테마: ${activity.theme} · 콘텐츠: ${activity.content}`
        : activity.theme || ''
    case 'KOPIC':
      return activity.theme ? `테마: ${activity.theme}` : ''
    default:
      return ''
  }
}

const MyActivityFeed = () => {
  const [activities, setActivities] = useState<RecentActivity[]>([])
  const [isLoading, setIsLoading] = useState(false)

  useEffect(() => {
    const loadActivities = async () => {
      setIsLoading(true)
      try {
        const response = await getRecentActivities()
        if (response.data.success && response.data.data) {
          setActivities(response.data.data)
        }
      } catch (error) {
        console.error('Failed to load recent activities:', error)
      } finally {
        setIsLoading(false)
      }
    }

    loadActivities()
  }, [])

  return (
    <div className='rounded-2xl border border-gray-200 bg-white p-5 flex flex-col lg:h-full lg:max-h-225'>
      <div className='mb-4 shrink-0'>
        <h2 className='text-xl font-semibold text-gray-900'>활동</h2>
        <p className='text-sm text-gray-500'>최근 학습 활동 내역입니다.</p>
      </div>

      <div className='space-y-3 overflow-y-auto flex-1 min-h-0'>
        {isLoading && (
          <div className='space-y-3'>
            {[1, 2, 3, 4, 5].map((i) => (
              <div key={i} className='border-l-4 border-gray-200 p-4 flex flex-col gap-3'>
                <div className='h-6 w-24 bg-gray-200 rounded-full animate-pulse' />
                <div className='space-y-2'>
                  <div className='h-4 w-full bg-gray-200 rounded animate-pulse' />
                  <div className='h-4 w-3/4 bg-gray-200 rounded animate-pulse' />
                  <div className='h-3 w-20 bg-gray-200 rounded animate-pulse mt-2' />
                </div>
              </div>
            ))}
          </div>
        )}

        {!isLoading && activities.length === 0 && (
          <div className='border border-gray-200 rounded-xl p-6 text-center text-gray-500'>
            아직 활동 내역이 없습니다. 학습을 시작해보세요!
          </div>
        )}

        {!isLoading && activities.length > 0 && (
          <div className='space-y-3 max-w-2xl'>
            {activities.map((activity, index) => (
              <div
                key={`${activity.activity_type}-${activity.created_at}-${index}`}
                className='border-l-4 border-[#2D9CDB] p-4 flex flex-col gap-3'
              >
                <div className='flex items-center gap-3'>
                  <div
                    className={`px-2.5 py-1 rounded-full border text-xs font-semibold ${
                      typeBadgeClass[activity.activity_type]
                    }`}
                  >
                    {getTypeLabel(activity.activity_type)}
                  </div>
                </div>
                <div>
                  <h3 className='text-sm font-semibold text-gray-900'>{activity.title}</h3>
                  <p className='text-sm text-gray-600 mt-1'>{getActivitySummary(activity)}</p>
                  <div className='flex items-center gap-1 mt-2'>
                    <Clock size={12} className='text-gray-400' />
                    <span className='text-xs text-gray-400'>{getDateTimeLabel(activity)}</span>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}

export default MyActivityFeed
