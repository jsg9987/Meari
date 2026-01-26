import CreateRoomButton from './CreateRoomButton'

const ShadowingPanel = () => {
  return (
    <section className='rounded-2xl border border-white/10 bg-white/5 p-6'>
      <div className='flex items-center justify-between gap-4'>
        <div>
          <h2 className='text-xl font-semibold'>쉐도잉</h2>
          <p className='mt-2 text-sm'>
            오늘의 문장으로 쉐도잉 연습을 시작해보세요.
          </p>
        </div>
        <CreateRoomButton />
      </div>
    </section>
  )
}

export default ShadowingPanel
