"use client";

/*
 * S06 — 백로그 / 스프린트
 *
 * 접근 사용자: A/P/C. 화면정의서(docs/03-design/화면정의서.md §S06) 기준
 * 스프린트 미배정 이슈(백로그) 목록 + 스프린트별 이슈 목록, 스프린트 생성/
 * 시작/종료, 이슈 배정을 구현한다.
 *
 * 설계 메모 반영:
 *   - 스프린트 생성/시작/종료는 P/C 전용. 프로젝트 상세의 `viewer.canManageProject`
 *     (backend에서 COMPANY_ADMIN || PROJECT_ADMIN으로 계산 = 화면정의서의 "P/C"와
 *     정확히 일치)로 분기한다. 실제 접근 차단은 백엔드가 하고, 여기서는 UI 노출만
 *     맞춘다.
 *   - 스프린트 시작 시 "S11 알림 생성"은 실제 알림 API 연동 전이라 생략.
 *   - 스프린트 종료 시 미완료 이슈(= DONE이 아닌 이슈)는 백로그로 되돌린다.
 *
 * db/schema.sql 기준 sprints.status enum: PLANNED/ACTIVE/COMPLETED.
 *
 * ⚠️ MOCK: 스프린트/이슈 데이터는 아직 로컬 state로 흉내낸다. S05 칸반 보드와
 * 이슈 저장소를 공유하지 않아 여기서 만든 이슈가 보드에 실제로 나타나지는 않는다.
 * 실 API 연동은 WBS 4.4.2(S05/S06 Frontend) 범위다.
 *
 * (2026-07-15, WBS 4.2.2) role만 먼저 정확해졌다. 이전엔 useCurrentCompanyRole을
 * 썼는데 그 훅은 회사 role만 알아서 "P"를 반환하지 못했고, 그 결과 진짜
 * PROJECT_ADMIN이 "A"로 강등돼 스프린트 관리 버튼을 못 봤다. 지금은 S12와 같은
 * viewer 패턴으로 프로젝트 레벨 권한을 정확히 읽는다.
 */
import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { projectService } from "@/services/project.service";
import type { ProjectViewerData } from "@/types/project.types";

type SprintStatus = "PLANNED" | "ACTIVE" | "COMPLETED";
type IssueStatus = "BACKLOG" | "TODO" | "IN_PROGRESS" | "IN_REVIEW" | "DONE";

type BacklogIssue = {
  id: string;
  issueKey: string;
  title: string;
  status: IssueStatus;
  sprintId: string | null;
};

type Sprint = {
  id: string;
  name: string;
  goal: string;
  status: SprintStatus;
  startDate: string | null;
  endDate: string | null;
};

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

const MOCK_SPRINTS: Sprint[] = [
  { id: "s1", name: "Sprint 1 — Auth", goal: "인증 흐름 완성", status: "COMPLETED", startDate: "2026-06-15", endDate: "2026-06-28" },
  { id: "s2", name: "Sprint 2 — Company/Project", goal: "회사·프로젝트 관리 화면", status: "ACTIVE", startDate: "2026-06-29", endDate: "2026-07-12" },
];

const MOCK_ISSUES: BacklogIssue[] = [
  { id: "i1", issueKey: "THR-101", title: "로그인 화면 반응형 깨짐", status: "IN_REVIEW", sprintId: "s2" },
  { id: "i2", issueKey: "THR-102", title: "허브 상단바 알림 뱃지 API 연동", status: "TODO", sprintId: "s2" },
  { id: "i3", issueKey: "THR-103", title: "회사 로고 업로드 S3 연동", status: "BACKLOG", sprintId: null },
  { id: "i4", issueKey: "THR-106", title: "이슈 필터링 UI", status: "BACKLOG", sprintId: null },
  { id: "i5", issueKey: "THR-050", title: "OAuth 로그인 QA", status: "DONE", sprintId: "s1" },
];

