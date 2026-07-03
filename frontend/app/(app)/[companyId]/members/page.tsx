"use client";

/*
 * S14 — 구성원·권한 관리
 *
 * 접근 사용자: C(COMPANY_ADMIN)만. 화면정의서(docs/03-design/화면정의서.md §S14) 기준
 * 구성원 목록(역할/부서 변경, DM 시작) + 구성원 추가(이메일 조회 → 초대) +
 * 대기 중 초대 목록을 구현한다.
 *
 * ⚠️ MOCK: 회사 구성원/초대 API(`/company-members`, `/invitations`,
 * `/users/lookup`)가 아직 없어서 로컬 state로 흉내낸다. 이메일 조회 결과도
 * 아래 MOCK_REGISTERED_EMAILS 기준으로 판정한다. 부서 목록도 S13과 상태를
 * 공유하는 저장소가 없어서 별도로 목업했다(실연동 시 공통 스토어로 합쳐야 한다).
 */
import { useMemo, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";

type CompanyRole = "COMPANY_ADMIN" | "MEMBER";

type Member = {
  id: string;
  name: string;
  email: string;
  companyRole: CompanyRole;
  departmentId: string | null;
  isProjectAdmin: boolean;
};

type InvitationStatus = "PENDING" | "ACCEPTED" | "DECLINED" | "CANCELED" | "EXPIRED";

type Invitation = {
  id: string;
  email: string;
  role: CompanyRole;
  status: InvitationStatus;
};

const MOCK_DEPARTMENTS = [
  { id: "d1", name: "개발팀" },
  { id: "d2", name: "디자인팀" },
  { id: "d3", name: "기획팀" },
];

const MOCK_MEMBERS: Member[] = [
  { id: "u1", name: "정철진", email: "cjfwls39@threadia.com", companyRole: "COMPANY_ADMIN", departmentId: "d1", isProjectAdmin: true },
  { id: "u2", name: "김민정", email: "minjeong@threadia.com", companyRole: "MEMBER", departmentId: "d2", isProjectAdmin: true },
  { id: "u3", name: "여도현", email: "dohyun@threadia.com", companyRole: "MEMBER", departmentId: "d1", isProjectAdmin: false },
  { id: "u4", name: "황스일", email: "seil@threadia.com", companyRole: "MEMBER", departmentId: "d3", isProjectAdmin: false },
];

const MOCK_INVITATIONS: Invitation[] = [
  { id: "i1", email: "new-hire@threadia.com", role: "MEMBER", status: "PENDING" },
  { id: "i2", email: "expired-invite@threadia.com", role: "MEMBER", status: "EXPIRED" },
];

// /api/v1/users/lookup 목업 — 이 목록에 있으면 "등록 사용자", 이미 구성원 이메일이면
// ALREADY_COMPANY_MEMBER, PENDING 초대 이메일이면 INVITATION_ALREADY_PENDING.
const MOCK_REGISTERED_EMAILS = new Set([
  ...MOCK_MEMBERS.map((m) => m.email),
  ...MOCK_INVITATIONS.map((i) => i.email),
  "hong@threadia.com",
]);

const STATUS_LABEL: Record<InvitationStatus, string> = {
  PENDING: "대기 중",
  ACCEPTED: "수락됨",
  DECLINED: "거절됨",
  CANCELED: "취소됨",
  EXPIRED: "만료됨",
};

const STATUS_CLASS: Record<InvitationStatus, string> = {
  PENDING: "bg-warning/15 text-warning",
  ACCEPTED: "bg-success/15 text-success",
  DECLINED: "bg-destructive-subtle text-destructive",
  CANCELED: "bg-destructive-subtle text-destructive",
  EXPIRED: "bg-border text-muted",
};

export default function MembersPage() {
  const params = useParams<{ companyId: string }>();
  const companyId = params.companyId;

  const [members, setMembers] = useState<Member[]>(MOCK_MEMBERS);
  const [invitations, setInvitations] = useState<Invitation[]>(MOCK_INVITATIONS);
  const [query, setQuery] = useState("");

  const [inviteEmail, setInviteEmail] = useState("");
  const [inviteError, setInviteError] = useState<string | null>(null);
  const [pendingInvite, setPendingInvite] = useState<string | null>(null); // 조회 성공 → 확인 modal용

  const filteredMembers = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return members;
    return members.filter(
      (m) => m.name.toLowerCase().includes(q) || m.email.toLowerCase().includes(q)
    );
  }, [members, query]);

  const departmentName = (id: string | null) =>
    MOCK_DEPARTMENTS.find((d) => d.id === id)?.name ?? "미배정";

  const setMemberRole = (id: string, role: CompanyRole) => {
    // TODO: PATCH /api/v1/company-members/{companyMemberId}
    setMembers((prev) => prev.map((m) => (m.id === id ? { ...m, companyRole: role } : m)));
  };

  const setMemberDepartment = (id: string, departmentId: string) => {
    // TODO: PATCH /api/v1/company-members/{companyMemberId}
    setMembers((prev) => prev.map((m) => (m.id === id ? { ...m, departmentId } : m)));
  };

  // 화면정의서 §S14 "구성원 추가 정책" 표 기준 3케이스
  const lookupEmail = () => {
    const email = inviteEmail.trim().toLowerCase();
    setInviteError(null);
    setPendingInvite(null);
    if (!email) return;

    if (members.some((m) => m.email === email)) {
      setInviteError("이미 회사에 소속된 사용자입니다.");
      return;
    }
    if (invitations.some((i) => i.email === email && i.status === "PENDING")) {
      setInviteError("이미 대기 중인 초대가 있습니다.");
      return;
    }
    if (!MOCK_REGISTERED_EMAILS.has(email)) {
      setInviteError("등록된 사용자를 찾을 수 없습니다. (Sprint 1.5 범위 밖: 외부 이메일 초대 미지원)");
      return;
    }
    setPendingInvite(email);
  };

  const confirmInvite = () => {
    if (!pendingInvite) return;
    // TODO: POST /api/v1/companies/{companyId}/invitations
    setInvitations((prev) => [
      ...prev,
      { id: crypto.randomUUID(), email: pendingInvite, role: "MEMBER", status: "PENDING" },
    ]);
    setPendingInvite(null);
    setInviteEmail("");
  };

  const cancelInvitation = (id: string) => {
    // TODO: POST /api/v1/invitations/{invitationId}/cancel
    setInvitations((prev) =>
      prev.map((i) => (i.id === id ? { ...i, status: "CANCELED" as InvitationStatus } : i))
    );
  };

  return (
    <div className="mx-auto max-w-4xl px-6 py-10">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-foreground">구성원·권한 관리</h1>
          <p className="mt-1 text-sm text-muted">회사 구성원을 초대하고 역할·부서를 관리합니다.</p>
        </div>
        <Link
          href={`/${companyId}/settings`}
          className="rounded-md px-3 py-1.5 text-sm font-medium text-primary transition-colors duration-fast hover:bg-primary-subtle"
        >
          회사 관리로
        </Link>
      </div>

      {/* 구성원 추가 */}
      <section className="mt-6 rounded-xl border border-border bg-surface p-6 shadow-sm">
        <h2 className="text-sm font-semibold text-foreground">구성원 추가</h2>
        <div className="mt-3 flex gap-2">
          <input
            aria-label="초대할 이메일"
            type="email"
            value={inviteEmail}
            onChange={(e) => setInviteEmail(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && lookupEmail()}
            placeholder="example@threadia.com"
            className="flex-1 rounded-lg border border-border bg-surface px-3 py-2 text-sm text-foreground placeholder:text-placeholder focus:border-ring focus:outline-none"
          />
          <button
            onClick={lookupEmail}
            className="rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground transition-colors duration-fast hover:bg-primary-hover"
          >
            조회
          </button>
        </div>

        {inviteError && <p className="mt-2 text-xs text-destructive">{inviteError}</p>}

        {pendingInvite && (
          <div className="mt-3 flex items-center justify-between rounded-lg border border-primary/40 bg-primary-subtle px-4 py-3">
            <span className="text-sm text-foreground">
              <strong>{pendingInvite}</strong> 사용자에게 초대를 보낼까요?
            </span>
            <div className="flex gap-2">
              <button
                onClick={() => setPendingInvite(null)}
                className="rounded-md px-3 py-1.5 text-sm font-medium text-muted hover:bg-surface"
              >
                취소
              </button>
              <button
                onClick={confirmInvite}
                className="rounded-md bg-primary px-3 py-1.5 text-sm font-semibold text-primary-foreground hover:bg-primary-hover"
              >
                초대 보내기
              </button>
            </div>
          </div>
        )}

        {invitations.length > 0 && (
          <ul className="mt-4 flex flex-col gap-2">
            {invitations.map((inv) => (
              <li
                key={inv.id}
                className="flex items-center justify-between rounded-lg border border-border px-3 py-2"
              >
                <span className="text-sm text-foreground">{inv.email}</span>
                <div className="flex items-center gap-2">
                  <span className={`rounded-full px-2.5 py-0.5 text-xs font-medium ${STATUS_CLASS[inv.status]}`}>
                    {STATUS_LABEL[inv.status]}
                  </span>
                  {inv.status === "PENDING" && (
                    <button
                      onClick={() => cancelInvitation(inv.id)}
                      className="text-xs font-medium text-destructive transition-colors duration-fast hover:opacity-70"
                    >
                      취소
                    </button>
                  )}
                </div>
              </li>
            ))}
          </ul>
        )}
      </section>

      {/* 구성원 목록 */}
      <section className="mt-6 rounded-xl border border-border bg-surface p-6 shadow-sm">
        <div className="flex items-center justify-between">
          <h2 className="text-sm font-semibold text-foreground">구성원 ({members.length})</h2>
          <input
            aria-label="구성원 검색"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="이름 또는 이메일 검색"
            className="w-56 rounded-lg border border-border bg-surface px-3 py-1.5 text-sm text-foreground placeholder:text-placeholder focus:border-ring focus:outline-none"
          />
        </div>

        <ul className="mt-4 flex flex-col gap-2">
          {filteredMembers.map((member) => (
            <li
              key={member.id}
              className="flex flex-wrap items-center justify-between gap-3 rounded-lg border border-border px-3 py-2.5"
            >
              <div className="flex items-center gap-3">
                <span className="flex h-9 w-9 items-center justify-center rounded-full bg-primary text-sm font-semibold text-on-primary">
                  {member.name.slice(0, 1)}
                </span>
                <div>
                  <p className="text-sm font-medium text-foreground">
                    {member.name}
                    {member.isProjectAdmin && (
                      <span className="ml-2 rounded-full bg-accent-subtle px-2 py-0.5 text-[10px] font-semibold text-accent">
                        PM
                      </span>
                    )}
                  </p>
                  <p className="text-xs text-muted">{member.email}</p>
                </div>
              </div>

              <div className="flex items-center gap-2">
                <select
                  aria-label={`${member.name} 부서`}
                  value={member.departmentId ?? ""}
                  onChange={(e) => setMemberDepartment(member.id, e.target.value)}
                  className="rounded-md border border-border bg-surface px-2 py-1.5 text-xs text-foreground focus:border-ring focus:outline-none"
                >
                  {MOCK_DEPARTMENTS.map((d) => (
                    <option key={d.id} value={d.id}>
                      {d.name}
                    </option>
                  ))}
                </select>

                <select
                  aria-label={`${member.name} 회사 역할`}
                  value={member.companyRole}
                  onChange={(e) => setMemberRole(member.id, e.target.value as CompanyRole)}
                  className="rounded-md border border-border bg-surface px-2 py-1.5 text-xs text-foreground focus:border-ring focus:outline-none"
                >
                  <option value="COMPANY_ADMIN">COMPANY_ADMIN</option>
                  <option value="MEMBER">MEMBER</option>
                </select>

                <Link
                  href={`/${companyId}/dm/${member.id}`}
                  className="rounded-md px-2.5 py-1.5 text-xs font-medium text-primary transition-colors duration-fast hover:bg-primary-subtle"
                >
                  DM
                </Link>
              </div>
            </li>
          ))}
          {filteredMembers.length === 0 && (
            <li className="rounded-lg border border-dashed border-border px-3 py-4 text-center text-sm text-muted">
              검색 결과가 없습니다.
            </li>
          )}
        </ul>
      </section>
    </div>
  );
}
