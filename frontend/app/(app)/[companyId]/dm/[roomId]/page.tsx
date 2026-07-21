"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { useParams } from "next/navigation";
import { AttachmentControls } from "@/components/attachments/AttachmentControls";
import { UserAvatar } from "@/components/profile/UserAvatar";
import { useAuth } from "@/contexts/AuthContext";
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
  const [pageState, setPageState] = useState<LoadState>("loading");
  const [pageError, setPageError] = useState<string | null>(null);
  const [room, setRoom] = useState<DirectMessageRoomResponse | null>(null);
  const [messages, setMessages] = useState<DirectMessageResponse[]>([]);
  const [draft, setDraft] = useState("");
  const [sending, setSending] = useState(false);
  const [sendError, setSendError] = useState<string | null>(null);
  const scrollRef = useRef<HTMLDivElement>(null);
  const roomId = Number(params.roomId);
  const isValidRoomId = Number.isInteger(roomId) && roomId > 0;
  const isCompanyAdmin = companyMemberships.find((membership) => membership.companyId === Number(params.companyId))?.role === "COMPANY_ADMIN";

  const load = useCallback(async () => {
    try {
      const [roomsResponse, messagesResponse] = await Promise.all([
        directMessageService.listRooms(params.companyId),
        directMessageService.listMessages(roomId),
      ]);
      const loadedRoom = roomsResponse.data.data.rooms.find((item) => item.id === roomId) ?? null;
      if (!loadedRoom) {
        setPageError("대화방을 찾을 수 없습니다.");
        setPageState("error");
        return;
      }
      setRoom(loadedRoom);
      setMessages(messagesResponse.data.data.messages);
      setPageError(null);
      setPageState("ready");

      const latestMessage = messagesResponse.data.data.messages.at(-1);
      if (latestMessage) {
        try {
          await directMessageService.markRead(roomId, { lastReadDirectMessageId: latestMessage.id });
        } catch {
          // 읽음 상태 갱신 실패는 대화 조회/전송을 막지 않는다.
        }
      }
    } catch (loadError) {
      setPageError(invitationErrorMessage(loadError, "대화를 불러오지 못했습니다."));
      setPageState("error");
    }
  }, [params.companyId, roomId]);

  useEffect(() => {
    if (!isValidRoomId) return;
    void (async () => {
      await load();
    })();
  }, [isValidRoomId, load]);

  useEffect(() => {
    if (pageState === "ready") scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight });
  }, [messages.length, pageState]);

  const send = async () => {
    const content = draft.trim();
    if (!content || sending || !isValidRoomId) return;
    setSending(true);
    setSendError(null);
    try {
      const { data } = await directMessageService.createMessage(roomId, { content });
      setMessages((current) => [...current, data.data.message]);
      setDraft("");
    } catch (sendRequestError) {
      setSendError(invitationErrorMessage(sendRequestError, "메시지 전송에 실패했습니다."));
    } finally {
      setSending(false);
    }
  };

  if (!isValidRoomId) return <div className="px-6 py-8 text-sm text-destructive">유효하지 않은 대화방입니다.</div>;
  if (pageState === "loading") return <div className="px-6 py-8 text-sm text-muted">대화를 불러오는 중...</div>;
  if (pageState === "error") {
    return <div className="px-6 py-8"><div className="rounded-lg border border-destructive/30 bg-destructive-subtle px-4 py-6 text-center text-sm text-destructive">{pageError}</div></div>;
  }

  return (
    <div className="flex h-full flex-col">
      <div className="flex h-14 shrink-0 items-center gap-2 border-b border-border px-5">
        <UserAvatar nickname={room!.otherUser.nickname} avatarUrl={room!.otherUser.avatarUrl} userId={room!.otherUser.id} className="h-7 w-7 text-xs text-foreground" />
        <h1 className="text-sm font-bold text-foreground">{room!.otherUser.nickname}</h1>
      </div>

      <div ref={scrollRef} className="flex-1 overflow-y-auto px-5 py-4">
        <div className="flex flex-col">
          {messages.map((message, index) => {
            const previous = messages[index - 1];
            const showHeader = !previous || previous.sender.id !== message.sender.id;
            const canMutate = message.sender.id === user?.id || isCompanyAdmin;
            return (
              <div key={message.id} className={"group flex gap-3 rounded-md px-2 py-0.5 hover:bg-border/20 " + (showHeader ? "mt-3" : "")}>
                <div className="w-9 shrink-0 text-right">
                  {showHeader ? (
                    <UserAvatar nickname={message.sender.nickname} avatarUrl={message.sender.avatarUrl} userId={message.sender.id} className="h-9 w-9 text-xs text-foreground" />
                  ) : (
                    <span className="hidden text-[10px] text-placeholder group-hover:inline-block">{formatTime(message.createdAt)}</span>
                  )}
                </div>
                <div className="min-w-0 flex-1">
                  {showHeader && <div className="flex items-baseline gap-2"><span className="text-sm font-semibold text-foreground">{message.sender.nickname}</span><span className="text-[10px] text-placeholder">{formatTime(message.createdAt)}</span></div>}
                  <p className="whitespace-pre-wrap text-sm text-foreground">{message.content}</p>
                  <AttachmentControls targetType="DIRECT_MESSAGE" targetId={message.id} canMutate={canMutate} compact />
                </div>
              </div>
            );
          })}
          {messages.length === 0 && <p className="py-10 text-center text-sm text-muted">아직 메시지가 없습니다.</p>}
        </div>
      </div>

      <div className="px-5 pb-5">
        {sendError && <p className="mb-2 text-xs text-destructive">{sendError}</p>}
        <div className="flex items-center gap-2 rounded-xl bg-surface px-3 py-2 shadow-sm">
          <input aria-label="메시지 입력" value={draft} onChange={(event) => setDraft(event.target.value)} onKeyDown={(event) => event.key === "Enter" && void send()} disabled={sending} placeholder={`${room!.otherUser.nickname}에게 메시지 보내기`} className="flex-1 bg-transparent text-sm text-foreground placeholder:text-placeholder focus:outline-none disabled:opacity-50" />
          <button onClick={() => void send()} disabled={sending || !draft.trim()} className="shrink-0 rounded-lg btn-gradient px-3 py-1.5 text-xs font-semibold text-primary-foreground transition-colors duration-fast disabled:cursor-not-allowed disabled:opacity-50">{sending ? "전송 중..." : "전송"}</button>
        </div>
      </div>
    </div>
  );
}
