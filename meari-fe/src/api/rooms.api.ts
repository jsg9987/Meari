import axiosInstance from './axiosInstance';
import { apiConfig } from './apiConfig';

export interface CreateRoomRequest {
  title: string;
  theme_id: number;
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
          content_id: payload.theme_id,
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
  const useMock = import.meta.env.VITE_USE_MOCK_ROOMS === 'true';
  if (useMock) {
    return createRoomMock(payload);
  }
  const response = await axiosInstance.post('/api/v1/rooms', payload);
  const responseData = (response as { data?: CreateRoomResponse }).data ?? response;
  return responseData as CreateRoomResponse;
};

const useMock = apiConfig.shouldMock('ROOMS');

export const createRoom = useMock ? createRoomMock : createRoomReal;

// --- Get Rooms (방 목록 조회) ---
export interface RoomItem {
  room_id: string;
  title: string;
  current_people: number;
  max_people: number;
  has_password: boolean;
  theme: string;
}

export interface GetRoomsRequest {
  theme?: string;
  keyword?: string;
}

export interface GetRoomsResponse {
  success: boolean;
  data: {
    rooms: RoomItem[];
  } | null;
  error: {
    code: string;
    message: string;
  } | null;
}

const mockRooms: RoomItem[] = [
  { room_id: 'room_1', title: '초보만 들어오세요 :(', current_people: 4, max_people: 4, has_password: false, theme: '생활' },
  { room_id: 'room_2', title: '빠 근', current_people: 2, max_people: 4, has_password: false, theme: '생활' },
  { room_id: 'room_3', title: '잠수방', current_people: 2, max_people: 4, has_password: true, theme: '비즈니스' },
  { room_id: 'room_4', title: 'SSAFY 광주 2반', current_people: 2, max_people: 4, has_password: true, theme: '비즈니스' },
  { room_id: 'room_5', title: '아무나 ㄱ', current_people: 4, max_people: 4, has_password: false, theme: '뉴스' },
  { room_id: 'room_6', title: 'vị trí việt nam', current_people: 4, max_people: 4, has_password: false, theme: '뉴스' },
  { room_id: 'room_7', title: 'ตำแหน่งของชาวไทย', current_people: 2, max_people: 4, has_password: false, theme: '공공행정' },
  { room_id: 'room_8', title: '아무나 들어와', current_people: 2, max_people: 4, has_password: false, theme: '공공행정' },
];

export const getRoomsMock = async (
  params: GetRoomsRequest
): Promise<GetRoomsResponse> => {
  console.log('[API] Mock Get Rooms Requested:', params);
  return new Promise((resolve) => {
    setTimeout(() => {
      let filteredRooms = [...mockRooms];

      if (params.theme && params.theme !== '전체') {
        filteredRooms = filteredRooms.filter(room => room.theme === params.theme);
      }

      if (params.keyword) {
        const keyword = params.keyword.toLowerCase();
        filteredRooms = filteredRooms.filter(room =>
          room.title.toLowerCase().includes(keyword)
        );
      }

      resolve({
        success: true,
        data: { rooms: filteredRooms },
        error: null,
      });
    }, 300);
  });
};

export const getRoomsReal = async (
  params: GetRoomsRequest
): Promise<GetRoomsResponse> => {
  const response = await axiosInstance.get('/api/v1/rooms', { params });
  return response.data;
};

export const getRooms = useMock ? getRoomsMock : getRoomsReal;

// --- Join Room (방 참여) ---
export interface JoinRoomRequest {
  room_id: string;
  password?: string;
}

export interface JoinRoomResponse {
  success: boolean;
  data: {
    room_id: string;
  } | null;
  error: {
    code: string;
    message: string;
  } | null;
}

export const joinRoomMock = async (
  payload: JoinRoomRequest
): Promise<JoinRoomResponse> => {
  console.log('[API] Mock Join Room Requested:', payload);
  return new Promise((resolve, reject) => {
    setTimeout(() => {
      const room = mockRooms.find(r => r.room_id === payload.room_id);

      if (room?.has_password && payload.password !== '1234') {
        reject({
          response: {
            status: 401,
            data: {
              success: false,
              data: null,
              error: {
                code: 'ROOM_INVALID_PASSWORD',
                message: '비밀번호가 올바르지 않습니다.',
              },
            },
          },
        });
        return;
      }

      resolve({
        success: true,
        data: { room_id: payload.room_id },
        error: null,
      });
    }, 300);
  });
};

export const joinRoomReal = async (
  payload: JoinRoomRequest
): Promise<JoinRoomResponse> => {
  const response = await axiosInstance.post(`/api/v1/rooms/${payload.room_id}/join`, {
    password: payload.password,
  });
  return response.data;
};

export const joinRoom = useMock ? joinRoomMock : joinRoomReal;

console.log(`[RoomsAPI] Initialized. Mode: ${useMock ? 'MOCK' : 'REAL'}`);

