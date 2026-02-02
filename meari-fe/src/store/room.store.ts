import { create } from 'zustand';
import type { RoomDetailData } from '../api/rooms.api';

interface RoomState {
  roomData: RoomDetailData | null;
  setRoomData: (data: RoomDetailData | null) => void;
  clearRoomData: () => void;
}

export const useRoomStore = create<RoomState>((set) => ({
  roomData: null,
  setRoomData: (data) => set({ roomData: data }),
  clearRoomData: () => set({ roomData: null }),
}));
