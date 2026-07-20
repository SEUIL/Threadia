/*
 * Platform Company 목록/상태변경 타입 — WBS 4.10.5 후속(Issue #196, PR #203).
 * 근거: docs/04-api/API-명세서.md §10-C.2, backend com.threadia.backend.platform.company.*
 * companies 배열 항목은 기존 CompanyResponse(types/company.types.ts)와 동일하다.
 */
import type { CompanyResponse } from "@/types/company.types";

export type CompanyStatus = "ACTIVE" | "INACTIVE";

export interface PlatformCompanyPageData {
  companies: CompanyResponse[];
  nextCursor: string | null;
  hasNext: boolean;
  size: number;
}

/** GET /api/v1/platform/companies query filter — 전부 optional. */
export interface PlatformCompanyFilters {
  status?: CompanyStatus;
  q?: string;
  cursor?: string;
  size?: number;
}

export interface CompanyStatusUpdateRequest {
  status: CompanyStatus;
  reason: string;
}

export interface CompanyData {
  company: CompanyResponse;
}
