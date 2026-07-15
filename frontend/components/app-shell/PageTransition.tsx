"use client";

/*
 * PageTransition — components/app-shell/PageTransition.tsx
 *
 * 라우트가 바뀔 때마다 콘텐츠가 살짝 아래에서 떠오르며 페이드인 되는 공용
 * 전환 연출이다. 새 애니메이션을 만들지 않고 globals.css에 이미 있던
 * `animate-enter`(카드 진입 등에 쓰던 fade + translateY 0.75rem, 300ms
 * ease-out)를 그대로 재사용한다 — 화면마다 다른 모션이 섞이지 않게.
 *
 * `key={pathname}`으로 라우트가 바뀔 때마다 이 div를 강제로 새로 마운트해서
 * 애니메이션이 매번 재생되게 한다. RailShell의 `<main>` 안에서만 쓰므로
 * 허브(3D 화면)는 영향받지 않는다.
 *
 * ⚠️ `h-full`을 유지한다 — messages/dm 화면처럼 `h-full` 기준으로 내부
 * flex-col 레이아웃(메시지 목록 + 하단 컴포저)을 잡는 화면들이 있어서, 이
 * 래퍼가 높이를 안 주면 그 화면들의 내부 배치가 깨진다.
 */
import { usePathname } from "next/navigation";

export function PageTransition({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();

  return (
    <div key={pathname} className="h-full animate-enter">
      {children}
    </div>
  );
}
