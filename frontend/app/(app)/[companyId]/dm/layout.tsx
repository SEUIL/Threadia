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
import { useCallback, useEffect, useRef, useState } from "react";
import Link from "next/link";
import { usePathname, useParams, useRouter } from "next/navigation";
import { companyMemberService } from "@/services/company-member.service";
import { directMessageService } from "@/services/direct-message.service";
import { useAuth } from "@/contexts/AuthContext";
import { DmRoomsProvider } from "@/contexts/DmRoomsContext";
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

  /*
   * stale response 방어(PR #173 2차 재리뷰 반영, pktewon): 처음엔 generation이
   * 안 맞는 loadRooms() 응답을 통째로 버렸는데, 이러면 production처럼 초기
   * loadRooms() 호출이 단 하나뿐인 환경에서 그 호출이 markRoomRead()보다 늦게
   * 끝나면 응답 전체가 버려져 rail이 텅 빈 채로 남는 회귀가 있었다(dev의 React
   * Strict Mode는 mount effect를 두 번 실행해 두 번째 호출이 첫 번째의 빈 결과를
   * 덮어써 줬을 뿐 — 그 안전망이 없는 production에서는 재현됐다).
   *
   * 그래서 응답 자체는 항상 반영하되(room 목록 데이터는 신뢰), 그 요청이 시작된
   * "이후"에 markRoomRead()로 로컬 읽음 처리된 room만 서버의 옛 unreadCount로
   * 되돌아가지 않게 개별 보정한다 — 목록 전체 폐기가 아니라 room 단위 merge.
   */
  const roomsGenerationRef = useRef(0);
  // roomId → 그 room을 읽음 처리한 시점의 generation. loadRooms() 응답을 적용할
  // 때, 이 값이 요청 시작 시점의 generation보다 크면(=응답이 시작된 뒤에 읽음
  // 처리가 일어났으면) 응답의 unreadCount를 무시하고 0으로 강제한다.
  const readGenerationsRef = useRef<Map<number, number>>(new Map());

  const loadRooms = useCallback(async () => {
    const generation = ++roomsGenerationRef.current;
    try {
      const { data } = await directMessageService.listRooms(companyId);
      const merged = data.data.rooms.map((r) => {
        const readGeneration = readGenerationsRef.current.get(r.id);
        return readGeneration !== undefined && readGeneration > generation
          ? { ...r, unreadCount: 0 }
          : r;
      });
      setRooms(merged);
    } catch {
      // 네트워크/서버 오류는 목록을 지울 근거가 없다 — 이미 더 최신 결과가
      // 반영돼 있다면(generation 불일치) 그 상태를 그대로 둔다.
      if (roomsGenerationRef.current !== generation) return;
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

  // room 입장 시 markRead API 호출이 성공하면 이걸로 레일 배지를 즉시 지운다
  // (재조회 없이 로컬 state만 갱신). 이 room을 이 generation에 읽었다고 기록해
  // 두면, 이 시점 이전에 시작된 loadRooms() 응답이 나중에 도착해도 이 room의
  // unreadCount만 0으로 보정되고 나머지 목록 데이터는 정상 반영된다.
  const markRoomRead = useCallback((roomId: number) => {
    const generation = ++roomsGenerationRef.current;
    readGenerationsRef.current.set(roomId, generation);
    setRooms((prev) => prev.map((r) => (r.id === roomId ? { ...r, unreadCount: 0 } : r)));
  }, []);

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
      <div className="min-w-0 flex-1">
        <DmRoomsProvider value={{ rooms, markRoomRead }}>{children}</DmRoomsProvider>
      </div>
    </div>
  );
}
