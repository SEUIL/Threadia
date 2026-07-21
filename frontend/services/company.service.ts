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

  /** 전용 로고 URL 저장(API-명세서 §5) — update()의 full-replace PATCH와 별개 endpoint다. */
  updateLogo: (companyId: string | number, body: { logoUrl: string }) =>
    apiClient.post<ApiResponse<CompanyData>>(`/api/v1/companies/${companyId}/logo`, body),
};
