/*
 * Platform Company API 클라이언트 — WBS 4.10.5 후속(Issue #196, PR #203).
 * 둘 다 requireAdmin(PLATFORM_ADMIN/SUPER_ADMIN 공통) — SUPER_ADMIN 전용 제한 없음.
 */
import apiClient from "@/lib/axios";
import type { ApiResponse } from "@/types/auth.types";
import type {
  CompanyData,
  CompanyStatusUpdateRequest,
  PlatformCompanyFilters,
  PlatformCompanyPageData,
} from "@/types/platform-company.types";

export const platformCompanyService = {
  list: (filters?: PlatformCompanyFilters) =>
    apiClient.get<ApiResponse<PlatformCompanyPageData>>("/api/v1/platform/companies", {
      params: filters,
    }),

  updateStatus: (companyId: number, body: CompanyStatusUpdateRequest) =>
    apiClient.patch<ApiResponse<CompanyData>>(
      `/api/v1/platform/companies/${companyId}/status`,
      body
    ),
};
