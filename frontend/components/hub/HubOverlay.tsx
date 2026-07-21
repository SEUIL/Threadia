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
 * 회사 컨텍스트: useAuth().companyMemberships(WBS 3.2.1, GET /users/me/
 *    company-memberships)로 실제 값을 쓴다. 다중 회사 소속 시 어떤 회사로
 *    진입할지는 화면정의서에 명시가 없어, 우선 companyMemberships[0]
 *    (서버가 companyId asc로 정렬해서 줌)을 쓴다 — 회사 전환 UI는 범위 밖
 *    (설계문서 §8.3, §9 미결 1번).
 */
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { usePendingInvitationCount } from "@/hooks/usePendingInvitationCount";
import { useUnreadNotificationCount } from "@/hooks/useUnreadNotificationCount";
import { useDmUnreadCount } from "@/hooks/useDmUnreadCount";
import { useIsPlatformAdmin } from "@/hooks/useIsPlatformAdmin";
import { UserAvatar } from "@/components/profile/UserAvatar";
import { ProfileEditModal } from "@/components/profile/ProfileEditModal";

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

function HelpIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" className="h-5 w-5">
      <circle cx="12" cy="12" r="8.5" stroke="currentColor" strokeWidth="1.6" />
      <path
        d="M9.8 9.5a2.2 2.2 0 1 1 3.3 1.9c-.7.4-1.1.9-1.1 1.6v.3"
        stroke="currentColor"
        strokeWidth="1.6"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <circle cx="12" cy="16.3" r="0.9" fill="currentColor" />
    </svg>
  );
}

function ShieldIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" className="h-5 w-5">
      <path
        d="M12 3.5 5 6v5.5c0 4.4 3 8.1 7 9 4-.9 7-4.6 7-9V6l-7-2.5Z"
        stroke="currentColor"
        strokeWidth="1.6"
        strokeLinejoin="round"
      />
      <path d="m9 12 2 2 4-4.5" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />
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
  const { user, companyMemberships, logout } = useAuth();
  const router = useRouter();
  const [open, setOpen] = useState(false);
  const [profileOpen, setProfileOpen] = useState(false);
  const nickname = user?.nickname ?? "";
  const pendingInvitationCount = usePendingInvitationCount();
  // 다중 회사 소속 시 회사 전환 UI는 범위 밖 — 첫 번째 회사로 진입한다.
  const primaryMembership = companyMemberships[0] ?? null;
  const unreadNotificationCount = useUnreadNotificationCount(primaryMembership?.companyId);
  const dmUnreadCount = useDmUnreadCount();
  const isPlatformAdmin = useIsPlatformAdmin();

  const handleLogout = async () => {
    await logout();
    router.push("/login");
  };

  // company-agnostic한 "받은 초대"/"문의하기"는 소속 회사가 없어도 항상 보인다
  // — 신규 유저가 첫 초대를 확인하거나 문의를 남길 유일한 통로다.
  const items: NavItem[] = [
    { href: "/invitations", label: "받은 초대", icon: <MailIcon />, badge: pendingInvitationCount },
    { href: "/support", label: "문의하기", icon: <HelpIcon /> },
  ];
  if (primaryMembership) {
    const { companyId, role } = primaryMembership;
    items.unshift(
      { href: `/${companyId}/projects`, label: "프로젝트", icon: <FolderIcon /> },
      { href: `/${companyId}/dm`, label: "채팅·DM", icon: <ChatIcon />, badge: dmUnreadCount }
    );
    items.push({ href: `/${companyId}/notifications`, label: "알림", icon: <BellIcon />, badge: unreadNotificationCount });
    // C(회사 관리자)만 회사 관리 메뉴가 보인다 (화면정의서 S03 메뉴 노출 규칙, RailShell과 동일)
    if (role === "COMPANY_ADMIN") {
      items.push({ href: `/${companyId}/settings`, label: "회사 관리", icon: <BuildingIcon /> });
    }
  }
  // platform admin(회사 membership과 무관한 별도 authority)만 S15 콘솔 진입점이 보인다.
  // company-agnostic이라 primaryMembership 유무와 무관하게 노출한다(RailShell과 동일 원칙).
  if (isPlatformAdmin) {
    items.push({ href: "/platform", label: "플랫폼 관리자", icon: <ShieldIcon /> });
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

      {/* 아바타/로그아웃 — RailShell과 동일하게 레일 하단 고정.
          아바타를 누르면 프로필 수정 모달이 열린다(WBS 4.2.2, RailShell과 동일) */}
      <div className="mt-auto flex flex-col items-center gap-2">
        <button
          type="button"
          onClick={() => setProfileOpen(true)}
          title={`${nickname} — 프로필 수정`}
          // 버튼 안이 이니셜 한 글자라 aria-label이 없으면 스크린리더가 "A 버튼"으로만 읽는다
          // (title은 내용이 있으면 접근성 이름으로 쓰이지 않는다). RailShell과 동일.
          aria-label={`${nickname} — 프로필 수정`}
          className="rounded-full transition-opacity duration-fast hover:opacity-80"
        >
          <UserAvatar nickname={nickname} avatarUrl={user?.avatarUrl} userId={user?.id} />
        </button>
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
          <p className="mt-0.5 text-xs text-chrome-muted">
            {primaryMembership?.companyName ?? "소속 회사 없음"}
          </p>
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
          <button
            type="button"
            onClick={() => setProfileOpen(true)}
            title="프로필 수정"
            aria-label={`${nickname} — 프로필 수정`}
            className="flex min-w-0 flex-1 items-center gap-2 rounded-lg px-1 py-1 text-left transition-colors duration-fast hover:bg-chrome-hover"
          >
            <UserAvatar nickname={nickname} avatarUrl={user?.avatarUrl} userId={user?.id} />
            <span className="min-w-0 flex-1 truncate text-sm font-medium text-chrome-foreground">
              {nickname}
            </span>
          </button>
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

      {/* 모달은 pointer-events-auto인 레일 안에서 렌더한다 — 바깥 HubOverlay 래퍼는
          3D 씬 조작을 막지 않으려고 pointer-events-none이라 거기 두면 클릭이 안 먹는다 */}
      <ProfileEditModal open={profileOpen} onClose={() => setProfileOpen(false)} />
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
