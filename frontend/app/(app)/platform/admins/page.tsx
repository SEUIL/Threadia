"use client";

/*
 * S15 플랫폼 관리자 목록/lifecycle (WBS 4.10.5, §10-C)
 *
 * 조회(actor/admins)는 PLATFORM_ADMIN도 가능하지만 grant/revoke/reactivate/
 * role 변경은 전부 SUPER_ADMIN 전용(backend requireSuperAdminFresh)이라
 * PLATFORM_ADMIN에게는 액션 UI를 숨긴다. userId로 직접 grant하는 것은
 * email lookup API가 없기 때문 — GrantPlatformAdminRequest 계약이 userId만
 * 받는다(4.10.4 backend 고정).
 */
import { useCallback, useEffect, useState } from "react";
import { platformActorService } from "@/services/platform-actor.service";
import { platformErrorMessage, isValidPlatformReason } from "@/lib/platform";
import { usePlatformAdmin } from "@/contexts/PlatformAdminContext";
import type { PlatformAdminResponse, PlatformAdminRole } from "@/types/platform.types";

type LoadState = "loading" | "ready" | "error";

export default function PlatformAdminsPage() {
  const { actor, isSuperAdmin } = usePlatformAdmin();
  const [admins, setAdmins] = useState<PlatformAdminResponse[]>([]);
  const [state, setState] = useState<LoadState>("loading");
  const [error, setError] = useState<string | null>(null);

  const [grantUserId, setGrantUserId] = useState("");
  const [grantRole, setGrantRole] = useState<PlatformAdminRole>("PLATFORM_ADMIN");
  const [grantReason, setGrantReason] = useState("");
  const [grantBusy, setGrantBusy] = useState(false);
  const [grantError, setGrantError] = useState<string | null>(null);

  const [actionId, setActionId] = useState<number | null>(null);
  const [actionReason, setActionReason] = useState("");
  const [actionKind, setActionKind] = useState<"revoke" | "reactivate" | "role" | null>(null);
  const [actionRole, setActionRole] = useState<PlatformAdminRole>("PLATFORM_ADMIN");
  const [actionBusy, setActionBusy] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);

  const fetchAdmins = useCallback(async () => {
    setState("loading");
    setError(null);
    try {
      const { data } = await platformActorService.listAdmins();
      setAdmins(data.data.admins);
      setState("ready");
    } catch (err) {
      setError(platformErrorMessage(err, "관리자 목록을 불러오지 못했습니다."));
      setState("error");
    }
  }, []);

  useEffect(() => {
    (async () => {
      await fetchAdmins();
    })();
  }, [fetchAdmins]);

  const submitGrant = async () => {
    setGrantError(null);
    const userId = Number(grantUserId);
    if (!grantUserId.trim() || !Number.isInteger(userId) || userId <= 0) {
      setGrantError("올바른 사용자 ID를 입력해 주세요.");
      return;
    }
    if (!isValidPlatformReason(grantReason)) {
      setGrantError("사유는 3~500자로 입력해 주세요.");
      return;
    }
    setGrantBusy(true);
    try {
      const { data } = await platformActorService.grant({ userId, role: grantRole, reason: grantReason.trim() });
      setAdmins((prev) => [data.data, ...prev]);
      setGrantUserId("");
      setGrantReason("");
    } catch (err) {
      setGrantError(platformErrorMessage(err, "관리자 부여에 실패했습니다."));
    } finally {
      setGrantBusy(false);
    }
  };

  const openAction = (admin: PlatformAdminResponse, kind: "revoke" | "reactivate" | "role") => {
    setActionId(admin.id);
    setActionKind(kind);
    setActionRole(admin.role);
    setActionReason("");
    setActionError(null);
  };

  const closeAction = () => {
    setActionId(null);
    setActionKind(null);
    setActionError(null);
  };

  const submitAction = async (admin: PlatformAdminResponse) => {
    setActionError(null);
    if (!isValidPlatformReason(actionReason)) {
      setActionError("사유는 3~500자로 입력해 주세요.");
      return;
    }
    setActionBusy(true);
    try {
      const body = { reason: actionReason.trim() };
      const res =
        actionKind === "revoke"
          ? await platformActorService.revoke(admin.id, body)
          : actionKind === "reactivate"
            ? await platformActorService.reactivate(admin.id, body)
            : await platformActorService.changeRole(admin.id, { role: actionRole, reason: actionReason.trim() });
      setAdmins((prev) => prev.map((a) => (a.id === admin.id ? res.data.data : a)));
      closeAction();
    } catch (err) {
      setActionError(platformErrorMessage(err, "처리에 실패했습니다."));
    } finally {
      setActionBusy(false);
    }
  };

  return (
    <div>
      <h2 className="text-lg font-semibold text-foreground">플랫폼 관리자</h2>
      <p className="mt-1 text-xs text-muted">
        현재 로그인 계정: <strong className="text-foreground">{actor.nickname}</strong> ({actor.role})
      </p>

      {isSuperAdmin && (
        <section className="mt-4 rounded-xl border border-border bg-surface p-5">
          <h3 className="text-sm font-semibold text-foreground">관리자 부여</h3>
          <div className="mt-3 flex gap-2">
            <input
              aria-label="사용자 ID"
              value={grantUserId}
              onChange={(e) => setGrantUserId(e.target.value)}
              placeholder="사용자 ID"
              disabled={grantBusy}
              className="w-32 rounded-md border border-border bg-surface px-3 py-1.5 text-xs text-foreground placeholder:text-placeholder focus:border-ring focus:outline-none disabled:opacity-50"
            />
            <select
              aria-label="부여할 role"
              value={grantRole}
              onChange={(e) => setGrantRole(e.target.value as PlatformAdminRole)}
              disabled={grantBusy}
              className="rounded-md border border-border bg-surface px-2 py-1.5 text-xs text-foreground focus:border-ring focus:outline-none disabled:opacity-50"
            >
              <option value="PLATFORM_ADMIN">PLATFORM_ADMIN</option>
              <option value="SUPER_ADMIN">SUPER_ADMIN</option>
            </select>
            <input
              aria-label="부여 사유"
              value={grantReason}
              onChange={(e) => setGrantReason(e.target.value)}
              placeholder="부여 사유(3~500자)"
              disabled={grantBusy}
              className="flex-1 rounded-md border border-border bg-surface px-3 py-1.5 text-xs text-foreground placeholder:text-placeholder focus:border-ring focus:outline-none disabled:opacity-50"
            />
            <button
              onClick={submitGrant}
              disabled={grantBusy}
              className="rounded-md btn-gradient px-4 py-1.5 text-xs font-semibold text-primary-foreground disabled:opacity-50"
            >
              부여
            </button>
          </div>
          {grantError && <p className="mt-2 text-xs text-destructive">{grantError}</p>}
        </section>
      )}

      {state === "loading" && <p className="mt-4 text-xs text-muted">불러오는 중...</p>}
      {state === "error" && <p className="mt-4 text-xs text-destructive">{error}</p>}

      {state === "ready" && (
        <ul className="mt-4 divide-y divide-border rounded-xl border border-border bg-surface">
          {admins.map((admin) => (
            <li key={admin.id} className="px-4 py-3">
              <div className="flex items-center justify-between gap-3">
                <div>
                  <p className="text-sm font-medium text-foreground">
                    {admin.nickname} <span className="text-muted">({admin.email})</span>
                  </p>
                  <p className="text-xs text-muted">
                    {admin.role} ·{" "}
                    <span className={admin.status === "ACTIVE" ? "text-success" : "text-destructive"}>
                      {admin.status}
                    </span>
                    {admin.revokedAt && ` · revoked ${new Date(admin.revokedAt).toLocaleString("ko-KR")}`}
                  </p>
                </div>
                {isSuperAdmin && (
                  <div className="flex shrink-0 gap-2">
                    {admin.status === "ACTIVE" ? (
                      <>
                        <button
                          onClick={() => openAction(admin, "role")}
                          className="rounded-md px-2.5 py-1.5 text-xs font-medium text-primary transition-colors duration-fast hover:bg-primary-subtle"
                        >
                          역할 변경
                        </button>
                        <button
                          onClick={() => openAction(admin, "revoke")}
                          className="rounded-md px-2.5 py-1.5 text-xs font-medium text-destructive transition-colors duration-fast hover:bg-destructive-subtle"
                        >
                          권한 회수
                        </button>
                      </>
                    ) : (
                      <button
                        onClick={() => openAction(admin, "reactivate")}
                        className="rounded-md px-2.5 py-1.5 text-xs font-medium text-primary transition-colors duration-fast hover:bg-primary-subtle"
                      >
                        재활성화
                      </button>
                    )}
                  </div>
                )}
              </div>

              {actionId === admin.id && (
                <div className="mt-3 space-y-2 rounded-md border border-border bg-background px-3 py-3">
                  {actionKind === "role" && (
                    <select
                      aria-label="변경할 role"
                      value={actionRole}
                      onChange={(e) => setActionRole(e.target.value as PlatformAdminRole)}
                      disabled={actionBusy}
                      className="rounded-md border border-border bg-surface px-2 py-1.5 text-xs text-foreground focus:border-ring focus:outline-none disabled:opacity-50"
                    >
                      <option value="PLATFORM_ADMIN">PLATFORM_ADMIN</option>
                      <option value="SUPER_ADMIN">SUPER_ADMIN</option>
                    </select>
                  )}
                  <div className="flex gap-2">
                    <input
                      aria-label="처리 사유"
                      value={actionReason}
                      onChange={(e) => setActionReason(e.target.value)}
                      placeholder="처리 사유(3~500자)"
                      disabled={actionBusy}
                      className="flex-1 rounded-md border border-border bg-surface px-3 py-1.5 text-xs text-foreground placeholder:text-placeholder focus:border-ring focus:outline-none disabled:opacity-50"
                    />
                    <button
                      onClick={() => submitAction(admin)}
                      disabled={actionBusy}
                      className="rounded-md btn-gradient px-3 py-1.5 text-xs font-semibold text-primary-foreground disabled:opacity-50"
                    >
                      확인
                    </button>
                    <button
                      onClick={closeAction}
                      disabled={actionBusy}
                      className="rounded-md px-3 py-1.5 text-xs font-medium text-muted hover:bg-surface"
                    >
                      취소
                    </button>
                  </div>
                  {actionError && <p className="text-xs text-destructive">{actionError}</p>}
                </div>
              )}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
