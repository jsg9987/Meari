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
    room_id: number;
    title: string;
    owner_id: number;
    owner_nickname: string;
    theme_id: number;
    theme_name: string;
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
  console.log('[API] Mock Create Room Requested:', payload);
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

export const createRoomReal = async (
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

const useMock = apiConfig.shouldMock('ROOMS');

export const createRoom = useMock ? createRoomMock : createRoomReal;

// --- Get Rooms (방 목록 조회) ---
export interface RoomItem {
  room_id: number;
  title: string;
  content_title?: string;
  theme_name: string;
  current_people: number;
  max_people: number;
  status: string;
  has_password: boolean;
  created_at: string;
}

export interface GetRoomsRequest {
  themeId?: number;
  cursor?: number;
  size?: number;
}

export interface GetRoomsResponse {
  success: boolean;
  data: {
    contents: RoomItem[];
    next_cursor: number | null;
    has_next: boolean;
    size: number;
  } | null;
  error: {
    code: string;
    message: string;
  } | null;
}

const mockRooms: RoomItem[] = [
  { room_id: 1, title: '초보만 들어오세요 :(', content_title: '카페에서 주문하기', current_people: 4, max_people: 4, has_password: false, theme_name: '일상회화', status: 'WAITING', created_at: '2024-01-01T00:00:00Z' },
  { room_id: 2, title: '빠 근', content_title: '길 찾기 대화', current_people: 2, max_people: 4, has_password: false, theme_name: '일상회화', status: 'WAITING', created_at: '2024-01-01T00:00:00Z' },
  { room_id: 3, title: '잠수방', content_title: '업무 미팅 인사', current_people: 2, max_people: 4, has_password: true, theme_name: '비즈니스', status: 'WAITING', created_at: '2024-01-01T00:00:00Z' },
  { room_id: 4, title: 'SSAFY 광주 2반', content_title: '팀 프로젝트 회의', current_people: 2, max_people: 4, has_password: true, theme_name: '비즈니스', status: 'WAITING', created_at: '2024-01-01T00:00:00Z' },
  { room_id: 5, title: '아무나 ㄱ', content_title: '시사 뉴스 브리핑', current_people: 4, max_people: 4, has_password: false, theme_name: '뉴스', status: 'WAITING', created_at: '2024-01-01T00:00:00Z' },
  { room_id: 6, title: 'vị trí việt nam', content_title: '경제 동향 파악', current_people: 4, max_people: 4, has_password: false, theme_name: '뉴스', status: 'WAITING', created_at: '2024-01-01T00:00:00Z' },
  { room_id: 7, title: 'ตำแหน่ง củaชาวไทย', content_title: '공항 체크인 안내', current_people: 2, max_people: 4, has_password: false, theme_name: '여행', status: 'WAITING', created_at: '2024-01-01T00:00:00Z' },
  { room_id: 8, title: '아무나 들어와', content_title: '기내 서비스 음성', current_people: 2, max_people: 4, has_password: false, theme_name: '여행', status: 'WAITING', created_at: '2024-01-01T00:00:00Z' },
];

export const getRoomsMock = async (
  params: GetRoomsRequest
): Promise<GetRoomsResponse> => {
  console.log('[API] Mock Get Rooms Requested:', params);
  return new Promise((resolve) => {
    setTimeout(() => {
      let filteredRooms = [...mockRooms];

      if (params.themeId) {
        const themeMap: Record<number, string> = { 1: '생활', 2: '비즈니스', 3: '뉴스', 4: '공공행정' };
        filteredRooms = filteredRooms.filter(room => room.theme_name === themeMap[params.themeId as number]);
      }

      resolve({
        success: true,
        data: {
          contents: filteredRooms,
          next_cursor: null,
          has_next: false,
          size: filteredRooms.length
        },
        error: null,
      });
    }, 300);
  });
};

export const getRoomsReal = async (
  params: GetRoomsRequest
): Promise<GetRoomsResponse> => {
  const response = await axiosInstance.get('/rooms', { params });
  return response.data as GetRoomsResponse;
};

export const getRooms = useMock ? getRoomsMock : getRoomsReal;

// --- Join Room (방 참여) ---
export interface JoinRoomRequest {
  room_id: number;
  password?: string;
}

export interface JoinRoomResponse {
  success: boolean;
  data: {
    room_id: number;
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
  const response = await axiosInstance.post(`/rooms/${payload.room_id}/enter`, {
    password: payload.password,
  });
  return response.data as JoinRoomResponse;
};

export const joinRoom = useMock ? joinRoomMock : joinRoomReal;

console.log(`[RoomsAPI] Initialized. Mode: ${useMock ? 'MOCK' : 'REAL'}`);

