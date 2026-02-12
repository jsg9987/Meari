import { useEffect, useState, useRef, useCallback } from 'react'
import {
  getShadowingReports,
  getKopicReports,
  getShadowingReportDetail,
  type ShadowingReport,
  type ShadowingReportDetail,
  type KopicReport
} from '../../api/mypage.api'
import { getKopicTotalReport, type KopicTotalReportResponse } from '../../api/kopic.api'
import ReportStats from './report/ReportStats'
import CircularProgress from './report/CircularProgress'
import ScoreRadialChart from './report/ScoreRadialChart'
import SentenceChart from './report/SentenceChart'
import SyllableChart from './report/SyllableChart'
import ErrorPieChart from './report/ErrorPieChart'
import { getRelativeTime } from '../../utils/reportUtils'

type ReportTabType = 'shadowing' | 'kopic'

// 스켈레톤 UI 컴포넌트
const ShadowingReportSkeleton = () => (
  <div className='flex gap-3 p-3 bg-white rounded-lg shadow-sm animate-pulse'>
    <div className='w-24 h-16 bg-gray-300 rounded flex-shrink-0' />
    <div className='flex-1 space-y-2'>
      <div className='h-3 bg-gray-300 rounded w-16' />
      <div className='h-4 bg-gray-300 rounded w-3/4' />
      <div className='h-3 bg-gray-300 rounded w-1/2' />
    </div>
    <div className='flex flex-col items-end justify-between'>
      <div className='h-6 w-14 bg-gray-300 rounded' />
      <div className='h-3 w-20 bg-gray-300 rounded' />
    </div>
  </div>
)

const KopicReportSkeleton = () => (
  <div className='flex gap-3 p-3 bg-white rounded-lg shadow-sm animate-pulse'>
    <div className='w-20 h-14 bg-gray-300 rounded flex-shrink-0' />
    <div className='flex-1 space-y-2'>
      <div className='h-4 bg-gray-300 rounded w-2/3' />
      <div className='h-3 bg-gray-300 rounded w-1/3' />
    </div>
    <div className='flex items-center'>
      <div className='h-6 w-16 bg-gray-300 rounded' />
    </div>
  </div>
)

