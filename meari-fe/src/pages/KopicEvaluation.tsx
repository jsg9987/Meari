import { useState, useEffect, useRef, useCallback } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { Check, Mic, MicOff } from 'lucide-react'
import logoWhite from '../assets/images/common/logo-white.svg'
import {
    getKopicSentences,
    submitKopicAnswer,
    createKopicTotalReport,
    type KopicSentence,
} from '../api/kopic.api'
import { getThemes } from '../api/contents.api'

const KOPIC_SESSION_STORAGE_KEY = 'kopic_session'

type KopicSessionSnapshot = {
    themeId: number
    themeName: string
    themeImageUrl: string
    kopicTotalReportId: number
}

const saveKopicSession = (snapshot: KopicSessionSnapshot) => {
    sessionStorage.setItem(KOPIC_SESSION_STORAGE_KEY, JSON.stringify(snapshot))
}

const loadKopicSession = (): KopicSessionSnapshot | null => {
    try {
        const raw = sessionStorage.getItem(KOPIC_SESSION_STORAGE_KEY)
        if (!raw) return null
        const parsed = JSON.parse(raw) as KopicSessionSnapshot
        if (!parsed?.kopicTotalReportId) return null
        return parsed
    } catch {
        return null
    }
}

// 세션 데이터 전역 저장 (리포트 페이지 전달)
export const kopicSessionData = {
    themeId: 0,
    themeName: '',
    themeImageUrl: '',
    kopicTotalReportId: 0,
    sentences: [] as KopicSentence[],
    reportIds: [] as number[], // 각 문제별 reportId 저장
}

// 세션 데이터 초기화
const clearKopicSession = () => {
    kopicSessionData.themeId = 0
    kopicSessionData.themeName = ''
    kopicSessionData.themeImageUrl = ''
    kopicSessionData.kopicTotalReportId = 0
    kopicSessionData.sentences = []
    kopicSessionData.reportIds = []
    sessionStorage.removeItem(KOPIC_SESSION_STORAGE_KEY)
}

