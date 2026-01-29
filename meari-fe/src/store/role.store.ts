import { create } from 'zustand';
import type { Role } from '../hooks/useRoomWebSocket';

interface RoleAssignment {
  member_id: number;
  role_id: number;
}

interface RoleStore {
  // 사용 가능한 역할 목록
  availableRoles: Role[];
  // 멤버별 선택된 역할 (member_id -> role_id)
  selectedRoles: Record<number, number>;
  // 내가 선택한 역할 ID
  mySelectedRoleId: number | undefined;

  // Actions
  setAvailableRoles: (roles: Role[]) => void;
  setMySelectedRole: (roleId: number | undefined) => void;
  setMemberRole: (memberId: number, roleId: number) => void;
  removeMemberRole: (memberId: number) => void;
  clearRoles: () => void;
  getConfirmData: () => RoleAssignment[];
}

export const useRoleStore = create<RoleStore>((set, get) => ({
  availableRoles: [],
  selectedRoles: {},
  mySelectedRoleId: undefined,

  setAvailableRoles: (roles) => set({ availableRoles: roles }),

  setMySelectedRole: (roleId) => set({ mySelectedRoleId: roleId }),

  setMemberRole: (memberId, roleId) =>
    set((state) => ({
      selectedRoles: {
        ...state.selectedRoles,
        [memberId]: roleId,
      },
    })),

  removeMemberRole: (memberId) =>
    set((state) => {
      const newRoles = { ...state.selectedRoles };
      delete newRoles[memberId];
      return { selectedRoles: newRoles };
    }),

  clearRoles: () =>
    set({
      selectedRoles: {},
      mySelectedRoleId: undefined,
    }),

  getConfirmData: () => {
    const { selectedRoles } = get();
    return Object.entries(selectedRoles).map(([memberId, roleId]) => ({
      member_id: Number(memberId),
      role_id: roleId,
    }));
  },
}));
