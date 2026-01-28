import axiosInstance from './axiosInstance';

export interface CreateRoomRequest {
  title: string;
  theme_id: number;
  password: string | null;
  max_people: number;
}

export interface CreateRoomResponse {
  success: boolean;
  data: {
    room_id: number;
    owner_id: number;
    owner_nickname: string;
    theme_id: number;
    theme_name: string;
    title: string;
    max_people: number;
    current_people: number;
    status: string;
    has_password: boolean;
    created_at: string;
  } | null;
  error: {
    code: string;
    message: string;
  } | null;
}

export const createRoomMock = async (
  payload: CreateRoomRequest
): Promise<CreateRoomResponse> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({
        success: true,
        data: {
          room_id: 1,
          owner_id: 1,
          owner_nickname: '김싸피',
          theme_id: payload.theme_id,
          theme_name: '일상회화',
          title: payload.title,
          max_people: payload.max_people,
          current_people: 1,
          status: 'WAITING',
          has_password: payload.password !== null,
          created_at: new Date().toISOString(),
        },
        error: null,
      });
    }, 600);
  });
};

export const createRoom = async (
  payload: CreateRoomRequest
) => {
  const useMock = import.meta.env.VITE_USE_MOCK_ROOMS === 'true';
  if (useMock) {
    return { data: await createRoomMock(payload) };
  }
  const response = await axiosInstance.post('/rooms', payload);
  return response;
};

export interface RoomMember {
  member_id: number;
  nickname: string;
  profile_url: string | null;
  is_owner: boolean;
  is_ready: boolean;
  role_id: number | null;
}

export interface RoomDetailData {
  room_id: number;
  owner_id: number;
  theme_id: number;
  theme_name: string;
  title: string;
  max_people: number;
  status: string;
  has_password: boolean;
  created_at: string;
  members: RoomMember[];
}

export interface RoomDetailResponse {
  success: boolean;
  data: RoomDetailData | null;
  error: {
    code: string;
    message: string;
  } | null;
}

export const getRoomDetail = async (roomId: number) => {
  const response = await axiosInstance.get(`/rooms/${roomId}`);
  return response;
};

export interface EnterRoomRequest {
  password?: string;
}

export interface EnterRoomResponse {
  success: boolean;
  data: {
    message: string;
  } | null;
  error: {
    code: string;
    message: string;
  } | null;
}

// TODO: 방 꽉차면 처리
export const enterRoom = async (
  roomId: number,
  payload: EnterRoomRequest
) => {
  const response = await axiosInstance.post(`/rooms/${roomId}/enter`, payload);
  return response;
};

export const leaveRoom = async (roomId: number): Promise<void> => {
  await axiosInstance.delete(`/rooms/${roomId}/leave`);
};

export interface WebRTCEnterRequest {
  password?: string;
}

export interface WebRTCEnterResponse {
  success: boolean;
  data: {
    token: string;
    sessionId: string;
  } | null;
  error: {
    code: string;
    message: string;
  } | null;
}

export const enterWebRTC = async (
  roomId: number,
  payload: WebRTCEnterRequest
) => {
  const response = await axiosInstance.post(`/rooms/${roomId}/webrtc/enter`, payload);
  return response;
};

export const leaveWebRTC = async (roomId: number): Promise<void> => {
  await axiosInstance.post(`/rooms/${roomId}/webrtc/leave`);
};
