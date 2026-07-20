/*
 * System Settings(WBS 4.10.3) 타입 — 8개 고정 key catalog.
 * 근거: docs/04-api/API-명세서.md §10-B, backend com.threadia.backend.platform.settings.*
 * value shape는 key별로 다르며 JsonNode로만 받아 화면에서 key별 렌더러가 해석한다.
 */

export type SystemSettingKey =
  | "SUPPORT_CASE_CATEGORIES"
  | "REPORT_REASON_CATEGORIES"
  | "ENFORCEMENT_REASON_PRESETS"
  | "ENFORCEMENT_DURATION_PRESETS"
  | "REPLY_TEMPLATES"
  | "ANNOUNCEMENT_BANNER"
  | "SUPPORT_CENTER_STATUS"
  | "RESULT_NOTIFICATION_POLICY";

export interface SystemSettingResponse {
  id: number;
  key: SystemSettingKey;
  value: unknown;
  valueSchemaVersion: number;
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface SystemSettingsData {
  settings: SystemSettingResponse[];
}

export interface ReplaceSystemSettingRequest {
  value: unknown;
  expectedVersion: number;
  reason: string;
}

export const SYSTEM_SETTING_KEY_LABEL: Record<SystemSettingKey, string> = {
  SUPPORT_CASE_CATEGORIES: "문의 카테고리",
  REPORT_REASON_CATEGORIES: "신고 사유 카테고리",
  ENFORCEMENT_REASON_PRESETS: "제재 사유 프리셋",
  ENFORCEMENT_DURATION_PRESETS: "제재 기간 프리셋",
  REPLY_TEMPLATES: "답변 템플릿",
  ANNOUNCEMENT_BANNER: "공지 배너",
  SUPPORT_CENTER_STATUS: "고객센터 운영 상태",
  RESULT_NOTIFICATION_POLICY: "처리결과 알림 정책",
};
