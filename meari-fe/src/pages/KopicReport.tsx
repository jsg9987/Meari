import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { ChevronDown, ChevronUp } from 'lucide-react'
import { kopicSessionData } from './KopicEvaluation'
import { getKopicTotalReport } from '../api/kopic.api'
import type { KopicReportItem, KopicTotalReportItem } from '../api/kopic.api'
import logoWhite from '../assets/images/common/logo-white.svg'

const KOPIC_SESSION_STORAGE_KEY = 'kopic_session'

type KopicSessionSnapshot = {
    themeId: number
    themeName: string
    themeImageUrl: string
    kopicTotalReportId: number
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

const CircularProgress = ({ score, size = 200 }: { score: number; size?: number }) => {
    const [displayScore, setDisplayScore] = useState(0)

    useEffect(() => {
        const timer = setTimeout(() => {
            setDisplayScore(score)
        }, 100)
        return () => clearTimeout(timer)
    }, [score])

    const strokeWidth = 12
    const radius = (size - strokeWidth) / 2
    const circumference = 2 * Math.PI * radius
    const progress = (displayScore / 100) * circumference
    const offset = circumference - progress

    return (
        <div className="relative" style={{ width: size, height: size }}>
            <svg
                width={size}
                height={size}
                viewBox={`0 0 ${size} ${size}`}
                style={{ transform: 'rotate(-90deg) scaleY(-1)' }}
            >
                {/* 배경 원 */}
                <circle
                    cx={size / 2}
                    cy={size / 2}
                    r={radius}
                    fill="none"
                    stroke="#e5e7eb"
                    strokeWidth={strokeWidth}
                />
                {/* 진행 원 */}
                <circle
                    cx={size / 2}
                    cy={size / 2}
                    r={radius}
                    fill="none"
                    stroke="#2D9CDB"
                    strokeWidth={strokeWidth}
                    strokeLinecap="round"
                    strokeDasharray={circumference}
                    strokeDashoffset={offset}
                    className="transition-all duration-1000 ease-out"
                />
            </svg>
            {/* 점수 텍스트 */}
            <div className="absolute inset-0 flex items-center justify-center">
                <span className="text-4xl font-bold text-gray-900">{score}점</span>
            </div>
        </div>
    )
}

// 로딩 스켈레톤
const ReportItemSkeleton = () => (
    <div className="border border-gray-200 rounded-lg overflow-hidden animate-pulse">
        <div className="px-4 py-4 bg-gray-50">
            <div className="flex items-center justify-between">
                <div className="h-5 bg-gray-200 rounded w-64" />
                <div className="h-5 bg-gray-200 rounded w-8" />
            </div>
        </div>
    </div>
)

// 리포트 아이템
const ReportItem = ({
    item,
    index,
    isExpanded,
    onToggle,
}: {
    item: KopicReportItem
    index: number
    isExpanded: boolean
    onToggle: () => void
}) => {
    return (
        <div className="border border-gray-200 rounded-lg overflow-hidden">
            {/* 헤더 (클릭하여 펼치기) */}
            <button
                onClick={onToggle}
                className="w-full px-4 py-4 bg-gray-50 hover:bg-gray-100 transition-colors flex items-center justify-between text-left"
            >
                <span className="text-gray-700 font-medium">
                    문제 {index + 1}: {item.text_ko}
                </span>
                {isExpanded ? (
                    <ChevronUp size={20} className="text-gray-400" />
                ) : (
                    <ChevronDown size={20} className="text-gray-400" />
                )}
            </button>

            {/* 상세 내용 */}
            {isExpanded && (
                <div className="px-4 py-4 bg-white border-t border-gray-100">
                    {/* 사용한 발화 */}
                    {item.user_answer && (
                        <div className="mb-4">
                            <p className="text-sm text-gray-600 mb-1">
                                <strong>발화:</strong> {item.user_answer}
                            </p>
                        </div>
                    )}

                    {item.detailed_analysis ? (
                        <div className="space-y-3">
                            <div className="bg-gray-50 p-3 rounded-lg">
                                <p className="text-sm font-medium text-gray-900 mb-2">문장별 피드백</p>

                                <div className="space-y-2 text-sm text-gray-700">
                                    <div>
                                        <span className="font-medium">발음 문제점:</span>{' '}
                                        <span className="text-red-600">{item.detailed_analysis.missed_point}</span>
                                    </div>
                                    <div>
                                        <span className="font-medium">교정 표현:</span>{' '}
                                        <span className="text-blue-600">"{item.detailed_analysis.correction}"</span>
                                    </div>
                                    <div>
                                        <span className="font-medium">팁:</span>{' '}
                                        <span className="text-green-600">{item.detailed_analysis.tip}</span>
                                    </div>
                                </div>
                            </div>

                            {/* 점수 표시 */}
                            <div className="flex gap-4 text-sm">
                                <span className="text-gray-600">
                                    정확도: <strong className="text-gray-900">{item.accuracy ?? '-'}</strong>
                                </span>
                                <span className="text-gray-600">
                                    억양: <strong className="text-gray-900">{item.intonation ?? '-'}</strong>
                                </span>
                            </div>
                        </div>
                    ) : (
                        <p className="text-sm text-gray-500">분석 중입니다.</p>
                    )}
                </div>
            )}
        </div>
    )
}

export default function KopicReport() {
    const navigate = useNavigate()
    const [reportItems, setReportItems] = useState<(KopicReportItem | null)[]>([])
    const [expandedIndex, setExpandedIndex] = useState<number>(0)
    const [averageScore, setAverageScore] = useState(0)
    const [progressMessage, setProgressMessage] = useState<string | null>(null)
    const [isSessionReady, setIsSessionReady] = useState(false)

    const [session, setSession] = useState<KopicSessionSnapshot>(() => ({
        themeId: kopicSessionData.themeId,
        themeName: kopicSessionData.themeName,
        themeImageUrl: kopicSessionData.themeImageUrl,
        kopicTotalReportId: kopicSessionData.kopicTotalReportId,
    }))

    // 세션 데이터 복구
    useEffect(() => {
        if (kopicSessionData.kopicTotalReportId) {
            setSession({
                themeId: kopicSessionData.themeId,
                themeName: kopicSessionData.themeName,
                themeImageUrl: kopicSessionData.themeImageUrl,
                kopicTotalReportId: kopicSessionData.kopicTotalReportId,
            })
            setIsSessionReady(true)
            return
        }

        const stored = loadKopicSession()
        if (stored) {
            kopicSessionData.themeId = stored.themeId
            kopicSessionData.themeName = stored.themeName
            kopicSessionData.themeImageUrl = stored.themeImageUrl
            kopicSessionData.kopicTotalReportId = stored.kopicTotalReportId
            setSession(stored)
        }
        setIsSessionReady(true)
    }, [])

    const { themeId, themeName, themeImageUrl, kopicTotalReportId } = session
    const safeThemeImageUrl = themeImageUrl?.startsWith('http') ? themeImageUrl : ''

    const clearSessionAndGoHome = () => {
        kopicSessionData.themeId = 0
        kopicSessionData.themeName = ''
        kopicSessionData.themeImageUrl = ''
        kopicSessionData.kopicTotalReportId = 0
        kopicSessionData.sentences = []
        sessionStorage.removeItem(KOPIC_SESSION_STORAGE_KEY)
        navigate('/')
    }

    // 비동기 분석 결과 조회
    useEffect(() => {
        if (!isSessionReady) {
            return
        }
        if (!themeId) {
            navigate('/')
            return
        }

        if (!kopicTotalReportId) {
            setProgressMessage('세션 정보를 찾을 수 없습니다. 다시 시작해주세요.')
            return
        }

        let cancelled = false
        let intervalId: ReturnType<typeof setInterval> | null = null

        const mapTotalReportItem = (item: KopicTotalReportItem): KopicReportItem => ({
            kopic_report_id: item.kopic_report_id,
            status: 'COMPLETED',
            kopic_sentence_id: item.kopic_sentence_id,
            text_ko: item.text_ko,
            user_answer: '',
            audio_url: '',
            accuracy: item.accuracy,
            intonation: item.intonation,
            detailed_analysis: item.detailed_analysis,
        })

        const fetchTotalReport = async () => {
            try {
                const response = await getKopicTotalReport(kopicTotalReportId)
                if (cancelled) {
                    return
                }

                if (!response.data.success || !response.data.data) {
                    setProgressMessage('분석 결과를 불러오지 못했습니다.')
                    return
                }

                const totalReport = response.data.data
                const reportData = totalReport.report_data || []
                const mappedItems = reportData.map(mapTotalReportItem)

                if (totalReport.total_count && totalReport.total_count > mappedItems.length) {
                    const paddedItems: (KopicReportItem | null)[] = new Array(
                        totalReport.total_count
                    ).fill(null)
                    mappedItems.forEach((item, index) => {
                        paddedItems[index] = item
                    })
                    setReportItems(paddedItems)
                } else {
                    setReportItems(mappedItems)
                }

                if (totalReport.total_score !== null && totalReport.total_score !== undefined) {
                    setAverageScore(Math.round(totalReport.total_score))
                } else if (
                    totalReport.avg_accuracy !== null &&
                    totalReport.avg_accuracy !== undefined &&
                    totalReport.avg_intonation !== null &&
                    totalReport.avg_intonation !== undefined
                ) {
                    setAverageScore(
                        Math.round((totalReport.avg_accuracy + totalReport.avg_intonation) / 2)
                    )
                }

                if (totalReport.status === 'PROCESSING') {
                    const completed = totalReport.completed_count ?? reportData.length
                    const total = totalReport.total_count ?? reportData.length
                    if (total) {
                        setProgressMessage(`${completed}/${total} 분석 중...`)
                    } else {
                        setProgressMessage('분석 중...')
                    }
                } else {
                    setProgressMessage(null)
                    if (intervalId) {
                        clearInterval(intervalId)
                        intervalId = null
                    }
                }
            } catch (error) {
                if (!cancelled) {
                    setProgressMessage('분석 결과를 불러오지 못했습니다.')
                }
                if (intervalId) {
                    clearInterval(intervalId)
                    intervalId = null
                }
            }
        }

        fetchTotalReport()
        intervalId = setInterval(fetchTotalReport, 2000)

        return () => {
            cancelled = true
            if (intervalId) {
                clearInterval(intervalId)
            }
        }
    }, [themeId, navigate, kopicTotalReportId, isSessionReady])

    const handleToggle = (index: number) => {
        setExpandedIndex(expandedIndex === index ? -1 : index)
    }

    return (
        <div className="min-h-screen w-full">
            {/* 헤더 */}
            <header className="w-full h-[50px] bg-[var(--color-bg-root)]">
                <div className="mx-auto h-full w-full max-w-[75rem] flex items-center px-6">
                    <img src={logoWhite} alt="Meari" className="h-[16px]" />
                </div>
            </header>

            {/* 메인 컨텐츠 */}
            <main className="mx-auto w-full max-w-[68.2rem] py-11">
                {/* 상단: 점수 및 테마 정보 */}
                <div className="grid grid-cols-[1fr_1.2fr] gap-8 mb-10">
                    {/* 왼쪽: 종합 점수 */}
                    <div>
                        <h2 className="text-xl font-bold text-gray-900 mb-14">코픽 종합 점수</h2>
                        <div className="flex justify-center">
                            <CircularProgress score={averageScore} />
                        </div>
                    </div>

                    {/* 오른쪽: 선택 테마 */}
                    <div className="flex flex-col h-full pl-12 pr-4">
                        <h2 className="text-xl font-bold text-gray-900 mb-4">
                            선택 테마 - {themeName || ''}
                        </h2>
                        {safeThemeImageUrl && (
                            <div className="flex-1 overflow-hidden relative aspect-video rounded-xl">
                                <img
                                    src={safeThemeImageUrl}
                                    alt={themeName}
                                    className="w-full h-full object-cover absolute inset-0"
                                />
                            </div>
                        )}
                        {!safeThemeImageUrl && (
                            <div className="flex-1 rounded-xl bg-gray-100 flex items-center justify-center min-h-[200px]">
                                <p className="text-gray-400">이미지 없음</p>
                            </div>
                        )}
                    </div>
                </div>

                {/* 리포트 보기 */}
                <div>
                    <h3 className="text-xl font-bold text-gray-900 mb-6">리포트 보기</h3>

                    {progressMessage && (
                        <p className="text-sm text-gray-600 mb-3">{progressMessage}</p>
                    )}

                    <div className="space-y-3">
                        {reportItems.length > 0 ? (
                            reportItems.map((item, index) =>
                                item ? (
                                    <ReportItem
                                        key={item.kopic_report_id}
                                        item={item}
                                        index={index}
                                        isExpanded={expandedIndex === index}
                                        onToggle={() => handleToggle(index)}
                                    />
                                ) : (
                                    <ReportItemSkeleton key={`skeleton-${index}`} />
                                )
                            )
                        ) : (
                            Array.from({ length: 5 }).map((_, index) => (
                                <ReportItemSkeleton key={`empty-skeleton-${index}`} />
                            ))
                        )}
                    </div>
                </div>

                {/* 하단 버튼 */}
                <div className="mt-8 flex justify-center pt-4">
                    <button
                        onClick={clearSessionAndGoHome}
                        className="px-10 py-3 bg-[#2D9CDB] text-white font-semibold rounded-full hover:bg-[#2789c2] transition-colors shadow-md"
                    >
                        처음으로 돌아가기
                    </button>
                </div>
            </main>
        </div>
    )
}

