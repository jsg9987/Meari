import { useEffect, useRef } from "react";
import { MicOff, CheckCircle2 } from "lucide-react";
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

  useEffect(() => {
    if (!streamManager || !videoRef.current) return;
    streamManager.addVideoElement(videoRef.current);
  }, [streamManager]);

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
      {label && (
        <span className="absolute bottom-0 left-0 px-2 py-1 text-sm text-gray-900 bg-white/90 rounded-tr-lg font-medium">
          {label}
        </span>
      )}
      {muted && (
        <div className="absolute bottom-0 right-0 px-3 py-1 mx-auto text-red-600 bg-white/90 rounded-tl-lg">
          <MicOff size={16} className="text-red-600" />
        </div>
      )}
      {isReady && (
        <div className="absolute top-2 right-2 px-2 py-1 bg-green-500 text-white rounded-lg flex items-center gap-1 text-xs font-medium">
          <CheckCircle2 size={14} />
          <span>준비완료</span>
        </div>
      )}
    </div>
  );
}
