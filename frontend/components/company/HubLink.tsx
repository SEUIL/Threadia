"use client";

import Link from "next/link";
import { writeHubCompanyId } from "@/lib/hubCompanySession";

export function HubLink({
  companyId,
  className,
  title,
  children,
}: {
  companyId: number | string;
  className?: string;
  title?: string;
  children: React.ReactNode;
}) {
  return (
    <Link href="/hub" onClick={() => writeHubCompanyId(Number(companyId))} title={title} className={className}>
      {children}
    </Link>
  );
}
