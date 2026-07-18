"use client";

/*
 * DM 아이콘 레일 — dm/layout.tsx
 *
 * 채팅 상대를 디스코드 서버 목록처럼 좌측에 아바타로 나열하고, 아이콘 클릭
 * 한 번으로 대화를 바로 전환한다(2026-07-06). `/dm`(인덱스)은 첫 번째 방으로
 * 바로 리다이렉트한다(dm/page.tsx).
 *
 * WBS 4.7.3.2 — 4.7.3.1 Backend(PR #104)의 실 API로 연동했다.
 *   - 방 목록(`GET /companies/{companyId}/direct-messages/rooms`)은 이미 대화가
 *     있는 상대만 준다. "+" 버튼으로 새 대화 상대(같은 회사 ACTIVE 구성원, 나
 *     제외, 아직 방이 없는 사람만)를 골라 get-or-create로 방을 연다.
 *   - unread가 있는 방은 아바타에 점 배지를 보여준다(설계 메모: unread count만
 *     다룬다, read receipt는 제외).
 */
import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { usePathname, useParams, useRouter } from "next/navigation";
import { companyMemberService } from "@/services/company-member.service";
import { directMessageService } from "@/services/direct-message.service";
import { useAuth } from "@/contexts/AuthContext";
import type { CompanyMemberResponse } from "@/types/company.types";
import type { DirectMessageRoomResponse } from "@/types/direct-message.types";

export default function DmLayout({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const { companyId } = useParams<{ companyId: string }>();
  const { user } = useAuth();

  const [rooms, setRooms] = useState<DirectMessageRoomResponse[]>([]);
  const [pickerOpen, setPickerOpen] = useState(false);
  const [candidates, setCandidates] = useState<CompanyMemberResponse[]>([]);
  const [pickerError, setPickerError] = useState<string | null>(null);
  const [starting, setStarting] = useState(false);

  const loadRooms = useCallback(async () => {
    try {
      const { data } = await directMessageService.listRooms(companyId);
      setRooms(data.data.rooms);
    } catch {
      setRooms([]);
    }
  }, [companyId]);

  useEffect(() => {
    (async () => {
      await loadRooms();
    })();
  }, [loadRooms]);

  const openPicker = async () => {
    setPickerError(null);
    setPickerOpen((open) => !open);
    try {
      const { data } = await companyMemberService.list(companyId, { status: "ACTIVE" });
      const roomPeerIds = new Set(rooms.map((r) => r.otherUser.id));
      setCandidates(
        data.data.companyMembers.filter(
          (member) => member.user.id !== user?.id && !roomPeerIds.has(member.user.id)
        )
      );
    } catch {
      setPickerError("구성원 목록을 불러오지 못했습니다.");
    }
  };

  const startDm = async (otherUserId: number) => {
    if (starting) return;
    setStarting(true);
    setPickerError(null);
    try {
      const { data } = await directMessageService.getOrCreateRoom(companyId, { otherUserId });
      setPickerOpen(false);
      await loadRooms();
      router.push(`/${companyId}/dm/${data.data.room.id}`);
    } catch {
      setPickerError("대화를 시작하지 못했습니다.");
    } finally {
      setStarting(false);
    }
  };

  return (
    <div className="flex h-full">
      <div className="relative flex w-16 shrink-0 flex-col items-center gap-2 border-r border-border bg-surface py-4">
        {rooms.map((room) => {
          const href = `/${companyId}/dm/${room.id}`;
          const active = pathname === href;
          return (
            <Link
              key={room.id}
              href={href}
              title={room.otherUser.nickname}
              className="relative flex h-11 w-11 shrink-0 items-center justify-center"
            >
              <span
                className={
                  "flex h-11 w-11 items-center justify-center rounded-full avatar-ring text-sm font-semibold text-foreground transition-opacity duration-fast " +
                  (active ? "ring-2 ring-primary" : "opacity-60 hover:opacity-100")
                }
              >
                {room.otherUser.nickname.slice(0, 1)}
              </span>
              {room.unreadCount > 0 && (
                <span className="absolute -right-0.5 -top-0.5 h-3 w-3 rounded-full border-2 border-surface bg-destructive" />
              )}
            </Link>
          );
        })}

        <button
          type="button"
          onClick={() => void openPicker()}
          title="새 대화 시작"
          className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full border border-dashed border-border text-lg text-muted transition-colors duration-fast hover:border-primary hover:text-primary"
        >
          +
        </button>

        {pickerOpen && (
          <div className="absolute left-16 top-4 z-overlay w-56 rounded-xl border border-border bg-surface p-2 shadow-lg">
            <p className="px-2 py-1 text-[11px] font-semibold uppercase tracking-wide text-muted">
              새 대화 시작
            </p>
            {pickerError && <p className="px-2 py-1 text-xs text-destructive">{pickerError}</p>}
            <ul className="max-h-64 overflow-y-auto">
              {candidates.map((member) => (
                <li key={member.id}>
                  <button
                    type="button"
                    onClick={() => void startDm(member.user.id)}
                    disabled={starting}
                    className="flex w-full items-center gap-2 rounded-md px-2 py-1.5 text-left text-sm text-foreground transition-colors duration-fast hover:bg-primary-subtle disabled:opacity-50"
                  >
                    <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full avatar-ring text-[11px] font-semibold">
                      {member.user.nickname.slice(0, 1)}
                    </span>
                    {member.user.nickname}
                  </button>
                </li>
              ))}
              {candidates.length === 0 && !pickerError && (
                <li className="px-2 py-1.5 text-xs text-muted">대화를 시작할 구성원이 없습니다.</li>
              )}
            </ul>
          </div>
        )}
      </div>
      <div className="min-w-0 flex-1">{children}</div>
    </div>
  );
}
