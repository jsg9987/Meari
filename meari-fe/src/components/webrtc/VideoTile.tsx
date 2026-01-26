import { useEffect, useRef } from "react";
import type { StreamManager } from "openvidu-browser";

interface VideoTileProps {
  streamManager: StreamManager;
  muted?: boolean;
  label?: string;
}

export default function VideoTile({ streamManager, muted, label }: VideoTileProps) {
  const videoRef = useRef<HTMLVideoElement | null>(null);

  useEffect(() => {
    if (!videoRef.current) return;
    streamManager.addVideoElement(videoRef.current);
  }, [streamManager]);

  return (
    <div className="relative overflow-hidden rounded-xl bg-gray-900">
      <video
        ref={videoRef}
        autoPlay
        playsInline
        muted={muted}
        className="w-full aspect-video object-cover"
      />
      {label && (
        <span className="absolute bottom-2 left-2 px-2 py-1 text-sm text-white bg-black/60 rounded">
          {label}
        </span>
      )}
    </div>
  );
}
