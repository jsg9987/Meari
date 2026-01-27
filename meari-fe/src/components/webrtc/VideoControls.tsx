type VideoControlsProps = {
  isAudioEnabled: boolean;
  isVideoEnabled: boolean;
  onToggleAudio: () => void;
  onToggleVideo: () => void;
  onLeave: () => void;
};

export default function VideoControls({
  isAudioEnabled,
  isVideoEnabled,
  onToggleAudio,
  onToggleVideo,
  onLeave,
}: VideoControlsProps) {
  return (
    <div className="flex justify-center gap-3 mt-4">
      <button
        onClick={onToggleAudio}
        className={`px-4 py-2 rounded-lg transition-colors ${
          isAudioEnabled
            ? "bg-gray-200 hover:bg-gray-300 text-gray-800"
            : "bg-red-500 hover:bg-red-600 text-white"
        }`}
      >
        {isAudioEnabled ? "음소거" : "음소거 해제"}
      </button>

      <button
        onClick={onToggleVideo}
        className={`px-4 py-2 rounded-lg transition-colors ${
          isVideoEnabled
            ? "bg-gray-200 hover:bg-gray-300 text-gray-800"
            : "bg-red-500 hover:bg-red-600 text-white"
        }`}
      >
        {isVideoEnabled ? "비디오 끄기" : "비디오 켜기"}
      </button>

      <button
        onClick={onLeave}
        className="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors"
      >
        나가기
      </button>
    </div>
  );
}
