import { useEffect, useState } from 'react'
import {
  getActivityFeedMock,
  type ActivityItem,
  type ActivityType
} from '../../api/activityFeed.api'

const typeBadgeClass: Record<ActivityType, string> = {
  DAILY_LEARNING: 'bg-blue-50 text-blue-700 border-blue-200',
  COPIC: 'bg-amber-50 text-amber-700 border-amber-200',
  SHADOWING: 'bg-emerald-50 text-emerald-700 border-emerald-200'
}

const getTypeLabel = (type: ActivityType) => {
  const labels = {
    DAILY_LEARNING: 'Daily Learning',
    COPIC: 'KOPIC',
    SHADOWING: 'Shadowing'
  }
  return labels[type]
}

const getDateTimeLabel = (item: ActivityItem) => {
  const date = new Date(item.occurredAt)
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${month}/${day} ${hours}:${minutes}`
}

const getPayloadSummary = (item: ActivityItem) => {
  switch (item.type) {
    case 'DAILY_LEARNING':
      return `Category: ${item.payload.category} · Status: ${item.payload.status}`
    case 'COPIC':
      return `Theme: ${item.payload.theme} · Event: ${item.payload.event}`
    case 'SHADOWING':
      return `Theme: ${item.payload.theme} · Content: ${item.payload.contentName}`
    default:
      return ''
  }
}

const MyActivityFeed = () => {
  const [activities, setActivities] = useState<ActivityItem[]>([])
  const [isLoading, setIsLoading] = useState(false)

  useEffect(() => {
    const loadActivities = async () => {
      setIsLoading(true)
      try {
        const response = await getActivityFeedMock({ size: 100 })
        if (response.data.success && response.data.data) {
          setActivities(response.data.data.items)
        }
      } catch (error) {
        console.error('Failed to load activities:', error)
      } finally {
        setIsLoading(false)
      }
    }

    loadActivities()
  }, [])

  return (
    <div className='rounded-2xl border-gray-200 bg-white p-5'>
      <div className='mb-4'>
        <h2 className='text-xl font-semibold text-gray-900'>활동</h2>
        <p className='text-sm text-gray-500'>Latest actions from your account.</p>
      </div>

      <div className='space-y-3'>
        {isLoading && (
          <div className='flex justify-center py-8'>
            <div className='w-8 h-8 border-4 border-gray-300 border-t-gray-900 rounded-full animate-spin' />
          </div>
        )}

        {!isLoading && activities.length === 0 && (
          <div className='border border-gray-200 rounded-xl p-6 text-center text-gray-500'>
            No activity yet. Start a session to see updates here.
          </div>
        )}

        {!isLoading && activities.length > 0 && (
          <div className='space-y-3 max-w-2xl'>
            {activities.map((item) => (
              <div
                key={item.activityId}
                className='border-l-4 border-[#2D9CDB] p-4 flex flex-col gap-3'
              >
                <div className='flex items-center gap-3'>
                  <div
                    className={`px-2.5 py-1 rounded-full border text-xs font-semibold ${
                      typeBadgeClass[item.type]
                    }`}
                  >
                    {getTypeLabel(item.type)}
                  </div>
                </div>
                <div>
                  <h3 className='text-sm font-semibold text-gray-900'>
                    {item.title}
                  </h3>
                  <p className='text-sm text-gray-600 mt-1'>{getPayloadSummary(item)}</p>
                  <span className='text-xs text-gray-400 mt-2 block'>{getDateTimeLabel(item)}</span>
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
