"use client";

/*
 * S15 Platform Admin 콘솔 — 공통 게이트 + 탭 nav (WBS 4.10.5)
 *
 * company-agnostic 경로(/platform, hub/invitations와 같은 패턴). platform admin
 * 여부는 회사 membership과 무관한 별도 authority라 AuthContext가 아니라
 * GET /api/v1/platform/actor로 직접 판별한다(PlatformAdminContext 참고).
 * 403(FORBIDDEN)이면 platform 관리자가 아니라는 뜻 — children을 렌더링하지
 * 않고 접근 거부 안내만 보여준다.
 */
import { useEffect, useState } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { platformActorService } from "@/services/platform-actor.service";
import { platformErrorMessage } from "@/lib/platform";
import { PlatformAdminProvider } from "@/contexts/PlatformAdminContext";
import type { PlatformAdminResponse } from "@/types/platform.types";

type GateState = "loading" | "ready" | "denied" | "error";

const NAV_ITEMS = [
  { href: "/platform/support-cases", label: "문의·신고" },
  { href: "/platform/settings", label: "시스템 설정" },
  { href: "/platform/admins", label: "관리자" },
  { href: "/platform/audits", label: "운영 로그" },
];

export default function PlatformLayout({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const [state, setState] = useState<GateState>("loading");
  const [error, setError] = useState<string | null>(null);
  const [actor, setActor] = useState<PlatformAdminResponse | null>(null);

  useEffect(() => {
    (async () => {
      try {
        const { data } = await platformActorService.getActor();
        setActor(data.data.platformAdmin);
        setState("ready");
      } catch (err) {
        setState("denied");
        setError(platformErrorMessage(err, "플랫폼 관리자 정보를 불러오지 못했습니다."));
      }
    })();
  }, []);

  if (state === "loading") {
    return <div className="mx-auto max-w-5xl px-6 py-10 text-sm text-muted">확인하는 중...</div>;
  }

  if (state === "denied" || !actor) {
    return (
      <div className="mx-auto max-w-5xl px-6 py-10">
        <div className="rounded-xl border border-destructive-subtle bg-destructive-subtle px-6 py-8 text-center">
          <p className="text-sm font-semibold text-destructive">플랫폼 관리자 콘솔에 접근할 수 없습니다.</p>
          <p className="mt-1 text-xs text-destructive">{error}</p>
          <Link href="/hub" className="mt-4 inline-block text-sm font-medium text-primary hover:underline">
            허브로 돌아가기
          </Link>
        </div>
      </div>
    );
  }

  return (
    <PlatformAdminProvider actor={actor}>
      <div className="mx-auto max-w-5xl px-6 py-8">
        <div className="flex items-center justify-between">
          <div>
            <Link href="/hub" className="text-sm text-muted transition-colors duration-fast hover:text-primary">
              ← 허브
            </Link>
            <h1 className="mt-2 text-xl font-bold text-foreground">플랫폼 관리자 콘솔</h1>
          </div>
          <div className="text-right text-xs text-muted">
            <p className="text-sm font-medium text-foreground">{actor.nickname}</p>
            <p>{actor.role === "SUPER_ADMIN" ? "SUPER_ADMIN" : "PLATFORM_ADMIN"}</p>
          </div>
        </div>

        <nav className="mt-6 flex gap-1 border-b border-border">
          {NAV_ITEMS.map((item) => {
            const active = pathname?.startsWith(item.href);
            return (
              <Link
                key={item.href}
                href={item.href}
                className={`px-4 py-2.5 text-sm font-medium transition-colors duration-fast ${
                  active
                    ? "border-b-2 border-primary text-primary"
                    : "text-muted hover:text-foreground"
                }`}
              >
                {item.label}
              </Link>
            );
          })}
        </nav>

        <div className="mt-6">{children}</div>
      </div>
    </PlatformAdminProvider>
  );
}
