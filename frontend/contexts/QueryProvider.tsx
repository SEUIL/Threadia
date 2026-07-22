"use client";

/*
 * QueryProvider — contexts/QueryProvider.tsx
 *
 * React Query(TanStack Query) 도입(이슈 #217, PR #173 4차 재리뷰 사후 논의).
 * DM 화면(dm/layout.tsx)에 손으로 짠 동시성 가드(sequenceRef,
 * latestLoadSequenceRef, readGenerationsRef, startDmSequenceRef,
 * pickerRequestSequenceRef, mountedRef, pathnameRef)가 7개나 쌓였는데, 매번
 * "이 응답이 아직 유효한가"를 직접 판정하는 로직을 재발명한 결과였다. React
 * Query는 요청 최신성 판정, 자동 취소, 캐시 무효화, dedup을 라이브러리
 * 차원에서 제공해서 이 가드 대부분을 없앨 수 있다.
 *
 * QueryClient는 컴포넌트당 한 번만 만들어야 한다(요청마다 새로 만들면 캐시가
 * 매번 리셋된다) — useState(() => new QueryClient())로 최초 렌더에서만
 * 생성한다(공식 Next.js App Router 가이드 패턴). RootLayout은 서버
 * 컴포넌트라 QueryClientProvider를 직접 못 쓰므로 이 클라이언트 컴포넌트로
 * 감싼다.
 *
 * 기본 옵션:
 *   - staleTime 10초: 대부분의 화면 데이터가 "약간 오래된 값 허용"이면
 *     충분하고, 0으로 두면 화면 전환마다 불필요한 재조회가 生긴다.
 *   - refetchOnWindowFocus: true(기본값 유지) — 배지/읽지 않은 개수처럼
 *     "다른 탭 갔다 오면 최신값 보고 싶은" 데이터가 많아서 기본 동작이 이
 *     프로젝트에 맞는다.
 */
import { useState } from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";

export function QueryProvider({ children }: { children: React.ReactNode }) {
  const [client] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            staleTime: 10_000,
            retry: 1,
          },
        },
      })
  );

  return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
}
