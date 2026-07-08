"use client";

/*
 * HubOverlay — components/hub/HubOverlay.tsx
 *
 * S03(3D 사무실 메인 허브) 위에 얹는 HTML 오버레이. 3D 씬은 그대로 두고,
 * Canvas 위에 레일 내비게이션만 얹어서 다른 화면(S04/S09/S11/S13)으로
 * 이동할 수 있게 한다.
 *
 * ⚠️ 2026-07-06: 원래 여기 있던 상단바+좌측 박스 메뉴는 옛 AppShell 스타일
 * 그대로였다 — 다른 모든 (app) 화면은 이미 RailShell(레일+플라이아웃)로
 * 바꿨는데 허브만 톤이 달랐다. 그래서 오버레이 구조(3D Canvas는 항상
 * 풀스크린, HTML은 그 위에 절대 위치로 얹는 방식)는 그대로 두고 내용물만
 * RailShell과 같은 레일 스타일로 다시 스킨했다. `<RailShell>`을 그대로
 * 씌우면 실제 flex 레이아웃이 생겨서 Canvas 폭이 레일 너비만큼 줄고 카메라
 * 구도가 틀어지므로, RailShell을 감싸 쓰지 않고 이 오버레이 전용으로 따로
 * 구현했다(로직/스타일은 RailShell의 Rail과 동일하게 맞춤). 배경 색(navy)은
 * 추후 별도로 다시 다듬을 예정이라 지금은 RailShell과 동일한 값을 그대로
 * 가져다 썼다.
 *
 * ⚠️ MOCK: 회사 컨텍스트(companyId)와 로그인 사용자 role/뱃지 수는 아직
 *    회사 API 연동 전이라 아래 상수로 임시 고정했다. Sprint 2에서 Company
 *    컨텍스트가 붙으면 이 부분을 실제 값으로 교체해야 한다. 사용자 이름은
 *    useAuth()의 실제 로그인 사용자 정보로 교체 완료.
 */
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { usePendingInvitationCount } from "@/hooks/usePendingInvitationCount";

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

function MailIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" className="h-5 w-5">
      <path
        d="M4.5 6h15A1.5 1.5 0 0 1 21 7.5v9a1.5 1.5 0 0 1-1.5 1.5h-15A1.5 1.5 0 0 1 3 16.5v-9A1.5 1.5 0 0 1 4.5 6Z"
        stroke="currentColor"
        strokeWidth="1.6"
        strokeLinejoin="round"
      />
      <path d="m4 7 8 6 8-6" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />
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

