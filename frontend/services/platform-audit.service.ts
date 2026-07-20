/*
 * Platform Audit 조회 API 클라이언트 — WBS 4.10.4 기준(§10-C.1).
 * cursor는 opaque HMAC 값이라 그대로 다음 요청에 넘기기만 하면 된다.
 */
import apiClient from "@/lib/axios";
import type { ApiResponse } from "@/types/auth.types";
import type { AuditFilters, AuditPageData } from "@/types/audit.types";

export const platformAuditService = {
  list: (filters?: AuditFilters) =>
    apiClient.get<ApiResponse<AuditPageData>>("/api/v1/platform/audits", { params: filters }),
};
