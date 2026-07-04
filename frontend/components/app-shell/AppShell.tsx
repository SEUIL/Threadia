"use client";

/*
 * AppShell — components/app-shell/AppShell.tsx
 *
 * 허브(3D 화면)를 제외한 모든 (app) 화면(프로젝트/칸반/문서/채팅/설정 등)에
 * 지속되는 좌측 사이드바 + 상단바를 제공한다.
 *
 * ⚠️ 왜 필요했는지: 지금까지는 허브에만 HubOverlay(사이드바/탑바)가 있고
 *    그 밖의 모든 화면은 "← 뒤로가기" 링크 하나뿐인 독립 페이지였다. 그래서
 *    예를 들어 칸반 보드를 보다가 DM을 확인하려면 무조건 허브로 돌아갔다가
 *    다시 들어가야 했다 — Slack/Notion/Linear류 B2B SaaS의 "항상 보이는
 *    좌측 사이드바" 관습에서 크게 벗어난 구조였다. 이 컴포넌트가 그 공백을 메운다.
 *
 * 적용 위치: app/(app)/[companyId]/layout.tsx 에서만 감싼다. hub, invitations는
 *    [companyId] 밖에 있어서 이 셸의 영향을 받지 않는다(허브는 자체 3D 오버레이 유지).
 *
 * ⚠️ MOCK: 회사 컨텍스트/역할은 HubOverlay와 동일하게 아직 회사 API가 없어서
 *    상수로 고정했다. Sprint 2 연동 후 실제 값으로 교체해야 한다.
 *
 * 알려진 한계: "프로젝트" 사이드바 항목의 active 판정은 /projects 경로 기준이라
 *    /{companyId}/{projectId}/board 같은 프로젝트 상세 화면에서는 켜지지 않는다.
 *    URL 구조상 프로젝트 상세가 /projects 하위 경로가 아니라서(설계 그대로) 정확한
 *    매칭이 어렵다 — 후속 개선 대상으로 남겨둔다.
 */
import Link from "next/link";
import { usePathname, useParams, useRouter } from "next/navigation";
import { useAuth } from "@/contexts/AuthContext";

type Role = "A" | "P" | "C";

// TODO(Sprint 2): 회사 API 연동 후 실제 회사/역할 정보로 교체 (HubOverlay와 동일 기준)
const MOCK_ROLE: Role = "C";
const MOCK_NOTIFICATION_COUNT = 3;
const MOCK_DM_UNREAD_COUNT = 1;

function BellIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" className="h-5 w-5">
      <path
        d="M12 3a5 5 0 0 0-5 5v3.2c0 .6-.2 1.2-.6 1.7L5 15h14l-1.4-2.1c-.4-.5-.6-1.1-.6-1.7V8a5 5 0 0 0-5-5Z"
        stroke="currentColor"
        strokeWidth="1.6"
        strokeLinejoin="round"
      />
      <path d="M9.5 18a2.5 2.5 0 0 0 5 0" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
    </svg>
  );
}

function ChatIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" className="h-5 w-5">
      <path
        d="M4 5.5A1.5 1.5 0 0 1 5.5 4h13A1.5 1.5 0 0 1 20 5.5v9A1.5 1.5 0 0 1 18.5 16H9l-4 4v-4H5.5A1.5 1.5 0 0 1 4 14.5v-9Z"
        stroke="currentColor"
        strokeWidth="1.6"
        strokeLinejoin="round"
      />
    </svg>
  );
}

function FolderIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" className="h-5 w-5">
      <path
        d="M4 6.5A1.5 1.5 0 0 1 5.5 5h4l2 2h8A1.5 1.5 0 0 1 21 8.5v9A1.5 1.5 0 0 1 19.5 19h-14A1.5 1.5 0 0 1 4 17.5v-11Z"
        stroke="currentColor"
        strokeWidth="1.6"
        strokeLinejoin="round"
      />
    </svg>
  );
}

function BuildingIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" className="h-5 w-5">
      <path d="M5 20V5.5A1.5 1.5 0 0 1 6.5 4h6A1.5 1.5 0 0 1 14 5.5V20" stroke="currentColor" strokeWidth="1.6" strokeLinejoin="round" />
      <path d="M14 10.5h3.5A1.5 1.5 0 0 1 19 12v8" stroke="currentColor" strokeWidth="1.6" strokeLinejoin="round" />
      <path d="M8 8h2M8 11.5h2M8 15h2" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
      <path d="M5 20h14" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
    </svg>
  );
}

function LogoutIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" className="h-5 w-5">
      <path d="M15 17.5 20 12l-5-5.5" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />
      <path d="M20 12H9" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
      <path d="M9 4H5.5A1.5 1.5 0 0 0 4 5.5v13A1.5 1.5 0 0 0 5.5 20H9" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function Badge({ count }: { count: number }) {
  if (count <= 0) return null;
  return (
    <span className="absolute -right-1 -top-1 flex h-4 min-w-4 items-center justify-center rounded-full bg-destructive px-1 text-[10px] font-semibold leading-none text-on-primary">
      {count > 9 ? "9+" : count}
    </span>
  );
}

function IconButton({
  href,
  label,
  badge,
  children,
}: {
  href: string;
  label: string;
  badge?: number;
  children: React.ReactNode;
}) {
  return (
    <Link
      href={href}
      aria-label={label}
      title={label}
      className="relative flex h-9 w-9 items-center justify-center rounded-full text-foreground transition-colors duration-fast hover:bg-primary-subtle hover:text-primary"
    >
      {children}
      {typeof badge === "number" && <Badge count={badge} />}
    </Link>
  );
}

function Sidebar({ companyId }: { companyId: string }) {
  const pathname = usePathname();

  const items = [
    { href: `/${companyId}/projects`, label: "프로젝트", icon: <FolderIcon /> },
    { href: `/${companyId}/dm`, label: "채팅·DM", icon: <ChatIcon /> },
    { href: `/${companyId}/notifications`, label: "알림", icon: <BellIcon /> },
  ];
  // C(회사 관리자)만 회사 관리 메뉴가 보인다 (화면정의서 S03 메뉴 노출 규칙, HubOverlay와 동일)
  if (MOCK_ROLE === "C") {
    items.push({ href: `/${companyId}/settings`, label: "회사 관리", icon: <BuildingIcon /> });
  }

  return (
    <aside className="hidden w-56 shrink-0 flex-col border-r border-border bg-surface md:flex">
      <Link href="/hub" className="flex items-center gap-2 border-b border-border px-5 py-4">
        <span className="grid h-7 w-7 place-items-center rounded-md text-sm font-extrabold bg-gradient-to-br from-primary to-accent text-primary-foreground">
          T
        </span>
        <span className="text-base font-bold text-gradient-brand">Threadia</span>
      </Link>

      <nav className="flex flex-col gap-1 p-3">
        {items.map((item) => {
          const active = pathname === item.href || pathname.startsWith(item.href + "/");
          return (
            <Link
              key={item.href}
              href={item.href}
              className={
                "flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors duration-fast " +
                (active
                  ? "bg-primary-subtle text-primary"
                  : "text-foreground hover:bg-primary-subtle hover:text-primary")
              }
            >
              {item.icon}
              {item.label}
            </Link>
          );
        })}
      </nav>
    </aside>
  );
}

function Topbar({ companyId }: { companyId: string }) {
  const { user, logout } = useAuth();
  const router = useRouter();
  const nickname = user?.nickname ?? "";

  const handleLogout = async () => {
    await logout();
    router.push("/login");
  };

  return (
    <header className="sticky top-0 z-sticky flex h-14 shrink-0 items-center justify-between border-b border-border bg-surface/90 px-4 shadow-sm backdrop-blur">
      <span className="text-sm text-muted">Acorn Labs 사무실</span>

      <div className="flex items-center gap-1">
        <IconButton href={`/${companyId}/notifications`} label="알림" badge={MOCK_NOTIFICATION_COUNT}>
          <BellIcon />
        </IconButton>
        <IconButton href={`/${companyId}/dm`} label="채팅·DM" badge={MOCK_DM_UNREAD_COUNT}>
          <ChatIcon />
        </IconButton>

        <span className="mx-2 h-5 w-px bg-border" aria-hidden="true" />

        <div className="flex items-center gap-2 pl-1">
          <span className="flex h-8 w-8 items-center justify-center rounded-full avatar-ring text-xs font-semibold">
            {nickname.slice(0, 1)}
          </span>
          <span className="text-sm font-medium text-foreground">{nickname}</span>
        </div>

        <button
          type="button"
          onClick={handleLogout}
          aria-label="로그아웃"
          title="로그아웃"
          className="relative flex h-9 w-9 items-center justify-center rounded-full text-foreground transition-colors duration-fast hover:bg-primary-subtle hover:text-primary"
        >
          <LogoutIcon />
        </button>
      </div>
    </header>
  );
}

export function AppShell({ children }: { children: React.ReactNode }) {
  const params = useParams<{ companyId: string }>();
  const companyId = params.companyId;

  return (
    <div className="flex min-h-screen">
      <Sidebar companyId={companyId} />
      <div className="flex min-w-0 flex-1 flex-col">
        <Topbar companyId={companyId} />
        <main className="flex-1">{children}</main>
      </div>
    </div>
  );
}
