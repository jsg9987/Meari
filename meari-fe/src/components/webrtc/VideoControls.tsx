import { useState, useEffect, useRef } from "react";
import { Mic, MicOff, Video, VideoOff, Volume2, VolumeX, LogOut, ChevronUp, Subtitles } from "lucide-react";
import "../../styles/VolumeSlider.css"
import koreanFlag from "../../assets/images/flag/Flag_of_South_Korea.svg";
import vietnamFlag from "../../assets/images/flag/Flag_of_Vietnam.svg.webp";

type MediaDeviceInfo = {
  deviceId: string;
  label: string;
};

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
  selectedAudioDevice?: string;
  selectedVideoDevice?: string;
  onAudioDeviceChange?: (deviceId: string) => void;
  onVideoDeviceChange?: (deviceId: string) => void;
  isSubtitleEnabled?: boolean;
  onToggleSubtitle?: () => void;
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
  selectedAudioDevice,
  selectedVideoDevice,
  onAudioDeviceChange,
  onVideoDeviceChange,
  isSubtitleEnabled = false,
  onToggleSubtitle,
}: VideoControlsProps) {
  const [isNationalityOpen, setIsNationalityOpen] = useState(false);
  const [isAudioDeviceOpen, setIsAudioDeviceOpen] = useState(false);
  const [isVideoDeviceOpen, setIsVideoDeviceOpen] = useState(false);
  const [previousVolume, setPreviousVolume] = useState(100);
  const [audioDevices, setAudioDevices] = useState<MediaDeviceInfo[]>([]);
  const [videoDevices, setVideoDevices] = useState<MediaDeviceInfo[]>([]);

  const audioDeviceRef = useRef<HTMLDivElement>(null);
  const videoDeviceRef = useRef<HTMLDivElement>(null);
  const nationalityRef = useRef<HTMLDivElement>(null);

  // 장치 목록 가져오기
  useEffect(() => {
    const getDevices = async () => {
      try {
        const devices = await navigator.mediaDevices.enumerateDevices();

        const audioInputs = devices
          .filter(device => device.kind === 'audioinput')
          .map(device => ({
            deviceId: device.deviceId,
            label: device.label || `마이크 ${device.deviceId.slice(0, 5)}`,
          }));

        const videoInputs = devices
          .filter(device => device.kind === 'videoinput')
          .map(device => ({
            deviceId: device.deviceId,
            label: device.label || `카메라 ${device.deviceId.slice(0, 5)}`,
          }));

        setAudioDevices(audioInputs);
        setVideoDevices(videoInputs);
      } catch (error) {
        console.error('Failed to enumerate devices:', error);
      }
    };

    getDevices();

    // 장치 변경 감지
    navigator.mediaDevices.addEventListener('devicechange', getDevices);
    return () => {
      navigator.mediaDevices.removeEventListener('devicechange', getDevices);
    };
  }, []);

  // 바깥 클릭 감지
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (audioDeviceRef.current && !audioDeviceRef.current.contains(event.target as Node)) {
        setIsAudioDeviceOpen(false);
      }
      if (videoDeviceRef.current && !videoDeviceRef.current.contains(event.target as Node)) {
        setIsVideoDeviceOpen(false);
      }
      if (nationalityRef.current && !nationalityRef.current.contains(event.target as Node)) {
        setIsNationalityOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, []);

  const handleVolumeChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const newVolume = Number(e.target.value);
    if (newVolume > 0) {
      setPreviousVolume(newVolume);
    }
    onVolumeChange?.(newVolume);
  };

  const handleVolumeToggle = () => {
    if (volume > 0) {
      setPreviousVolume(volume);
      onVolumeChange?.(0);
    } else {
      onVolumeChange?.(previousVolume > 0 ? previousVolume : 100);
    }
  };

  const handleNationalitySelect = (nationality: "KR" | "VN") => {
    onNationalityChange?.(nationality);
    setIsNationalityOpen(false);
  };

  const handleAudioDeviceSelect = (deviceId: string) => {
    onAudioDeviceChange?.(deviceId);
    setIsAudioDeviceOpen(false);
  };

  const handleVideoDeviceSelect = (deviceId: string) => {
    onVideoDeviceChange?.(deviceId);
    setIsVideoDeviceOpen(false);
  };

  const getFlagImage = (nationality: "KR" | "VN") => {
    return nationality === "KR" ? koreanFlag : vietnamFlag;
  };

  const getNationalityLabel = (nationality: "KR" | "VN") => {
    return nationality === "KR" ? "한국" : "베트남";
  };

  return (
    <div className="flex justify-between items-center gap-3 p-4">
      {/* Volume Control */}
      <div className="flex items-center gap-2 px-3 py-2 rounded-lg">
        <button
          onClick={handleVolumeToggle}
          className="text-gray-500 hover:text-gray-700 transition-colors shrink-0"
          title={volume > 0 ? "음소거" : "음소거 해제"}
        >
          {volume > 0 ? <Volume2 size={20} /> : <VolumeX size={20} />}
        </button>
        <input
          type="range"
          min="0"
          max="100"
          value={volume}
          onChange={handleVolumeChange}
          className="volume-slider w-16"
          style={{ '--volume-percent': `${volume}%` } as React.CSSProperties}
        />
      </div>

      {/* Control Buttons */}
      <div className="flex gap-4">
        {/* Audio Control with Device Selector */}
        <div className="relative" ref={audioDeviceRef}>
          <div className="flex">
            <button
              onClick={onToggleAudio}
              disabled={!isRoomConnected}
              className={`flex items-center gap-2 p-3 rounded-l-lg border border-gray-300 transition-colors ${
                !isRoomConnected
                  ? "bg-gray-100 text-gray-400 cursor-not-allowed"
                  : isAudioEnabled
                  ? "bg-gray-100 hover:bg-gray-200 text-gray-800"
                  : "bg-red-50 hover:bg-red-100 text-red-600 border-red-300"
              }`}
              title={isAudioEnabled ? "음소거" : "음소거 해제"}
            >
              {isAudioEnabled ? <Mic size={24} /> : <MicOff size={24} />}
            </button>
            <button
              onClick={() => setIsAudioDeviceOpen(!isAudioDeviceOpen)}
              disabled={!isRoomConnected}
              className={`px-2 rounded-r-lg border-l-0 border border-gray-300 transition-colors ${
                !isRoomConnected
                  ? "bg-gray-100 text-gray-400 cursor-not-allowed"
                  : isAudioEnabled
                  ? "bg-gray-100 hover:bg-gray-200 text-gray-800"
                  : "bg-red-50 hover:bg-red-100 text-red-600 border-red-300"
              }`}
              title="마이크 선택"
            >
              <ChevronUp size={16} className="text-gray-400"/>
            </button>
          </div>

          {/* Audio Device Dropdown */}
          {isAudioDeviceOpen && audioDevices.length > 0 && (
            <div className="absolute bottom-full mb-2 left-0 bg-white border border-gray-300 rounded-lg shadow-lg overflow-hidden z-10 min-w-64">
              {audioDevices.map((device) => (
                <button
                  key={device.deviceId}
                  onClick={() => handleAudioDeviceSelect(device.deviceId)}
                  className={`flex items-center gap-2 px-4 py-2 w-full hover:bg-gray-100 transition-colors text-left ${
                    selectedAudioDevice === device.deviceId ? "bg-blue-50 text-blue-600" : ""
                  }`}
                >
                  <Mic size={16} />
                  <span className="text-sm truncate">{device.label}</span>
                </button>
              ))}
            </div>
          )}
        </div>

        {/* Video Control with Device Selector */}
        <div className="relative" ref={videoDeviceRef}>
          <div className="flex">
            <button
              onClick={onToggleVideo}
              disabled={!isRoomConnected}
              className={`flex items-center gap-2 p-3 rounded-l-lg border border-gray-300 transition-colors ${
                !isRoomConnected
                  ? "bg-gray-100 text-gray-400 cursor-not-allowed"
                  : isVideoEnabled
                  ? "bg-gray-100 hover:bg-gray-200 text-gray-700"
                  : "bg-red-50 hover:bg-red-100 text-red-600 border-red-300"
              }`}
              title={isVideoEnabled ? "비디오 끄기" : "비디오 켜기"}
            >
              {isVideoEnabled ? <Video size={24} /> : <VideoOff size={24} />}
            </button>
            <button
              onClick={() => setIsVideoDeviceOpen(!isVideoDeviceOpen)}
              disabled={!isRoomConnected}
              className={`px-2 rounded-r-lg border-l-0 border border-gray-300 transition-colors ${
                !isRoomConnected
                  ? "bg-gray-100 text-gray-400 cursor-not-allowed"
                  : isVideoEnabled
                  ? "bg-gray-100 hover:bg-gray-200 text-gray-700"
                  : "bg-red-50 hover:bg-red-100 text-red-600 border-red-300"
              }`}
              title="카메라 선택"
            >
              <ChevronUp size={16} className="text-gray-400"/>
            </button>
          </div>

          {/* Video Device Dropdown */}
          {isVideoDeviceOpen && videoDevices.length > 0 && (
            <div className="absolute bottom-full mb-2 left-0 bg-white border border-gray-300 rounded-lg shadow-lg overflow-hidden z-10 min-w-64">
              {videoDevices.map((device) => (
                <button
                  key={device.deviceId}
                  onClick={() => handleVideoDeviceSelect(device.deviceId)}
                  className={`flex items-center gap-2 px-4 py-2 w-full hover:bg-gray-100 transition-colors text-left ${
                    selectedVideoDevice === device.deviceId ? "bg-blue-50 text-blue-600" : ""
                  }`}
                >
                  <Video size={16} />
                  <span className="text-sm truncate">{device.label}</span>
                </button>
              ))}
            </div>
          )}
        </div>

        {/* Subtitle Toggle */}
        <button
          onClick={onToggleSubtitle}
          disabled={!isRoomConnected}
          className={`flex items-center gap-2 p-3 rounded-lg border border-gray-300 transition-colors ${
            !isRoomConnected
              ? "bg-gray-100 text-gray-400 cursor-not-allowed"
              : isSubtitleEnabled
              ? "bg-gray-100 hover:bg-gray-200 text-gray-800"
              : "bg-red-50 hover:bg-red-100 text-red-600 border-red-300"
          }`}
          title={isSubtitleEnabled ? "자막 끄기" : "자막 켜기"}
        >
          <Subtitles size={24} />
        </button>

        {/* Nationality Selector */}
        <div className="relative" ref={nationalityRef}>
          <button
            onClick={() => setIsNationalityOpen(!isNationalityOpen)}
            disabled={!isRoomConnected}
            className={`flex items-center gap-2 p-3 rounded-lg border border-gray-300 transition-colors ${
              !isRoomConnected
                ? "bg-gray-100 cursor-not-allowed opacity-50"
                : "bg-gray-100 hover:bg-gray-200"
            }`}
            title="국적 선택"
          >
            <img
              src={getFlagImage(selectedNationality)}
              alt={getNationalityLabel(selectedNationality)}
              className="w-6 h-6 rounded-full object-cover"
            />
            <ChevronUp size={16} className="text-gray-400"/>
          </button>

          {/* Dropdown (opens upward) */}
          {isNationalityOpen && (
            <div className="absolute bottom-full mb-2 left-0 bg-white border border-gray-300 rounded-lg shadow-lg overflow-hidden z-10 min-w-32">
              <button
                onClick={() => handleNationalitySelect("KR")}
                className={`flex items-center gap-3 px-4 py-2 w-full hover:bg-gray-100 transition-colors text-left ${
                  selectedNationality === "KR" ? "bg-blue-50 text-blue-600" : ""
                }`}
              >
                <img
                  src={koreanFlag}
                  alt="한국"
                  className="w-6 h-6 rounded-full object-cover shrink-0"
                />
                <span className="text-sm whitespace-nowrap">한국</span>
              </button>
              <button
                onClick={() => handleNationalitySelect("VN")}
                className={`flex items-center gap-3 px-4 py-2 w-full hover:bg-gray-100 transition-colors text-left ${
                  selectedNationality === "VN" ? "bg-blue-50 text-blue-600" : ""
                }`}
              >
                <img
                  src={vietnamFlag}
                  alt="베트남"
                  className="w-6 h-6 rounded-full object-cover shrink-0"
                />
                <span className="text-sm whitespace-nowrap">베트남</span>
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
