export type ThreadTargetType = "ISSUE" | "DOCUMENT" | "MESSAGE";

/** GET/POST/PATCH /threads 공통 응답 shape (API-명세서 §13.7, WBS 4.7.1.1). */
export interface ThreadResponse {
  id: number;
  projectId: number;
  targetType: ThreadTargetType;
  targetId: number;
  topic: string | null;
  createdByUserId: number;
  createdAt: string;
  updatedAt: string;
}

export interface ThreadData {
  thread: ThreadResponse;
}

/** 생성 순서(id ASC) append-only 댓글. 수정/삭제, 멘션 파싱은 이번 slice 범위 밖이다. */
export interface ThreadCommentResponse {
  id: number;
  threadId: number;
  authorUserId: number;
  content: string;
  createdAt: string;
}

export interface ThreadCommentsData {
  comments: ThreadCommentResponse[];
}

export interface ThreadCommentData {
  comment: ThreadCommentResponse;
}
