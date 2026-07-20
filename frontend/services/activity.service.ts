import apiClient from "@/lib/axios";
import type { ApiResponse } from "@/types/auth.types";
import type { ActivityEventPageData } from "@/types/activity.types";

export const activityService = {
  /** page 기본 0, size 기본 20(1~100). offset pagination — cursor 아님(§10.1). */
  list: (projectId: string | number, params?: { page?: number; size?: number }) =>
    apiClient.get<ApiResponse<ActivityEventPageData>>(
      `/api/v1/projects/${projectId}/activity-events`,
      { params }
    ),
};
