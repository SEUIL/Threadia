/*
 * 1:1 DM API 클라이언트 — WBS 4.7.3.1 Backend 계약(docs/04-api/API-명세서.md §8, §8.1).
 * unreadCount만 우선 구현했다(RailShell/HubOverlay 배지 하드코딩 fix). 방 목록/
 * 메시지 등 나머지는 PR #173이 별도로 추가하는 중 — 합쳐질 때 add/add 충돌 예상.
 */
import apiClient from "@/lib/axios";
import type { ApiResponse } from "@/types/auth.types";
import type {
  DirectMessageData,
  DirectMessageMarkReadData,
  DirectMessageRoomData,
  DirectMessageRoomsData,
  DirectMessageUnreadCountData,
  DirectMessagesData,
} from "@/types/direct-message.types";

export const directMessageService = {
  listRooms: (companyId: string | number) =>
    apiClient.get<ApiResponse<DirectMessageRoomsData>>(`/api/v1/companies/${companyId}/direct-messages/rooms`),

  createRoom: (companyId: string | number, body: { otherUserId: number }) =>
    apiClient.post<ApiResponse<DirectMessageRoomData>>(`/api/v1/companies/${companyId}/direct-messages/rooms`, body),

  listMessages: (roomId: string | number) =>
    apiClient.get<ApiResponse<DirectMessagesData>>(`/api/v1/direct-messages/rooms/${roomId}/messages`),

  createMessage: (roomId: string | number, body: { content: string }) =>
    apiClient.post<ApiResponse<DirectMessageData>>(`/api/v1/direct-messages/rooms/${roomId}/messages`, body),

  markRead: (roomId: string | number, body: { lastReadDirectMessageId: number }) =>
    apiClient.post<ApiResponse<DirectMessageMarkReadData>>(`/api/v1/direct-messages/rooms/${roomId}/mark-read`, body),

  /** 모든 회사에 걸친 DM room의 unread 합계 — companyId로 필터하지 않는다(backend 계약). */
  unreadCount: () =>
    apiClient.get<ApiResponse<DirectMessageUnreadCountData>>("/api/v1/direct-messages/unread-count"),
};
