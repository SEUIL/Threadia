/*
 * Support Case API 클라이언트 — 일반 사용자용 본인 case 생성/조회
 * (docs/04-api/API-명세서.md §10-A). platform 전용 처리 endpoint는
 * platform-support-case.service.ts가 따로 다룬다.
 *
 * 코드 vs API-명세서 감사 중 발견: 이 endpoint들은 이미 완전히 문서화·구현돼
 * 있었는데(인증 ACTIVE 사용자 누구나 호출 가능, platform admin 전용 아님)
 * 프론트에 이 서비스 자체가 없었다 — 관리자용 서비스만 있고 일반 사용자가
 * 문의를 넣을 화면이 아예 없는 상태였다.
 */
import apiClient from "@/lib/axios";
import type { ApiResponse } from "@/types/auth.types";
import type {
  SupportCaseCreateRequest,
  SupportCaseDetailData,
  SupportCasesData,
} from "@/types/support-case.types";

export const supportCaseService = {
  create: (body: SupportCaseCreateRequest) =>
    apiClient.post<ApiResponse<SupportCaseDetailData>>("/api/v1/support-cases", body),

  listOwn: () => apiClient.get<ApiResponse<SupportCasesData>>("/api/v1/support-cases"),

  getOwn: (supportCaseId: number | string) =>
    apiClient.get<ApiResponse<SupportCaseDetailData>>(`/api/v1/support-cases/${supportCaseId}`),
};
