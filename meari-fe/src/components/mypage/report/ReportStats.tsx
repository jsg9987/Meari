import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer
} from 'recharts'
import type { ShadowingReport } from '../../../api/mypage.api'
import { calculateReportStats, getScoreColorClass } from '../../../utils/reportUtils'

interface ReportStatsProps {
  reports: ShadowingReport[]
}

const ReportStats = ({ reports }: ReportStatsProps) => {
  const { totalReports, averageScore, unreadCount, scoresTrend } =
    calculateReportStats(reports)

  if (reports.length === 0) {
    return null
  }

  return (
    <div className='mb-4 bg-gradient-to-br from-blue-50 to-indigo-50 rounded-lg p-5 border border-blue-200'>
      {/* 통계 카드 */}
      <div className='grid grid-cols-3 gap-4 mb-5'>
        <div className='bg-white rounded-lg p-4 text-center shadow-sm border border-blue-100'>
          <p className='text-xs text-gray-500 mb-1'>전체 리포트</p>
          <p className='text-2xl font-bold text-blue-600'>{totalReports}개</p>
        </div>
        <div className='bg-white rounded-lg p-4 text-center shadow-sm border border-blue-100'>
          <p className='text-xs text-gray-500 mb-1'>평균 점수</p>
          <p className={`text-2xl font-bold ${getScoreColorClass(averageScore)}`}>
            {averageScore}점
          </p>
        </div>
        <div className='bg-white rounded-lg p-4 text-center shadow-sm border border-blue-100'>
          <p className='text-xs text-gray-500 mb-1'>읽지 않음</p>
          <p className='text-2xl font-bold text-red-600'>{unreadCount}개</p>
        </div>
      </div>

      {/* 점수 추이 차트 */}
      {scoresTrend.length > 1 && (
        <div className='bg-white rounded-lg p-4 shadow-sm border border-blue-100'>
          <h3 className='text-sm font-semibold text-gray-900 mb-3'>최근 점수 추이</h3>
          <ResponsiveContainer width='100%' height={150}>
            <AreaChart data={scoresTrend}>
              <defs>
                <linearGradient id='colorScore' x1='0' y1='0' x2='0' y2='1'>
                  <stop offset='5%' stopColor='#3b82f6' stopOpacity={0.3} />
                  <stop offset='95%' stopColor='#3b82f6' stopOpacity={0} />
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray='3 3' stroke='#e5e7eb' />
              <XAxis
                dataKey='date'
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
              <Area
                type='monotone'
                dataKey='score'
                stroke='#3b82f6'
                strokeWidth={2}
                fill='url(#colorScore)'
                name='점수'
              />
            </AreaChart>
          </ResponsiveContainer>
        </div>
      )}
    </div>
  )
}

export default ReportStats
