"use client";

/*
 * (app) 레이아웃 — 로그인 사용자만 접근 가능한 화면 그룹의 공통 게이트.
 * status가 loading인 동안은 아무것도 렌더링하지 않고, unauthenticated면
 * /login으로 돌려보낸다. 실제 콘텐츠는 authenticated일 때만 그린다.
 */
import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/contexts/AuthContext";

export default function AppLayout({ children }: { children: React.ReactNode }) {
  const { status } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (status === "unauthenticated") {
      router.replace("/login");
    }
  }, [status, router]);

  if (status !== "authenticated") {
    return null;
  }

  return <main>{children}</main>;
}
