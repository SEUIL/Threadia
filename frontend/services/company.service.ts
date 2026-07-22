/*
 * Company API 클라이언트 — auth.service.ts/invitation.service.ts와 동일 패턴.
 * 계약: docs/04-api/API-명세서.md, WBS 3.1.2(Company API)/3.2.1(membership context)
 */
import apiClient from "@/lib/axios";
import type { ApiResponse } from "@/types/auth.types";
import type {
  CompanyCreateData,
  CompanyData,
  CompanyStatsData,
  UserCompanyMembershipsData,
} from "@/types/company.types";

export const companyService = {
  /** 현재 로그인 사용자의 ACTIVE 회사 membership 목록 (PR #60) */
  getMyMemberships: () =>
    apiClient.get<ApiResponse<UserCompanyMembershipsData>>(
      "/api/v1/users/me/company-memberships"
    ),

  get: (companyId: string | number) =>
    apiClient.get<ApiResponse<CompanyData>>(`/api/v1/companies/${companyId}`),

  create: (body: {
    name: string;
    representativeName?: string | null;
    description?: string | null;
    industryCategory?: string | null;
    logoUrl?: string | null;
  }) => apiClient.post<ApiResponse<CompanyCreateData>>("/api/v1/companies", body),

  update: (
    companyId: string | number,
    body: {
      name: string;
      representativeName?: string | null;
      description?: string | null;
      industryCategory?: string | null;
      logoUrl?: string | null;
    }
  ) => apiClient.patch<ApiResponse<CompanyData>>(`/api/v1/companies/${companyId}`, body),

  stats: (companyId: string | number) =>
    apiClient.get<ApiResponse<CompanyStatsData>>(`/api/v1/companies/${companyId}/stats`),

  // 로고 URL 저장(POST /{companyId}/logo)은 WBS 4.11(PR #215) S3 파일 업로드로
  // 대체돼 프론트에서 더 이상 쓰지 않는다 — updateLogo 바인딩을 제거했다.
  // (백엔드 endpoint 자체는 남아있으니 필요하면 다시 붙일 수 있다.)
};
