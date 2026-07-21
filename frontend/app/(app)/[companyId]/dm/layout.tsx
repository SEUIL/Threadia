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
import { useEffect, useState } from "react";
import { usePathname, useParams } from "next/navigation";
import { UserAvatar } from "@/components/profile/UserAvatar";
import { invitationErrorMessage } from "@/lib/invitation";
import { directMessageService } from "@/services/direct-message.service";
import type { DirectMessageRoomResponse } from "@/types/direct-message.types";

export default function DmLayout({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const { companyId } = useParams<{ companyId: string }>();
  const [rooms, setRooms] = useState<DirectMessageRoomResponse[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    void (async () => {
      try {
        const { data } = await directMessageService.listRooms(companyId);
        if (!active) return;
        setRooms(data.data.rooms);
        setError(null);
      } catch (loadError) {
        if (active) setError(invitationErrorMessage(loadError, "DM 목록을 불러오지 못했습니다."));
      } finally {
        if (active) setLoading(false);
      }
    })();
    return () => {
      active = false;
    };
  }, [companyId]);

  return (
    <div className="flex h-full">
      <div className="flex w-16 shrink-0 flex-col items-center gap-2 border-r border-border bg-surface py-4">
        {rooms.map((room) => {
          const href = `/${companyId}/dm/${room.id}`;
          const active = pathname === href;
          return (
            <Link
              key={room.id}
              href={href}
              title={room.otherUser.nickname}
              className={
                "flex h-11 w-11 shrink-0 items-center justify-center rounded-full avatar-ring text-sm font-semibold text-foreground transition-opacity duration-fast " +
                (active ? "ring-2 ring-primary" : "opacity-60 hover:opacity-100")
              }
            >
              <UserAvatar nickname={room.otherUser.nickname} avatarUrl={room.otherUser.avatarUrl} userId={room.otherUser.id} className="h-11 w-11 text-sm text-foreground" />
              {room.unreadCount > 0 && <span className="absolute ml-8 mt-8 rounded-full bg-destructive px-1.5 py-0.5 text-[10px] font-semibold leading-none text-on-primary">{room.unreadCount > 9 ? "9+" : room.unreadCount}</span>}
            </Link>
          );
        })}
        {loading && <span className="px-2 text-center text-[10px] text-muted">불러오는 중...</span>}
        {rooms.length === 0 && !loading && !error && <span className="px-2 text-center text-[10px] text-muted">대화 없음</span>}
        {error && <span className="px-2 text-center text-[10px] text-destructive">목록 오류</span>}
      </div>
      <div className="min-w-0 flex-1">{children}</div>
    </div>
  );
}
