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
 */
import { useCallback, useEffect, useRef, useState } from "react";
import { useParams } from "next/navigation";
import { AttachmentControls } from "@/components/attachments/AttachmentControls";
import { UserAvatar } from "@/components/profile/UserAvatar";
import { useAuth } from "@/contexts/AuthContext";
import { useDmRooms } from "@/contexts/DmRoomsContext";
import { invitationErrorMessage } from "@/lib/invitation";
import { directMessageService } from "@/services/direct-message.service";
import type { DirectMessageResponse, DirectMessageRoomResponse } from "@/types/direct-message.types";

type LoadState = "loading" | "ready" | "error";

function formatTime(createdAt: string) {
  return new Date(createdAt).toLocaleTimeString("ko-KR", { hour: "2-digit", minute: "2-digit" });
}

export default function DmRoomPage() {
  const params = useParams<{ companyId: string; roomId: string }>();
  const { user, companyMemberships } = useAuth();
  const { markRoomRead } = useDmRooms();

  const [pageState, setPageState] = useState<LoadState>("loading");
  const [pageError, setPageError] = useState<string | null>(null);
  const [room, setRoom] = useState<DirectMessageRoomResponse | null>(null);
  const [messages, setMessages] = useState<DirectMessageResponse[]>([]);

  const [draft, setDraft] = useState("");
  const [sending, setSending] = useState(false);
  const [sendError, setSendError] = useState<string | null>(null);
  const scrollRef = useRef<HTMLDivElement>(null);

  const isCompanyAdmin =
    companyMemberships.find((membership) => membership.companyId === Number(params.companyId))?.role ===
    "COMPANY_ADMIN";

  /*
   * 재리뷰 반영(PR #173, pktewon):
   *   - room 목록(`GET /companies/{companyId}/direct-messages/rooms`)에서 이
   *     roomId를 찾지 못하면(다른 회사의 방이거나 참여자가 아닌 방) 상대 이름을
   *     "알 수 없는 사용자"로 대충 보여주며 그대로 진행했다 — messages/markRead는
   *     roomId만으로 호출되고 companyId를 검증하지 않아, URL을 바꿔치기하면 다른
   *     회사 DM방의 메시지를 그대로 읽고 markRead까지 호출할 수 있었다. 이제
   *     현재 companyId의 room 목록에 없는 roomId는 에러로 처리하고 메시지 조회
   *     결과를 렌더링하지 않는다.
   *   - 페이지를 벗어난 뒤(unmount) 뒤늦게 도착한 응답으로 markRead/setState가
   *     실행되지 않도록 cancelled 가드를 추가한다.
   *
   * 4차 재리뷰 반영(pktewon, blocker 1): "room 목록에 있는지"만으로는 부족했다
   * — room.companyId를 직접 비교하는 invariant가 없으면, listRooms가 어떤
   * 이유로든(백엔드 스코핑 우회, 캐시 등) 다른 회사 room을 섞어 돌려주는
   * 경우까지는 못 막는다. room.companyId === routeCompanyId를 직접 검증하고,
   * 그 검증이 끝난 뒤에만 messages API를 호출한다(Promise.all 병렬 호출 금지
   * — 검증 전에 이미 다른 회사 메시지를 받아와 버리면 검증 자체가 무의미하다).
   */
  const load = useCallback(
    async (isCancelled: () => boolean) => {
      try {
        const roomsResponse = await directMessageService.listRooms(params.companyId);
        if (isCancelled()) return;

        const loadedRoom = roomsResponse.data.data.rooms.find((r) => String(r.id) === params.roomId);
        if (!loadedRoom || loadedRoom.companyId !== Number(params.companyId)) {
          setPageError("존재하지 않거나 접근할 수 없는 대화방입니다.");
          setPageState("error");
          return;
        }

        const messagesResponse = await directMessageService.listMessages(params.roomId);
        if (isCancelled()) return;

        setPageError(null);
        setRoom(loadedRoom);
        const list = messagesResponse.data.data.messages;
        setMessages(list);
        setPageState("ready");

        const latest = list[list.length - 1];
        if (latest) {
          try {
            const { data } = await directMessageService.markRead(params.roomId, {
              lastReadDirectMessageId: latest.id,
            });
            if (isCancelled()) return;
            markRoomRead(Number(params.roomId), data.data.unreadCount);
          } catch {
            // unread badge 갱신 실패는 대화 화면 기능에 영향 없다. 레일 배지도
            // 실제로 안 읽힌 상태를 유지해야 하므로 markRoomRead를 호출하지 않는다.
          }
        }
      } catch (error) {
        if (isCancelled()) return;
        setPageError(invitationErrorMessage(error, "대화를 불러오지 못했습니다."));
        setPageState("error");
      }
    },
    [params.companyId, params.roomId, markRoomRead]
  );

  useEffect(() => {
    let cancelled = false;
    (async () => {
      await load(() => cancelled);
    })();
    return () => {
      cancelled = true;
    };
  }, [load]);

  useEffect(() => {
    if (pageState === "ready") scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight });
  }, [messages.length, pageState]);

  const send = async () => {
    const content = draft.trim();
    if (!content || sending) return;
    setSendError(null);
    setSending(true);
    try {
      const { data } = await directMessageService.send(params.roomId, { content });
      setMessages((prev) => [...prev, data.data.message]);
      setDraft("");
    } catch (error) {
      setSendError(invitationErrorMessage(error, "메시지 전송에 실패했습니다."));
    } finally {
      setSending(false);
    }
  };

  if (pageState === "loading") {
    return <div className="px-6 py-8 text-sm text-muted">대화를 불러오는 중...</div>;
  }

  if (pageState === "error") {
    return (
      <div className="px-6 py-8">
        <div className="rounded-lg border border-destructive/30 bg-destructive-subtle px-4 py-6 text-center text-sm text-destructive">
          {pageError}
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
        {sendError && <p className="mb-2 text-xs text-destructive">{sendError}</p>}
        <div className="flex items-center gap-2 rounded-xl bg-surface px-3 py-2 shadow-sm">
          <input
            aria-label="메시지 입력"
            value={draft}
            onChange={(e) => setDraft(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && !e.nativeEvent.isComposing && void send()}
            disabled={sending}
            placeholder={`${peer.nickname}에게 메시지 보내기`}
            className="flex-1 bg-transparent text-sm text-foreground placeholder:text-placeholder focus:outline-none disabled:opacity-50"
          />
          <button
            onClick={() => void send()}
            disabled={sending || !draft.trim()}
            className="shrink-0 rounded-lg btn-gradient px-3 py-1.5 text-xs font-semibold text-primary-foreground transition-colors duration-fast disabled:cursor-not-allowed disabled:opacity-50"
          >
            {sending ? "전송 중..." : "전송"}
          </button>
        </div>
      </div>
    </div>
  );
}
