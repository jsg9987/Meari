import MyActivityFeed from './MyActivityFeed'
import ShadowingProgressChart from './ShadowingProgressChart'

const Dashboard = () => {
  return (
    <div className='p-8 space-y-6'>
      <div>
        <h1 className='text-3xl font-bold text-gray-900 mb-2'>Dashboard</h1>
        <p className='text-gray-500'>Track your recent learning activity.</p>
      </div>

      <div className='grid grid-cols-1 lg:grid-cols-2 gap-6'>
        <ShadowingProgressChart />
        <MyActivityFeed />
      </div>
    </div>
  )
}

export default Dashboard
