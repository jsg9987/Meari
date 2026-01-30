import { useEffect, useRef, useState } from "react";
import { MicOff, VideoOff, CheckCircle2 } from "lucide-react";
import type { StreamManager } from "openvidu-browser";

interface VideoTileProps {
  streamManager: StreamManager;
  muted?: boolean;
  label?: string;
  isSpeaker?: boolean;
  isReady?: boolean;
  className?: string;
  videoClassName?: string;
}

export default function VideoTile({ streamManager, muted, label, isSpeaker, isReady, className, videoClassName }: VideoTileProps) {
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const [isAudioActive, setIsAudioActive] = useState(true);
  const [isVideoActive, setIsVideoActive] = useState(true);

  useEffect(() => {
    if (!videoRef.current) return;
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
    </div>
  );
}
