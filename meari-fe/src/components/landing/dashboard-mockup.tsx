"use client"

import React, { useState } from "react"
import { motion } from "framer-motion"
import {
  Inbox,
  FolderKanban,
  FileText,
  Users,
  MessageCircle,
  Lock,
  Unlock,
  Copy,
  Check,
  LayoutList,
  LayoutGrid,
  Maximize2,
} from "lucide-react"

function ThemeCard({
  icon,
  name,
  description,
  contentCount,
  color,
}: {
  icon: string
  name: string
  description: string
  contentCount: number
  color: string
}) {
  return (
    <div className="bg-white rounded-lg p-3 hover:bg-gray-50 transition-all cursor-pointer border border-gray-200 hover:border-gray-300">
      <div className="flex items-start gap-3">
        <div className={`${color} w-10 h-10 rounded-lg flex items-center justify-center text-xl shrink-0`}>
          {icon}
        </div>
        <div className="flex-1 min-w-0">
          <h3 className="text-sm font-semibold text-gray-900 mb-0.5">{name}</h3>
          <p className="text-gray-600 text-[11px] mb-1.5 line-clamp-1">{description}</p>
          <div className="flex items-center gap-1">
            <span className="text-[10px] text-gray-400">컨텐츠</span>
            <span className="text-xs font-medium text-gray-700">{contentCount}개</span>
          </div>
        </div>
      </div>
    </div>
  )
}

