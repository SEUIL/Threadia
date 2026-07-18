/*
 * Project Chat 메시지 응답 타입 — backend record와 1:1 매핑.
 * 계약 기준: docs/04-api/API-명세서.md §13.7 (WBS 4.7.2.1, PR #103).
 *
 * threadId는 WBS 4.7.1 완료 전에는 항상 null이고, non-null 요청은
 * 409 THREAD_DEFERRED다(§13.7 WBS 4.7.2.1 정책). Thread Frontend(4.7.1.2,
 * PR #170)가 머지된 뒤에도 프로젝트 채팅 메시지 작성 UI는 아직 thread 연동을
 * 하지 않아 threadId는 계속 null로 보낸다 — Thread 컨텍스트 배너(S10)는
 * 이 타입의 detail() 조회만 공유해서 쓴다.
 */
export interface MessageResponse {
  id: number;
  projectId: number;
  authorUserId: number;
  content: string;
  threadId: number | null;
  createdAt: string;
}

/** GET /projects/{projectId}/messages — createdAt DESC, id DESC keyset. */
export interface MessagesData {
  messages: MessageResponse[];
  nextCursor: string | null;
}

export interface MessageData {
  message: MessageResponse;
}

export interface MarkReadData {
  projectId: number;
  lastReadMessageId: number;
  unreadCount: number;
}

export interface UnreadCountData {
  projectId: number;
  unreadCount: number;
}
