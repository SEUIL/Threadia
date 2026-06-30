import apiClient, { setAccessToken } from "@/lib/axios";

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

  passwordResetRequest: (body: { email: string }) =>
    apiClient.post("/api/v1/auth/password-reset/request", body),

  passwordResetConfirm: (body: { token: string; newPassword: string }) =>
    apiClient.post("/api/v1/auth/password-reset/confirm", body),
};
