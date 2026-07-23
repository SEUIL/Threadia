import type { CompanyMembership } from "@/types/company.types";

export const HUB_COMPANY_SESSION_KEY = "threadia:hubCompanyId";
export const HUB_COMPANY_CHANGED_EVENT = "threadia:hubCompanyChanged";

type HubCompanyChangedDetail = {
  companyId: number;
};

function parseCompanyId(value: string | null): number | null {
  if (value === null) return null;
  const companyId = Number(value);
  return Number.isInteger(companyId) && companyId > 0 ? companyId : null;
}

export function readHubCompanyId(): number | null {
  if (typeof window === "undefined") return null;
  return parseCompanyId(window.sessionStorage.getItem(HUB_COMPANY_SESSION_KEY));
}

export function writeHubCompanyId(companyId: number) {
  if (typeof window === "undefined") return;
  if (!Number.isInteger(companyId) || companyId <= 0) return;

  window.sessionStorage.setItem(HUB_COMPANY_SESSION_KEY, String(companyId));
  window.dispatchEvent(new CustomEvent<HubCompanyChangedDetail>(HUB_COMPANY_CHANGED_EVENT, { detail: { companyId } }));
}

export function subscribeHubCompanyChange(listener: (companyId: number | null) => void) {
  if (typeof window === "undefined") return () => {};

  const handleChange = () => {
    listener(readHubCompanyId());
  };

  window.addEventListener(HUB_COMPANY_CHANGED_EVENT, handleChange);
  return () => {
    window.removeEventListener(HUB_COMPANY_CHANGED_EVENT, handleChange);
  };
}

export function selectValidHubMembership(
  companyMemberships: CompanyMembership[],
  storedCompanyId: number | null
): CompanyMembership | null {
  if (companyMemberships.length === 0) return null;
  if (storedCompanyId !== null) {
    return companyMemberships.find((membership) => membership.companyId === storedCompanyId) ?? companyMemberships[0];
  }
  return companyMemberships[0];
}
