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
 * WBS 4.3.2 실 연동: 멤버 "추가"(초대)를 붙였다. 이메일로 후보를 조회한 뒤
 * (`GET /projects/{id}/invitable-members`, Issue #108) 확인하고 초대를 만든다.
 * 회사 초대(S14)가 쓰는 전역 `GET /users/lookup`은 COMPANY_ADMIN 전용이라 P가
 * 쓸 수 없어서, 프로젝트 범위 후보 조회를 따로 쓴다.
 *
 * 초대 후보가 아니면 서버가 전부 404로 통일해서 준다(미가입·비활성·다른 회사를
 * 구분하지 않는다 — 회사 밖 email의 가입 여부를 노출하지 않기 위한 계약).
 * 그래서 이 화면도 그 셋을 하나의 문구로 안내한다.
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
import { isAxiosError } from "axios";
import { projectService } from "@/services/project.service";
import { projectMemberService } from "@/services/project-member.service";
import { invitationService } from "@/services/invitation.service";
import {
  INVITATION_STATUS_CLASS,
  INVITATION_STATUS_LABEL,
  formatExpiresAt,
  invitationErrorMessage,
} from "@/lib/invitation";
import type { ProjectResponse, ProjectStatus, ProjectViewerData } from "@/types/project.types";
import type { ProjectMemberResponse } from "@/types/project-member.types";
import type { InvitationResponse, ProjectInvitationRole } from "@/types/invitation.types";

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

  // 초대 — 프로젝트 상세/멤버 목록과 독립된 요청이라 loading/error를 따로 둔다(F-3 패턴).
  const [invitations, setInvitations] = useState<InvitationResponse[]>([]);
  const [invitationsState, setInvitationsState] = useState<LoadState>("loading");
  const [invitationsError, setInvitationsError] = useState<string | null>(null);
  const [inviteEmail, setInviteEmail] = useState("");
  const [inviteRole, setInviteRole] = useState<ProjectInvitationRole>("MEMBER");
  const [lookupBusy, setLookupBusy] = useState(false);
  // 후보 조회에 성공한 사용자 — 확인 후 생성한다.
  const [pendingInvitee, setPendingInvitee] = useState<{ id: number; email: string } | null>(null);
  const [createBusy, setCreateBusy] = useState(false);
  const [inviteError, setInviteError] = useState<string | null>(null);
  const [cancelBusyId, setCancelBusyId] = useState<number | null>(null);
  // 목록 로딩 실패(invitationsState)와 분리한다 — 같은 state를 쓰면 action 실패
  // 하나가 렌더 조건을 깨서 목록 전체가 사라진다(PR #55 리뷰, 김민정).
  const [cancelError, setCancelError] = useState<string | null>(null);

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

  // silent: action 직후 재조회처럼 이미 목록이 떠 있을 때 깜빡임을 막는다.
  const loadInvitations = useCallback(
    async (options?: { silent?: boolean }) => {
      if (!options?.silent) setInvitationsState("loading");
      setInvitationsError(null);
      try {
        const { data } = await invitationService.listProject(params.projectId);
        setInvitations(data.data.invitations);
        setInvitationsState("ready");
      } catch (error) {
        setInvitationsError(invitationErrorMessage(error, "초대 목록을 불러오지 못했습니다."));
        setInvitationsState("error");
      }
    },
    [params.projectId]
  );

  useEffect(() => {
    (async () => {
      await Promise.all([loadPage(), loadMembers()]);
    })();
  }, [loadPage, loadMembers]);

  const isArchived = project?.status === "ARCHIVED";
  const canManage = viewer?.canManageProject ?? false;

  // 초대 목록은 P/C 전용이라 권한을 확인한 뒤에 부른다(아니면 403).
  useEffect(() => {
    if (!canManage) return;
    (async () => {
      await loadInvitations();
    })();
  }, [canManage, loadInvitations]);

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

  const lookupInvitee = async () => {
    const email = inviteEmail.trim().toLowerCase();
    setInviteError(null);
    setPendingInvitee(null);
    if (!email) return;

    setLookupBusy(true);
    try {
      const { data } = await invitationService.findInvitableMember(params.projectId, email);
      setPendingInvitee({ id: data.data.user.id, email: data.data.user.email });
    } catch (error) {
      // 서버는 미가입·비활성·다른 회사를 구분 없이 404로 준다(회사 밖 email의 가입
      // 여부를 노출하지 않기 위한 계약). 서버 message("사용자를 찾을 수 없습니다")를
      // 그대로 쓰면 "그런 계정이 없다"로 읽혀 오해를 주므로, 이 화면에서 할 수 있는
      // 일을 알려주는 문구로 바꾼다. 409(이미 멤버·대기 중 초대)는 매핑된 안내를 쓴다.
      const notCandidate = isAxiosError(error) && error.response?.status === 404;
      setInviteError(
        notCandidate
          ? "이 회사의 구성원 중에서 해당 이메일을 찾을 수 없습니다."
          : invitationErrorMessage(error, "초대 후보를 조회하지 못했습니다.")
      );
    } finally {
      setLookupBusy(false);
    }
  };

  const confirmInvite = async () => {
    if (!pendingInvitee) return;
    setCreateBusy(true);
    setInviteError(null);
    try {
      await invitationService.createProject(params.projectId, {
        inviteeUserId: pendingInvitee.id,
        role: inviteRole,
      });
      setPendingInvitee(null);
      setInviteEmail("");
      await loadInvitations({ silent: true });
    } catch (error) {
      setInviteError(invitationErrorMessage(error, "초대 생성에 실패했습니다."));
    } finally {
      setCreateBusy(false);
    }
  };

  const cancelInvitation = async (invitationId: number) => {
    setCancelError(null);
    setCancelBusyId(invitationId);
    try {
      // 성공 시엔 재조회 대신 응답으로 해당 행만 갱신한다 — 재조회가 실패 메시지를
      // 지우는 흐름과 얽히지 않게 하고, 목록 깜빡임도 없다.
      const { data } = await invitationService.cancel(invitationId);
      setInvitations((prev) =>
        prev.map((i) => (i.id === invitationId ? { ...i, status: data.data.status } : i))
      );
    } catch (error) {
      setCancelError(invitationErrorMessage(error, "초대 취소에 실패했습니다."));
      // 이미 수락/만료돼 실패했을 수 있으니 최신 상태로 맞춘다. loadInvitations는
      // cancelError를 건드리지 않으므로 방금 띄운 메시지가 지워지지 않는다.
      await loadInvitations({ silent: true });
    } finally {
      setCancelBusyId(null);
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
                    <p className="flex flex-wrap items-center gap-1.5 text-sm font-medium text-foreground">
                      <span>{member.user.nickname}</span>
                      {member.companyRole === "COMPANY_ADMIN" && (
                        <span className="rounded-full bg-primary-subtle px-2 py-0.5 text-[10px] font-medium text-primary">
                          회사 관리자
                        </span>
                      )}
                    </p>
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

          </>
        )}
      </section>

      {/* 멤버 초대 — P/C 전용. 아카이빙된 프로젝트에는 초대하지 않는다. */}
      {canManage && !isArchived && (
        <section className="mt-6 rounded-xl border border-border bg-surface p-6">
          <h2 className="text-sm font-semibold text-foreground">멤버 초대</h2>
          <p className="mt-1 text-xs text-muted">
            같은 회사 구성원만 초대할 수 있습니다. 수락하면 프로젝트 멤버가 됩니다.
          </p>

          <div className="mt-4 flex flex-wrap items-center gap-2">
            <input
              type="email"
              aria-label="초대할 이메일"
              value={inviteEmail}
              onChange={(e) => setInviteEmail(e.target.value)}
              onKeyDown={(e) => e.key === "Enter" && lookupInvitee()}
              placeholder="member@example.com"
              className="min-w-[220px] flex-1 rounded-md border border-border bg-surface px-3 py-2 text-sm text-foreground placeholder:text-placeholder focus:border-ring focus:outline-none"
            />
            <select
              aria-label="초대 역할"
              value={inviteRole}
              onChange={(e) => setInviteRole(e.target.value as ProjectInvitationRole)}
              className="rounded-md border border-border bg-surface px-2 py-2 text-sm text-foreground focus:border-ring focus:outline-none"
            >
              <option value="MEMBER">MEMBER</option>
              <option value="PROJECT_ADMIN">PROJECT_ADMIN</option>
            </select>
            <button
              type="button"
              onClick={lookupInvitee}
              disabled={lookupBusy || !inviteEmail.trim()}
              className="rounded-md btn-gradient px-4 py-2 text-sm font-semibold text-primary-foreground disabled:cursor-not-allowed disabled:opacity-50"
            >
              조회
            </button>
          </div>

          {inviteError && <p className="mt-3 text-xs text-destructive">{inviteError}</p>}

          {/* 후보 확인 — 실수로 엉뚱한 사람을 초대하지 않게 한 단계 둔다(S14와 동일) */}
          {pendingInvitee && (
            <div className="mt-3 flex flex-wrap items-center justify-between gap-2 rounded-md border border-border bg-primary-subtle px-3 py-2">
              <p className="text-xs text-foreground">
                <span className="font-medium">{pendingInvitee.email}</span> 님을 {inviteRole}(으)로
                초대할까요?
              </p>
              <div className="flex gap-2">
                <button
                  type="button"
                  onClick={() => setPendingInvitee(null)}
                  className="rounded-md px-2 py-1 text-xs font-medium text-muted transition-colors duration-fast hover:bg-surface"
                >
                  취소
                </button>
                <button
                  type="button"
                  onClick={confirmInvite}
                  disabled={createBusy}
                  className="rounded-md btn-gradient px-3 py-1 text-xs font-semibold text-primary-foreground disabled:cursor-not-allowed disabled:opacity-50"
                >
                  초대하기
                </button>
              </div>
            </div>
          )}

          <h3 className="mt-6 text-xs font-semibold text-foreground">
            보낸 초대{invitationsState === "ready" && ` (${invitations.length})`}
          </h3>

          {invitationsState === "loading" && (
            <p className="mt-2 text-xs text-muted">초대 목록을 불러오는 중...</p>
          )}
          {invitationsState === "error" && (
            <p className="mt-2 text-xs text-destructive">{invitationsError}</p>
          )}

          {invitationsState === "ready" && (
            <>
              {cancelError && <p className="mt-2 text-xs text-destructive">{cancelError}</p>}
              <ul className="mt-2 divide-y divide-border">
                {invitations.map((invitation) => (
                  <li
                    key={invitation.id}
                    className="flex flex-wrap items-center justify-between gap-2 px-1 py-2.5"
                  >
                    <div className="min-w-0">
                      <p className="truncate text-sm text-foreground">
                        {invitation.inviteeNickname}
                        <span className="ml-1.5 text-xs text-muted">{invitation.inviteeEmail}</span>
                      </p>
                      <p className="mt-0.5 text-xs text-muted">
                        {invitation.role} · {formatExpiresAt(invitation.expiresAt)}
                      </p>
                    </div>
                    <div className="flex items-center gap-2">
                      <span
                        className={`rounded-full px-2 py-0.5 text-[10px] font-medium ${INVITATION_STATUS_CLASS[invitation.status]}`}
                      >
                        {INVITATION_STATUS_LABEL[invitation.status]}
                      </span>
                      {/* PENDING만 취소할 수 있다(서버도 그 외 상태는 409) */}
                      {invitation.status === "PENDING" && (
                        <button
                          type="button"
                          onClick={() => cancelInvitation(invitation.id)}
                          disabled={cancelBusyId === invitation.id}
                          className="text-xs font-medium text-destructive transition-colors duration-fast hover:opacity-70 disabled:cursor-not-allowed disabled:opacity-40"
                        >
                          취소
                        </button>
                      )}
                    </div>
                  </li>
                ))}
                {invitations.length === 0 && (
                  <li className="py-2.5 text-center text-xs text-muted">보낸 초대가 없습니다.</li>
                )}
              </ul>
            </>
          )}
        </section>
      )}

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
