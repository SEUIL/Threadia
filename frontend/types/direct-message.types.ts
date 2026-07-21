import type { UserSummary } from "@/types/auth.types";

/** DirectMessageController DTO와 1:1 매핑한다. */
export interface DirectMessageRoomResponse {
  id: number;
  companyId: number;
  otherUser: UserSummary;
  unreadCount: number;
  createdAt: string;
}

export interface DirectMessageResponse {
  id: number;
  roomId: number;
  sender: UserSummary;
  content: string;
  createdAt: string;
}

export interface DirectMessageRoomsData {
  rooms: DirectMessageRoomResponse[];
}

export interface DirectMessageRoomData {
  room: DirectMessageRoomResponse;
}

export interface DirectMessagesData {
  messages: DirectMessageResponse[];
}

export interface DirectMessageData {
  message: DirectMessageResponse;
}

export interface DirectMessageMarkReadData {
  roomId: number;
  lastReadDirectMessageId: number | null;
  unreadCount: number;
  readAt: string;
}

export interface DirectMessageUnreadCountData {
  unreadCount: number;
}
