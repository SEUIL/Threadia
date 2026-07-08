export interface UserSummary {
  id: number;
  email: string;
  nickname: string;
  avatarUrl: string | null;
}

// companies 필드는 backend가 List<Object>(항상 빈 배열)로만 보내는 죽은
// 필드라 타입에서 제외한다. 실제 회사 membership은 companyService.
// getMyMemberships()(GET /users/me/company-memberships)로 별도 조회한다.
export interface LoginResponse {
  accessToken: string;
  user: UserSummary;
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
