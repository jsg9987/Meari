interface ChatPanelProps {
  roomId: string;
}

export default function ChatPanel({ roomId }: ChatPanelProps) {
  return (
    <div className="flex flex-col h-full">
      <div className="flex-1 overflow-y-auto p-3">
        <p className="text-center text-gray-500 text-sm">
          채팅 기능 준비 중입니다
        </p>
      </div>

      <div className="border-t border-white/10 p-3">
        <div className="flex gap-2">
          <input
            type="text"
            placeholder="메시지를 입력하세요"
            disabled
            className="flex-1 rounded-lg bg-white/5 border border-white/10 px-3 py-2 text-sm text-white placeholder-gray-500 focus:outline-none focus:border-white/30 disabled:opacity-50"
          />
          <button
            disabled
            className="px-4 py-2 bg-blue-600 text-white text-sm rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          >
            전송
          </button>
        </div>
      </div>
    </div>
  );
}
