import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ChevronLeft, ChevronRight } from 'lucide-react'
import logoWhite from '../../assets/images/common/logo-white.svg'
import progressBarBg from '../../assets/images/daily/word-study/word-study-top-frame-2.svg'
import dailyCharacter from '../../assets/images/daily/word-study/daily-study-character.svg'
import goalTrophy from '../../assets/images/daily/word-study/goal-trophy.svg'
import { getQuiz, type QuizSentence } from '../../api/quiz.api'

const TOTAL_QUESTIONS = 5
const SKY_ASPECT = 'aspect-[1520/223]'
const PROGRESS_TRACK_WIDTH = 'w-full'
const PROGRESS_OVERLAY_GAP = 'pt-4'
const PROGRESS_ROW_GAP = 'gap-3'
const NAV_BUTTON_BASE =
  'inline-flex items-center gap-2 rounded-full px-5 py-2 text-sm font-semibold transition'
const NAV_BUTTON_ENABLED = 'bg-white text-gray-700 border border-gray-200 hover:border-[#2D9CDB]'
const NAV_BUTTON_DISABLED = 'cursor-not-allowed bg-gray-100 text-gray-400'

type WordToken = {
  id: string
  text: string
  index: number
}

const makeWordId = (sentenceId: number, index: number, text: string) =>
  `${sentenceId}-${index}-${text}`

const WordPill = ({
  id,
  text,
  onClick,
  isGhost = false
}: {
  id: string
  text: string
  onClick?: (id: string) => void
  isGhost?: boolean
}) => {
  return (
    <button
      type="button"
      style={{ cursor: isGhost ? 'default' : 'pointer' }}
      onClick={() => onClick?.(id)}
      className={`rounded-full border border-[#D7E7FF] bg-white px-4 py-2 text-sm font-semibold text-gray-700 shadow-sm transition ${
        isGhost ? 'opacity-50 cursor-default' : 'hover:-translate-y-0.5 hover:shadow-md'
      }`}
    >
      {text}
    </button>
  )
}

