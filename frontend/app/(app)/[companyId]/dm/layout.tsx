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
 *
 * 이슈 #208 후속(PR #213): openPicker()가 원래 쓰던 `companyMemberService.list()`
 * 는 COMPANY_ADMIN 전용이라 일반 MEMBER는 "+" 눌러도 구성원 목록을 못 받아왔고,
 * 응답에 UserStatus가 없어 정지된 사용자도 후보에 섞여 나올 수 있었다. 전용
 * `GET .../direct-messages/candidates`로 교체한다 — 서버가 이미 ACTIVE membership
 * + ACTIVE user + self 제외까지 다 걸러서 준다. 기존 DM room 상대는 응답에
 * 포함되므로(계약), room 상대 제외만 프론트에서 유지한다.
 */
import { useCallback, useEffect, useRef, useState } from "react";
import Link from "next/link";
import { usePathname, useParams, useRouter } from "next/navigation";
import { directMessageService } from "@/services/direct-message.service";
import { DmRoomsProvider } from "@/contexts/DmRoomsContext";
import type {
  DirectMessageCandidateResponse,
  DirectMessageRoomResponse,
} from "@/types/direct-message.types";

export default function DmLayout({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const { companyId } = useParams<{ companyId: string }>();

  const [rooms, setRooms] = useState<DirectMessageRoomResponse[]>([]);
  const [pickerOpen, setPickerOpen] = useState(false);
  const [candidates, setCandidates] = useState<DirectMessageCandidateResponse[]>([]);
  const [pickerError, setPickerError] = useState<string | null>(null);
  const [starting, setStarting] = useState(false);

  /*
   * stale response 방어(PR #173 2차 재리뷰 반영, pktewon): 처음엔 generation이
   * 안 맞는 loadRooms() 응답을 통째로 버렸는데, 이러면 production처럼 초기
   * loadRooms() 호출이 단 하나뿐인 환경에서 그 호출이 markRoomRead()보다 늦게
   * 끝나면 응답 전체가 버려져 rail이 텅 빈 채로 남는 회귀가 있었다(dev의 React
   * Strict Mode는 mount effect를 두 번 실행해 두 번째 호출이 첫 번째의 빈 결과를
   * 덮어써 줬을 뿐 — 그 안전망이 없는 production에서는 재현됐다). 그래서 한 번
   * "room 단위 merge"(목록은 항상 반영, unreadCount만 보정)로 바꿨다.
   *
   * 3차 재리뷰 반영(pktewon): 그 merge 방식도 완전하지 않았다 — 응답에 "있는"
   * room의 unreadCount만 보정할 뿐, 응답 자체가 옛 목록이라 새로 생긴 room이
   * 아예 빠져 있는 경우(예: startDm 직후 재조회 vs. 그보다 먼저 시작됐던 느린
   * loadRooms())는 막지 못한다 — 더 완전한 최신 목록을 더 불완전한 옛 목록이
   * 덮어쓸 수 있었다.
   *
   * 그래서 두 개의 별도 ref로 나눈다:
   *   - sequenceRef: loadRooms()와 markRoomRead() 양쪽이 공유하는 논리 시계.
   *   - latestLoadSequenceRef: loadRooms()만 증가시킨다. 응답을 받았을 때 이
   *     값이 자신이 시작될 때 찍은 시퀀스와 다르면(=그 사이 더 최신 loadRooms()
   *     호출이 있었으면) 응답 전체를 버린다 — 옛 목록이 새 목록을 덮어쓰는 걸
   *     원천적으로 막는다.
   *   - readGenerationsRef: roomId → 읽음 처리된 시점의 sequence. 위 stale
   *     체크를 통과한(=최신) 응답이라도, 그 요청이 시작된 "이후"에 markRoomRead()
   *     로 로컬 읽음 처리된 room만 서버의 옛 unreadCount로 되돌아가지 않게
   *     개별 보정한다.
   */
  const sequenceRef = useRef(0);
  const latestLoadSequenceRef = useRef(0);
  const readGenerationsRef = useRef<Map<number, number>>(new Map());
  const mountedRef = useRef(true);

  useEffect(() => {
    mountedRef.current = true;
    return () => {
      mountedRef.current = false;
    };
  }, []);

  const loadRooms = useCallback(async () => {
    const mySequence = ++sequenceRef.current;
    latestLoadSequenceRef.current = mySequence;
    try {
      const { data } = await directMessageService.listRooms(companyId);
      // 언마운트됐거나 이 응답보다 더 최신 loadRooms() 호출이 이미 있었다면
      // (=옛 목록이다) 그대로 버린다 — 더 완전한 최신 상태를 덮어쓰지 않는다.
      if (!mountedRef.current || latestLoadSequenceRef.current !== mySequence) return;
      const merged = data.data.rooms.map((r) => {
        const readGeneration = readGenerationsRef.current.get(r.id);
        return readGeneration !== undefined && readGeneration > mySequence
          ? { ...r, unreadCount: 0 }
          : r;
      });
      setRooms(merged);
    } catch {
      // 네트워크/서버 오류는 목록을 지울 근거가 없다 — 이미 화면에 있는 마지막
      // 정상 상태를 그대로 둔다(stale이든 최신이든 동일하게 적용).
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
      const { data } = await directMessageService.listCandidates(companyId);
      const roomPeerIds = new Set(rooms.map((r) => r.otherUser.id));
      setCandidates(data.data.candidates.filter((candidate) => !roomPeerIds.has(candidate.id)));
    } catch {
      setPickerError("구성원 목록을 불러오지 못했습니다.");
    }
  };

  // room 입장 시 markRead API 호출이 성공하면 이걸로 레일 배지를 즉시 지운다
  // (재조회 없이 로컬 state만 갱신). 이 room을 이 sequence에 읽었다고 기록해
  // 두면, 이 시점 이전에 시작된 loadRooms() 응답이 나중에 도착해도 이 room의
  // unreadCount만 보정되고 나머지 목록 데이터는 정상 반영된다.
  //
  // unreadCount는 호출자가 markRead API 응답의 실제 값을 넘긴다(3차 재리뷰
  // 반영, pktewon) — 방 입장과 동시에 새 메시지가 도착했다면 0이 아닐 수 있어
  // 무조건 0으로 강제하면 실제로 안 읽은 메시지의 배지가 사라지는 문제가 있었다.
  const markRoomRead = useCallback((roomId: number, unreadCount = 0) => {
    const generation = ++sequenceRef.current;
    readGenerationsRef.current.set(roomId, generation);
    setRooms((prev) => prev.map((r) => (r.id === roomId ? { ...r, unreadCount } : r)));
  }, []);

  // 새 대화 생성 도중(getOrCreateRoom/loadRooms 대기 중) 사용자가 이 회사의
  // /dm 밖으로(다른 회사, Hub 등) 이미 navigate해서 DmLayout이 unmount됐다면,
  // 뒤늦게 도착한 응답으로 사용자를 원치 않게 새 DM 방으로 강제 이동시키지
  // 않는다(3차 재리뷰 반영, pktewon).
  const startDm = async (otherUserId: number) => {
    if (starting) return;
    setStarting(true);
    setPickerError(null);
    try {
      const { data } = await directMessageService.getOrCreateRoom(companyId, { otherUserId });
      if (!mountedRef.current) return;
      setPickerOpen(false);
      await loadRooms();
      if (!mountedRef.current) return;
      router.push(`/${companyId}/dm/${data.data.room.id}`);
    } catch {
      if (mountedRef.current) setPickerError("대화를 시작하지 못했습니다.");
    } finally {
      if (mountedRef.current) setStarting(false);
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
              {candidates.map((candidate) => (
                <li key={candidate.id}>
                  <button
                    type="button"
                    onClick={() => void startDm(candidate.id)}
                    disabled={starting}
                    className="flex w-full items-center gap-2 rounded-md px-2 py-1.5 text-left text-sm text-foreground transition-colors duration-fast hover:bg-primary-subtle disabled:opacity-50"
                  >
                    <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full avatar-ring text-[11px] font-semibold">
                      {candidate.nickname.slice(0, 1)}
                    </span>
                    {candidate.nickname}
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
