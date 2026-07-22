/*
 * Auth Layout — app/(auth)/layout.tsx
 *
 * Server Component 껍데기 — title template만 선언하고 나머지 장식/애니메이션은
 * AuthLayoutClient.tsx(Client Component)에 위임한다. login/signup/
 * forgot-password 자식 layout.tsx는 `title: "로그인"`처럼 짧은 값만 선언하면
 * 이 템플릿이 자동으로 "로그인 | Threadia"를 만든다(리뷰 관찰사항, PR #238
 * 후속 — 이전엔 자식 layout 3개가 전체 문자열을 각자 하드코딩했었다).
 */
import type { Metadata } from "next";
import { AuthLayoutClient } from "./AuthLayoutClient";

export const metadata: Metadata = {
  title: {
    template: "%s | Threadia",
    default: "Threadia",
  },
};

export default function AuthLayout({ children }: { children: React.ReactNode }) {
  return <AuthLayoutClient>{children}</AuthLayoutClient>;
}
