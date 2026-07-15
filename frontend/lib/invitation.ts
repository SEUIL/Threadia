/*
 * Invitation 상태 표시·에러 매핑 공용 모듈.
 * 수신 화면(app/(app)/invitations)과 발신 화면([companyId]/members)이 공유한다.
 * 기준: s14-invitation-프론트-연동-설계.md §3(상태), §4(에러 코드)
 */
import { isAxiosError } from "axios";
import type { ApiError } from "@/types/auth.types";
import type { InvitationResponse, InvitationStatus } from "@/types/invitation.types";

export const INVITATION_STATUS_LABEL: Record<InvitationStatus, string> = {
  PENDING: "대기 중",
  ACCEPTED: "수락됨",
  DECLINED: "거절됨",
  CANCELED: "취소됨",
  EXPIRED: "만료됨",
};

export const INVITATION_STATUS_CLASS: Record<InvitationStatus, string> = {
  PENDING: "bg-warning/15 text-warning",
  ACCEPTED: "bg-success/15 text-success",
  DECLINED: "bg-destructive-subtle text-destructive",
  CANCELED: "bg-destructive-subtle text-destructive",
  EXPIRED: "bg-border text-muted",
};

// 409 conflict 코드 → 사용자 안내 문구. 매핑에 없는 코드는 서버 message를 그대로 쓴다.
// 기준: backend InvitationConflictException(회사/프로젝트 초대 공용).
const INVITATION_ERROR_MESSAGE: Record<string, string> = {
  ALREADY_COMPANY_MEMBER: "이미 회사에 소속된 사용자입니다.",
  ALREADY_PROJECT_MEMBER: "이미 프로젝트에 속한 구성원입니다.",
  TARGET_NOT_ACTIVE_COMPANY_MEMBER: "같은 회사의 활성 구성원만 초대할 수 있습니다.",
  INVITATION_SELF_NOT_ALLOWED: "자기 자신은 초대할 수 없습니다.",
  INVITATION_ALREADY_PENDING: "이미 대기 중인 초대가 있습니다.",
  COMPANY_MEMBER_SUSPENDED: "정지된 구성원은 초대 수락으로 복구할 수 없습니다.",
  INVITATION_EXPIRED: "만료된 초대입니다.",
  INVITATION_CANCELED: "이미 취소된 초대입니다.",
  INVITATION_INVALID_STATUS: "처리할 수 없는 초대 상태입니다.",
};

/**
 * axios 오류에서 공통 에러 body를 꺼내 사용자 메시지로 변환한다.
 * invitation 409 코드는 위 매핑을 우선하고, 그 외에는 서버 message → 기본 문구 순.
 */
export function invitationErrorMessage(error: unknown, fallback: string): string {
  if (isAxiosError(error)) {
    if (!error.response) return "서버에 연결할 수 없습니다. 백엔드 실행 상태를 확인해주세요.";
    const body = error.response.data as ApiError | undefined;
    const code = body?.error?.code;
    if (code && INVITATION_ERROR_MESSAGE[code]) return INVITATION_ERROR_MESSAGE[code];
    if (body?.error?.message) return body.error.message;
  }
  return fallback;
}

/**
 * 백엔드 lazy expiration(조회/action 시점) 이후 화면에 머무는 동안 만료된 PENDING을
 * 방어적으로 판정한다. 수락/거절 버튼 비활성화용 — 최종 판정은 백엔드가 한다.
 */
export function isInvitationExpired(invitation: InvitationResponse): boolean {
  return invitation.status === "PENDING" && Date.parse(invitation.expiresAt) < Date.now();
}

/** expiresAt(ISO-8601 UTC)을 로컬 기준 날짜 문자열로 표시한다. */
export function formatExpiresAt(expiresAt: string): string {
  const date = new Date(expiresAt);
  if (Number.isNaN(date.getTime())) return expiresAt;
  return date.toLocaleDateString("ko-KR", { month: "long", day: "numeric" }) + " 까지";
}
