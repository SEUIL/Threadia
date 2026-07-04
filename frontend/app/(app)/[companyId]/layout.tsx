/*
 * (app)/[companyId] 공통 레이아웃 — companyId 하위 모든 화면(프로젝트/칸반/문서/
 * 채팅/설정/알림 등)에 지속되는 좌측 사이드바 + 상단바(AppShell)를 씌운다.
 * hub, invitations는 이 경로 밖에 있어서 영향을 받지 않는다.
 */
import { AppShell } from "@/components/app-shell/AppShell";

export default function CompanyLayout({ children }: { children: React.ReactNode }) {
  return <AppShell>{children}</AppShell>;
}
