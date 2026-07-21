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
 * 기반 프로필 이미지 지원)로 바꿨다.
 *
 * React Query 마이그레이션(이슈 #217, PR #173 4차 재리뷰 사후): 방 목록/후보
 * 조회는 useDmQueries.ts의 useDmRoomsQuery/useDmCandidatesQuery/
 * useStartDmMutation을 쓴다. 예전에 여기 있던 손짠 동시성 가드(sequenceRef,
 * latestLoadSequenceRef, readGenerationsRef, pickerRequestSequenceRef,
 * mountedRef)는 React Query가 대신 처리해서 다 지웠다 — 남은 건 pathnameRef
 * 하나뿐이다(아래 startDm 주석 참고, router.push는 React Query 영역 밖).
 * dm/[roomId]/page.tsx와 방 목록을 공유하던 DmRoomsContext도 같은 이유로
 * 없앴다 — 둘 다 같은 queryKey(dmRoomsQueryKey)를 쓰면 React Query가 알아서
 * 캐시를 공유한다.
 */
import { useEffect, useMemo, useRef, useState } from "react";
import Link from "next/link";
import { usePathname, useParams, useRouter } from "next/navigation";
import { UserAvatar } from "@/components/profile/UserAvatar";
import { useDmRoomsQuery, useDmCandidatesQuery, useStartDmMutation } from "@/hooks/useDmQueries";

export default function DmLayout({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const { companyId } = useParams<{ companyId: string }>();

  const roomsQuery = useDmRoomsQuery(companyId);
  const rooms = useMemo(() => roomsQuery.data ?? [], [roomsQuery.data]);

  const [pickerOpen, setPickerOpen] = useState(false);
  const [pickerError, setPickerError] = useState<string | null>(null);
  const candidatesQuery = useDmCandidatesQuery(companyId, pickerOpen);

  const roomPeerIds = useMemo(() => new Set(rooms.map((r) => r.otherUser.id)), [rooms]);
  const visibleCandidates = useMemo(
    () => (candidatesQuery.data ?? []).filter((candidate) => !roomPeerIds.has(candidate.id)),
    [candidatesQuery.data, roomPeerIds]
  );

  const startDmMutation = useStartDmMutation(companyId);

  // startDm 응답이 늦게 도착했을 때 "그 요청을 시작한 시점과 같은 화면인지"를
  // 판단하는 데 쓴다(4차 재리뷰 blocker 4) — React Query는 캐시 최신성은
  // 대신 챙겨주지만 router.push(화면 전환)는 캐시가 아니라서 직접 확인해야
  // 한다. pathname은 리렌더 시점의 클로저 값이라 응답 콜백 안에서 "지금" 값을
  // 읽으려면 ref로 항상 최신화해둬야 한다.
  const pathnameRef = useRef(pathname);
  useEffect(() => {
    pathnameRef.current = pathname;
  }, [pathname]);

  const openPicker = () => {
    setPickerError(null);
    setPickerOpen((open) => !open);
  };

  const startDm = async (otherUserId: number) => {
    if (startDmMutation.isPending) return;
    setPickerError(null);
    const startPathname = pathname;
    try {
      const response = await startDmMutation.mutateAsync(otherUserId);
      setPickerOpen(false);
      if (pathnameRef.current !== startPathname) return;
      router.push(`/${companyId}/dm/${response.data.data.room.id}`);
    } catch {
      setPickerError("대화를 시작하지 못했습니다.");
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
          onClick={openPicker}
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
            {(pickerError || candidatesQuery.isError) && (
              <p className="px-2 py-1 text-xs text-destructive">
                {pickerError ?? "구성원 목록을 불러오지 못했습니다."}
              </p>
            )}
            {candidatesQuery.isLoading ? (
              <p className="px-2 py-1.5 text-xs text-muted">불러오는 중...</p>
            ) : (
              <ul className="max-h-64 overflow-y-auto">
                {visibleCandidates.map((candidate) => (
                  <li key={candidate.id}>
                    <button
                      type="button"
                      onClick={() => void startDm(candidate.id)}
                      disabled={startDmMutation.isPending}
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
                {visibleCandidates.length === 0 && !pickerError && !candidatesQuery.isError && (
                  <li className="px-2 py-1.5 text-xs text-muted">대화를 시작할 구성원이 없습니다.</li>
                )}
              </ul>
            )}
          </div>
        )}
      </div>
      <div className="min-w-0 flex-1">{children}</div>
    </div>
  );
}
