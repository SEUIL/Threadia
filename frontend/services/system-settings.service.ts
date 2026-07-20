/*
 * System Settings API 클라이언트 — WBS 4.10.3 기준.
 * PUT은 partial patch가 아니라 whole-document replacement이며 expectedVersion으로
 * optimistic concurrency를 건다(§10-B.2).
 */
import apiClient from "@/lib/axios";
import type { ApiResponse } from "@/types/auth.types";
import type {
  ReplaceSystemSettingRequest,
  SystemSettingKey,
  SystemSettingResponse,
  SystemSettingsData,
} from "@/types/system-settings.types";

export const systemSettingsService = {
  list: () => apiClient.get<ApiResponse<SystemSettingsData>>("/api/v1/platform/system-settings"),

  get: (key: SystemSettingKey) =>
    apiClient.get<ApiResponse<SystemSettingResponse>>(`/api/v1/platform/system-settings/${key}`),

  replace: (key: SystemSettingKey, body: ReplaceSystemSettingRequest) =>
    apiClient.put<ApiResponse<SystemSettingResponse>>(
      `/api/v1/platform/system-settings/${key}`,
      body
    ),
};
