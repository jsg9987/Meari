import { useState } from "react";
import { Mic, MicOff, Video, VideoOff, Volume2, LogOut, ChevronUp } from "lucide-react";

type VideoControlsProps = {
  isAudioEnabled: boolean;
  isVideoEnabled: boolean;
  onToggleAudio: () => void;
  onToggleVideo: () => void;
  onLeave: () => void;
  volume?: number;
  onVolumeChange?: (volume: number) => void;
  selectedNationality?: "KR" | "VN";
  onNationalityChange?: (nationality: "KR" | "VN") => void;
  isRoomConnected?: boolean;
};

export default function VideoControls({
  isAudioEnabled,
  isVideoEnabled,
  onToggleAudio,
  onToggleVideo,
  onLeave,
  volume = 100,
  onVolumeChange,
  selectedNationality = "KR",
  onNationalityChange,
  isRoomConnected = true,
}: VideoControlsProps) {
  const [isNationalityOpen, setIsNationalityOpen] = useState(false);

  const handleVolumeChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const newVolume = Number(e.target.value);
    onVolumeChange?.(newVolume);
  };

  const handleNationalitySelect = (nationality: "KR" | "VN") => {
    onNationalityChange?.(nationality);
    setIsNationalityOpen(false);
  };

  const getFlagEmoji = (nationality: "KR" | "VN") => {
    return nationality === "KR" ? "🇰🇷" : "🇻🇳";
  };

  return (
    <div className="flex justify-between items-center gap-3 p-4">
      {/* Volume Control */}
      <div className="flex items-center gap-3 px-4 py-3 min-w-50">
        <Volume2 size={24} className="text-gray-500 shrink-0" />
        <input
          type="range"
          min="0"
          max="100"
          value={volume}
          onChange={handleVolumeChange}
          className="volume-slider flex-1"
          style={{ '--volume-percent': `${volume}%` } as React.CSSProperties}
        />
      </div>

      {/* Control Buttons */}
      <div className="flex gap-4">
        <button
          onClick={onToggleAudio}
          disabled={!isRoomConnected}
          className={`flex items-center gap-2 p-3 rounded-lg border border-gray-300 transition-colors ${
            !isRoomConnected
              ? "bg-gray-100 text-gray-400 cursor-not-allowed"
              : isAudioEnabled
              ? "bg-gray-100 hover:bg-gray-200 text-gray-800"
              : "bg-red-50 hover:bg-red-100 text-red-600 border-red-300"
          }`}
          title={isAudioEnabled ? "음소거" : "음소거 해제"}
        >
          {isAudioEnabled ? <Mic size={24} /> : <MicOff size={20} />}
          <ChevronUp size={16} className="text-gray-400"/>
        </button>

        <button
          onClick={onToggleVideo}
          disabled={!isRoomConnected}
          className={`flex items-center gap-2 p-3 border border-gray-300 rounded-lg transition-colors ${
            !isRoomConnected
              ? "bg-gray-100 text-gray-400 cursor-not-allowed"
              : isVideoEnabled
              ? "bg-gray-100 hover:bg-gray-200 text-gray-700"
              : "bg-red-50 hover:bg-red-100 text-red-600 border-red-300"
          }`}
          title={isVideoEnabled ? "비디오 끄기" : "비디오 켜기"}
        >
          {isVideoEnabled ? <Video size={24} /> : <VideoOff size={20} />}
          <ChevronUp size={16} className="text-gray-400"/>
        </button>

        {/* Nationality Selector */}
        <div className="relative">
          <button
            onClick={() => setIsNationalityOpen(!isNationalityOpen)}
            disabled={!isRoomConnected}
            className={`flex items-center gap-2 p-3 rounded-lg border border-gray-300 transition-colors text-2xl ${
              !isRoomConnected
                ? "bg-gray-100 cursor-not-allowed opacity-50"
                : "bg-gray-100 hover:bg-gray-200"
            }`}
            title="국적 선택"
          >
            {getFlagEmoji(selectedNationality)}
            <ChevronUp size={16} className="text-gray-400"/>
          </button>

          {/* Dropdown (opens upward) */}
          {isNationalityOpen && (
            <div className="absolute bottom-full mb-2 left-0 bg-white border border-gray-300 rounded-lg shadow-lg overflow-hidden z-10">
              <button
                onClick={() => handleNationalitySelect("KR")}
                className="flex items-center gap-2 px-4 py-2 w-full hover:bg-gray-100 transition-colors text-left"
              >
                <span className="text-2xl">🇰🇷</span>
                <span className="text-sm">한국</span>
              </button>
              <button
                onClick={() => handleNationalitySelect("VN")}
                className="flex items-center gap-2 px-4 py-2 w-full hover:bg-gray-100 transition-colors text-left"
              >
                <span className="text-2xl">🇻🇳</span>
                <span className="text-sm">베트남</span>
              </button>
            </div>
          )}
        </div>
      </div>

      {/* Leave Button */}
      <button
        onClick={onLeave}
        className="flex items-center cursor-pointer gap-2 px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors"
      >
        <LogOut size={20} />
        <span>나가기</span>
      </button>
    </div>
  );
}
