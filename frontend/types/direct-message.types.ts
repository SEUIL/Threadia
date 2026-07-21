/*
 * 1:1 DM 응답 타입 — backend record와 1:1 매핑.
 * 계약 기준: docs/04-api/API-명세서.md §8, §8.1 (WBS 4.7.3.1, PR #104).
 *
 * 다자간 DM, room+participants 일반화, 상대 read receipt는 이번 개발 기간
 * 보류다(§8.1) — 이 타입들은 1:1 room만 다룬다.
 */
import type { UserSummary } from "./auth.types";

export interface DirectMessageRoomResponse {
  id: number;
  companyId: number;
  otherUser: UserSummary;
  unreadCount: number;
  createdAt: string;
}

export interface DirectMessageRoomsData {
  rooms: DirectMessageRoomResponse[];
}

export interface DirectMessageRoomData {
  room: DirectMessageRoomResponse;
}

/** 메시지 목록은 생성 시각/ID 오름차순이다(페이지네이션 없음, §8.1). */
export interface DirectMessageResponse {
  id: number;
  roomId: number;
  sender: UserSummary;
  content: string;
  createdAt: string;
}

export interface DirectMessagesData {
  messages: DirectMessageResponse[];
}

export interface DirectMessageData {
  message: DirectMessageResponse;
}

export interface DirectMessageReadData {
  roomId: number;
  lastReadDirectMessageId: number;
  unreadCount: number;
  readAt: string;
}

/** GET /api/v1/direct-messages/unread-count — 모든 회사에 걸친 합계(PR #206). */
export interface DirectMessageUnreadCountData {
  unreadCount: number;
}

/**
 * GET /api/v1/companies/{companyId}/direct-messages/candidates — 새 DM 시작 후보
 * (이슈 #208, PR #213). ACTIVE 회사 구성원 + ACTIVE user만, self 제외, nickname
 * ASC 정렬로 서버가 이미 필터링해서 준다. 기존 DM room 상대는 포함돼서 오므로
 * 프론트에서 room 상대만 별도로 걸러낸다.
 */
export interface DirectMessageCandidateResponse {
  id: number;
  nickname: string;
  avatarUrl: string | null;
}

export interface DirectMessageCandidatesData {
  candidates: DirectMessageCandidateResponse[];
}