const ReportTab = () => {
  const [activeTab, setActiveTab] = useState<ReportTabType>('shadowing')

  // 쉐도잉 리포트 상태
  const [shadowingReports, setShadowingReports] = useState<ShadowingReport[]>([])
  const [cursor, setCursor] = useState<number | undefined>(undefined)
  const [hasMore, setHasMore] = useState(true)
  const [isLoadingShadowing, setIsLoadingShadowing] = useState(false)
  const observerTarget = useRef<HTMLDivElement>(null)
  const cursorRef = useRef<number | undefined>(undefined)
  const hasMoreRef = useRef(true)
  const isLoadingRef = useRef(false)

  // 코픽 리포트 상태
  const [kopicReports, setKopicReports] = useState<KopicReport[]>([])
  const [kopicCursor, setKopicCursor] = useState<number | undefined>(undefined)
  const [hasMoreKopic, setHasMoreKopic] = useState(true)
  const [isLoadingKopic, setIsLoadingKopic] = useState(false)
  const kopicObserverTarget = useRef<HTMLDivElement>(null)
  const kopicCursorRef = useRef<number | undefined>(undefined)
  const hasMoreKopicRef = useRef(true)
  const isLoadingKopicRef = useRef(false)

  // 상세 패널 상태
  const [selectedReportId, setSelectedReportId] = useState<number | null>(null)
  const [selectedReportType, setSelectedReportType] = useState<'shadowing' | 'kopic' | null>(null)
  const [selectedReportDetail, setSelectedReportDetail] = useState<ShadowingReportDetail | KopicTotalReportResponse['data'] | null>(null)
  const [isLoadingDetail, setIsLoadingDetail] = useState(false)

  // ref 업데이트
  useEffect(() => {
    cursorRef.current = cursor
    hasMoreRef.current = hasMore
    isLoadingRef.current = isLoadingShadowing
  }, [cursor, hasMore, isLoadingShadowing])

  // 코픽 ref 업데이트
  useEffect(() => {
    kopicCursorRef.current = kopicCursor
    hasMoreKopicRef.current = hasMoreKopic
    isLoadingKopicRef.current = isLoadingKopic
  }, [kopicCursor, hasMoreKopic, isLoadingKopic])

  // 쉐도잉 리포트 로드
  const loadShadowingReports = useCallback(async (reset: boolean = false) => {
    if (isLoadingRef.current || (!hasMoreRef.current && !reset)) return

    isLoadingRef.current = true
    setIsLoadingShadowing(true)

    try {
      const response = await getShadowingReports(
        reset ? undefined : cursorRef.current,
        10
      )

      if (response.data.success && response.data.data) {
        const newReports = response.data.data.contents
        setShadowingReports((prev) => (reset ? newReports : [...prev, ...newReports]))
        setHasMore(response.data.data.has_next)
        setCursor(response.data.data.next_cursor ?? undefined)
      }
    } catch (error) {
      console.error('Failed to load shadowing reports:', error)
    } finally {
      isLoadingRef.current = false
      setIsLoadingShadowing(false)
    }
  }, [])

  // 쉐도잉 탭 활성화 시 초기 로드
  useEffect(() => {
    if (activeTab === 'shadowing') {
      loadShadowingReports(true)
    }
  }, [activeTab])

  // 무한 스크롤
  useEffect(() => {
    if (activeTab !== 'shadowing') return

    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting) {
          loadShadowingReports()
        }
      },
      { threshold: 0.1 }
    )

    const currentTarget = observerTarget.current
    if (currentTarget) {
      observer.observe(currentTarget)
    }

    return () => {
      if (currentTarget) {
        observer.unobserve(currentTarget)
      }
    }
  }, [activeTab, loadShadowingReports])

  // 코픽 리포트 로드
  const loadKopicReports = useCallback(async (reset: boolean = false) => {
    if (isLoadingKopicRef.current || (!hasMoreKopicRef.current && !reset)) return

    isLoadingKopicRef.current = true
    setIsLoadingKopic(true)

    try {
      const response = await getKopicReports(
        reset ? undefined : kopicCursorRef.current,
        10
      )

      if (response.data.success && response.data.data) {
        const newReports = response.data.data.contents
        setKopicReports((prev) => (reset ? newReports : [...prev, ...newReports]))
        setHasMoreKopic(response.data.data.has_next)
        setKopicCursor(response.data.data.next_cursor ?? undefined)
      }
    } catch (error) {
      console.error('Failed to load kopic reports:', error)
    } finally {
      isLoadingKopicRef.current = false
      setIsLoadingKopic(false)
    }
  }, [])

  // 코픽 탭 활성화 시 초기 로드
  useEffect(() => {
    if (activeTab === 'kopic' && kopicReports.length === 0) {
      loadKopicReports(true)
    }
  }, [activeTab])

  // 코픽 무한 스크롤
  useEffect(() => {
    if (activeTab !== 'kopic') return

    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting) {
          loadKopicReports()
        }
      },
      { threshold: 0.1 }
    )

    const currentTarget = kopicObserverTarget.current
    if (currentTarget) {
      observer.observe(currentTarget)
    }

    return () => {
      if (currentTarget) {
        observer.unobserve(currentTarget)
      }
    }
  }, [activeTab, loadKopicReports])

  // 쉐도잉 리포트 클릭 핸들러
  const handleShadowingReportClick = async (reportId: number) => {
    setSelectedReportId(reportId)
    setSelectedReportType('shadowing')
    setIsLoadingDetail(true)
    setSelectedReportDetail(null)

    try {
      const response = await getShadowingReportDetail(reportId)
      if (response.data.success && response.data.data) {
        setSelectedReportDetail(response.data.data)
      }
    } catch (error) {
      console.error('Failed to load shadowing report detail:', error)
    } finally {
      setIsLoadingDetail(false)
    }
  }

  // 코픽 리포트 클릭 핸들러
  const handleKopicReportClick = async (reportId: number) => {
    setSelectedReportId(reportId)
    setSelectedReportType('kopic')
    setIsLoadingDetail(true)
    setSelectedReportDetail(null)

    try {
      const response = await getKopicTotalReport(reportId)
      if (response.data.success && response.data.data) {
        setSelectedReportDetail(response.data.data)
      }
    } catch (error) {
      console.error('Failed to load kopic report detail:', error)
    } finally {
      setIsLoadingDetail(false)
    }
  }

  // 상세 패널 닫기
  const closeDetailPanel = () => {
    setSelectedReportId(null)
    setSelectedReportType(null)
    setSelectedReportDetail(null)
  }

  return (
    <div className='p-8'>
      <h1 className='text-3xl font-bold text-gray-900 mb-6'>리포트</h1>

      {/* 탭 메뉴 */}
      <div className='flex gap-2 mb-0 bg-gray-100 rounded-t-lg p-1 max-w-2xl'>
        <button
          type='button'
          onClick={() => setActiveTab('shadowing')}
          className={`flex-1 px-4 py-2.5 font-semibold rounded-md transition-all ${
            activeTab === 'shadowing'
              ? 'bg-white text-blue-600 shadow-sm'
              : 'text-gray-600 hover:text-gray-900 hover:bg-gray-50'
          }`}
        >
          쉐도잉 리포트
        </button>
        <button
          type='button'
          onClick={() => setActiveTab('kopic')}
          className={`flex-1 px-4 py-2.5 font-semibold rounded-md transition-all ${
            activeTab === 'kopic'
              ? 'bg-white text-blue-600 shadow-sm'
              : 'text-gray-600 hover:text-gray-900 hover:bg-gray-50'
          }`}
        >
          KOPIC 리포트
        </button>
      </div>

      {/* 컨텐츠 영역 */}
      <div className='bg-white border border-gray-200 rounded-b-lg shadow-sm max-w-2xl'>
        {/* 쉐도잉 리포트 */}
        {activeTab === 'shadowing' && (
          <div className='p-4 flex flex-col h-[calc(100vh-280px)]'>
            {/* 통계 대시보드 */}
            <ReportStats reports={shadowingReports} />

            {/* 리포트 목록 - 스크롤 영역 */}
            <div className='flex-1 overflow-y-auto space-y-2.5 scrollbar-hide'>
              {shadowingReports.map((report) => (
                <div
                  key={report.shadowing_report_id}
                  onClick={() => handleShadowingReportClick(report.shadowing_report_id)}
                  className={`flex gap-3 p-3 bg-gray-50 rounded-lg hover:bg-gray-100 transition-all cursor-pointer border-l-4 ${
                    !report.is_read ? 'border-blue-500 shadow-md' : 'border-transparent'
                  } hover:shadow-lg`}
                >
                  {/* 썸네일 */}
                  <div className='relative flex-shrink-0'>
                    <img
                      src={report.thumbnail_url}
                      alt={report.content_title}
                      className='w-24 h-16 object-cover rounded'
                    />
                    {!report.is_read && (
                      <div className='absolute -top-1 -right-1 w-3 h-3 bg-blue-500 rounded-full border-2 border-white animate-pulse' />
                    )}
                  </div>

                  {/* 정보 */}
                  <div className='flex-1 min-w-0'>
                    <div className='flex items-start gap-2 mb-1'>
                      <h3 className='text-sm font-bold text-gray-900 truncate flex-1'>
                        {report.content_title}
                      </h3>
                      {!report.is_read && (
                        <span className='px-2 py-0.5 bg-blue-100 text-blue-700 rounded text-xs font-semibold whitespace-nowrap'>
                          NEW
                        </span>
                      )}
                    </div>
                    <p className='text-xs text-gray-600 truncate'>{report.room_title}</p>
                    <p className='text-xs text-gray-400 mt-1'>
                      {getRelativeTime(report.created_at)}
                    </p>
                  </div>

                  {/* 점수 */}
                  <div className='flex items-center'>
                    <CircularProgress score={report.total_score} size={60} strokeWidth={6} />
                  </div>
                </div>
              ))}

              {/* 스켈레톤 로딩 */}
              {isLoadingShadowing &&
                Array.from({ length: 3 }).map((_, index) => (
                  <ShadowingReportSkeleton key={`skeleton-${index}`} />
                ))}

              {/* 무한 스크롤 감지 요소 */}
              <div ref={observerTarget} className='h-8 flex justify-center items-center'>
                {!hasMore && shadowingReports.length > 0 && (
                  <span className='text-gray-500 text-xs'>모든 리포트를 불러왔습니다.</span>
                )}
              </div>

              {shadowingReports.length === 0 && !isLoadingShadowing && (
                <div className='text-center py-12 text-gray-500 text-sm'>리포트가 없습니다.</div>
              )}
            </div>
          </div>
        )}

        {/* 코픽 리포트 */}
        {activeTab === 'kopic' && (
          <div className='p-4 flex flex-col h-[calc(100vh-280px)]'>
            <div className='flex-1 overflow-y-auto space-y-2.5 scrollbar-hide'>
              {kopicReports.map((report) => (
                <div
                  key={report.kopic_report_id}
                  onClick={() => handleKopicReportClick(report.kopic_report_id)}
                  className='flex gap-3 p-3 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors cursor-pointer border border-gray-200'
                >
                  <div className='relative'>
                    <img
                      src={report.thumbnail_url}
                      alt={report.theme}
                      className='w-20 h-14 object-cover rounded flex-shrink-0'
                    />
                    {!report.is_read && (
                      <div className='absolute top-1 right-1 w-2 h-2 bg-red-500 rounded-full' />
                    )}
                  </div>
                  <div className='flex-1 min-w-0'>
                    <h3 className='text-sm font-bold text-gray-900 mb-0.5 truncate capitalize'>
                      {report.theme}
                    </h3>
                    <p className='text-xs text-gray-500'>
                      {new Date(report.created_at).toLocaleDateString('ko-KR')}
                    </p>
                  </div>
                  <div className='flex items-center'>
                    <span className='text-lg font-bold text-blue-600'>{report.total_score}점</span>
                  </div>
                </div>
              ))}

              {/* 스켈레톤 로딩 */}
              {isLoadingKopic &&
                Array.from({ length: 3 }).map((_, index) => (
                  <KopicReportSkeleton key={`skeleton-${index}`} />
                ))}

              {/* 무한 스크롤 감지 요소 */}
              <div ref={kopicObserverTarget} className='h-8 flex justify-center items-center'>
                {!hasMoreKopic && kopicReports.length > 0 && (
                  <span className='text-gray-500 text-xs'>모든 리포트를 불러왔습니다.</span>
                )}
              </div>

              {kopicReports.length === 0 && !isLoadingKopic && (
                <div className='text-center py-12 text-gray-500 text-sm'>리포트가 없습니다.</div>
              )}
            </div>
          </div>
        )}
      </div>

      {/* 오른쪽 슬라이드 패널 */}
      <div
        className={`fixed top-0 right-0 h-full w-[900px] bg-white shadow-2xl transform transition-transform duration-300 ease-in-out overflow-y-auto z-50 border-l border-gray-200 ${
          selectedReportId ? 'translate-x-0' : 'translate-x-full'
        }`}
      >
        {selectedReportId && (
          <div className='p-6'>
            <div className='flex justify-between items-center mb-6 pb-4 border-b border-gray-200'>
              <h2 className='text-2xl font-bold text-gray-900'>리포트 상세</h2>
              <button
                type='button'
                onClick={closeDetailPanel}
                className='text-gray-400 hover:text-gray-600 text-3xl leading-none w-8 h-8 flex items-center justify-center hover:bg-gray-100 rounded-full transition-colors'
              >
                ×
              </button>
            </div>

            {/* 로딩 중 */}
            {isLoadingDetail && (
              <div className='flex justify-center items-center py-20'>
                <div className='animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600' />
              </div>
            )}

            {/* 코픽 리포트 - PROCESSING 상태 */}
            {!isLoadingDetail &&
              selectedReportType === 'kopic' &&
              selectedReportDetail &&
              'kopic_total_report_id' in selectedReportDetail &&
              selectedReportDetail.status === 'PROCESSING' && (
              <div className='space-y-6'>
                <div className='bg-yellow-50 border border-yellow-200 rounded-lg p-4'>
                  <p className='text-sm font-semibold text-yellow-900'>
                    코픽 리포트 #{selectedReportId} - 분석 중
                  </p>
                </div>

                <div className='bg-white rounded-lg p-6 border border-gray-200'>
                  <h3 className='font-semibold text-gray-900 mb-4 text-lg'>진행 상황</h3>
                  <div className='space-y-3'>
                    <div className='flex justify-between text-sm'>
                      <span className='text-gray-600'>완료된 문장</span>
                      <span className='font-bold text-gray-900'>
                        {selectedReportDetail.completed_count || 0} / {selectedReportDetail.total_count || 0}
                      </span>
                    </div>
                    <div className='w-full bg-gray-200 rounded-full h-2.5'>
                      <div
                        className='bg-blue-600 h-2.5 rounded-full transition-all duration-300'
                        style={{
                          width: `${((selectedReportDetail.completed_count || 0) / (selectedReportDetail.total_count || 1)) * 100}%`
                        }}
                      />
                    </div>
                  </div>
                </div>

                <div className='bg-blue-50 rounded-lg p-5 border border-blue-200'>
                  <p className='text-sm text-blue-800 text-center'>
                    AI가 발화를 분석 중입니다. 잠시만 기다려주세요.
                  </p>
                </div>
              </div>
            )}

            {/* 코픽 리포트 - COMPLETED 상태 */}
            {!isLoadingDetail &&
              selectedReportType === 'kopic' &&
              selectedReportDetail &&
              'kopic_total_report_id' in selectedReportDetail &&
              selectedReportDetail.status === 'COMPLETED' && (
              <div className='space-y-6'>
                <div className='bg-blue-50 border border-blue-200 rounded-lg p-4'>
                  <p className='text-sm font-semibold text-blue-900'>
                    코픽 리포트 #{selectedReportId}
                  </p>
                </div>

                {/* 전체 점수 요약 */}
                <div className='bg-gradient-to-br from-blue-50 to-indigo-50 rounded-lg p-6 border border-blue-200'>
                  <h3 className='font-semibold text-gray-900 mb-4 text-lg'>전체 요약</h3>
                  <div className='grid grid-cols-3 gap-4'>
                    <div className='bg-white rounded-lg p-4 border border-blue-100 text-center'>
                      <p className='text-xs text-gray-500 mb-1'>평균 정확도</p>
                      <p className='text-2xl font-bold text-blue-600'>{selectedReportDetail.avg_accuracy}%</p>
                    </div>
                    <div className='bg-white rounded-lg p-4 border border-blue-100 text-center'>
                      <p className='text-xs text-gray-500 mb-1'>총점</p>
                      <p className='text-2xl font-bold text-indigo-600'>{selectedReportDetail.total_score}점</p>
                    </div>
                    <div className='bg-white rounded-lg p-4 border border-blue-100 text-center'>
                      <p className='text-xs text-gray-500 mb-1'>문장 수</p>
                      <p className='text-2xl font-bold text-gray-900'>{selectedReportDetail.sentence_count}개</p>
                    </div>
                  </div>
                </div>

                {/* 문장별 상세 분석 */}
                <div className='space-y-4'>
                  <h3 className='font-semibold text-gray-900 text-lg'>문장별 분석</h3>
                  {selectedReportDetail.report_data?.map((item, index) => (
                    <div key={item.kopic_report_id} className='bg-gray-50 rounded-lg p-4 border border-gray-200'>
                      <div className='flex justify-between items-start mb-3'>
                        <h4 className='font-semibold text-gray-900 text-sm'>문장 {index + 1}</h4>
                        <span className='text-lg font-bold text-blue-600'>{item.total_score}점</span>
                      </div>

                      <p className='text-sm text-gray-700 mb-3 leading-relaxed'>{item.text_ko}</p>

                      <div className='grid grid-cols-2 gap-2 mb-3'>
                        <div className='bg-white rounded p-2 border border-gray-200'>
                          <p className='text-xs text-gray-500'>정확도</p>
                          <p className='text-lg font-bold text-gray-900'>{item.accuracy}%</p>
                        </div>
                        <div className='bg-white rounded p-2 border border-gray-200'>
                          <p className='text-xs text-gray-500'>억양</p>
                          <p className='text-lg font-bold text-gray-900'>{item.intonation}%</p>
                        </div>
                      </div>

                      {item.detailed_analysis && (
                        <div className='bg-blue-50 rounded p-3 border border-blue-100 space-y-2'>
                          <div>
                            <p className='text-xs font-semibold text-blue-900 mb-1'>놓친 부분</p>
                            <p className='text-xs text-blue-800'>{item.detailed_analysis.feedback.missed_point}</p>
                          </div>
                          <div>
                            <p className='text-xs font-semibold text-blue-900 mb-1'>교정</p>
                            <p className='text-xs text-blue-800'>{item.detailed_analysis.feedback.correction}</p>
                          </div>
                          <div>
                            <p className='text-xs font-semibold text-blue-900 mb-1'>팁</p>
                            <p className='text-xs text-blue-800'>{item.detailed_analysis.feedback.tip}</p>
                          </div>
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* 쉐도잉 리포트 상세 */}
            {!isLoadingDetail &&
              selectedReportType === 'shadowing' &&
              selectedReportDetail &&
              'content_title' in selectedReportDetail && (
                <div className='space-y-6'>
                  {/* 헤더 정보 */}
                  <div className='bg-gradient-to-r from-green-50 to-emerald-50 border border-green-200 rounded-lg p-5'>
                    <div className='space-y-2'>
                      <p className='text-sm font-semibold text-green-900'>
                        쉐도잉 리포트 #{selectedReportDetail.shadowing_report_id}
                      </p>
                      <h3 className='text-xl font-bold text-gray-900'>
                        {selectedReportDetail.content_title}
                      </h3>
                      <div className='flex items-center gap-4 text-sm text-gray-600'>
                        <span>방: {selectedReportDetail.room_title}</span>
                        <span>역할: {selectedReportDetail.role_name}</span>
                      </div>
                      <p className='text-xs text-gray-500'>
                        {new Date(selectedReportDetail.created_at).toLocaleString('ko-KR')}
                      </p>
                    </div>
                  </div>

                  {/* 전체 점수 요약 - RadialBarChart */}
                  <ScoreRadialChart
                    accuracy={selectedReportDetail.accuracy}
                    intonation={selectedReportDetail.intonation}
                    totalScore={selectedReportDetail.total_score}
                  />

                  {/* 문장별 점수 비교 차트 */}
                  <SentenceChart detail={selectedReportDetail} />

                  {/* 오류 타입 분포 차트 */}
                  <ErrorPieChart detail={selectedReportDetail} />

                  {/* 전체 분석 요약 */}
                  {selectedReportDetail.detailed_analysis && (
                    <div className='bg-white rounded-lg p-5 border border-gray-200'>
                      <h3 className='font-semibold text-gray-900 mb-4 text-lg'>분석 요약</h3>
                      <div className='grid grid-cols-2 gap-3 text-sm'>
                        <div className='flex justify-between items-center p-3 bg-gray-50 rounded-lg'>
                          <span className='text-gray-600'>전체 문장</span>
                          <span className='font-bold text-gray-900'>
                            {selectedReportDetail.detailed_analysis.summary.total_sentences}개
                          </span>
                        </div>
                        <div className='flex justify-between items-center p-3 bg-gray-50 rounded-lg'>
                          <span className='text-gray-600'>분석 완료</span>
                          <span className='font-bold text-gray-900'>
                            {selectedReportDetail.detailed_analysis.summary.analyzed_sentences}개
                          </span>
                        </div>
                        <div className='flex justify-between items-center p-3 bg-gray-50 rounded-lg'>
                          <span className='text-gray-600'>평균 정확도</span>
                          <span className='font-bold text-green-600'>
                            {selectedReportDetail.detailed_analysis.summary.average_accuracy}%
                          </span>
                        </div>
                        <div className='flex justify-between items-center p-3 bg-gray-50 rounded-lg'>
                          <span className='text-gray-600'>평균 억양</span>
                          <span className='font-bold text-teal-600'>
                            {selectedReportDetail.detailed_analysis.summary.average_intonation}%
                          </span>
                        </div>
                      </div>
                    </div>
                  )}

                  {/* 문장별 상세 분석 */}
                  {selectedReportDetail.detailed_analysis?.sentences && (
                    <div className='space-y-4'>
                      <h3 className='font-semibold text-gray-900 text-lg'>문장별 분석</h3>
                      {selectedReportDetail.detailed_analysis.sentences.map((sentence, index) => (
                        <div
                          key={sentence.sentence_id}
                          className='bg-white rounded-lg p-5 border border-gray-200 space-y-4'
                        >
                          {/* 문장 헤더 */}
                          <div className='flex justify-between items-start'>
                            <h4 className='font-semibold text-gray-900'>문장 {index + 1}</h4>
                            <div className='flex gap-2'>
                              <span className='px-2 py-1 bg-green-100 text-green-700 rounded text-xs font-medium'>
                                정확도 {sentence.accuracy}%
                              </span>
                              <span className='px-2 py-1 bg-teal-100 text-teal-700 rounded text-xs font-medium'>
                                억양 {sentence.intonation.score}%
                              </span>
                            </div>
                          </div>

                          {/* 예상 문장 vs 인식된 문장 */}
                          <div className='space-y-2'>
                            <div className='bg-blue-50 rounded-lg p-3 border border-blue-100'>
                              <p className='text-xs font-semibold text-blue-900 mb-1'>예상 문장</p>
                              <p className='text-sm text-blue-800'>{sentence.text_expected}</p>
                            </div>
                            <div className='bg-purple-50 rounded-lg p-3 border border-purple-100'>
                              <p className='text-xs font-semibold text-purple-900 mb-1'>
                                인식된 문장 (STT)
                              </p>
                              <p className='text-sm text-purple-800'>{sentence.text_recognized}</p>
                            </div>
                          </div>

                          {/* 오류 분석 */}
                          {sentence.errors && sentence.errors.length > 0 && (
                            <div className='bg-red-50 rounded-lg p-4 border border-red-100'>
                              <p className='text-xs font-semibold text-red-900 mb-3'>
                                오류 분석 ({sentence.errors.length}개)
                              </p>
                              <div className='space-y-2'>
                                {sentence.errors.map((error, errorIdx) => (
                                  <div
                                    key={errorIdx}
                                    className='bg-white rounded p-2 border border-red-200'
                                  >
                                    <div className='flex items-center gap-2 mb-1'>
                                      <span className='px-1.5 py-0.5 bg-red-100 text-red-700 rounded text-xs font-medium uppercase'>
                                        {error.type}
                                      </span>
                                      <span className='text-xs text-gray-500'>
                                        위치: {error.position}
                                      </span>
                                      <span className='text-xs text-gray-500'>
                                        신뢰도: {(error.confidence * 100).toFixed(1)}%
                                      </span>
                                    </div>
                                    <p className='text-xs text-gray-700'>{error.description}</p>
                                    <p className='text-xs text-red-600 mt-1'>
                                      <span className='line-through'>{error.expected}</span>{' '}
                                      → <span className='font-semibold'>{error.actual}</span>
                                    </p>
                                  </div>
                                ))}
                              </div>
                            </div>
                          )}

                          {/* 억양 피드백 */}
                          {sentence.intonation.feedback && (
                            <div className='bg-teal-50 rounded-lg p-3 border border-teal-100'>
                              <p className='text-xs font-semibold text-teal-900 mb-1'>억양 피드백</p>
                              <p className='text-sm text-teal-800'>{sentence.intonation.feedback}</p>
                            </div>
                          )}

                          {/* 음절별 신뢰도 차트 */}
                          {sentence.syllables.length > 0 && (
                            <SyllableChart
                              syllables={sentence.syllables}
                              confidences={sentence.syllable_confidences}
                              sentenceIndex={index}
                            />
                          )}
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              )}
          </div>
        )}
      </div>
    </div>
  )
}

export default ReportTab
