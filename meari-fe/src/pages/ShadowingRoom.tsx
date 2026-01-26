import { useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { Users, MessageCircle, Lock, Unlock, Copy, Check } from "lucide-react";
// import Header from "../components/common/Header";
import VideoTile from "../components/webrtc/VideoTile";
import VideoControls from "../components/webrtc/VideoControls";
import ChatPanel from "../components/webrtc/ChatPanel";
// import { useVideoRoom } from "../hooks/useVideoRoom";
import type { VideoTileData, ConnectionStatus } from "../hooks/useVideoRoom";
import type { Publisher, Subscriber } from "openvidu-browser";

type SidebarTab = "video" | "chat";

// TODO: 헤더 변경, 비디오 타일 변경
export default function ShadowingRoom() {
  const { roomId } = useParams<{ roomId: string }>();
  const navigate = useNavigate();
  const [sidebarTab, setSidebarTab] = useState<SidebarTab>("video");
  const [copiedPassword, setCopiedPassword] = useState(false);

  const nickname = "User";

  // Mock 방 정보 (실제로는 API에서 가져와야 함)
  const roomInfo = {
    isLocked: true,
    title: "English Conversation Practice Room",
    password: "abc123"
  };

  const mockProfileImages: Record<string, string> = {
    me: "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=800&q=80",
    user1: "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&w=800&q=80",
    user2: "https://images.unsplash.com/photo-1521572267360-ee0c2909d518?auto=format&fit=crop&w=800&q=80",
    user3: "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=800&q=80",
  };


  // ============================================================================
  // MOCK DATA - UI 개발용 (실제 배포시 주석 제거하고 아래 useVideoRoom 주석 해제)
  // ============================================================================
  const [status, setStatus] = useState<ConnectionStatus>("connected");
  const [error, setError] = useState<string | null>(null);
  const [isAudioEnabled, setIsAudioEnabled] = useState(true);
  const [isVideoEnabled, setIsVideoEnabled] = useState(true);
  const [volume, setVolume] = useState(100);
  const [selectedAudioDevice, setSelectedAudioDevice] = useState<string>();
  const [selectedVideoDevice, setSelectedVideoDevice] = useState<string>();
  const [selectedNationality, setSelectedNationality] = useState<"KR" | "VN">("KR");
  const [isSubtitleEnabled, setIsSubtitleEnabled] = useState(false);

  // Mock StreamManager 생성
  const createMockStreamManager = (id: string): Publisher | Subscriber => {
    return {
      addVideoElement: (videoElement: HTMLVideoElement) => {
        // Mock: 색상 배경으로 비디오 대체
        const imageUrl = mockProfileImages[id];
        if (imageUrl) {
          videoElement.style.backgroundImage = `url("${imageUrl}")`;
          videoElement.style.backgroundSize = "cover";
          videoElement.style.backgroundPosition = "center";
          videoElement.style.backgroundRepeat = "no-repeat";
        } else {
          videoElement.style.backgroundColor = "#111827";
        }
      },
      stream: {
        streamId: id,
        connection: {
          data: JSON.stringify({ clientData: id === "me" ? nickname : `참여자${id}` })
        }
      }
    } as Publisher | Subscriber;
  };

  // Mock tiles 데이터 (4명의 참가자)
  const tiles: VideoTileData[] = [
    {
      id: "me",
      streamManager: createMockStreamManager("me"),
      muted: true,
      label: `${nickname} (Me)`
    },
    {
      id: "user1",
      streamManager: createMockStreamManager("user1"),
      label: "User 1",
      isSpeaker: true,
    },
    {
      id: "user2",
      streamManager: createMockStreamManager("user2"),
      label: "User 2",
    },
    {
      id: "user3",
      streamManager: createMockStreamManager("user3"),
      label: "User 3",
    },
  ];

  const join = () => {
    console.log("Mock join");
    setStatus("connected");
  };
  const leave = () => {
    console.log("Mock leave");
    setStatus("idle");
  };
  const toggleAudio = () => setIsAudioEnabled(!isAudioEnabled);
  const toggleVideo = () => setIsVideoEnabled(!isVideoEnabled);
  const toggleSubtitle = () => setIsSubtitleEnabled(!isSubtitleEnabled);

  const handleAudioDeviceChange = (deviceId: string) => {
    setSelectedAudioDevice(deviceId);
    // TODO: 실제 구현시 미디어 스트림 변경 로직 추가
    console.log('Audio device changed to:', deviceId);
  };

  const handleVideoDeviceChange = (deviceId: string) => {
    setSelectedVideoDevice(deviceId);
    // TODO: 실제 구현시 미디어 스트림 변경 로직 추가
    console.log('Video device changed to:', deviceId);
  };
  // ============================================================================
  // MOCK DATA 끝
  // ============================================================================

  // ============================================================================
  // 실제 API 사용시 아래 주석 해제하고 위의 MOCK DATA 섹션 주석 처리
  // ============================================================================
  // const {
  //   status,
  //   error,
  //   tiles,
  //   isAudioEnabled,
  //   isVideoEnabled,
  //   join,
  //   leave,
  //   toggleAudio,
  //   toggleVideo,
  // } = useVideoRoom({ sessionName: roomId ?? "", nickname, autoJoin: !!roomId });
  // ============================================================================

  if (!roomId) {
    return (
      <div className="flex flex-col items-center justify-center min-h-screen gap-4 bg-gray-50">
        <p className="text-red-600 text-lg font-medium">유효하지 않은 방 ID입니다</p>
        <button
          onClick={() => navigate("/")}
          className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
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

  const handleCopyPassword = async () => {
    try {
      await navigator.clipboard.writeText(roomInfo.password);
      setCopiedPassword(true);
      setTimeout(() => setCopiedPassword(false), 2000);
    } catch (err) {
      console.error("Failed to copy password:", err);
    }
  };

  return (
    <div className="flex h-screen bg-gray-50">
      {/* 왼쪽 메인 영역 */}
      <div className="flex flex-1 flex-col">
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
            <h1 className="text-lg font-semibold text-gray-900 flex-1">
              {roomInfo.title}
            </h1>

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
          <div className="relative h-full w-full rounded-lg bg-gray-900 flex items-center justify-center">
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

        <div className="border-t border-gray-200 p-4 bg-white">
          <VideoControls
            isAudioEnabled={isAudioEnabled}
            isVideoEnabled={isVideoEnabled}
            onToggleAudio={toggleAudio}
            onToggleVideo={toggleVideo}
            onLeave={handleLeave}
            volume={volume}
            onVolumeChange={setVolume}
            selectedAudioDevice={selectedAudioDevice}
            selectedVideoDevice={selectedVideoDevice}
            onAudioDeviceChange={handleAudioDeviceChange}
            onVideoDeviceChange={handleVideoDeviceChange}
            selectedNationality={selectedNationality}
            onNationalityChange={setSelectedNationality}
            isSubtitleEnabled={isSubtitleEnabled}
            onToggleSubtitle={toggleSubtitle}
          />
        </div>
      </div>

      {/* 오른쪽 사이드바 (360px) */}
      <div className="w-90 flex flex-col border-l border-gray-200 bg-white">
        {/* 탭 버튼 */}
        <div className="flex gap-2 p-3 py-4 bg-gray-50">
          <button
            onClick={() => setSidebarTab("video")}
            className={`flex-1 flex items-center cursor-pointer justify-center gap-4 px-4 py-2 text-sm font-medium rounded-lg transition-all ${
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
            className={`flex-1 flex items-center cursor-pointer justify-center gap-4 px-4 py-2 text-sm font-medium rounded-lg transition-all ${
              sidebarTab === "chat"
                ? "bg-blue-600 text-white shadow-md"
                : "bg-white text-gray-600 hover:text-gray-900 hover:bg-gray-100 border border-gray-200"
            }`}
          >
            <MessageCircle size={18} />
            <span>채팅</span>
          </button>
        </div>

        {/* 탭 콘텐츠 */}
        <div className="flex-1 overflow-hidden bg-white">
          {sidebarTab === "video" && (
            <div className="h-full overflow-y-auto p-3 space-y-3">
              {status === "connected" && tiles.length > 0 ? (
                tiles.map((t) => (
                  <VideoTile
                    key={t.id}
                    streamManager={t.streamManager}
                    muted={t.muted}
                    label={t.label}
                    isSpeaker={t.isSpeaker}
                  />
                ))
              ) : (
                <p className="text-center text-gray-500 text-sm py-8">
                  {status === "connecting" ? "연결 중..." : "참여자가 없습니다"}
                </p>
              )}
            </div>
          )}
          {sidebarTab === "chat" && (
            <div className="h-full">
              <ChatPanel roomId={roomId} />
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
