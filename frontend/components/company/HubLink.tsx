"use client";

import Link from "next/link";
import { writeHubCompanyId } from "@/lib/hubCompanySession";

export function HubLink({
  companyId,
  className,
  children,
}: {
  companyId: number | string;
  className?: string;
  children: React.ReactNode;
}) {
  return (
    <Link href="/hub" onClick={() => writeHubCompanyId(Number(companyId))} className={className}>
      {children}
    </Link>
  );
}
