"use client";

/*
 * CompanySwitcher — components/company/CompanySwitcher.tsx
 *
 * 다중 회사 소속 사용자의 회사 전환 UI. 앱 화면은 URL의 companyId를 따르고,
 * 허브는 현재 탭의 sessionStorage companyId를 따른다.
 * 이 컴포넌트는 AuthContext.companyMemberships 목록을 보여주고, 선택 시 현재 탭의
 * 허브 회사 상태를 갱신한 뒤 대상 화면으로 이동한다. AuthContext activeCompany는 만들지 않는다.
 */
import { useState } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/contexts/AuthContext";
import { writeHubCompanyId } from "@/lib/hubCompanySession";
import type { CompanyMembership } from "@/types/company.types";

type SwitchTarget = "hub" | "projects";

interface CompanySwitcherProps {
  currentMembership: CompanyMembership | null;
  target: SwitchTarget;
}

function targetHref(target: SwitchTarget, companyId: number) {
  return target === "hub" ? "/hub" : `/${companyId}/projects`;
}

function roleLabel(role: CompanyMembership["role"]) {
  return role === "COMPANY_ADMIN" ? "관리자" : "구성원";
}

export function CompanySwitcher({ currentMembership, target }: CompanySwitcherProps) {
  const router = useRouter();
  const { companyMemberships } = useAuth();
  const [open, setOpen] = useState(false);
  const switchableMemberships = companyMemberships.filter(
    (membership) => membership.companyId !== currentMembership?.companyId
  );

  if (!currentMembership) {
    return <p className="mt-0.5 text-xs text-chrome-muted">소속 회사 없음</p>;
  }

  const selectCompany = (membership: CompanyMembership) => {
    setOpen(false);
    writeHubCompanyId(membership.companyId);
    router.push(targetHref(target, membership.companyId));
  };

  return (
    <div className="relative mt-1">
      <div className="flex items-center gap-2">
        <p className="min-w-0 flex-1 truncate text-xs text-chrome-muted">{currentMembership.companyName}</p>
        {switchableMemberships.length > 0 && (
          <button
            type="button"
            aria-haspopup="menu"
            aria-expanded={open}
            aria-label="회사 교체"
            onClick={() => setOpen((value) => !value)}
            className="shrink-0 rounded-md border border-chrome-foreground/10 px-2 py-1 text-[11px] font-semibold text-chrome-foreground transition-colors duration-fast hover:bg-chrome-hover"
          >
            교체
          </button>
        )}
      </div>

      {open && (
        <div
          role="menu"
          aria-label="회사 선택"
          className="absolute left-0 right-0 top-full z-overlay mt-2 rounded-xl border border-chrome-foreground/10 bg-chrome p-1.5 shadow-xl"
        >
          {switchableMemberships.map((membership) => (
            <button
              key={membership.companyId}
              type="button"
              role="menuitem"
              onClick={() => selectCompany(membership)}
              className="flex w-full items-center justify-between gap-3 rounded-lg px-3 py-2 text-left text-sm transition-colors duration-fast hover:bg-chrome-hover"
            >
              <span className="min-w-0 flex-1 truncate font-medium text-chrome-foreground">
                {membership.companyName}
              </span>
              <span className="shrink-0 text-[11px] text-chrome-muted">{roleLabel(membership.role)}</span>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
