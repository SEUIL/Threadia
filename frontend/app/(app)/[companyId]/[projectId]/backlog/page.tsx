"use client";

/*
 * S06 — 백로그 / 스프린트
 *
 * 접근 사용자: A/P/C. 화면정의서(docs/03-design/화면정의서.md §S06) 기준
 * 스프린트 미배정 이슈(백로그) 목록 + 스프린트별 이슈 목록, 스프린트 생성/
 * 시작/종료, 이슈 배정을 구현한다.
 *
 * WBS 4.4.2 실 연동(PR #99 backend): sprint.service / issue.service 사용.
 * 이전에는 스프린트·이슈가 전부 로컬 state mock이었다.
 *
 * 설계 메모 반영:
 *   - 스프린트 생성/시작/종료/배정은 P/C 전용. 프로젝트 상세의 `viewer.canManageProject`
 *     (backend에서 COMPANY_ADMIN || PROJECT_ADMIN으로 계산 = 화면정의서의 "P/C"와
 *     정확히 일치)로 UI 노출을 맞춘다. 실제 차단은 백엔드가 한다.
 *   - 스프린트 시작 시 "S11 알림 생성"은 실제 알림 API 연동 전이라 생략.
 *
 * 계약에서 화면 설계에 영향을 준 부분(전부 backend 코드로 확인):
 *   - 상태는 PLANNED → ACTIVE → COMPLETED 단방향이고, 프로젝트당 ACTIVE는 하나뿐이다.
 *     그래서 이미 ACTIVE가 있으면 "시작" 버튼을 내지 않는다(서버도 409로 막는다).
 *   - 스프린트 상세는 배정 이슈를 주지 않는다. 이슈는 프로젝트 이슈 목록을 한 번
 *     불러 sprintId로 그룹핑한다(스프린트마다 따로 조회하면 N+1이다).
 *   - 종료 시 서버가 **DONE 포함 모든 배정 이슈의 sprint를 null로** 만들고, DONE도
 *     BACKLOG도 아닌 이슈만 BACKLOG로 되돌린다(원자적). 그래서 프론트에서 이슈를
 *     정리하지 않고 재조회만 하며, 종료된 스프린트는 이슈 목록 대신 summary를 보여준다
 *     — 어차피 배정 이슈가 남지 않기 때문이다.
 *
 * ⚠️ 이슈를 스프린트에서 빼는 API가 없다(배정은 POST /sprints/{id}/issues뿐이고
 * PATCH /issues/{id}의 수정 가능 필드에 sprintId가 없다). 스프린트 종료 말고는
 * 되돌릴 경로가 없어서 mock에 있던 "백로그로" 버튼은 뺐다. 별도 이슈로 올릴 예정.
 */
