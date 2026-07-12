/*
 * Project Member 응답 타입 — backend record와 1:1 매핑.
 * 계약 기준: WBS 3.3.4(PR #70) ProjectMemberResponse/ProjectMembersData/
 * ProjectMemberData/ProjectMemberRemoveData.
 *
 * ⚠️ 멤버 "추가"(초대) API는 이 범위에 없다 — WBS 3.3.4는 조회/역할변경/제거만
 * 다루고, 초대는 WBS 4.3(Project Invitation, Sprint 3)으로 명확히 경계가
 * 나뉘어 있다(PR #70 비고 참고).
 */
import type { UserSummary } from "@/types/auth.types";
import type { ProjectMemberRole, ProjectMemberStatus } from "@/types/project.types";

export interface ProjectMemberResponse {
  id: number;
  projectId: number;
  role: ProjectMemberRole;
  status: ProjectMemberStatus;
  joinedAt: string | null;
  createdAt: string;
  updatedAt: string;
  user: UserSummary;
}

export interface ProjectMembersData {
  projectMembers: ProjectMemberResponse[];
}

export interface ProjectMemberData {
  projectMember: ProjectMemberResponse;
}

export interface ProjectMemberRemoveData {
  removed: boolean;
}
