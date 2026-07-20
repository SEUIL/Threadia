"use client";

/*
 * S15 회사 관리 (WBS 4.10.5 후속, Issue #196 → PR #203 Backend 완료 후 구현)
 *
 * 목록 API가 없어 4.10.5 1차 구현에서 제외했던 화면. `GET /platform/companies`가
 * 생겨서 이어서 구현한다. 조회/상태변경 둘 다 requireAdmin(PLATFORM_ADMIN/
 * SUPER_ADMIN 공통)이라 SUPER_ADMIN 전용 UI 분기는 없다(다른 platform 화면과 차이).
 */
import { useCallback, useState } from "react";
import { platformCompanyService } from "@/services/platform-company.service";
import { platformErrorMessage, isValidPlatformReason } from "@/lib/platform";
import type { CompanyResponse } from "@/types/company.types";
import type { CompanyStatus } from "@/types/platform-company.types";

type LoadState = "idle" | "loading" | "ready" | "error";

const STATUS_LABEL: Record<CompanyStatus, string> = {
  ACTIVE: "활성",
  INACTIVE: "비활성",
};

const STATUS_CLASS: Record<CompanyStatus, string> = {
  ACTIVE: "bg-success/15 text-success",
  INACTIVE: "bg-border text-muted",
};

export default function PlatformCompaniesPage() {
  const [companies, setCompanies] = useState<CompanyResponse[]>([]);
  const [nextCursor, setNextCursor] = useState<string | null>(null);
  const [hasNext, setHasNext] = useState(false);
  const [state, setState] = useState<LoadState>("idle");
  const [error, setError] = useState<string | null>(null);

  const [statusFilter, setStatusFilter] = useState<CompanyStatus | "">("");
  const [q, setQ] = useState("");

  const [actionId, setActionId] = useState<number | null>(null);
  const [actionReason, setActionReason] = useState("");
  const [actionBusy, setActionBusy] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);

  const buildFilters = useCallback(
    (cursor?: string) => ({
      status: statusFilter || undefined,
      q: q.trim() || undefined,
      cursor,
      size: 20,
    }),
    [statusFilter, q]
  );

  const search = useCallback(async () => {
    setState("loading");
    setError(null);
    try {
      const { data } = await platformCompanyService.list(buildFilters());
      setCompanies(data.data.companies);
      setNextCursor(data.data.nextCursor);
      setHasNext(data.data.hasNext);
      setState("ready");
    } catch (err) {
      setError(platformErrorMessage(err, "회사 목록을 불러오지 못했습니다."));
      setState("error");
    }
  }, [buildFilters]);

  const loadMore = async () => {
    if (!nextCursor) return;
    setState("loading");
    setError(null);
    try {
      const { data } = await platformCompanyService.list(buildFilters(nextCursor));
      setCompanies((prev) => [...prev, ...data.data.companies]);
      setNextCursor(data.data.nextCursor);
      setHasNext(data.data.hasNext);
      setState("ready");
    } catch (err) {
      setError(platformErrorMessage(err, "다음 페이지를 불러오지 못했습니다."));
      setState("error");
    }
  };

  const openAction = (company: CompanyResponse) => {
    setActionId(company.id);
    setActionReason("");
    setActionError(null);
  };

  const closeAction = () => {
    setActionId(null);
    setActionError(null);
  };

  const submitStatusChange = async (company: CompanyResponse) => {
    setActionError(null);
    if (!isValidPlatformReason(actionReason)) {
      setActionError("사유는 3~500자로 입력해 주세요.");
      return;
    }
    const nextStatus: CompanyStatus = company.status === "ACTIVE" ? "INACTIVE" : "ACTIVE";
    setActionBusy(true);
    try {
      const { data } = await platformCompanyService.updateStatus(company.id, {
        status: nextStatus,
        reason: actionReason.trim(),
      });
      setCompanies((prev) => prev.map((c) => (c.id === company.id ? data.data.company : c)));
      closeAction();
    } catch (err) {
      setActionError(platformErrorMessage(err, "상태 변경에 실패했습니다."));
    } finally {
      setActionBusy(false);
    }
  };

  return (
    <div>
      <h2 className="text-lg font-semibold text-foreground">회사 관리</h2>

      <div className="mt-3 flex gap-2">
        <select
          aria-label="상태 필터"
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value as CompanyStatus | "")}
          className="rounded-md border border-border bg-surface px-2 py-1.5 text-xs text-foreground focus:border-ring focus:outline-none"
        >
          <option value="">전체 상태</option>
          <option value="ACTIVE">활성</option>
          <option value="INACTIVE">비활성</option>
        </select>
        <input
          aria-label="회사명 검색"
          value={q}
          onChange={(e) => setQ(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && search()}
          placeholder="회사명 검색"
          className="flex-1 rounded-md border border-border bg-surface px-3 py-1.5 text-xs text-foreground placeholder:text-placeholder focus:border-ring focus:outline-none"
        />
        <button
          onClick={() => void search()}
          disabled={state === "loading"}
          className="rounded-md btn-gradient px-4 py-1.5 text-xs font-semibold text-primary-foreground disabled:opacity-50"
        >
          조회
        </button>
      </div>

      {state === "error" && <p className="mt-4 text-xs text-destructive">{error}</p>}
      {state === "idle" && <p className="mt-4 text-xs text-muted">조회 버튼을 눌러 회사 목록을 불러오세요.</p>}

      {companies.length > 0 && (
        <ul className="mt-4 divide-y divide-border rounded-xl border border-border bg-surface">
          {companies.map((company) => {
            // 백엔드가 ACTIVE/INACTIVE 외 값을 주는 경우를 대비해 알 수 없는
            // status는 캐스팅 없이 raw 문자열 그대로 중립 스타일로 보여준다
            // (리뷰 반영, minjeong333).
            const isKnownStatus = company.status === "ACTIVE" || company.status === "INACTIVE";
            const status = isKnownStatus ? (company.status as CompanyStatus) : null;
            const statusLabel = status ? STATUS_LABEL[status] : company.status;
            const statusClass = status ? STATUS_CLASS[status] : "bg-border text-muted";
            return (
              <li key={company.id} className="px-4 py-3">
                <div className="flex items-center justify-between gap-3">
                  <div className="min-w-0">
                    <p className="text-sm font-medium text-foreground">{company.name}</p>
                    <p className="text-xs text-muted">
                      #{company.id}
                      {company.representativeName && ` · ${company.representativeName}`}
                      {company.industryCategory && ` · ${company.industryCategory}`}
                    </p>
                  </div>
                  <div className="flex shrink-0 items-center gap-2">
                    <span className={`rounded-full px-2.5 py-0.5 text-xs font-medium ${statusClass}`}>
                      {statusLabel}
                    </span>
                    <button
                      onClick={() => openAction(company)}
                      className="rounded-md px-2.5 py-1.5 text-xs font-medium text-primary transition-colors duration-fast hover:bg-primary-subtle"
                    >
                      {status === "ACTIVE" ? "비활성화" : "활성화"}
                    </button>
                  </div>
                </div>

                {actionId === company.id && (
                  <div className="mt-3 flex gap-2 rounded-md border border-border bg-background px-3 py-3">
                    <input
                      aria-label="상태 변경 사유"
                      value={actionReason}
                      onChange={(e) => setActionReason(e.target.value)}
                      placeholder="변경 사유(3~500자)"
                      disabled={actionBusy}
                      className="flex-1 rounded-md border border-border bg-surface px-3 py-1.5 text-xs text-foreground placeholder:text-placeholder focus:border-ring focus:outline-none disabled:opacity-50"
                    />
                    <button
                      onClick={() => submitStatusChange(company)}
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
                )}
                {actionId === company.id && actionError && (
                  <p className="mt-2 text-xs text-destructive">{actionError}</p>
                )}
              </li>
            );
          })}
        </ul>
      )}
      {state === "ready" && companies.length === 0 && (
        <p className="mt-4 text-xs text-muted">조건에 맞는 회사가 없습니다.</p>
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
