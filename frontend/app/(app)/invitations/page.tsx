"use client";

/*
 * S14 수신 — 내가 받은 초대 목록 (Sprint 1.5)
 *
 * company-agnostic 경로(/invitations): 초대 수신자는 아직 그 회사 membership이
 * 없어 /{companyId}/... 하위로 진입시킬 수 없다. 설계 근거는
 * docs/08-members/member-정철진/기술-연구/s14-invitation-프론트-연동-설계.md §1.
 *
 * ⚠️ accept 성공 후에도 로그인 세션(AuthContext.companyMemberships)은 갱신되지
 *    않는다. AuthContext는 로그인/세션 복원 시점에만
 *    GET /users/me/company-memberships를 호출하고(AuthContext.tsx 참고),
 *    이 화면의 accept 액션 후 자동 재조회는 하지 않으므로 새 membership을
 *    실제로 쓰려면(RailShell 등에서) 재로그인이 필요하다.
 */
import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { invitationService } from "@/services/invitation.service";
import {
  INVITATION_STATUS_CLASS,
  INVITATION_STATUS_LABEL,
  formatExpiresAt,
  invitationErrorMessage,
  isInvitationExpired,
} from "@/lib/invitation";
import type { InvitationResponse } from "@/types/invitation.types";

type LoadState = "loading" | "ready" | "error";

export default function InvitationsPage() {
  const [invitations, setInvitations] = useState<InvitationResponse[]>([]);
  const [loadState, setLoadState] = useState<LoadState>("loading");
  const [loadError, setLoadError] = useState<string | null>(null);
  // action 진행 중인 invitationId. 중복 클릭 방지 + 버튼별 로딩 표시에 쓴다.
  const [actionId, setActionId] = useState<number | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  // accept 성공 후 안내 배너. AuthContext.companies가 즉시 안 바뀌므로(주석 참고) 필요하다.
  const [acceptNotice, setAcceptNotice] = useState<string | null>(null);

  const fetchInvitations = useCallback(async () => {
    setLoadState("loading");
    setLoadError(null);
    try {
      const { data } = await invitationService.listMine();
      setInvitations(data.data.invitations);
      setLoadState("ready");
    } catch (error) {
      setLoadError(invitationErrorMessage(error, "초대 목록을 불러오지 못했습니다."));
      setLoadState("error");
    }
  }, []);

  // 마운트 시 1회 로드. AuthContext.tsx와 동일하게 인라인 IIFE로 감싼다
  // (fetchInvitations를 직접 호출하면 react-hooks/set-state-in-effect에 걸린다).
  useEffect(() => {
    (async () => {
      await fetchInvitations();
    })();
  }, [fetchInvitations]);

  const respond = async (invitation: InvitationResponse, action: "accept" | "decline") => {
    setActionError(null);
    setActionId(invitation.id);
    try {
      const { data } =
        action === "accept"
          ? await invitationService.accept(invitation.id)
          : await invitationService.decline(invitation.id);
      setInvitations((prev) =>
        prev.map((i) => (i.id === invitation.id ? { ...i, status: data.data.status } : i))
      );
      if (action === "accept") {
        setAcceptNotice(
          `${invitation.companyName} 소속이 반영됐습니다. 새 회사는 재로그인 후 확인할 수 있습니다.`
        );
      }
    } catch (error) {
      setActionError(
        invitationErrorMessage(
          error,
          action === "accept" ? "초대 수락에 실패했습니다." : "초대 거절에 실패했습니다."
        )
      );
      // 최신 상태(만료/취소 등으로 이미 바뀌었을 수 있음)를 다시 반영한다.
      fetchInvitations();
    } finally {
      setActionId(null);
    }
  };

  return (
    <div className="mx-auto max-w-2xl px-6 py-10">
      <Link
        href="/hub"
        className="text-sm text-muted transition-colors duration-fast hover:text-primary"
      >
        ← 허브로
      </Link>
      <div className="mt-3">
        <h1 className="text-xl font-bold text-foreground">받은 초대</h1>
        <p className="mt-1 text-sm text-muted">회사에서 보낸 초대를 확인하고 수락 또는 거절합니다.</p>
      </div>

      {actionError && <p className="mt-4 text-sm text-destructive">{actionError}</p>}

      {acceptNotice && (
        <p className="mt-4 rounded-lg border border-primary/40 bg-primary-subtle px-4 py-3 text-sm text-foreground">
          {acceptNotice}
        </p>
      )}

      {loadState === "loading" && (
        <p className="mt-6 text-sm text-muted">불러오는 중...</p>
      )}

      {loadState === "error" && (
        <div className="mt-6 rounded-xl border border-destructive-subtle bg-surface p-6 text-sm text-destructive">
          {loadError}
        </div>
      )}

      {loadState === "ready" && (
        <ul className="mt-6 flex flex-col gap-3">
          {invitations.map((invitation) => {
            const expired = isInvitationExpired(invitation);
            const canRespond = invitation.status === "PENDING" && !expired;
            const busy = actionId === invitation.id;
            return (
              <li
                key={invitation.id}
                className="rounded-xl border border-border bg-surface p-5"
              >
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <p className="text-sm font-semibold text-foreground">{invitation.companyName}</p>
                    <p className="mt-1 text-xs text-muted">
                      {invitation.inviterNickname} 님이 {invitation.role === "COMPANY_ADMIN" ? "관리자" : "구성원"}(으)로 초대했습니다
                    </p>
                    <p className="mt-1 text-xs text-muted">{formatExpiresAt(invitation.expiresAt)}</p>
                  </div>
                  <span
                    className={`shrink-0 rounded-full px-2.5 py-0.5 text-xs font-medium ${INVITATION_STATUS_CLASS[invitation.status]}`}
                  >
                    {expired ? INVITATION_STATUS_LABEL.EXPIRED : INVITATION_STATUS_LABEL[invitation.status]}
                  </span>
                </div>

                {canRespond && (
                  <div className="mt-4 flex justify-end gap-2">
                    <button
                      onClick={() => respond(invitation, "decline")}
                      disabled={busy}
                      className="rounded-md px-3 py-1.5 text-sm font-medium text-muted transition-colors duration-fast hover:bg-primary-subtle disabled:opacity-50"
                    >
                      거절
                    </button>
                    <button
                      onClick={() => respond(invitation, "accept")}
                      disabled={busy}
                      className="rounded-md btn-gradient px-3 py-1.5 text-sm font-semibold text-primary-foreground disabled:opacity-50"
                    >
                      수락
                    </button>
                  </div>
                )}
              </li>
            );
          })}
          {invitations.length === 0 && (
            <li className="rounded-lg border border-dashed border-border px-3 py-6 text-center text-sm text-muted">
              받은 초대가 없습니다.
            </li>
          )}
        </ul>
      )}
    </div>
  );
}
