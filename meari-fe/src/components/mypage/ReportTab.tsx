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
  <div className='flex gap-4 p-4 bg-white rounded-lg shadow-sm animate-pulse'>
    <div className='w-32 h-20 bg-gray-300 rounded flex-shrink-0' />
    <div className='flex-1 space-y-2'>
      <div className='h-4 bg-gray-300 rounded w-20' />
      <div className='h-5 bg-gray-300 rounded w-3/4' />
      <div className='h-3 bg-gray-300 rounded w-1/2' />
    </div>
    <div className='flex flex-col items-end justify-between'>
      <div className='h-8 w-16 bg-gray-300 rounded' />
      <div className='h-3 w-24 bg-gray-300 rounded' />
    </div>
  </div>
)

const KopicReportSkeleton = () => (
  <div className='flex gap-4 p-4 bg-white rounded-lg shadow-sm animate-pulse'>
    <div className='w-24 h-16 bg-gray-300 rounded flex-shrink-0' />
    <div className='flex-1 space-y-2'>
      <div className='h-5 bg-gray-300 rounded w-2/3' />
      <div className='h-3 bg-gray-300 rounded w-1/3' />
    </div>
    <div className='flex items-center'>
      <div className='h-8 w-20 bg-gray-300 rounded' />
    </div>
  </div>
)

