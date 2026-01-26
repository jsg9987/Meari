interface ChatPanelProps {
  roomId: string;
}

// TODO: 민혁님 채팅 여기서 구현하심 됩니다.

export default function ChatPanel({ roomId }: ChatPanelProps) {
  return (
    <div className="flex flex-col h-full bg-white">
      <div className="flex-1 overflow-y-auto p-4">
        <p className="text-center text-gray-500 text-sm py-8">
          채팅 기능 준비 중입니다
        </p>
      </div>

      <div className="border-t border-gray-200 p-3 bg-gray-50">
        <div className="flex gap-2">
          <input
            type="text"
            placeholder="메시지를 입력하세요"
            disabled
            className="flex-1 rounded-lg bg-white border border-gray-300 px-3 py-2 text-sm text-gray-900 placeholder-gray-400 focus:outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 disabled:opacity-50 disabled:bg-gray-100"
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
