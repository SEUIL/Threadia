"use client";

/*
 * S04 — 프로젝트 목록
 *
 * 접근 사용자: A/P/C. 화면정의서(docs/03-design/화면정의서.md §S04) 기준
 * 프로젝트 카드 목록 + 새 프로젝트 생성(C만) + 카드 클릭 시 그래프 뷰로 이동.
 *
 * 설계 메모 반영:
 *   - A는 "+ 새 프로젝트" 버튼을 보지 않는다.
 *   - C는 회사 전체 프로젝트, A/P는 소속 프로젝트만 보는 게 원칙이고, 실제
 *     필터링은 백엔드 `GET /companies/{companyId}/projects` 응답이 이미
 *     역할별로 걸러서 내려준다(PR #61) — 프론트는 받은 목록을 그대로 렌더링한다.
 *
 * WBS 3.4.1 실 연동: project.service 사용. role은 useCurrentCompanyRole()을 쓴다 —
 * 이 화면은 companyId만 알고 projectId는 모르는데, 프로젝트 생성 권한은 애초에
 * 회사 레벨(COMPANY_ADMIN)이라 회사 role만으로 정확히 판단된다.
 *
 * ⚠️ 화면정의서 S04 "주요 데이터"는 "프로젝트명, 상태, 멤버 수, 이슈 수, PM"인데
 * `ProjectResponse`(PR #61)에는 멤버 수/이슈 수/PM 이름이 없어 카드에서 뺐다.
 * Project Member API(3.3.4)와 Issue API(3.5)가 이제 생겨서 기술적으로는 조회가
 * 가능하지만, 카드마다 개별 호출하면 N+1이라 목록 응답이 집계 필드를 내려주기
 * 전까지는 그대로 둔다(임시로 채우면 나중에 걷어내는 재작업이 생긴다).
 * 근거: sprint2-3.4-project-frontend-설계.md, 화면정의서 S04 인용구 메모.
 */
import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { useCurrentCompanyRole } from "@/hooks/useCurrentCompanyRole";
import { projectService } from "@/services/project.service";
import { invitationErrorMessage } from "@/lib/invitation";
import type { ProjectResponse, ProjectStatus } from "@/types/project.types";

const STATUS_LABEL: Record<ProjectStatus, string> = {
  PLANNING: "기획중",
  ACTIVE: "진행중",
  PAUSED: "일시중지",
  COMPLETED: "완료",
  ARCHIVED: "보관됨",
};

const STATUS_CLASS: Record<ProjectStatus, string> = {
  PLANNING: "bg-accent-subtle text-accent",
  ACTIVE: "bg-success/15 text-success",
  PAUSED: "bg-warning/15 text-warning",
  COMPLETED: "bg-border text-muted",
  ARCHIVED: "bg-border text-muted",
};

type LoadState = "loading" | "ready" | "error";

