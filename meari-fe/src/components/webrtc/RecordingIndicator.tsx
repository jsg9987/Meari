interface RecordingIndicatorProps {
  message?: string;
}

export default function RecordingIndicator({ message = "녹음중..." }: RecordingIndicatorProps) {
  return (
    <div className="fixed top-4 left-1/2 -translate-x-1/2 z-50 animate-in fade-in slide-in-from-top-2">
      <div className="bg-black/90 text-white px-6 py-4 rounded-lg shadow-2xl border border-white/10">
        {/* 웨이브폼 애니메이션 */}
        <div className="flex items-center justify-center gap-0.75 h-12 mb-3">
          {Array.from({ length: 30 }).map((_, i) => (
            <div
              key={i}
              className="w-1.5 bg-accent-blue rounded-full"
              style={{
                animation: `waveform ${0.8 + (i % 5) * 0.15}s ease-in-out ${i * 0.05}s infinite`,
                opacity: 0.5 + Math.sin(i * 0.3) * 0.5,
              }}
            />
          ))}
        </div>

        {/* 녹음중 텍스트 */}
        <p className="text-center font-medium text-sm">{message}</p>
      </div>
    </div>
  );
}
