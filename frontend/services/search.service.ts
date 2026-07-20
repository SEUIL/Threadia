import apiClient from "@/lib/axios";
import type { ApiResponse } from "@/types/auth.types";
import type { SearchData } from "@/types/search.types";

export const searchService = {
  /** q는 trim 후 nonblank·최대 100자만 서버가 허용한다. cursor는 이전 응답의 nextCursor만 유효하다. */
  search: (projectId: string | number, params: { q: string; cursor?: string; size?: number }) =>
    apiClient.get<ApiResponse<SearchData>>("/api/v1/search", {
      params: { projectId, ...params },
    }),
};
