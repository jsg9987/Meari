import { create } from 'zustand';
import type { RoomDetailData } from '../api/rooms.api';

interface RoomState {
  roomData: RoomDetailData | null;
  contentId: number | null;
  setRoomData: (data: RoomDetailData | null) => void;
  setContentId: (contentId: number | null) => void;
  clearRoomData: () => void;
}

export const useRoomStore = create<RoomState>((set) => ({
  roomData: null,
  contentId: null,
  setRoomData: (data) => set({ roomData: data }),
  setContentId: (contentId) => set({ contentId }),
  clearRoomData: () => set({ roomData: null, contentId: null }),
}));
