import { useVideoRoom } from "../../hooks/useVideoRoom";
import VideoTile from "./VideoTile";
import VideoControls from "./VideoControls";

interface VideoRoomProps {
  sessionName: string;
  nickname: string;
  autoJoin?: boolean;
}

export default function VideoRoom({ sessionName, nickname, autoJoin = true }: VideoRoomProps) {
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
  } = useVideoRoom({ sessionName, nickname, autoJoin });

  if (status === "idle") {
    return (
      <div className="flex flex-col items-center justify-center min-h-[400px] gap-4">
        <p className="text-gray-400">세션에 참여하려면 버튼을 클릭하세요</p>
        <button
          onClick={join}
          className="px-6 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
        >
          참여하기
        </button>
      </div>
    );
  }

  if (status === "connecting") {
    return (
      <div className="flex flex-col items-center justify-center min-h-[400px] gap-4">
        <div className="w-10 h-10 border-4 border-blue-600 border-t-transparent rounded-full animate-spin" />
        <p className="text-gray-400">연결 중...</p>
      </div>
    );
  }

  if (status === "error") {
    return (
      <div className="flex flex-col items-center justify-center min-h-[400px] gap-4">
        <div className="text-red-500 text-center">
          <p className="text-lg font-medium">연결 실패</p>
          <p className="text-sm mt-1">{error}</p>
        </div>
        <button
          onClick={join}
          className="px-6 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
        >
          다시 시도
        </button>
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-4 p-4">
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
        {tiles.map((t) => (
          <VideoTile
            key={t.id}
            streamManager={t.streamManager}
            muted={t.muted}
            label={t.label}
          />
        ))}
      </div>

      <VideoControls
        isAudioEnabled={isAudioEnabled}
        isVideoEnabled={isVideoEnabled}
        onToggleAudio={toggleAudio}
        onToggleVideo={toggleVideo}
        onLeave={leave}
      />
    </div>
  );
}