export default function KopicEvaluation() {
    const { themeId } = useParams<{ themeId: string }>()
    const navigate = useNavigate()

    // 질문 관련 상태
    const [sentences, setSentences] = useState<KopicSentence[]>([])
    const [currentIndex, setCurrentIndex] = useState(0)
    const [isLoading, setIsLoading] = useState(true)

    // 녹음 관련 상태
    const [isRecording, setIsRecording] = useState(false)
    const mediaRecorderRef = useRef<MediaRecorder | null>(null)
    const audioChunksRef = useRef<Blob[]>([])

    // 타이머 상태
    const [timeRemaining, setTimeRemaining] = useState(30)
    const [isTimerRunning, setIsTimerRunning] = useState(false)
    const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null)

    // 전환 안내 상태
    const [isTransitioning, setIsTransitioning] = useState(false)
    const [transitionMessage, setTransitionMessage] = useState('')

    const currentSentence = sentences[currentIndex]
    const totalSentences = sentences.length

    // 뒤로가기/새로고침 시 세션 초기화
    useEffect(() => {
        const handleBeforeUnload = () => {
            clearKopicSession()
        }

        const handlePopState = () => {
            clearKopicSession()
            navigate('/main', { replace: true })
        }

        window.addEventListener('beforeunload', handleBeforeUnload)
        window.addEventListener('popstate', handlePopState)

        window.history.pushState(null, '', window.location.href)

        return () => {
            window.removeEventListener('beforeunload', handleBeforeUnload)
            window.removeEventListener('popstate', handlePopState)
        }
    }, [navigate])

    // 질문/테마 데이터 로드
    useEffect(() => {
        const loadData = async () => {
            if (!themeId) return

            try {
                const numericThemeId = Number(themeId)
                const stored = loadKopicSession()
                const hasStoredSession =
                    stored &&
                    stored.themeId === numericThemeId &&
                    Number.isFinite(stored.kopicTotalReportId) &&
                    stored.kopicTotalReportId > 0

                if (hasStoredSession && stored) {
                    kopicSessionData.themeId = stored.themeId
                    kopicSessionData.themeName = stored.themeName
                    kopicSessionData.themeImageUrl = stored.themeImageUrl
                    kopicSessionData.kopicTotalReportId = stored.kopicTotalReportId
                } else {
                    clearKopicSession()
                }

                setSentences([])
                setCurrentIndex(0)
                setIsLoading(true)
                const totalReportRes = hasStoredSession
                    ? null
                    : await createKopicTotalReport(numericThemeId)
                const [sentencesRes, themesRes] = await Promise.all([
                    getKopicSentences(numericThemeId),
                    getThemes(),
                ])

                if (sentencesRes.data.success && sentencesRes.data.data) {
                    setSentences(sentencesRes.data.data)
                    kopicSessionData.sentences = sentencesRes.data.data
                }

                if (themesRes.data.success && themesRes.data.data) {
                    const foundTheme = themesRes.data.data.find(t => t.theme_id === Number(themeId))
                    if (foundTheme) {
                        kopicSessionData.themeId = foundTheme.theme_id
                        kopicSessionData.themeName = foundTheme.name
                        kopicSessionData.themeImageUrl = foundTheme.theme_url
                    }
                }

                if (totalReportRes?.data.success && totalReportRes.data.data) {
                    kopicSessionData.kopicTotalReportId = totalReportRes.data.data.kopic_total_report_id
                }

                if (kopicSessionData.kopicTotalReportId) {
                    saveKopicSession({
                        themeId: kopicSessionData.themeId,
                        themeName: kopicSessionData.themeName,
                        themeImageUrl: kopicSessionData.themeImageUrl,
                        kopicTotalReportId: kopicSessionData.kopicTotalReportId,
                    })
                }
            } catch (error) {
                console.error('Failed to load data:', error)
            } finally {
                setIsLoading(false)
            }
        }

        loadData()
    }, [themeId])

    // 녹음 시작
    const handleStartRecording = async () => {
        try {
            const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
            const mediaRecorder = new MediaRecorder(stream)
            mediaRecorderRef.current = mediaRecorder
            audioChunksRef.current = []

            mediaRecorder.ondataavailable = (event) => {
                if (event.data.size > 0) {
                    audioChunksRef.current.push(event.data)
                }
            }

            mediaRecorder.start()
            setIsRecording(true)
            setIsTimerRunning(true)
        } catch (error) {
            console.error('Failed to start recording:', error)
            alert('마이크 권한을 허용해주세요.')
        }
    }

    // 녹음 종료 및 분석 요청
    const handleStopRecording = useCallback(async (isAutoStop = false) => {
        if (!mediaRecorderRef.current || !currentSentence) return

        const mediaRecorder = mediaRecorderRef.current

        return new Promise<void>((resolve) => {
            mediaRecorder.onstop = async () => {
                const audioBlob = new Blob(audioChunksRef.current, { type: 'audio/wav' })

                // 분석 요청 및 reportId 저장
                try {
                    const result = await submitKopicAnswer(
                        kopicSessionData.kopicTotalReportId,
                        currentSentence.kopic_sentence_id,
                        audioBlob,
                        currentSentence.text_ko
                    )
                    // reportId 저장
                    kopicSessionData.reportIds[currentIndex] = result.kopic_report_id
                } catch (error) {
                    console.error('Failed to submit answer:', error)
                }

                mediaRecorder.stream.getTracks().forEach(track => track.stop())

                setIsRecording(false)
                setIsTimerRunning(false)
                setTimeRemaining(30)

                const isLastQuestion = currentIndex >= totalSentences - 1
                if (isLastQuestion) {
                    showTransition('학습이 완료되었습니다. 리포트를 생성 중입니다...', () => {
                        navigate('/kopic/report')
                    })
                } else {
                    const message = isAutoStop
                        ? '시간이 초과되었습니다. 다음 문제로 넘어갑니다.'
                        : '녹음이 완료되었습니다. 다음 문제로 넘어갑니다.'
                    showTransition(message, () => {
                        setCurrentIndex(prev => prev + 1)
                    })
                }

                resolve()
            }

            mediaRecorder.stop()
        })
    }, [currentSentence, currentIndex, totalSentences, navigate])

    // 타이머 로직
    useEffect(() => {
        if (isTimerRunning && timeRemaining > 0) {
            timerRef.current = setTimeout(() => {
                setTimeRemaining(prev => prev - 1)
            }, 1000)
        } else if (timeRemaining === 0 && isTimerRunning) {
            handleStopRecording(true)
        }

        return () => {
            if (timerRef.current) {
                clearTimeout(timerRef.current)
            }
        }
    }, [isTimerRunning, timeRemaining, handleStopRecording])

    // 전환 안내 표시
    const showTransition = (message: string, callback: () => void) => {
        setTransitionMessage(message)
        setIsTransitioning(true)

        setTimeout(() => {
            setIsTransitioning(false)
            setTransitionMessage('')
            callback()
        }, 1500)
    }

    // 타이머 진행률 계산
    const timerProgress = (timeRemaining / 30) * 100

    if (isLoading) {
        return (
            <div className="min-h-screen w-full bg-gray-50 flex items-center justify-center">
                <div className="text-center">
                    <div className="w-10 h-10 border-4 border-blue-600 border-t-transparent rounded-full animate-spin mx-auto mb-4" />
                    <p className="text-gray-600">문장을 불러오는 중...</p>
                </div>
            </div>
        )
    }

    if (!currentSentence) {
        return (
            <div className="min-h-screen w-full bg-gray-50 flex items-center justify-center">
                <div className="text-center">
                    <p className="text-gray-600 mb-4">문장을 찾을 수 없습니다.</p>
                    <button
                        onClick={() => {
                            clearKopicSession()
                            navigate('/main')
                        }}
                        className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
                    >
                        처음으로 돌아가기
                    </button>
                </div>
            </div>
        )
    }

    return (
        <div className="min-h-screen w-full">
            {/* 헤더 */}
            <header className="w-full h-[50px] bg-[var(--color-bg-root)]">
                <div className="mx-auto h-full w-full max-w-[75rem] flex items-center px-6">
                    <img src={logoWhite} alt="Meari" className="h-[16px]" />
                </div>
            </header>

            {/* 전환 안내 오버레이 */}
            {isTransitioning && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60">
                    <div className="bg-white rounded-xl px-8 py-6 shadow-2xl text-center max-w-sm mx-4">
                        <div className="w-8 h-8 border-4 border-blue-500 border-t-transparent rounded-full animate-spin mx-auto mb-4" />
                        <p className="text-base font-medium text-gray-900">{transitionMessage}</p>
                    </div>
                </div>
            )}

            {/* 사이드 인디케이터 */}
            <div
                className="fixed top-1/2 -translate-y-1/2 flex flex-col items-center justify-center gap-10 z-10"
                style={{ left: 'calc(50% + 34.1rem + 4rem)' }}
            >
                {Array.from({ length: totalSentences }, (_, index) => {
                    const isCompleted = index < currentIndex
                    const isCurrent = index === currentIndex

                    return (
                        <div
                            key={index}
                            className={`w-14 h-14 rounded-full flex items-center justify-center text-base font-semibold transition-all shadow-md ${
                                isCompleted
                                    ? 'bg-gray-400 text-white'
                                    : isCurrent
                                        ? 'bg-[#2D9CDB] text-white ring-4 ring-blue-100 scale-110'
                                        : 'bg-gray-200 text-white border border-gray-200'
                            }`}
                        >
                            {isCompleted ? <Check size={20} /> : index + 1}
                        </div>
                    )
                })}
            </div>

            {/* 메인 컨텐츠 */}
            <main
                className="mx-auto w-full max-w-[68.2rem] py-11"
                style={{ transform: 'scale(1.1)', transformOrigin: 'top center' }}
            >
                {/* 타이머 영역 */}
                <div className="relative mb-1 max-w-4xl mx-auto w-full h-8">
                    <div className="h-2 bg-gray-200 rounded-full overflow-hidden">
                        <div
                            className={`h-full transition-all duration-1000 ease-linear ${
                                isRecording
                                    ? timeRemaining <= 10
                                        ? 'bg-red-500'
                                        : 'bg-blue-500'
                                    : 'bg-gray-300'
                            }`}
                            style={{ width: isRecording ? `${timerProgress}%` : '100%' }}
                        />
                    </div>
                    <div
                        className={`absolute right-0 top-4 text-sm font-medium transition-opacity ${
                            isRecording
                                ? timeRemaining <= 10
                                    ? 'text-red-500 opacity-100'
                                    : 'text-gray-500 opacity-100'
                                : 'opacity-0'
                        }`}
                    >
                        {timeRemaining}초
                    </div>
                </div>

                {/* 질문 컨텐츠 영역 */}
                <div className="flex flex-col items-center justify-center max-w-5xl mx-auto w-full px-16">
                    {/* 문장 이미지 */}
                    <div className="w-full aspect-video bg-gray-100 rounded-xl overflow-hidden mb-6 shadow-lg flex items-center justify-center">
                        {currentSentence.kopic_picture_url ? (
                            <img
                                src={currentSentence.kopic_picture_url}
                                alt="KOPIC 상황 이미지"
                                className="w-full h-full object-contain"
                            />
                        ) : (
                            <p className="text-gray-400">이미지가 없습니다.</p>
                        )}
                    </div>

                    {/* 질문 텍스트 */}
                    <p className="text-center text-xl font-medium text-gray-900 mb-11 mt-6 px-4">
                        {currentSentence.text_ko}
                    </p>

                    {/* 녹음 버튼 / 녹음 UI */}
                    <div className="flex flex-col items-center">
                        {!isRecording ? (
                            <div className="flex flex-col items-center">
                                <button
                                    onClick={handleStartRecording}
                                    className="w-16 h-16 rounded-full flex items-center justify-center bg-green-500 text-white shadow-lg hover:bg-green-600 transition-all"
                                    title="클릭하여 녹음 시작"
                                >
                                    <Mic size={30} />
                                </button>
                                <p className="mt-3 mb-3 text-sm text-gray-500">클릭하여 녹음 시작</p>
                            </div>
                        ) : (
                            <div className="flex flex-col items-center">
                                <button
                                    onClick={() => handleStopRecording(false)}
                                    className="w-16 h-16 rounded-full flex items-center justify-center bg-red-500 text-white shadow-lg hover:bg-red-600 transition-all animate-pulse"
                                    title="클릭하여 녹음 종료"
                                >
                                    <MicOff size={30} />
                                </button>
                                <p className="mt-3 mb-3 text-sm text-gray-500">클릭하여 녹음 종료</p>
                            </div>
                        )}
                    </div>
                </div>

                {/* 하단 안내 메시지 */}
                <div className="text-center pt-2">
                    {!isRecording ? (
                        <p className="text-sm text-gray-400">
                            "녹음 시작하기"를 누르면 30초 타이머가 시작됩니다.
                        </p>
                    ) : (
                        <p className="text-sm text-gray-400">
                            녹음 중입니다. 버튼을 누르거나 시간이 초과되면 자동으로 종료됩니다.
                        </p>
                    )}
                </div>
            </main>
        </div>
    )
}
