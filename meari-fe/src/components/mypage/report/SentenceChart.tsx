import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer
} from 'recharts'
import type { ShadowingReportDetail } from '../../../api/mypage.api'
import { transformSentenceData } from '../../../utils/reportUtils'

interface SentenceChartProps {
  detail: ShadowingReportDetail
}

const SentenceChart = ({ detail }: SentenceChartProps) => {
  const data = transformSentenceData(detail)

  if (data.length === 0) {
    return null
  }

  return (
    <div className='bg-white rounded-lg p-5 border border-gray-200'>
      <h3 className='font-semibold text-gray-900 mb-4 text-lg'>문장별 점수 비교</h3>
      <ResponsiveContainer width='100%' height={300}>
        <BarChart data={data}>
          <CartesianGrid strokeDasharray='3 3' stroke='#e5e7eb' />
          <XAxis
            dataKey='name'
            tick={{ fontSize: 11, fill: '#6b7280' }}
            tickLine={false}
          />
          <YAxis
            domain={[0, 100]}
            tick={{ fontSize: 11, fill: '#6b7280' }}
            tickLine={false}
          />
          <Tooltip
            contentStyle={{
              backgroundColor: '#fff',
              border: '1px solid #e5e7eb',
              borderRadius: '8px',
              fontSize: '12px'
            }}
            labelStyle={{ fontWeight: 'bold', marginBottom: '4px' }}
          />
          <Legend
            wrapperStyle={{
              paddingTop: '10px',
              fontSize: '12px'
            }}
          />
          <Bar dataKey='accuracy' fill='#10b981' name='정확도 (%)' radius={[8, 8, 0, 0]} />
          <Bar
            dataKey='confidence'
            fill='#3b82f6'
            name='신뢰도 (%)'
            radius={[8, 8, 0, 0]}
          />
          <Bar dataKey='intonation' fill='#14b8a6' name='억양 (%)' radius={[8, 8, 0, 0]} />
        </BarChart>
      </ResponsiveContainer>
    </div>
  )
}

export default SentenceChart
