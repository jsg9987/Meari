import {
  RadialBarChart,
  RadialBar,
  Legend,
  ResponsiveContainer,
  PolarAngleAxis
} from 'recharts'

interface ScoreRadialChartProps {
  accuracy: number
  intonation: number
  totalScore: number
}

const ScoreRadialChart = ({
  accuracy,
  intonation,
  totalScore
}: ScoreRadialChartProps) => {
  const data = [
    {
      name: '총점',
      value: totalScore,
      fill: '#8b5cf6' // purple
    },
    {
      name: '억양',
      value: intonation,
      fill: '#14b8a6' // teal
    },
    {
      name: '정확도',
      value: accuracy,
      fill: '#10b981' // green
    }
  ]

  return (
    <div className='bg-gradient-to-br from-purple-50 to-indigo-50 rounded-lg p-6 border border-purple-200'>
      <h3 className='font-semibold text-gray-900 mb-4 text-lg text-center'>전체 점수 분석</h3>
      <ResponsiveContainer width='100%' height={300}>
        <RadialBarChart
          cx='50%'
          cy='50%'
          innerRadius='10%'
          outerRadius='90%'
          data={data}
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
              fontWeight: 'bold',
              formatter: (value: unknown) => `${value ?? 0}점`
            }}
          />
          <Legend
            iconSize={12}
            layout='horizontal'
            verticalAlign='bottom'
            align='center'
            wrapperStyle={{
              paddingTop: '20px',
              fontSize: '13px',
              fontWeight: '600'
            }}
          />
        </RadialBarChart>
      </ResponsiveContainer>
    </div>
  )
}

export default ScoreRadialChart
