/*
 * 프로젝트 범위 통합 검색 응답 타입 — backend record와 1:1 매핑.
 * 계약 기준: docs/04-api/API-명세서.md §10.2 (WBS 4.9.4.1, Backend 완료).
 *
 * source별 원본 field나 projectId는 내려오지 않는다 — id/type/title/snippet/
 * sortAt만 공통 shape으로 받는다.
 */
export type SearchResultType = "ISSUE" | "DOCUMENT" | "THREAD" | "PROJECT_MESSAGE";

export interface SearchResult {
  id: number;
  type: SearchResultType;
  title: string;
  snippet: string;
  sortAt: string;
}

/** GET /api/v1/search — (sortAt DESC, type ASC, id DESC) signed keyset cursor. */
export interface SearchData {
  results: SearchResult[];
  nextCursor: string | null;
}