const ReportTab = () => {
  const navigate = useNavigate()
  const [activeTab, setActiveTab] = useState<ReportTabType>('shadowing')

  // 쉐도잉 리포트 상태
  const [shadowingReports, setShadowingReports] = useState<ShadowingReport[]>([])
  const [selectedTheme, setSelectedTheme] = useState<string>('all')
  const [page, setPage] = useState(0)
  const [hasMore, setHasMore] = useState(true)
  const [isLoadingShadowing, setIsLoadingShadowing] = useState(false)
  const observerTarget = useRef<HTMLDivElement>(null)

  // 코픽 리포트 상태
  const [kopicReports, setKopicReports] = useState<KopicReport[]>([])
  const [isLoadingKopic, setIsLoadingKopic] = useState(false)

  // 사전 학습 상태
  const [preStudyItems, setPreStudyItems] = useState<PreStudyItem[]>([])
  const [isLoadingPreStudy, setIsLoadingPreStudy] = useState(false)

  // 상세 패널 상태
  const [selectedReportId, setSelectedReportId] = useState<number | null>(null)
  const [selectedReportType, setSelectedReportType] = useState<'shadowing' | 'kopic' | null>(null)

  // 쉐도잉 리포트 로드
  const loadShadowingReports = useCallback(
    async (reset: boolean = false) => {
      if (isLoadingShadowing || (!hasMore && !reset)) return

      setIsLoadingShadowing(true)
      try {
        const currentPage = reset ? 0 : page
        const response = await getShadowingReports(
          currentPage,
          10,
          selectedTheme === 'all' ? undefined : selectedTheme
        )

        if (response.data.success && response.data.data) {
          const newReports = response.data.data.reports
          setShadowingReports((prev) => (reset ? newReports : [...prev, ...newReports]))
          setHasMore(response.data.data.hasMore)
          setPage(reset ? 1 : currentPage + 1)
        }
      } catch (error) {
        console.error('Failed to load shadowing reports:', error)
      } finally {
        setIsLoadingShadowing(false)
      }
    },
    [page, hasMore, selectedTheme, isLoadingShadowing]
  )

  // 테마 변경 시 리포트 리셋
  useEffect(() => {
    setPage(0)
    setHasMore(true)
    setShadowingReports([])
    loadShadowingReports(true)
  }, [selectedTheme])

  // 무한 스크롤
  useEffect(() => {
    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting && hasMore && !isLoadingShadowing) {
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
  }, [hasMore, isLoadingShadowing, loadShadowingReports])

  // 코픽 리포트 로드
  useEffect(() => {
    if (activeTab === 'kopic' && kopicReports.length === 0) {
      setIsLoadingKopic(true)
      getKopicReports()
        .then((response) => {
          if (response.data.success && response.data.data) {
            setKopicReports(response.data.data.reports)
          }
        })
        .catch((error) => {
          console.error('Failed to load kopic reports:', error)
        })
        .finally(() => {
          setIsLoadingKopic(false)
        })
    }
  }, [activeTab, kopicReports.length])

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
    // TODO: 실제 방 생성 로직 구현
    console.log('방 생성하기:', contentName)
    navigate('/shadowing/create-room')
  }

  // 방 검색하기
  const handleSearchRoom = (contentName: string) => {
    // TODO: 실제 방 검색 로직 구현
    console.log('방 검색하기:', contentName)
    navigate('/shadowing/search-room')
  }

  return (
    <div className='relative flex h-full'>
      {/* 메인 컨텐츠 */}
      <div className={`flex-1 p-8 transition-all duration-300 ${selectedReportId ? 'mr-96' : ''}`}>
        <h1 className='text-3xl font-bold text-gray-900 mb-6'>리포트</h1>

        {/* 탭 메뉴 */}
        <div className='flex gap-4 mb-6 border-b border-gray-200'>
          <button
            type='button'
            onClick={() => setActiveTab('shadowing')}
            className={`px-4 py-2 font-semibold transition-colors ${
              activeTab === 'shadowing'
                ? 'text-blue-600 border-b-2 border-blue-600'
                : 'text-gray-600 hover:text-gray-900'
            }`}
          >
            쉐도잉 리포트
          </button>
          <button
            type='button'
            onClick={() => setActiveTab('kopic')}
            className={`px-4 py-2 font-semibold transition-colors ${
              activeTab === 'kopic'
                ? 'text-blue-600 border-b-2 border-blue-600'
                : 'text-gray-600 hover:text-gray-900'
            }`}
          >
            코픽(OPIc) 리포트
          </button>
          <button
            type='button'
            onClick={() => setActiveTab('prestudy')}
            className={`px-4 py-2 font-semibold transition-colors ${
              activeTab === 'prestudy'
                ? 'text-blue-600 border-b-2 border-blue-600'
                : 'text-gray-600 hover:text-gray-900'
            }`}
          >
            사전 학습
          </button>
        </div>

        {/* 쉐도잉 리포트 */}
        {activeTab === 'shadowing' && (
          <div>
            {/* 테마 필터 */}
            <div className='mb-4'>
              <label htmlFor='theme-filter' className='block text-sm font-medium text-gray-700 mb-2'>
                테마 필터
              </label>
              <select
                id='theme-filter'
                value={selectedTheme}
                onChange={(e) => setSelectedTheme(e.target.value)}
                className='px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500'
              >
                <option value='all'>전체</option>
                <option value='여행'>여행</option>
                <option value='비즈니스'>비즈니스</option>
                <option value='일상'>일상</option>
                <option value='음식'>음식</option>
                <option value='쇼핑'>쇼핑</option>
              </select>
            </div>

            {/* 리포트 목록 - 스크롤 영역 */}
            <div className='max-h-[600px] overflow-y-auto pr-2 space-y-3'>
              {shadowingReports.map((report) => (
                <div
                  key={report.id}
                  className='flex gap-4 p-4 bg-white rounded-lg shadow-sm hover:shadow-md transition-shadow border border-gray-100'
                >
                  {/* 썸네일 */}
                  <div
                    onClick={() => handleShadowingReportClick(report.id)}
                    className='cursor-pointer'
                  >
                    <img
                      src={report.thumbnail}
                      alt={report.contentName}
                      className='w-32 h-20 object-cover rounded flex-shrink-0'
                    />
                  </div>

                  {/* 정보 */}
                  <div
                    className='flex-1 cursor-pointer'
                    onClick={() => handleShadowingReportClick(report.id)}
                  >
                    <div className='text-xs text-blue-600 font-semibold mb-1'>
                      {report.themeName}
                    </div>
                    <h3 className='text-base font-bold text-gray-900 mb-1 line-clamp-1'>
                      {report.contentName}
                    </h3>
                    <p className='text-sm text-gray-600 line-clamp-1'>{report.roomName}</p>
                  </div>

                  {/* 점수 및 액션 버튼 */}
                  <div className='flex flex-col items-end justify-between gap-2'>
                    <div className='text-right'>
                      <div className='text-2xl font-bold text-green-600'>{report.totalScore}점</div>
                      <div className='text-xs text-gray-500'>{report.date}</div>
                    </div>
                    <div className='flex gap-2'>
                      <button
                        type='button'
                        onClick={(e) => {
                          e.stopPropagation()
                          handleCreateRoom(report.contentName)
                        }}
                        className='px-3 py-1 text-xs bg-blue-600 text-white rounded hover:bg-blue-700 transition-colors whitespace-nowrap'
                      >
                        방 생성
                      </button>
                      <button
                        type='button'
                        onClick={(e) => {
                          e.stopPropagation()
                          handleSearchRoom(report.contentName)
                        }}
                        className='px-3 py-1 text-xs bg-gray-600 text-white rounded hover:bg-gray-700 transition-colors whitespace-nowrap'
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
              <div ref={observerTarget} className='h-10 flex justify-center items-center'>
                {!hasMore && shadowingReports.length > 0 && (
                  <span className='text-gray-500 text-sm'>모든 리포트를 불러왔습니다.</span>
                )}
              </div>

              {shadowingReports.length === 0 && !isLoadingShadowing && (
                <div className='text-center py-12 text-gray-500'>리포트가 없습니다.</div>
              )}
            </div>
          </div>
        )}

        {/* 코픽 리포트 */}
        {activeTab === 'kopic' && (
          <div className='max-h-[600px] overflow-y-auto pr-2 space-y-3'>
            {isLoadingKopic ? (
              Array.from({ length: 5 }).map((_, index) => (
                <KopicReportSkeleton key={`skeleton-${index}`} />
              ))
            ) : (
              <>
                {kopicReports.map((report) => (
                  <div
                    key={report.id}
                    onClick={() => handleKopicReportClick(report.id)}
                    className='flex gap-4 p-4 bg-white rounded-lg shadow-sm hover:shadow-md transition-shadow cursor-pointer border border-gray-100'
                  >
                    <img
                      src={report.thumbnail}
                      alt={report.themeName}
                      className='w-24 h-16 object-cover rounded flex-shrink-0'
                    />
                    <div className='flex-1'>
                      <h3 className='text-base font-bold text-gray-900 mb-1'>{report.themeName}</h3>
                      <p className='text-sm text-gray-500'>{report.date}</p>
                    </div>
                    <div className='flex items-center'>
                      <span className='text-xl font-bold text-blue-600'>평균 {report.averageScore}점</span>
                    </div>
                  </div>
                ))}

                {kopicReports.length === 0 && (
                  <div className='text-center py-12 text-gray-500'>리포트가 없습니다.</div>
                )}
              </>
            )}
          </div>
        )}

        {/* 사전 학습 */}
        {activeTab === 'prestudy' && (
          <div className='max-h-[600px] overflow-y-auto pr-2 space-y-3'>
            {isLoadingPreStudy ? (
              <div className='text-center py-12 text-gray-500'>로딩 중...</div>
            ) : (
              <>
                {preStudyItems.map((item) => (
                  <div
                    key={item.id}
                    className='bg-white rounded-lg shadow-sm p-4 flex items-center justify-between border border-gray-100'
                  >
                    <div className='flex items-center gap-4'>
                      <input
                        type='checkbox'
                        checked={item.isCompleted}
                        readOnly
                        className='w-5 h-5 text-blue-600 rounded cursor-not-allowed'
                      />
                      <div>
                        <h3 className='text-base font-semibold text-gray-900'>{item.contentName}</h3>
                        {item.completedAt && (
                          <p className='text-sm text-gray-500'>완료일: {item.completedAt}</p>
                        )}
                      </div>
                    </div>
                    <div>
                      {item.isCompleted ? (
                        <span className='px-3 py-1 bg-green-100 text-green-700 rounded-full text-sm font-medium'>
                          완료
                        </span>
                      ) : (
                        <span className='px-3 py-1 bg-gray-100 text-gray-700 rounded-full text-sm font-medium'>
                          미완료
                        </span>
                      )}
                    </div>
                  </div>
                ))}

                {preStudyItems.length === 0 && (
                  <div className='text-center py-12 text-gray-500'>학습 항목이 없습니다.</div>
                )}
              </>
            )}
          </div>
        )}
      </div>

      {/* 오른쪽 슬라이드 패널 */}
      {selectedReportId && (
        <div className='fixed top-0 right-0 h-full w-96 bg-white shadow-2xl transform transition-transform duration-300 ease-in-out overflow-y-auto z-50'>
          <div className='p-6'>
            <div className='flex justify-between items-center mb-4'>
              <h2 className='text-2xl font-bold text-gray-900'>리포트 상세</h2>
              <button
                type='button'
                onClick={closeDetailPanel}
                className='text-gray-500 hover:text-gray-700 text-2xl'
              >
                ×
              </button>
            </div>

            <div className='space-y-4'>
              <p className='text-gray-600'>
                {selectedReportType === 'shadowing' ? '쉐도잉' : '코픽'} 리포트 ID: {selectedReportId}
              </p>
              <p className='text-sm text-gray-500'>
                여기에 리포트 상세 내용이 표시됩니다.
              </p>

              {/* 임시 상세 내용 */}
              <div className='bg-gray-50 rounded-lg p-4'>
                <h3 className='font-semibold mb-2'>상세 정보</h3>
                <ul className='space-y-2 text-sm text-gray-600'>
                  <li>• 정확도: 85%</li>
                  <li>• 유창성: 78%</li>
                  <li>• 발음: 82%</li>
                  <li>• 억양: 90%</li>
                </ul>
              </div>

              <div className='bg-blue-50 rounded-lg p-4'>
                <h3 className='font-semibold mb-2 text-blue-900'>피드백</h3>
                <p className='text-sm text-blue-800'>
                  전반적으로 좋은 발음을 보여주셨습니다. 특히 억양이 매우 자연스러웠습니다.
                </p>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* 오버레이 */}
      {selectedReportId && (
        <div
          className='fixed inset-0 bg-black bg-opacity-30 z-40'
          onClick={closeDetailPanel}
        />
      )}
    </div>
  )
}

export default ReportTab
