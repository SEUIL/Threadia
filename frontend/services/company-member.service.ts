/*
 * Company Member API 클라이언트 — WBS 3.1.4(PR #56) 기준.
 *
 * ⚠️ update()는 partial patch가 아니라 full-replace다. role/status가 둘 다
 * 필수(@NotNull)라서 호출마다 세 필드를 항상 같이 보내야 한다. 설계 근거:
 * docs/08-members/member-정철진/기술-연구/sprint2-3.2-company-frontend-rbac-사전조사.md §5
 */
import apiClient from "@/lib/axios";
import type { ApiResponse } from "@/types/auth.types";
import type {
  CompanyMemberData,
  CompanyMemberRemoveData,
  CompanyMemberRole,
  CompanyMemberStatus,
  CompanyMembersData,
} from "@/types/company.types";

export const companyMemberService = {
  list: (
    companyId: string | number,
    filters?: { status?: CompanyMemberStatus; departmentId?: number; q?: string }
  ) =>
    apiClient.get<ApiResponse<CompanyMembersData>>(
      `/api/v1/companies/${companyId}/company-members`,
      { params: filters }
    ),

  /** role/status/departmentId 전체를 항상 같이 보낸다(full-replace). */
  update: (
    companyId: string | number,
    companyMemberId: number,
    body: { role: CompanyMemberRole; status: CompanyMemberStatus; departmentId: number | null }
  ) =>
    apiClient.patch<ApiResponse<CompanyMemberData>>(
      `/api/v1/companies/${companyId}/company-members/${companyMemberId}`,
      body
    ),

  /**
   * soft remove(status=LEFT, departmentId=null) — 같은 회사 내 ACTIVE project
   * membership도 REMOVED로 같이 전환된다. 자기 자신/마지막 ACTIVE COMPANY_ADMIN/
   * 어느 프로젝트의 마지막 ACTIVE PROJECT_ADMIN 제거는 서버가 409로 막는다.
   */
  remove: (companyId: string | number, companyMemberId: number) =>
    apiClient.delete<ApiResponse<CompanyMemberRemoveData>>(
      `/api/v1/companies/${companyId}/company-members/${companyMemberId}`
    ),
};
