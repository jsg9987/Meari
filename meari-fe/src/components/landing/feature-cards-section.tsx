"use client"

import { motion } from "framer-motion"
import { Video, Brain, FileText, ChevronRight } from "lucide-react"

const features = [
  {
    icon: Video,
    title: "실시간 화상 쉐도잉",
    description: "4인이 동시에 화상으로 접속하여 한국어 콘텐츠를 함께 쉐도잉합니다. WebRTC 기반의 안정적인 실시간 통신을 제공합니다.",
    gradient: "from-blue-500/10 to-indigo-500/10",
    iconBg: "bg-blue-500/10",
    iconColor: "text-blue-500",
    mockup: (
      <div className="mt-6 grid grid-cols-2 gap-2">
        {["유진", "사라", "마이크", "하나"].map((name, i) => (
          <div
            key={name}
            className={`aspect-video rounded-lg flex items-center justify-center ${
              ["bg-blue-500/20", "bg-emerald-500/20", "bg-amber-500/20", "bg-purple-500/20"][i]
            }`}
          >
            <div className="w-8 h-8 rounded-full bg-white/60 flex items-center justify-center">
              <span className="text-navy text-xs font-medium">{name[0]}</span>
            </div>
          </div>
        ))}
      </div>
    ),
  },
  {
    icon: Brain,
    title: "AI 발음 분석",
    description: "Wav2Vec 2.0 기반 음소 분석 모델이 발음의 정확도, 유창성, 억양, 속도를 다각도로 평가합니다.",
    gradient: "from-purple-500/10 to-pink-500/10",
    iconBg: "bg-purple-500/10",
    iconColor: "text-purple-500",
    mockup: (
      <div className="mt-6 space-y-3">
        <div className="flex items-center justify-center gap-[2px] h-10">
          {Array.from({ length: 24 }).map((_, i) => (
            <div
              key={i}
              className="w-1 bg-purple-400 rounded-full"
              style={{
                height: `${6 + Math.abs(Math.sin(i * 0.4)) * 20}px`,
                opacity: 0.4 + Math.abs(Math.sin(i * 0.3)) * 0.6,
              }}
            />
          ))}
        </div>
        <div className="flex items-center justify-between text-xs">
          <span className="text-navy-faint">정확도</span>
          <span className="text-purple-500 font-medium">94%</span>
        </div>
        <div className="h-1.5 bg-purple-100 rounded-full overflow-hidden">
          <div className="h-full w-[94%] bg-purple-500 rounded-full" />
        </div>
      </div>
    ),
  },
  {
    icon: FileText,
    title: "맞춤형 학습 리포트",
    description: "세션마다 개인별 상세 리포트를 생성합니다. 문장별 분석, 시각화된 그래프로 학습 성과를 한눈에 확인하세요.",
    gradient: "from-emerald-500/10 to-teal-500/10",
    iconBg: "bg-emerald-500/10",
    iconColor: "text-emerald-500",
    mockup: (
      <div className="mt-6 space-y-2">
        {[
          { label: "이번 세션", score: 87, prev: 82 },
          { label: "지난 세션", score: 82, prev: 75 },
          { label: "첫 세션", score: 75, prev: 0 },
        ].map((s) => (
          <div key={s.label} className="flex items-center gap-3 p-2 rounded-lg bg-white/60">
            <span className="text-xs text-navy-faint w-16">{s.label}</span>
            <div className="flex-1 h-1.5 bg-emerald-100 rounded-full overflow-hidden">
              <div className="h-full bg-emerald-500 rounded-full" style={{ width: `${s.score}%` }} />
            </div>
            <span className="text-xs text-emerald-600 font-medium">{s.score}</span>
          </div>
        ))}
      </div>
    ),
  },
]

export function FeatureCardsSection() {
  return (
    <section id="features" className="relative py-32 bg-white">
      <div className="max-w-6xl mx-auto px-6">
        <div className="flex flex-col lg:flex-row lg:items-end lg:justify-between gap-6 mb-16">
          <motion.div
            initial={{ opacity: 0, y: 30 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: false, amount: 0.3 }}
            transition={{ duration: 0.6 }}
          >
            <span className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-blue-50 text-blue-600 text-sm font-medium mb-4">
              핵심 기능
            </span>
            <h2 className="text-3xl md:text-4xl lg:text-5xl font-bold text-navy tracking-tight max-w-lg">
              효과적인 한국어 학습을 위한 모든 것
            </h2>
          </motion.div>
          <motion.p
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: false, amount: 0.3 }}
            transition={{ duration: 0.6, delay: 0.1 }}
            className="text-navy-muted max-w-md leading-relaxed lg:text-right"
          >
            실시간 화상 쉐도잉, AI 발음 분석, 맞춤 리포트까지.
            <br />
            메아리가 제공하는 3가지 핵심 기능을 만나보세요.
          </motion.p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {features.map((feature, index) => {
            const Icon = feature.icon
            return (
              <motion.div
                key={feature.title}
                initial={{ opacity: 0, y: 40, scale: 0.95 }}
                whileInView={{ opacity: 1, y: 0, scale: 1 }}
                viewport={{ once: false, amount: 0.2 }}
                transition={{ duration: 0.6, delay: index * 0.1 }}
                className={`group relative p-6 rounded-3xl border border-gray-200 bg-gradient-to-br ${feature.gradient} hover:border-gray-300 hover:shadow-lg transition-all duration-300 cursor-pointer`}
              >
                <div className={`w-12 h-12 rounded-2xl ${feature.iconBg} flex items-center justify-center mb-4`}>
                  <Icon className={`w-6 h-6 ${feature.iconColor}`} />
                </div>
                <h3 className="text-xl font-bold text-navy mb-2">{feature.title}</h3>
                <p className="text-navy-muted text-sm leading-relaxed">{feature.description}</p>
                {feature.mockup}
                <div className="mt-4 flex items-center gap-1 text-accent-blue text-sm font-medium opacity-0 group-hover:opacity-100 transition-opacity">
                  자세히 보기 <ChevronRight className="w-4 h-4" />
                </div>
              </motion.div>
            )
          })}
        </div>
      </div>
    </section>
  )
}
