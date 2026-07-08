"use client";

/*
 * S14 발신·관리 — 구성원·권한 관리
 *
 * 접근 사용자: C(COMPANY_ADMIN)만. 화면정의서(docs/03-design/화면정의서.md §S14) 기준
 * 구성원 목록(역할/부서 변경, DM 시작) + 구성원 추가(이메일 조회 → 초대) +
 * 보낸 초대 목록을 구현한다.
 *
 * Invitation 흐름(이메일 조회/생성/목록/취소)은 WBS 2.2~2.4 backend(PR #26, #47,
 * #48) 기준 실 API 연동이다. 계약/에러코드/상태 전이는
 * docs/08-members/member-정철진/기술-연구/s14-invitation-프론트-연동-설계.md 참고.
 *
 * "내가 받은 초대" 수락/거절은 이 화면이 아니라 company-agnostic한 /invitations
 * (S14 수신)에서 처리한다 — 초대 수신자는 이 회사의 membership이 아직 없어
 * /{companyId}/... 하위로 못 들어오기 때문이다(위 설계 문서 §1).
 *
 * ⚠️ MOCK 유지 (Sprint 2 범위): 구성원 목록 조회(`GET /company-members`)와
 * 역할/부서 변경(`PATCH /company-members/{id}`)은 WBS 3.1.4 backend가 아직
 * 없어서 로컬 state로 유지한다. 부서 목록도 S13과 공유 저장소가 없어 별도 목업.
 *
 * ⚠️ MOCK_ROLE: 현재 로그인 사용자의 이 회사 내 role. Sprint 1 `/auth/me`는
 * companyMemberships를 안 내려주므로(AuthContext.tsx 참고) 실제 role을 아직
 * 못 구한다. RBAC 적용(WBS 3.1.4/3.2.4) 전까지 다른 화면과 동일하게 "C"로
 * 고정한다. getMockRole() 우회는 PR #45(TypeScript literal narrowing) 참고.
 *
 * ⚠️ 알려진 gap: 위 고정값 때문에 실제로는 A/P 역할 사용자가 이 화면에
 * 들어와도 프론트에서는 "구성원 추가"(관리자 전용 UI)가 그대로 노출된다.
 * 실제 요청은 백엔드 `assertCompanyAdmin`이 막아 403으로 거절하므로 보안
 * 문제는 아니지만(리뷰 PR #55 참고), UX상 권한 없는 사용자에게 관리자
 * 버튼이 보이는 상태다. RBAC 연동(WBS 3.2.4)에서 role 기반 조건부 렌더링을
 * 실제 값으로 교체하면 해결된다.
 */
