"use client";

/*
 * 화면 RBAC 분기용 role 훅 — projects/backlog/board/project-settings 등
 * A/P/C 3단계 role을 쓰는 화면 공용. WBS 3.2.4.
 *
 * ⚠️ "P"(PROJECT_ADMIN, 프로젝트 레벨 관리자)는 절대 반환하지 않는다.
 * 이 훅은 companyId만 알고 projectId는 모르는 라우트(S04 목록 등)에서도
 * 쓰이는데, 프로젝트 레벨 role은 "어느 프로젝트냐"에 따라 달라지는 정보라
 * 애초에 companyId 기준 회사 role(companyMemberships)만으로는 표현이 안
 * 된다. 대신 COMPANY_ADMIN이 아니면 전부 "A"로 보수적으로 처리한다: 권한
 * 없는 사용자에게 관리 UI가 잘못 노출되는 것보다 안전하다는 판단이다.
 *
 * (2026-07-11 갱신) Project Member API(WBS 3.3.4, `GET /projects/{projectId}/
 * project-members`)는 PR #70으로 이미 존재한다 — "API가 없어서"는 더 이상
 * 이 훅의 한계 사유가 아니다. projectId가 있는 라우트(backlog/board 등)는
 * 이제 기술적으로 프로젝트별 실 role을 조회할 수 있다(S12가 이미
 * `viewer.canManageProject`로 그렇게 하고 있다, `project.service.ts` 참고).
 * 다만 이 훅을 그렇게 확장하는 건 WBS 3.4 범위 밖이라 손대지 않았다 — 백엔드
 * 쪽에서 목록 응답에 role 정보를 함께 내려주는 방식으로 갈지, 프론트가
 * project-members를 별도 조회할지 방향이 아직 안 정해졌다. 다음에 이
 * 영역을 다시 볼 때는 이 갱신 내용부터 확인할 것.
 */
import { useParams } from "next/navigation";
import { useAuth } from "@/contexts/AuthContext";

export type ScreenRole = "A" | "P" | "C";

export function useCurrentCompanyRole(): ScreenRole {
  const params = useParams<{ companyId: string }>();
  const { companyMemberships } = useAuth();
  const membership = companyMemberships.find((m) => m.companyId === Number(params.companyId));
  return membership?.role === "COMPANY_ADMIN" ? "C" : "A";
}
