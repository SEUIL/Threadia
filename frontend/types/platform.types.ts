/*
 * Platform Admin(WBS 4.10.4 lifecycle, 4.10.1/4.10.3 접근 공통) 타입.
 * 근거: docs/04-api/API-명세서.md §10-A~§10-C, backend
 * com.threadia.backend.platform.lifecycle.* record 1:1 대응.
 */

export type PlatformAdminRole = "PLATFORM_ADMIN" | "SUPER_ADMIN";
export type PlatformAdminStatus = "ACTIVE" | "REVOKED";

export interface PlatformAdminResponse {
  id: number;
  userId: number;
  email: string;
  nickname: string;
  role: PlatformAdminRole;
  status: PlatformAdminStatus;
  createdAt: string;
  revokedAt: string | null;
  reactivatedAt: string | null;
  updatedAt: string;
}

export interface PlatformActorData {
  platformAdmin: PlatformAdminResponse;
}

export interface PlatformAdminsData {
  admins: PlatformAdminResponse[];
}

export interface GrantPlatformAdminRequest {
  userId: number;
  role: PlatformAdminRole;
  reason: string;
}

export interface ChangePlatformAdminRoleRequest {
  role: PlatformAdminRole;
  reason: string;
}

export interface PlatformAdminLifecycleRequest {
  reason: string;
}
