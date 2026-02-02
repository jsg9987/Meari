import MyActivityFeed from './MyActivityFeed'

const Dashboard = () => {
  return (
    <div className='p-8 space-y-6'>
      <div>
        <h1 className='text-3xl font-bold text-gray-900 mb-2'>Dashboard</h1>
        <p className='text-gray-500'>Track your recent learning activity.</p>
      </div>

      <MyActivityFeed />
    </div>
  )
}

export default Dashboard
