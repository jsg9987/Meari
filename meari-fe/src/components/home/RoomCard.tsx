type RoomCardProps = {
    roomId: number | string
    title: string
    currentPeople: number
    hasPassword: boolean
    contentTitle?: string
    onClick: () => void
}

const RoomCard = ({ title, currentPeople, hasPassword, contentTitle, onClick }: RoomCardProps) => {
    return (
        <div
            className='rounded-lg overflow-hidden bg-white cursor-pointer shadow-lg hover:shadow-xl hover:scale-[1.02] hover:ring-2 hover:ring-[#2D9CDB] transition-all duration-300'
            onClick={onClick}
        >
            {/* 썸네일 영역 - 빈 플레이스홀더 */}
            <div className='relative aspect-5/4 bg-[#1e3a5f]'>
                {/* 상단 라벨 영역 */}
                <div className='absolute top-[13px] left-[13px] right-[13px]'>
                    <div className='inline-block bg-[#2D9CDB] text-white text-[13px] px-[9px] py-[4px] rounded'>
                        {contentTitle || '쉐도잉'}
                    </div>
                </div>

                {/* 플레이스홀더 이미지 영역 */}
                <div className='absolute bottom-0 right-0 w-[106px] h-[106px] opacity-30'>
                    {/* 이미지 플레이스홀더 - 추후 테마 썸네일로 대체 */}
                </div>
            </div>

            {/* 하단 정보 영역 */}
            <div className='p-[12px] bg-white'>
                <h3 className='text-[13px] font-medium text-gray-600 truncate mb-2'>
                    {title}
                </h3>
                <div className='flex items-center justify-between text-[13px] text-gray-500'>
                    <div className='flex items-center gap-2'>
                        {/* 참여 인원 */}
                        <div className='flex items-center gap-1'>
                            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                                <circle cx="9" cy="7" r="4"></circle>
                                <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
                                <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
                            </svg>
                            <span>{currentPeople}</span>
                        </div>

                        {/* 잠금 아이콘 */}
                        {hasPassword && (
                            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
                                <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
                            </svg>
                        )}
                    </div>

                    {/* 영상 길이 + 섹션 수 - 비워둠 */}
                    <div className='text-gray-400'>
                        {/* 추후 구현 */}
                    </div>
                </div>
            </div>
        </div>
    )
}

export default RoomCard
