"use client"

import { useRef, useState } from "react"
import { motion, useInView } from "framer-motion"
import { DoorOpen, UserCheck, Mic2, BarChart3, Check } from "lucide-react"
import type { ReactNode } from "react"
import CreateRoomMockModal from "./create-room-mock-modal"

const CHARACTERS = [
  { name: "선엽", roleId: 1 },
  { name: "하나", roleId: 2 },
  { name: "오타니", roleId: 3 },
  { name: "야마모토", roleId: 4 },
]

function CharacterSelectVisual() {
  const [selectedId, setSelectedId] = useState(1)

  return (
    <div className="bg-white rounded-2xl shadow-lg border border-gray-200 w-full max-w-sm overflow-hidden">
      {/* 헤더 */}
      <div className="flex items-center justify-between px-5 py-4 border-b border-gray-200">
        <h3 className="text-base font-semibold text-gray-900">캐릭터 선택</h3>
      </div>

      {/* 캐릭터 그리드 */}
      <div className="p-4">
        <div className="grid grid-cols-2 gap-3">
          {CHARACTERS.map((char) => {
            const isSelected = selectedId === char.roleId
            return (
              <button
                key={char.roleId}
                onClick={() => setSelectedId(char.roleId)}
                className={`p-4 rounded-xl border-2 text-left transition-all cursor-pointer ${
                  isSelected
                    ? "border-blue-600 bg-blue-50"
                    : "border-gray-200 bg-white hover:border-blue-300 hover:bg-gray-50"
                }`}
              >
                <div className="flex items-center justify-between">
                  <span className="text-base font-semibold text-gray-900">{char.name}</span>
                  {isSelected && (
                    <div className="w-6 h-6 bg-blue-600 rounded-full flex items-center justify-center">
                      <Check size={14} className="text-white" />
                    </div>
                  )}
                </div>
                <div className="text-xs text-gray-500 mt-1.5">역할 ID: {char.roleId}</div>
              </button>
            )
          })}
        </div>
      </div>

      {/* 하단 버튼 */}
      <div className="px-4 pb-4">
        <div className="bg-blue-600 text-white text-sm font-semibold py-2.5 rounded-lg flex items-center justify-center gap-2">
          <Check size={16} />
          <span>선택 완료</span>
        </div>
      </div>
    </div>
  )
}

const METRICS = [
  { label: "정확도", score: 92, color: "bg-blue-500" },
  { label: "발음", score: 85, color: "bg-emerald-500" },
  { label: "억양", score: 78, color: "bg-amber-500" },
]

