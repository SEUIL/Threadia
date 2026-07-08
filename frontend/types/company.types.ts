/*
 * Company/Department/CompanyMember 응답 타입 — backend record와 1:1 매핑.
 * 계약 기준: docs/04-api/API-명세서.md,
 * docs/08-members/member-정철진/기술-연구/sprint2-3.2-company-frontend-rbac-사전조사.md §5, §8
 */
import type { UserSummary } from "@/types/auth.types";

export type CompanyMemberRole = "COMPANY_ADMIN" | "MEMBER";
export type CompanyMemberStatus = "ACTIVE" | "SUSPENDED" | "LEFT";

/** GET /users/me/company-memberships — 응답은 ACTIVE만 온다 */
export interface CompanyMembership {
  companyMemberId: number;
  companyId: number;
  companyName: string;
  role: CompanyMemberRole;
  status: CompanyMemberStatus;
}

export interface UserCompanyMembershipsData {
  companyMemberships: CompanyMembership[];
}

export interface CompanyResponse {
  id: number;
  name: string;
  representativeName: string | null;
  description: string | null;
  industryCategory: string | null;
  logoUrl: string | null;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface CompanyMemberSummaryResponse {
  id: number;
  role: string;
  status: string;
}

export interface CompanyData {
  company: CompanyResponse;
}

export interface CompanyCreateData {
  company: CompanyResponse;
  companyMember: CompanyMemberSummaryResponse;
}

export interface CompanyStatsData {
  companyId: number;
  memberCount: number;
  departmentCount: number;
  projectCount: number;
  projectStatusCounts: Record<string, number>;
}

export interface DepartmentResponse {
  id: number;
  companyId: number;
  name: string;
  createdAt: string;
  updatedAt: string;
}

export interface DepartmentsData {
  departments: DepartmentResponse[];
}

export interface DepartmentData {
  department: DepartmentResponse;
}

export interface DepartmentDeleteData {
  deleted: boolean;
  affectedMemberCount: number;
}

export interface CompanyMemberResponse {
  id: number;
  companyId: number;
  departmentId: number | null;
  role: CompanyMemberRole;
  status: CompanyMemberStatus;
  joinedAt: string | null;
  createdAt: string;
  updatedAt: string;
  user: UserSummary;
}

export interface CompanyMembersData {
  companyMembers: CompanyMemberResponse[];
}

export interface CompanyMemberData {
  companyMember: CompanyMemberResponse;
}
