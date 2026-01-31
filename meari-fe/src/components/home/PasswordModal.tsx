import { useState, useEffect } from 'react'
import { createPortal } from 'react-dom'

type PasswordModalProps = {
    isOpen: boolean
    onClose: () => void
    onSubmit: (password: string) => void
}

const PasswordModal = ({ isOpen, onClose, onSubmit }: PasswordModalProps) => {
    const [password, setPassword] = useState('')

    const handleSubmit = () => {
        if (password.trim()) {
            onSubmit(password)
            setPassword('')
        }
    }

    const handleKeyDown = (e: React.KeyboardEvent) => {
        if (e.key === 'Enter') {
            handleSubmit()
        }
    }

    // 모달이 열릴 때 스크롤 방지
    useEffect(() => {
        if (isOpen) {
            const scrollBarWidth = window.innerWidth - document.documentElement.clientWidth;
            document.body.style.overflow = 'hidden';
            // 스크롤바가 사라지면서 화면이 밀리는 것 방지
            document.body.style.paddingRight = `${scrollBarWidth}px`;
        } else {
            document.body.style.overflow = '';
            document.body.style.paddingRight = '';
        }

        return () => {
            document.body.style.overflow = '';
            document.body.style.paddingRight = '';
        }
    }, [isOpen])

    if (!isOpen) return null

    return createPortal(
        <div
            className='fixed inset-0 bg-black/50 flex items-center justify-center z-50'
            onClick={onClose}
        >
            <div
                className='bg-white rounded-lg p-6 w-[280px] shadow-xl'
                onClick={(e) => e.stopPropagation()}
            >
                <input
                    type='password'
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    onKeyDown={handleKeyDown}
                    placeholder=''
                    className='w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-[#2D9CDB] focus:border-transparent mb-4'
                    autoFocus
                />
                <button
                    type='button'
                    onClick={handleSubmit}
                    className='w-full bg-[#2D9CDB] hover:bg-[#2789c2] text-white text-sm py-2 rounded-md transition-colors font-medium'
                >
                    비밀번호 입력
                </button>
            </div>
        </div>,
        document.body
    )
}

export default PasswordModal
