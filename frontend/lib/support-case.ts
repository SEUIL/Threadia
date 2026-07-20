/*
 * Support Case 상태/유형 표시 공용 모듈. 목록(support-cases/page.tsx)과
 * 상세(support-cases/[id]/page.tsx)가 공유한다.
 */
import type { SupportCaseStatus, SupportCaseType } from "@/types/support-case.types";

export const SUPPORT_CASE_STATUS_LABEL: Record<SupportCaseStatus, string> = {
  OPEN: "접수",
  IN_PROGRESS: "처리중",
  RESOLVED: "해결됨",
  REJECTED: "반려됨",
  CLOSED: "종료",
};

export const SUPPORT_CASE_STATUS_CLASS: Record<SupportCaseStatus, string> = {
  OPEN: "bg-warning/15 text-warning",
  IN_PROGRESS: "bg-primary-subtle text-primary",
  RESOLVED: "bg-success/15 text-success",
  REJECTED: "bg-destructive-subtle text-destructive",
  CLOSED: "bg-border text-muted",
};

export const SUPPORT_CASE_TYPE_LABEL: Record<SupportCaseType, string> = {
  INQUIRY: "문의",
  REPORT: "신고",
};
