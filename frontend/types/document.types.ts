import type { IssueResponse } from "./issue.types";

export type DocumentType = "DOC" | "MEETING_NOTE";

/** GET/POST/PATCH /documents 공통 응답 shape (API-명세서 §13.6). */
export interface DocumentResponse {
  id: number;
  projectId: number;
  authorUserId: number;
  title: string;
  type: DocumentType;
  content: string;
  trashedAt: string | null;
  restoredAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface DocumentsData {
  documents: DocumentResponse[];
}

export interface DocumentData {
  document: DocumentResponse;
}

export type GraphNodeType = "DOCUMENT" | "ISSUE";

export interface GraphNodeResponse {
  id: number;
  type: GraphNodeType;
  title: string;
  issueKey: string | null;
  status: string | null;
}

export interface GraphEdgeResponse {
  linkId: number;
  sourceDocumentId: number;
  targetIssueId: number;
}

export interface GraphData {
  nodes: GraphNodeResponse[];
  edges: GraphEdgeResponse[];
}

export interface DocumentIssueLinkResponse {
  id: number;
  projectId: number;
  documentId: number;
  issueId: number;
  createdByUserId: number;
}

export interface DocumentIssueLinksData {
  links: DocumentIssueLinkResponse[];
}

export interface DocumentIssueLinkData {
  link: DocumentIssueLinkResponse;
}

export interface IssuesOfDocumentData {
  issues: IssueResponse[];
}

export interface DocumentsOfIssueData {
  documents: DocumentResponse[];
}
