"use client";

/*
 * (app) 레이아웃 — 로그인 사용자만 접근 가능한 화면 그룹의 공통 게이트.
 * status가 loading인 동안은 아무것도 렌더링하지 않고, unauthenticated면
 * /login으로 돌려보낸다. 실제 콘텐츠는 authenticated일 때만 그린다.
 *
 * ⚠️ 여기서 <main>으로 감싸지 않는다 — [companyId] 하위는 RailShell이 자체
 *    <main>(스크롤 컨테이너)을 이미 제공한다. 여기서 또 <main>으로 감싸면
 *    <main>이 중첩돼(문서에 랜드마크 2개) RailShell 내부 스크롤 컨테이너를
 *    찾는 셀렉터가 바깥쪽 빈 <main>을 잘못 잡는 문제가 있었다.
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

  return <>{children}</>;
}
