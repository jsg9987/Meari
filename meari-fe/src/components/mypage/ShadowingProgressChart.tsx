import { useEffect, useState } from 'react'
import {
  ComposedChart,
  Line,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer
} from 'recharts'
import {
  getShadowingPracticeHistory,
  type ShadowingPracticeRecord
} from '../../api/mypage.api'

// 날짜 포맷 함수 (한글 형식)
const formatDate = (dateString: string) => {
  const date = new Date(dateString)
  const month = date.getMonth() + 1
  const day = date.getDate()
  return `${month}월 ${day}일`
}

// 커스텀 툴팁
interface CustomTooltipProps {
  active?: boolean
  payload?: Array<{
    name: string
    value: number
    color: string
    payload: ShadowingPracticeRecord
  }>
}

const CustomTooltip = ({ active, payload }: CustomTooltipProps) => {
  if (active && payload && payload.length) {
    return (
      <div className='bg-white border border-gray-200 rounded-lg p-3 shadow-lg'>
        <p className='text-xs text-gray-500 mb-2'>{payload[0].payload.date}</p>
        {payload.map((entry, index) => (
          <p key={index} className='text-sm' style={{ color: entry.color }}>
            <span className='font-semibold'>{entry.name}:</span> {entry.value}
            {entry.name === 'Errors' ? '개' : '%'}
          </p>
        ))}
      </div>
    )
  }
  return null
}

const ShadowingProgressChart = () => {
  const [data, setData] = useState<ShadowingPracticeRecord[]>([])
  const [isLoading, setIsLoading] = useState(false)

  useEffect(() => {
    const loadData = async () => {
      setIsLoading(true)
      try {
        const response = await getShadowingPracticeHistory()
        if (response.data.success && response.data.data) {
          // 날짜순으로 정렬 (오래된 순)
          const sortedData = [...response.data.data].sort(
            (a, b) => new Date(a.date).getTime() - new Date(b.date).getTime()
          )
          setData(sortedData)
        }
      } catch (error) {
        console.error('Failed to load practice history:', error)
      } finally {
        setIsLoading(false)
      }
    }

    loadData()
  }, [])

  if (isLoading) {
    return (
      <div className='rounded-2xl border border-gray-200 bg-white p-5'>
        <div className='mb-4'>
          <div className='h-7 w-40 bg-gray-200 rounded animate-pulse mb-2' />
          <div className='h-5 w-32 bg-gray-200 rounded animate-pulse' />
        </div>
        <div className='h-[400px] bg-gray-200 rounded-lg animate-pulse' />
      </div>
    )
  }

  if (data.length === 0) {
    return (
      <div className='rounded-2xl border border-gray-200 bg-white p-5'>
        <h2 className='text-xl font-semibold text-gray-900 mb-4'>쉐도잉 연습 기록</h2>
        <div className='flex justify-center items-center h-80 text-gray-500'>
          연습 기록이 없습니다
        </div>
      </div>
    )
  }

  return (
    <div className='rounded-2xl border border-gray-200 bg-white p-5'>
      <div className='mb-4'>
        <h2 className='text-xl font-semibold text-gray-900'>쉐도잉 연습 기록</h2>
        <p className='text-sm text-gray-500'>최근 5회 연습 결과</p>
      </div>

      <ResponsiveContainer width='100%' height={400}>
        <ComposedChart
          data={data}
          margin={{ top: 30, right: 80, left: 80, bottom: 20 }}
        >
          <CartesianGrid strokeDasharray='3 3' stroke='#f0f0f0' />

          {/* X축 - 날짜 */}
          <XAxis
            dataKey='date'
            tickFormatter={formatDate}
            tick={{ fontSize: 12, fill: '#6b7280' }}
            stroke='#d1d5db'
            label={{ value: '날짜', position: 'insideBottom', offset: -10, style: { fontSize: 12, fill: '#6b7280', fontWeight: 600 } }}
          />

          {/* 왼쪽 Y축 - Accuracy, Intonation (0-100) */}
          <YAxis
            yAxisId='left'
            domain={[0, 100]}
            tick={{ fontSize: 12, fill: '#6b7280' }}
            stroke='#d1d5db'
            label={{ value: '점수 (%)', angle: 0, position: 'insideLeft', style: { fontSize: 12, fill: '#6b7280', fontWeight: 600, textAnchor: 'middle' } }}
          />

          {/* 오른쪽 Y축 - Errors (0-10) */}
          <YAxis
            yAxisId='right'
            orientation='right'
            domain={[0, 10]}
            tick={{ fontSize: 12, fill: '#6b7280' }}
            stroke='#d1d5db'
            label={{ value: '오류 (개)', angle: 0, position: 'insideRight', style: { fontSize: 12, fill: '#6b7280', fontWeight: 600, textAnchor: 'middle' } }}
          />

          <Tooltip content={<CustomTooltip />} />

          <Legend
            wrapperStyle={{ paddingTop: '20px' }}
            iconType='line'
          />

          {/* Accuracy 라인 */}
          <Line
            yAxisId='left'
            type='monotone'
            dataKey='accuracy'
            name='정확도'
            stroke='#3b82f6'
            strokeWidth={3}
            dot={{ fill: '#3b82f6', strokeWidth: 2, r: 5 }}
            activeDot={{ r: 7 }}
            animationDuration={1500}
            animationEasing='ease-in-out'
          />

          {/* Intonation 라인 */}
          <Line
            yAxisId='left'
            type='monotone'
            dataKey='intonation'
            name='억양'
            stroke='#10b981'
            strokeWidth={3}
            dot={{ fill: '#10b981', strokeWidth: 2, r: 5 }}
            activeDot={{ r: 7 }}
            animationDuration={1500}
            animationEasing='ease-in-out'
          />

          {/* Errors 바 차트 */}
          <Bar
            yAxisId='right'
            dataKey='errorsCount'
            name='오류'
            fill='#ef4444'
            fillOpacity={0.6}
            barSize={30}
            animationDuration={1500}
            animationEasing='ease-in-out'
          />
        </ComposedChart>
      </ResponsiveContainer>
    </div>
  )
}

export default ShadowingProgressChart
