/*
 * Platform Admin 화면 공용 — 에러 메시지 매핑.
 * 기준: docs/04-api/API-명세서.md §10-A.3, §10-B.2, §10-C(409 conflict code 목록).
 */
import { isAxiosError } from "axios";
import type { ApiError } from "@/types/auth.types";

const PLATFORM_ERROR_MESSAGE: Record<string, string> = {
  FORBIDDEN: "플랫폼 관리자 권한이 없습니다.",
  CONFLICT: "이미 처리할 수 없는 상태입니다.",
  SYSTEM_SETTING_VERSION_CONFLICT: "다른 곳에서 먼저 수정됐습니다. 최신 값을 다시 불러와 주세요.",
  SYSTEM_SETTING_NO_CHANGE: "기존 값과 동일해 변경 사항이 없습니다.",
  LAST_ACTIVE_SUPER_ADMIN: "마지막 남은 SUPER_ADMIN은 권한을 회수하거나 강등할 수 없습니다.",
  DUPLICATE_PLATFORM_ADMIN_HISTORY: "이미 관리자 이력이 있는 사용자입니다.",
  PLATFORM_ADMIN_ALREADY_ACTIVE: "이미 활성 상태인 관리자입니다.",
  PLATFORM_ADMIN_REVOKED: "이미 권한이 회수된 관리자입니다.",
  PLATFORM_ADMIN_ROLE_NO_CHANGE: "기존 역할과 동일해 변경 사항이 없습니다.",
};

/** axios 오류에서 공통 에러 body를 꺼내 사용자 메시지로 변환한다. */
export function platformErrorMessage(error: unknown, fallback: string): string {
  if (isAxiosError(error)) {
    if (!error.response) return "서버에 연결할 수 없습니다. 백엔드 실행 상태를 확인해주세요.";
    const body = error.response.data as ApiError | undefined;
    const code = body?.error?.code;
    if (code && PLATFORM_ERROR_MESSAGE[code]) return PLATFORM_ERROR_MESSAGE[code];
    if (body?.error?.message) return body.error.message;
  }
  return fallback;
}

/** 이 화면군의 모든 mutation이 요구하는 reason 최소/최대 길이(§10-A.1 등 공통). */
export const PLATFORM_REASON_MIN = 3;
export const PLATFORM_REASON_MAX = 500;

export function isValidPlatformReason(reason: string): boolean {
  const trimmed = reason.trim();
  return trimmed.length >= PLATFORM_REASON_MIN && trimmed.length <= PLATFORM_REASON_MAX;
}
