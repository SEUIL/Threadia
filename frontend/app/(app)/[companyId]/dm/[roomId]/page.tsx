"use client";

/*
 * S09 — 1:1 DM (채팅방)
 *
 * 접근 사용자: A/P/C. 화면정의서(docs/03-design/화면정의서.md §S09) 기준
 * 회사 구성원과의 1:1 DM 채팅. WBS 4.7.3.2 — 4.7.3.1 Backend(PR #104)의 실
 * API로 연동했다.
 *
 * 설계 메모 반영:
 *   - DM 메시지는 이슈 생성 불가 — 프로젝트 채팅과 달리 "이슈로 만들기"
 *     액션이 없다(원래도 없었다).
 *   - 메시지는 삭제 불가.
 *   - 다자간 DM, read receipt는 MVP 제외 — unread count만 다룬다(방 진입 시
 *     마지막 메시지까지 읽음 처리).
 *
 * DM 메시지 목록(`GET /direct-messages/rooms/{roomId}/messages`)은 페이지네이션이
 * 없다(§8.1) — 생성 시각/ID 오름차순 전체를 한 번에 받는다. 상대 정보는 방
 * 목록(`GET /companies/{companyId}/direct-messages/rooms`)에서 찾는다 — 단건
 * 조회 endpoint가 없다.
 *
 * WBS 4.11(PR #215) 반영: 메시지별 첨부 파일(AttachmentControls, DIRECT_MESSAGE
 * target)과 UserAvatar(attachment 기반 프로필 이미지)를 붙였다. 첨부 삭제는
 * 작성자 본인 또는 COMPANY_ADMIN만(canMutate) — 메시지 자체 삭제는 여전히
 * 지원하지 않는다(위 설계 메모).
 *
 * React Query 마이그레이션(이슈 #217, PR #173 4차 재리뷰 사후):
 *   - room 목록은 dm/layout.tsx와 같은 useDmRoomsQuery(같은 queryKey)를 써서
 *     캐시를 공유한다 — 이전엔 DmRoomsContext로 layout이 가진 rooms를
 *     내려받았는데, 이제 이 화면이 직접 같은 쿼리를 구독하면 된다.
 *   - blocker 1(room.companyId 검증 전 messages를 먼저 받아오면 안 됨)은
 *     useDmMessagesQuery의 enabled를 "room 검증 통과 여부"로 걸어서 해결한다
 *     — Promise.all 병렬 호출을 순차 호출로 바꿨던 예전 방식보다 선언적이다.
 *   - mark-read는 useMarkDmRoomReadMutation을 직접 호출한다. 성공하면 그
 *     mutation이 dm-rooms 캐시를 patch해서 rail 배지가 그냥 갱신된다 —
 *     markRoomRead()를 Context로 끌어올려 부르던 방식이 필요 없어졌다.
 */
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useParams } from "next/navigation";
import { AttachmentControls } from "@/components/attachments/AttachmentControls";
import { UserAvatar } from "@/components/profile/UserAvatar";
import { useAuth } from "@/contexts/AuthContext";
import { useDmRoomsQuery, useDmMessagesQuery, useMarkDmRoomReadMutation, useSendDmMessageMutation } from "@/hooks/useDmQueries";

function formatTime(createdAt: string) {
  return new Date(createdAt).toLocaleTimeString("ko-KR", { hour: "2-digit", minute: "2-digit" });
}

