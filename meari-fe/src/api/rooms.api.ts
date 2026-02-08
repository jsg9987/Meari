import axiosInstance from './axiosInstance';
import { apiConfig } from './apiConfig';
import type { AxiosResponse } from 'axios';
import type { ApiResponse } from './auth.api';

export interface CreateRoomRequest {
  title: string;
  theme_id: number;
  password: string | null;
  max_people: number;
}

export interface CreateRoomData {
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
}

export type CreateRoomResponse = AxiosResponse<ApiResponse<CreateRoomData>>;

export const createRoomMock = async (
  payload: CreateRoomRequest
): Promise<CreateRoomResponse> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({
        data: {
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
        }
      } as CreateRoomResponse);
    }, 600);
  });
};

export const createRoomReal = async (
  payload: CreateRoomRequest
): Promise<CreateRoomResponse> => {
  const useMock = import.meta.env.VITE_USE_MOCK_ROOMS === 'true';
  if (useMock) {
    return await createRoomMock(payload);
  }
  const response = await axiosInstance.post<ApiResponse<CreateRoomData>>('/rooms', payload);
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
  content_id?: number | null;
}

export type RoomDetailResponse = AxiosResponse<ApiResponse<RoomDetailData>>;

export const getRoomDetail = async (roomId: number): Promise<RoomDetailResponse> => {
  const response = await axiosInstance.get<ApiResponse<RoomDetailData>>(`/rooms/${roomId}`);
  return response;
};

export interface EnterRoomRequest {
  password?: string;
}

export interface EnterRoomData {
  message: string;
}

export type EnterRoomResponse = AxiosResponse<ApiResponse<EnterRoomData>>;

// TODO: 방 꽉차면 처리
export const enterRoom = async (
  roomId: number,
  payload: EnterRoomRequest
): Promise<EnterRoomResponse> => {
  const response = await axiosInstance.post<ApiResponse<EnterRoomData>>(`/rooms/${roomId}/enter`, payload);
  return response;
};

export const leaveRoom = async (roomId: number): Promise<void> => {
  await axiosInstance.delete(`/rooms/${roomId}/leave`);
};

export interface WebRTCEnterRequest {
  password?: string;
}

export interface WebRTCEnterData {
  token: string;
  sessionId: string;
}

export type WebRTCEnterResponse = AxiosResponse<ApiResponse<WebRTCEnterData>>;

export const enterWebRTC = async (
  roomId: number,
  payload: WebRTCEnterRequest
): Promise<WebRTCEnterResponse> => {
  const response = await axiosInstance.post<ApiResponse<WebRTCEnterData>>(`/rooms/${roomId}/webrtc/enter`, payload);
  return response;
};

export const leaveWebRTC = async (roomId: number): Promise<void> => {
  await axiosInstance.post(`/rooms/${roomId}/webrtc/leave`);
};

const useMock = apiConfig.shouldMock('ROOMS');

export const createRoom = useMock ? createRoomMock : createRoomReal;

// --- Quick Create Room (빠른 방 생성) ---
export interface QuickCreateRoomRequest {
  theme_id: number;
}

export type QuickCreateRoomResponse = AxiosResponse<ApiResponse<CreateRoomData>>;

export const quickCreateRoom = async (
  payload: QuickCreateRoomRequest
): Promise<QuickCreateRoomResponse> => {
  const response = await axiosInstance.post<ApiResponse<CreateRoomData>>('/rooms/quick', payload);
  return response;
};

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
  thumbnail?: string;
}

export interface GetRoomsRequest {
  themeId?: number;
  cursor?: number;
  size?: number;
}

export interface GetRoomsData {
  contents: RoomItem[];
  next_cursor: number | null;
  has_next: boolean;
  size: number;
}

export type GetRoomsResponse = AxiosResponse<ApiResponse<GetRoomsData>>;

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
  return new Promise((resolve) => {
    setTimeout(() => {
      let filteredRooms = [...mockRooms];

      if (params.themeId) {
        const themeMap: Record<number, string> = { 1: '생활', 2: '비즈니스', 3: '뉴스', 4: '공공행정' };
        filteredRooms = filteredRooms.filter(room => room.theme_name === themeMap[params.themeId as number]);
      }

      resolve({
        data: {
          success: true,
          data: {
            contents: filteredRooms,
            next_cursor: null,
            has_next: false,
            size: filteredRooms.length
          },
          error: null,
        }
      } as GetRoomsResponse);
    }, 300);
  });
};

export const getRoomsReal = async (
  params: GetRoomsRequest
): Promise<GetRoomsResponse> => {
  const response = await axiosInstance.get<ApiResponse<GetRoomsData>>('/rooms', { params });
  return response;
};

export const getRooms = useMock ? getRoomsMock : getRoomsReal;

// --- Join Room (방 참여) ---
export interface JoinRoomRequest {
  room_id: number;
  password?: string;
}

export interface JoinRoomData {
  room_id: number;
}

export type JoinRoomResponse = AxiosResponse<ApiResponse<JoinRoomData>>;

export const joinRoomMock = async (
  payload: JoinRoomRequest
): Promise<JoinRoomResponse> => {
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
        data: {
          success: true,
          data: { room_id: payload.room_id },
          error: null,
        }
      } as JoinRoomResponse);
    }, 300);
  });
};

