import { useState, useEffect } from "react";
import { X, Clock } from "lucide-react";
import { getThemeContents, type Content } from "../../api/contents.api";

type ContentSelectModalProps = {
  themeId: number;
  onClose: () => void;
  onSelect: (content: Content) => void;
};

export default function ContentSelectModal({ themeId, onClose, onSelect }: ContentSelectModalProps) {
  const [contents, setContents] = useState<Content[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const loadContents = async () => {
      setIsLoading(true);
      setError(null);
      try {
        const response = await getThemeContents(themeId);
        if (response.data.success) {
          setContents(response.data.data);
        } else {
          setError(response.data.error?.message || '컨텐츠를 불러오는데 실패했습니다.');
        }
      } catch (err) {
        console.error('Failed to load contents:', err);
        setError('컨텐츠를 불러오는데 실패했습니다.');
      } finally {
        setIsLoading(false);
      }
    };

    loadContents();
  }, [themeId]);

  const formatDuration = (seconds: number) => {
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = seconds % 60;
    return `${minutes}:${remainingSeconds.toString().padStart(2, '0')}`;
  };

  return (
    <div
      className="fixed inset-0 bg-black/40 flex items-center justify-center z-1000 p-4"
      onClick={onClose}
    >
      <div
        role="dialog"
        aria-modal="true"
        className="bg-white rounded-xl w-full max-w-4xl max-h-[85vh] overflow-hidden shadow-2xl flex flex-col"
        onClick={(e) => e.stopPropagation()}
      >
        {/* 헤더 */}
        <div className="flex items-center justify-between p-6 border-b border-gray-200">
          <h2 className="text-xl font-semibold text-gray-900">컨텐츠 선택</h2>
          <button
            onClick={onClose}
            className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
            aria-label="닫기"
          >
            <X size={24} className="text-gray-600" />
          </button>
        </div>

        {/* 컨텐츠 영역 */}
        <div className="flex-1 overflow-y-auto p-6">
          {error && (
            <div className="p-4 bg-red-50 border border-red-200 rounded-lg text-red-800 text-sm">
              {error}
            </div>
          )}

          {isLoading ? (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {[1, 2, 3, 4, 5, 6].map((i) => (
                <div key={i} className="border border-gray-200 rounded-lg overflow-hidden">
                  <div className="w-full h-40 bg-gray-300 animate-pulse" />
                  <div className="p-4 space-y-2">
                    <div className="h-5 bg-gray-300 rounded animate-pulse" />
                    <div className="h-4 bg-gray-300 rounded animate-pulse w-3/4" />
                    <div className="h-4 bg-gray-300 rounded animate-pulse w-1/2" />
                  </div>
                </div>
              ))}
            </div>
          ) : contents.length === 0 ? (
            <div className="flex items-center justify-center py-16 text-gray-500">
              컨텐츠가 없습니다.
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {contents.map((content) => (
                <button
                  key={content.content_id}
                  onClick={() => onSelect(content)}
                  className="border border-gray-200 rounded-lg overflow-hidden hover:border-blue-500 hover:shadow-lg transition-all text-left group"
                >
                  <div className="relative w-full h-40 overflow-hidden">
                    <img
                      src={content.thumbnail_url}
                      alt={content.title}
                      className="w-full h-full object-cover group-hover:scale-105 transition-transform"
                    />
                    <div className="absolute bottom-2 right-2 bg-black/70 text-white px-2 py-1 rounded text-xs flex items-center gap-1">
                      <Clock size={12} />
                      <span>{formatDuration(content.duration)}</span>
                    </div>
                  </div>
                  <div className="p-4">
                    <h3 className="font-semibold text-gray-900 mb-1 line-clamp-1">
                      {content.title}
                    </h3>
                    <p className="text-sm text-gray-600 line-clamp-2">
                      {content.description}
                    </p>
                  </div>
                </button>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