export function DashboardMockup() {
  const [sidebarTab, setSidebarTab] = useState<"video" | "chat">("video")
  const [layoutMode, setLayoutMode] = useState<"narrow" | "grid" | "wide">("narrow")
  const [isLayoutDropdownOpen, setIsLayoutDropdownOpen] = useState(false)
  const [copiedPassword, setCopiedPassword] = useState(false)

  // 레이아웃 설정
  const layoutConfigs = {
    narrow: { width: "w-80", label: "좁은 사이드바", icon: LayoutList },
    grid: { width: "w-[480px]", label: "그리드 레이아웃", icon: LayoutGrid },
    wide: { width: "w-[480px]", label: "넓은 사이드바", icon: Maximize2 },
  }

  // 목업 데이터
  const roomInfo = {
    title: "쉐도잉 연습방",
    isLocked: true,
    password: "1234",
  }

  const mockParticipants = [
    { id: 1, name: "Jane", isReady: true, isOwner: true, avatar: "/src/assets/images/avatars/user1.jpg" },
    { id: 2, name: "박민수", isReady: true, isOwner: false, avatar: "/src/assets/images/avatars/user2.jpg" },
    { id: 3, name: "이영희", isReady: false, isOwner: false, avatar: "/src/assets/images/avatars/user3.jpg" },
  ]

  const mockSubtitles = [
    { role: "Alice", roleId: 1, text: "Hello, how are you today?", isMyRole: false, color: "#3B82F6" },
    { role: "Bob", roleId: 2, text: "I'm fine, thank you!", isMyRole: true, color: "#10B981" },
  ]

  const handleCopyPassword = () => {
    navigator.clipboard.writeText(roomInfo.password)
    setCopiedPassword(true)
    setTimeout(() => setCopiedPassword(false), 2000)
  }

  const handleLayoutChange = (mode: "narrow" | "grid" | "wide") => {
    setLayoutMode(mode)
    setIsLayoutDropdownOpen(false)
  }

  const containerVariants = {
    hidden: {},
    visible: {
      transition: {
        staggerChildren: 0.3,
        delayChildren: 0.5,
      },
    },
  }

  const panelVariants = {
    hidden: {
      opacity: 0,
      x: 100,
      y: -80,
    },
    visible: {
      opacity: 1,
      x: 0,
      y: 0,
      transition: {
        duration: 1.2,
        ease: [0.22, 1, 0.36, 1] as const,
      },
    },
  }

  return (
    <motion.div
      className="w-full h-full bg-white flex overflow-hidden"
      variants={containerVariants}
      initial="hidden"
      animate="visible"
    >
      {/* Sidebar */}
      <motion.div
        className="w-55 h-full bg-white border-r border-gray-200 flex flex-col py-6 px-4 shrink-0"
        variants={panelVariants}
      >
        {/* Logo */}
        <div className="pb-8">
          <div className="h-4 flex items-center justify-start">
            <img src="/src/assets/images/common/logo-dark-2.svg" alt="MEARI" className="h-full" />
          </div>
        </div>

        {/* Menu Items */}
        <div className="flex flex-col gap-2">
          <NavItem icon={Inbox} label="쉐도잉" active />
          <NavItem icon={FolderKanban} label="코픽" />
          <NavItem icon={FileText} label="일일학습" />
        </div>
      </motion.div>

      {/* Theme Selection List */}
      <motion.div
        className="w-[320px] h-full bg-gray-50/50 border-r border-gray-200 flex flex-col shrink-0"
        variants={panelVariants}
      >
        <div className="px-4 py-3 border-b border-gray-200">
          <h3 className="text-gray-900 font-semibold text-sm">테마 선택</h3>
          <p className="text-gray-500 text-xs mt-1">학습할 테마를 선택하세요</p>
        </div>

        <div className="flex-1 overflow-auto p-3 space-y-3">
          <ThemeCard
            icon="💬"
            name="일상회화"
            description="일상생활 회화 표현"
            contentCount={24}
            color="bg-blue-500"
          />
          <ThemeCard
            icon="💼"
            name="비즈니스"
            description="비즈니스 전문 표현"
            contentCount={18}
            color="bg-purple-500"
          />
          <ThemeCard
            icon="📰"
            name="뉴스"
            description="시사 영어 학습"
            contentCount={32}
            color="bg-green-500"
          />
          <ThemeCard
            icon="✈️"
            name="여행"
            description="여행 회화 표현"
            contentCount={20}
            color="bg-orange-500"
          />
        </div>
      </motion.div>

      {/* Shadowing Room Main Area */}
      <motion.div className="flex-1 h-full flex flex-col" variants={panelVariants}>
        {/* 헤더 */}
        <div className="w-full border-b border-gray-200 px-6 py-6 bg-white">
          <div className="flex items-center gap-3">
            {/* 잠금 아이콘 */}
            <div className={`p-2 rounded-lg ${roomInfo.isLocked ? "bg-yellow-50" : "bg-green-50"}`}>
              {roomInfo.isLocked ? (
                <Lock size={20} className="text-yellow-600" />
              ) : (
                <Unlock size={20} className="text-green-600" />
              )}
            </div>

            {/* 방 제목 */}
            <h1 className="text-lg font-semibold text-gray-900 flex-1">{roomInfo.title}</h1>

            {/* 비밀번호 복사 버튼 */}
            {roomInfo.isLocked && (
              <button
                onClick={handleCopyPassword}
                className="flex items-center gap-2 px-3 py-2 bg-gray-100 hover:bg-gray-200 rounded-lg transition-colors"
              >
                {copiedPassword ? (
                  <>
                    <Check size={16} className="text-blue-600" />
                    <span className="text-sm text-blue-600">복사됨</span>
                  </>
                ) : (
                  <>
                    <Copy size={16} className="text-gray-600" />
                    <span className="text-sm text-gray-700">비밀번호 복사</span>
                  </>
                )}
              </button>
            )}
          </div>
        </div>

        {/* 메인 비디오 영역 */}
        <div className="flex-1 p-4 bg-white">
          <div className="relative h-full w-full rounded-lg bg-black flex items-center justify-center overflow-hidden">
            {/* 목업 비디오 배경 */}
            <img
              src="/src/assets/images/video_mock.jpg"
              alt="영상"
              className="absolute inset-0 w-full h-full object-cover"
            />

            {/* 검은색 Gradient Overlay */}
            <div className="absolute inset-0 bg-linear-to-b from-black/70 via-black/30 to-transparent pointer-events-none" />

            {/* 영상 제목 */}
            <div className="absolute left-8 top-[40%] -translate-y-1/2 z-10">
              <h2 className="text-white text-2xl font-bold drop-shadow-lg">영화관 예매하기</h2>
            </div>

            {/* 자막 표시 (하단) */}
            <div className="absolute bottom-8 left-1/2 transform -translate-x-1/2 w-full max-w-5xl px-4 z-10">
              <div className="space-y-2">
                {mockSubtitles.map((subtitle) => (
                  <div
                    key={subtitle.roleId}
                    className={`flex items-center gap-3 px-4 py-3 rounded-lg backdrop-blur-md transition-all ${
                      subtitle.isMyRole ? "bg-blue-600/80 border-2 border-white/50" : "bg-black/60"
                    }`}
                  >
                    {/* 역할 이름 */}
                    <span
                      className="px-3 py-1 rounded-full text-sm font-bold text-white shrink-0"
                      style={{ backgroundColor: subtitle.color }}
                    >
                      {subtitle.role}
                    </span>
                    {/* 대사 */}
                    <p className={`text-lg font-medium ${subtitle.isMyRole ? "text-white" : "text-gray-100"}`}>
                      {subtitle.text}
                    </p>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </motion.div>

      {/* 오른쪽 사이드바 (동적 너비) */}
      <div className={`${layoutConfigs[layoutMode].width} flex flex-col border-l border-gray-200 bg-white transition-all duration-300`}>
        {/* 탭 버튼 */}
        <div className="flex gap-2 p-3 py-4 bg-white border-b border-gray-200">
          <div className="flex gap-2 flex-1">
            <button
              onClick={() => setSidebarTab("video")}
              className={`flex-1 flex items-center cursor-pointer justify-center gap-2 px-3 py-2 text-sm font-medium rounded-lg transition-all ${
                sidebarTab === "video"
                  ? "bg-blue-600 text-white shadow-md"
                  : "bg-white text-gray-600 hover:text-gray-900 hover:bg-gray-100 border border-gray-200"
              }`}
            >
              <Users size={18} />
              <span>참여자</span>
            </button>
            <button
              onClick={() => setSidebarTab("chat")}
              className={`flex-1 flex items-center cursor-pointer justify-center gap-2 px-3 py-2 text-sm font-medium rounded-lg transition-all ${
                sidebarTab === "chat"
                  ? "bg-blue-600 text-white shadow-md"
                  : "bg-white text-gray-600 hover:text-gray-900 hover:bg-gray-100 border border-gray-200"
              }`}
            >
              <MessageCircle size={18} />
              <span>채팅</span>
            </button>
          </div>

          {/* 레이아웃 드롭다운 */}
          <div className="relative">
            <button
              onClick={() => setIsLayoutDropdownOpen(!isLayoutDropdownOpen)}
              className="flex items-center justify-center gap-1 px-3 py-2 text-sm font-medium rounded-lg transition-all bg-white text-gray-600 hover:text-gray-900 hover:bg-gray-100 border border-gray-200"
              title="레이아웃 변경"
            >
              {(() => {
                const Icon = layoutConfigs[layoutMode].icon
                return <Icon size={18} />
              })()}
            </button>

            {/* 드롭다운 메뉴 */}
            {isLayoutDropdownOpen && (
              <div className="absolute top-full right-0 mt-2 bg-white border border-gray-200 rounded-lg shadow-lg overflow-hidden z-10 min-w-48">
                {(
                  Object.entries(layoutConfigs) as [
                    "narrow" | "grid" | "wide",
                    (typeof layoutConfigs)[keyof typeof layoutConfigs],
                  ][]
                ).map(([mode, config]) => {
                  const Icon = config.icon
                  return (
                    <button
                      key={mode}
                      onClick={() => handleLayoutChange(mode)}
                      className={`flex items-center gap-3 px-4 py-3 w-full hover:bg-gray-50 transition-colors text-left ${
                        layoutMode === mode ? "bg-blue-50 text-blue-600" : "text-gray-700"
                      }`}
                    >
                      <Icon size={18} />
                      <span className="text-sm">{config.label}</span>
                    </button>
                  )
                })}
              </div>
            )}
          </div>
        </div>

        {/* 탭 콘텐츠 */}
        <div className="flex-1 overflow-hidden bg-white">
          {sidebarTab === "video" && (
            <div
              className={`h-full overflow-y-auto p-3 ${layoutMode === "grid" ? "grid grid-cols-2 gap-3 auto-rows-min" : "space-y-3"}`}
            >
              {/* WebRTC 참여자 목업 */}
              {mockParticipants.map((participant) => (
                <div
                  key={participant.id}
                  className="relative bg-gray-200 rounded-lg overflow-hidden aspect-video"
                >
                  {/* 목업 웹캠 화면 - 이미지가 전체를 채움 */}
                  <img
                    src={participant.avatar}
                    alt={participant.name}
                    className="w-full h-full object-cover"
                  />

                  {/* 이름 태그 */}
                  <div className="absolute bottom-2 left-2 bg-black/60 px-3 py-1.5 rounded-lg">
                    <span className="text-white text-sm font-medium">
                      {participant.isOwner && "[방장] "}
                      {participant.name}
                    </span>
                  </div>

                  {/* 준비 완료 표시 */}
                  {participant.isReady && (
                    <div className="absolute top-2 right-2 bg-green-500 text-white px-2 py-1 rounded-md text-xs font-semibold flex items-center gap-1">
                      <Check size={14} />
                      준비완료
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </motion.div>
  )
}

function NavItem({
  icon: Icon,
  label,
  badge,
  active,
}: {
  icon: React.ElementType
  label: string
  badge?: number
  active?: boolean
  hasSubmenu?: boolean
  color?: string
}) {
  return (
    <button
      type="button"
      className={`flex items-center gap-3 px-3 py-2 rounded-lg transition-all ${
        active ? "bg-blue-600 text-white" : "text-gray-600 hover:text-gray-900 hover:bg-gray-100"
      }`}
    >
      <Icon className="w-4 h-4" />
      <span className="text-sm font-medium">{label}</span>
      {badge && (
        <span className="bg-indigo-500/80 text-white text-[10px] min-w-[18px] h-[18px] flex items-center justify-center rounded-full font-medium px-1 ml-auto">
          {badge}
        </span>
      )}
    </button>
  )
}
