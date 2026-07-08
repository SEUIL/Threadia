"use client";

/*
 * S06 — 백로그 / 스프린트
 *
 * 접근 사용자: A/P/C. 화면정의서(docs/03-design/화면정의서.md §S06) 기준
 * 스프린트 미배정 이슈(백로그) 목록 + 스프린트별 이슈 목록, 스프린트 생성/
 * 시작/종료, 이슈 배정을 구현한다.
 *
 * 설계 메모 반영:
 *   - 스프린트 생성/시작/종료는 P/C 전용(useCurrentCompanyRole로 분기, 실제
 *     auth 가드는 아직 없음 — 화면정의서 규칙을 코드에 반영해두는 정도).
 *   - 스프린트 시작 시 "S11 알림 생성"은 실제 알림 API 연동 전이라 생략.
 *   - 스프린트 종료 시 미완료 이슈(= DONE이 아닌 이슈)는 백로그로 되돌린다.
 *
 * db/schema.sql 기준 sprints.status enum: PLANNED/ACTIVE/COMPLETED.
 *
 * ⚠️ MOCK: 스프린트/이슈 API(`GET/POST /projects/{projectId}/sprints`,
 * `/start`, `/complete`, `/issues`)가 아직 없어서 로컬 state로 흉내낸다.
 * S05 칸반 보드와 이슈 저장소를 공유하지 않아 여기서 만든 이슈가 보드에
 * 실제로 나타나지는 않는다(연동은 Sprint 2 백엔드 이후).
 *
 * ⚠️ role만 WBS 3.2.4로 실 연동했다(useCurrentCompanyRole, "P"는 Project
 * Member API 부재로 항상 "A" 취급 — hooks/useCurrentCompanyRole.ts 참고).
 * 스프린트/이슈 데이터 자체는 여전히 mock이다.
 */
import { useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { useCurrentCompanyRole } from "@/hooks/useCurrentCompanyRole";

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
  const role = useCurrentCompanyRole();
  const canManageSprint = role === "P" || role === "C";

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