export default function DmRoomPage() {
  const params = useParams<{ companyId: string; roomId: string }>();
  const { user, companyMemberships } = useAuth();
  const scrollRef = useRef<HTMLDivElement>(null);

  const roomId = Number(params.roomId);
  const isValidRoomId = Number.isInteger(roomId) && roomId > 0;

  const roomsQuery = useDmRoomsQuery(params.companyId);
  const room = useMemo(
    () => roomsQuery.data?.find((r) => r.id === roomId) ?? null,
    [roomsQuery.data, roomId]
  );
  // 4차 재리뷰 blocker 1: room이 목록에 "있어도" companyId가 다르면(백엔드
  // 스코핑을 우회하는 응답이 와도 대비) 접근 불가로 처리한다. 이 판정이
  // 끝나기 전에는 messages 쿼리를 아예 안 보낸다(enabled).
  const roomValid = Boolean(room && room.companyId === Number(params.companyId));

  const messagesQuery = useDmMessagesQuery(roomId, isValidRoomId && roomsQuery.isSuccess && roomValid);
  const messages = messagesQuery.data ?? [];

  const markReadMutation = useMarkDmRoomReadMutation(params.companyId);
  const sendMutation = useSendDmMessageMutation(roomId);

  const [draft, setDraft] = useState("");

  const isCompanyAdmin =
    companyMemberships.find((membership) => membership.companyId === Number(params.companyId))?.role ===
    "COMPANY_ADMIN";

  // 이 room의 이 메시지까지는 이미 mark-read를 보냈다 — 같은 메시지로
  // 중복 호출하지 않는다(messagesQuery가 refetch될 때마다 다시 부르면 안 됨).
  const lastMarkedMessageIdRef = useRef<number | null>(null);
  useEffect(() => {
    lastMarkedMessageIdRef.current = null;
  }, [roomId]);

  useEffect(() => {
    if (!messagesQuery.isSuccess) return;
    const latest = messages.at(-1);
    if (!latest || lastMarkedMessageIdRef.current === latest.id) return;
    lastMarkedMessageIdRef.current = latest.id;
    markReadMutation.mutate({ roomId, lastReadDirectMessageId: latest.id });
    // eslint-disable-next-line react-hooks/exhaustive-deps -- markReadMutation은 매 렌더 새 객체라 deps에 넣으면 무한 재실행된다.
  }, [messagesQuery.isSuccess, messages, roomId]);

  useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight });
  }, [messages.length]);

  const send = useCallback(async () => {
    const content = draft.trim();
    if (!content || sendMutation.isPending) return;
    try {
      await sendMutation.mutateAsync(content);
      setDraft("");
    } catch {
      // 에러는 sendMutation.error로 아래에서 그대로 보여준다.
    }
  }, [draft, sendMutation]);

  if (!isValidRoomId) {
    return <div className="px-6 py-8 text-sm text-destructive">유효하지 않은 대화방입니다.</div>;
  }

  if (roomsQuery.isLoading || (roomValid && messagesQuery.isLoading)) {
    return <div className="px-6 py-8 text-sm text-muted">대화를 불러오는 중...</div>;
  }

  if (roomsQuery.isError || !roomValid || messagesQuery.isError) {
    const message = roomsQuery.isError || messagesQuery.isError
      ? "대화를 불러오지 못했습니다."
      : "존재하지 않거나 접근할 수 없는 대화방입니다.";
    return (
      <div className="px-6 py-8">
        <div className="rounded-lg border border-destructive/30 bg-destructive-subtle px-4 py-6 text-center text-sm text-destructive">
          {message}
        </div>
      </div>
    );
  }

  const peer = room!.otherUser;

  return (
    <div className="flex h-full flex-col">
      <div className="flex h-14 shrink-0 items-center gap-2 border-b border-border px-5">
        <UserAvatar nickname={peer.nickname} avatarUrl={peer.avatarUrl} userId={peer.id} className="h-7 w-7 text-xs text-foreground" />
        <h1 className="text-sm font-bold text-foreground">{peer.nickname}</h1>
      </div>

      <div ref={scrollRef} className="flex-1 overflow-y-auto px-5 py-4">
        <div className="flex flex-col">
          {messages.map((message, index) => {
            const previous = messages[index - 1];
            const showHeader = !previous || previous.sender.id !== message.sender.id;
            const canMutate = message.sender.id === user?.id || isCompanyAdmin;
            const time = formatTime(message.createdAt);
            return (
              <div
                key={message.id}
                className={"group flex gap-3 rounded-md px-2 py-0.5 hover:bg-border/20 " + (showHeader ? "mt-3" : "")}
              >
                <div className="w-9 shrink-0 text-right">
                  {showHeader ? (
                    <UserAvatar
                      nickname={message.sender.nickname}
                      avatarUrl={message.sender.avatarUrl}
                      userId={message.sender.id}
                      className="h-9 w-9 text-xs text-foreground"
                    />
                  ) : (
                    <span className="hidden text-[10px] text-placeholder group-hover:inline-block">{time}</span>
                  )}
                </div>
                <div className="min-w-0 flex-1">
                  {showHeader && (
                    <div className="flex items-baseline gap-2">
                      <span className="text-sm font-semibold text-foreground">
                        {message.sender.id === user?.id ? "나" : message.sender.nickname}
                      </span>
                      <span className="text-[10px] text-placeholder">{time}</span>
                    </div>
                  )}
                  <p className="whitespace-pre-wrap text-sm text-foreground">{message.content}</p>
                  <AttachmentControls targetType="DIRECT_MESSAGE" targetId={message.id} canMutate={canMutate} compact />
                </div>
              </div>
            );
          })}
          {messages.length === 0 && (
            <p className="py-10 text-center text-sm text-muted">아직 메시지가 없습니다.</p>
          )}
        </div>
      </div>

      <div className="px-5 pb-5">
        {sendMutation.isError && <p className="mb-2 text-xs text-destructive">메시지 전송에 실패했습니다.</p>}
        <div className="flex items-center gap-2 rounded-xl bg-surface px-3 py-2 shadow-sm">
          <input
            aria-label="메시지 입력"
            value={draft}
            onChange={(e) => setDraft(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && !e.nativeEvent.isComposing && void send()}
            disabled={sendMutation.isPending}
            placeholder={`${peer.nickname}에게 메시지 보내기`}
            className="flex-1 bg-transparent text-sm text-foreground placeholder:text-placeholder focus:outline-none disabled:opacity-50"
          />
          <button
            onClick={() => void send()}
            disabled={sendMutation.isPending || !draft.trim()}
            className="shrink-0 rounded-lg btn-gradient px-3 py-1.5 text-xs font-semibold text-primary-foreground transition-colors duration-fast disabled:cursor-not-allowed disabled:opacity-50"
          >
            {sendMutation.isPending ? "전송 중..." : "전송"}
          </button>
        </div>
      </div>
    </div>
  );
}
