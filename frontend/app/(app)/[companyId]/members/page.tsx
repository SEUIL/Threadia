"use client";

/*
 * S14 발신·관리 — 구성원·권한 관리
 *
 * 접근 사용자: C(COMPANY_ADMIN)만. 화면정의서(docs/03-design/화면정의서.md §S14) 기준
 * 구성원 목록(역할/부서 변경, DM 시작) + 구성원 추가(이메일 조회 → 초대) +
 * 보낸 초대 목록을 구현한다.
 *
 * Invitation 흐름(이메일 조회/생성/목록/취소)은 WBS 2.2~2.4 backend 기준 실 API
 * 연동이다. 계약/에러코드/상태 전이는
 * docs/08-members/member-정철진/기술-연구/s14-invitation-프론트-연동-설계.md 참고.
 *
 * "내가 받은 초대" 수락/거절은 이 화면이 아니라 company-agnostic한 /invitations
 * (S14 수신)에서 처리한다 — 초대 수신자는 이 회사의 membership이 아직 없어
 * /{companyId}/... 하위로 못 들어오기 때문이다(위 설계 문서 §1).
 *
 * WBS 3.2.3 실 연동: 구성원 목록/역할·부서 변경은 Company Member API(3.1.4,
 * PR #56), 부서 목록은 Department API(3.1.3, PR #54)를 쓴다. role은 §8.2
 * 통합(AuthContext.companyMemberships)로 실제 값을 쓴다. 근거·계약은
 * docs/08-members/member-정철진/기술-연구/sprint2-3.2-company-frontend-rbac-사전조사.md
 * §5, §8.5.
 *
 * ⚠️ PM 배지 제거: mock에 있던 isProjectAdmin은 Project Member API(3.3.4,
 * 아직 없음) 없이는 얻을 수 없는 정보라 임의로 만들지 않고 뺐다.
 *
 * ⚠️ PATCH는 partial이 아니라 full-replace다(role/status 둘 다 @NotNull).
 * updateMember()가 역할·부서 select 어느 쪽이 바뀌든 항상 현재
 * role/status/departmentId 전체를 합쳐서 보낸다. status(정지/탈퇴) 변경
 * UI는 아직 없어 항상 현재 status를 그대로 유지해서 보낸다(설계문서 §9 미결).
 *
 * 구성원 제거: 백엔드 DELETE endpoint(PR #181)는 있었는데 이 화면에 액션이 없어서
 * 못 쓰고 있었다 — 코드 vs WBS 감사 중 발견. soft remove(status=LEFT)라 서버가
 * 자기 자신/마지막 ACTIVE COMPANY_ADMIN/어느 프로젝트의 마지막 ACTIVE
 * PROJECT_ADMIN 제거를 409로 막아준다(API-명세서 §3.1.4) — 프론트는 그 에러
 * 메시지만 그대로 보여주면 된다. 실수 클릭 방지로 인라인 2단계 확인을 둔다(이
 * 코드베이스에 window.confirm 사용례가 없어 pendingInvitee와 같은 패턴을 따른다).
 */
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useAuth } from "@/contexts/AuthContext";
import { invitationService } from "@/services/invitation.service";
import { companyMemberService } from "@/services/company-member.service";
import { departmentService } from "@/services/department.service";
import { useStartDmMutation } from "@/hooks/useDmQueries";
import {
  INVITATION_STATUS_CLASS,
  INVITATION_STATUS_LABEL,
  invitationErrorMessage,
} from "@/lib/invitation";
import type { CompanyInvitationRole, InvitationResponse } from "@/types/invitation.types";
import type { CompanyMemberRole, CompanyMemberResponse, DepartmentResponse } from "@/types/company.types";

type LoadState = "loading" | "ready" | "error";

