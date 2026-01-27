import { useState } from 'react'

type PasswordModalProps = {
    isOpen: boolean
    onClose: () => void
    onSubmit: (password: string) => void
}

const PasswordModal = ({ isOpen, onClose, onSubmit }: PasswordModalProps) => {
    const [password, setPassword] = useState('')

    if (!isOpen) return null

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

    return (
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
                    className='w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-[#2563EB] focus:border-transparent mb-4'
                    autoFocus
                />
                <button
                    type='button'
                    onClick={handleSubmit}
                    className='w-full bg-gray-200 hover:bg-gray-300 text-gray-700 text-sm py-2 rounded-md transition-colors'
                >
                    비밀번호 입력
                </button>
            </div>
        </div>
    )
}

export default PasswordModal
