import { createPortal } from 'react-dom'

type ThemeConfirmModalProps = {
    isOpen: boolean
    themeName: string
    onClose: () => void
    onConfirm: () => void
}

const ThemeConfirmModal = ({ isOpen, themeName, onClose, onConfirm }: ThemeConfirmModalProps) => {
    if (!isOpen) return null

    return createPortal(
        <div
            className='fixed inset-0 bg-black/50 flex items-center justify-center z-50'
            onClick={onClose}
        >
            <div
                className='bg-white rounded-xl p-6 w-[400px] h-[250px] shadow-xl text-center flex flex-col justify-between'
                onClick={(e) => e.stopPropagation()}
            >
                <div className='mt-7'>
                    <h2 className='text-[24px] font-bold text-black mb-3'>{themeName}</h2>
                    <p className='text-[18px] text-gray-700 font-medium'>정말 이 테마로 시작하시겠어요?</p>
                </div>

                <div className='flex justify-center gap-6 w-full mb-2'>
                    <button
                        type='button'
                        onClick={onClose}
                        className='flex-2 px-6 py-2 border border-black bg-white text-black text-md font-semibold transition-colors hover:bg-gray-50'
                    >
                        아니요
                    </button>
                    <button
                        type='button'
                        onClick={onConfirm}
                        className='flex-2 px-6 py-2 bg-[#D9D9D9] text-black text-md font-semibold transition-colors hover:bg-gray-300'
                    >
                        예
                    </button>
                </div>
            </div>
        </div>,
        document.body
    )
}

export default ThemeConfirmModal
