/*
 * 프로젝트 활동 피드 응답 타입 — backend record와 1:1 매핑.
 * 계약 기준: docs/04-api/API-명세서.md §10.1 (WBS 4.9.1.1, Backend 완료).
 *
 * metadata는 allowlist DTO만 온다 — raw metadata_json, reporter 등 다른
 * 사용자 정보는 노출하지 않는다. actor의 nickname도 안 온다(actorUserId만).
 */
export type ActivityEventType =
  | "ISSUE_CREATED"
  | "ISSUE_STATUS_CHANGED"
  | "ISSUE_TRASHED"
  | "ISSUE_RESTORED";

export type ActivityTargetType = "ISSUE";

export interface ActivityEventMetadataResponse {
  issueKey: string | null;
  title: string | null;
  assigneeUserId: number | null;
  fromStatus: string | null;
  toStatus: string | null;
}

export interface ActivityEventResponse {
  id: number;
  eventType: ActivityEventType;
  targetType: ActivityTargetType;
  targetId: number;
  actorUserId: number | null;
  metadata: ActivityEventMetadataResponse;
  createdAt: string;
}

/** offset pagination — page 사이 새 event 삽입/삭제로 인한 중복/누락은 허용된 drift(§10.1). */
export interface ActivityEventPageData {
  events: ActivityEventResponse[];
  page: number;
  size: number;
  total: number;
}