export default function MembersPage() {
  const params = useParams<{ companyId: string }>();
  const router = useRouter();
  const companyId = params.companyId;
  const { companyMemberships } = useAuth();
  const canManageMembers =
    companyMemberships.find((m) => m.companyId === Number(companyId))?.role === "COMPANY_ADMIN";

  const [members, setMembers] = useState<CompanyMemberResponse[]>([]);
  const [departments, setDepartments] = useState<DepartmentResponse[]>([]);
  const [membersState, setMembersState] = useState<LoadState>("loading");
  const [membersError, setMembersError] = useState<string | null>(null);
  const [updateBusyId, setUpdateBusyId] = useState<number | null>(null);
  const [updateError, setUpdateError] = useState<string | null>(null);
  const [query, setQuery] = useState("");
  const [dmError, setDmError] = useState<string | null>(null);
  const [removeConfirmId, setRemoveConfirmId] = useState<number | null>(null);
  const [removeBusyId, setRemoveBusyId] = useState<number | null>(null);
  const [removeError, setRemoveError] = useState<string | null>(null);

  const [invitations, setInvitations] = useState<InvitationResponse[]>([]);
  const [invitationsState, setInvitationsState] = useState<LoadState>("loading");
  const [invitationsError, setInvitationsError] = useState<string | null>(null);

  const [inviteEmail, setInviteEmail] = useState("");
  const [inviteRole, setInviteRole] = useState<CompanyInvitationRole>("MEMBER");
  const [inviteError, setInviteError] = useState<string | null>(null);
  const [lookupBusy, setLookupBusy] = useState(false);
  // lookup 성공한 등록 사용자 — 확인 modal용
  const [pendingInvitee, setPendingInvitee] = useState<{ id: number; email: string } | null>(null);
  const [createBusy, setCreateBusy] = useState(false);
  const [cancelBusyId, setCancelBusyId] = useState<number | null>(null);
  // 목록 자체 로딩 실패(invitationsState)와 별개로 관리한다 — 같은 state를 쓰면
  // action 실패 하나가 렌더 조건(invitationsState === "ready")을 깨서 목록
  // 전체가 사라지는 회귀가 생긴다(PR #55 리뷰, 김민정).
  const [cancelError, setCancelError] = useState<string | null>(null);

  const fetchMembers = useCallback(async () => {
    setMembersState("loading");
    setMembersError(null);
    try {
      const [membersRes, departmentsRes] = await Promise.all([
        companyMemberService.list(companyId),
        departmentService.list(companyId),
      ]);
      setMembers(membersRes.data.data.companyMembers);
      setDepartments(departmentsRes.data.data.departments);
      setMembersState("ready");
    } catch (error) {
      setMembersError(invitationErrorMessage(error, "구성원 목록을 불러오지 못했습니다."));
      setMembersState("error");
    }
  }, [companyId]);

  // silent: 취소 실패 후 재검증처럼 이미 목록이 떠 있는 상태에서 다시 부를 때 쓴다.
  // invitationsState를 "loading"으로 안 바꿔서 목록이 순간적으로 사라지는 깜빡임을 막는다.
  const fetchInvitations = useCallback(
    async (options?: { silent?: boolean }) => {
      if (!options?.silent) {
        setInvitationsState("loading");
      }
      setInvitationsError(null);
      try {
        const { data } = await invitationService.listCompany(companyId);
        setInvitations(data.data.invitations);
        setInvitationsState("ready");
        // 재조회 성공 = 최신 상태 확보. 이전 취소 실패 메시지는 여기서만 지운다
        // (cancelInvitation의 setCancelError 직후에 지우면 배칭 때문에 메시지가
        // 아예 안 뜰 수 있다).
        setCancelError(null);
      } catch (error) {
        setInvitationsError(invitationErrorMessage(error, "초대 목록을 불러오지 못했습니다."));
        setInvitationsState("error");
      }
    },
    [companyId]
  );

  // 마운트 시 1회 로드. AuthContext.tsx와 동일하게 인라인 IIFE로 감싼다
  // (직접 호출하면 react-hooks/set-state-in-effect에 걸린다).
  useEffect(() => {
    (async () => {
      await fetchMembers();
      if (canManageMembers) await fetchInvitations();
    })();
  }, [fetchMembers, fetchInvitations, canManageMembers]);

  const filteredMembers = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return members;
    return members.filter(
      (m) =>
        m.user.nickname.toLowerCase().includes(q) || m.user.email.toLowerCase().includes(q)
    );
  }, [members, query]);

  // role/departmentId 어느 쪽이 바뀌든 현재 값 전체를 합쳐서 보낸다(PATCH full-replace).
  const updateMember = async (
    member: CompanyMemberResponse,
    partial: { role?: CompanyMemberRole; departmentId?: number | null }
  ) => {
    setUpdateError(null);
    setUpdateBusyId(member.id);
    try {
      const { data } = await companyMemberService.update(companyId, member.id, {
        role: partial.role ?? member.role,
        status: member.status,
        departmentId: partial.departmentId !== undefined ? partial.departmentId : member.departmentId,
      });
      setMembers((prev) => prev.map((m) => (m.id === member.id ? data.data.companyMember : m)));
    } catch (error) {
      setUpdateError(invitationErrorMessage(error, "구성원 정보 변경에 실패했습니다."));
    } finally {
      setUpdateBusyId(null);
    }
  };

  /*
   * 재리뷰 반영(PR #173, pktewon): 이전에는 `/${companyId}/dm/${member.user.id}`
   * 로 바로 링크해서, DM room id와 상대 user id를 같은 것으로 취급하는 잘못이
   * 있었다(우연히 같은 숫자대일 때만 동작). DM room id는 별도 개념이라
   * get-or-create API로 실제 room을 만들거나 찾은 뒤 그 room.id로 이동해야 한다.
   *
   * React Query 마이그레이션(이슈 #217): dm/layout.tsx와 같은
   * useStartDmMutation을 쓴다. mutation.isPending 하나로 "요청 중 다른 DM
   * 버튼도 비활성화"(4차 재리뷰 blocker 4)가 자연히 된다 — 모든 버튼이 같은
   * mutation 인스턴스의 isPending을 보고 disabled되므로, 겹쳐 누르는 것
   * 자체가 안 된다(예전의 dmRequestSequenceRef/mountedRef는 그래서 필요 없다).
   */
  // 이슈 #217 코멘트(dhy257)의 late navigation 지적 반영 — React Query가
  // mutation/cache는 관리해도 router.push() 같은 side effect는 취소해주지
  // 않는다. 응답을 기다리는 동안 사용자가 이 화면을 벗어나면(언마운트) 늦게
  // 온 응답이 뒤늦게 DM 방으로 강제 이동시키면 안 된다.
  const mountedRef = useRef(true);
  useEffect(() => {
    mountedRef.current = true;
    return () => {
      mountedRef.current = false;
    };
  }, []);

  const startDmMutation = useStartDmMutation(companyId);
  const startDm = async (otherUserId: number) => {
    if (startDmMutation.isPending) return;
    setDmError(null);
    try {
      const response = await startDmMutation.mutateAsync(otherUserId);
      if (!mountedRef.current) return;
      router.push(`/${companyId}/dm/${response.data.data.room.id}`);
    } catch (error) {
      if (!mountedRef.current) return;
      setDmError(invitationErrorMessage(error, "대화를 시작하지 못했습니다."));
    }
  };

  const removeMember = async (member: CompanyMemberResponse) => {
    setRemoveError(null);
    setRemoveBusyId(member.id);
    try {
      await companyMemberService.remove(companyId, member.id);
      setMembers((prev) => prev.filter((m) => m.id !== member.id));
      setRemoveConfirmId(null);
    } catch (error) {
      setRemoveError(invitationErrorMessage(error, "구성원 제거에 실패했습니다."));
    } finally {
      setRemoveBusyId(null);
    }
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
    setCancelError(null);
    setCancelBusyId(invitationId);
    try {
      const { data } = await invitationService.cancel(invitationId);
      setInvitations((prev) =>
        prev.map((i) => (i.id === invitationId ? { ...i, status: data.data.status } : i))
      );
    } catch (error) {
      setCancelError(invitationErrorMessage(error, "초대 취소에 실패했습니다."));
      // 최신 상태(다른 경로로 이미 바뀌었을 수 있음)를 다시 반영한다. silent라
      // 목록이 화면에서 사라지지 않고 그대로 있다가 새 데이터로 갱신된다.
      fetchInvitations({ silent: true });
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
              onChange={(e) => setInviteRole(e.target.value as CompanyInvitationRole)}
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
          {cancelError && <p className="mt-4 text-xs text-destructive">{cancelError}</p>}
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

      {/* 구성원 목록 */}
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

        {membersState === "loading" && <p className="mt-4 text-xs text-muted">불러오는 중...</p>}
        {membersState === "error" && <p className="mt-4 text-xs text-destructive">{membersError}</p>}
        {updateError && <p className="mt-4 text-xs text-destructive">{updateError}</p>}
        {dmError && <p className="mt-4 text-xs text-destructive">{dmError}</p>}
        {removeError && <p className="mt-4 text-xs text-destructive">{removeError}</p>}

        {membersState === "ready" && (
          <ul className="mt-4 divide-y divide-border">
            {filteredMembers.map((member) => (
              <li
                key={member.id}
                className="flex flex-wrap items-center justify-between gap-3 px-1 py-3"
              >
                <div className="flex items-center gap-3">
                  <span className="flex h-9 w-9 items-center justify-center rounded-full avatar-ring text-sm font-semibold">
                    {member.user.nickname.slice(0, 1)}
                  </span>
                  <div>
                    <p className="text-sm font-medium text-foreground">{member.user.nickname}</p>
                    <p className="text-xs text-muted">{member.user.email}</p>
                  </div>
                </div>

                <div className="flex items-center gap-2">
                  <select
                    aria-label={`${member.user.nickname} 부서`}
                    value={member.departmentId ?? ""}
                    disabled={!canManageMembers || updateBusyId === member.id}
                    onChange={(e) =>
                      updateMember(member, {
                        departmentId: e.target.value ? Number(e.target.value) : null,
                      })
                    }
                    className="rounded-md border border-border bg-surface px-2 py-1.5 text-xs text-foreground focus:border-ring focus:outline-none disabled:opacity-50"
                  >
                    <option value="">미배정</option>
                    {departments.map((d) => (
                      <option key={d.id} value={d.id}>
                        {d.name}
                      </option>
                    ))}
                  </select>

                  <select
                    aria-label={`${member.user.nickname} 회사 역할`}
                    value={member.role}
                    disabled={!canManageMembers || updateBusyId === member.id}
                    onChange={(e) => updateMember(member, { role: e.target.value as CompanyMemberRole })}
                    className="rounded-md border border-border bg-surface px-2 py-1.5 text-xs text-foreground focus:border-ring focus:outline-none disabled:opacity-50"
                  >
                    <option value="COMPANY_ADMIN">COMPANY_ADMIN</option>
                    <option value="MEMBER">MEMBER</option>
                  </select>

                  <button
                    type="button"
                    onClick={() => void startDm(member.user.id)}
                    disabled={startDmMutation.isPending}
                    className="rounded-md px-2.5 py-1.5 text-xs font-medium text-primary transition-colors duration-fast hover:bg-primary-subtle disabled:opacity-50"
                  >
                    DM
                  </button>

                  {canManageMembers &&
                    (removeConfirmId === member.id ? (
                      <span className="flex items-center gap-1">
                        <span className="text-xs text-muted">정말 제거?</span>
                        <button
                          type="button"
                          onClick={() => void removeMember(member)}
                          disabled={removeBusyId === member.id}
                          className="rounded-md px-2 py-1 text-xs font-semibold text-destructive transition-colors duration-fast hover:bg-destructive-subtle disabled:opacity-50"
                        >
                          {removeBusyId === member.id ? "제거 중..." : "확인"}
                        </button>
                        <button
                          type="button"
                          onClick={() => setRemoveConfirmId(null)}
                          disabled={removeBusyId === member.id}
                          className="rounded-md px-2 py-1 text-xs font-medium text-muted transition-colors duration-fast hover:bg-primary-subtle disabled:opacity-50"
                        >
                          취소
                        </button>
                      </span>
                    ) : (
                      <button
                        type="button"
                        onClick={() => setRemoveConfirmId(member.id)}
                        className="rounded-md px-2.5 py-1.5 text-xs font-medium text-destructive transition-colors duration-fast hover:bg-destructive-subtle"
                      >
                        제거
                      </button>
                    ))}
                </div>
              </li>
            ))}
            {filteredMembers.length === 0 && (
              <li className="rounded-lg border border-dashed border-border px-3 py-4 text-center text-sm text-muted">
                검색 결과가 없습니다.
              </li>
            )}
          </ul>
        )}
      </section>
    </div>
  );
}
