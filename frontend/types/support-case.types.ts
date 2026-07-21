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

/**
 * POST /api/v1/support-cases — 일반 사용자용 생성(§10-A.1). INQUIRY는
 * targetType/targetId를 쓰지 않는다. REPORT는 targetType이 필수인데, 이걸
 * 채울 "신고" 진입점(이슈/문서 등 콘텐츠에서 신고하기 액션)이 아직 어디에도
 * 없어서 이번 슬라이스는 INQUIRY 생성만 다룬다 — REPORT는 범위 밖.
 */
export interface SupportCaseCreateRequest {
  type: SupportCaseType;
  title: string;
  body: string;
  targetType?: SupportCaseTargetType | null;
  targetId?: string | null;
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
