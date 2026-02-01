import { useEffect, useMemo, useState } from 'react'
import SideBar, { type SideBarMenu } from '../components/common/SideBar'
import {
  getActivityFeed,
  type ActivityItem,
  type ActivityType
} from '../api/activityFeed.api'

const activityTypeOptions: { type: ActivityType; label: string }[] = [
  { type: 'DAILY_LEARNING', label: 'Daily Learning' },
  { type: 'COPIC', label: 'KOPIC' },
  { type: 'SHADOWING', label: 'Shadowing' }
]

const typeBadgeClass: Record<ActivityType, string> = {
  DAILY_LEARNING: 'bg-blue-50 text-blue-700 border-blue-200',
  COPIC: 'bg-amber-50 text-amber-700 border-amber-200',
  SHADOWING: 'bg-emerald-50 text-emerald-700 border-emerald-200'
}

const getTypeLabel = (type: ActivityType) =>
  activityTypeOptions.find((option) => option.type === type)?.label ?? type

const getDateTimeLabel = (item: ActivityItem) => {
  if (item.dateLabel && item.timeLabel) {
    return `${item.dateLabel} ， ${item.timeLabel}`
  }
  const parsed = new Date(item.occurredAt)
  if (Number.isNaN(parsed.getTime())) return item.occurredAt
  return parsed.toLocaleString()
}

const getPayloadSummary = (item: ActivityItem) => {
  switch (item.type) {
    case 'DAILY_LEARNING':
      return `Category: ${item.payload.category} ， Status: ${item.payload.status}`
    case 'COPIC':
      return `Theme: ${item.payload.theme} ， Event: ${item.payload.event}`
    case 'SHADOWING':
      return `Theme: ${item.payload.theme} ， Content: ${item.payload.contentName}`
    default:
      return ''
  }
}

const ActivitySkeleton = () => (
  <div className='border border-gray-200 rounded-xl p-4 animate-pulse bg-white'>
    <div className='flex items-center justify-between'>
      <div className='h-4 w-24 bg-gray-200 rounded' />
      <div className='h-3 w-20 bg-gray-200 rounded' />
    </div>
    <div className='h-4 w-4/5 bg-gray-200 rounded mt-4' />
    <div className='h-3 w-2/3 bg-gray-200 rounded mt-2' />
  </div>
)

