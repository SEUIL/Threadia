import axios from "axios";

const apiClient = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080",
  withCredentials: true, // refresh token HttpOnly cookie 전송
});

let accessToken: string | null = null;

export const setAccessToken = (token: string | null) => {
  accessToken = token;
};

apiClient.interceptors.request.use((config) => {
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config;
    // 이 endpoint들의 401은 "세션 만료"가 아니라 각자의 비즈니스 로직 실패
    // 응답이다(로그인: 자격증명 불일치, 비밀번호 재설정: 토큰 무효/만료) —
    // refresh를 시도하고 실패하면 강제로 /login으로 리다이렉트해버리면, 각
    // 화면이 준비해둔 인라인 오류 메시지를 사용자가 볼 새도 없이 리로드된다
    // (이슈 #236, PR #238에서 로그인만 먼저 고쳤고 같은 원인이던 이 endpoint는
    // 이번에 마저 고친다).
    const ANONYMOUS_AUTH_ENDPOINTS = ["/api/v1/auth/login", "/api/v1/auth/password-reset/confirm"];
    const isAnonymousAuthRequest = ANONYMOUS_AUTH_ENDPOINTS.some((path) => original?.url?.endsWith(path));
    if (error.response?.status === 401 && !original._retry && !isAnonymousAuthRequest) {
      original._retry = true;
      try {
        const { data } = await axios.post(
          `${process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080"}/api/v1/auth/refresh`,
          {},
          { withCredentials: true }
        );
        setAccessToken(data.data.accessToken);
        original.headers.Authorization = `Bearer ${data.data.accessToken}`;
        return apiClient(original);
      } catch {
        setAccessToken(null);
        window.location.href = "/login";
      }
    }
    return Promise.reject(error);
  }
);

export default apiClient;
