import { useEffect, useState } from 'react'
import { getDailyActivity, type DailyActivity, type DailyActivityStatus } from '../../api/mypage.api'

const statusColors: Record<DailyActivityStatus, string> = {
  NONE: 'bg-gray-100',
  WORD: 'bg-blue-300',
  SENTENCE: 'bg-green-300',
  BOTH: 'bg-purple-400'
}

const statusLabels: Record<DailyActivityStatus, string> = {
  NONE: '학습 X',
  WORD: '단어 학습',
  SENTENCE: '문장 학습',
  BOTH: '단어+문장 학습'
}

const DailyActivityHeatmap = () => {
  const [activities, setActivities] = useState<DailyActivity[]>([])
  const [isLoading, setIsLoading] = useState(false)

  useEffect(() => {
    const loadData = async () => {
      setIsLoading(true)
      try {
        const response = await getDailyActivity()
        if (response.data.success && response.data.data) {
          setActivities(response.data.data.activities)
        }
      } catch (error) {
        console.error('Failed to load daily activity:', error)
      } finally {
        setIsLoading(false)
      }
    }

    loadData()
  }, [])

  if (isLoading) {
    return (
      <div className='rounded-2xl border border-gray-200 bg-white py-8 px-5 hidden lg:block w-fit'>
        <div className='mb-4'>
          <div className='h-7 w-40 bg-gray-200 rounded animate-pulse mb-2' />
          <div className='h-5 w-32 bg-gray-200 rounded animate-pulse' />
        </div>
        <div className='space-y-4'>
          <div className='h-4 w-full bg-gray-200 rounded animate-pulse' />
          <div className='h-24 w-full bg-gray-200 rounded animate-pulse' />
          <div className='h-4 w-3/4 bg-gray-200 rounded animate-pulse' />
        </div>
      </div>
    )
  }

  // 날짜를 주 단위로 그룹화
  const getWeeksData = (acts: DailyActivity[]) => {
    const weeks: DailyActivity[][] = []
    let currentWeek: DailyActivity[] = []

    if (acts.length === 0) return weeks

    // 시작 날짜 찾기 (일요일부터 시작하도록)
    const startDate = new Date(acts[0].date)
    const startDay = startDate.getDay()

    // 시작 요일이 일요일이 아니면 앞부분을 빈 칸으로 채움
    for (let i = 0; i < startDay; i++) {
      currentWeek.push({ date: '', status: 'NONE' })
    }

    acts.forEach((activity) => {
      currentWeek.push(activity)

      if (currentWeek.length === 7) {
        weeks.push(currentWeek)
        currentWeek = []
      }
    })

    // 마지막 주가 7일이 안 되면 빈 칸으로 채움
    if (currentWeek.length > 0) {
      while (currentWeek.length < 7) {
        currentWeek.push({ date: '', status: 'NONE' })
      }
      weeks.push(currentWeek)
    }

    return weeks
  }

  const weeks = getWeeksData(activities)
  const dayLabels = ['일', '월', '화', '수', '목', '금', '토']

  // 월 레이블 생성 (잔디 뷰용)
  const getMonthLabels = () => {
    const labels: { month: string; weekIndex: number }[] = []
    let lastMonth = ''

    weeks.forEach((week, weekIndex) => {
      const firstDayWithDate = week.find((day) => day.date !== '')
      if (firstDayWithDate) {
        const date = new Date(firstDayWithDate.date)
        const month = `${date.getMonth() + 1}월`
        if (month !== lastMonth) {
          labels.push({ month, weekIndex })
          lastMonth = month
        }
      }
    })

    return labels
  }

  const monthLabels = getMonthLabels()

  // 잔디 뷰 렌더링
  const renderGrassView = () => {
    // 각 주(week)의 너비 계산: 셀 너비(12px) + 셀 간 간격(2px)
    const cellSize = 12; // w-3 (12px)
    const gap = 2; // gap-0.5 (2px)
    const weekWidth = cellSize + gap; // 14px

    return (
      <div className='overflow-x-auto'>
        <div className='inline-block min-w-full'>
          {/* 월 레이블 */}
          <div className='flex mb-4 relative'>
            <div className='w-8' />
            <div className='relative pr-10' style={{ width: `${weeks.length * weekWidth + 40}px` }}>
              {monthLabels.map((label, index) => (
                <div
                  key={index}
                  className='absolute text-xs text-gray-500 font-medium whitespace-nowrap'
                  style={{
                    left: `${label.weekIndex * weekWidth}px`
                  }}
                >
                  {label.month}
                </div>
              ))}
            </div>
          </div>

          {/* 히트맵 그리드 */}
          <div className='flex gap-0.5'>
            {/* 요일 레이블 */}
            <div className='flex flex-col gap-0.5'>
              {dayLabels.map((label, index) => (
                <div key={index} className='w-8 h-3 flex items-center justify-start text-xs text-gray-500'>
                  {index % 2 === 1 ? label : ''}
                </div>
              ))}
            </div>

            {/* 날짜 그리드 */}
            <div className='flex gap-0.5'>
              {weeks.map((week, weekIndex) => (
                <div key={weekIndex} className='flex flex-col gap-0.5'>
                  {week.map((day, dayIndex) => {
                    if (day.date === '') {
                      return <div key={dayIndex} className='w-3 h-3 rounded-sm bg-transparent' />
                    }

                    const date = new Date(day.date)
                    const dateString = `${date.getMonth() + 1}/${date.getDate()}`

                    return (
                      <div
                        key={dayIndex}
                        className={`w-3 h-3 rounded-sm ${statusColors[day.status]} border border-gray-200 hover:ring-2 hover:ring-gray-400 transition-all cursor-pointer`}
                        title={`${dateString} - ${statusLabels[day.status]}`}
                      />
                    )
                  })}
                </div>
              ))}
            </div>
          </div>

          {/* 범례 */}
          <div className='flex items-center gap-3 mt-4 text-xs text-gray-600'>
            {(['NONE', 'WORD', 'SENTENCE', 'BOTH'] as DailyActivityStatus[]).map((status) => (
              <div key={status} className='flex items-center gap-1.5'>
                <div
                  className={`w-3 h-3 rounded-sm ${statusColors[status]} border border-gray-200`}
                />
                <span>{statusLabels[status]}</span>
              </div>
            ))}
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className='rounded-2xl border border-gray-200 bg-white py-8 px-5 hidden lg:block w-fit'>
      <div className='mb-4'>
        <h2 className='text-xl font-semibold text-gray-900'>일일 학습 활동</h2>
        <p className='text-sm text-gray-500'>최근 1년간의 학습 기록</p>
      </div>

      {renderGrassView()}
    </div>
  )
}

export default DailyActivityHeatmap
