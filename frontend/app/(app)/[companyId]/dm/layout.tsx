"use client";

/*
 * DM 아이콘 레일 — dm/layout.tsx
 *
 * 채팅 상대를 디스코드 서버 목록처럼 좌측에 아바타로 나열하고, 아이콘 클릭
 * 한 번으로 대화를 바로 전환한다(2026-07-06). 기존엔 목록 화면(`/dm`)에서
 * 직사각형 방 목록을 보고 클릭해야 방(`/dm/[roomId]`)으로 들어가는 2단계
 * 구조였다 — 그 사이에 낀 "목록 화면"을 걷어내고, 이 레일이 항상 떠 있는
 * 채로 대화 내용만 바뀌게 만들었다. `/dm`(인덱스)은 첫 번째 방으로 바로
 * 리다이렉트한다(dm/page.tsx).
 */
import Link from "next/link";
import { usePathname, useParams } from "next/navigation";
import { MOCK_ROOMS } from "./mockRooms";

export default function DmLayout({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const { companyId } = useParams<{ companyId: string }>();

  return (
    <div className="flex h-full">
      <div className="flex w-16 shrink-0 flex-col items-center gap-2 border-r border-border bg-surface py-4">
        {MOCK_ROOMS.map((room) => {
          const href = `/${companyId}/dm/${room.roomId}`;
          const active = pathname === href;
          return (
            <Link
              key={room.roomId}
              href={href}
              title={room.name}
              className={
                "flex h-11 w-11 shrink-0 items-center justify-center rounded-full avatar-ring text-sm font-semibold text-foreground transition-opacity duration-fast " +
                (active ? "ring-2 ring-primary" : "opacity-60 hover:opacity-100")
              }
            >
              {room.name.slice(0, 1)}
            </Link>
          );
        })}
      </div>
      <div className="min-w-0 flex-1">{children}</div>
    </div>
  );
}
