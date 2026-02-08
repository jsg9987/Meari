"use client"

import { motion } from "framer-motion"
import { Shuffle, BookOpen, MessageCircle, GripVertical } from "lucide-react"

export function StudyToolsSection() {
  return (
    <section id="study-tools" className="relative py-32 bg-gradient-to-b from-white to-gray-50/50">
      <div className="max-w-6xl mx-auto px-6">
        <motion.div
          initial={{ opacity: 0, y: 30 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: false, amount: 0.3 }}
          transition={{ duration: 0.6 }}
          className="text-center mb-16"
        >
          <span className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-50 text-indigo-600 text-sm font-medium mb-4">
            학습 도구
          </span>
          <h2 className="text-3xl md:text-4xl lg:text-5xl font-bold text-navy tracking-tight">
            쉐도잉 전후로 실력을 다져요
          </h2>
          <p className="mt-4 text-navy-muted text-lg max-w-lg mx-auto">
            문장 구조 학습, 단어 공부, AI 대화까지 다양한 보조 학습 도구를 활용하세요
          </p>
        </motion.div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {/* Sentence Ordering */}
          <motion.div
            initial={{ opacity: 0, y: 40 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: false, amount: 0.2 }}
            transition={{ duration: 0.6 }}
            className="bg-white rounded-2xl border border-gray-200 p-6 hover:shadow-lg transition-shadow"
          >
            <div className="w-12 h-12 rounded-2xl bg-indigo-500/10 flex items-center justify-center mb-4">
              <Shuffle className="w-6 h-6 text-indigo-500" />
            </div>
            <h3 className="text-xl font-bold text-navy mb-2">문장 순서 맞추기</h3>
            <p className="text-navy-muted text-sm leading-relaxed mb-6">
              무작위로 섞인 문장 조각을 올바른 순서로 배열하며 한국어 문장 구조를 학습합니다.
            </p>

            {/* Mockup */}
            <div className="space-y-2">
              {[
                { text: "저는", order: 1, placed: true },
                { text: "한국어를", order: 2, placed: true },
                { text: "매일", order: 3, placed: false },
                { text: "공부합니다", order: 4, placed: false },
              ].map((word, i) => (
                <div
                  key={word.text}
                  className={`flex items-center gap-2 p-2.5 rounded-lg border transition-colors ${
                    word.placed
                      ? "border-indigo-200 bg-indigo-50/50"
                      : "border-gray-200 bg-gray-50 border-dashed"
                  }`}
                >
                  <GripVertical className="w-3.5 h-3.5 text-gray-300" />
                  <span className="text-xs text-indigo-400 font-medium w-4">{i + 1}</span>
                  <span className={`text-sm ${word.placed ? "text-navy" : "text-navy-faint"}`}>
                    {word.text}
                  </span>
                  {word.placed && (
                    <svg className="w-4 h-4 text-indigo-500 ml-auto" viewBox="0 0 16 16" fill="currentColor">
                      <path d="M13.78 4.22a.75.75 0 010 1.06l-7.25 7.25a.75.75 0 01-1.06 0L2.22 9.28a.75.75 0 011.06-1.06L6 10.94l6.72-6.72a.75.75 0 011.06 0z" />
                    </svg>
                  )}
                </div>
              ))}
            </div>
          </motion.div>

          {/* Vocabulary */}
          <motion.div
            initial={{ opacity: 0, y: 40 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: false, amount: 0.2 }}
            transition={{ duration: 0.6, delay: 0.1 }}
            className="bg-white rounded-2xl border border-gray-200 p-6 hover:shadow-lg transition-shadow"
          >
            <div className="w-12 h-12 rounded-2xl bg-emerald-500/10 flex items-center justify-center mb-4">
              <BookOpen className="w-6 h-6 text-emerald-500" />
            </div>
            <h3 className="text-xl font-bold text-navy mb-2">단어 학습</h3>
            <p className="text-navy-muted text-sm leading-relaxed mb-6">
              주제별 주요 단어를 빈칸 채우기로 학습하고, 국립국어원 사전과 연동된 검색을 활용하세요.
            </p>

            {/* Mockup */}
            <div className="space-y-3">
              <div className="bg-emerald-50/50 rounded-xl p-4 border border-emerald-100">
                <p className="text-navy text-sm mb-3">
                  법정에서 <span className="inline-block w-16 h-6 bg-emerald-200/50 rounded border border-emerald-300 border-dashed mx-1 align-middle" /> 을 제출했습니다.
                </p>
                <div className="flex flex-wrap gap-2">
                  {["증거", "변호", "판결"].map((word) => (
                    <span
                      key={word}
                      className={`px-3 py-1.5 rounded-lg text-xs font-medium border cursor-pointer transition-colors ${
                        word === "증거"
                          ? "bg-emerald-500 text-white border-emerald-500"
                          : "bg-white text-navy-muted border-gray-200 hover:border-emerald-300"
                      }`}
                    >
                      {word}
                    </span>
                  ))}
                </div>
              </div>
              <div className="flex items-center gap-2 p-3 bg-gray-50 rounded-lg border border-gray-200">
                <svg className="w-4 h-4 text-navy-faint" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.5">
                  <circle cx="7" cy="7" r="5" />
                  <path d="M11 11L14 14" strokeLinecap="round" />
                </svg>
                <span className="text-navy-faint text-xs">국립국어원 사전 검색...</span>
              </div>
            </div>
          </motion.div>

          {/* Kopic AI */}
          <motion.div
            initial={{ opacity: 0, y: 40 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: false, amount: 0.2 }}
            transition={{ duration: 0.6, delay: 0.2 }}
            className="bg-white rounded-2xl border border-gray-200 p-6 hover:shadow-lg transition-shadow"
          >
            <div className="w-12 h-12 rounded-2xl bg-rose-500/10 flex items-center justify-center mb-4">
              <MessageCircle className="w-6 h-6 text-rose-500" />
            </div>
            <h3 className="text-xl font-bold text-navy mb-2">Kopic AI 대화</h3>
            <p className="text-navy-muted text-sm leading-relaxed mb-6">
              특정 테마와 상황을 선택하여 AI와 한국어로 대화하고 즉각적인 피드백을 받으세요.
            </p>

            {/* Chat mockup */}
            <div className="space-y-3">
              <div className="flex gap-2">
                <div className="w-6 h-6 rounded-full bg-rose-500/10 flex items-center justify-center shrink-0">
                  <svg className="w-3 h-3 text-rose-500" viewBox="0 0 16 16" fill="currentColor">
                    <path d="M8 0L9.5 5.5L15 7L9.5 8.5L8 14L6.5 8.5L1 7L6.5 5.5L8 0Z" />
                  </svg>
                </div>
                <div className="bg-gray-50 rounded-xl rounded-tl-none p-3 text-sm text-navy">
                  안녕하세요! 오늘은 카페에서 주문하는 상황을 연습해볼까요?
                </div>
              </div>
              <div className="flex gap-2 justify-end">
                <div className="bg-accent-blue/10 rounded-xl rounded-tr-none p-3 text-sm text-navy">
                  네, 좋아요! 아메리카노 한 잔 주세요.
                </div>
              </div>
              <div className="flex gap-2">
                <div className="w-6 h-6 rounded-full bg-rose-500/10 flex items-center justify-center shrink-0">
                  <svg className="w-3 h-3 text-rose-500" viewBox="0 0 16 16" fill="currentColor">
                    <path d="M8 0L9.5 5.5L15 7L9.5 8.5L8 14L6.5 8.5L1 7L6.5 5.5L8 0Z" />
                  </svg>
                </div>
                <div className="bg-gray-50 rounded-xl rounded-tl-none p-3">
                  <p className="text-sm text-navy">좋은 문장이에요! 💯</p>
                  <p className="text-xs text-emerald-500 mt-1">발음 점수: 91/100</p>
                </div>
              </div>
            </div>
          </motion.div>
        </div>
      </div>
    </section>
  )
}
