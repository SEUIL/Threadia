"use client";

/*
 * HubOverlay — components/hub/HubOverlay.tsx
 *
 * S03(3D 사무실 메인 허브) 위에 얹는 HTML 오버레이. 3D 씬은 그대로 두고,
 * Canvas 위에 상단바 + 좌측 메뉴만 얹어서 다른 화면(S04/S09/S11/S13)으로
 * 이동할 수 있게 한다. (관련: docs/03-design/화면정의서.md S03,
 * docs/00-project/sprint-운영/sprint-2-backlog.md 3.2 Company Frontend/Entry)
 *
 * 오브젝트 클릭으로 진입하는 방식은 이번 범위가 아니다 — 이번엔 오버레이 UI만.
 *
 * ⚠️ MOCK: 회사 컨텍스트(companyId)와 로그인 사용자 role/뱃지 수는 아직
 *    회사 API 연동 전이라 아래 상수로 임시 고정했다. Sprint 2에서 Company
 *    컨텍스트가 붙으면 이 부분을 실제 값으로 교체해야 한다. 사용자 이름은
 *    useAuth()의 실제 로그인 사용자 정보로 교체 완료.
 */
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAuth } from "@/contexts/AuthContext";

type Role = "A" | "P" | "C";

// TODO(Sprint 2): 회사 API 연동 후 실제 회사/역할 정보로 교체
const MOCK_COMPANY_ID = "demo-company";
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

function TopBar() {
  const { user, logout } = useAuth();
  const router = useRouter();
  const nickname = user?.nickname ?? "";

  const handleLogout = async () => {
    await logout();
    router.push("/login");
  };

  return (
    <header className="pointer-events-auto absolute inset-x-0 top-0 z-sticky flex h-14 items-center justify-between border-b border-border bg-surface/90 px-4 shadow-sm backdrop-blur">
      <div className="flex items-center gap-3">
        <span className="grid h-7 w-7 place-items-center rounded-md text-sm font-extrabold bg-gradient-to-br from-primary to-accent text-primary-foreground">
          T
        </span>
        <span className="text-base font-bold text-gradient-brand">Threadia</span>
        <span className="text-sm text-muted">Acorn Labs 사무실</span>
      </div>

      <div className="flex items-center gap-1">
        <IconButton href={`/${MOCK_COMPANY_ID}/notifications`} label="알림" badge={MOCK_NOTIFICATION_COUNT}>
          <BellIcon />
        </IconButton>
        <IconButton href={`/${MOCK_COMPANY_ID}/dm`} label="채팅·DM" badge={MOCK_DM_UNREAD_COUNT}>
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

function SideMenu() {
  const items = [
    { href: `/${MOCK_COMPANY_ID}/projects`, label: "프로젝트", icon: <FolderIcon /> },
    { href: `/${MOCK_COMPANY_ID}/dm`, label: "채팅·DM", icon: <ChatIcon /> },
    { href: `/${MOCK_COMPANY_ID}/notifications`, label: "알림", icon: <BellIcon /> },
  ];
  // C(회사 관리자)만 회사 관리 메뉴가 보인다 (화면정의서 S03 메뉴 노출 규칙)
  if (MOCK_ROLE === "C") {
    items.push({ href: `/${MOCK_COMPANY_ID}/settings`, label: "회사 관리", icon: <BuildingIcon /> });
  }

  return (
    <nav className="pointer-events-auto absolute left-0 top-14 z-sticky flex flex-col gap-1 rounded-r-xl border border-l-0 border-border bg-surface/90 p-2 shadow-md backdrop-blur">
      {items.map((item) => (
        <Link
          key={item.href}
          href={item.href}
          className="flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium text-foreground transition-colors duration-fast hover:bg-primary-subtle hover:text-primary"
        >
          {item.icon}
          {item.label}
        </Link>
      ))}
    </nav>
  );
}

export function HubOverlay() {
  return (
    <div className="pointer-events-none absolute inset-0 z-sticky">
      <TopBar />
      <SideMenu />
    </div>
  );
}
