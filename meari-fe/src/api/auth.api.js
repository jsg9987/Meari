import axiosInstance from "./axiosInstance";

// export const login = async ({ email, password }) => {
//   const response = await axiosInstance.post('/auth/login', {
//     email,
//     password,
//   });

//   // response === { success, data, error }
//   return response.data.access_token;
// };

export const loginMock = async ({ email, password }) => {
  return new Promise((resolve, reject) => {
    setTimeout(() => {
      if (email === "user@gmail.com" && password === "1234") {
        // 로그인 요청 성공
        resolve({
          success: true,
          data: {
            access_token: "mock-jwt-access-token-eyJhbGciOi-mock",
          },
          error: null,
        });
        return;
      }

      // 로그인 요청 실패
      reject({
        response: {
          status: 401,
          data: {
            success: false,
            data: null,
            error: {
              code: "AUTH_INVALID_CREDENTIALS",
              message: "이메일 또는 비밀번호가 올바르지 않습니다.",
            },
          },
        },
      });
    }, 700); // 네트워크 지연 흉내
  });
};