import { useState, useEffect } from 'react'
import MyActivityFeed from './MyActivityFeed'
import ShadowingProgressChart from './ShadowingProgressChart'
import KopicSummaryChart from './KopicSummaryChart'
import DailyActivityHeatmap from './DailyActivityHeatmap'

const Dashboard = () => {
  const [isInitialLoading, setIsInitialLoading] = useState(true)

  useEffect(() => {
    // 초기 로딩을 시뮬레이션 (실제로는 필요한 경우에만 사용)
    const timer = setTimeout(() => {
      setIsInitialLoading(false)
    }, 100)

    return () => clearTimeout(timer)
  }, [])

  if (isInitialLoading) {
    return (
      <div className='p-8 space-y-6'>
        <div>
          <div className='h-10 w-48 bg-gray-200 rounded animate-pulse mb-2' />
          <div className='h-6 w-80 bg-gray-200 rounded animate-pulse' />
        </div>

        <div className='grid grid-cols-1 lg:grid-cols-3 gap-6 lg:items-start'>
          <div className='lg:col-span-2 space-y-6'>
            {/* ShadowingProgressChart 스켈레톤 */}
            <div className='rounded-2xl border border-gray-200 bg-white p-5'>
              <div className='mb-4'>
                <div className='h-7 w-40 bg-gray-200 rounded animate-pulse mb-2' />
                <div className='h-5 w-32 bg-gray-200 rounded animate-pulse' />
              </div>
              <div className='h-[400px] bg-gray-200 rounded-lg animate-pulse' />
            </div>

            {/* KopicSummaryChart 스켈레톤 */}
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

            {/* DailyActivityHeatmap 스켈레톤 */}
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
          </div>

          {/* MyActivityFeed 스켈레톤 */}
          <div className='lg:self-stretch lg:max-h-full'>
            <div className='rounded-2xl border border-gray-200 bg-white p-5 flex flex-col lg:h-full lg:max-h-225'>
              <div className='mb-4 shrink-0'>
                <div className='h-7 w-20 bg-gray-200 rounded animate-pulse mb-2' />
                <div className='h-5 w-48 bg-gray-200 rounded animate-pulse' />
              </div>
              <div className='space-y-3 overflow-y-auto flex-1 min-h-0'>
                {[1, 2, 3, 4, 5].map((i) => (
                  <div key={i} className='border-l-4 border-gray-200 p-4 flex flex-col gap-3'>
                    <div className='h-6 w-24 bg-gray-200 rounded-full animate-pulse' />
                    <div className='space-y-2'>
                      <div className='h-4 w-full bg-gray-200 rounded animate-pulse' />
                      <div className='h-4 w-3/4 bg-gray-200 rounded animate-pulse' />
                      <div className='h-3 w-20 bg-gray-200 rounded animate-pulse mt-2' />
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className='p-8 space-y-6'>
      <div>
        <h1 className='text-3xl font-bold text-gray-900 mb-2'>Dashboard</h1>
        <p className='text-gray-500'>Track your recent learning activity.</p>
      </div>

      <div className='grid grid-cols-1 lg:grid-cols-3 gap-6 lg:items-start'>
        <div className='lg:col-span-2 space-y-6'>
          <ShadowingProgressChart />
          <KopicSummaryChart />
          <DailyActivityHeatmap />
        </div>
        <div className='lg:self-stretch lg:max-h-full'>
          <MyActivityFeed />
        </div>
      </div>
    </div>
  )
}

export default Dashboard
