import { useEffect, useState } from 'react'
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
  RadialBarChart,
  RadialBar,
  PolarAngleAxis
} from 'recharts'
import { getKopicSummary, type KopicSummaryData } from '../../api/mypage.api'

const KopicSummaryChart = () => {
  const [data, setData] = useState<KopicSummaryData | null>(null)
  const [isLoading, setIsLoading] = useState(false)

  useEffect(() => {
    const loadData = async () => {
      setIsLoading(true)
      try {
        const response = await getKopicSummary()
        if (response.data.success && response.data.data) {
          setData(response.data.data)
        }
      } catch (error) {
        console.error('Failed to load KOPIC summary:', error)
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
          <div className='h-5 w-64 bg-gray-200 rounded animate-pulse' />
        </div>
        <div className='grid grid-cols-1 lg:grid-cols-3 gap-6'>
          <div className='lg:col-span-1'>
            <div className='h-5 w-32 bg-gray-200 rounded animate-pulse mb-3 mx-auto' />
            <div className='h-64 bg-gray-200 rounded-lg animate-pulse' />
          </div>
          <div className='lg:col-span-2'>
            <div className='h-5 w-32 bg-gray-200 rounded animate-pulse mb-3 mx-auto' />
            <div className='h-64 bg-gray-200 rounded-lg animate-pulse' />
          </div>
        </div>
      </div>
    )
  }

  if (!data) {
    return (
      <div className='rounded-2xl border border-gray-200 bg-white p-5'>
        <h2 className='text-xl font-semibold text-gray-900 mb-4'>KOPIC 시험 요약</h2>
        <div className='flex justify-center items-center h-80 text-gray-500'>
          데이터가 없습니다
        </div>
      </div>
    )
  }

  const { best, average } = data

  if (!best?.sentences || !average?.sentences) {
    return (
      <div className='rounded-2xl border border-gray-200 bg-white p-5'>
        <h2 className='text-xl font-semibold text-gray-900 mb-4'>KOPIC 시험 요약</h2>
        <div className='flex justify-center items-center h-80 text-gray-500'>
          데이터가 없습니다
        </div>
      </div>
    )
  }

  // 문항별 점수 데이터 (막대 그래프용)
  const questionData = best.sentences.map((sentence, index) => {
    const avgSentence = average.sentences.find(
      (s) => s.kopic_sentence_id === sentence.kopic_sentence_id
    )
    return {
      question: `문항 ${index + 1}`,
      평균: avgSentence?.avg_score || 0,
      최고: sentence.score
    }
  })

  // 토탈 점수 데이터 (원 그래프용)
  const totalScoreData = [
    {
      name: '평균',
      value: average.total_avg_score,
      fill: '#3b82f6'
    },
    {
      name: '최고',
      value: best.total_avg_score,
      fill: '#10b981'
    }
  ]

  return (
    <div className='rounded-2xl border border-gray-200 bg-white p-5'>
      <div className='mb-4'>
        <h2 className='text-xl font-semibold text-gray-900'>KOPIC 시험 요약</h2>
        <p className='text-sm text-gray-500'>
          최고 점수 기록일: {best.exam_date}
        </p>
      </div>

      <div className='grid grid-cols-1 lg:grid-cols-3 gap-6'>
        {/* 토탈 점수 - 원 그래프 */}
        <div className='lg:col-span-1'>
          <h3 className='text-sm font-semibold text-gray-700 mb-3 text-center'>전체 평균 점수</h3>
          <ResponsiveContainer width='100%' height={250}>
            <RadialBarChart
              cx='50%'
              cy='50%'
              innerRadius='30%'
              outerRadius='90%'
              data={totalScoreData}
              startAngle={90}
              endAngle={-270}
            >
              <PolarAngleAxis type='number' domain={[0, 100]} angleAxisId={0} tick={false} />
              <RadialBar
                background
                dataKey='value'
                cornerRadius={10}
                label={{
                  position: 'insideStart',
                  fill: '#fff',
                  fontSize: 14,
                  fontWeight: 600,
                  formatter: (value: any) => (typeof value === 'number' ? `${value}점` : '')
                }}
              />
              <Legend
                iconType='circle'
                layout='horizontal'
                verticalAlign='bottom'
                align='center'
                wrapperStyle={{ paddingTop: '20px' }}
              />
              <Tooltip
                formatter={(value: any) => [typeof value === 'number' ? `${value}점` : '', '']}
                contentStyle={{
                  backgroundColor: 'white',
                  border: '1px solid #e5e7eb',
                  borderRadius: '8px',
                  padding: '8px 12px'
                }}
              />
            </RadialBarChart>
          </ResponsiveContainer>
        </div>

        {/* 문항별 점수 - 막대 그래프 */}
        <div className='lg:col-span-2'>
          <h3 className='text-sm font-semibold text-gray-700 mb-3 text-center'>문항별 점수</h3>
          <ResponsiveContainer width='100%' height={250}>
            <BarChart
              data={questionData}
              margin={{ top: 20, right: 30, left: 20, bottom: 20 }}
            >
              <CartesianGrid strokeDasharray='3 3' stroke='#f0f0f0' />
              <XAxis
                dataKey='question'
                tick={{ fontSize: 12, fill: '#6b7280' }}
                stroke='#d1d5db'
              />
              <YAxis
                domain={[0, 100]}
                tick={{ fontSize: 12, fill: '#6b7280' }}
                stroke='#d1d5db'
                label={{
                  value: '점수',
                  angle: 0,
                  position: 'insideLeft',
                  style: { fontSize: 12, fill: '#6b7280', fontWeight: 600, textAnchor: 'middle' }
                }}
              />
              <Tooltip
                contentStyle={{
                  backgroundColor: 'white',
                  border: '1px solid #e5e7eb',
                  borderRadius: '8px',
                  padding: '8px 12px'
                }}
                formatter={(value: any) => (typeof value === 'number' ? `${value}점` : '')}
              />
              <Legend wrapperStyle={{ paddingTop: '10px' }} />
              <Bar dataKey='평균' fill='#3b82f6' radius={[4, 4, 0, 0]} />
              <Bar dataKey='최고' fill='#10b981' radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>
    </div>
  )
}

export default KopicSummaryChart
