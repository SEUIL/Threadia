import apiClient from "@/lib/axios";
import type { ApiResponse } from "@/types/auth.types";
import type {
  DirectMessageData,
  DirectMessageReadData,
  DirectMessageRoomData,
  DirectMessageRoomsData,
  DirectMessagesData,
  DirectMessageUnreadCountData,
} from "@/types/direct-message.types";

export const directMessageService = {
  listRooms: (companyId: string | number) =>
    apiClient.get<ApiResponse<DirectMessageRoomsData>>(
      `/api/v1/companies/${companyId}/direct-messages/rooms`
    ),

  /** get-or-create — 이미 있으면 기존 normalized pair room을 그대로 반환한다. */
  getOrCreateRoom: (companyId: string | number, body: { otherUserId: number }) =>
    apiClient.post<ApiResponse<DirectMessageRoomData>>(
      `/api/v1/companies/${companyId}/direct-messages/rooms`,
      body
    ),

  listMessages: (roomId: string | number) =>
    apiClient.get<ApiResponse<DirectMessagesData>>(`/api/v1/direct-messages/rooms/${roomId}/messages`),

  send: (roomId: string | number, body: { content: string }) =>
    apiClient.post<ApiResponse<DirectMessageData>>(
      `/api/v1/direct-messages/rooms/${roomId}/messages`,
      body
    ),

  markRead: (roomId: string | number, body: { lastReadDirectMessageId: number }) =>
    apiClient.post<ApiResponse<DirectMessageReadData>>(
      `/api/v1/direct-messages/rooms/${roomId}/mark-read`,
      body
    ),

  unreadCount: () =>
    apiClient.get<ApiResponse<DirectMessageUnreadCountData>>("/api/v1/direct-messages/unread-count"),
};
