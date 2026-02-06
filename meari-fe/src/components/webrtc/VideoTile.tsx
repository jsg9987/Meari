import { useEffect, useRef, useState } from "react";
import { MicOff, VideoOff, CheckCircle2, Volume2, VolumeX } from "lucide-react";
import type { StreamManager } from "openvidu-browser";

interface VideoTileProps {
  streamManager?: StreamManager;
  muted?: boolean;
  label?: string;
  isSpeaker?: boolean;
  isReady?: boolean;
  isSettingUp?: boolean;
  className?: string;
  videoClassName?: string;
}

export default function VideoTile({ streamManager, muted, label, isSpeaker, isReady, isSettingUp, className, videoClassName }: VideoTileProps) {
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const [isAudioActive, setIsAudioActive] = useState(true);
  const [isVideoActive, setIsVideoActive] = useState(true);
  const [volume, setVolume] = useState(100);
  const [showVolumeControl, setShowVolumeControl] = useState(false);

  useEffect(() => {
    if (!streamManager || !videoRef.current) return;
    streamManager.addVideoElement(videoRef.current);

    // 초기 오디오/비디오 상태 설정
    const updateMediaState = () => {
      if (streamManager.stream) {
        setIsAudioActive(streamManager.stream.audioActive);
        setIsVideoActive(streamManager.stream.videoActive);
      }
    };

    updateMediaState();

    // 스트림 상태 변경 감지
    const checkInterval = setInterval(updateMediaState, 500);

    return () => {
      clearInterval(checkInterval);
    };
  }, [streamManager]);

  // 개별 볼륨 적용 (자신의 비디오가 아닌 경우에만)
  useEffect(() => {
    if (videoRef.current && !muted) {
      videoRef.current.volume = volume / 100;
    }
  }, [volume, muted]);

  const handleVolumeChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setVolume(Number(e.target.value));
  };

  const handleVolumeToggle = () => {
    if (volume > 0) {
      setVolume(0);
    } else {
      setVolume(100);
    }
  };

  // 세팅 중인 경우
  if (isSettingUp) {
    return (
      <div
        className={`relative overflow-hidden rounded-xl bg-gray-800 border border-gray-200 ${className || ""}`}
      >
        <div className="w-full aspect-video flex items-center justify-center">
          <div className="flex flex-col items-center gap-3">
            <div className="w-16 h-16 rounded-full bg-gray-700 flex items-center justify-center animate-pulse">
              <div className="w-12 h-12 rounded-full border-4 border-gray-500 border-t-blue-500 animate-spin" />
            </div>
            {label && (
              <div className="text-center">
                <p className="text-sm text-gray-300 font-medium">{label}</p>
                <p className="text-xs text-gray-400 mt-1">세팅 중...</p>
              </div>
            )}
          </div>
        </div>
      </div>
    );
  }

  // streamManager가 없으면 렌더링하지 않음
  if (!streamManager) {
    return null;
  }

  return (
    <div
      className={`relative overflow-hidden rounded-xl bg-gray-100 border border-gray-200 ${
        isSpeaker ? "ring-2 ring-blue-500 ring-offset-2 ring-offset-white" : ""
      } ${className || ""}`}
    >
      <video
        ref={videoRef}
        autoPlay
        playsInline
        muted={muted}
        className={`w-full object-cover ${videoClassName || "aspect-video"}`}
      />

      {/* 비디오 꺼짐 오버레이 */}
      {!isVideoActive && (
        <div className="absolute inset-0 flex items-center justify-center bg-gray-800">
          <div className="flex flex-col items-center gap-2">
            <div className="w-16 h-16 rounded-full bg-gray-700 flex items-center justify-center">
              <VideoOff size={32} className="text-gray-400" />
            </div>
            {label && (
              <span className="text-sm text-gray-300 font-medium">
                {label.replace(' (나)', '')}
              </span>
            )}
          </div>
        </div>
      )}

      {/* 닉네임 라벨 (비디오 켜졌을 때만) */}
      {label && isVideoActive && (
        <span className="absolute bottom-0 left-0 px-2 py-1 text-sm text-gray-900 bg-white/90 rounded-tr-lg font-medium">
          {label}
        </span>
      )}

      {/* 음소거 아이콘 (실제 음소거 상태일 때만) */}
      {!isAudioActive && (
        <div className="absolute bottom-0 right-0 px-3 py-1 mx-auto text-red-600 bg-white/90 rounded-tl-lg">
          <MicOff size={16} className="text-red-600" />
        </div>
      )}

      {/* 준비 완료 배지 */}
      {isReady && (
        <div className="absolute top-2 right-2 px-2 py-1 bg-green-500 text-white rounded-lg flex items-center gap-1 text-xs font-medium">
          <CheckCircle2 size={14} />
          <span>준비완료</span>
        </div>
      )}

      {/* 개별 볼륨 컨트롤 (자신의 타일이 아닌 경우만 표시) */}
      {!muted && (
        <div
          className="absolute top-2 left-2 flex items-center gap-2 bg-black/60 px-2 py-1.5 rounded-lg backdrop-blur-sm"
          onMouseEnter={() => setShowVolumeControl(true)}
          onMouseLeave={() => setShowVolumeControl(false)}
        >
          <button
            onClick={(e) => {
              e.stopPropagation();
              handleVolumeToggle();
            }}
            className="text-white hover:text-blue-400 transition-colors"
            title={volume > 0 ? "음소거" : "음소거 해제"}
          >
            {volume > 0 ? <Volume2 size={16} /> : <VolumeX size={16} />}
          </button>
          {showVolumeControl && (
            <input
              type="range"
              min="0"
              max="100"
              value={volume}
              onChange={handleVolumeChange}
              onClick={(e) => e.stopPropagation()}
              className="w-16 h-1 bg-gray-300 rounded-lg appearance-none cursor-pointer [&::-webkit-slider-thumb]:appearance-none [&::-webkit-slider-thumb]:w-3 [&::-webkit-slider-thumb]:h-3 [&::-webkit-slider-thumb]:rounded-full [&::-webkit-slider-thumb]:bg-white [&::-webkit-slider-thumb]:cursor-pointer [&::-moz-range-thumb]:w-3 [&::-moz-range-thumb]:h-3 [&::-moz-range-thumb]:rounded-full [&::-moz-range-thumb]:bg-white [&::-moz-range-thumb]:border-0 [&::-moz-range-thumb]:cursor-pointer"
            />
          )}
        </div>
      )}
    </div>
  );
}
