import { useEffect, useRef } from "react";
import { MicOff } from "lucide-react";
import type { StreamManager } from "openvidu-browser";

interface VideoTileProps {
  streamManager: StreamManager;
  muted?: boolean;
  label?: string;
  isSpeaker?: boolean;
  className?: string;
  videoClassName?: string;
}

export default function VideoTile({ streamManager, muted, label, isSpeaker, className, videoClassName }: VideoTileProps) {
  const videoRef = useRef<HTMLVideoElement | null>(null);

  useEffect(() => {
    if (!videoRef.current) return;
    streamManager.addVideoElement(videoRef.current);
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
    </div>
  );
}
