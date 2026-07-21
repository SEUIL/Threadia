"use client";

/*
 * 현재 로그인 사용자가 platform admin(PLATFORM_ADMIN/SUPER_ADMIN)인지 —
 * RailShell/HubOverlay의 "플랫폼 관리자" nav 진입점 노출 여부에 쓴다.
 *
 * WBS 감사 중 발견: S15 플랫폼 관리자 콘솔(WBS 4.10.5, PR #198)이 구현·머지는
 * 됐지만 nav 어디에도 `/platform` 진입점이 없어서, 실제 플랫폼 관리자도 URL을
 * 직접 입력해야만 들어갈 수 있었다. platform admin 여부는 회사 membership과
 * 무관한 별도 authority라 AuthContext가 아니라 GET /api/v1/platform/actor로
 * 직접 판별한다(platform/layout.tsx의 게이트와 동일한 API). 403이든 다른
 * 오류든 "관리자 아님"으로 보수적으로 처리해 nav 항목을 숨긴다 — 실제 접근
 * 차단은 어차피 platform/layout.tsx 게이트가 한 번 더 하므로, 여기서는 UI
 * 노출만 안전 측으로 판단하면 된다(RailShell의 회사 역할 판단과 동일한 원칙).
 */
import { useEffect, useState } from "react";
import { platformActorService } from "@/services/platform-actor.service";

export function useIsPlatformAdmin(): boolean {
  const [isPlatformAdmin, setIsPlatformAdmin] = useState(false);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        await platformActorService.getActor();
        if (!cancelled) setIsPlatformAdmin(true);
      } catch {
        if (!cancelled) setIsPlatformAdmin(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  return isPlatformAdmin;
}
