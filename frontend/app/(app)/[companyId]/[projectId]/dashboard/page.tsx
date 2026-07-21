"use client";

/*
 * 프로젝트 홈/대시보드 (WBS 4.9.3.2) — 화면정의서에 별도 S-ID 없이
 * "Sprint 3 후보"로만 표기돼 있어(docs/03-design/화면정의서.md L68,706)
 * 선행 Backend(4.9.3.1, API-명세서.md §10.3) 계약을 기준으로 구현했다.
 *
 * GET /projects/{projectId}/dashboard는 live-read 집계라 progress/
 * recentIssues/recentDocuments/sprintSummary 4개만 온다 — activity feed는
 * WBS 4.9.1 deferred 범위라 없다. pagination도 없어서(§10.3 비범위) "더 보기"
 * 없이 최근 10개만 그대로 보여준다.
 */
import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { invitationErrorMessage } from "@/lib/invitation";
import { projectService } from "@/services/project.service";
import type {
  ProjectDashboardData,
  ProjectDashboardRecentDocument,
  ProjectDashboardRecentIssue,
} from "@/types/project.types";

type LoadState = "loading" | "ready" | "error";

const ISSUE_STATUS_LABEL: Record<string, string> = {
  BACKLOG: "백로그",
  TODO: "할일",
  IN_PROGRESS: "진행중",
  IN_REVIEW: "검토중",
  DONE: "완료",
};

const DOCUMENT_TYPE_LABEL: Record<string, string> = {
  DOC: "문서",
  MEETING_NOTE: "회의록",
};

function formatDate(value: string) {
  return new Date(value).toLocaleDateString("ko-KR");
}

export default function ProjectDashboardPage() {
  const { companyId, projectId } = useParams<{ companyId: string; projectId: string }>();

  const [data, setData] = useState<ProjectDashboardData | null>(null);
  const [state, setState] = useState<LoadState>("loading");
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setState("loading");
    setError(null);
    try {
      const { data } = await projectService.dashboard(projectId);
      setData(data.data);
      setState("ready");
    } catch (err) {
      setError(invitationErrorMessage(err, "대시보드를 불러오지 못했습니다."));
      setState("error");
    }
  }, [projectId]);

  useEffect(() => {
    (async () => {
      await load();
    })();
  }, [load]);

  if (state === "loading") {
    return <div className="px-6 py-8 text-sm text-muted">대시보드를 불러오는 중...</div>;
  }

  if (state === "error" || !data) {
    return (
      <div className="mx-auto max-w-4xl px-6 py-10">
        <div className="rounded-lg border border-destructive/30 bg-destructive-subtle px-4 py-6 text-center text-sm text-destructive">
          {error}
        </div>
      </div>
    );
  }

  const { progress, recentIssues, recentDocuments, sprintSummary } = data;

  return (
    <div className="mx-auto max-w-4xl px-6 py-8">
      <h1 className="text-xl font-bold text-foreground">프로젝트 홈</h1>

      <div className="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-2">
        {/* 진행률 */}
        <section className="rounded-xl border border-border bg-surface p-5">
          <h2 className="text-sm font-semibold text-foreground">이슈 진행률</h2>
          <p className="mt-2 text-3xl font-bold text-foreground">{progress.percentage}%</p>
          <p className="mt-1 text-xs text-muted">
            전체 {progress.totalIssueCount}개 중 완료 {progress.doneIssueCount}개
          </p>
          <div className="mt-3 h-2 w-full overflow-hidden rounded-full bg-border">
            <div
              className="h-full rounded-full bg-primary transition-all duration-fast"
              style={{ width: `${progress.percentage}%` }}
            />
          </div>
        </section>

        {/* 진행중 스프린트 */}
        <section className="rounded-xl border border-border bg-surface p-5">
          <h2 className="text-sm font-semibold text-foreground">진행중 스프린트</h2>
          {sprintSummary ? (
            <>
              <p className="mt-2 text-sm font-medium text-foreground">{sprintSummary.name}</p>
              {sprintSummary.goal && <p className="mt-1 text-xs text-muted">{sprintSummary.goal}</p>}
              <p className="mt-2 text-xs text-muted">
                {formatDate(sprintSummary.startDate)} ~ {formatDate(sprintSummary.endDate)}
              </p>
              <p className="mt-1 text-xs text-muted">
                이슈 {sprintSummary.doneIssueCount}/{sprintSummary.issueCount}개 완료
              </p>
            </>
          ) : (
            <p className="mt-3 text-xs text-muted">진행중인 스프린트가 없습니다.</p>
          )}
        </section>
      </div>

      <div className="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2">
        {/* 최근 이슈 */}
        <section className="rounded-xl border border-border bg-surface p-5">
          <h2 className="text-sm font-semibold text-foreground">최근 이슈</h2>
          <ul className="mt-3 divide-y divide-border">
            {recentIssues.map((issue: ProjectDashboardRecentIssue) => (
              <li key={issue.id} className="py-2">
                <Link
                  href={`/${companyId}/${projectId}/board`}
                  className="block transition-colors duration-fast hover:text-primary"
                >
                  <div className="flex items-center gap-2">
                    <span className="rounded-full bg-border px-2 py-0.5 text-[10px] font-medium text-muted">
                      {ISSUE_STATUS_LABEL[issue.status] ?? issue.status}
                    </span>
                    <span className="truncate text-sm text-foreground">{issue.title}</span>
                  </div>
                  <p className="mt-0.5 text-xs text-muted">
                    {issue.issueKey} · {formatDate(issue.updatedAt)}
                  </p>
                </Link>
              </li>
            ))}
            {recentIssues.length === 0 && (
              <li className="py-4 text-center text-xs text-muted">최근 이슈가 없습니다.</li>
            )}
          </ul>
        </section>

        {/* 최근 문서 */}
        <section className="rounded-xl border border-border bg-surface p-5">
          <h2 className="text-sm font-semibold text-foreground">최근 문서</h2>
          <ul className="mt-3 divide-y divide-border">
            {recentDocuments.map((doc: ProjectDashboardRecentDocument) => (
              <li key={doc.id} className="py-2">
                <Link
                  href={`/${companyId}/${projectId}/docs/${doc.id}`}
                  className="block transition-colors duration-fast hover:text-primary"
                >
                  <div className="flex items-center gap-2">
                    <span className="rounded-full bg-border px-2 py-0.5 text-[10px] font-medium text-muted">
                      {DOCUMENT_TYPE_LABEL[doc.type] ?? doc.type}
                    </span>
                    <span className="truncate text-sm text-foreground">{doc.title}</span>
                  </div>
                  <p className="mt-0.5 text-xs text-muted">{formatDate(doc.updatedAt)}</p>
                </Link>
              </li>
            ))}
            {recentDocuments.length === 0 && (
              <li className="py-4 text-center text-xs text-muted">최근 문서가 없습니다.</li>
            )}
          </ul>
        </section>
      </div>
    </div>
  );
}