const MyPage = () => {
  const [activeMenu, setActiveMenu] = useState<SideBarMenu>('dashboard')
  const [activities, setActivities] = useState<ActivityItem[]>([])
  const [cursor, setCursor] = useState<string | null>(null)
  const [hasNext, setHasNext] = useState(false)
  const [isLoading, setIsLoading] = useState(false)
  const [isLoadingMore, setIsLoadingMore] = useState(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const [selectedTypes, setSelectedTypes] = useState<ActivityType[]>([])

  const stats = useMemo(() => {
    const summary = {
      total: activities.length,
      daily: 0,
      copic: 0,
      shadowing: 0
    }

    activities.forEach((item) => {
      if (item.type === 'DAILY_LEARNING') summary.daily += 1
      if (item.type === 'COPIC') summary.copic += 1
      if (item.type === 'SHADOWING') summary.shadowing += 1
    })

    return summary
  }, [activities])

  const loadActivities = async (append: boolean) => {
    if (append) {
      setIsLoadingMore(true)
    } else {
      setIsLoading(true)
      setActivities([])
      setCursor(null)
      setHasNext(false)
    }

    setErrorMessage(null)

    try {
      const response = await getActivityFeed({
        cursor: append ? cursor ?? undefined : undefined,
        size: 6,
        types: selectedTypes.length ? selectedTypes : undefined
      })

      if (response.data.success && response.data.data) {
        const { items, nextCursor, hasNext: responseHasNext } = response.data.data
        setActivities((prev) => (append ? [...prev, ...items] : items))
        setCursor(nextCursor)
        setHasNext(responseHasNext)
      } else {
        setErrorMessage(response.data.error?.message ?? 'Failed to load activity feed.')
      }
    } catch (error) {
      setErrorMessage('Failed to load activity feed.')
      console.error(error)
    } finally {
      setIsLoading(false)
      setIsLoadingMore(false)
    }
  }

  useEffect(() => {
    if (activeMenu !== 'dashboard') return
    void loadActivities(false)
  }, [activeMenu, selectedTypes])

  const toggleTypeFilter = (type: ActivityType) => {
    setSelectedTypes((prev) =>
      prev.includes(type) ? prev.filter((item) => item !== type) : [...prev, type]
    )
  }

  const renderDashboard = () => (
    <div className='p-8 space-y-6'>
      <div>
        <h1 className='text-3xl font-bold text-gray-900 mb-2'>Dashboard</h1>
        <p className='text-gray-500'>Track your recent learning activity.</p>
      </div>

      <div className='grid grid-cols-1 md:grid-cols-3 gap-4'>
        <div className='rounded-xl border border-gray-200 bg-white p-4'>
          <p className='text-sm text-gray-500'>Total activities</p>
          <p className='text-2xl font-semibold text-gray-900 mt-2'>{stats.total}</p>
        </div>
        <div className='rounded-xl border border-gray-200 bg-white p-4'>
          <p className='text-sm text-gray-500'>Daily learning</p>
          <p className='text-2xl font-semibold text-gray-900 mt-2'>{stats.daily}</p>
        </div>
        <div className='rounded-xl border border-gray-200 bg-white p-4'>
          <p className='text-sm text-gray-500'>Shadowing + KOPIC</p>
          <p className='text-2xl font-semibold text-gray-900 mt-2'>
            {stats.shadowing + stats.copic}
          </p>
        </div>
      </div>

      <div className='rounded-2xl border border-gray-200 bg-white p-5'>
        <div className='flex flex-col md:flex-row md:items-center md:justify-between gap-3 mb-4'>
          <div>
            <h2 className='text-xl font-semibold text-gray-900'>Activity feed</h2>
            <p className='text-sm text-gray-500'>Latest actions from your account.</p>
          </div>
          <div className='flex flex-wrap items-center gap-2'>
            {activityTypeOptions.map((option) => {
              const isActive = selectedTypes.includes(option.type)
              return (
                <button
                  key={option.type}
                  type='button'
                  onClick={() => toggleTypeFilter(option.type)}
                  className={`px-3 py-1 rounded-full border text-xs font-semibold transition-colors ${
                    isActive
                      ? 'bg-gray-900 text-white border-gray-900'
                      : 'bg-white text-gray-600 border-gray-200 hover:border-gray-300'
                  }`}
                >
                  {option.label}
                </button>
              )
            })}
            {selectedTypes.length > 0 && (
              <button
                type='button'
                onClick={() => setSelectedTypes([])}
                className='px-3 py-1 rounded-full border text-xs font-semibold text-gray-500 border-gray-200 hover:border-gray-300'
              >
                Clear
              </button>
            )}
          </div>
        </div>

        <div className='space-y-3'>
          {isLoading && (
            <>
              {Array.from({ length: 4 }).map((_, index) => (
                <ActivitySkeleton key={`activity-skeleton-${index}`} />
              ))}
            </>
          )}

          {!isLoading && errorMessage && (
            <div className='border border-red-200 bg-red-50 text-red-700 rounded-xl p-4'>
              {errorMessage}
            </div>
          )}

          {!isLoading && !errorMessage && activities.length === 0 && (
            <div className='border border-gray-200 rounded-xl p-6 text-center text-gray-500'>
              No activity yet. Start a session to see updates here.
            </div>
          )}

          {!isLoading && !errorMessage && activities.length > 0 && (
            <div className='space-y-3'>
              {activities.map((item) => (
                <div
                  key={item.activityId}
                  className='border border-gray-200 rounded-xl p-4 flex flex-col md:flex-row md:items-center gap-4'
                >
                  <div
                    className={`px-2.5 py-1 rounded-full border text-xs font-semibold w-fit ${
                      typeBadgeClass[item.type]
                    }`}
                  >
                    {getTypeLabel(item.type)}
                  </div>
                  <div className='flex-1'>
                    <div className='flex flex-col md:flex-row md:items-center md:justify-between gap-2'>
                      <h3 className='text-sm md:text-base font-semibold text-gray-900'>
                        {item.title}
                      </h3>
                      <span className='text-xs text-gray-500'>{getDateTimeLabel(item)}</span>
                    </div>
                    <p className='text-sm text-gray-600 mt-1'>{getPayloadSummary(item)}</p>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {hasNext && !isLoading && !errorMessage && (
          <div className='mt-4 flex justify-center'>
            <button
              type='button'
              onClick={() => void loadActivities(true)}
              disabled={isLoadingMore}
              className='px-4 py-2 rounded-full text-sm font-semibold border border-gray-200 text-gray-700 hover:border-gray-300 disabled:opacity-60'
            >
              {isLoadingMore ? 'Loading...' : 'Load more'}
            </button>
          </div>
        )}
      </div>
    </div>
  )

  const renderContent = () => {
    switch (activeMenu) {
      case 'dashboard':
        return renderDashboard()
      case 'profile':
        return (
          <div className='p-8'>
            <h1 className='text-3xl font-bold text-gray-900 mb-6'>Profile</h1>
            <p className='text-gray-600'>View and update your profile details.</p>
          </div>
        )
      case 'report':
        return (
          <div className='p-8'>
            <h1 className='text-3xl font-bold text-gray-900 mb-6'>Reports</h1>
            <p className='text-gray-600'>Check your learning reports here.</p>
          </div>
        )
      case 'settings':
        return (
          <div className='p-8'>
            <h1 className='text-3xl font-bold text-gray-900 mb-6'>Settings</h1>
            <p className='text-gray-600'>Manage account and notification settings.</p>
          </div>
        )
      default:
        return null
    }
  }

  return (
    <div className='flex min-h-screen w-full'>
      <aside>
        <SideBar activeMenu={activeMenu} onMenuChange={setActiveMenu} userInitial='U' />
      </aside>

      <main className='flex-1 bg-white'>
        {renderContent()}
      </main>
    </div>
  )
}

export default MyPage
