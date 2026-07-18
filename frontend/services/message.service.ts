import apiClient from "@/lib/axios";
import type { ApiResponse } from "@/types/auth.types";
import type { MarkReadData, MessageData, MessagesData, UnreadCountData } from "@/types/message.types";

export const messageService = {
  /** 최신순(createdAt DESC, id DESC) keyset. cursor는 이전 응답의 nextCursor만 유효하다. */
  list: (projectId: string | number, params?: { cursor?: string; limit?: number }) =>
    apiClient.get<ApiResponse<MessagesData>>(`/api/v1/projects/${projectId}/messages`, { params }),

  /** threadId는 항상 null로 보낸다 — Thread 연동은 별도 WBS(4.7.1.2). */
  create: (projectId: string | number, body: { content: string }) =>
    apiClient.post<ApiResponse<MessageData>>(`/api/v1/projects/${projectId}/messages`, {
      ...body,
      threadId: null,
    }),

  detail: (messageId: string | number) =>
    apiClient.get<ApiResponse<MessageData>>(`/api/v1/messages/${messageId}`),

  markRead: (projectId: string | number, body: { lastReadMessageId: number }) =>
    apiClient.post<ApiResponse<MarkReadData>>(`/api/v1/projects/${projectId}/messages/mark-read`, body),

  unreadCount: (projectId: string | number) =>
    apiClient.get<ApiResponse<UnreadCountData>>(`/api/v1/projects/${projectId}/messages/unread-count`),
};