const SentenceOrder = () => {
  const navigate = useNavigate()
  const [currentIndex, setCurrentIndex] = useState(0)
  const [questions, setQuestions] = useState<QuizSentence[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [containers, setContainers] = useState<{ bank: string[]; sentence: string[] }>({
    bank: [],
    sentence: []
  })
  const [feedbackMessage, setFeedbackMessage] = useState<string | null>(null)
  const [isWrongModalOpen, setIsWrongModalOpen] = useState(false)

  const total = questions.length
  const activeQuestion = questions[currentIndex]
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

    const previousBodyOverflow = document.body.style.overflow
    const previousHtmlOverflow = document.documentElement.style.overflow
    document.body.style.overflow = 'hidden'
    document.documentElement.style.overflow = 'hidden'

    const loadQuiz = async () => {
      setIsLoading(true)
      try {
        const response = await getQuiz(TOTAL_QUESTIONS)
        const data = response.data?.data ?? []
        setQuestions(data)
        setCurrentIndex(0)
      } catch (error) {
        console.error('Failed to load quiz data:', error)
        setQuestions([])
      } finally {
        setIsLoading(false)
      }
    }

    loadQuiz()
    return () => {
      document.body.style.overflow = previousBodyOverflow
      document.documentElement.style.overflow = previousHtmlOverflow
    }
  }, [])

  useEffect(() => {
    if (!activeQuestion) return
    const ids = activeQuestion.words.map((word) =>
      makeWordId(activeQuestion.sentence_id, word.index, word.text)
    )
    setContainers({ bank: ids, sentence: [] })
    setFeedbackMessage(null)
    setIsWrongModalOpen(false)
  }, [activeQuestion])

  const handlePrev = () => {
    if (!total) return
    setCurrentIndex((prev) => Math.max(0, prev - 1))
  }

  const handleNext = () => {
    if (!total) return
    if (!activeQuestion) return
    if (currentIndex >= total) return
    const correctIds = [...activeQuestion.words]
      .sort((a, b) => a.index - b.index)
      .map((word) => makeWordId(activeQuestion.sentence_id, word.index, word.text))
    const isCorrect =
      selectedIds.length === correctIds.length &&
      selectedIds.every((id, index) => id === correctIds[index])

    if (!isCorrect) {
      const resetIds = activeQuestion.words.map((word) =>
        makeWordId(activeQuestion.sentence_id, word.index, word.text)
      )
      setContainers({ bank: resetIds, sentence: [] })
      setFeedbackMessage('오답입니다')
      setIsWrongModalOpen(true)
      return
    }

    setFeedbackMessage(null)
    setIsWrongModalOpen(false)
    setCurrentIndex((prev) => Math.min(total, prev + 1))
  }

  const isFirst = currentIndex === 0
  const isLast = currentIndex === total - 1
  const isEndCard = currentIndex >= total && total > 0
  const showContent = !isLoading && total > 0

  const wordMap = useMemo(() => {
    if (!activeQuestion) return new Map<string, WordToken>()
    const map = new Map<string, WordToken>()
    activeQuestion.words.forEach((word) => {
      const id = makeWordId(activeQuestion.sentence_id, word.index, word.text)
      map.set(id, { id, text: word.text, index: word.index })
    })
    return map
  }, [activeQuestion])

  const bankOrderMap = useMemo(() => {
    if (!activeQuestion) return new Map<string, number>()
    const map = new Map<string, number>()
    activeQuestion.words.forEach((word, orderIndex) => {
      const id = makeWordId(activeQuestion.sentence_id, word.index, word.text)
      map.set(id, orderIndex)
    })
    return map
  }, [activeQuestion])

  const bankIds = containers.bank
  const selectedIds = containers.sentence

  const moveToSentence = useCallback(
    (id: string) => {
      setContainers((prev) => {
        if (!prev.bank.includes(id)) return prev
        return {
          bank: prev.bank.filter((itemId) => itemId !== id),
          sentence: [...prev.sentence, id]
        }
      })
      setFeedbackMessage(null)
      setIsWrongModalOpen(false)
    },
    [setContainers]
  )

  const moveToBank = useCallback(
    (id: string) => {
      setContainers((prev) => {
        if (!prev.sentence.includes(id)) return prev
        const nextBank = [...prev.bank, id].sort(
          (a, b) => (bankOrderMap.get(a) ?? 0) - (bankOrderMap.get(b) ?? 0)
        )
        return {
          bank: nextBank,
          sentence: prev.sentence.filter((itemId) => itemId !== id)
        }
      })
      setFeedbackMessage(null)
      setIsWrongModalOpen(false)
    },
    [setContainers, bankOrderMap]
  )

  return (
    <div className="h-screen w-full overflow-y-auto overflow-x-hidden bg-[#695C51]">
      <header className="relative z-20 h-[50px] w-full bg-[#4F4F4F]">
        <div className="mx-auto flex h-full w-full max-w-[75rem] items-center px-6">
          <button
            type="button"
            onClick={() => navigate('/')}
            className="inline-flex items-center"
            aria-label="메인 페이지로 이동"
          >
            <img src={logoWhite} alt="Meari" className="h-[16px] cursor-pointer" />
          </button>
        </div>
      </header>

      <div className="relative z-0 mx-auto w-[calc(100%/1.1)] origin-top-center scale-[1.1] overflow-hidden">
        <main className="mx-auto w-full max-w-[80rem] pb-5 px-10">
          {isLoading && (
            <div className="flex h-[60vh] items-center justify-center">
              <div className="text-center">
                <div className="mx-auto mb-4 h-10 w-10 animate-spin rounded-full border-4 border-blue-500 border-t-transparent" />
                <p className="text-sm text-gray-200">문장 데이터를 불러오는 중...</p>
              </div>
            </div>
          )}

          {!isLoading && total === 0 && (
            <div className="flex h-[60vh] items-center justify-center">
              <p className="text-gray-200">표시할 문장이 없습니다.</p>
            </div>
          )}

          {showContent && (
            <>
              <section
                className={`relative left-1/2 w-screen -translate-x-1/2 bg-[#5255B2] ${SKY_ASPECT}`}
              >
                <div className="relative h-full w-full overflow-hidden bg-[#5255B2]">
                  <img
                    src={progressBarBg}
                    alt="상단 배경"
                    className="absolute inset-0 h-full w-full origin-bottom scale-[0.9091] translate-y-[6%] object-cover object-[50%_100%]"
                  />
                  <div className="relative mx-auto h-full px-[30px]" />
                </div>
              </section>

              <div className={`flex w-full justify-center bg-[#695C51] px-5 ${PROGRESS_OVERLAY_GAP}`}>
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
                  <div
                    className="absolute -top-14 transition-all duration-500 ease-out"
                    style={characterStyle}
                  >
                    <img src={dailyCharacter} alt="character" className="h-13 w-auto drop-shadow-md" />
                  </div>
                  <img
                    src={goalTrophy}
                    alt="goal trophy"
                    className="absolute -top-12.5 right-0 h-12 w-auto drop-shadow-sm"
                  />
                </div>
              </div>

              <section className="rounded-xl border border-gray-200 bg-white pt-4 pb-10 px-5 shadow-sm">
                <div className="mb-2 flex items-center justify-between">
                  <div>
                    <p className="text-sm text-gray-500">
                      {isEndCard ? '완료' : `문장 ${Math.min(currentIndex + 1, total)}`}
                    </p>
                    <p className="text-lg font-semibold text-gray-900">
                      {isEndCard ? '학습이 끝났어요' : '문장 순서를 올바르게 배열하세요'}
                    </p>
                  </div>
                </div>

                <div className="relative overflow-hidden">
                  <div
                    className="flex transition-transform duration-500 ease-out"
                    style={{ transform: `translateX(-${currentIndex * 100}%)` }}
                  >
                    {questions.map((question, questionIndex) => {
                      if (questionIndex !== currentIndex) {
                        return (
                          <div key={question.sentence_id} className="min-w-full px-1">
                            <div className="rounded-xl border border-[#E6E8FF] bg-[#F7F8FF] pb-10 pt-5 text-center" />
                          </div>
                        )
                      }

                      const ordered = [...question.words]
                        .sort((a, b) => a.index - b.index)
                        .map((word) => word.text)
                        .join(' ')

                      return (
                        <div key={question.sentence_id} className="min-w-full px-1">
                          <div className="rounded-xl border border-[#E6E8FF] bg-[#F7F8FF] p-5 text-center">
                            <div className="flex flex-col items-center gap-2 py-7">
                              <div className="relative inline-block max-w-[680px] text-center">
                                <h2 className="text-4xl font-bold text-gray-900">문장 순서 맞추기</h2>
                              </div>
                              <p className="text-sm text-gray-500">{question.text_vn}</p>
                            </div>

                            <div className="mx-auto flex max-w-3xl flex-col gap-[10px] px-5">
                              <div className="rounded-xl border border-[#D7E7FF] bg-white/80 px-6 py-6">
                                <div className="flex flex-wrap justify-center gap-3 min-h-[52px]">
                                  {selectedIds.map((id) => {
                                    const word = wordMap.get(id)
                                    if (!word) return null
                                    return (
                                      <WordPill
                                        key={id}
                                        id={id}
                                        text={word.text}
                                        onClick={(removeId) => moveToBank(removeId)}
                                      />
                                    )
                                  })}
                                  {selectedIds.length === 0 && (
                                    <span className="text-sm text-gray-400">
                                      단어를 클릭해 여기에 놓으세요
                                    </span>
                                  )}
                                </div>
                                <div className="mt-[6px]">
                                  <div className="h-0 border-b-2 border-gray-300" />
                                </div>
                              </div>

                              <div>
                                <div className="flex flex-wrap justify-center gap-3">
                                  {bankIds.map((id) => {
                                    const word = wordMap.get(id)
                                    if (!word) return null
                                    return (
                                      <WordPill
                                        key={id}
                                        id={id}
                                        text={word.text}
                                        onClick={(wordId) => moveToSentence(wordId)}
                                      />
                                    )
                                  })}
                                </div>
                              </div>
                            </div>

                            <div className="mt-5 px-5 text-sm text-gray-500">
                              정답 예시: <span className="font-medium text-gray-700">{ordered}</span>
                            </div>
                          </div>
                        </div>
                      )
                    })}
                    <div className="min-w-full px-1">
                      <div className="rounded-xl border border-[#E6E8FF] bg-[#F7F8FF] pb-10 pt-5 text-center">
                        <div className="flex flex-col items-center gap-4 py-10">
                          <h2 className="text-3xl font-bold text-gray-900">학습이 끝났어요!</h2>
                          <p className="text-base text-gray-600">다음으로 이동할까요?</p>
                        </div>
                        <div className="flex flex-wrap items-center justify-center gap-4 pb-6">
                          <button
                            type="button"
                            onClick={() => navigate('/daily/word-study')}
                            className="rounded-full bg-[#2D9CDB] px-6 py-2 text-sm font-semibold text-white transition hover:-translate-y-0.5 hover:shadow-md"
                          >
                            단어 학습으로 이동
                          </button>
                          <button
                            type="button"
                            onClick={() => navigate('/')}
                            className="rounded-full border border-gray-200 bg-white px-6 py-2 text-sm font-semibold text-gray-700 transition hover:border-gray-300"
                          >
                            홈으로 이동
                          </button>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </section>

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

                <div className="text-sm text-white">
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
      {isWrongModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-6">
          <div className="w-full max-w-sm rounded-xl bg-white p-6 text-center shadow-xl">
            <p className="text-lg font-semibold text-gray-900">{feedbackMessage ?? '오답입니다'}</p>
            <p className="mt-2 text-sm text-gray-600">단어를 다시 배치해주세요.</p>
            <button
              type="button"
              onClick={() => {
                setIsWrongModalOpen(false)
                setFeedbackMessage(null)
              }}
              className="mt-5 rounded-full bg-[#2D9CDB] px-6 py-2 text-sm font-semibold text-white transition hover:-translate-y-0.5 hover:shadow-md"
            >
              확인
            </button>
          </div>
        </div>
      )}
    </div>
  )
}

export default SentenceOrder
