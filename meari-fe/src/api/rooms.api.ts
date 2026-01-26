import axiosInstance from './axiosInstance';
import { apiConfig } from './apiConfig';

export interface CreateRoomRequest {
  title: string;
  password: string | null;
  max_people: number;
}

export interface CreateRoomResponse {
  success: boolean;
  data: {
    room_id: string;
    title: string;
    owner_id: number;
    content_id: number;
    is_active: string;
  } | null;
  error: {
    code: string;
    message: string;
  } | null;
}

export const createRoomMock = async (
  payload: CreateRoomRequest
): Promise<CreateRoomResponse> => {
  console.log('[API] Mock Create Room Requested:', payload);
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({
        success: true,
        data: {
          room_id: 'room_uuid_1234',
          title: payload.title,
          owner_id: 1,
          content_id: 101,
          is_active: 'ACTIVE',
        },
        error: null,
      });
    }, 600);
  });
};

export const createRoomReal = async (
  payload: CreateRoomRequest
): Promise<CreateRoomResponse> => {
  const response = await axiosInstance.post('/api/v1/rooms', payload);
  const responseData = (response as { data?: CreateRoomResponse }).data ?? response;
  return responseData as CreateRoomResponse;
};

const useMock = apiConfig.shouldMock('ROOMS');

export const createRoom = useMock ? createRoomMock : createRoomReal;

console.log(`[RoomsAPI] Initialized. Mode: ${useMock ? 'MOCK' : 'REAL'}`);
