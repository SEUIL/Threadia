/*
 * Project Member API 클라이언트 — company-member.service.ts와 동일 패턴.
 * 계약: WBS 3.3.4(PR #70).
 *
 * ⚠️ update()는 partial patch가 아니라 full-replace다. role/status가 둘 다
 * @NotNull이라 호출마다 두 필드를 항상 같이 보내야 한다. 마지막 활성
 * PROJECT_ADMIN 강등/제거는 서버가 409로 막고(최소 1명 유지), REMOVED 멤버를
 * 다시 ACTIVE로 돌리거나 PROJECT_ADMIN으로 승격할 때 대상자가 활성 회사
 * 구성원이 아니면 마찬가지로 409를 던진다.
 */
import apiClient from "@/lib/axios";
import type { ApiResponse } from "@/types/auth.types";
import type {
  ProjectMemberData,
  ProjectMemberRemoveData,
  ProjectMembersData,
} from "@/types/project-member.types";
import type { ProjectMemberRole, ProjectMemberStatus } from "@/types/project.types";

export const projectMemberService = {
  list: (
    projectId: string | number,
    filters?: { role?: ProjectMemberRole; status?: ProjectMemberStatus; q?: string }
  ) =>
    apiClient.get<ApiResponse<ProjectMembersData>>(
      `/api/v1/projects/${projectId}/project-members`,
      { params: filters }
    ),

  /** role/status 전체를 항상 같이 보낸다(full-replace). */
  update: (
    projectMemberId: string | number,
    body: { role: ProjectMemberRole; status: ProjectMemberStatus }
  ) =>
    apiClient.patch<ApiResponse<ProjectMemberData>>(
      `/api/v1/project-members/${projectMemberId}`,
      body
    ),

  remove: (projectMemberId: string | number) =>
    apiClient.delete<ApiResponse<ProjectMemberRemoveData>>(
      `/api/v1/project-members/${projectMemberId}`
    ),
};
