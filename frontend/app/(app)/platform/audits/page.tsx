"use client";

/*
 * S15 운영 로그(Audit) 조회 (WBS 4.10.5, §10-C.1)
 *
 * keyset(cursor) pagination — "더 보기"를 누를 때마다 마지막 nextCursor로
 * 다음 페이지를 이어 붙인다. 필터를 바꾸면 cursor는 다른 filter 조합에서
 * 재사용 불가(400 VALIDATION_ERROR)이므로 목록을 비우고 처음부터 다시 연다.
 * beforeState/afterState는 backend가 이미 scalar allowlist로 redact해서
 * 내려주므로 그대로 JSON pretty-print만 한다.
 */
import { useCallback, useState } from "react";
import { platformAuditService } from "@/services/platform-audit.service";
import { platformErrorMessage } from "@/lib/platform";
import { ADMIN_ACTION_LABEL } from "@/types/audit.types";
import type { AdminAction, AuditFilters, AuditResponse } from "@/types/audit.types";

type LoadState = "idle" | "loading" | "ready" | "error";

export default function PlatformAuditsPage() {
  const [audits, setAudits] = useState<AuditResponse[]>([]);
  const [nextCursor, setNextCursor] = useState<string | null>(null);
  const [hasNext, setHasNext] = useState(false);
  const [state, setState] = useState<LoadState>("idle");
  const [error, setError] = useState<string | null>(null);

  const [actionFilter, setActionFilter] = useState<AdminAction | "">("");
  const [actorUserId, setActorUserId] = useState("");

  const buildFilters = useCallback(
    (cursor?: string): AuditFilters => ({
      action: actionFilter || undefined,
      actorUserId: actorUserId.trim() ? Number(actorUserId) : undefined,
      cursor,
      size: 20,
    }),
    [actionFilter, actorUserId]
  );

  const search = useCallback(async () => {
    setState("loading");
    setError(null);
    try {
      const { data } = await platformAuditService.list(buildFilters());
      setAudits(data.data.audits);
      setNextCursor(data.data.nextCursor);
      setHasNext(data.data.hasNext);
      setState("ready");
    } catch (err) {
      setError(platformErrorMessage(err, "운영 로그를 불러오지 못했습니다."));
      setState("error");
    }
  }, [buildFilters]);

  const loadMore = async () => {
    if (!nextCursor) return;
    setState("loading");
    setError(null);
    try {
      const { data } = await platformAuditService.list(buildFilters(nextCursor));
      setAudits((prev) => [...prev, ...data.data.audits]);
      setNextCursor(data.data.nextCursor);
      setHasNext(data.data.hasNext);
      setState("ready");
    } catch (err) {
      setError(platformErrorMessage(err, "다음 페이지를 불러오지 못했습니다."));
      setState("error");
    }
  };

  return (
    <div>
      <h2 className="text-lg font-semibold text-foreground">운영 로그</h2>

      <div className="mt-3 flex gap-2">
        <select
          aria-label="action 필터"
          value={actionFilter}
          onChange={(e) => setActionFilter(e.target.value as AdminAction | "")}
          className="rounded-md border border-border bg-surface px-2 py-1.5 text-xs text-foreground focus:border-ring focus:outline-none"
        >
          <option value="">전체 action</option>
          {Object.entries(ADMIN_ACTION_LABEL).map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </select>
        <input
          aria-label="actor 사용자 ID"
          value={actorUserId}
          onChange={(e) => setActorUserId(e.target.value)}
          placeholder="actor 사용자 ID"
          className="w-40 rounded-md border border-border bg-surface px-3 py-1.5 text-xs text-foreground placeholder:text-placeholder focus:border-ring focus:outline-none"
        />
        <button
          onClick={search}
          disabled={state === "loading"}
          className="rounded-md btn-gradient px-4 py-1.5 text-xs font-semibold text-primary-foreground disabled:opacity-50"
        >
          조회
        </button>
      </div>

      {state === "error" && <p className="mt-4 text-xs text-destructive">{error}</p>}
      {state === "idle" && <p className="mt-4 text-xs text-muted">조회 버튼을 눌러 로그를 불러오세요.</p>}

      {audits.length > 0 && (
        <ul className="mt-4 divide-y divide-border rounded-xl border border-border bg-surface">
          {audits.map((audit) => (
            <li key={audit.id} className="px-4 py-3">
              <div className="flex items-center justify-between">
                <p className="text-sm font-medium text-foreground">{ADMIN_ACTION_LABEL[audit.action]}</p>
                <p className="text-xs text-muted">{new Date(audit.createdAt).toLocaleString("ko-KR")}</p>
              </div>
              <p className="mt-1 text-xs text-muted">
                actor #{audit.actorUserId} ({audit.authoritySource}
                {audit.authorityRole ? `/${audit.authorityRole}` : ""}) · target {audit.targetType}
                {audit.targetId ? ` #${audit.targetId}` : ""}
                {audit.supportCaseId ? ` · case #${audit.supportCaseId}` : ""}
              </p>
              <p className="mt-1 text-sm text-foreground">{audit.reason}</p>
              {(audit.beforeState || audit.afterState) && (
                <div className="mt-2 grid grid-cols-2 gap-2">
                  <pre className="overflow-x-auto rounded-md bg-border/30 px-2 py-1.5 font-mono text-[11px] text-muted">
                    before: {JSON.stringify(audit.beforeState ?? {}, null, 2)}
                  </pre>
                  <pre className="overflow-x-auto rounded-md bg-border/30 px-2 py-1.5 font-mono text-[11px] text-muted">
                    after: {JSON.stringify(audit.afterState ?? {}, null, 2)}
                  </pre>
                </div>
              )}
            </li>
          ))}
        </ul>
      )}
      {state === "ready" && audits.length === 0 && (
        <p className="mt-4 text-xs text-muted">조건에 맞는 로그가 없습니다.</p>
      )}

      {hasNext && (
        <button
          onClick={loadMore}
          disabled={state === "loading"}
          className="mt-3 w-full rounded-md border border-border py-2 text-xs font-medium text-muted transition-colors duration-fast hover:bg-surface disabled:opacity-50"
        >
          더 보기
        </button>
      )}
    </div>
  );
}
