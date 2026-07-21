import apiClient from "@/lib/axios";
import type { ApiResponse } from "@/types/auth.types";
import type {
  DirectMessageCandidatesData,
  DirectMessageData,
  DirectMessageReadData,
  DirectMessageRoomData,
  DirectMessageRoomsData,
  DirectMessagesData,
  DirectMessageUnreadCountData,
} from "@/types/direct-message.types";

export const directMessageService = {
  /** 새 DM 시작 후보 목록 — 일반 ACTIVE MEMBER도 호출 가능(이슈 #208, PR #213). */
  listCandidates: (companyId: string | number) =>
    apiClient.get<ApiResponse<DirectMessageCandidatesData>>(
      `/api/v1/companies/${companyId}/direct-messages/candidates`
    ),

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

  /** 모든 회사에 걸친 DM room의 unread 합계 — companyId로 필터하지 않는다(backend 계약, PR #206). */
  unreadCount: () =>
    apiClient.get<ApiResponse<DirectMessageUnreadCountData>>("/api/v1/direct-messages/unread-count"),
};
