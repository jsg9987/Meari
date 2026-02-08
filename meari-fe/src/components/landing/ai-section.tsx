"use client"

import { motion } from "framer-motion"
import { ChevronRight } from "lucide-react"

function PhonemeBar({ phoneme, score, delay }: { phoneme: string; score: number; delay: number }) {
  const getColor = (s: number) => {
    if (s >= 90) return "bg-emerald-500"
    if (s >= 70) return "bg-amber-500"
    return "bg-red-500"
  }

  return (
    <motion.div
      initial={{ opacity: 0, scaleY: 0 }}
      whileInView={{ opacity: 1, scaleY: 1 }}
      viewport={{ once: false }}
      transition={{ duration: 0.5, delay }}
      className="flex flex-col items-center gap-1"
      style={{ transformOrigin: "bottom" }}
    >
      <span className="text-[10px] text-navy-faint">{score}</span>
      <div className="w-6 bg-gray-100 rounded-full overflow-hidden" style={{ height: "60px" }}>
        <div
          className={`w-full rounded-full ${getColor(score)} transition-all`}
          style={{ height: `${score}%`, marginTop: `${100 - score}%` }}
        />
      </div>
      <span className="text-xs text-navy font-medium">{phoneme}</span>
    </motion.div>
  )
}

export function AISection() {
  const phonemes = [
    { phoneme: "저", score: 95 },
    { phoneme: "는", score: 88 },
    { phoneme: "우", score: 92 },
    { phoneme: "영", score: 76 },
    { phoneme: "우", score: 94 },
    { phoneme: "변", score: 82 },
    { phoneme: "호", score: 90 },
    { phoneme: "사", score: 68 },
    { phoneme: "입", score: 91 },
    { phoneme: "니", score: 87 },
    { phoneme: "다", score: 93 },
  ]

  return (
    <section id="ai-analysis" className="relative py-32 bg-white overflow-hidden">
      {/* Background accent */}
      <div
        className="absolute pointer-events-none"
        style={{
          top: "50%",
          right: "-200px",
          transform: "translateY(-50%)",
          width: "600px",
          height: "600px",
          background: "radial-gradient(circle, oklch(0.64 0.23 255 / 0.05) 0%, transparent 70%)",
        }}
      />

      <div className="max-w-6xl mx-auto px-6">
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-16 items-center">
          {/* Left: Text */}
          <motion.div
            initial={{ opacity: 0, x: -40 }}
            whileInView={{ opacity: 1, x: 0 }}
            viewport={{ once: false, amount: 0.3 }}
            transition={{ duration: 0.7 }}
          >
            <span className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-purple-50 text-purple-600 text-sm font-medium mb-4">
              <svg className="w-3.5 h-3.5" viewBox="0 0 16 16" fill="currentColor">
                <path d="M8 0L9.5 5.5L15 7L9.5 8.5L8 14L6.5 8.5L1 7L6.5 5.5L8 0Z" />
              </svg>
              AI 발음 분석
            </span>
            <h2 className="text-3xl md:text-4xl lg:text-5xl font-bold text-navy tracking-tight mb-6">
              음소 단위로
              <br />
              <span className="bg-gradient-to-r from-purple-500 to-accent-blue bg-clip-text text-transparent">
                정밀 분석
              </span>
            </h2>
            <p className="text-navy-muted leading-relaxed mb-8 text-lg">
              Wav2Vec 2.0 기반의 AI 모델이 음성 데이터를 음소 단위로 분해하여 분석합니다. 
              정확도, 유창성, 억양, 속도까지 다각도로 평가하여 맞춤형 피드백을 제공합니다.
            </p>

            <div className="space-y-4 mb-8">
              {[
                { label: "음소별 정확도 분석", desc: "각 음소의 발음 정확도를 시각적으로 표시" },
                { label: "원어민 비교 분석", desc: "원어민 발음과의 차이를 파형으로 비교" },
                { label: "개인화 피드백", desc: "반복적인 오류 패턴을 찾아 맞춤 교정 제안" },
              ].map((item) => (
                <div key={item.label} className="flex items-start gap-3">
                  <div className="w-5 h-5 rounded-full bg-accent-blue/10 flex items-center justify-center mt-0.5 shrink-0">
                    <svg className="w-3 h-3 text-accent-blue" viewBox="0 0 16 16" fill="currentColor">
                      <path d="M13.78 4.22a.75.75 0 010 1.06l-7.25 7.25a.75.75 0 01-1.06 0L2.22 9.28a.75.75 0 011.06-1.06L6 10.94l6.72-6.72a.75.75 0 011.06 0z" />
                    </svg>
                  </div>
                  <div>
                    <p className="text-navy font-medium text-sm">{item.label}</p>
                    <p className="text-navy-faint text-sm">{item.desc}</p>
                  </div>
                </div>
              ))}
            </div>

            <button className="flex items-center gap-2 px-5 py-2.5 bg-accent-blue text-white rounded-lg hover:opacity-90 transition-opacity text-sm font-medium">
              AI 분석 체험하기
              <ChevronRight className="w-4 h-4" />
            </button>
          </motion.div>

          {/* Right: Analysis mockup */}
          <motion.div
            initial={{ opacity: 0, x: 40 }}
            whileInView={{ opacity: 1, x: 0 }}
            viewport={{ once: false, amount: 0.3 }}
            transition={{ duration: 0.7, delay: 0.2 }}
            className="relative"
          >
            <div className="bg-white rounded-2xl border border-gray-200 shadow-xl shadow-gray-200/50 overflow-hidden">
              {/* Header */}
              <div className="px-6 py-4 border-b border-gray-100 flex items-center justify-between">
                <div>
                  <p className="text-navy font-medium">발음 분석 리포트</p>
                  <p className="text-navy-faint text-xs mt-0.5">2024.01.15 세션 #12</p>
                </div>
                <div className="flex items-center gap-2">
                  <span className="text-3xl font-bold text-accent-blue">87</span>
                  <span className="text-navy-faint text-xs">/100</span>
                </div>
              </div>

              {/* Score breakdown */}
              <div className="px-6 py-4 grid grid-cols-4 gap-4 border-b border-gray-100">
                {[
                  { label: "정확도", score: 92, color: "text-blue-500" },
                  { label: "유창성", score: 85, color: "text-emerald-500" },
                  { label: "억양", score: 78, color: "text-amber-500" },
                  { label: "속도", score: 90, color: "text-purple-500" },
                ].map((item) => (
                  <div key={item.label} className="text-center">
                    <p className={`text-xl font-bold ${item.color}`}>{item.score}</p>
                    <p className="text-navy-faint text-[10px] mt-0.5">{item.label}</p>
                  </div>
                ))}
              </div>

              {/* Analyzed sentence */}
              <div className="px-6 py-4 border-b border-gray-100">
                <p className="text-xs text-navy-faint mb-2">분석 문장</p>
                <p className="text-navy font-medium">
                  &quot;저는 우영우 변호사입니다&quot;
                </p>
              </div>

              {/* Phoneme analysis */}
              <div className="px-6 py-6">
                <p className="text-xs text-navy-faint mb-4">음소별 정확도</p>
                <div className="flex items-end justify-center gap-2">
                  {phonemes.map((p, i) => (
                    <PhonemeBar key={i} phoneme={p.phoneme} score={p.score} delay={i * 0.05} />
                  ))}
                </div>
              </div>

              {/* Feedback */}
              <div className="px-6 py-4 bg-amber-50/50 border-t border-amber-100/50">
                <div className="flex items-start gap-2">
                  <svg className="w-4 h-4 text-amber-500 mt-0.5 shrink-0" viewBox="0 0 16 16" fill="currentColor">
                    <path d="M8 0L9.5 5.5L15 7L9.5 8.5L8 14L6.5 8.5L1 7L6.5 5.5L8 0Z" />
                  </svg>
                  <div>
                    <p className="text-amber-800 text-sm font-medium">AI 피드백</p>
                    <p className="text-amber-700 text-xs mt-1 leading-relaxed">
                      &apos;사&apos; 발음에서 치경음 위치를 조금 더 앞으로 이동시켜보세요. 
                      &apos;영&apos;의 비음 처리가 약간 부족합니다. 전반적으로 좋은 발음입니다!
                    </p>
                  </div>
                </div>
              </div>
            </div>

            {/* Decorative elements */}
            <div className="absolute -top-4 -right-4 w-24 h-24 bg-accent-blue/5 rounded-full blur-2xl" />
            <div className="absolute -bottom-4 -left-4 w-32 h-32 bg-purple-500/5 rounded-full blur-2xl" />
          </motion.div>
        </div>
      </div>
    </section>
  )
}
