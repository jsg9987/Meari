import { useEffect, useState } from 'react'
import { getDailyRecords, type DailyRecordActivity } from '../../api/mypage.api'

// completed_count에 따른 색상 강도
const getColorByCount = (count: number): string => {
  if (count === 0) return 'bg-gray-100'
  if (count === 1) return 'bg-blue-200'
  if (count === 2) return 'bg-blue-300'
  if (count === 3) return 'bg-blue-400'
  if (count >= 4) return 'bg-blue-500'
  return 'bg-gray-100'
}

const getCountLabel = (count: number): string => {
  if (count === 0) return '학습 안함'
  if (count === 1) return '단어 학습'
  if (count === 2) return '문장 학습'
  return '모두 학습'
}

interface DailyActivityHeatmapProps {
  className?: string
}

const DailyActivityHeatmap = ({ className }: DailyActivityHeatmapProps) => {
  const [activities, setActivities] = useState<DailyRecordActivity[]>([])
  const [dateRange, setDateRange] = useState<{ startDate: string; endDate: string } | null>(null)
  const [isLoading, setIsLoading] = useState(false)

  useEffect(() => {
    const loadData = async () => {
      setIsLoading(true)
      try {
        const response = await getDailyRecords('yearly')
        if (response.data.success && response.data.data) {
          setActivities(response.data.data.activities)
          setDateRange({
            startDate: response.data.data.startDate,
            endDate: response.data.data.endDate
          })
        }
      } catch (error) {
        console.error('Failed to load daily records:', error)
      } finally {
        setIsLoading(false)
      }
    }

    loadData()
  }, [])

  if (isLoading) {
    return (
      <div className={`rounded-2xl border border-gray-200 bg-white py-8 px-5 ${className || 'hidden lg:block w-fit'}`}>
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
  const getWeeksData = (acts: DailyRecordActivity[]) => {
    const weeks: (DailyRecordActivity | { date: ''; completed_count: 0 })[][] = []
    let currentWeek: (DailyRecordActivity | { date: ''; completed_count: 0 })[] = []

    // API에서 받은 날짜 범위 사용
    if (!dateRange) return weeks

    const startDate = new Date(dateRange.startDate)
    const endDate = new Date(dateRange.endDate)

    // 활동 데이터를 Map으로 변환 (빠른 조회)
    const activityMap = new Map<string, number>()
    acts.forEach((act) => {
      activityMap.set(act.date, act.completed_count)
    })

    // 시작 요일이 일요일이 아니면 앞부분을 빈 칸으로 채움
    const startDay = startDate.getDay()
    for (let i = 0; i < startDay; i++) {
      currentWeek.push({ date: '', completed_count: 0 })
    }

    // 모든 날짜를 순회하면서 데이터 채우기
    const currentDate = new Date(startDate)
    while (currentDate <= endDate) {
      const dateString = currentDate.toISOString().split('T')[0]
      const completedCount = activityMap.get(dateString) || 0

      currentWeek.push({ date: dateString, completed_count: completedCount })

      if (currentWeek.length === 7) {
        weeks.push(currentWeek)
        currentWeek = []
      }

      currentDate.setDate(currentDate.getDate() + 1)
    }

    // 마지막 주가 7일이 안 되면 빈 칸으로 채움
    if (currentWeek.length > 0) {
      while (currentWeek.length < 7) {
        currentWeek.push({ date: '', completed_count: 0 })
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
    // 각 주(week)의 너비 계산: 셀 너비(20px) + 셀 간 간격(4px)
    const cellSize = 20; // w-5 (20px)
    const gap = 4; // gap-1 (4px)
    const weekWidth = cellSize + gap; // 24px

    return (
      <div className='flex flex-col items-center overflow-x-auto pt-2'>
        <div className='inline-block'>
          {/* 월 레이블 */}
          <div className='flex mb-10 relative'>
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
          <div className='flex gap-1'>
            {/* 요일 레이블 */}
            <div className='flex flex-col gap-1'>
              {dayLabels.map((label, index) => (
                <div key={index} className='w-8 h-5 flex items-center justify-start text-xs text-gray-500'>
                  {index % 2 === 1 ? label : ''}
                </div>
              ))}
            </div>

            {/* 날짜 그리드 */}
            <div className='flex gap-1'>
              {weeks.map((week, weekIndex) => (
                <div key={weekIndex} className='flex flex-col gap-1'>
                  {week.map((day, dayIndex) => {
                    if (day.date === '') {
                      return <div key={dayIndex} className='w-5 h-5 rounded-sm bg-transparent' />
                    }

                    const date = new Date(day.date)
                    const dateString = `${date.getMonth() + 1}/${date.getDate()}`

                    return (
                      <div key={dayIndex} className='relative group'>
                        <div
                          className={`w-5 h-5 rounded-sm ${getColorByCount(day.completed_count)} border border-gray-200 hover:ring-2 hover:ring-gray-400 transition-all cursor-pointer`}
                        />
                        <div className='absolute hidden group-hover:block bg-gray-900/90 text-white text-xs px-2 py-1 rounded whitespace-nowrap -top-8 left-1/2 -translate-x-1/2 z-10 pointer-events-none'>
                          {dateString} - {getCountLabel(day.completed_count)}
                        </div>
                      </div>
                    )
                  })}
                </div>
              ))}
            </div>
          </div>

          {/* 범례 */}
          <div className='flex items-center justify-end gap-3 mt-4 text-xs text-gray-600'>
            {[
              { count: 0, label: '학습 안함' },
              { count: 1, label: '단어 학습' },
              { count: 2, label: '문장 학습' },
              { count: 3, label: '모두 학습' }
            ].map(({ count, label }) => (
              <div key={count} className='flex items-center gap-1.5'>
                <div
                  className={`w-5 h-5 rounded-sm ${getColorByCount(count)} border border-gray-200`}
                />
                <span>{label}</span>
              </div>
            ))}
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className={`rounded-2xl border border-gray-200 bg-white py-8 px-5 ${className || 'hidden lg:block w-fit'}`}>
      <div className='mb-4'>
        <h2 className='text-xl font-semibold text-gray-900'>일일 학습 활동</h2>
        <p className='text-sm text-gray-500'>최근 1년간의 학습 기록</p>
      </div>

      {renderGrassView()}
    </div>
  )
}

export default DailyActivityHeatmap
