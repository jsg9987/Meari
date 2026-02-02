import { useState, useRef, useEffect } from 'react';
import type { ChatMessage } from '../../hooks/useRoomWebSocket';
import LinkPreview from './LinkPreview';

interface ChatPanelProps {
  messages: ChatMessage[];
  onSendMessage: (message: string, nickname: string) => void;
  nickname: string;
  currentUserId: number;
}

// URL 감지 정규식 (http, https 프로토콜 포함)
const URL_REGEX = /(https?:\/\/[^\s]+)/g;

// 메시지에서 첫 번째 URL 추출
function extractFirstUrl(text: string): string | null {
  const match = text.match(URL_REGEX);
  return match ? match[0] : null;
}

// 메시지 텍스트를 파싱하여 링크를 <a> 태그로 변환
function parseMessageWithLinks(text: string): (string | JSX.Element)[] {
  const parts: (string | JSX.Element)[] = [];
  let lastIndex = 0;
  let match;

  while ((match = URL_REGEX.exec(text)) !== null) {
    // URL 이전 텍스트 추가
    if (match.index > lastIndex) {
      parts.push(text.substring(lastIndex, match.index));
    }

    // URL을 링크로 변환
    const url = match[0];
    parts.push(
      <a
        key={match.index}
        href={url}
        target="_blank"
        rel="noopener noreferrer"
        className="underline hover:text-blue-300 break-all"
        onClick={(e) => e.stopPropagation()}
      >
        {url}
      </a>
    );

    lastIndex = match.index + url.length;
  }

  // 남은 텍스트 추가
  if (lastIndex < text.length) {
    parts.push(text.substring(lastIndex));
  }

  return parts.length > 0 ? parts : [text];
}

export default function ChatPanel({ messages, onSendMessage, nickname, currentUserId }: ChatPanelProps) {
  const [inputMessage, setInputMessage] = useState('');
  const messagesEndRef = useRef<HTMLDivElement>(null);

  // 새 메시지가 오면 스크롤을 맨 아래로
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const handleSendMessage = () => {
    if (inputMessage.trim()) {
      onSendMessage(inputMessage.trim(), nickname);
      setInputMessage('');
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSendMessage();
    }
  };

  return (
    <div className="flex flex-col h-full bg-white">
      <div className="flex-1 overflow-y-auto p-4 space-y-3">
        {messages.length === 0 ? (
          <p className="text-center text-gray-500 text-sm py-8">
            채팅을 시작해보세요
          </p>
        ) : (
          messages.map((msg, index) => {
            // 시스템 메시지인 경우
            if (msg.isSystem) {
              return (
                <div key={index} className="flex justify-center">
                  <div className="px-3 py-1.5 bg-gray-200 text-gray-600 rounded-full text-xs">
                    {msg.message}
                  </div>
                </div>
              );
            }

            // 일반 메시지
            const isMyMessage = msg.sender_id === currentUserId;
            const firstUrl = extractFirstUrl(msg.message);

            return (
              <div
                key={index}
                className={`flex flex-col ${isMyMessage ? 'items-end' : 'items-start'}`}
              >
                <div className="flex items-baseline gap-2">
                  <span className="text-xs font-semibold text-gray-700">
                    {msg.nickname}
                  </span>
                  <span className="text-xs text-gray-400">
                    {new Date(msg.timestamp).toLocaleTimeString('ko-KR', {
                      hour: '2-digit',
                      minute: '2-digit',
                    })}
                  </span>
                </div>
                <div className="max-w-[85%] flex flex-col">
                  <div
                    className={`mt-1 px-3 py-2 rounded-lg inline-block ${
                      isMyMessage
                        ? 'bg-blue-500 text-white'
                        : 'bg-gray-100 text-gray-900'
                    }`}
                  >
                    <p className="text-sm wrap-break-word">
                      {parseMessageWithLinks(msg.message)}
                    </p>
                  </div>
                  {/* 링크 미리보기 (첫 번째 URL에 대해서만) */}
                  {firstUrl && (
                    <LinkPreview url={firstUrl} isMyMessage={isMyMessage} />
                  )}
                </div>
              </div>
            );
          })
        )}
        <div ref={messagesEndRef} />
      </div>

      <div className="border-t border-gray-200 p-3 bg-gray-50">
        <div className="flex gap-2">
          <input
            type="text"
            placeholder="메시지를 입력하세요"
            value={inputMessage}
            onChange={(e) => setInputMessage(e.target.value)}
            onKeyDown={handleKeyDown}
            className="flex-1 rounded-lg bg-white border border-gray-300 px-3 py-2 text-sm text-gray-900 placeholder-gray-400 focus:outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500"
          />
          <button
            onClick={handleSendMessage}
            disabled={!inputMessage.trim()}
            className="px-4 py-2 bg-blue-600 text-white text-sm rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          >
            전송
          </button>
        </div>
      </div>
    </div>
  );
}
