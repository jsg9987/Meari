import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
// import { createRoom } from '../../api/rooms.api';
import { createRoomMock as createRoom } from '../../api/rooms.api';

const CreateRoomButton = () => {
  const navigate = useNavigate();
  const [isOpen, setIsOpen] = useState(false);
  const [title, setTitle] = useState('');
  const [usePassword, setUsePassword] = useState(false);
  const [password, setPassword] = useState('');
  const [maxPeople, setMaxPeople] = useState(4);
  const [errorMessage, setErrorMessage] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const resetForm = () => {
    setTitle('');
    setUsePassword(false);
    setPassword('');
    setMaxPeople(4);
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
      });

      if (!response.success) {
        setErrorMessage(response.error?.message ?? '방 생성에 실패했습니다.');
        return;
      }

      const roomId =
        response.data?.room_id ??
        (response as { data?: { data?: { room_id?: string } } }).data?.data?.room_id;
      if (!roomId) {
        return;
      }

      handleClose();
      resetForm();
      navigate(`/shadowing/${roomId}`);
    } catch (error) {
      setErrorMessage('방 생성에 실패했습니다.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div>
      <button type="button" onClick={handleOpen}>
        방 생성
      </button>

      {isOpen && (
        <div style={styles.overlay} onClick={handleClose}>
          <div
            role="dialog"
            aria-modal="true"
            style={styles.modal}
            onClick={(event) => event.stopPropagation()}
          >
            <h2 style={styles.title}>방 만들기</h2>

            <label style={styles.label}>
              방 제목 <span style={styles.required}>*</span>
            </label>
            <input
              type="text"
              value={title}
              onChange={(event) => setTitle(event.target.value)}
              placeholder="예: 같이 카페 대화 연습해요!"
              style={styles.input}
            />

            <label style={styles.checkboxLabel}>
              <input
                type="checkbox"
                checked={usePassword}
                onChange={(event) => setUsePassword(event.target.checked)}
              />
              비밀번호 사용
            </label>

            <input
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              placeholder="비밀번호 입력"
              style={{
                ...styles.input,
                ...(!usePassword ? styles.inputDisabled : {}),
              }}
              disabled={!usePassword}
            />

            <label style={styles.label}>최대 인원</label>
            <select
              value={maxPeople}
              onChange={(event) => setMaxPeople(Number(event.target.value))}
              style={styles.select}
            >
              {[1, 2, 3, 4].map((count) => (
                <option key={count} value={count}>
                  {count}명
                </option>
              ))}
            </select>

            {errorMessage && <p style={styles.error}>{errorMessage}</p>}

            <div style={styles.actions}>
              <button type="button" onClick={handleClose} disabled={isSubmitting}>
                취소
              </button>
              <button type="button" onClick={handleSubmit} disabled={isSubmitting}>
                {isSubmitting ? '생성 중...' : '생성'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

const styles: Record<string, React.CSSProperties> = {
  overlay: {
    position: 'fixed',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    backgroundColor: 'rgba(0, 0, 0, 0.4)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 1000,
    padding: 16,
  },
  modal: {
    backgroundColor: '#ffffff',
    borderRadius: 12,
    padding: 24,
    width: '100%',
    maxWidth: 420,
    boxShadow: '0 20px 40px rgba(0, 0, 0, 0.2)',
    display: 'flex',
    flexDirection: 'column',
    gap: 12,
  },
  title: {
    margin: 0,
    fontSize: 20,
  },
  label: {
    fontWeight: 600,
  },
  required: {
    color: '#d32f2f',
  },
  checkboxLabel: {
    display: 'flex',
    gap: 8,
    alignItems: 'center',
  },
  input: {
    padding: '10px 12px',
    borderRadius: 8,
    border: '1px solid #d0d0d0',
  },
  inputDisabled: {
    backgroundColor: '#f3f3f3',
  },
  select: {
    padding: '10px 12px',
    borderRadius: 8,
    border: '1px solid #d0d0d0',
  },
  actions: {
    display: 'flex',
    justifyContent: 'flex-end',
    gap: 8,
    marginTop: 8,
  },
  error: {
    color: '#d32f2f',
    margin: 0,
  },
};

export default CreateRoomButton;
