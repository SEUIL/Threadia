"use client";

/*
 * RailShell — components/app-shell/RailShell.tsx
 *
 * AppShell(지속 좌측 사이드바 + 상단바)을 대체하는 chrome. 허브(3D 화면)를
 * 제외한 모든 (app) 화면에 적용된다.
 *
 * 배경: 기존 AppShell의 "항상 펼쳐진 사이드바 + 고정 상단바" 조합을 hover 시에만
 * 펼쳐지는 아이콘 레일 + 상단바 제거 + 하단 플로팅 커맨드바로 전면 교체했다.
 * 실험 과정과 근거는
 * docs/08-members/member-정철진/기술-연구/레이아웃-구조-실험-레일-그래프-조합.md
 * 참고.
 *
 * 적용 위치: app/(app)/[companyId]/layout.tsx 에서만 감싼다. hub, invitations는
 * [companyId] 밖에 있어서 영향을 받지 않는다(허브는 자체 3D 오버레이 유지).
 *
 * ⚠️ MOCK: 회사 컨텍스트/역할은 이전 AppShell과 동일하게 아직 회사 API가 없어서
 * 상수로 고정했다. Sprint 2 연동 후 실제 값으로 교체해야 한다.
 */
import Link from "next/link";
import { usePathname, useParams, useRouter } from "next/navigation";
import { useState } from "react";
import { useAuth } from "@/contexts/AuthContext";

type Role = "A" | "P" | "C";

// TODO(Sprint 2): 회사 API 연동 후 실제 회사/역할 정보로 교체 (이전 AppShell과 동일 기준)
const MOCK_ROLE: Role = "C";
const MOCK_COMPANY_NAME = "Acorn Labs 사무실";
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

type NavItem = { href: string; label: string; icon: React.ReactNode; badge?: number };

function Rail({ companyId }: { companyId: string }) {
  const pathname = usePathname();
  const [open, setOpen] = useState(false);

  const items: NavItem[] = [
    { href: `/${companyId}/projects`, label: "프로젝트", icon: <FolderIcon /> },
    { href: `/${companyId}/dm`, label: "채팅·DM", icon: <ChatIcon />, badge: MOCK_DM_UNREAD_COUNT },
    { href: `/${companyId}/notifications`, label: "알림", icon: <BellIcon />, badge: MOCK_NOTIFICATION_COUNT },
  ];
  // C(회사 관리자)만 회사 관리 메뉴가 보인다 (화면정의서 S03 메뉴 노출 규칙, 이전 AppShell과 동일)
  if (MOCK_ROLE === "C") {
    items.push({ href: `/${companyId}/settings`, label: "회사 관리", icon: <BuildingIcon /> });
  }

  return (
    <div
      onMouseEnter={() => setOpen(true)}
      onMouseLeave={() => setOpen(false)}
      className="relative z-sticky hidden h-full w-16 shrink-0 flex-col items-center gap-3 bg-chrome py-4 md:flex"
    >
      <Link
        href="/hub"
        title="허브"
        className="grid h-9 w-9 place-items-center rounded-lg text-sm font-extrabold bg-gradient-to-br from-primary to-accent text-primary-foreground"
      >
        T
      </Link>

      <span className="h-px w-8 bg-chrome-foreground/10" aria-hidden="true" />

      {items.map((item) => {
        const active = pathname === item.href || pathname.startsWith(item.href + "/");
        return (
          <Link
            key={item.href}
            href={item.href}
            title={item.label}
            className={
              "relative grid h-9 w-9 place-items-center rounded-lg transition-colors duration-fast " +
              (active
                ? "bg-primary text-primary-foreground"
                : "text-chrome-muted hover:bg-chrome-hover hover:text-chrome-foreground")
            }
          >
            {item.icon}
            {typeof item.badge === "number" && <Badge count={item.badge} />}
          </Link>
        );
      })}

      {/* 플라이아웃 — hover 시 확장, 레이아웃을 밀지 않고 위에 뜬다 */}
      <div
        className="absolute left-16 top-0 h-full w-56 origin-left rounded-r-2xl bg-chrome py-4 shadow-xl transition-all duration-normal ease-out"
        style={{
          transform: open ? "scaleX(1)" : "scaleX(0)",
          opacity: open ? 1 : 0,
          pointerEvents: open ? "auto" : "none",
        }}
      >
        <div className="px-4 pb-3">
          <p className="text-sm font-bold text-chrome-foreground">Threadia</p>
          <p className="mt-0.5 text-xs text-chrome-muted">{MOCK_COMPANY_NAME}</p>
        </div>
        {items.map((item) => {
          const active = pathname === item.href || pathname.startsWith(item.href + "/");
          return (
            <Link
              key={item.href}
              href={item.href}
              className={
                "mx-2 mt-1 flex items-center justify-between rounded-lg px-3 py-2 text-sm font-medium transition-colors duration-fast " +
                (active
                  ? "bg-chrome-hover text-chrome-foreground"
                  : "text-chrome-muted hover:bg-chrome-hover hover:text-chrome-foreground")
              }
            >
              {item.label}
              {typeof item.badge === "number" && item.badge > 0 && (
                <span className="rounded-full bg-destructive px-1.5 py-0.5 text-[10px] font-semibold leading-none text-on-primary">
                  {item.badge > 9 ? "9+" : item.badge}
                </span>
              )}
            </Link>
          );
        })}
      </div>
    </div>
  );
}

