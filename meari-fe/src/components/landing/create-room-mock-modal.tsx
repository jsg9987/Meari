'use client'

import { useState } from 'react';

// Mock data
const MOCK_THEMES = [
  {
    theme_id: 1,
    name: "일상회화",
    description: "일상에서 자주 사용하는 회화 표현을 중심으로 자연스럽게 말하는 연습",
    theme_url: "https://images.unsplash.com/photo-1554118811-1e0d58224f24?w=400"
  },
  {
    theme_id: 2,
    name: "비즈니스",
    description: "회사에서 사용하는 업무 관련 대화를 상황별로 익히는 실전 연습",
    theme_url: "https://images.unsplash.com/photo-1542744173-8e7e53415bb0?w=400"
  },
  {
    theme_id: 3,
    name: "뉴스",
    description: "시사 뉴스 리포트를 따라하며 발음과 억양을 함께 다듬는 연습",
    theme_url: "https://images.unsplash.com/photo-1504711434969-e33886168f5c?w=400"
  },
  {
    theme_id: 4,
    name: "여행",
    description: "가이드와 관광객의 대화를 통해 다양한 여행 상황 회화를 연습",
    theme_url: "https://images.unsplash.com/photo-1488646953014-85cb44e25828?w=400"
  }
];

const CreateRoomMockModal = () => {
  const [selectedThemeId, setSelectedThemeId] = useState<number>(1);

  return (
    <div className="bg-white rounded-xl p-5 w-full max-w-md shadow-lg flex flex-col gap-3">
      <h2 className="text-lg font-semibold m-0">방 만들기</h2>

      <label className="font-semibold text-sm">
        테마 선택 <span className="text-red-600">*</span>
      </label>

      <div className="grid grid-cols-2 gap-2">
        {MOCK_THEMES.map((theme) => (
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
              className="w-full h-16 object-cover"
            />
            <div className="p-2">
              <h3 className="m-0 text-xs font-semibold mb-0.5">{theme.name}</h3>
              <p className="m-0 text-[10px] text-gray-600 leading-tight line-clamp-2">
                {theme.description}
              </p>
            </div>
          </button>
        ))}
      </div>

      <div className="flex justify-end gap-2 mt-2">
        <button
          type="button"
          className="px-4 py-1.5 text-sm rounded-lg border border-gray-300 bg-white hover:bg-gray-50 transition-colors cursor-pointer"
        >
          취소
        </button>
        <button
          type="button"
          className="px-4 py-1.5 text-sm rounded-lg bg-blue-600 text-white hover:bg-blue-700 transition-colors cursor-pointer"
        >
          생성
        </button>
      </div>
    </div>
  );
};

export default CreateRoomMockModal;
