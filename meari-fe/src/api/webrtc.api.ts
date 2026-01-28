import axiosInstance from "./axiosInstance";

// 세션 생성
export interface CreateSessionRequest {
  custom_session_id: string;
  room_id: number;
}

export interface CreateSessionResponse {
  success: boolean;
  data: {
    session_id: string;
  } | null;
  error: {
    code: string;
    message: string;
  } | null;
}

export const createSession = async (payload: CreateSessionRequest) => {
  const response = await axiosInstance.post('/openvidu/sessions', payload);
  return response;
};

// 연결 토큰 생성
export interface CreateConnectionRequest {
  member_id: number;
  nickname: string;
  role_id: number | null;
}

export interface CreateConnectionResponse {
  success: boolean;
  data: {
    session_id: string;
    token: string;
    connection_id: string;
  } | null;
  error: {
    code: string;
    message: string;
  } | null;
}

export const createConnection = async (
  sessionId: string,
  payload: CreateConnectionRequest
) => {
  const response = await axiosInstance.post(`/openvidu/sessions/${sessionId}/connections`, payload);
  return response;
};

// 세션 종료
export const deleteSession = async (sessionId: string): Promise<void> => {
  await axiosInstance.delete(`/openvidu/sessions/${sessionId}`);
};
