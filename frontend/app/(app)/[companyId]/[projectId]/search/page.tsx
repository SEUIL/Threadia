"use client";

/*
 * WBS 4.9.4.2 — 프로젝트 범위 통합 검색 Frontend
 *
 * 선행 Backend(4.9.2.1/4.9.4.1, docs/04-api/API-명세서.md §10.2)가 완료돼
 * 실 API로 바로 연동했다. 화면정의서에 별도 S-ID가 없어 프로젝트 탭
 * navigation(layout.tsx)에 "검색" 탭으로 추가했다.
 *
 * type별 결과 클릭 시 이동:
 *   - DOCUMENT → docs/[docId] 상세
 *   - THREAD → threads/[threadId] 상세
 *   - ISSUE/PROJECT_MESSAGE → 서버 응답에 결과 단건 상세 조회 API가 없어(§10.2
 *     "source별 원본 field나 projectId는 반환하지 않는다") board/messages
 *     목록으로만 이동한다. 단건 딥링크는 API 비범위.
 *
 * type filter, source별 상세 field는 현재 API 범위 밖(§10.2 비범위)이라
 * 구현하지 않는다.
 *
 * React Query 마이그레이션(이슈 #217): PR #191 리뷰(xaphxe) 반영으로 있던
 * `searchGenerationRef` 손짠 stale-response 가드를 없앴다. debounce된 검색어를
 * `useInfiniteQuery`의 queryKey에 그대로 넣으면, 검색어가 바뀌는 순간 React
 * Query가 완전히 다른 캐시 엔트리로 구독을 옮긴다 — 이전 검색어의 loadMore
 * 응답이 늦게 도착해도 그건 옛 queryKey의 캐시만 갱신할 뿐, 화면이 구독 중인
 * 새 queryKey의 데이터와는 애초에 섞일 수가 없다(생성이 아니라 key 자체가
 * 격리 경계가 된다). project 이동(projectId 변경)도 같은 원리로 자동 처리된다
 * (queryKey에 projectId가 들어있다).
 */
import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { useInfiniteQuery } from "@tanstack/react-query";
import { invitationErrorMessage } from "@/lib/invitation";
import { searchService } from "@/services/search.service";
import { INPUT_BASE } from "@/components/ui/inputBase";
import type { SearchResult, SearchResultType } from "@/types/search.types";

const TYPE_LABEL: Record<SearchResultType, string> = {
  ISSUE: "이슈",
  DOCUMENT: "문서",
  THREAD: "쓰레드",
  PROJECT_MESSAGE: "채팅",
};

function resultHref(companyId: string, projectId: string, result: SearchResult): string {
  switch (result.type) {
    case "DOCUMENT":
      return `/${companyId}/${projectId}/docs/${result.id}`;
    case "THREAD":
      return `/${companyId}/${projectId}/threads/${result.id}`;
    case "ISSUE":
      return `/${companyId}/${projectId}/board`;
    case "PROJECT_MESSAGE":
      return `/${companyId}/${projectId}/messages`;
  }
}

export default function ProjectSearchPage() {
  const { companyId, projectId } = useParams<{ companyId: string; projectId: string }>();

  const [query, setQuery] = useState("");
  const [debouncedQuery, setDebouncedQuery] = useState("");

  // query 입력마다 요청하면 서버 부하가 크므로 300ms debounce.
  useEffect(() => {
    const timer = setTimeout(() => setDebouncedQuery(query), 300);
    return () => clearTimeout(timer);
  }, [query]);

  const trimmed = debouncedQuery.trim();

  const searchQuery = useInfiniteQuery({
    queryKey: ["project-search", projectId, trimmed],
    queryFn: async ({ pageParam }) => {
      const { data } = await searchService.search(projectId, { q: trimmed, cursor: pageParam });
      return data.data;
    },
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (lastPage) => lastPage.nextCursor ?? undefined,
    enabled: trimmed !== "",
  });

  const results: SearchResult[] = searchQuery.data?.pages.flatMap((page) => page.results) ?? [];

  const loadMore = () => {
    if (!searchQuery.hasNextPage || searchQuery.isFetchingNextPage) return;
    void searchQuery.fetchNextPage();
  };

  const inputRef = useRef<HTMLInputElement>(null);
  useEffect(() => {
    inputRef.current?.focus();
  }, []);

  return (
    <div className="mx-auto max-w-2xl px-6 py-10">
      <h1 className="text-xl font-bold text-foreground">검색</h1>
      <p className="mt-1 text-sm text-muted">이슈, 문서, 쓰레드, 채팅 메시지를 이 프로젝트 안에서 찾습니다.</p>

      <input
        ref={inputRef}
        type="text"
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        placeholder="검색어를 입력하세요"
        maxLength={100}
        className={INPUT_BASE + " mt-4"}
      />

      {trimmed === "" && (
        <p className="mt-10 text-center text-sm text-placeholder">검색어를 입력해주세요.</p>
      )}

      {trimmed !== "" && searchQuery.isLoading && (
        <p className="mt-6 text-center text-sm text-muted">검색 중...</p>
      )}

      {trimmed !== "" && searchQuery.isError && !searchQuery.data && (
        <div className="mt-6 rounded-lg border border-destructive/30 bg-destructive-subtle px-4 py-6 text-center text-sm text-destructive">
          {invitationErrorMessage(searchQuery.error, "검색 결과를 불러오지 못했습니다.")}
        </div>
      )}

      {searchQuery.isSuccess && (
        <ul className="mt-4 flex flex-col gap-2">
          {results.map((result) => (
            <li key={`${result.type}-${result.id}`}>
              <Link
                href={resultHref(companyId, projectId, result)}
                className="block rounded-xl bg-surface px-4 py-3 shadow-sm transition-shadow duration-fast hover:shadow-md"
              >
                <div className="flex items-center gap-2">
                  <span className="rounded-full bg-border px-2 py-0.5 text-[10px] font-medium text-muted">
                    {TYPE_LABEL[result.type]}
                  </span>
                  <p className="text-sm font-semibold text-foreground">{result.title}</p>
                </div>
                {result.snippet && (
                  <p className="mt-1 line-clamp-2 text-xs text-muted">{result.snippet}</p>
                )}
              </Link>
            </li>
          ))}
          {results.length === 0 && (
            <li className="rounded-xl border border-dashed border-border px-4 py-10 text-center text-sm text-muted">
              검색 결과가 없습니다.
            </li>
          )}
        </ul>
      )}

      {searchQuery.isFetchNextPageError && (
        <p className="mt-4 text-sm text-destructive">
          {invitationErrorMessage(searchQuery.error, "다음 결과를 불러오지 못했습니다.")}
        </p>
      )}

      {searchQuery.isSuccess && searchQuery.hasNextPage && (
        <button
          onClick={loadMore}
          disabled={searchQuery.isFetchingNextPage}
          className="mt-4 w-full rounded-lg border border-border py-2 text-sm font-medium text-muted transition-colors duration-fast hover:text-foreground disabled:opacity-50"
        >
          {searchQuery.isFetchingNextPage ? "불러오는 중..." : "더 보기"}
        </button>
      )}
    </div>
  );
}