import { useCallback, useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { invitationService } from "@/services/invitation.service";
import {
  INVITATION_STATUS_CLASS,
  INVITATION_STATUS_LABEL,
  invitationErrorMessage,
} from "@/lib/invitation";
import type { InvitationResponse, InvitationRole } from "@/types/invitation.types";

type Role = "A" | "P" | "C";
const getMockRole = (): Role => "C";
const MOCK_ROLE = getMockRole();
const canManageMembers = MOCK_ROLE === "C";

type CompanyRole = "COMPANY_ADMIN" | "MEMBER";

type Member = {
  id: string;
  name: string;
  email: string;
  companyRole: CompanyRole;
  departmentId: string | null;
  isProjectAdmin: boolean;
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

type LoadState = "loading" | "ready" | "error";

export default function MembersPage() {
  const params = useParams<{ companyId: string }>();
  const companyId = params.companyId;

  const [members, setMembers] = useState<Member[]>(MOCK_MEMBERS);
  const [query, setQuery] = useState("");

  const [invitations, setInvitations] = useState<InvitationResponse[]>([]);
  const [invitationsState, setInvitationsState] = useState<LoadState>("loading");
  const [invitationsError, setInvitationsError] = useState<string | null>(null);

  const [inviteEmail, setInviteEmail] = useState("");
  const [inviteRole, setInviteRole] = useState<InvitationRole>("MEMBER");
  const [inviteError, setInviteError] = useState<string | null>(null);
  const [lookupBusy, setLookupBusy] = useState(false);
  // lookup 성공한 등록 사용자 — 확인 modal용
  const [pendingInvitee, setPendingInvitee] = useState<{ id: number; email: string } | null>(null);
  const [createBusy, setCreateBusy] = useState(false);
  const [cancelBusyId, setCancelBusyId] = useState<number | null>(null);

  const fetchInvitations = useCallback(async () => {
    setInvitationsState("loading");
    setInvitationsError(null);
    try {
      const { data } = await invitationService.listCompany(companyId);
      setInvitations(data.data.invitations);
      setInvitationsState("ready");
    } catch (error) {
      setInvitationsError(invitationErrorMessage(error, "초대 목록을 불러오지 못했습니다."));
      setInvitationsState("error");
    }
  }, [companyId]);

  // 마운트 시 1회 로드. AuthContext.tsx와 동일하게 인라인 IIFE로 감싼다
  // (fetchInvitations를 직접 호출하면 react-hooks/set-state-in-effect에 걸린다).
  useEffect(() => {
    if (!canManageMembers) return;
    (async () => {
      await fetchInvitations();
    })();
  }, [fetchInvitations]);

  const filteredMembers = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return members;
    return members.filter(
      (m) => m.name.toLowerCase().includes(q) || m.email.toLowerCase().includes(q)
    );
  }, [members, query]);

  const setMemberRole = (id: string, role: CompanyRole) => {
    // TODO(Sprint 2, WBS 3.1.4): PATCH /api/v1/company-members/{companyMemberId}
    setMembers((prev) => prev.map((m) => (m.id === id ? { ...m, companyRole: role } : m)));
  };

  const setMemberDepartment = (id: string, departmentId: string) => {
    // TODO(Sprint 2, WBS 3.1.4): PATCH /api/v1/company-members/{companyMemberId}
    setMembers((prev) => prev.map((m) => (m.id === id ? { ...m, departmentId } : m)));
  };

  const lookupEmail = async () => {
    const email = inviteEmail.trim().toLowerCase();
    setInviteError(null);
    setPendingInvitee(null);
    if (!email) return;

    setLookupBusy(true);
    try {
      const { data } = await invitationService.lookup(email);
      setPendingInvitee({ id: data.data.user.id, email: data.data.user.email });
    } catch (error) {
      setInviteError(
        invitationErrorMessage(
          error,
          "등록된 사용자를 찾을 수 없습니다. (Sprint 1.5 범위 밖: 외부 이메일 초대 미지원)"
        )
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
      await invitationService.create(companyId, {
        inviteeUserId: pendingInvitee.id,
        role: inviteRole,
      });
      setPendingInvitee(null);
      setInviteEmail("");
      await fetchInvitations();
    } catch (error) {
      setInviteError(invitationErrorMessage(error, "초대 생성에 실패했습니다."));
    } finally {
      setCreateBusy(false);
    }
  };

  const cancelInvitation = async (invitationId: number) => {
    setCancelBusyId(invitationId);
    try {
      const { data } = await invitationService.cancel(invitationId);
      setInvitations((prev) =>
        prev.map((i) => (i.id === invitationId ? { ...i, status: data.data.status } : i))
      );
    } catch (error) {
      setInvitationsError(invitationErrorMessage(error, "초대 취소에 실패했습니다."));
      setInvitationsState("error");
    } finally {
      setCancelBusyId(null);
    }
  };

  return (
    <div className="mx-auto max-w-4xl px-6 py-10">
      <Link
        href={`/${companyId}/settings`}
        className="text-sm text-muted transition-colors duration-fast hover:text-primary"
      >
        ← 회사 관리
      </Link>
      <div className="mt-3 flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-foreground">구성원·권한 관리</h1>
          <p className="mt-1 text-sm text-muted">회사 구성원을 초대하고 역할·부서를 관리합니다.</p>
        </div>
      </div>

      {/* 구성원 추가 — COMPANY_ADMIN 전용 */}
      {canManageMembers && (
        <section className="mt-6 rounded-xl border border-border bg-surface p-6">
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
            <select
              aria-label="초대 역할"
              value={inviteRole}
              onChange={(e) => setInviteRole(e.target.value as InvitationRole)}
              className="rounded-lg border border-border bg-surface px-2 py-2 text-sm text-foreground focus:border-ring focus:outline-none"
            >
              <option value="MEMBER">MEMBER</option>
              <option value="COMPANY_ADMIN">COMPANY_ADMIN</option>
            </select>
            <button
              onClick={lookupEmail}
              disabled={lookupBusy}
              className="rounded-lg btn-gradient px-4 py-2 text-sm font-semibold text-primary-foreground transition-colors duration-fast disabled:opacity-50"
            >
              조회
            </button>
          </div>

          {inviteError && <p className="mt-2 text-xs text-destructive">{inviteError}</p>}

          {pendingInvitee && (
            <div className="mt-3 flex items-center justify-between rounded-lg border border-primary/40 bg-primary-subtle px-4 py-3">
              <span className="text-sm text-foreground">
                <strong>{pendingInvitee.email}</strong> 사용자에게 {inviteRole} 역할로 초대를 보낼까요?
              </span>
              <div className="flex gap-2">
                <button
                  onClick={() => setPendingInvitee(null)}
                  className="rounded-md px-3 py-1.5 text-sm font-medium text-muted hover:bg-surface"
                >
                  취소
                </button>
                <button
                  onClick={confirmInvite}
                  disabled={createBusy}
                  className="rounded-md btn-gradient px-3 py-1.5 text-sm font-semibold text-primary-foreground disabled:opacity-50"
                >
                  초대 보내기
                </button>
              </div>
            </div>
          )}

          {invitationsState === "loading" && (
            <p className="mt-4 text-xs text-muted">초대 목록을 불러오는 중...</p>
          )}
          {invitationsState === "error" && (
            <p className="mt-4 text-xs text-destructive">{invitationsError}</p>
          )}
          {invitationsState === "ready" && invitations.length > 0 && (
            <ul className="mt-4 divide-y divide-border">
              {invitations.map((inv) => (
                <li key={inv.id} className="flex items-center justify-between px-1 py-2.5">
                  <span className="text-sm text-foreground">
                    {inv.inviteeNickname} <span className="text-muted">({inv.inviteeEmail})</span>
                  </span>
                  <div className="flex items-center gap-2">
                    <span
                      className={`rounded-full px-2.5 py-0.5 text-xs font-medium ${INVITATION_STATUS_CLASS[inv.status]}`}
                    >
                      {INVITATION_STATUS_LABEL[inv.status]}
                    </span>
                    {inv.status === "PENDING" && (
                      <button
                        onClick={() => cancelInvitation(inv.id)}
                        disabled={cancelBusyId === inv.id}
                        className="text-xs font-medium text-destructive transition-colors duration-fast hover:opacity-70 disabled:opacity-50"
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
      )}

      {/* 구성원 목록 — Sprint 2(WBS 3.1.4) 전까지 mock */}
      <section className="mt-6 rounded-xl border border-border bg-surface p-6">
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

        <ul className="mt-4 divide-y divide-border">
          {filteredMembers.map((member) => (
            <li
              key={member.id}
              className="flex flex-wrap items-center justify-between gap-3 px-1 py-3"
            >
              <div className="flex items-center gap-3">
                <span className="flex h-9 w-9 items-center justify-center rounded-full avatar-ring text-sm font-semibold">
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
