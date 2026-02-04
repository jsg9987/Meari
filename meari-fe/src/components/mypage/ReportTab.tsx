import { useEffect, useState, useRef, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  getShadowingReports,
  getKopicReports,
  getPreStudyItems,
  type ShadowingReport,
  type KopicReport,
  type PreStudyItem
} from '../../api/mypage.api'

type ReportTabType = 'shadowing' | 'kopic' | 'prestudy'

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
  const navigate = useNavigate()
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

  // 사전 학습 상태
  const [preStudyItems, setPreStudyItems] = useState<PreStudyItem[]>([])
  const [isLoadingPreStudy, setIsLoadingPreStudy] = useState(false)

  // 상세 패널 상태
  const [selectedReportId, setSelectedReportId] = useState<number | null>(null)
  const [selectedReportType, setSelectedReportType] = useState<'shadowing' | 'kopic' | null>(null)

  // ref 업데이트
  useEffect(() => {
    cursorRef.current = cursor
    hasMoreRef.current = hasMore
    isLoadingRef.current = isLoadingShadowing
  }, [cursor, hasMore, isLoadingShadowing])

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

  // 초기 로드
  useEffect(() => {
    loadShadowingReports(true)
  }, [])

  // 무한 스크롤
  useEffect(() => {
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
  }, [loadShadowingReports])

  // 코픽 리포트 로드
  const loadKopicReports = useCallback(
    async (reset: boolean = false) => {
      if (isLoadingKopic || (!hasMoreKopic && !reset)) return

      setIsLoadingKopic(true)
      try {
        const currentCursor = reset ? undefined : kopicCursor
        const response = await getKopicReports(currentCursor, 10)

        if (response.data.success && response.data.data) {
          const newReports = response.data.data.contents
          setKopicReports((prev) => (reset ? newReports : [...prev, ...newReports]))
          setHasMoreKopic(response.data.data.has_next)
          setKopicCursor(response.data.data.next_cursor ?? undefined)
        }
      } catch (error) {
        console.error('Failed to load kopic reports:', error)
      } finally {
        setIsLoadingKopic(false)
      }
    },
    [kopicCursor, hasMoreKopic, isLoadingKopic]
  )

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
        if (entries[0].isIntersecting && hasMoreKopic && !isLoadingKopic) {
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
  }, [activeTab, hasMoreKopic, isLoadingKopic, loadKopicReports])

  // 사전 학습 로드
  useEffect(() => {
    if (activeTab === 'prestudy' && preStudyItems.length === 0) {
      setIsLoadingPreStudy(true)
      getPreStudyItems()
        .then((response) => {
          if (response.data.success && response.data.data) {
            setPreStudyItems(response.data.data.items)
          }
        })
        .catch((error) => {
          console.error('Failed to load prestudy items:', error)
        })
        .finally(() => {
          setIsLoadingPreStudy(false)
        })
    }
  }, [activeTab, preStudyItems.length])

  // 쉐도잉 리포트 클릭 핸들러
  const handleShadowingReportClick = (reportId: number) => {
    setSelectedReportId(reportId)
    setSelectedReportType('shadowing')
  }

  // 코픽 리포트 클릭 핸들러
  const handleKopicReportClick = (reportId: number) => {
    setSelectedReportId(reportId)
    setSelectedReportType('kopic')
  }

  // 상세 패널 닫기
  const closeDetailPanel = () => {
    setSelectedReportId(null)
    setSelectedReportType(null)
  }

  // 방 생성하기
  const handleCreateRoom = (contentName: string) => {
    console.log('방 생성하기:', contentName)
    navigate('/shadowing/create-room')
  }

  // 방 검색하기
  const handleSearchRoom = (contentName: string) => {
    console.log('방 검색하기:', contentName)
    navigate('/shadowing/search-room')
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
          코픽(OPIc) 리포트
        </button>
        <button
          type='button'
          onClick={() => setActiveTab('prestudy')}
          className={`flex-1 px-4 py-2.5 font-semibold rounded-md transition-all ${
            activeTab === 'prestudy'
              ? 'bg-white text-blue-600 shadow-sm'
              : 'text-gray-600 hover:text-gray-900 hover:bg-gray-50'
          }`}
        >
          사전 학습
        </button>
      </div>

      {/* 컨텐츠 영역 */}
      <div className='bg-white border border-gray-200 rounded-b-lg shadow-sm max-w-2xl'>
        {/* 쉐도잉 리포트 */}
        {activeTab === 'shadowing' && (
          <div className='p-4 flex flex-col h-[calc(100vh-280px)]'>
            {/* 리포트 목록 - 스크롤 영역 */}
            <div className='flex-1 overflow-y-auto space-y-2.5 scrollbar-hide'>
              {shadowingReports.map((report) => (
                <div
                  key={report.shadowing_report_id}
                  className='flex gap-3 p-3 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors border border-gray-200'
                >
                  {/* 썸네일 */}
                  <div
                    onClick={() => handleShadowingReportClick(report.shadowing_report_id)}
                    className='cursor-pointer relative'
                  >
                    <img
                      src={report.thumbnail_url}
                      alt={report.content_title}
                      className='w-24 h-16 object-cover rounded flex-shrink-0'
                    />
                    {!report.is_read && (
                      <div className='absolute top-1 right-1 w-2 h-2 bg-red-500 rounded-full' />
                    )}
                  </div>

                  {/* 정보 */}
                  <div
                    className='flex-1 cursor-pointer min-w-0'
                    onClick={() => handleShadowingReportClick(report.shadowing_report_id)}
                  >
                    <h3 className='text-sm font-bold text-gray-900 mb-0.5 truncate'>
                      {report.content_title}
                    </h3>
                    <p className='text-xs text-gray-600 truncate'>{report.room_title}</p>
                    <p className='text-xs text-gray-400 mt-1'>
                      {new Date(report.created_at).toLocaleDateString('ko-KR')}
                    </p>
                  </div>

                  {/* 점수 및 액션 */}
                  <div className='flex flex-col items-end justify-between gap-1.5'>
                    <div className='text-right'>
                      <div className='text-xl font-bold text-green-600'>{report.total_score}점</div>
                    </div>
                    <div className='flex gap-1.5'>
                      <button
                        type='button'
                        onClick={(e) => {
                          e.stopPropagation()
                          handleCreateRoom(report.content_title)
                        }}
                        className='px-2.5 py-1 text-xs bg-blue-600 text-white rounded hover:bg-blue-700 transition-colors whitespace-nowrap'
                      >
                        방 생성
                      </button>
                      <button
                        type='button'
                        onClick={(e) => {
                          e.stopPropagation()
                          handleSearchRoom(report.content_title)
                        }}
                        className='px-2.5 py-1 text-xs bg-gray-600 text-white rounded hover:bg-gray-700 transition-colors whitespace-nowrap'
                      >
                        방 검색
                      </button>
                    </div>
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

        {/* 사전 학습 */}
        {activeTab === 'prestudy' && (
          <div className='p-4 flex flex-col h-[calc(100vh-280px)]'>
            <div className='flex-1 overflow-y-auto space-y-2.5 scrollbar-hide'>
              {isLoadingPreStudy ? (
                <div className='text-center py-12 text-gray-500 text-sm'>로딩 중...</div>
              ) : (
                <>
                  {preStudyItems.map((item) => (
                    <div
                      key={item.id}
                      className='bg-gray-50 rounded-lg p-3 flex items-center justify-between border border-gray-200'
                    >
                      <div className='flex items-center gap-3'>
                        <input
                          type='checkbox'
                          checked={item.isCompleted}
                          readOnly
                          className='w-4 h-4 text-blue-600 rounded cursor-not-allowed'
                        />
                        <div>
                          <h3 className='text-sm font-semibold text-gray-900'>
                            {item.contentName}
                          </h3>
                          {item.completedAt && (
                            <p className='text-xs text-gray-500'>완료일: {item.completedAt}</p>
                          )}
                        </div>
                      </div>
                      <div>
                        {item.isCompleted ? (
                          <span className='px-2.5 py-1 bg-green-100 text-green-700 rounded-full text-xs font-medium'>
                            완료
                          </span>
                        ) : (
                          <span className='px-2.5 py-1 bg-gray-200 text-gray-700 rounded-full text-xs font-medium'>
                            미완료
                          </span>
                        )}
                      </div>
                    </div>
                  ))}

                  {preStudyItems.length === 0 && (
                    <div className='text-center py-12 text-gray-500 text-sm'>
                      학습 항목이 없습니다.
                    </div>
                  )}
                </>
              )}
            </div>
          </div>
        )}
      </div>

      {/* 오른쪽 슬라이드 패널 */}
      <div
        className={`fixed top-0 right-0 h-full w-[600px] bg-white shadow-2xl transform transition-transform duration-300 ease-in-out overflow-y-auto z-50 border-l border-gray-200 ${
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

            <div className='space-y-6'>
              <div className='bg-blue-50 border border-blue-200 rounded-lg p-4'>
                <p className='text-sm font-semibold text-blue-900'>
                  {selectedReportType === 'shadowing' ? '쉐도잉' : '코픽'} 리포트 #{selectedReportId}
                </p>
              </div>

              {/* 임시 상세 내용 */}
              <div className='bg-gray-50 rounded-lg p-5 border border-gray-200'>
                <h3 className='font-semibold text-gray-900 mb-3 text-lg'>상세 정보</h3>
                <div className='grid grid-cols-2 gap-4'>
                  <div className='bg-white rounded-lg p-3 border border-gray-200'>
                    <p className='text-xs text-gray-500 mb-1'>정확도</p>
                    <p className='text-2xl font-bold text-gray-900'>85%</p>
                  </div>
                  <div className='bg-white rounded-lg p-3 border border-gray-200'>
                    <p className='text-xs text-gray-500 mb-1'>유창성</p>
                    <p className='text-2xl font-bold text-gray-900'>78%</p>
                  </div>
                  <div className='bg-white rounded-lg p-3 border border-gray-200'>
                    <p className='text-xs text-gray-500 mb-1'>발음</p>
                    <p className='text-2xl font-bold text-gray-900'>82%</p>
                  </div>
                  <div className='bg-white rounded-lg p-3 border border-gray-200'>
                    <p className='text-xs text-gray-500 mb-1'>억양</p>
                    <p className='text-2xl font-bold text-gray-900'>90%</p>
                  </div>
                </div>
              </div>

              <div className='bg-blue-50 rounded-lg p-5 border border-blue-200'>
                <h3 className='font-semibold mb-3 text-blue-900 text-lg'>AI 피드백</h3>
                <p className='text-sm text-blue-800 leading-relaxed'>
                  전반적으로 좋은 발음을 보여주셨습니다. 특히 억양이 매우 자연스러웠습니다.
                  정확도와 발음 부분에서 조금 더 연습하시면 더 좋은 결과를 얻으실 수 있을 것 같습니다.
                </p>
              </div>

              <div className='bg-gray-50 rounded-lg p-5 border border-gray-200'>
                <h3 className='font-semibold text-gray-900 mb-3 text-lg'>개선 제안</h3>
                <ul className='space-y-2'>
                  <li className='flex items-start gap-2 text-sm text-gray-700'>
                    <span className='text-green-600 font-bold'>•</span>
                    <span>모음 발음을 좀 더 정확하게 해보세요</span>
                  </li>
                  <li className='flex items-start gap-2 text-sm text-gray-700'>
                    <span className='text-green-600 font-bold'>•</span>
                    <span>문장 끝의 억양 처리를 연습해보세요</span>
                  </li>
                  <li className='flex items-start gap-2 text-sm text-gray-700'>
                    <span className='text-green-600 font-bold'>•</span>
                    <span>연음 처리에 주의를 기울여보세요</span>
                  </li>
                </ul>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}

export default ReportTab
