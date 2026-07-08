"use client";

/*
 * 화면 RBAC 분기용 role 훅 — projects/backlog/board/project-settings 등
 * A/P/C 3단계 role을 쓰는 화면 공용. WBS 3.2.4.
 *
 * ⚠️ "P"(PROJECT_ADMIN, 프로젝트 레벨 관리자)는 절대 반환하지 않는다.
 * Project Member API(WBS 3.3.4)가 아직 없어 "이 사용자가 이 프로젝트의
 * PM인지"를 알 방법이 없다 — 화면정의서의 회사 레벨 role
 * (companyMemberships)만으로는 표현이 안 되는 정보다. 대신 COMPANY_ADMIN이
 * 아니면 전부 "A"로 보수적으로 처리한다: 실제 PM인 사용자가 이 화면의 관리
 * 기능을 못 쓰는 건 아쉽지만(백엔드가 없어 그 기능 자체도 아직 mock이다),
 * 권한 없는 사용자에게 관리 UI가 잘못 노출되는 것보다는 안전하다.
 * Project Member API가 생기면 이 훅을 프로젝트별 role까지 반환하도록
 * 확장하면 된다.
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
