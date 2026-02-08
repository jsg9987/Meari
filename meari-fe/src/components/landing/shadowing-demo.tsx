"use client"

import { motion } from "framer-motion"
import { Play, SkipForward, Volume2 } from "lucide-react"

export function ShadowingDemo() {
  return (
    <section className="relative py-32 bg-gradient-to-b from-white via-gray-50/50 to-white overflow-hidden">
      <div className="max-w-6xl mx-auto px-6">
        <motion.div
          initial={{ opacity: 0, y: 30 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: false, amount: 0.3 }}
          transition={{ duration: 0.6 }}
          className="text-center mb-16"
        >
          <span className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-amber-50 text-amber-600 text-sm font-medium mb-4">
            쉐도잉 세션
          </span>
          <h2 className="text-3xl md:text-4xl lg:text-5xl font-bold text-navy tracking-tight">
            실제 콘텐츠로 생생하게
          </h2>
          <p className="mt-4 text-navy-muted text-lg max-w-lg mx-auto">
            드라마, 영화, 예능 등 다양한 한국어 콘텐츠로 자연스러운 발화를 연습하세요
          </p>
        </motion.div>

        {/* Shadowing session mockup */}
        <motion.div
          initial={{ opacity: 0, y: 50 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: false, amount: 0.2 }}
          transition={{ duration: 0.8, ease: [0.22, 1, 0.36, 1] }}
          className="max-w-4xl mx-auto"
          style={{ perspective: "1200px" }}
        >
          <div
            className="bg-zinc-900 rounded-2xl border border-zinc-700/50 overflow-hidden shadow-2xl"
            style={{
              transform: "rotateX(2deg)",
              transformOrigin: "center bottom",
            }}
          >
            {/* Video area */}
            <div className="relative aspect-video bg-gradient-to-br from-zinc-800 to-zinc-900">
              {/* Fake video frame */}
              <div className="absolute inset-0 flex items-center justify-center">
                <img
                  src=""
                  alt="한국어 드라마 영상 - 두 명의 등장인물이 카페에서 대화하는 장면. 아래에 한국어 자막이 표시됨"
                  className="w-full h-full object-cover opacity-0"
                />
                {/* Placeholder scene */}
                <div className="absolute inset-0 bg-gradient-to-br from-indigo-900/80 via-zinc-800 to-purple-900/60" />
                <div className="absolute inset-0 flex items-center justify-center">
                  <div className="text-center">
                    <div className="w-20 h-20 rounded-full bg-white/10 backdrop-blur-sm flex items-center justify-center mx-auto mb-4 border border-white/20">
                      <Play className="w-8 h-8 text-white ml-1" />
                    </div>
                    <p className="text-white/60 text-sm">드라마 - 이상한 변호사 우영우 EP.3</p>
                  </div>
                </div>
              </div>

              {/* Subtitles overlay */}
              <div className="absolute bottom-0 left-0 right-0 p-6">
                <div className="bg-black/70 backdrop-blur-sm rounded-xl p-4 max-w-xl mx-auto">
                  <div className="flex items-center gap-2 mb-2">
                    <div className="w-5 h-5 rounded-full bg-blue-500 flex items-center justify-center">
                      <span className="text-white text-[9px] font-medium">우</span>
                    </div>
                    <span className="text-blue-400 text-xs">우영우</span>
                    <span className="text-zinc-500 text-xs ml-auto">02:34 - 02:38</span>
                  </div>
                  <p className="text-white text-lg font-medium mb-1">
                    &quot;저는 우영우 변호사입니다. 처음 뵙겠습니다.&quot;
                  </p>
                  <p className="text-zinc-400 text-xs">
                    I am attorney Woo Young-woo. Nice to meet you.
                  </p>
                </div>
              </div>

              {/* Current speaker indicator */}
              <div className="absolute top-4 right-4">
                <div className="flex items-center gap-2 bg-green-500/20 backdrop-blur-sm text-green-400 px-3 py-1.5 rounded-full text-xs border border-green-500/30">
                  <div className="w-2 h-2 rounded-full bg-green-400 animate-recording" />
                  유진님 차례
                </div>
              </div>

              {/* Round indicator */}
              <div className="absolute top-4 left-4">
                <div className="bg-zinc-800/80 backdrop-blur-sm text-zinc-300 px-3 py-1.5 rounded-full text-xs border border-zinc-700">
                  라운드 2/5
                </div>
              </div>
            </div>

            {/* Video controls */}
            <div className="flex items-center gap-4 px-6 py-3 bg-zinc-800/80 border-t border-zinc-700/50">
              {/* Progress */}
              <span className="text-zinc-500 text-xs w-10">02:34</span>
              <div className="flex-1 h-1 bg-zinc-700 rounded-full overflow-hidden">
                <div className="h-full w-[35%] bg-accent-blue rounded-full" />
              </div>
              <span className="text-zinc-500 text-xs w-10">07:12</span>

              <div className="flex items-center gap-2 ml-2">
                <button className="text-zinc-400 hover:text-white transition-colors">
                  <Volume2 className="w-4 h-4" />
                </button>
                <button className="text-zinc-400 hover:text-white transition-colors">
                  <SkipForward className="w-4 h-4" />
                </button>
              </div>
            </div>

            {/* Participants strip */}
            <div className="flex items-center gap-3 px-6 py-3 bg-zinc-900 border-t border-zinc-800">
              <span className="text-zinc-500 text-xs">참여자</span>
              <div className="flex -space-x-2">
                {[
                  { name: "유진", color: "bg-blue-500", active: true },
                  { name: "사라", color: "bg-emerald-500", active: false },
                  { name: "마이크", color: "bg-amber-500", active: false },
                  { name: "하나", color: "bg-purple-500", active: false },
                ].map((user) => (
                  <div
                    key={user.name}
                    className={`w-7 h-7 rounded-full ${user.color} flex items-center justify-center text-white text-[10px] font-medium border-2 ${
                      user.active ? "border-green-400" : "border-zinc-900"
                    }`}
                  >
                    {user.name[0]}
                  </div>
                ))}
              </div>
              <div className="ml-auto flex items-center gap-2">
                <div className="flex items-center gap-1 text-xs text-zinc-500">
                  <div className="w-1.5 h-1.5 rounded-full bg-green-400" />
                  온라인 4명
                </div>
              </div>
            </div>
          </div>
        </motion.div>

        {/* Content categories */}
        <motion.div
          initial={{ opacity: 0, y: 30 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: false, amount: 0.3 }}
          transition={{ duration: 0.6, delay: 0.3 }}
          className="mt-16 flex flex-wrap items-center justify-center gap-3"
        >
          {["드라마", "영화", "예능", "뉴스", "K-POP", "애니메이션", "다큐멘터리"].map((genre, i) => (
            <span
              key={genre}
              className={`px-4 py-2 rounded-full text-sm border transition-colors cursor-pointer ${
                i === 0
                  ? "bg-accent-blue text-white border-accent-blue"
                  : "bg-white text-navy-muted border-gray-200 hover:border-accent-blue hover:text-accent-blue"
              }`}
            >
              {genre}
            </span>
          ))}
        </motion.div>
      </div>
    </section>
  )
}
