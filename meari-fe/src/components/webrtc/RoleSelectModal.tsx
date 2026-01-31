import { X, Check } from "lucide-react";
import type { Role } from "../../hooks/useRoomWebSocket";

interface RoleSelectModalProps {
  roles: Role[];
  onSelect: (role: Role) => void;
  onClose: () => void;
  onConfirm?: () => void;
  selectedRoleId?: number;
  isHost?: boolean;
  isConfirming?: boolean;
}

export default function RoleSelectModal({
  roles,
  onSelect,
  onClose,
  onConfirm,
  selectedRoleId,
  isHost = false,
  isConfirming = false,
}: RoleSelectModalProps) {
  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-xl max-w-2xl w-full mx-4 max-h-[80vh] overflow-hidden">
        {/* 헤더 */}
        <div className="flex items-center justify-between p-6 border-b border-gray-200">
          <h2 className="text-xl font-semibold text-gray-900">
            캐릭터 선택
          </h2>
          <button
            onClick={onClose}
            className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
            title="닫기"
          >
            <X size={20} className="text-gray-600" />
          </button>
        </div>

        {/* 역할 목록 */}
        <div className="p-6 overflow-y-auto max-h-[calc(80vh-120px)]">
          <div className="grid grid-cols-2 gap-4">
            {roles.map((role) => {
              const isSelected = selectedRoleId === role.role_id;
              return (
                <button
                  key={role.id}
                  onClick={() => onSelect(role)}
                  className={`p-4 rounded-lg border-2 transition-all text-left ${
                    isSelected
                      ? "border-blue-600 bg-blue-50"
                      : "border-gray-200 hover:border-blue-400 hover:bg-gray-50"
                  }`}
                >
                  <div className="flex flex-col gap-2">
                    <div className="flex items-center justify-between">
                      <span className="text-lg font-semibold text-gray-900">
                        {role.name}
                      </span>
                      {isSelected && (
                        <div className="px-2 py-1 bg-blue-600 text-white text-xs rounded-full">
                          선택됨
                        </div>
                      )}
                    </div>
                    <div className="text-sm text-gray-500">
                      역할 ID: {role.role_id}
                    </div>
                  </div>
                </button>
              );
            })}
          </div>

          {roles.length === 0 && (
            <div className="text-center py-12">
              <p className="text-gray-500">사용 가능한 캐릭터가 없습니다</p>
            </div>
          )}
        </div>

        {/* 하단 영역 */}
        <div className="border-t border-gray-200">
          {/* 안내 문구 */}
          <div className="p-4 bg-gray-50">
            <p className="text-sm text-gray-600 text-center">
              캐릭터를 선택하여 역할을 등록하세요
            </p>
          </div>

          {/* 선택 완료 버튼 (방장만) */}
          {isHost && onConfirm && (
            <div className="p-4 bg-white">
              <button
                onClick={onConfirm}
                disabled={isConfirming}
                className={`w-full flex items-center justify-center gap-2 px-6 py-3 rounded-lg font-semibold transition-all ${
                  isConfirming
                    ? "bg-gray-400 cursor-not-allowed text-white"
                    : "bg-blue-600 hover:bg-blue-700 text-white"
                }`}
              >
                {isConfirming ? (
                  <>
                    <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin" />
                    <span>확정 중...</span>
                  </>
                ) : (
                  <>
                    <Check size={20} />
                    <span>역할 선택 완료</span>
                  </>
                )}
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
