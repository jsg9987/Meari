import { PieChart, Pie, Cell, Tooltip, Legend, ResponsiveContainer } from 'recharts'
import type { ShadowingReportDetail } from '../../../api/mypage.api'
import { aggregateErrorTypes } from '../../../utils/reportUtils'

interface ErrorPieChartProps {
  detail: ShadowingReportDetail
}

const ErrorPieChart = ({ detail }: ErrorPieChartProps) => {
  const data = aggregateErrorTypes(detail)

  if (data.length === 0) {
    return (
      <div className='bg-green-50 rounded-lg p-6 border border-green-200 text-center'>
        <p className='text-green-800 font-semibold'>🎉 오류가 없습니다!</p>
        <p className='text-green-600 text-sm mt-1'>완벽한 발음이에요!</p>
      </div>
    )
  }

  const RADIAN = Math.PI / 180
  const renderCustomizedLabel = ({
    cx,
    cy,
    midAngle,
    innerRadius,
    outerRadius,
    percent
  }: {
    cx?: number
    cy?: number
    midAngle?: number
    innerRadius?: number
    outerRadius?: number
    percent?: number
  }) => {
    if (
      cx === undefined ||
      cy === undefined ||
      midAngle === undefined ||
      innerRadius === undefined ||
      outerRadius === undefined ||
      percent === undefined
    ) {
      return null
    }

    const radius = innerRadius + (outerRadius - innerRadius) * 0.5
    const x = cx + radius * Math.cos(-midAngle * RADIAN)
    const y = cy + radius * Math.sin(-midAngle * RADIAN)

    return (
      <text
        x={x}
        y={y}
        fill='white'
        textAnchor={x > cx ? 'start' : 'end'}
        dominantBaseline='central'
        fontSize='14'
        fontWeight='bold'
      >
        {`${(percent * 100).toFixed(0)}%`}
      </text>
    )
  }

  return (
    <div className='bg-white rounded-lg p-5 border border-gray-200'>
      <h3 className='font-semibold text-gray-900 mb-4 text-lg'>오류 타입 분포</h3>
      <ResponsiveContainer width='100%' height={300}>
        <PieChart>
          <Pie
            data={data}
            cx='50%'
            cy='50%'
            labelLine={false}
            label={renderCustomizedLabel}
            outerRadius={100}
            dataKey='value'
          >
            {data.map((entry, index) => (
              <Cell key={`cell-${index}`} fill={entry.fill} />
            ))}
          </Pie>
          <Tooltip
            contentStyle={{
              backgroundColor: '#fff',
              border: '1px solid #e5e7eb',
              borderRadius: '8px',
              fontSize: '12px'
            }}
            formatter={(value: number | undefined, name: string | undefined) => [
              `${value ?? 0}개`,
              name ?? ''
            ]}
          />
          <Legend
            verticalAlign='bottom'
            height={36}
            wrapperStyle={{
              fontSize: '13px',
              fontWeight: '600'
            }}
          />
        </PieChart>
      </ResponsiveContainer>
    </div>
  )
}

export default ErrorPieChart
