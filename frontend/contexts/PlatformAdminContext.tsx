"use client";

/*
 * PlatformAdminContext — /platform 화면군 전용 게이트.
 *
 * AuthContext에는 platform admin 여부가 없다(회사 membership과 무관한 별도
 * authority). 그래서 `app/(app)/platform/layout.tsx`가 마운트 시
 * GET /api/v1/platform/actor를 1회 호출해 role을 여기 담아두고, 하위
 * 페이지들은 이 context로 SUPER_ADMIN 전용 액션(설정 수정, 관리자 lifecycle
 * write)의 노출 여부를 판단한다. actor 조회 자체가 403이면 platform
 * 관리자가 아니라는 뜻이라 layout이 접근 거부 화면을 보여주고 children을
 * 렌더링하지 않는다.
 */
import { createContext, useContext } from "react";
import type { PlatformAdminResponse } from "@/types/platform.types";

interface PlatformAdminContextValue {
  actor: PlatformAdminResponse;
  isSuperAdmin: boolean;
}

const PlatformAdminContext = createContext<PlatformAdminContextValue | null>(null);

export function PlatformAdminProvider({
  actor,
  children,
}: {
  actor: PlatformAdminResponse;
  children: React.ReactNode;
}) {
  return (
    <PlatformAdminContext.Provider value={{ actor, isSuperAdmin: actor.role === "SUPER_ADMIN" }}>
      {children}
    </PlatformAdminContext.Provider>
  );
}

export function usePlatformAdmin() {
  const ctx = useContext(PlatformAdminContext);
  if (!ctx) {
    throw new Error("usePlatformAdmin은 PlatformAdminProvider 안에서만 사용할 수 있습니다.");
  }
  return ctx;
}
