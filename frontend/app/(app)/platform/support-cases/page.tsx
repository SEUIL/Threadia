"use client";

/*
 * S15 문의·신고 목록 (WBS 4.10.5) — GET /api/v1/platform/support-cases
 * 필터/정렬은 backend가 제공하지 않아(§10-A) 클라이언트에서 상태/유형만 좁힌다.
 */
import { useCallback, useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { platformSupportCaseService } from "@/services/platform-support-case.service";
import { platformErrorMessage } from "@/lib/platform";
import {
  SUPPORT_CASE_STATUS_CLASS as STATUS_CLASS,
  SUPPORT_CASE_STATUS_LABEL as STATUS_LABEL,
  SUPPORT_CASE_TYPE_LABEL as TYPE_LABEL,
} from "@/lib/support-case";
import type { SupportCaseResponse, SupportCaseStatus, SupportCaseType } from "@/types/support-case.types";

type LoadState = "loading" | "ready" | "error";

export default function PlatformSupportCasesPage() {
  const [cases, setCases] = useState<SupportCaseResponse[]>([]);
  const [state, setState] = useState<LoadState>("loading");
  const [error, setError] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState<SupportCaseStatus | "ALL">("ALL");
  const [typeFilter, setTypeFilter] = useState<SupportCaseType | "ALL">("ALL");

  const fetchCases = useCallback(async () => {
    setState("loading");
    setError(null);
    try {
      const { data } = await platformSupportCaseService.list();
      setCases(data.data.supportCases);
      setState("ready");
    } catch (err) {
      setError(platformErrorMessage(err, "문의·신고 목록을 불러오지 못했습니다."));
      setState("error");
    }
  }, []);

  useEffect(() => {
    (async () => {
      await fetchCases();
    })();
  }, [fetchCases]);

  const filtered = useMemo(
    () =>
      cases
        .filter((c) => statusFilter === "ALL" || c.status === statusFilter)
        .filter((c) => typeFilter === "ALL" || c.type === typeFilter)
        .sort((a, b) => b.createdAt.localeCompare(a.createdAt)),
    [cases, statusFilter, typeFilter]
  );

  return (
    <div>
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold text-foreground">문의·신고 ({filtered.length})</h2>
        <div className="flex gap-2">
          <select
            aria-label="유형 필터"
            value={typeFilter}
            onChange={(e) => setTypeFilter(e.target.value as SupportCaseType | "ALL")}
            className="rounded-md border border-border bg-surface px-2 py-1.5 text-xs text-foreground focus:border-ring focus:outline-none"
          >
            <option value="ALL">전체 유형</option>
            <option value="INQUIRY">문의</option>
            <option value="REPORT">신고</option>
          </select>
          <select
            aria-label="상태 필터"
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value as SupportCaseStatus | "ALL")}
            className="rounded-md border border-border bg-surface px-2 py-1.5 text-xs text-foreground focus:border-ring focus:outline-none"
          >
            <option value="ALL">전체 상태</option>
            {Object.entries(STATUS_LABEL).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </select>
        </div>
      </div>

      {state === "loading" && <p className="mt-4 text-xs text-muted">불러오는 중...</p>}
      {state === "error" && <p className="mt-4 text-xs text-destructive">{error}</p>}

      {state === "ready" && (
        <ul className="mt-4 divide-y divide-border rounded-xl border border-border bg-surface">
          {filtered.map((c) => (
            <li key={c.id}>
              <Link
                href={`/platform/support-cases/${c.id}`}
                className="flex items-center justify-between gap-3 px-4 py-3 transition-colors duration-fast hover:bg-primary-subtle"
              >
                <div className="min-w-0">
                  <div className="flex items-center gap-2">
                    <span className="rounded-full bg-border px-2 py-0.5 text-xs font-medium text-muted">
                      {TYPE_LABEL[c.type]}
                    </span>
                    <p className="truncate text-sm font-medium text-foreground">{c.title}</p>
                  </div>
                  <p className="mt-1 text-xs text-muted">
                    #{c.id} · reporter #{c.reporterUserId} ·{" "}
                    {new Date(c.createdAt).toLocaleString("ko-KR")}
                  </p>
                </div>
                <span className={`shrink-0 rounded-full px-2.5 py-0.5 text-xs font-medium ${STATUS_CLASS[c.status]}`}>
                  {STATUS_LABEL[c.status]}
                </span>
              </Link>
            </li>
          ))}
          {filtered.length === 0 && (
            <li className="px-4 py-8 text-center text-sm text-muted">해당하는 문의·신고가 없습니다.</li>
          )}
        </ul>
      )}
    </div>
  );
}
