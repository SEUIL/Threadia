import apiClient from "@/lib/axios";
import type { ApiResponse } from "@/types/auth.types";
import type {
  NotificationListResponse,
  NotificationReadAllData,
  NotificationReadData,
  NotificationUnreadCountData,
} from "@/types/notification.types";

export const notificationService = {
  /** data가 배열인 응답이라 ApiResponse<T>로 감싸지 않고 raw shape 그대로 받는다. */
  list: (params?: {
    companyId?: string | number;
    projectId?: string | number;
    type?: string;
    unreadOnly?: boolean;
    cursor?: string;
    size?: number;
  }) => apiClient.get<NotificationListResponse>("/api/v1/notifications", { params }),

  unreadCount: (params?: { companyId?: string | number; projectId?: string | number; type?: string }) =>
    apiClient.get<ApiResponse<NotificationUnreadCountData>>("/api/v1/notifications/unread-count", {
      params,
    }),

  markRead: (notificationRecipientId: string | number) =>
    apiClient.post<ApiResponse<NotificationReadData>>(
      `/api/v1/notifications/${notificationRecipientId}/read`
    ),

  /** 세 필드 모두 선택. {}는 현재 사용자의 모든 unread를 읽음 처리한다. */
  markAllRead: (body?: { type?: string; companyId?: string | number; projectId?: string | number }) =>
    apiClient.post<ApiResponse<NotificationReadAllData>>("/api/v1/notifications/read-all", body ?? {}),
};
