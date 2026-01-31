import { useState, useEffect } from 'react';
import { createPortal } from 'react-dom';
import { Lock, X } from 'lucide-react';

interface PasswordModalProps {
  roomTitle: string;
  onSubmit: (password: string) => Promise<void>;
  onCancel: () => void;
  errorMessage?: string;
}

// TODO: 비밀번호 모달 UI 수정

export default function PasswordModal({ roomTitle, onSubmit, onCancel, errorMessage }: PasswordModalProps) {
  const [password, setPassword] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState(errorMessage || '');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!password.trim()) {
      setError('비밀번호를 입력해주세요.');
      return;
    }

    setIsSubmitting(true);
    setError('');
    try {
      await onSubmit(password);
    } catch (err) {
      setError('비밀번호가 일치하지 않습니다.');
    } finally {
      setIsSubmitting(false);
    }
  };

  // 모달이 열릴 때 스크롤 방지
  useEffect(() => {
    const scrollBarWidth = window.innerWidth - document.documentElement.clientWidth;
    document.body.style.overflow = 'hidden';
    document.body.style.paddingRight = `${scrollBarWidth}px`;

    return () => {
      document.body.style.overflow = '';
      document.body.style.paddingRight = '';
    }
  }, [])

  return createPortal(
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-1000 p-4">
      <div
        className="bg-white rounded-xl p-6 w-full max-w-md shadow-2xl"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-yellow-50 rounded-lg">
              <Lock size={24} className="text-yellow-600" />
            </div>
            <div>
              <h2 className="text-xl font-semibold">비밀번호 입력</h2>
              <p className="text-sm text-gray-600 mt-1">{roomTitle}</p>
            </div>
          </div>
          <button
            onClick={onCancel}
            className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
            disabled={isSubmitting}
          >
            <X size={20} className="text-gray-600" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              방 비밀번호
            </label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="비밀번호를 입력하세요"
              className="w-full px-4 py-3 rounded-lg border border-gray-300 focus:outline-none focus:border-blue-500"
              disabled={isSubmitting}
              autoFocus
            />
          </div>

          {error && (
            <p className="text-red-600 text-sm">{error}</p>
          )}

          <div className="flex gap-3 pt-2">
            <button
              type="button"
              onClick={onCancel}
              disabled={isSubmitting}
              className="flex-1 px-4 py-3 rounded-lg border border-gray-300 bg-white hover:bg-gray-50 transition-colors disabled:opacity-50 disabled:cursor-not-allowed font-medium"
            >
              나가기
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="flex-1 px-4 py-3 rounded-lg bg-blue-600 text-white hover:bg-blue-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed font-medium"
            >
              {isSubmitting ? '확인 중...' : '입장'}
            </button>
          </div>
        </form>
      </div>
    </div>,
    document.body
  );
}
