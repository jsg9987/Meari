import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Cell
} from 'recharts'
import { transformSyllableData } from '../../../utils/reportUtils'

interface SyllableChartProps {
  syllables: string[]
  confidences: number[]
  sentenceIndex: number
}

const SyllableChart = ({
  syllables,
  confidences,
  sentenceIndex
}: SyllableChartProps) => {
  const data = transformSyllableData(syllables, confidences)

  if (data.length === 0) {
    return null
  }

  return (
    <div className='bg-gray-50 rounded-lg p-4 border border-gray-200'>
      <h4 className='text-sm font-semibold text-gray-900 mb-3'>
        문장 {sentenceIndex + 1} - 음절별 신뢰도
      </h4>
      <ResponsiveContainer width='100%' height={200}>
        <BarChart data={data}>
          <CartesianGrid strokeDasharray='3 3' stroke='#e5e7eb' />
          <XAxis
            dataKey='syllable'
            tick={{ fontSize: 11, fill: '#6b7280' }}
            tickLine={false}
          />
          <YAxis
            domain={[0, 100]}
            tick={{ fontSize: 11, fill: '#6b7280' }}
            tickLine={false}
            label={{
              value: '신뢰도 (%)',
              angle: -90,
              position: 'insideLeft',
              style: { fontSize: 11, fill: '#6b7280' }
            }}
          />
          <Tooltip
            contentStyle={{
              backgroundColor: '#fff',
              border: '1px solid #e5e7eb',
              borderRadius: '8px',
              fontSize: '12px'
            }}
            formatter={(value: number | undefined) => [`${value ?? 0}%`, '신뢰도']}
          />
          <Bar dataKey='confidence' radius={[8, 8, 0, 0]}>
            {data.map((entry, index) => (
              <Cell key={`cell-${index}`} fill={entry.fill} />
            ))}
          </Bar>
        </BarChart>
      </ResponsiveContainer>
    </div>
  )
}

export default SyllableChart