function HubRail() {
  const { user, logout } = useAuth();
  const router = useRouter();
  const [open, setOpen] = useState(false);
  const nickname = user?.nickname ?? "";
  const pendingInvitationCount = usePendingInvitationCount();

  const handleLogout = async () => {
    await logout();
    router.push("/login");
  };

  const items: NavItem[] = [
    { href: `/${MOCK_COMPANY_ID}/projects`, label: "프로젝트", icon: <FolderIcon /> },
    { href: `/${MOCK_COMPANY_ID}/dm`, label: "채팅·DM", icon: <ChatIcon />, badge: MOCK_DM_UNREAD_COUNT },
    // company-agnostic — 아직 어느 회사에도 소속 안 된 신규 유저의 유일한 진입점이기도 하다.
    { href: "/invitations", label: "받은 초대", icon: <MailIcon />, badge: pendingInvitationCount },
    { href: `/${MOCK_COMPANY_ID}/notifications`, label: "알림", icon: <BellIcon />, badge: MOCK_NOTIFICATION_COUNT },
  ];
  // C(회사 관리자)만 회사 관리 메뉴가 보인다 (화면정의서 S03 메뉴 노출 규칙, RailShell과 동일)
  if (MOCK_ROLE === "C") {
    items.push({ href: `/${MOCK_COMPANY_ID}/settings`, label: "회사 관리", icon: <BuildingIcon /> });
  }

  return (
    <div
      onMouseEnter={() => setOpen(true)}
      onMouseLeave={() => setOpen(false)}
      className="pointer-events-auto relative z-sticky flex h-full w-16 shrink-0 flex-col items-center gap-3 bg-chrome py-4"
    >
      <div
        title="허브"
        className="grid h-9 w-9 place-items-center rounded-lg text-sm font-extrabold bg-gradient-to-br from-primary to-accent text-primary-foreground"
      >
        T
      </div>

      <span className="h-px w-8 bg-chrome-foreground/10" aria-hidden="true" />

      {items.map((item) => (
        <Link
          key={item.href}
          href={item.href}
          title={item.label}
          className="relative grid h-9 w-9 place-items-center rounded-lg text-chrome-muted transition-colors duration-fast hover:bg-chrome-hover hover:text-chrome-foreground"
        >
          {item.icon}
          {typeof item.badge === "number" && <Badge count={item.badge} />}
        </Link>
      ))}

      {/* 아바타/로그아웃 — RailShell과 동일하게 레일 하단 고정 */}
      <div className="mt-auto flex flex-col items-center gap-2">
        <span
          title={nickname}
          className="flex h-8 w-8 items-center justify-center rounded-full avatar-ring text-xs font-semibold text-chrome-foreground"
        >
          {nickname.slice(0, 1)}
        </span>
        <button
          type="button"
          onClick={handleLogout}
          title="로그아웃"
          className="grid h-9 w-9 place-items-center rounded-lg text-chrome-muted transition-colors duration-fast hover:bg-chrome-hover hover:text-chrome-foreground"
        >
          <LogoutIcon />
        </button>
      </div>

      {/* 플라이아웃 — hover 시 확장, RailShell과 동일 */}
      <div
        className="absolute left-16 top-0 flex h-full w-56 origin-left flex-col rounded-r-2xl bg-chrome py-4 shadow-xl transition-all duration-normal ease-out"
        style={{
          transform: open ? "scaleX(1)" : "scaleX(0)",
          opacity: open ? 1 : 0,
          pointerEvents: open ? "auto" : "none",
        }}
      >
        <div className="px-4 pb-3">
          <p className="text-sm font-bold text-chrome-foreground">Threadia</p>
          <p className="mt-0.5 text-xs text-chrome-muted">Acorn Labs 사무실</p>
        </div>
        {items.map((item) => (
          <Link
            key={item.href}
            href={item.href}
            className="mx-2 mt-1 flex items-center justify-between rounded-lg px-3 py-2 text-sm font-medium text-chrome-muted transition-colors duration-fast hover:bg-chrome-hover hover:text-chrome-foreground"
          >
            {item.label}
            {typeof item.badge === "number" && item.badge > 0 && (
              <span className="rounded-full bg-destructive px-1.5 py-0.5 text-[10px] font-semibold leading-none text-on-primary">
                {item.badge > 9 ? "9+" : item.badge}
              </span>
            )}
          </Link>
        ))}

        <div className="mt-auto flex items-center gap-2 border-t border-chrome-foreground/10 px-4 pt-3">
          <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full avatar-ring text-xs font-semibold text-chrome-foreground">
            {nickname.slice(0, 1)}
          </span>
          <span className="min-w-0 flex-1 truncate text-sm font-medium text-chrome-foreground">{nickname}</span>
          <button
            type="button"
            onClick={handleLogout}
            title="로그아웃"
            className="grid h-8 w-8 shrink-0 place-items-center rounded-lg text-chrome-muted transition-colors duration-fast hover:bg-chrome-hover hover:text-chrome-foreground"
          >
            <LogoutIcon />
          </button>
        </div>
      </div>
    </div>
  );
}

export function HubOverlay() {
  return (
    <div className="pointer-events-none absolute inset-0 z-sticky flex">
      <HubRail />
    </div>
  );
}
