import { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ChevronLeft, ChevronRight, Volume2 } from 'lucide-react'
import logoWhite from '../../assets/images/common/logo-white.svg'
import progressBarBg from '../../assets/images/daily/word-study/word-study-top-frame.svg'
import dailyCharacter from '../../assets/images/daily/word-study/daily-study-character.svg'
import goalTrophy from '../../assets/images/daily/word-study/goal-trophy.svg'
import { getWordStudy, type WordStudyWord } from '../../api/word-study.api'

const TOTAL_QUESTIONS = 10
const SKY_ASPECT = 'aspect-[1520/223]'
const PROGRESS_TRACK_WIDTH = 'w-full'
const PROGRESS_OVERLAY_GAP = 'pt-4'
const PROGRESS_ROW_GAP = 'gap-3'
const NAV_BUTTON_BASE =
  'inline-flex items-center gap-2 rounded-full px-5 py-2 text-sm font-semibold transition'
const NAV_BUTTON_ENABLED = 'bg-white text-gray-700 border border-gray-200 hover:border-[#2D9CDB]'
const NAV_BUTTON_DISABLED = 'cursor-not-allowed bg-gray-100 text-gray-400'

const hasHangul = (value: string) => /[\uAC00-\uD7A3]/.test(value)

const WordStudy = () => {
  const navigate = useNavigate()
  const [questions, setQuestions] = useState<WordStudyWord[]>([])
  const [currentIndex, setCurrentIndex] = useState(0)
  const [speakingId, setSpeakingId] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const isMountedRef = useRef(true)

  const total = questions.length
  const progressPercent = useMemo(() => {
    if (!total) return 0
    const progressIndex = Math.min(currentIndex, total)
    return Math.round((progressIndex / total) * 100)
  }, [currentIndex, total])

  const clampedProgress = Math.min(100, Math.max(0, progressPercent))
  const bubblePosition = Math.min(100, Math.max(0, clampedProgress))
  const bubbleStyle = useMemo(
    () => ({
      left: `calc(${Math.min(100, bubblePosition)}% + 1px)`,
      transform: 'translateX(-50%)'
    }),
    [bubblePosition]
  )

  const characterStyle = useMemo(
    () => ({
      left: `calc(${Math.min(100, clampedProgress)}% + 1px)`,
      transform: 'translateX(-50%)'
    }),
    [clampedProgress]
  )

  useEffect(() => {
    isMountedRef.current = true
    return () => {
      isMountedRef.current = false
      if ('speechSynthesis' in window) {
        window.speechSynthesis.cancel()
      }
    }
  }, [])

  const stopPlayback = () => {
    if ('speechSynthesis' in window) {
      window.speechSynthesis.cancel()
    }
  }

  const setSpeakingSafe = (value: string | null) => {
    if (!isMountedRef.current) return
    setSpeakingId(value)
  }

  useEffect(() => {
    const loadQuestions = async () => {
      setIsLoading(true)
      try {
        const response = await getWordStudy()
        const data = response.data?.data ?? []
        const firstBatch = data.slice(0, TOTAL_QUESTIONS)
        if (isMountedRef.current) {
          setQuestions(firstBatch)
          setCurrentIndex(0)
        }
      } catch (error) {
        console.error('Failed to load word study data:', error)
        if (isMountedRef.current) {
          setQuestions([])
        }
      } finally {
        if (isMountedRef.current) {
          setIsLoading(false)
        }
      }
    }

    loadQuestions()
  }, [])

  useEffect(() => {
    const handleKeydown = (event: KeyboardEvent) => {
      if (!total) return
      if (event.key === 'ArrowLeft') {
        setCurrentIndex((prev) => Math.max(0, prev - 1))
      }
      if (event.key === 'ArrowRight') {
        setCurrentIndex((prev) => Math.min(total, prev + 1))
      }
    }

    window.addEventListener('keydown', handleKeydown)
    return () => window.removeEventListener('keydown', handleKeydown)
  }, [total])

  const handlePrev = () => {
    if (!total) return
    setCurrentIndex((prev) => Math.max(0, prev - 1))
  }

  const handleNext = () => {
    if (!total) return
    if (currentIndex >= total) return
    setCurrentIndex((prev) => Math.min(total, prev + 1))
  }

  const playPronunciation = (question: WordStudyWord) => {
    if (!question) return
    if (!('speechSynthesis' in window)) return

    stopPlayback()
    window.speechSynthesis.cancel()
    const utterance = new SpeechSynthesisUtterance(question.wordKr)
    utterance.lang = hasHangul(question.wordKr) ? 'ko-KR' : 'en-US'
    utterance.rate = 0.95
    utterance.onend = () => setSpeakingSafe(null)
    utterance.onerror = () => setSpeakingSafe(null)
    setSpeakingSafe(String(question.wordId))
    window.speechSynthesis.speak(utterance)
  }

  const isFirst = currentIndex === 0
  const isLast = currentIndex === total - 1
  const isEndCard = currentIndex >= total && total > 0
  const showContent = !isLoading && total > 0

  return (
    <div className="min-h-screen w-full overflow-x-hidden bg-[#907761]">
      {/* 헤더 */}
      <header className="relative z-20 h-[50px] w-full bg-[#4F4F4F]">
        <div className="mx-auto flex h-full w-full max-w-[75rem] items-center px-6">
          <button
            type="button"
            onClick={() => navigate('/main')}
            className="inline-flex items-center"
            aria-label="메인 페이지로 이동"
          >
            <img src={logoWhite} alt="Meari" className="h-[16px] cursor-pointer" />
          </button>
        </div>
      </header>

      <div className="relative z-0 mx-auto w-[calc(100%/1.1)] origin-top-center scale-[1.1]">
        <main className="mx-auto w-full max-w-[80rem] pb-10 px-10">
        {/* 로딩 */}
        {isLoading && (
          <div className="flex h-[60vh] items-center justify-center">
            <div className="text-center">
              <div className="mx-auto mb-4 h-10 w-10 animate-spin rounded-full border-4 border-blue-500 border-t-transparent" />
              <p className="text-sm text-gray-600">문장을 불러오는 중...</p>
            </div>
          </div>
        )}

        {/* 빈 상태 */}
        {!isLoading && total === 0 && (
          <div className="flex h-[60vh] items-center justify-center">
            <p className="text-gray-600">문장을 불러오지 못했습니다.</p>
          </div>
        )}

        {showContent && (
          <>
            {/* 상단 배경 */}
            <section
              className={`relative left-1/2 w-screen -translate-x-1/2 bg-[#A4DFFF] ${SKY_ASPECT}`}
            >
              <div className="relative h-full w-full overflow-hidden bg-[#A4DFFF]">
                <img
                  src={progressBarBg}
                  alt="상단 배경"
                  className="absolute inset-0 h-full w-full origin-bottom scale-[0.9091] translate-y-[6%] object-cover object-[50%_100%]"
                />
                <div className="relative mx-auto h-full px-[30px]" />
              </div>
            </section>

            {/* 진행률 오버레이 */}
            <div className={`flex w-full justify-center bg-[#907761] px-5 ${PROGRESS_OVERLAY_GAP}`}>
              <div className={`relative w-full ${PROGRESS_TRACK_WIDTH}`}>
                <div className={`relative ${PROGRESS_ROW_GAP}`}>
                  <div className="relative z-0 h-2 overflow-hidden rounded-full bg-[#D1ADFF] shadow-inner">
                    <div
                      className="h-full rounded-full bg-[#9747FF] transition-all duration-500 ease-out"
                      style={{ width: `${clampedProgress}%` }}
                    />
                  </div>
                  <div className="relative h-10 py-8">
                    <div
                      className="absolute top-1/2 z-20 -translate-y-1/2 transition-all duration-500 ease-out"
                      style={bubbleStyle}
                    >
                      <div className="relative z-10 rounded-full bg-white px-4 py-1.5 text-xs font-semibold text-[#2D9CDB]">
                        진행률 {clampedProgress}%
                        <span
                          className="absolute left-1/2 bottom-full z-0 h-3 w-3 -translate-x-1/2 translate-y-[2px] bg-white"
                          style={{ clipPath: 'polygon(50% 0%, 0% 100%, 100% 100%)' }}
                        />
                      </div>
                    </div>
                  </div>
                </div>
                <div className="absolute -top-14 transition-all duration-500 ease-out" style={characterStyle}>
                  <img src={dailyCharacter} alt="character" className="h-13 w-auto drop-shadow-md" />
                </div>
                <img
                  src={goalTrophy}
                  alt="goal trophy"
                  className="absolute -top-12.5 right-0 h-12 w-auto drop-shadow-sm"
                />
              </div>
            </div>

            {/* 콘텐츠 카드 */}
            <section className="rounded-xl border border-gray-200 bg-white p-10 shadow-sm">
              <div className="mb-6 flex items-center justify-between">
                <div>
                  <p className="text-sm text-gray-500">
                    {isEndCard ? '학습 완료' : `단어 ${Math.min(currentIndex + 1, total)}`}
                  </p>
                  <p className="text-lg font-semibold text-gray-900">
                    {isEndCard ? '다음 학습을 선택하세요' : '제시된 단어와 설명을 확인하세요'}
                  </p>
                </div>
              </div>

              <div className="relative overflow-hidden">
                <div
                  className="flex transition-transform duration-500 ease-out"
                  style={{ transform: `translateX(-${currentIndex * 100}%)` }}
                >
                  {questions.map((question) => (
                    <div key={question.wordId} className="min-w-full px-1">
                      <div className="rounded-xl border border-[#E6E8FF] bg-[#F7F8FF] pb-10 pt-5 text-center">
                        <div className="flex flex-col items-center gap-2 py-8">
                          <div className="relative inline-block max-w-[680px] text-center">
                            <h2 className="text-4xl font-bold text-gray-900">{question.wordKr}</h2>
                            <button
                              type="button"
                              onClick={() => playPronunciation(question)}
                              className="absolute left-full top-1/2 inline-flex -translate-y-1/2 items-center gap-2 rounded-full border border-[#D7E7FF] bg-white p-3 text-sm font-medium text-[#2D9CDB] shadow-sm transition hover:-translate-y-[55%] hover:shadow ml-3"
                            >
                              <Volume2 size={18} />
                            </button>
                          </div>
                          {speakingId === String(question.wordId) && (
                            <span className="text-xs text-[#2D9CDB]">재생 중...</span>
                          )}
                        </div>
                        <div className="px-5 py-3 text-base text-gray-600 space-y-2">
                          <p>{question.definitionKr}</p>
                          <p>{question.wordVn} · {question.definitionVn}</p>
                        </div>
                      </div>
                    </div>
                  ))}
                  <div className="min-w-full px-1">
                    <div className="rounded-xl border border-[#E6E8FF] bg-[#F7F8FF] pb-10 pt-5 text-center">
                      <div className="flex flex-col items-center gap-4 py-10">
                        <h2 className="text-3xl font-bold text-gray-900">단어 학습 완료!</h2>
                        <p className="text-base text-gray-600">
                          이어서 문장 순서 맞추기 학습을 진행할까요?
                        </p>
                      </div>
                      <div className="flex flex-wrap items-center justify-center gap-4 pb-6">
                        <button
                          type="button"
                          onClick={() => navigate('/daily/sentence-order')}
                          className="rounded-full bg-[#2D9CDB] px-6 py-2 text-sm font-semibold text-white transition hover:-translate-y-0.5 hover:shadow-md"
                        >
                          문장 순서 맞추기 하러 가기
                        </button>
                        <button
                          type="button"
                          onClick={() => navigate('/main')}
                          className="rounded-full border border-gray-200 bg-white px-6 py-2 text-sm font-semibold text-gray-700 transition hover:border-gray-300"
                        >
                          홈으로 이동하기
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </section>

            {/* 네비게이션 */}
            <div className="mt-8 flex flex-wrap items-center justify-between gap-4">
              <button
                type="button"
                onClick={handlePrev}
                disabled={isFirst}
                className={`${NAV_BUTTON_BASE} ${isFirst ? NAV_BUTTON_DISABLED : NAV_BUTTON_ENABLED}`}
              >
                <ChevronLeft size={10} />
                이전
              </button>

              <div className="text-sm text-gray-500">
                {Math.min(currentIndex + 1, total)} / {total}
              </div>

              <button
                type="button"
                onClick={handleNext}
                disabled={isEndCard}
                className={`inline-flex items-center gap-2 rounded-full px-5 py-2 text-sm font-semibold shadow transition ${
                  isEndCard
                    ? 'cursor-not-allowed bg-gray-200 text-gray-400'
                    : 'bg-[#2D9CDB] text-white hover:-translate-y-0.5 hover:shadow-md'
                }`}
              >
                {isLast ? '완료' : '다음'}
                <ChevronRight size={10} />
              </button>
            </div>
          </>
        )}
        </main>
      </div>
    </div>
  )
}

export default WordStudy