function AIReportVisual() {
  const ref = useRef(null)
  const isInView = useInView(ref, { once: false, amount: 0.5 })

  return (
    <div ref={ref} className="bg-gray-50 rounded-2xl border border-gray-200 p-6 w-full max-w-sm">
      <div className="flex items-center justify-between mb-5">
        <span className="text-sm text-navy-muted">종합 점수</span>
        <span className="text-3xl font-bold text-accent-blue">87</span>
      </div>
      <div className="space-y-4">
        {METRICS.map((metric, i) => (
          <div key={metric.label}>
            <div className="flex items-center justify-between mb-1.5">
              <span className="text-sm text-navy-muted">{metric.label}</span>
              <span className="text-sm text-navy font-medium">{metric.score}%</span>
            </div>
            <div className="h-2 bg-gray-200 rounded-full overflow-hidden">
              <div
                className={`h-full rounded-full ${metric.color} transition-all duration-1000 ease-out`}
                style={{
                  width: isInView ? `${metric.score}%` : "0%",
                  transitionDelay: `${i * 150}ms`,
                }}
              />
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}

interface Step {
  number: string
  icon: typeof DoorOpen
  title: string
  description: string
  visual: ReactNode
}

const steps: Step[] = [
  {
    number: "01",
    icon: DoorOpen,
    title: "방 만들기 & 입장",
    description: "테마별로 학습 방을 검색하거나 직접 만들어 친구들과 함께 입장하세요. 비밀번호 설정도 가능합니다.",
    visual: <CreateRoomMockModal />,
  },
  {
    number: "02",
    icon: UserCheck,
    title: "역할 선택 & 영상 시청",
    description: "학습할 영상의 캐릭터 역할을 선택하세요. 자막과 타임스탬프가 자동으로 동기화됩니다.",
    visual: <CharacterSelectVisual />,
  },
  {
    number: "03",
    icon: Mic2,
    title: "자막에 맞춰 쉐도잉",
    description: "본인 차례가 되면 자막을 따라 발화하세요. 브라우저가 자동으로 해당 구간을 녹음합니다.",
    visual: (
      <div className="bg-zinc-900 rounded-2xl border border-zinc-700 p-6 w-full max-w-sm">
        <div className="flex items-center gap-2 mb-5">
          <div className="w-2.5 h-2.5 rounded-full bg-green-400 animate-recording" />
          <span className="text-green-400 text-sm font-medium">녹음 중</span>
          <span className="text-zinc-500 text-sm ml-auto">00:03.2</span>
        </div>
        <div className="flex items-center justify-center gap-0.75 h-16 mb-5">
          {Array.from({ length: 30 }).map((_, i) => (
            <div
              key={i}
              className="w-1.5 bg-accent-blue rounded-full"
              style={{
                animation: `waveform ${0.8 + (i % 5) * 0.15}s ease-in-out ${i * 0.05}s infinite`,
                opacity: 0.5 + Math.sin(i * 0.3) * 0.5,
              }}
            />
          ))}
        </div>
        <div className="bg-zinc-800 rounded-xl p-4">
          <p className="text-white text-base text-center font-medium">
            &quot;고래가 바다를 자유롭게 헤엄칩니다&quot;
          </p>
          <p className="text-zinc-500 text-xs text-center mt-1.5">The whale swims freely in the sea</p>
        </div>
        <div className="mt-4 flex items-center justify-center">
          <span className="text-sm text-accent-blue bg-accent-blue/10 px-4 py-1.5 rounded-full">유진님의 차례</span>
        </div>
      </div>
    ),
  },
  {
    number: "04",
    icon: BarChart3,
    title: "AI 리포트 확인",
    description: "세션 종료 후 AI가 정확도, 발음, 억양을 분석하여 상세 리포트를 제공합니다.",
    visual: <AIReportVisual />,
  },
]

export function   HowItWorks() {
  return (
    <section id="how-it-works" className="relative py-32 bg-white">
      <div className="max-w-6xl mx-auto px-6">
        <motion.div
          initial={{ opacity: 0, y: 30 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: false, amount: 0.3 }}
          transition={{ duration: 0.6 }}
          className="text-center mb-20"
        >
          <span className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-emerald-50 text-emerald-600 text-sm font-medium mb-4">
            간단한 4단계
          </span>
          <h2 className="text-3xl md:text-4xl lg:text-5xl font-bold text-navy tracking-tight">
            이렇게 학습해요
          </h2>
          <p className="mt-4 text-navy-muted text-lg max-w-lg mx-auto">
            방에 입장해서 AI 리포트를 받기까지, 단 4단계면 충분합니다
          </p>
        </motion.div>

        <div className="space-y-24">
          {steps.map((step, index) => {
            const Icon = step.icon
            const isEven = index % 2 === 0
            return (
              <motion.div
                key={step.number}
                initial={{ opacity: 0, x: isEven ? -60 : 60 }}
                whileInView={{ opacity: 1, x: 0 }}
                viewport={{ once: false, amount: 0.3 }}
                transition={{ duration: 0.7, ease: [0.22, 1, 0.36, 1] }}
                className={`flex flex-col ${isEven ? "lg:flex-row" : "lg:flex-row-reverse"} items-center gap-12 lg:gap-20`}
              >
                <div className="flex-1 max-w-md">
                  <div className="flex items-center gap-3 mb-4">
                    <span className="text-5xl font-bold text-accent-blue/15">{step.number}</span>
                    <div className="w-10 h-10 rounded-xl bg-accent-blue/10 flex items-center justify-center">
                      <Icon className="w-5 h-5 text-accent-blue" />
                    </div>
                  </div>
                  <h3 className="text-2xl font-bold text-navy mb-3">{step.title}</h3>
                  <p className="text-navy-muted leading-relaxed">{step.description}</p>
                </div>

                <motion.div
                  initial={{ opacity: 0, scale: 0.9 }}
                  whileInView={{ opacity: 1, scale: 1 }}
                  viewport={{ once: false, amount: 0.3 }}
                  transition={{ duration: 0.6, delay: 0.2 }}
                  className="flex-1 flex justify-center"
                >
                  {step.visual}
                </motion.div>
              </motion.div>
            )
          })}
        </div>
      </div>
    </section>
  )
}
