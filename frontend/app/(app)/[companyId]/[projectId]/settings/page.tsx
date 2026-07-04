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
 *   - PROJECT_ADMIN은 최소 1명 유지해야 한다 — 마지막 1명을 MEMBER로 바꾸거나
 *     제거하려 하면 막는다.
 *   - 프로젝트별(PROJECT scope) 알림 설정은 여기서 제공하지 않는다 — S11의
 *     GLOBAL 설정 기준으로 처리(설계 메모에 명시).
 *
 * ⚠️ MOCK: 프로젝트/프로젝트멤버 API(`GET/PATCH /projects/{projectId}`,
 * `/archive`, `/project-members`, invitation)가 아직 없어서 로컬 state로
 * 흉내낸다. MOCK_ROLE은 HubOverlay와 동일한 상수 — S12는 P/C만 접근 가능한
 * 화면이라 A라면 접근 자체를 막아야 하지만, 실제 auth 가드가 없는 지금은
 * 화면 하단에 배지로만 role을 표시해 접근 제약을 시각적으로 남겨둔다.
 */
import { useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";

type ProjectStatus = "PLANNING" | "ACTIVE" | "PAUSED" | "COMPLETED" | "ARCHIVED";
type ProjectMemberRole = "PROJECT_ADMIN" | "MEMBER";

type ProjectInfo = {
  name: string;
  projectKey: string;
  description: string;
  startDate: string | null;
  endDate: string | null;
  status: ProjectStatus;
};

type ProjectMember = {
  id: string;
  name: string;
  email: string;
  role: ProjectMemberRole;
};

const STATUS_LABEL: Record<ProjectStatus, string> = {
  PLANNING: "기획중",
  ACTIVE: "진행중",
  PAUSED: "일시중지",
  COMPLETED: "완료",
  ARCHIVED: "보관됨",
};

const MOCK_PROJECT: ProjectInfo = {
  name: "Threadia MVP",
  projectKey: "THR",
  description: "Threadia 6주 MVP 데모 프로젝트",
  startDate: "2026-06-15",
  endDate: "2026-07-31",
  status: "ACTIVE",
};

const MOCK_MEMBERS: ProjectMember[] = [
  { id: "u1", name: "정철진", email: "cjfwls39@threadia.com", role: "PROJECT_ADMIN" },
  { id: "u2", name: "김민정", email: "minjeong@threadia.com", role: "PROJECT_ADMIN" },
  { id: "u3", name: "여도현", email: "dohyun@threadia.com", role: "MEMBER" },
  { id: "u4", name: "황스일", email: "seil@threadia.com", role: "MEMBER" },
];

export default function ProjectSettingsPage() {
  const params = useParams<{ companyId: string; projectId: string }>();

  const [project, setProject] = useState<ProjectInfo>(MOCK_PROJECT);
  const [draft, setDraft] = useState<ProjectInfo>(MOCK_PROJECT);
  const [editing, setEditing] = useState(false);

  const [members, setMembers] = useState<ProjectMember[]>(MOCK_MEMBERS);
  const [memberError, setMemberError] = useState<string | null>(null);
  const [inviteEmail, setInviteEmail] = useState("");
  const [pendingInvites, setPendingInvites] = useState<string[]>([]);

  const [showArchiveConfirm, setShowArchiveConfirm] = useState(false);

  const adminCount = members.filter((m) => m.role === "PROJECT_ADMIN").length;
  const isArchived = project.status === "ARCHIVED";

  const startEdit = () => {
    setDraft(project);
    setEditing(true);
  };

  const saveEdit = () => {
    // TODO: PATCH /api/v1/projects/{projectId}
    setProject(draft);
    setEditing(false);
  };

  const setMemberRole = (id: string, role: ProjectMemberRole) => {
    const target = members.find((m) => m.id === id);
    if (target?.role === "PROJECT_ADMIN" && role === "MEMBER" && adminCount <= 1) {
      setMemberError("PROJECT_ADMIN은 최소 1명 유지해야 합니다.");
      return;
    }
    setMemberError(null);
    // TODO: PATCH /api/v1/project-members/{projectMemberId}
    setMembers((prev) => prev.map((m) => (m.id === id ? { ...m, role } : m)));
  };

  const removeMember = (id: string) => {
    const target = members.find((m) => m.id === id);
    if (target?.role === "PROJECT_ADMIN" && adminCount <= 1) {
      setMemberError("PROJECT_ADMIN은 최소 1명 유지해야 합니다.");
      return;
    }
    setMemberError(null);
    // TODO: DELETE /api/v1/project-members/{projectMemberId}
    setMembers((prev) => prev.filter((m) => m.id !== id));
  };

  const inviteMember = () => {
    const email = inviteEmail.trim().toLowerCase();
    if (!email) return;
    // TODO: POST /api/v1/projects/{projectId}/invitations — accept 전엔 membership 미생성
    setPendingInvites((prev) => [...prev, email]);
    setInviteEmail("");
  };

  const archiveProject = () => {
    // TODO: POST /api/v1/projects/{projectId}/archive
    setProject((prev) => ({ ...prev, status: "ARCHIVED" }));
    setShowArchiveConfirm(false);
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
          {!editing && !isArchived && (
            <button
              onClick={startEdit}
              className="rounded-md px-3 py-1.5 text-sm font-medium text-primary transition-colors duration-fast hover:bg-primary-subtle"
            >
              수정
            </button>
          )}
        </div>

        {editing ? (
          <div className="mt-4 flex flex-col gap-3">
            <input
              aria-label="프로젝트 이름"
              value={draft.name}
              onChange={(e) => setDraft({ ...draft, name: e.target.value })}
              className="rounded-lg border border-border bg-surface px-3 py-2 text-sm text-foreground focus:border-ring focus:outline-none"
            />
            <textarea
              aria-label="프로젝트 설명"
              value={draft.description}
              onChange={(e) => setDraft({ ...draft, description: e.target.value })}
              rows={2}
              className="resize-none rounded-lg border border-border bg-surface px-3 py-2 text-sm text-foreground focus:border-ring focus:outline-none"
            />
            <div className="flex gap-2">
              <input
                aria-label="시작일"
                type="date"
                value={draft.startDate ?? ""}
                onChange={(e) => setDraft({ ...draft, startDate: e.target.value || null })}
                className="rounded-lg border border-border bg-surface px-3 py-2 text-sm text-foreground focus:border-ring focus:outline-none"
              />
              <input
                aria-label="종료일"
                type="date"
                value={draft.endDate ?? ""}
                onChange={(e) => setDraft({ ...draft, endDate: e.target.value || null })}
                className="rounded-lg border border-border bg-surface px-3 py-2 text-sm text-foreground focus:border-ring focus:outline-none"
              />
            </div>
            <div className="flex justify-end gap-2">
              <button
                onClick={() => setEditing(false)}
                className="rounded-md px-3 py-1.5 text-sm font-medium text-muted hover:bg-primary-subtle"
              >
                취소
              </button>
              <button
                onClick={saveEdit}
                className="rounded-md btn-gradient px-3 py-1.5 text-sm font-semibold text-primary-foreground"
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

      {/* 멤버 관리 */}
      <section className="mt-6 rounded-xl border border-border bg-surface p-6">
        <h2 className="text-sm font-semibold text-foreground">프로젝트 멤버 ({members.length})</h2>

        {memberError && <p className="mt-2 text-xs text-destructive">{memberError}</p>}

        <ul className="mt-3 divide-y divide-border">
          {members.map((member) => (
            <li
              key={member.id}
              className="flex flex-wrap items-center justify-between gap-2 px-1 py-2.5"
            >
              <div>
                <p className="text-sm font-medium text-foreground">{member.name}</p>
                <p className="text-xs text-muted">{member.email}</p>
              </div>
              <div className="flex items-center gap-2">
                <select
                  aria-label={`${member.name} 역할`}
                  value={member.role}
                  disabled={isArchived}
                  onChange={(e) => setMemberRole(member.id, e.target.value as ProjectMemberRole)}
                  className="rounded-md border border-border bg-surface px-2 py-1.5 text-xs text-foreground focus:border-ring focus:outline-none disabled:cursor-not-allowed"
                >
                  <option value="PROJECT_ADMIN">PROJECT_ADMIN</option>
                  <option value="MEMBER">MEMBER</option>
                </select>
                <button
                  onClick={() => removeMember(member.id)}
                  disabled={isArchived}
                  className="text-xs font-medium text-destructive transition-colors duration-fast hover:opacity-70 disabled:cursor-not-allowed disabled:opacity-40"
                >
                  제거
                </button>
              </div>
            </li>
          ))}
        </ul>

        {!isArchived && (
          <>
            <div className="mt-4 flex gap-2">
              <input
                aria-label="초대할 이메일"
                type="email"
                value={inviteEmail}
                onChange={(e) => setInviteEmail(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && inviteMember()}
                placeholder="example@threadia.com"
                className="flex-1 rounded-lg border border-border bg-surface px-3 py-2 text-sm text-foreground placeholder:text-placeholder focus:border-ring focus:outline-none"
              />
              <button
                onClick={inviteMember}
                className="rounded-lg btn-gradient px-4 py-2 text-sm font-semibold text-primary-foreground transition-colors duration-fast"
              >
                초대
              </button>
            </div>
            {pendingInvites.length > 0 && (
              <ul className="mt-2 flex flex-col gap-1">
                {pendingInvites.map((email) => (
                  <li key={email} className="text-xs text-muted">
                    {email} — 대기 중 (수락 시 멤버로 추가됩니다)
                  </li>
                ))}
              </ul>
            )}
          </>
        )}
      </section>

      {/* 아카이빙 */}
      {!isArchived && (
        <section className="mt-6 rounded-xl border border-destructive/30 bg-surface p-6">
          <h2 className="text-sm font-semibold text-destructive">프로젝트 아카이빙</h2>
          <p className="mt-1 text-xs text-muted">
            아카이빙된 프로젝트는 복원할 수 없습니다. 신중하게 진행해 주세요.
          </p>
          {showArchiveConfirm ? (
            <div className="mt-3 flex items-center gap-2">
              <span className="text-sm text-foreground">정말 아카이빙할까요?</span>
              <button
                onClick={() => setShowArchiveConfirm(false)}
                className="rounded-md px-3 py-1.5 text-sm font-medium text-muted hover:bg-primary-subtle"
              >
                취소
              </button>
              <button
                onClick={archiveProject}
                className="rounded-md bg-destructive px-3 py-1.5 text-sm font-semibold text-on-primary hover:opacity-90"
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

      <p className="mt-6 text-xs text-placeholder">
        프로젝트 {params.projectId} · 이 화면은 P/C만 접근 가능합니다(auth 연동 전이라 접근 제한은 미적용).
      </p>
    </div>
  );
}
