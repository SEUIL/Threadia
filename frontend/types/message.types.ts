/** 프로젝트 채팅 메시지 단건 조회 응답 (API-명세서 §13.7). S09 Frontend는 별도 WBS(4.7.2.2). */
export interface MessageResponse {
  id: number;
  projectId: number;
  authorUserId: number;
  content: string;
  threadId: number | null;
  createdAt: string;
}

export interface MessageData {
  message: MessageResponse;
}