export const joinRoomReal = async (
  payload: JoinRoomRequest
): Promise<JoinRoomResponse> => {
  const response = await axiosInstance.post<ApiResponse<JoinRoomData>>(`/rooms/${payload.room_id}/enter`, {
    password: payload.password,
  });
  return response;
};

export const joinRoom = useMock ? joinRoomMock : joinRoomReal;

// --- Start Game (게임 시작) ---
export interface StartGameRequest {
  content_id: number;
}

export interface StartGameData {
  message: string;
}

export type StartGameResponse = AxiosResponse<ApiResponse<StartGameData>>;

export const startGame = async (roomId: number, payload: StartGameRequest): Promise<StartGameResponse> => {
  const response = await axiosInstance.post<ApiResponse<StartGameData>>(`/rooms/${roomId}/start`, payload);
  return response;
};

// --- Watching Finish (영상 시청 완료) ---
export interface WatchingFinishRequest {
  content_id: number;
}

export interface WatchingFinishData {
  message: string;
}

export type WatchingFinishResponse = AxiosResponse<ApiResponse<WatchingFinishData>>;

export const finishWatching = async (roomId: number, payload: WatchingFinishRequest): Promise<WatchingFinishResponse> => {
  const response = await axiosInstance.post<ApiResponse<WatchingFinishData>>(`/rooms/${roomId}/watching/finish`, payload);
  return response;
};

// --- Confirm Roles (역할 확정) ---
export interface RoleAssignment {
  member_id: number;
  role_id: number;
}

export interface ConfirmRolesRequest {
  roles: RoleAssignment[];
}

export interface ConfirmRolesData {
  message: string;
}

export type ConfirmRolesResponse = AxiosResponse<ApiResponse<ConfirmRolesData>>;

export const confirmRoles = async (
  roomId: number,
  payload: ConfirmRolesRequest
): Promise<ConfirmRolesResponse> => {
  const response = await axiosInstance.post<ApiResponse<ConfirmRolesData>>(
    `/rooms/${roomId}/roles/confirm`,
    payload
  );
  return response;
};

// --- Start Round (라운드 시작) ---
export interface StartRoundRequest {
  round: number;
}

export interface StartRoundData {
  message: string;
}

export type StartRoundResponse = AxiosResponse<ApiResponse<StartRoundData>>;

export const startRound = async (
  roomId: number,
  payload: StartRoundRequest
): Promise<StartRoundResponse> => {
  const response = await axiosInstance.post<ApiResponse<StartRoundData>>(
    `/rooms/${roomId}/rounds/start`,
    payload
  );
  return response;
};

// --- Finish Round (라운드 종료) ---
export interface FinishRoundData {
  message: string;
}

export type FinishRoundResponse = AxiosResponse<ApiResponse<FinishRoundData>>;

export const finishRound = async (
  roomId: number,
  round: number
): Promise<FinishRoundResponse> => {
  const response = await axiosInstance.post<ApiResponse<FinishRoundData>>(
    `/rooms/${roomId}/rounds/${round}/finish`
  );
  return response;
};

// --- Finish Room (방 종료 - 처음으로 돌아가기) ---
export interface FinishRoomData {
  message: string;
}

export type FinishRoomResponse = AxiosResponse<ApiResponse<FinishRoomData>>;

export const finishRoom = async (roomId: number): Promise<FinishRoomResponse> => {
  const response = await axiosInstance.post<ApiResponse<FinishRoomData>>(
    `/rooms/${roomId}/finish`
  );
  return response;
};

// --- Get Content Video URL (컨텐츠 비디오 URL 조회) ---
export interface ContentVideoUrlData {
  video_url: string;
  expires_in: number;
}

export type ContentVideoUrlResponse = AxiosResponse<ApiResponse<ContentVideoUrlData>>;

export const getContentVideoUrl = async (contentId: number): Promise<ContentVideoUrlResponse> => {
  const response = await axiosInstance.get<ApiResponse<ContentVideoUrlData>>(
    `/s3/contents/${contentId}/video-url`
  );
  return response;
};

// --- Get Presigned URL for Recording Upload (녹음 파일 업로드용 Presigned URL) ---
export interface GetPresignedUrlRequest {
  room_id: number;
  round: number;
  member_id: number;
  sentence_id: number;
}

export interface PresignedUrlData {
  upload_url: string;
  s3_key: string;
  expires_in: number;
}

export type PresignedUrlResponse = AxiosResponse<ApiResponse<PresignedUrlData>>;

export const getPresignedUrl = async (
  payload: GetPresignedUrlRequest
): Promise<PresignedUrlResponse> => {
  const response = await axiosInstance.post<ApiResponse<PresignedUrlData>>(
    '/s3/presigned-url',
    payload
  );
  return response;
};

// --- Upload Recording to S3 (S3에 녹음 파일 업로드) ---
export const uploadRecordingToS3 = async (
  presignedUrl: string,
  audioBlob: Blob
): Promise<void> => {
  await fetch(presignedUrl, {
    method: 'PUT',
    headers: {
      'Content-Type': 'audio/wav',
    },
    body: audioBlob,
  });
};