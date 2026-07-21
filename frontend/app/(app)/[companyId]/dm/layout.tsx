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
 *   - unread가 있는 방은 아바타에 배지로 실제 개수를 보여준다(9개 초과는 "9+",
 *     WBS 4.11 PR #215 반영 — read receipt는 여전히 범위 밖, unread count만 다룬다).
 *
 * 이슈 #208 후속(PR #213): openPicker()가 원래 쓰던 `companyMemberService.list()`
 * 는 COMPANY_ADMIN 전용이라 일반 MEMBER는 "+" 눌러도 구성원 목록을 못 받아왔고,
 * 응답에 UserStatus가 없어 정지된 사용자도 후보에 섞여 나올 수 있었다. 전용
 * `GET .../direct-messages/candidates`로 교체한다 — 서버가 이미 ACTIVE membership
 * + ACTIVE user + self 제외까지 다 걸러서 준다. 기존 DM room 상대는 응답에
 * 포함되므로(계약), room 상대 제외만 프론트에서 유지한다.
 *
 * WBS 4.11(PR #215) 반영: room 아이콘을 이니셜 원 대신 UserAvatar(첨부 attachment
 * 기반 프로필 이미지 지원)로 바꿨다 — 아바타 표시 로직을 한 곳(UserAvatar)에
 * 모아두는 기존 원칙과 동일하게 맞춘다.
 */
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import Link from "next/link";
import { usePathname, useParams, useRouter } from "next/navigation";
import { UserAvatar } from "@/components/profile/UserAvatar";
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
  // candidates는 서버가 준 원본 그대로 저장한다(필터링 안 함) — "기존 room 상대
  // 제외"는 아래 visibleCandidates(useMemo)에서 매 렌더 최신 rooms 기준으로
  // 계산한다(4차 재리뷰 반영, pktewon: blocker 3, 아래 openPicker 주석 참고).
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
   *   - readGenerationsRef: roomId → { generation, unreadCount }. 위 stale
   *     체크를 통과한(=최신) 응답이라도, 그 요청이 시작된 "이후"에 markRoomRead()
   *     로 로컬 읽음 처리된 room만 그 markRead 응답의 실제 unreadCount로 보정한다
   *     (무조건 0이 아니다 — 4차 재리뷰 반영, pktewon: blocker 2. 이전엔
   *     generation만 저장하고 여기서 무조건 0으로 되돌려서, mark-read가
   *     unreadCount=1을 돌려준 경우에도 지연 도착한 loadRooms 응답이 그 값을
   *     0으로 다시 덮어썼다).
   */
  const sequenceRef = useRef(0);
  const latestLoadSequenceRef = useRef(0);
  const readGenerationsRef = useRef<Map<number, { generation: number; unreadCount: number }>>(new Map());
  const mountedRef = useRef(true);
  // startDm 응답이 늦게 도착했을 때 "그 요청을 시작한 시점과 같은 화면인지"를
  // 판단하는 데 쓴다(blocker 4). pathname은 리렌더 시점의 클로저 값이라 응답
  // 콜백 안에서 "지금" 값을 읽으려면 ref로 항상 최신화해둬야 한다.
  const pathnameRef = useRef(pathname);
  const startDmSequenceRef = useRef(0);

  useEffect(() => {
    pathnameRef.current = pathname;
  }, [pathname]);

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
        const read = readGenerationsRef.current.get(r.id);
        return read !== undefined && read.generation > mySequence ? { ...r, unreadCount: read.unreadCount } : r;
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

  /*
   * blocker 3(4차 재리뷰, pktewon): candidates를 필터링해서 저장했더니, 응답을
   * 기다리는 동안(await) room 목록이 바뀌면(예: 다른 탭/폴링으로 새 room 생김)
   * 그 시점 이후의 최신 rooms가 아니라 "요청을 시작했을 때의" rooms로 필터링된
   * 결과가 저장돼버렸다 — await 너머의 클로저는 그 시점 값을 그대로 들고 있기
   * 때문이다. candidates는 항상 서버 원본 그대로 저장하고, "기존 room 상대
   * 제외"는 렌더 시점마다 최신 rooms로 다시 계산되는 useMemo(visibleCandidates)
   * 에서 한다 — 그러면 응답이 언제 오든 필터링 결과가 항상 최신 rooms를 반영한다.
   */
  const pickerRequestSequenceRef = useRef(0);
  const openPicker = async () => {
    setPickerError(null);
    setPickerOpen((open) => !open);
    const mySequence = ++pickerRequestSequenceRef.current;
    try {
      const { data } = await directMessageService.listCandidates(companyId);
      if (!mountedRef.current || pickerRequestSequenceRef.current !== mySequence) return;
      setCandidates(data.data.candidates);
    } catch {
      if (!mountedRef.current || pickerRequestSequenceRef.current !== mySequence) return;
      // 재요청이 실패하면 화면에 이전 후보가 그대로 남아 "필터링 안 된 옛 목록"
      // 처럼 보일 수 있으니 비운다(4차 재리뷰 반영, pktewon).
      setCandidates([]);
      setPickerError("구성원 목록을 불러오지 못했습니다.");
    }
  };

  const roomPeerIds = useMemo(() => new Set(rooms.map((r) => r.otherUser.id)), [rooms]);
  const visibleCandidates = useMemo(
    () => candidates.filter((candidate) => !roomPeerIds.has(candidate.id)),
    [candidates, roomPeerIds]
  );

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
    readGenerationsRef.current.set(roomId, { generation, unreadCount });
    setRooms((prev) => prev.map((r) => (r.id === roomId ? { ...r, unreadCount } : r)));
  }, []);

  /*
   * 새 대화 생성 도중(getOrCreateRoom/loadRooms 대기 중) 사용자가 이 회사의
   * /dm 밖으로(다른 회사, Hub 등) 이미 navigate해서 DmLayout이 unmount됐다면,
   * 뒤늦게 도착한 응답으로 사용자를 원치 않게 새 DM 방으로 강제 이동시키지
   * 않는다(3차 재리뷰 반영, pktewon).
   *
   * 4차 재리뷰 반영(pktewon, blocker 4): 기존 room 링크(/dm/301 → /dm/302)로
   * 이동하는 경우도 같은 문제라 pathname(startPathname)도 같이 저장해 응답
   * 시점에 비교한다 — 실측해보니 이 앱에서는 room 사이 이동에도 DmLayout이
   * 그때그때 재마운트돼(mountedRef만으로도 이미 이 특정 케이스는 걸러진다)
   * pathname 체크가 지금은 중복 방어처럼 보이지만, 라우팅 동작이 바뀌어
   * DmLayout이 진짜 persistent해지는 경우까지 대비해 명시적으로 남겨둔다
   * (리뷰에서 정확히 이 방식을 요청하기도 했다).
   */
  const startDm = async (otherUserId: number) => {
    if (starting) return;
    setStarting(true);
    setPickerError(null);
    const mySequence = ++startDmSequenceRef.current;
    const startPathname = pathname;
    try {
      const { data } = await directMessageService.getOrCreateRoom(companyId, { otherUserId });
      if (!mountedRef.current) return;
      if (startDmSequenceRef.current !== mySequence) return;
      setPickerOpen(false);
      await loadRooms();
      if (!mountedRef.current || startDmSequenceRef.current !== mySequence) return;
      if (pathnameRef.current !== startPathname) return;
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
              <UserAvatar
                nickname={room.otherUser.nickname}
                avatarUrl={room.otherUser.avatarUrl}
                userId={room.otherUser.id}
                className={
                  "h-11 w-11 text-sm text-foreground transition-opacity duration-fast " +
                  (active ? "ring-2 ring-primary" : "opacity-60 hover:opacity-100")
                }
              />
              {room.unreadCount > 0 && (
                <span className="absolute -right-0.5 -top-0.5 rounded-full border-2 border-surface bg-destructive px-1.5 py-0.5 text-[10px] font-semibold leading-none text-on-primary">
                  {room.unreadCount > 9 ? "9+" : room.unreadCount}
                </span>
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
              {visibleCandidates.map((candidate) => (
                <li key={candidate.id}>
                  <button
                    type="button"
                    onClick={() => void startDm(candidate.id)}
                    disabled={starting}
                    className="flex w-full items-center gap-2 rounded-md px-2 py-1.5 text-left text-sm text-foreground transition-colors duration-fast hover:bg-primary-subtle disabled:opacity-50"
                  >
                    <UserAvatar
                      nickname={candidate.nickname}
                      avatarUrl={candidate.avatarUrl}
                      userId={candidate.id}
                      className="h-6 w-6 text-[11px]"
                    />
                    {candidate.nickname}
                  </button>
                </li>
              ))}
              {visibleCandidates.length === 0 && !pickerError && (
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
