import apiClient from "@/lib/axios";
import type { ApiResponse } from "@/types/auth.types";
import type {
  ThreadCommentData,
  ThreadCommentsData,
  ThreadData,
  ThreadTargetType,
} from "@/types/thread.types";

export const threadService = {
  /** 대상의 대표 Thread 조회. 아직 없으면 404. */
  getByTarget: (projectId: string | number, targetType: ThreadTargetType, targetId: number) =>
    apiClient.get<ApiResponse<ThreadData>>(`/api/v1/projects/${projectId}/threads`, {
      params: { targetType, targetId },
    }),

  /** get-or-create — 이미 있으면 기존 Thread를 그대로 반환한다(200/201 모두 이 타입). */
  create: (projectId: string | number, body: { targetType: ThreadTargetType; targetId: number; topic?: string }) =>
    apiClient.post<ApiResponse<ThreadData>>(`/api/v1/projects/${projectId}/threads`, body),

  detail: (threadId: string | number) =>
    apiClient.get<ApiResponse<ThreadData>>(`/api/v1/threads/${threadId}`),

  updateTopic: (threadId: string | number, body: { topic: string | null }) =>
    apiClient.patch<ApiResponse<ThreadData>>(`/api/v1/threads/${threadId}`, body),

  listComments: (threadId: string | number) =>
    apiClient.get<ApiResponse<ThreadCommentsData>>(`/api/v1/threads/${threadId}/comments`),

  createComment: (threadId: string | number, body: { content: string }) =>
    apiClient.post<ApiResponse<ThreadCommentData>>(`/api/v1/threads/${threadId}/comments`, body),
};
