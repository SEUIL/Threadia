"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";

/** /platform 진입 시 기본 탭(문의·신고)으로 보낸다. */
export default function PlatformIndexPage() {
  const router = useRouter();
  useEffect(() => {
    router.replace("/platform/support-cases");
  }, [router]);
  return null;
}
