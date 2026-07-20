/*
 * Platform Admin Lifecycle API 클라이언트 — WBS 4.10.4 기준.
 * grant/revoke/reactivate/role 변경은 SUPER_ADMIN 전용(backend requireSuperAdminFresh),
 * actor/admins 조회는 PLATFORM_ADMIN/SUPER_ADMIN 공통이다.
 */
import apiClient from "@/lib/axios";
import type { ApiResponse } from "@/types/auth.types";
import type {
  ChangePlatformAdminRoleRequest,
  GrantPlatformAdminRequest,
  PlatformActorData,
  PlatformAdminLifecycleRequest,
  PlatformAdminResponse,
  PlatformAdminsData,
} from "@/types/platform.types";

export const platformActorService = {
  getActor: () => apiClient.get<ApiResponse<PlatformActorData>>("/api/v1/platform/actor"),

  listAdmins: () => apiClient.get<ApiResponse<PlatformAdminsData>>("/api/v1/platform/admins"),

  grant: (body: GrantPlatformAdminRequest) =>
    apiClient.post<ApiResponse<PlatformAdminResponse>>("/api/v1/platform/admins", body),

  revoke: (platformAdminId: number, body: PlatformAdminLifecycleRequest) =>
    apiClient.post<ApiResponse<PlatformAdminResponse>>(
      `/api/v1/platform/admins/${platformAdminId}/revoke`,
      body
    ),

  reactivate: (platformAdminId: number, body: PlatformAdminLifecycleRequest) =>
    apiClient.post<ApiResponse<PlatformAdminResponse>>(
      `/api/v1/platform/admins/${platformAdminId}/reactivate`,
      body
    ),

  changeRole: (platformAdminId: number, body: ChangePlatformAdminRoleRequest) =>
    apiClient.patch<ApiResponse<PlatformAdminResponse>>(
      `/api/v1/platform/admins/${platformAdminId}/role`,
      body
    ),
};
