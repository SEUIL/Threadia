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

// GET /auth/me, PATCH /users/me/profile 공통 응답 data (backend MeData와 1:1).
export interface MeData {
  user: UserSummary;
}

/*
 * PATCH /api/v1/users/me/profile 요청 body (WBS 4.2.1 backend 계약).
 * 두 필드 모두 optional이지만 하나 이상은 보내야 한다(빈 객체는 400).
 *  - 생략한 필드는 서버가 기존 값을 보존한다.
 *  - avatarUrl: null 은 avatar를 명시적으로 제거한다(생략과 의미가 다르다).
 * ⚠️ backend가 미지원 필드를 거부하므로(ProfileUpdateRequest의 @JsonAnySetter)
 *    UserSummary를 통째로 spread하면 id/email이 섞여 실패한다 — 보낼 필드만 담는다.
 */
export interface ProfileUpdateRequest {
  nickname?: string;
  avatarUrl?: string | null;
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
