import { useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import Header from "../components/common/Header";
import VideoTile from "../components/webrtc/VideoTile";
import VideoControls from "../components/webrtc/VideoControls";
import ChatPanel from "../components/webrtc/ChatPanel";
import { useVideoRoom } from "../hooks/useVideoRoom";

type SidebarTab = "video" | "chat";

export default function ShadowingRoom() {
  const { roomId } = useParams<{ roomId: string }>();
  const navigate = useNavigate();
  const [sidebarTab, setSidebarTab] = useState<SidebarTab>("video");

  const nickname = "사용자";

  const {
    status,
    error,
    tiles,
    isAudioEnabled,
    isVideoEnabled,
    join,
    leave,
    toggleAudio,
    toggleVideo,
  } = useVideoRoom({ sessionName: roomId ?? "", nickname, autoJoin: !!roomId });

  if (!roomId) {
    return (
      <div className="flex flex-col items-center justify-center min-h-screen gap-4 bg-[var(--color-bg-root)]">
        <p className="text-red-500">유효하지 않은 방 ID입니다</p>
        <button
          onClick={() => navigate("/")}
          className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
        >
          홈으로 돌아가기
        </button>
      </div>
    );
  }

  const handleLeave = () => {
    leave();
    navigate("/");
  };

  return (
    <div className="flex h-screen bg-[var(--color-bg-root)]">
      {/* 왼쪽 메인 영역 */}
      <div className="flex flex-1 flex-col">
        {/* 헤더 */}
        <Header activeTab="shadowing" onTabChange={() => navigate("/")} />

        {/* 메인 비디오 영역 */}
        <div className="flex-1 p-4">
          <div className="relative h-full w-full rounded-2xl bg-black flex items-center justify-center">
            {status === "connecting" && (
              <div className="flex flex-col items-center gap-3">
                <div className="w-10 h-10 border-4 border-blue-600 border-t-transparent rounded-full animate-spin" />
                <p className="text-gray-400">연결 중...</p>
              </div>
            )}
            {status === "error" && (
              <div className="flex flex-col items-center gap-3">
                <p className="text-red-500">{error}</p>
                <button
                  onClick={join}
                  className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
                >
                  다시 시도
                </button>
              </div>
            )}
            {status === "connected" && (
              <p className="text-gray-500 text-sm">쉐도잉 콘텐츠 영역</p>
            )}
            {status === "idle" && (
              <button
                onClick={join}
                className="px-6 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
              >
                참여하기
              </button>
            )}
          </div>
        </div>

        <div className="border-t border-white/10 p-4">
          <VideoControls
            isAudioEnabled={isAudioEnabled}
            isVideoEnabled={isVideoEnabled}
            onToggleAudio={toggleAudio}
            onToggleVideo={toggleVideo}
            onLeave={handleLeave}
          />
        </div>
      </div>

      {/* 오른쪽 사이드바 (360px) */}
      <div className="w-[360px] flex flex-col border-l border-white/10">
        {/* 탭 버튼 */}
        <div className="flex border-b border-white/10">
          <button
            onClick={() => setSidebarTab("video")}
            className={`flex-1 py-3 text-sm font-medium transition-colors ${
              sidebarTab === "video"
                ? "text-white border-b-2 border-blue-500"
                : "text-gray-400 hover:text-white"
            }`}
          >
            참여자
          </button>
          <button
            onClick={() => setSidebarTab("chat")}
            className={`flex-1 py-3 text-sm font-medium transition-colors ${
              sidebarTab === "chat"
                ? "text-white border-b-2 border-blue-500"
                : "text-gray-400 hover:text-white"
            }`}
          >
            채팅
          </button>
        </div>

        {/* 탭 콘텐츠 */}
        <div className="flex-1 overflow-hidden">
          {sidebarTab === "video" ? (
            <div className="h-full overflow-y-auto p-3 space-y-3">
              {status === "connected" && tiles.length > 0 ? (
                tiles.map((t) => (
                  <VideoTile
                    key={t.id}
                    streamManager={t.streamManager}
                    muted={t.muted}
                    label={t.label}
                  />
                ))
              ) : (
                <p className="text-center text-gray-500 text-sm py-8">
                  {status === "connecting" ? "연결 중..." : "참여자가 없습니다"}
                </p>
              )}
            </div>
          ) : (
            <ChatPanel roomId={roomId} />
          )}
        </div>
      </div>
    </div>
  );
}