export default function ProjectsPage() {
  const params = useParams<{ companyId: string }>();
  const companyId = params.companyId;
  const role = useCurrentCompanyRole();

  const [listState, setListState] = useState<LoadState>("loading");
  const [listError, setListError] = useState<string | null>(null);
  const [projects, setProjects] = useState<ProjectResponse[]>([]);
  const [statusFilter, setStatusFilter] = useState<ProjectStatus | "ALL">("ALL");
  const [creating, setCreating] = useState(false);
  const [newName, setNewName] = useState("");
  const [newKey, setNewKey] = useState("");
  const [createBusy, setCreateBusy] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);

  const loadProjects = useCallback(async () => {
    setListState("loading");
    setListError(null);
    try {
      const { data } = await projectService.list(companyId);
      setProjects(data.data.projects);
      setListState("ready");
    } catch (error) {
      setListError(invitationErrorMessage(error, "프로젝트 목록을 불러오지 못했습니다."));
      setListState("error");
    }
  }, [companyId]);

  useEffect(() => {
    (async () => {
      await loadProjects();
    })();
  }, [loadProjects]);

  const visibleProjects = projects.filter(
    (p) => statusFilter === "ALL" || p.status === statusFilter
  );

  const createProject = async () => {
    const name = newName.trim();
    const key = newKey.trim().toUpperCase();
    if (!name || !key) return;
    setCreateBusy(true);
    setCreateError(null);
    try {
      const { data } = await projectService.create(companyId, { name, projectKey: key });
      setProjects((prev) => [...prev, data.data.project]);
      setNewName("");
      setNewKey("");
      setCreating(false);
    } catch (error) {
      setCreateError(invitationErrorMessage(error, "프로젝트 생성에 실패했습니다."));
    } finally {
      setCreateBusy(false);
    }
  };

  return (
    <div className="mx-auto max-w-4xl px-6 py-10">
      <Link href="/hub" className="text-sm text-muted transition-colors duration-fast hover:text-primary">
        ← 허브로
      </Link>
      <div className="mt-3 flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-foreground">프로젝트</h1>
          <p className="mt-1 text-sm text-muted">
            {role === "C" ? "회사 전체 프로젝트입니다." : "소속된 프로젝트입니다."}
          </p>
        </div>
        {/* 화면정의서: A는 새 프로젝트 버튼을 보지 않는다 */}
        {role !== "A" && (
          <button
            onClick={() => setCreating((v) => !v)}
            className="rounded-lg btn-gradient px-4 py-2 text-sm font-semibold text-primary-foreground transition-colors duration-fast"
          >
            + 새 프로젝트
          </button>
        )}
      </div>

      {creating && (
        <section className="mt-4 rounded-xl border border-border bg-surface p-5">
          <h2 className="text-sm font-semibold text-foreground">새 프로젝트</h2>
          {createError && <p className="mt-2 text-xs text-destructive">{createError}</p>}
          <div className="mt-3 flex flex-wrap gap-2">
            <input
              aria-label="프로젝트 이름"
              value={newName}
              onChange={(e) => setNewName(e.target.value)}
              placeholder="프로젝트 이름"
              disabled={createBusy}
              className="flex-1 rounded-lg border border-border bg-surface px-3 py-2 text-sm text-foreground placeholder:text-placeholder focus:border-ring focus:outline-none disabled:opacity-50"
            />
            <input
              aria-label="프로젝트 키"
              value={newKey}
              onChange={(e) => setNewKey(e.target.value)}
              placeholder="키 (예: THR)"
              maxLength={10}
              disabled={createBusy}
              className="w-32 rounded-lg border border-border bg-surface px-3 py-2 text-sm uppercase text-foreground placeholder:text-placeholder focus:border-ring focus:outline-none disabled:opacity-50"
            />
            <button
              onClick={createProject}
              disabled={createBusy}
              className="rounded-lg btn-gradient px-4 py-2 text-sm font-semibold text-primary-foreground transition-colors duration-fast disabled:opacity-50"
            >
              생성
            </button>
            <button
              onClick={() => setCreating(false)}
              disabled={createBusy}
              className="rounded-lg px-3 py-2 text-sm font-medium text-muted transition-colors duration-fast hover:bg-primary-subtle disabled:opacity-50"
            >
              취소
            </button>
          </div>
        </section>
      )}

      {/* 상태 필터 */}
      <div className="mt-6 flex flex-wrap gap-2">
        {(["ALL", "PLANNING", "ACTIVE", "PAUSED", "COMPLETED", "ARCHIVED"] as const).map((status) => (
          <button
            key={status}
            onClick={() => setStatusFilter(status)}
            className={
              "rounded-full border px-3 py-1 text-xs font-medium transition-colors duration-fast " +
              (statusFilter === status
                ? "border-primary bg-primary-subtle text-primary"
                : "border-border bg-surface text-muted hover:bg-primary-subtle")
            }
          >
            {status === "ALL" ? "전체" : STATUS_LABEL[status]}
          </button>
        ))}
      </div>

      {listState === "loading" && (
        <div className="mt-6 rounded-xl border border-dashed border-border px-4 py-10 text-center text-sm text-muted">
          불러오는 중...
        </div>
      )}

      {listState === "error" && (
        <div className="mt-6 rounded-xl border border-destructive/30 bg-destructive-subtle px-4 py-6 text-center text-sm text-destructive">
          {listError}
        </div>
      )}

      {/* 프로젝트 카드 목록 */}
      {listState === "ready" && (
        <div className="mt-4 grid grid-cols-1 gap-3 sm:grid-cols-2">
          {visibleProjects.map((project) => (
            <Link
              key={project.id}
              href={`/${companyId}/${project.id}/graph`}
              className="flex flex-col gap-3 rounded-xl bg-surface p-5 shadow-sm transition-all duration-fast hover:-translate-y-0.5 hover:bg-primary-subtle hover:shadow-md"
            >
              <div className="flex items-start justify-between">
                <div>
                  <span className="text-xs font-mono text-muted">{project.projectKey}</span>
                  <h3 className="text-sm font-semibold text-foreground">{project.name}</h3>
                </div>
                <span className={`rounded-full px-2.5 py-0.5 text-xs font-medium ${STATUS_CLASS[project.status]}`}>
                  {STATUS_LABEL[project.status]}
                </span>
              </div>
              {(project.startDate || project.endDate) && (
                <div className="flex items-center gap-1 text-xs text-muted">
                  <span>{project.startDate ?? "-"}</span>
                  <span>~</span>
                  <span>{project.endDate ?? "-"}</span>
                </div>
              )}
            </Link>
          ))}
          {visibleProjects.length === 0 && (
            <div className="col-span-full rounded-xl border border-dashed border-border px-4 py-10 text-center text-sm text-muted">
              해당 상태의 프로젝트가 없습니다.
            </div>
          )}
        </div>
      )}
    </div>
  );
}