import { useCallback, useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { projectService } from "@/services/project.service";
import { sprintService } from "@/services/sprint.service";
import { issueService } from "@/services/issue.service";
import { invitationErrorMessage } from "@/lib/invitation";
import type { ProjectViewerData } from "@/types/project.types";
import type { SprintResponse, SprintStatus } from "@/types/sprint.types";
import type { IssueResponse } from "@/types/issue.types";

type LoadState = "loading" | "ready" | "error";

const STATUS_LABEL: Record<SprintStatus, string> = {
  PLANNED: "예정",
  ACTIVE: "진행중",
  COMPLETED: "종료됨",
};

const STATUS_CLASS: Record<SprintStatus, string> = {
  PLANNED: "bg-border text-muted",
  ACTIVE: "bg-success/15 text-success",
  COMPLETED: "bg-accent-subtle text-accent",
};

export default function BacklogPage() {
  const params = useParams<{ companyId: string; projectId: string }>();

  const [viewer, setViewer] = useState<ProjectViewerData | null>(null);
  const [sprints, setSprints] = useState<SprintResponse[]>([]);
  const [issues, setIssues] = useState<IssueResponse[]>([]);
  const [listState, setListState] = useState<LoadState>("loading");
  const [listError, setListError] = useState<string | null>(null);
  // 목록 조회 실패와 별개로 둔다 — 같은 state를 쓰면 action 실패 하나가 렌더 조건을
  // 깨서 목록 전체가 사라진다(S14에서 겪은 회귀, PR #55 리뷰).
  const [actionError, setActionError] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<number | null>(null);

  const [creatingSprint, setCreatingSprint] = useState(false);
  const [newSprintName, setNewSprintName] = useState("");
  const [newSprintGoal, setNewSprintGoal] = useState("");
  const [createBusy, setCreateBusy] = useState(false);

  // 권한만 따로 가져온다. 실패하면 viewer가 null로 남아 관리 UI가 숨겨진다 —
  // 권한을 과다 노출하는 것보다 안전하고, 실제 차단은 어차피 백엔드가 한다.
  const loadViewer = useCallback(async () => {
    try {
      const { data } = await projectService.detail(params.projectId);
      setViewer(data.data.viewer);
    } catch {
      setViewer(null);
    }
  }, [params.projectId]);

  // silent: action 직후 재조회처럼 이미 목록이 떠 있을 때 깜빡임을 막는다.
  const loadBoard = useCallback(
    async (options?: { silent?: boolean }) => {
      if (!options?.silent) setListState("loading");
      setListError(null);
      try {
        const [sprintsRes, issuesRes] = await Promise.all([
          sprintService.list(params.projectId),
          issueService.list(params.projectId),
        ]);
        setSprints(sprintsRes.data.data.sprints);
        setIssues(issuesRes.data.data.issues);
        setListState("ready");
      } catch (error) {
        setListError(invitationErrorMessage(error, "스프린트와 이슈를 불러오지 못했습니다."));
        setListState("error");
      }
    },
    [params.projectId]
  );

  useEffect(() => {
    (async () => {
      await Promise.all([loadViewer(), loadBoard()]);
    })();
  }, [loadViewer, loadBoard]);

  const canManageSprint = viewer?.canManageProject ?? false;

  // 이슈 목록을 한 번만 불러서 나눈다. 서버 기본값이 includeTrashed=false라
  // 휴지통 이슈는 애초에 내려오지 않는다.
  const backlogIssues = useMemo(() => issues.filter((i) => i.sprintId === null), [issues]);
  const issuesBySprint = useMemo(() => {
    const map = new Map<number, IssueResponse[]>();
    for (const issue of issues) {
      if (issue.sprintId === null) continue;
      const list = map.get(issue.sprintId);
      if (list) list.push(issue);
      else map.set(issue.sprintId, [issue]);
    }
    return map;
  }, [issues]);

  // 프로젝트당 ACTIVE는 하나뿐이라, 하나라도 있으면 다른 스프린트는 시작할 수 없다.
  const hasActiveSprint = useMemo(() => sprints.some((s) => s.status === "ACTIVE"), [sprints]);

  const runAction = async (id: number, action: () => Promise<unknown>, fallback: string) => {
    setActionError(null);
    setBusyId(id);
    try {
      await action();
      await loadBoard({ silent: true });
    } catch (error) {
      setActionError(invitationErrorMessage(error, fallback));
    } finally {
      setBusyId(null);
    }
  };

  const createSprint = async () => {
    const name = newSprintName.trim();
    if (!name) return;
    setActionError(null);
    setCreateBusy(true);
    try {
      const goal = newSprintGoal.trim();
      await sprintService.create(params.projectId, { name, goal: goal || null });
      setNewSprintName("");
      setNewSprintGoal("");
      setCreatingSprint(false);
      await loadBoard({ silent: true });
    } catch (error) {
      setActionError(invitationErrorMessage(error, "스프린트 생성에 실패했습니다."));
    } finally {
      setCreateBusy(false);
    }
  };

  const startSprint = (sprint: SprintResponse) =>
    runAction(sprint.id, () => sprintService.start(sprint.id), "스프린트 시작에 실패했습니다.");

  const completeSprint = (sprint: SprintResponse) =>
    // 미완료 이슈 정리는 서버가 원자적으로 한다 — 재조회로 결과만 반영한다.
    runAction(sprint.id, () => sprintService.complete(sprint.id), "스프린트 종료에 실패했습니다.");

  const assignToSprint = (issue: IssueResponse, sprintId: number) =>
    runAction(
      issue.id,
      () => sprintService.assignIssues(sprintId, { issueIds: [issue.id] }),
      "스프린트 배정에 실패했습니다."
    );

  return (
    <div className="mx-auto max-w-3xl px-6 py-10">
      <Link
        href={`/${params.companyId}/projects`}
        className="text-sm text-muted transition-colors duration-fast hover:text-primary"
      >
        ← 프로젝트 목록
      </Link>
      <div className="mt-3 flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-foreground">백로그 / 스프린트</h1>
          <p className="mt-1 text-sm text-muted">프로젝트 {params.projectId}</p>
        </div>
        {canManageSprint && (
          <button
            onClick={() => setCreatingSprint((v) => !v)}
            className="rounded-lg btn-gradient px-4 py-2 text-sm font-semibold text-primary-foreground transition-colors duration-fast"
          >
            + 새 스프린트
          </button>
        )}
      </div>

      {creatingSprint && (
        <section className="mt-4 rounded-xl border border-border bg-surface p-5">
          <div className="flex flex-col gap-2">
            <input
              aria-label="스프린트 이름"
              value={newSprintName}
              onChange={(e) => setNewSprintName(e.target.value)}
              placeholder="스프린트 이름"
              maxLength={150}
              className="rounded-lg border border-border bg-surface px-3 py-2 text-sm text-foreground placeholder:text-placeholder focus:border-ring focus:outline-none"
            />
            <input
              aria-label="스프린트 목표"
              value={newSprintGoal}
              onChange={(e) => setNewSprintGoal(e.target.value)}
              placeholder="목표 (선택)"
              className="rounded-lg border border-border bg-surface px-3 py-2 text-sm text-foreground placeholder:text-placeholder focus:border-ring focus:outline-none"
            />
            <div className="flex justify-end gap-2">
              <button
                onClick={() => setCreatingSprint(false)}
                className="rounded-md px-3 py-1.5 text-sm font-medium text-muted transition-colors duration-fast hover:bg-primary-subtle"
              >
                취소
              </button>
              <button
                onClick={createSprint}
                disabled={createBusy || !newSprintName.trim()}
                className="rounded-md btn-gradient px-4 py-1.5 text-sm font-semibold text-primary-foreground disabled:cursor-not-allowed disabled:opacity-50"
              >
                생성
              </button>
            </div>
          </div>
        </section>
      )}

      {actionError && <p className="mt-4 text-sm text-destructive">{actionError}</p>}

      {listState === "loading" && <p className="mt-6 text-sm text-muted">불러오는 중...</p>}

      {listState === "error" && (
        <div className="mt-6 rounded-xl border border-border bg-surface p-5">
          <p className="text-sm text-destructive">{listError}</p>
          <button
            onClick={() => loadBoard()}
            className="mt-3 rounded-md border border-border px-3 py-1.5 text-xs font-medium text-foreground transition-colors duration-fast hover:bg-primary-subtle"
          >
            다시 시도
          </button>
        </div>
      )}

      {listState === "ready" && (
        <>
          {/* 스프린트별 이슈 */}
          <div className="mt-6 flex flex-col gap-4">
            {sprints.map((sprint) => {
              const sprintIssues = issuesBySprint.get(sprint.id) ?? [];
              const isCompleted = sprint.status === "COMPLETED";
              return (
                <section key={sprint.id} className="rounded-xl border border-border bg-surface p-5">
                  <div className="flex items-center justify-between">
                    <div>
                      <div className="flex items-center gap-2">
                        <h2 className="text-sm font-semibold text-foreground">{sprint.name}</h2>
                        <span
                          className={`rounded-full px-2 py-0.5 text-[10px] font-medium ${STATUS_CLASS[sprint.status]}`}
                        >
                          {STATUS_LABEL[sprint.status]}
                        </span>
                      </div>
                      {sprint.goal && <p className="mt-0.5 text-xs text-muted">{sprint.goal}</p>}
                    </div>
                    {/* 이미 진행중인 스프린트가 있으면 시작 불가(서버도 막는다) */}
                    {canManageSprint && sprint.status === "PLANNED" && !hasActiveSprint && (
                      <button
                        onClick={() => startSprint(sprint)}
                        disabled={busyId === sprint.id}
                        className="rounded-md btn-gradient px-3 py-1.5 text-xs font-semibold text-primary-foreground disabled:cursor-not-allowed disabled:opacity-50"
                      >
                        시작
                      </button>
                    )}
                    {canManageSprint && sprint.status === "ACTIVE" && (
                      <button
                        onClick={() => completeSprint(sprint)}
                        disabled={busyId === sprint.id}
                        className="rounded-md border border-border px-3 py-1.5 text-xs font-medium text-foreground transition-colors duration-fast hover:bg-primary-subtle disabled:cursor-not-allowed disabled:opacity-50"
                      >
                        종료
                      </button>
                    )}
                  </div>

                  {/* 종료된 스프린트는 배정 이슈가 남지 않는다(서버가 전부 clear) — 회고 요약만 보여준다 */}
                  {isCompleted ? (
                    <p className="mt-3 text-xs text-muted">
                      {sprint.summary
                        ? `회고: ${sprint.summary}`
                        : "종료된 스프린트입니다. 배정됐던 이슈는 백로그로 돌아갔습니다."}
                    </p>
                  ) : (
                    <ul className="mt-3 divide-y divide-border">
                      {sprintIssues.map((issue) => (
                        <li key={issue.id} className="flex items-center justify-between px-1 py-2.5 text-sm">
                          <span>
                            <span className="mr-2 font-mono text-xs text-muted">{issue.issueKey}</span>
                            {issue.title}
                          </span>
                          <span className="text-xs text-muted">{issue.status}</span>
                        </li>
                      ))}
                      {sprintIssues.length === 0 && (
                        <li className="py-2.5 text-center text-xs text-muted">배정된 이슈가 없습니다.</li>
                      )}
                    </ul>
                  )}
                </section>
              );
            })}
            {sprints.length === 0 && (
              <p className="rounded-xl border border-border bg-surface p-5 text-center text-xs text-muted">
                아직 스프린트가 없습니다.
              </p>
            )}
          </div>

          {/* 백로그 = sprintId가 없는 이슈 */}
          <section className="mt-6 rounded-xl border border-border bg-surface p-5">
            <h2 className="text-sm font-semibold text-foreground">백로그 ({backlogIssues.length})</h2>
            <ul className="mt-3 divide-y divide-border">
              {backlogIssues.map((issue) => (
                <li key={issue.id} className="flex items-center justify-between px-1 py-2.5 text-sm">
                  <span>
                    <span className="mr-2 font-mono text-xs text-muted">{issue.issueKey}</span>
                    {issue.title}
                  </span>
                  {canManageSprint && (
                    <select
                      aria-label={`${issue.issueKey} 스프린트 배정`}
                      value=""
                      disabled={busyId === issue.id}
                      onChange={(e) => e.target.value && assignToSprint(issue, Number(e.target.value))}
                      className="rounded-md border border-border bg-surface px-2 py-1 text-xs text-foreground focus:border-ring focus:outline-none disabled:cursor-not-allowed"
                    >
                      <option value="">스프린트 배정</option>
                      {/* COMPLETED 스프린트에는 배정할 수 없다(서버 409) */}
                      {sprints
                        .filter((s) => s.status !== "COMPLETED")
                        .map((s) => (
                          <option key={s.id} value={s.id}>
                            {s.name}
                          </option>
                        ))}
                    </select>
                  )}
                </li>
              ))}
              {backlogIssues.length === 0 && (
                <li className="py-2.5 text-center text-xs text-muted">백로그가 비어 있습니다.</li>
              )}
            </ul>
          </section>
        </>
      )}
    </div>
  );
}
