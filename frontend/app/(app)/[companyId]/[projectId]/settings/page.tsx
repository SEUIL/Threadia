"use client";

/*
 * S12 — 프로젝트 설정
 *
 * 접근 사용자: P/C. 화면정의서(docs/03-design/화면정의서.md §S12) 기준
 * 프로젝트 정보 수정, 멤버 관리(역할 변경/제거/초대), 아카이빙을 구현한다.
 *
 * 설계 메모 반영:
 *   - 삭제는 물리 삭제가 아니라 아카이빙(`status = ARCHIVED`) — 복원은 MVP 보류
 *     (`PROJECT_RESTORE_DEFERRED`)라 아카이빙 후에는 되돌리는 UI를 두지 않는다.
 *   - 프로젝트별(PROJECT scope) 알림 설정은 여기서 제공하지 않는다 — S11의
 *     GLOBAL 설정 기준으로 처리(설계 메모에 명시).
 *
 * WBS 3.4.3 실 연동: project.service 사용(PR #65). `GET /projects/{projectId}`가
 * 내려주는 `viewer.canManageProject`로 실제 프로젝트 레벨 권한을 그대로 쓴다 —
 * 이 화면이 WBS 3.2.4의 useCurrentCompanyRole()(회사 role만 아는 보수적 근사치)
 * 대신 진짜 프로젝트 단위 RBAC를 쓰는 첫 화면이다. 프로젝트 멤버도 아니고
 * 회사 관리자도 아니면 detail() 자체가 403을 던지므로 pageState "error"로
 * 자연스럽게 처리된다(별도 접근 차단 로직 불필요).
 *
 * WBS 3.4.3 Phase 8 실 연동: 멤버 역할변경/제거는 WBS 3.3.4(PR #70, Project
 * Member API)로 실 연동했다. `viewer.canManageProject`인 사용자만 역할 select/
 * 제거 버튼을 본다 — 이 조건은 서버의 `requireProjectManage`와 동일해서 UI에서
 * 숨긴 액션은 어차피 서버도 막는다. ACTIVE 상태 멤버만 조회한다(제거된 멤버는
 * 목록에서 사라짐, 복원 UI 없음).
 *
 * ⚠️ 멤버 "추가"(초대)는 WBS 4.3(Project Invitation, Sprint 3 범위)이 아직
 * 없어서 여전히 제공하지 않는다 — PR #70도 조회/역할변경/제거만 다루고 추가
 * endpoint 자체가 없다(경계가 코드로 확인됨). 근거:
 * sprint2-3.4-project-frontend-설계.md, 화면정의서 S12 인용구 메모.
 *
 * WBS 3.4.4 QA(PR #72 finding F-3, 김민정) 반영: 프로젝트 상세와 멤버 목록을
 * `Promise.all`로 묶어서 불러왔더니 멤버 목록 API만 실패해도 페이지 전체가
 * error로 빠져 프로젝트 정보까지 못 보는 문제가 있었다. 두 호출을 독립된
 * loading/error 상태로 분리해서, 멤버 목록만 실패해도 나머지 섹션은 그대로
 * 보이게 고쳤다.
 */
import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { projectService } from "@/services/project.service";
import { projectMemberService } from "@/services/project-member.service";
import { invitationErrorMessage } from "@/lib/invitation";
import type { ProjectResponse, ProjectStatus, ProjectViewerData } from "@/types/project.types";
import type { ProjectMemberResponse } from "@/types/project-member.types";

const STATUS_LABEL: Record<ProjectStatus, string> = {
  PLANNING: "기획중",
  ACTIVE: "진행중",
  PAUSED: "일시중지",
  COMPLETED: "완료",
  ARCHIVED: "보관됨",
};

type LoadState = "loading" | "ready" | "error";

type ProjectDraft = {
  name: string;
  description: string;
  startDate: string | null;
  endDate: string | null;
};

function toDraft(project: ProjectResponse): ProjectDraft {
  return {
    name: project.name,
    description: project.description ?? "",
    startDate: project.startDate,
    endDate: project.endDate,
  };
}

