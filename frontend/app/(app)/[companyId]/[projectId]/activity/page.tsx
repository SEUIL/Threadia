"use client";

/*
 * 프로젝트 활동 피드 (WBS 4.9.1.2) — 화면정의서에 별도 S-ID 없이 "Sprint 3
 * 후보"로만 표기돼 있어(§69, §704) 선행 Backend(4.9.1.1, API-명세서.md §10.1)
 * 계약을 기준으로 구현했다.
 *
 * 이번 slice의 producer는 Issue 생성/상태변경/trash/restore뿐이라(§10.1)
 * 이벤트 종류가 4개뿐이다. actor의 nickname은 응답에 없어(allowlist DTO)
 * "사용자 #id"로만 표시한다 — platform 운영 로그 화면과 같은 표기 방식.
 * offset(page) pagination이라 cursor가 아니라 page를 증가시키며 "더 보기"를
 * 구현한다.
 *
 * React Query 마이그레이션(이슈 #217): PR #205 리뷰(xaphxe) 반영으로 있던
 * `loadSeqRef` 손짠 stale-response 가드를 없앴다. queryKey에 projectId를
 * 넣으면 project 이동 시 React Query가 완전히 다른 캐시로 구독을 옮겨서,
 * 이전 project의 지연 응답이 새 project 화면에 섞일 여지가 애초에 없다
 * (search/page.tsx와 같은 원리 — 자세한 설명은 그쪽 주석 참고).
 */
import Link from "next/link";
import { useParams } from "next/navigation";
import { useInfiniteQuery } from "@tanstack/react-query";
import { activityService } from "@/services/activity.service";
import { invitationErrorMessage } from "@/lib/invitation";
import type { ActivityEventResponse, ActivityEventType } from "@/types/activity.types";

const PAGE_SIZE = 20;

const ISSUE_STATUS_LABEL: Record<string, string> = {
  BACKLOG: "백로그",
  TODO: "할일",
  IN_PROGRESS: "진행중",
  IN_REVIEW: "검토중",
  DONE: "완료",
};

function eventMessage(event: ActivityEventResponse): string {
  const issueKey = event.metadata.issueKey ?? `#${event.targetId}`;
  const title = event.metadata.title ?? "";
  switch (event.eventType) {
    case "ISSUE_CREATED":
      return `이슈 ${issueKey} "${title}"를 생성했습니다.`;
    case "ISSUE_STATUS_CHANGED": {
      const from = ISSUE_STATUS_LABEL[event.metadata.fromStatus ?? ""] ?? event.metadata.fromStatus;
      const to = ISSUE_STATUS_LABEL[event.metadata.toStatus ?? ""] ?? event.metadata.toStatus;
      return `이슈 ${issueKey} 상태를 ${from}에서 ${to}(으)로 변경했습니다.`;
    }
    case "ISSUE_TRASHED":
      return `이슈 ${issueKey}를 휴지통으로 이동했습니다.`;
    case "ISSUE_RESTORED":
      return `이슈 ${issueKey}를 복원했습니다.`;
  }
}

const EVENT_ICON: Record<ActivityEventType, string> = {
  ISSUE_CREATED: "🆕",
  ISSUE_STATUS_CHANGED: "🔄",
  ISSUE_TRASHED: "🗑️",
  ISSUE_RESTORED: "♻️",
};

export default function ProjectActivityPage() {
  const { companyId, projectId } = useParams<{ companyId: string; projectId: string }>();

  const activityQuery = useInfiniteQuery({
    queryKey: ["project-activity", projectId],
    queryFn: async ({ pageParam }) => {
      const { data } = await activityService.list(projectId, { page: pageParam, size: PAGE_SIZE });
      return data.data;
    },
    initialPageParam: 0,
    getNextPageParam: (lastPage, allPages) => {
      const loaded = allPages.reduce((sum, p) => sum + p.events.length, 0);
      return loaded < lastPage.total ? allPages.length : undefined;
    },
  });

  const events: ActivityEventResponse[] = activityQuery.data?.pages.flatMap((p) => p.events) ?? [];
  const total = activityQuery.data?.pages.at(-1)?.total ?? 0;

  const loadMore = () => {
    if (!activityQuery.hasNextPage || activityQuery.isFetchingNextPage) return;
    void activityQuery.fetchNextPage();
  };

  if (activityQuery.isLoading) {
    return <div className="px-6 py-8 text-sm text-muted">활동 내역을 불러오는 중...</div>;
  }

  if (activityQuery.isError && !activityQuery.data) {
    return (
      <div className="mx-auto max-w-3xl px-6 py-10">
        <div className="rounded-lg border border-destructive/30 bg-destructive-subtle px-4 py-6 text-center text-sm text-destructive">
          {invitationErrorMessage(activityQuery.error, "활동 내역을 불러오지 못했습니다.")}
        </div>
      </div>
    );
  }

  const hasMore = events.length < total;

  return (
    <div className="mx-auto max-w-3xl px-6 py-8">
      <h1 className="text-xl font-bold text-foreground">활동 피드</h1>
      <p className="mt-1 text-sm text-muted">이슈 생성·상태변경·휴지통 이동만 기록됩니다.</p>

      <ul className="mt-6 divide-y divide-border rounded-xl border border-border bg-surface">
        {events.map((event) => (
          <li key={event.id} className="flex items-start gap-3 px-4 py-3">
            <span className="text-base leading-none">{EVENT_ICON[event.eventType]}</span>
            <div className="min-w-0 flex-1">
              <Link
                href={`/${companyId}/${projectId}/board`}
                className="block text-sm text-foreground transition-colors duration-fast hover:text-primary"
              >
                {eventMessage(event)}
              </Link>
              <p className="mt-0.5 text-xs text-muted">
                사용자 #{event.actorUserId ?? "알 수 없음"} · {new Date(event.createdAt).toLocaleString("ko-KR")}
              </p>
            </div>
          </li>
        ))}
        {events.length === 0 && (
          <li className="px-4 py-10 text-center text-sm text-muted">아직 활동 내역이 없습니다.</li>
        )}
      </ul>

      {hasMore && (
        <button
          onClick={loadMore}
          disabled={activityQuery.isFetchingNextPage}
          className="mt-3 w-full rounded-md border border-border py-2 text-sm font-medium text-muted transition-colors duration-fast hover:bg-surface disabled:opacity-50"
        >
          {activityQuery.isFetchingNextPage ? "불러오는 중..." : "더 보기"}
        </button>
      )}
      {activityQuery.isFetchNextPageError && (
        <p className="mt-2 text-xs text-destructive">
          {invitationErrorMessage(activityQuery.error, "다음 활동 내역을 불러오지 못했습니다.")}
        </p>
      )}
    </div>
  );
}
