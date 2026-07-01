import { redirect } from "next/navigation";

/*
 * 루트 페이지 — app/page.tsx
 *
 * Sprint 1 은 인증 화면이 진입점이므로, 루트("/") 접속 시 로그인 화면으로 즉시 이동시킨다.
 * (npm run dev 후 주소창에 /login 을 직접 치지 않아도 되도록)
 *
 * TODO (인증 연동 후):
 *   - 로그인 상태(access token/세션)면 /hub 로, 아니면 /login 으로 분기.
 */
export default function Home() {
  redirect("/login");
}