function FloatingCommandBar({ companyId }: { companyId: string }) {
  const { user, logout } = useAuth();
  const router = useRouter();
  const nickname = user?.nickname ?? "";

  const handleLogout = async () => {
    await logout();
    router.push("/login");
  };

  return (
    <div className="pointer-events-none fixed inset-x-0 bottom-5 z-sticky flex justify-center px-4">
      <div className="pointer-events-auto flex items-center gap-1 rounded-full bg-chrome px-2 py-2 shadow-xl">
        <Link
          href={`/${companyId}/notifications`}
          title="알림"
          className="relative grid h-9 w-9 place-items-center rounded-full text-chrome-muted transition-colors duration-fast hover:bg-chrome-hover hover:text-chrome-foreground"
        >
          <BellIcon />
          <Badge count={MOCK_NOTIFICATION_COUNT} />
        </Link>
        <Link
          href={`/${companyId}/dm`}
          title="채팅·DM"
          className="relative grid h-9 w-9 place-items-center rounded-full text-chrome-muted transition-colors duration-fast hover:bg-chrome-hover hover:text-chrome-foreground"
        >
          <ChatIcon />
          <Badge count={MOCK_DM_UNREAD_COUNT} />
        </Link>

        <span className="mx-1 h-6 w-px bg-chrome-foreground/15" aria-hidden="true" />

        <div className="flex items-center gap-2 px-1">
          <span className="flex h-8 w-8 items-center justify-center rounded-full avatar-ring text-xs font-semibold text-chrome-foreground">
            {nickname.slice(0, 1)}
          </span>
          <span className="hidden text-sm font-medium text-chrome-foreground sm:inline">{nickname}</span>
        </div>

        <button
          type="button"
          onClick={handleLogout}
          title="로그아웃"
          className="grid h-9 w-9 place-items-center rounded-full text-chrome-muted transition-colors duration-fast hover:bg-chrome-hover hover:text-chrome-foreground"
        >
          <LogoutIcon />
        </button>
      </div>
    </div>
  );
}

export function RailShell({ children }: { children: React.ReactNode }) {
  const params = useParams<{ companyId: string }>();
  const companyId = params.companyId;

  return (
    <div className="flex h-screen overflow-hidden">
      <Rail companyId={companyId} />
      <main className="min-h-0 min-w-0 flex-1 overflow-y-auto pb-24">{children}</main>
      <FloatingCommandBar companyId={companyId} />
    </div>
  );
}
