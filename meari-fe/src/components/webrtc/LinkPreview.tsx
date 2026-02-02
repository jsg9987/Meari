import { ExternalLink } from 'lucide-react';

interface LinkPreviewProps {
  url: string;
  isMyMessage?: boolean;
}

export default function LinkPreview({ url, isMyMessage = false }: LinkPreviewProps) {
  // URL에서 도메인 추출
  let domain = '';
  try {
    const urlObj = new URL(url);
    domain = urlObj.hostname.replace('www.', '');
  } catch {
    domain = url;
  }

  return (
    <a
      href={url}
      target="_blank"
      rel="noopener noreferrer"
      className={`block mt-2 p-3 rounded-lg border transition-colors ${
        isMyMessage
          ? 'bg-blue-400 hover:bg-blue-300 border-blue-300 text-white'
          : 'bg-gray-50 hover:bg-gray-100 border-gray-200 text-gray-900'
      }`}
      onClick={(e) => e.stopPropagation()}
    >
      <div className="flex items-center gap-2">
        <ExternalLink size={16} className="shrink-0" />
        <div className="flex-1 min-w-0">
          <p className={`text-xs mb-0.5 ${isMyMessage ? 'text-blue-100' : 'text-gray-500'}`}>
            {domain}
          </p>
          <p className="text-sm truncate font-medium">{url}</p>
        </div>
      </div>
    </a>
  );
}
