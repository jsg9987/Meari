import axiosInstance from "./axiosInstance";
import { apiConfig } from './apiConfig';

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
