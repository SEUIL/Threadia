"use client";

/*
 * AuthContext — contexts/AuthContext.tsx
 *
 * 앱 전역 인증 상태의 단일 소스. 로그인 사용자(user)와 세션 상태(status)를
 * 보관하고 login/logout 액션을 제공한다. 모든 (app) 화면은 여기서 실제 사용자
 * 정보를 읽는다(기존 MOCK_USER_NAME 등 상수를 대체).
 *
 * 세션 복원:
 *   access token은 lib/axios.ts에 메모리로만 보관돼 새로고침 시 사라진다.
 *   refresh token은 HttpOnly 쿠키라 JS로 못 읽지만 브라우저가 자동 전송하므로,
 *   앱 로드 시 POST /auth/refresh를 한 번 호출해 accessToken+user를 복원한다.
 *   - 유효한 refresh 쿠키가 있으면 → authenticated
 *   - 없거나 만료됐으면(401) 또는 백엔드가 꺼져있으면(네트워크 오류) → unauthenticated
 *
 * ⚠️ 복원 호출은 apiClient(인터셉터 포함)가 아니라 raw axios로 직접 한다.
 *    apiClient의 401 인터셉터는 refresh 실패 시 /login으로 강제 이동시키는데,
 *    "로그인했나?" 판별 단계에서 그 리다이렉트가 튀면 안 되기 때문이다.
 *
 * ⚠️ role/companyId 미포함: Sprint 1 백엔드는 로그인/refresh 응답에 회사 멤버십을
 *    담지 않는다(companies는 항상 []). 따라서 여기서 role/companyId는 제공하지
 *    못하며, 화면의 역할 분기는 Sprint 2 회사 API 연동 전까지 mock을 유지한다.
 */
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useState,
} from "react";
import axios from "axios";
import { setAccessToken } from "@/lib/axios";
import { authService } from "@/services/auth.service";
import type { UserSummary, CompanyContext, ApiResponse, LoginResponse } from "@/types/auth.types";

const API_BASE = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

type AuthStatus = "loading" | "authenticated" | "unauthenticated";

interface AuthContextValue {
  user: UserSummary | null;
  companies: CompanyContext[];
  status: AuthStatus;
  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<UserSummary | null>(null);
  const [companies, setCompanies] = useState<CompanyContext[]>([]);
  const [status, setStatus] = useState<AuthStatus>("loading");

  // 앱 로드 시 refresh 쿠키로 세션 복원 시도(1회).
  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const { data } = await axios.post<ApiResponse<LoginResponse>>(
          `${API_BASE}/api/v1/auth/refresh`,
          {},
          { withCredentials: true }
        );
        if (cancelled) return;
        setAccessToken(data.data.accessToken);
        setUser(data.data.user);
        setCompanies(data.data.companies);
        setStatus("authenticated");
      } catch {
        // 401(쿠키 없음/만료) 또는 네트워크 오류(백엔드 다운) 모두 미인증으로 처리한다.
        if (cancelled) return;
        setAccessToken(null);
        setStatus("unauthenticated");
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  const login = useCallback(async (email: string, password: string) => {
    // authService.login이 성공 시 setAccessToken까지 처리한다(lib/axios).
    const res = await authService.login({ email, password });
    const data = (res.data as ApiResponse<LoginResponse>).data;
    setUser(data.user);
    setCompanies(data.companies);
    setStatus("authenticated");
  }, []);

  const logout = useCallback(async () => {
    try {
      await authService.logout();
    } finally {
      // 서버 실패와 무관하게 프론트 세션은 반드시 정리한다.
      setUser(null);
      setCompanies([]);
      setStatus("unauthenticated");
    }
  }, []);

  return (
    <AuthContext.Provider value={{ user, companies, status, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth는 AuthProvider 안에서만 사용할 수 있습니다.");
  }
  return ctx;
}
