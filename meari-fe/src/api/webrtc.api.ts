import axiosInstance from "./axiosInstance";
import { apiConfig } from './apiConfig';
import type { AxiosResponse } from 'axios';
import type { ApiResponse } from './auth.api';

// 세션 생성
export interface CreateSessionRequest {
  custom_session_id: string;
  room_id: number;
}

export interface CreateSessionData {
  session_id: string;
}

export type CreateSessionResponse = AxiosResponse<ApiResponse<CreateSessionData>>;

export const createSession = async (payload: CreateSessionRequest): Promise<CreateSessionResponse> => {
  const response = await axiosInstance.post<ApiResponse<CreateSessionData>>('/openvidu/sessions', payload);
  return response;
};

// 연결 토큰 생성
export interface CreateConnectionRequest {
  member_id: number;
  nickname: string;
  role_id: number | null;
}

export interface CreateConnectionData {
  session_id: string;
  token: string;
  connection_id: string;
}

export type CreateConnectionResponse = AxiosResponse<ApiResponse<CreateConnectionData>>;

export const createConnection = async (
  sessionId: string,
  payload: CreateConnectionRequest
): Promise<CreateConnectionResponse> => {
  const response = await axiosInstance.post<ApiResponse<CreateConnectionData>>(`/openvidu/sessions/${sessionId}/connections`, payload);
  return response;
};

// 세션 종료
export const deleteSession = async (sessionId: string): Promise<void> => {
  await axiosInstance.delete(`/openvidu/sessions/${sessionId}`);
};
export async function getTokenMock(sessionName: string): Promise<string> {
  console.log('[API] Mock GetToken Requested:', sessionName);
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve(`mock-token-${sessionName}-${Math.random().toString(36).substring(7)}`);
    }, 500);
  });
}

export async function getTokenReal(sessionName: string): Promise<string> {
  const res = await axiosInstance.post<{ token: string }>("/api/video/token", {
    sessionName,
  });
  return res.data.token;
}

const useMock = apiConfig.shouldMock('WEBRTC');

export const getToken = useMock ? getTokenMock : getTokenReal;

console.log(`[WebRTCAPI] Initialized. Mode: ${useMock ? 'MOCK' : 'REAL'}`);
