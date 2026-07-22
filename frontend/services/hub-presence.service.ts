/*
 * 허브 온라인 상태 API 클라이언트 — POST /api/v1/companies/{companyId}/hub-presence/heartbeat.
 * heartbeat 자체가 그 시점의 온라인 유저 id 목록을 돌려준다(별도 조회 GET 없음).
 */
import apiClient from "@/lib/axios";
import type { ApiResponse } from "@/types/auth.types";
import type { HubPresenceOnlineData } from "@/types/hub-presence.types";

export const hubPresenceService = {
  heartbeat: (companyId: string | number) =>
    apiClient.post<ApiResponse<HubPresenceOnlineData>>(`/api/v1/companies/${companyId}/hub-presence/heartbeat`),
};
