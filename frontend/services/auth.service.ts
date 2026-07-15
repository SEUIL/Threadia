import apiClient, { setAccessToken } from "@/lib/axios";
import type { ApiResponse, MeData, ProfileUpdateRequest } from "@/types/auth.types";

export const authService = {
  signup: (body: { email: string; password: string; nickname: string; avatarUrl?: string | null }) =>
    apiClient.post("/api/v1/auth/signup", body),

  login: (body: { email: string; password: string }) =>
    apiClient.post("/api/v1/auth/login", body).then((res) => {
      setAccessToken(res.data.data.accessToken);
      return res;
    }),

  logout: () =>
    apiClient.post("/api/v1/auth/logout").then((res) => {
      setAccessToken(null);
      return res;
    }),

  me: () => apiClient.get("/api/v1/auth/me"),

  /*
   * 본인 프로필 부분 수정 (WBS 4.2.1 backend, API-명세서 §13.3).
   * 응답은 /auth/me와 같은 { user } envelope이라 AuthContext에 그대로 넣을 수 있다.
   * body 구성 규칙은 ProfileUpdateRequest 타입 주석 참고(보낼 필드만 담을 것).
   */
  updateProfile: (body: ProfileUpdateRequest) =>
    apiClient.patch<ApiResponse<MeData>>("/api/v1/users/me/profile", body),

  passwordResetRequest: (body: { email: string }) =>
    apiClient.post("/api/v1/auth/password-reset/request", body),

  passwordResetConfirm: (body: { token: string; newPassword: string }) =>
    apiClient.post("/api/v1/auth/password-reset/confirm", body),
};