export default function ProjectSettingsPage() {
  const params = useParams<{ companyId: string; projectId: string }>();

  const [pageState, setPageState] = useState<LoadState>("loading");
  const [pageError, setPageError] = useState<string | null>(null);
  const [project, setProject] = useState<ProjectResponse | null>(null);
  const [viewer, setViewer] = useState<ProjectViewerData | null>(null);

  const [draft, setDraft] = useState<ProjectDraft | null>(null);
  const [editing, setEditing] = useState(false);
  const [editBusy, setEditBusy] = useState(false);
  const [editError, setEditError] = useState<string | null>(null);

  const [showArchiveConfirm, setShowArchiveConfirm] = useState(false);
  const [archiveBusy, setArchiveBusy] = useState(false);
  const [archiveError, setArchiveError] = useState<string | null>(null);

  const [members, setMembers] = useState<ProjectMemberResponse[]>([]);
  const [memberListState, setMemberListState] = useState<LoadState>("loading");
  const [memberListError, setMemberListError] = useState<string | null>(null);
  const [memberError, setMemberError] = useState<string | null>(null);
  const [memberBusyId, setMemberBusyId] = useState<number | null>(null);

  const loadPage = useCallback(async () => {
    setPageState("loading");
    setPageError(null);
    try {
      const { data } = await projectService.detail(params.projectId);
      setProject(data.data.project);
      setViewer(data.data.viewer);
      setPageState("ready");
    } catch (error) {
      setPageError(invitationErrorMessage(error, "프로젝트 정보를 불러오지 못했습니다."));
      setPageState("error");
    }
  }, [params.projectId]);

  // 프로젝트 상세와 별개 요청이라, 이 목록만 실패해도 나머지 화면은 그대로 유지한다.
  const loadMembers = useCallback(async () => {
    setMemberListState("loading");
    setMemberListError(null);
    try {
      const { data } = await projectMemberService.list(params.projectId, { status: "ACTIVE" });
      setMembers(data.data.projectMembers);
      setMemberListState("ready");
    } catch (error) {
      setMemberListError(invitationErrorMessage(error, "프로젝트 멤버 목록을 불러오지 못했습니다."));
      setMemberListState("error");
    }
  }, [params.projectId]);

  useEffect(() => {
    (async () => {
      await Promise.all([loadPage(), loadMembers()]);
    })();
  }, [loadPage, loadMembers]);

  const isArchived = project?.status === "ARCHIVED";
  const canManage = viewer?.canManageProject ?? false;

  const startEdit = () => {
    if (!project) return;
    setDraft(toDraft(project));
    setEditError(null);
    setEditing(true);
  };

  const saveEdit = async () => {
    if (!draft || !project || project.status === "ARCHIVED") return;
    setEditBusy(true);
    setEditError(null);
    try {
      const { data } = await projectService.update(params.projectId, {
        name: draft.name,
        description: draft.description || null,
        status: project.status,
        startDate: draft.startDate,
        endDate: draft.endDate,
      });
      setProject(data.data.project);
      setEditing(false);
    } catch (error) {
      setEditError(invitationErrorMessage(error, "프로젝트 정보 수정에 실패했습니다."));
    } finally {
      setEditBusy(false);
    }
  };

  const archiveProject = async () => {
    setArchiveBusy(true);
    setArchiveError(null);
    try {
      await projectService.archive(params.projectId);
      setProject((prev) => (prev ? { ...prev, status: "ARCHIVED" } : prev));
      setShowArchiveConfirm(false);
    } catch (error) {
      setArchiveError(invitationErrorMessage(error, "프로젝트 아카이빙에 실패했습니다."));
    } finally {
      setArchiveBusy(false);
    }
  };

  // role/status 전체를 항상 같이 보낸다(full-replace) — company-member와 동일한 이유.
  const updateMemberRole = async (member: ProjectMemberResponse, role: ProjectMemberResponse["role"]) => {
    setMemberError(null);
    setMemberBusyId(member.id);
    try {
      const { data } = await projectMemberService.update(member.id, { role, status: member.status });
      setMembers((prev) => prev.map((m) => (m.id === member.id ? data.data.projectMember : m)));
    } catch (error) {
      setMemberError(invitationErrorMessage(error, "멤버 역할 변경에 실패했습니다."));
    } finally {
      setMemberBusyId(null);
    }
  };

  const removeMember = async (member: ProjectMemberResponse) => {
    setMemberError(null);
    setMemberBusyId(member.id);
    try {
      await projectMemberService.remove(member.id);
      setMembers((prev) => prev.filter((m) => m.id !== member.id));
    } catch (error) {
      setMemberError(invitationErrorMessage(error, "멤버 제거에 실패했습니다."));
    } finally {
      setMemberBusyId(null);
    }
  };

  if (pageState === "loading") {
    return <div className="mx-auto max-w-3xl px-6 py-10 text-sm text-muted">불러오는 중...</div>;
  }

  if (pageState === "error" || !project) {
    return (
      <div className="mx-auto max-w-3xl px-6 py-10">
        <Link
          href={`/${params.companyId}/projects`}
          className="text-sm text-muted transition-colors duration-fast hover:text-primary"
        >
          ← 프로젝트 목록
        </Link>
        <div className="mt-4 rounded-lg border border-destructive/30 bg-destructive-subtle px-4 py-6 text-center text-sm text-destructive">
          {pageError}
        </div>
      </div>
    );
  }

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
          <h1 className="text-xl font-bold text-foreground">프로젝트 설정</h1>
          <p className="mt-1 text-sm text-muted">{project.projectKey} · {project.name}</p>
        </div>
        <span className="rounded-full bg-border px-3 py-1 text-xs font-medium text-muted">
          {STATUS_LABEL[project.status]}
        </span>
      </div>

      {isArchived && (
        <div className="mt-4 rounded-lg border border-warning/40 bg-warning/10 px-4 py-3 text-sm text-warning">
          보관된 프로젝트입니다. 복원은 지원하지 않습니다.
        </div>
      )}

      {/* 프로젝트 정보 */}
      <section className="mt-6 rounded-xl border border-border bg-surface p-6">
        <div className="flex items-center justify-between">
          <h2 className="text-sm font-semibold text-foreground">프로젝트 정보</h2>
          {canManage && !editing && !isArchived && (
            <button
              onClick={startEdit}
              className="rounded-md px-3 py-1.5 text-sm font-medium text-primary transition-colors duration-fast hover:bg-primary-subtle"
            >
              수정
            </button>
          )}
        </div>

        {editing && draft ? (
          <div className="mt-4 flex flex-col gap-3">
            {editError && <p className="text-xs text-destructive">{editError}</p>}
            <input
              aria-label="프로젝트 이름"
              value={draft.name}
              onChange={(e) => setDraft({ ...draft, name: e.target.value })}
              disabled={editBusy}
              className="rounded-lg border border-border bg-surface px-3 py-2 text-sm text-foreground focus:border-ring focus:outline-none disabled:opacity-50"
            />
            <textarea
              aria-label="프로젝트 설명"
              value={draft.description}
              onChange={(e) => setDraft({ ...draft, description: e.target.value })}
              rows={2}
              disabled={editBusy}
              className="resize-none rounded-lg border border-border bg-surface px-3 py-2 text-sm text-foreground focus:border-ring focus:outline-none disabled:opacity-50"
            />
            <div className="flex gap-2">
              <input
                aria-label="시작일"
                type="date"
                value={draft.startDate ?? ""}
                onChange={(e) => setDraft({ ...draft, startDate: e.target.value || null })}
                disabled={editBusy}
                className="rounded-lg border border-border bg-surface px-3 py-2 text-sm text-foreground focus:border-ring focus:outline-none disabled:opacity-50"
              />
              <input
                aria-label="종료일"
                type="date"
                value={draft.endDate ?? ""}
                onChange={(e) => setDraft({ ...draft, endDate: e.target.value || null })}
                disabled={editBusy}
                className="rounded-lg border border-border bg-surface px-3 py-2 text-sm text-foreground focus:border-ring focus:outline-none disabled:opacity-50"
              />
            </div>
            <div className="flex justify-end gap-2">
              <button
                onClick={() => setEditing(false)}
                disabled={editBusy}
                className="rounded-md px-3 py-1.5 text-sm font-medium text-muted hover:bg-primary-subtle disabled:opacity-50"
              >
                취소
              </button>
              <button
                onClick={saveEdit}
                disabled={editBusy}
                className="rounded-md btn-gradient px-3 py-1.5 text-sm font-semibold text-primary-foreground disabled:opacity-50"
              >
                저장
              </button>
            </div>
          </div>
        ) : (
          <dl className="mt-4 grid grid-cols-2 gap-4 text-sm">
            <div className="col-span-2">
              <dt className="text-muted">설명</dt>
              <dd className="mt-0.5 font-medium text-foreground">{project.description || "-"}</dd>
            </div>
            <div>
              <dt className="text-muted">시작일</dt>
              <dd className="mt-0.5 font-medium text-foreground">{project.startDate ?? "-"}</dd>
            </div>
            <div>
              <dt className="text-muted">종료일</dt>
              <dd className="mt-0.5 font-medium text-foreground">{project.endDate ?? "-"}</dd>
            </div>
          </dl>
        )}
      </section>

      {/* 멤버 관리 — 역할변경/제거는 WBS 3.3.4로 실 연동. 추가(초대)는 WBS 4.3 대기.
          프로젝트 상세와는 독립된 요청이라 이 섹션만 loading/error를 따로 표시한다. */}
      <section className="mt-6 rounded-xl border border-border bg-surface p-6">
        <h2 className="text-sm font-semibold text-foreground">
          프로젝트 멤버{memberListState === "ready" && ` (${members.length})`}
        </h2>

        {memberListState === "loading" && (
          <p className="mt-3 text-xs text-muted">멤버 목록을 불러오는 중...</p>
        )}

        {memberListState === "error" && (
          <p className="mt-3 text-xs text-destructive">{memberListError}</p>
        )}

        {memberListState === "ready" && (
          <>
            {memberError && <p className="mt-2 text-xs text-destructive">{memberError}</p>}

            <ul className="mt-3 divide-y divide-border">
              {members.map((member) => (
                <li key={member.id} className="flex flex-wrap items-center justify-between gap-2 px-1 py-2.5">
                  <div>
                    <p className="text-sm font-medium text-foreground">{member.user.nickname}</p>
                    <p className="text-xs text-muted">{member.user.email}</p>
                  </div>
                  {canManage && (
                    <div className="flex items-center gap-2">
                      <select
                        aria-label={`${member.user.nickname} 역할`}
                        value={member.role}
                        disabled={memberBusyId === member.id || isArchived}
                        onChange={(e) => updateMemberRole(member, e.target.value as ProjectMemberResponse["role"])}
                        className="rounded-md border border-border bg-surface px-2 py-1.5 text-xs text-foreground focus:border-ring focus:outline-none disabled:cursor-not-allowed"
                      >
                        <option value="PROJECT_ADMIN">PROJECT_ADMIN</option>
                        <option value="MEMBER">MEMBER</option>
                      </select>
                      <button
                        onClick={() => removeMember(member)}
                        disabled={memberBusyId === member.id || isArchived}
                        className="text-xs font-medium text-destructive transition-colors duration-fast hover:opacity-70 disabled:cursor-not-allowed disabled:opacity-40"
                      >
                        제거
                      </button>
                    </div>
                  )}
                </li>
              ))}
              {members.length === 0 && (
                <li className="py-2.5 text-center text-xs text-muted">멤버가 없습니다.</li>
              )}
            </ul>

            <p className="mt-4 text-xs text-placeholder">
              멤버 초대는 준비 중입니다. Project Invitation API 연동 후 제공될 예정입니다.
            </p>
          </>
        )}
      </section>

      {/* 아카이빙 */}
      {canManage && !isArchived && (
        <section className="mt-6 rounded-xl border border-destructive/30 bg-surface p-6">
          <h2 className="text-sm font-semibold text-destructive">프로젝트 아카이빙</h2>
          <p className="mt-1 text-xs text-muted">
            아카이빙된 프로젝트는 복원할 수 없습니다. 신중하게 진행해 주세요.
          </p>
          {archiveError && <p className="mt-2 text-xs text-destructive">{archiveError}</p>}
          {showArchiveConfirm ? (
            <div className="mt-3 flex items-center gap-2">
              <span className="text-sm text-foreground">정말 아카이빙할까요?</span>
              <button
                onClick={() => setShowArchiveConfirm(false)}
                disabled={archiveBusy}
                className="rounded-md px-3 py-1.5 text-sm font-medium text-muted hover:bg-primary-subtle disabled:opacity-50"
              >
                취소
              </button>
              <button
                onClick={archiveProject}
                disabled={archiveBusy}
                className="rounded-md bg-destructive px-3 py-1.5 text-sm font-semibold text-on-primary hover:opacity-90 disabled:opacity-50"
              >
                아카이빙
              </button>
            </div>
          ) : (
            <button
              onClick={() => setShowArchiveConfirm(true)}
              className="mt-3 rounded-md border border-destructive px-3 py-1.5 text-sm font-medium text-destructive transition-colors duration-fast hover:bg-destructive-subtle"
            >
              프로젝트 아카이빙
            </button>
          )}
        </section>
      )}
    </div>
  );
}
