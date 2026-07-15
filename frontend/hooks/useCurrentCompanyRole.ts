"use client";

/*
 * 현재 URL의 companyId 기준 **회사 레벨** role 훅. WBS 3.2.4 → 4.2.2에서 정정.
 *
 * 반환값은 "C"(COMPANY_ADMIN) 또는 "A"(그 외) 둘뿐이다. 회사 membership만 보고
 * 판단하므로 프로젝트 레벨 role은 원리적으로 알 수 없다 — companyId만 알고
 * projectId는 모르는 라우트(S04 프로젝트 목록 등)에서도 쓰이기 때문이다.
 *
 * ⚠️ 프로젝트 레벨 권한(P, PROJECT_ADMIN)이 필요하면 이 훅을 쓰지 말고
 * **viewer 패턴**을 쓴다: `projectService.detail(projectId)` 응답의
 * `viewer.canManageProject`(= COMPANY_ADMIN 이거나 PROJECT_ADMIN)나
 * `viewer.projectRole`을 읽으면 된다. S12(project settings), S05(board),
 * S06(backlog)이 이미 이 방식이다.
 *
 * (이력) 원래 이 훅은 타입이 "A" | "P" | "C" 였지만 "P"를 절대 반환하지 못해서
 * 진짜 PROJECT_ADMIN이 "A"로 강등됐다. Project Member API(WBS 3.3.4, PR #70)와
 * 프로젝트 상세의 viewer 계약이 생기면서 프로젝트 레벨 role을 정확히 알 수 있게
 * 됐고, WBS 4.2.2에서 소비자(S06)를 viewer로 옮기고 이 훅의 타입을 실제 동작에
 * 맞게 좁혔다.
 */
import { useParams } from "next/navigation";
import { useAuth } from "@/contexts/AuthContext";

/** 회사 레벨 화면 role. 프로젝트 레벨 "P"는 여기서 표현하지 않는다(위 주석 참고). */
export type CompanyScreenRole = "A" | "C";

export function useCurrentCompanyRole(): CompanyScreenRole {
  const params = useParams<{ companyId: string }>();
  const { companyMemberships } = useAuth();
  const membership = companyMemberships.find((m) => m.companyId === Number(params.companyId));
  return membership?.role === "COMPANY_ADMIN" ? "C" : "A";
}
