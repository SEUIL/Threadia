/*
 * Support Case(WBS 4.10.1) 타입 — platform 전용 조회/처리 endpoint 기준.
 * 근거: docs/04-api/API-명세서.md §10-A, backend com.threadia.backend.supportcase.dto.*
 */
import type { PlatformAdminRole } from "@/types/platform.types";

export type SupportCaseType = "INQUIRY" | "REPORT";
export type SupportCaseStatus = "OPEN" | "IN_PROGRESS" | "RESOLVED" | "REJECTED" | "CLOSED";
export type SupportCaseTargetType = "USER" | "COMPANY" | "PROJECT" | "CONTENT";

export interface SupportCaseResponse {
  id: number;
  reporterUserId: number;
  type: SupportCaseType;
  status: SupportCaseStatus;
  targetType: SupportCaseTargetType | null;
  targetId: string | null;
  title: string;
  body: string;
  createdAt: string;
  updatedAt: string;
}

export interface SupportCasesData {
  supportCases: SupportCaseResponse[];
}

export interface PublicReplyResponse {
  id: number;
  authorUserId: number;
  authorRole: PlatformAdminRole;
  body: string;
  createdAt: string;
}

export interface InternalNoteResponse {
  id: number;
  authorUserId: number;
  authorRole: PlatformAdminRole;
  body: string;
  createdAt: string;
}

export interface SupportCaseDetailData {
  supportCase: SupportCaseResponse;
  publicReplies: PublicReplyResponse[];
}

export interface InternalNotesData {
  internalNotes: InternalNoteResponse[];
}

export interface PublicReplyRequest {
  body: string;
  reason: string;
}

export interface InternalNoteRequest {
  body: string;
  reason: string;
}

export interface SupportCaseStatusUpdateRequest {
  status: SupportCaseStatus;
  reason: string;
}

/** case별 허용 상태 전이 — backend SupportCaseStatus.canTransitionTo와 1:1. */
export const SUPPORT_CASE_STATUS_TRANSITIONS: Record<SupportCaseStatus, SupportCaseStatus[]> = {
  OPEN: ["IN_PROGRESS", "REJECTED", "CLOSED"],
  IN_PROGRESS: ["RESOLVED", "REJECTED", "CLOSED"],
  RESOLVED: ["CLOSED"],
  REJECTED: ["CLOSED"],
  CLOSED: [],
};
