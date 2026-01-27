import { useEffect, useRef } from 'react'

type ProfileDropdownProps = {
    onClose: () => void
}

const ProfileDropdown = ({ onClose }: ProfileDropdownProps) => {
    const dropdownRef = useRef<HTMLDivElement>(null)

    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
                onClose()
            }
        }
        document.addEventListener('mousedown', handleClickOutside)
        return () => document.removeEventListener('mousedown', handleClickOutside)
    }, [onClose])

    const handleLogout = () => {
        // TODO: 로그아웃 로직
        onClose()
    }

    const handleMyPage = () => {
        // TODO: 마이페이지 이동
        onClose()
    }

    return (
        <div
            ref={dropdownRef}
            className='absolute right-0 top-14 w-[220px] bg-[#1a2332] rounded-xl shadow-lg overflow-hidden z-50'
        >
            {/* 로그아웃 */}
            <button
                type='button'
                className='w-full flex items-center gap-3 px-4 py-3 text-white/70 hover:bg-white/5 transition-colors'
                onClick={handleLogout}
            >
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path>
                    <polyline points="16 17 21 12 16 7"></polyline>
                    <line x1="21" y1="12" x2="9" y2="12"></line>
                </svg>
                <span className='text-sm'>로그아웃</span>
            </button>

            {/* 마이페이지 */}
            <button
                type='button'
                className='w-full flex items-center gap-3 px-4 py-3 text-white bg-[#2563EB] transition-colors'
                onClick={handleMyPage}
            >
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <circle cx="12" cy="8" r="4"></circle>
                    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                </svg>
                <span className='text-sm'>마이페이지</span>
            </button>

            {/* 사용자 정보 */}
            <div className='flex items-center gap-3 px-4 py-4 border-t border-white/10'>
                <div className='w-10 h-10 rounded-full overflow-hidden border-2 border-yellow-500 shrink-0'>
                    <img
                        src="https://api.dicebear.com/7.x/avataaars/svg?seed=user"
                        alt="프로필"
                        className='w-full h-full object-cover bg-gray-600'
                    />
                </div>
                <div className='flex-1 min-w-0'>
                    <div className='flex items-center gap-1'>
                        <span className='text-white text-sm font-medium truncate'>김선엽문</span>
                        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                            <polyline points="6 9 12 15 18 9"></polyline>
                        </svg>
                    </div>
                    <span className='text-white/50 text-xs'>UI / UX Developer</span>
                </div>
            </div>
        </div>
    )
}

export default ProfileDropdown
