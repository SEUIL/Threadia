/*
 * 알림(S11) 응답 타입 — backend record와 1:1 매핑.
 * 계약 기준: docs/04-api/API-명세서.md §13.8 (WBS 4.8.1, PR #147).
 *
 * NOTI-02/03(이벤트별/전체 on/off 설정, PROJECT scope)은 이번 slice 범위
 * 밖이다 — backend에 notification-preferences 컨트롤러 자체가 없다
 * (NotificationController에는 list/unread-count/read/read-all만 있다).
 */
export type NotificationType =
  | "ISSUE_ASSIGNED"
  | "ISSUE_STATUS_CHANGED"
  | "THREAD_COMMENTED"
  | "MENTIONED"
  | "DOCUMENT_UPDATED"
  | "DOCUMENT_LINKED_TO_ISSUE"
  | "SPRINT_STARTED"
  | "SPRINT_ENDED"
  | "PROJECT_MESSAGE"
  | "DIRECT_MESSAGE"
  | "MEMBER_ADDED"
  | "ROLE_CHANGED";

export type NotificationTargetType =
  | "ISSUE"
  | "DOCUMENT"
  | "MESSAGE"
  | "THREAD"
  | "DIRECT_MESSAGE_ROOM"
  | "PROJECT"
  | "SPRINT";

export interface NotificationTarget {
  type: NotificationTargetType;
  id: number;
}

export interface NotificationItem {
  notificationRecipientId: number;
  notificationEventId: number;
  type: NotificationType;
  title: string;
  body: string;
  readAt: string | null;
  target: NotificationTarget;
  createdAt: string;
}

export interface NotificationPage {
  cursor: string | null;
  hasNext: boolean;
  size: number;
}

/** GET /notifications — data가 object가 아니라 배열인 응답이라 ApiResponse<T>와 다르다. */
export interface NotificationListResponse {
  data: NotificationItem[];
  page: NotificationPage;
  meta: { requestId: string };
}

export interface NotificationReadData {
  notificationRecipientId: number;
  readAt: string;
}

export interface NotificationReadAllData {
  updatedCount: number;
}

export interface NotificationUnreadCountData {
  unreadCount: number;
}
