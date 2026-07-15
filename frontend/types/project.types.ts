/*
 * Project 응답 타입 — backend record와 1:1 매핑.
 * 계약 기준: PR #61(목록/생성), PR #65(상세/수정/archive), docs/04-api/API-명세서.md
 *
 * ⚠️ ProjectResponse에는 memberCount/issueCount/PM 이름이 없다. 화면정의서 S04
 * "주요 데이터"에는 명시돼 있지만 목록 응답이 이 집계를 내려주지 않는다.
 * Project Member API(WBS 3.3.4)와 Issue API(3.5)는 이제 있어서 프로젝트별로
 * 따로 조회하면 계산은 가능하지만, 카드 수만큼 요청이 늘어나는 N+1이라 목록
 * 응답 자체가 집계 필드를 갖기 전까지는 비워둔다. 화면정의서 인용구 메모로
 * gap을 남겨뒀다.
 */
export type ProjectStatus = "PLANNING" | "ACTIVE" | "PAUSED" | "COMPLETED" | "ARCHIVED";
export type ProjectMemberRole = "PROJECT_ADMIN" | "MEMBER";
export type ProjectMemberStatus = "ACTIVE" | "REMOVED";

export interface ProjectResponse {
  id: number;
  companyId: number;
  name: string;
  projectKey: string;
  description: string | null;
  status: ProjectStatus;
  startDate: string | null;
  endDate: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ProjectsData {
  projects: ProjectResponse[];
}

export interface ProjectMemberSummaryResponse {
  id: number;
  role: ProjectMemberRole;
  status: ProjectMemberStatus;
}

export interface ProjectCreateData {
  project: ProjectResponse;
  projectMember: ProjectMemberSummaryResponse;
}

export interface ProjectData {
  project: ProjectResponse;
}

/** GET /projects/{projectId}의 viewer — 이 프로젝트에 대한 내 실제 권한. */
export interface ProjectViewerData {
  companyRole: "COMPANY_ADMIN" | "MEMBER" | null;
  projectRole: ProjectMemberRole | null;
  canManageProject: boolean;
}

export interface ProjectDetailData {
  project: ProjectResponse;
  viewer: ProjectViewerData;
}

export interface ProjectArchiveData {
  projectId: number;
  status: ProjectStatus;
}
