import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { createRoom } from '../../api/rooms.api';
import { getThemes, type Theme } from '../../api/contents.api';

const CreateRoomButton = () => {
  const navigate = useNavigate();
  const [isOpen, setIsOpen] = useState(false);
  const [title, setTitle] = useState('');
  const [usePassword, setUsePassword] = useState(false);
  const [password, setPassword] = useState('');
  const [maxPeople, setMaxPeople] = useState(4);
  const [themes, setThemes] = useState<Theme[]>([]);
  const [selectedThemeId, setSelectedThemeId] = useState<number | null>(null);
  const [errorMessage, setErrorMessage] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isLoadingThemes, setIsLoadingThemes] = useState(false);

  useEffect(() => {
    if (isOpen) {
      loadThemes();
    }
  }, [isOpen]);

  const loadThemes = async () => {
    setIsLoadingThemes(true);
    try {
      const response = await getThemes();
      if (response.data.success && response.data.data.length > 0) {
        setThemes(response.data.data);
        setSelectedThemeId(response.data.data[0].theme_id);
      }
    } catch (error) {
      console.error('Failed to load themes:', error);
    } finally {
      setIsLoadingThemes(false);
    }
  };

  const resetForm = () => {
    setTitle('');
    setUsePassword(false);
    setPassword('');
    setMaxPeople(4);
    setSelectedThemeId(themes.length > 0 ? themes[0].theme_id : null);
    setErrorMessage('');
  };

  const handleOpen = () => {
    setIsOpen(true);
  };

  const handleClose = () => {
    setIsOpen(false);
    setIsSubmitting(false);
    setErrorMessage('');
  };

  const handleSubmit = async () => {
    const trimmedTitle = title.trim();
    if (!trimmedTitle) {
      setErrorMessage('방 제목을 입력해주세요.');
      return;
    }
    if (!selectedThemeId) {
      setErrorMessage('테마를 선택해주세요.');
      return;
    }
    if (usePassword && !password) {
      setErrorMessage('비밀번호를 입력해주세요.');
      return;
    }

    setIsSubmitting(true);
    setErrorMessage('');
    try {
      const response = await createRoom({
        title: trimmedTitle,
        password: usePassword ? password : null,
        max_people: maxPeople,
        theme_id: selectedThemeId
      });

      console.log(response)

      if (!response.data.success) {
        setErrorMessage(response.data.error?.message ?? '방 생성에 실패했습니다.');
        return;
      }

      const roomId = response.data.data?.room_id;
      if (!roomId) {
        setErrorMessage('방 ID를 가져올 수 없습니다.');
        return;
      }

      handleClose();
      resetForm();
      navigate(`/shadowing/${roomId}`, { state: { isOwner: true } });
    } catch (error) {
      setErrorMessage('방 생성에 실패했습니다.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div>
      <button
        className='bg-(--color-bg-button) rounded-(--radius-button) px-8 py-3 text-white font-medium cursor-pointer'
        type="button"
        onClick={handleOpen}
      >
        방 생성
      </button>

      {isOpen && (
        <div
          className="fixed inset-0 bg-black/40 flex items-center justify-center z-1000 p-4"
          onClick={handleClose}
        >
          <div
            role="dialog"
            aria-modal="true"
            className="bg-white rounded-xl p-6 w-full max-w-125 max-h-[90vh] overflow-y-auto shadow-2xl flex flex-col gap-3"
            onClick={(event) => event.stopPropagation()}
          >
            <h2 className="text-xl font-semibold m-0">방 만들기</h2>

            <label className="font-semibold">
              테마 선택 <span className="text-red-600">*</span>
            </label>
            {isLoadingThemes ? (
              <div className="grid grid-cols-2 gap-3">
                {[1, 2, 3, 4].map((index) => (
                  <div key={index} className="border-2 border-gray-300 rounded-lg overflow-hidden bg-white">
                    <div className="w-full h-25 bg-gray-300 animate-pulse" />
                    <div className="p-3">
                      <div className="h-3.5 bg-gray-300 rounded mb-2 animate-pulse" />
                      <div className="h-2.5 bg-gray-300 rounded mb-1 animate-pulse" />
                      <div className="h-2.5 bg-gray-300 rounded w-[70%] animate-pulse" />
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="grid grid-cols-2 gap-3">
                {themes.map((theme) => (
                  <button
                    key={theme.theme_id}
                    type="button"
                    onClick={() => setSelectedThemeId(theme.theme_id)}
                    className={`border-2 rounded-lg p-0 cursor-pointer bg-white transition-all overflow-hidden text-left ${
                      selectedThemeId === theme.theme_id
                        ? 'border-blue-500 bg-blue-50'
                        : 'border-gray-300 hover:border-gray-400'
                    }`}
                  >
                    <img
                      src={theme.theme_url}
                      alt={theme.name}
                      className="w-full h-25 object-cover"
                    />
                    <div className="p-3">
                      <h3 className="m-0 text-sm font-semibold mb-1">{theme.name}</h3>
                      <p className="m-0 text-xs text-gray-600 leading-snug">{theme.description}</p>
                    </div>
                  </button>
                ))}
              </div>
            )}

            <label className="font-semibold">
              방 제목 <span className="text-red-600">*</span>
            </label>
            <input
              type="text"
              value={title}
              onChange={(event) => setTitle(event.target.value)}
              placeholder="예: 같이 카페 대화 연습해요!"
              className="px-3 py-2.5 rounded-lg border border-gray-300 focus:outline-none focus:border-blue-500"
            />

            <label className="flex items-center gap-2 cursor-pointer">
              <input
                type="checkbox"
                checked={usePassword}
                onChange={(event) => setUsePassword(event.target.checked)}
                className="cursor-pointer"
              />
              비밀번호 사용
            </label>

            <input
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              placeholder="비밀번호 입력"
              className={`px-3 py-2.5 rounded-lg border border-gray-300 focus:outline-none focus:border-blue-500 ${
                !usePassword ? 'bg-gray-100' : ''
              }`}
              disabled={!usePassword}
            />

            <label className="font-semibold">최대 인원</label>
            <select
              value={maxPeople}
              onChange={(event) => setMaxPeople(Number(event.target.value))}
              className="px-3 py-2.5 rounded-lg border border-gray-300 focus:outline-none focus:border-blue-500 cursor-pointer"
            >
              {[1, 2, 3, 4].map((count) => (
                <option key={count} value={count}>
                  {count}명
                </option>
              ))}
            </select>

            {errorMessage && <p className="text-red-600 m-0 text-sm">{errorMessage}</p>}

            <div className="flex justify-end gap-2 mt-2">
              <button
                type="button"
                onClick={handleClose}
                disabled={isSubmitting}
                className="px-4 py-2 rounded-lg border border-gray-300 bg-white hover:bg-gray-50 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              >
                취소
              </button>
              <button
                type="button"
                onClick={handleSubmit}
                disabled={isSubmitting}
                className="px-4 py-2 rounded-lg bg-blue-600 text-white hover:bg-blue-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {isSubmitting ? '생성 중...' : '생성'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default CreateRoomButton;
