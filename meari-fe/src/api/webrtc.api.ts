import axiosInstance from "./axiosInstance";

export async function getToken(sessionName: string): Promise<string> {
  const res = await axiosInstance.post<{ token: string }>("/api/video/token", {
    sessionName,
  });
  return res.data.token;
}
