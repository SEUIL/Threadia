export interface UserSummary {
  id: number;
  email: string;
  nickname: string;
  avatarUrl: string | null;
}

export interface LoginResponse {
  accessToken: string;
  user: UserSummary;
  companies: CompanyContext[];
}

export interface CompanyContext {
  id: number;
  name: string;
  role: "COMPANY_ADMIN" | "MEMBER";
}

export interface ApiResponse<T> {
  data: T;
  meta: { requestId: string };
}

export interface ApiError {
  error: {
    code: string;
    message: string;
    details?: Record<string, unknown>;
  };
  meta: { requestId: string };
}
