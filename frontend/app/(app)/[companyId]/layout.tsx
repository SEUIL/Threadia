/*
 * (app)/[companyId] 공통 레이아웃 — companyId 하위 모든 화면(프로젝트/칸반/문서/
 * 채팅/설정/알림 등)에 RailShell(좌측 hover 레일 + 하단 플로팅 커맨드바)을 씌운다.
 * hub, invitations는 이 경로 밖에 있어서 영향을 받지 않는다.
 */
import { RailShell } from "@/components/app-shell/RailShell";

export default function CompanyLayout({ children }: { children: React.ReactNode }) {
  return <RailShell>{children}</RailShell>;
}