export default function BacklogPage() {
  const params = useParams<{ companyId: string; projectId: string }>();

  // 프로젝트 레벨 권한만 이 요청으로 가져온다(스프린트/이슈 데이터는 아직 mock).
  // 실패하면 viewer가 null로 남아 관리 UI가 숨겨진다 — 권한을 과다 노출하는 것보다
  // 안전한 쪽이고, 실제 차단은 어차피 백엔드가 한다.
  const [viewer, setViewer] = useState<ProjectViewerData | null>(null);

  const loadViewer = useCallback(async () => {
    try {
      const { data } = await projectService.detail(params.projectId);
      setViewer(data.data.viewer);
    } catch {
      setViewer(null);
    }
  }, [params.projectId]);

  useEffect(() => {
    (async () => {
      await loadViewer();
    })();
  }, [loadViewer]);

  const canManageSprint = viewer?.canManageProject ?? false;

  const [sprints, setSprints] = useState<Sprint[]>(MOCK_SPRINTS);
  const [issues, setIssues] = useState<BacklogIssue[]>(MOCK_ISSUES);
  const [creatingSprint, setCreatingSprint] = useState(false);
  const [newSprintName, setNewSprintName] = useState("");
  const [newSprintGoal, setNewSprintGoal] = useState("");

  const backlogIssues = issues.filter((i) => i.sprintId === null);

  const assignToSprint = (issueId: string, sprintId: string) => {
    // TODO: POST /api/v1/sprints/{sprintId}/issues
    setIssues((prev) => prev.map((i) => (i.id === issueId ? { ...i, sprintId: sprintId || null } : i)));
  };

  const createSprint = () => {
    const name = newSprintName.trim();
    if (!name) return;
    // TODO: POST /api/v1/projects/{projectId}/sprints
    setSprints((prev) => [
      ...prev,
      { id: crypto.randomUUID(), name, goal: newSprintGoal.trim(), status: "PLANNED", startDate: null, endDate: null },
    ]);
    setNewSprintName("");
    setNewSprintGoal("");
    setCreatingSprint(false);
  };

  const startSprint = (id: string) => {
    // TODO: POST /api/v1/sprints/{sprintId}/start (알림 발송은 백엔드/S11 연동 후)
    setSprints((prev) => prev.map((s) => (s.id === id ? { ...s, status: "ACTIVE" } : s)));
  };

  const completeSprint = (id: string) => {
    // TODO: POST /api/v1/sprints/{sprintId}/complete
    // 미완료(DONE 아닌) 이슈는 백로그로 되돌린다.
    setIssues((prev) =>
        prev.map((i) => (i.sprintId === id && i.status !== "DONE" ? { ...i, sprintId: null } : i))
    );
    setSprints((prev) => prev.map((s) => (s.id === id ? { ...s, status: "COMPLETED" } : s)));
  };

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
                      className="rounded-md px-3 py-1.5 text-sm font-medium text-muted hover:bg-primary-subtle"
                  >
                    취소
                  </button>
                  <button
                      onClick={createSprint}
                      className="rounded-md btn-gradient px-3 py-1.5 text-sm font-semibold text-primary-foreground"
                  >
                    생성
                  </button>
                </div>
              </div>
            </section>
        )}

        {/* 스프린트별 이슈 */}
        <div className="mt-6 flex flex-col gap-4">
          {sprints.map((sprint) => {
            const sprintIssues = issues.filter((i) => i.sprintId === sprint.id);
            return (
                <section key={sprint.id} className="rounded-xl border border-border bg-surface p-5">
                  <div className="flex items-center justify-between">
                    <div>
                      <div className="flex items-center gap-2">
                        <h2 className="text-sm font-semibold text-foreground">{sprint.name}</h2>
                        <span className={`rounded-full px-2 py-0.5 text-[10px] font-medium ${STATUS_CLASS[sprint.status]}`}>
                      {STATUS_LABEL[sprint.status]}
                    </span>
                      </div>
                      {sprint.goal && <p className="mt-0.5 text-xs text-muted">{sprint.goal}</p>}
                    </div>
                    {canManageSprint && sprint.status === "PLANNED" && (
                        <button
                            onClick={() => startSprint(sprint.id)}
                            className="rounded-md btn-gradient px-3 py-1.5 text-xs font-semibold text-primary-foreground"
                        >
                          시작
                        </button>
                    )}
                    {canManageSprint && sprint.status === "ACTIVE" && (
                        <button
                            onClick={() => completeSprint(sprint.id)}
                            className="rounded-md border border-border px-3 py-1.5 text-xs font-medium text-foreground hover:bg-primary-subtle"
                        >
                          종료
                        </button>
                    )}
                  </div>

                  <ul className="mt-3 divide-y divide-border">
                    {sprintIssues.map((issue) => (
                        <li
                            key={issue.id}
                            className="flex items-center justify-between px-1 py-2.5 text-sm"
                        >
                    <span>
                      <span className="mr-2 font-mono text-xs text-muted">{issue.issueKey}</span>
                      {issue.title}
                    </span>
                          {canManageSprint && (
                              <button
                                  onClick={() => assignToSprint(issue.id, "")}
                                  className="text-xs font-medium text-muted transition-colors duration-fast hover:text-primary"
                              >
                                백로그로
                              </button>
                          )}
                        </li>
                    ))}
                    {sprintIssues.length === 0 && (
                        <li className="text-center text-xs text-muted">배정된 이슈가 없습니다.</li>
                    )}
                  </ul>
                </section>
            );
          })}
        </div>

        {/* 백로그 */}
        <section className="mt-6 rounded-xl border border-border bg-surface p-5">
          <h2 className="text-sm font-semibold text-foreground">백로그 ({backlogIssues.length})</h2>
          <ul className="mt-3 divide-y divide-border">
            {backlogIssues.map((issue) => (
                <li
                    key={issue.id}
                    className="flex items-center justify-between px-1 py-2.5 text-sm"
                >
              <span>
                <span className="mr-2 font-mono text-xs text-muted">{issue.issueKey}</span>
                {issue.title}
              </span>
                  {canManageSprint && (
                      <select
                          aria-label={`${issue.issueKey} 스프린트 배정`}
                          value=""
                          onChange={(e) => e.target.value && assignToSprint(issue.id, e.target.value)}
                          className="rounded-md border border-border bg-surface px-2 py-1 text-xs text-foreground focus:border-ring focus:outline-none"
                      >
                        <option value="">스프린트 배정</option>
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
                <li className="text-center text-xs text-muted">백로그가 비어 있습니다.</li>
            )}
          </ul>
        </section>
      </div>
  );
}
