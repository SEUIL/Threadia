"use client";

/*
 * S09 — 프로젝트 채팅
 *
 * 접근 사용자: A/P/C. 화면정의서(docs/03-design/화면정의서.md §S09) 기준
 * 프로젝트 단위 채팅. WBS 4.7.2.2 — 4.7.2.1 Backend(PR #103)의 실 API로
 * 연동했다.
 *
 * 레이아웃은 디스코드 참조 스크린샷 기준(2026-07-06)을 유지한다: 채널 목록(좌)
 * + 메시지 목록(중앙) + 멤버 목록(우) 3단 구성, 말풍선 대신 아바타+이름+시각
 * 헤더 뒤에 본문이 이어지는 flat 메시지 스타일, 연속된 같은 작성자 메시지는
 * 헤더를 생략한다. Threadia는 아직 프로젝트당 채팅 채널이 1개뿐이라 좌측
 * 채널 목록은 "# 프로젝트 채팅" 단일 항목만 있는 고정 목록이다.
 *
	 * 이번 slice에 반영하지 않은 것:
	 *   - "메시지 → 이슈 생성"(design memo 액션) — Backend endpoint는 WBS 4.5.1.1에서
	 *     추가됐고, Frontend 액션은 WBS 4.5.1.2에서 별도 연동한다.
 *   - "쓰레드 보기" — Thread Frontend(WBS 4.7.1.2)가 별도 PR이라 이번 slice는
 *     메시지 작성 시 threadId를 항상 null로 보낸다.
 *   - 멤버 온라인/오프라인 — 실 presence API가 없어 ACTIVE 멤버를 하나의
 *     목록으로만 보여준다(이전 mock의 상수 고정 온라인/오프라인 구분 제거).
 *
 * 메시지 목록은 서버가 최신순(createdAt DESC, id DESC) keyset으로 주므로
 * 화면에는 reverse해서 오래된 메시지가 위로 가게 표시한다. "이전 메시지
 * 더 보기"는 nextCursor로 다음 page를 받아 위쪽에 이어 붙인다.
 *
 * mark-read 응답의 unreadCount를 채널 헤더 배지로 반영하고(PR #172 리뷰 반영),
 * "이전 메시지 더 보기" 실패는 초기 로드 실패(pageError)와 별도 상태
 * (loadMoreError)로 분리해서 보여준다 — 초기 목록은 이미 성공했는데 pagination
 * 실패를 pageError에 합치면 pageState==="ready" 렌더 분기에서는 안 보인다.
 */
import { useCallback, useEffect, useRef, useState } from "react";
import { useParams } from "next/navigation";
import { useAuth } from "@/contexts/AuthContext";
import { invitationErrorMessage } from "@/lib/invitation";
import { messageService } from "@/services/message.service";
import { projectMemberService } from "@/services/project-member.service";
import type { MessageResponse } from "@/types/message.types";
import type { ProjectMemberResponse } from "@/types/project-member.types";

type LoadState = "loading" | "ready" | "error";

function initials(name: string) {
  return name.slice(0, 1);
}

export default function ProjectMessagesPage() {
  const params = useParams<{ companyId: string; projectId: string }>();
  const { user } = useAuth();

  const [pageState, setPageState] = useState<LoadState>("loading");
  const [pageError, setPageError] = useState<string | null>(null);
  const [messages, setMessages] = useState<MessageResponse[]>([]);
  const [nextCursor, setNextCursor] = useState<string | null>(null);
  const [members, setMembers] = useState<ProjectMemberResponse[]>([]);
  const [loadingMore, setLoadingMore] = useState(false);
  const [loadMoreError, setLoadMoreError] = useState<string | null>(null);
  const [unreadCount, setUnreadCount] = useState(0);

  const [draft, setDraft] = useState("");
  const [sending, setSending] = useState(false);
  const [sendError, setSendError] = useState<string | null>(null);

  const scrollRef = useRef<HTMLDivElement>(null);

  const nicknameOf = (authorUserId: number) => {
    if (authorUserId === user?.id) return user.nickname;
    return members.find((m) => m.user.id === authorUserId)?.user.nickname ?? `사용자 #${authorUserId}`;
  };

  const load = useCallback(async () => {
    try {
      const [messagesResponse, membersResponse] = await Promise.all([
        messageService.list(params.projectId),
        projectMemberService.list(params.projectId, { status: "ACTIVE" }),
      ]);
      setPageError(null);
      // 서버는 최신순(DESC)이라 화면 표시 순서(오래된 → 최신)로 뒤집는다.
      const ordered = [...messagesResponse.data.data.messages].reverse();
      setMessages(ordered);
      setNextCursor(messagesResponse.data.data.nextCursor);
      setMembers(membersResponse.data.data.projectMembers);
      setPageState("ready");

      // 최신 메시지까지 읽음 처리 — 실패해도 채팅 자체는 계속 보여준다.
      // 응답의 unreadCount를 채널 헤더 배지에 반영한다(서버 계약으로 검증 가능하게).
      const latest = ordered[ordered.length - 1];
      if (latest) {
        try {
          const { data } = await messageService.markRead(params.projectId, {
            lastReadMessageId: latest.id,
          });
          setUnreadCount(data.data.unreadCount);
        } catch {
          // unread badge 갱신 실패는 채팅 화면 기능에 영향 없다.
        }
      }
    } catch (error) {
      setPageError(invitationErrorMessage(error, "채팅을 불러오지 못했습니다."));
      setPageState("error");
    }
  }, [params.projectId]);

  useEffect(() => {
    (async () => {
      await load();
    })();
  }, [load]);

  // 최초 로드/전송 후 맨 아래로 스크롤.
  useEffect(() => {
    if (pageState === "ready") scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight });
  }, [pageState, messages.length]);

  const loadOlder = async () => {
    if (!nextCursor || loadingMore) return;
    setLoadingMore(true);
    setLoadMoreError(null);
    try {
      const { data } = await messageService.list(params.projectId, { cursor: nextCursor });
      setMessages((prev) => [...[...data.data.messages].reverse(), ...prev]);
      setNextCursor(data.data.nextCursor);
    } catch (error) {
      setLoadMoreError(invitationErrorMessage(error, "이전 메시지를 불러오지 못했습니다."));
    } finally {
      setLoadingMore(false);
    }
  };

  const send = async () => {
    const content = draft.trim();
    if (!content || sending) return;
    setSendError(null);
    setSending(true);
    try {
      const { data } = await messageService.create(params.projectId, { content });
      setMessages((prev) => [...prev, data.data.message]);
      setDraft("");
    } catch (error) {
      setSendError(invitationErrorMessage(error, "메시지 전송에 실패했습니다."));
    } finally {
      setSending(false);
    }
  };

  if (pageState === "loading") {
    return <div className="px-6 py-8 text-sm text-muted">채팅을 불러오는 중...</div>;
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

  return (
    <div className="flex h-full">
      {/* 채널 목록 — 지금은 단일 채널뿐이라 항목 1개짜리 고정 목록 */}
      <aside className="hidden w-56 shrink-0 flex-col border-r border-border bg-surface md:flex">
        <div className="border-b border-border px-4 py-3">
          <p className="text-sm font-bold text-foreground">프로젝트 {params.projectId}</p>
        </div>
        <div className="flex-1 overflow-y-auto p-2">
          <p className="px-2 pb-1 text-[11px] font-semibold uppercase tracking-wide text-muted">
            채팅 채널
          </p>
          <div className="flex items-center gap-1.5 rounded-md bg-primary-subtle px-2 py-1.5 text-sm font-medium text-primary">
            <span className="text-muted">#</span>
            프로젝트 채팅
          </div>
        </div>
        {user && (
          <div className="flex items-center gap-2 border-t border-border px-3 py-2.5">
            <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full avatar-ring text-xs font-semibold text-foreground">
              {initials(user.nickname)}
            </span>
            <span className="min-w-0 flex-1 truncate text-sm font-medium text-foreground">
              {user.nickname}
            </span>
          </div>
        )}
      </aside>

      {/* 메시지 영역 */}
      <div className="flex min-w-0 flex-1 flex-col">
        <div className="flex h-14 shrink-0 items-center border-b border-border px-5">
          <span className="mr-1.5 text-muted">#</span>
          <h1 className="text-sm font-bold text-foreground">프로젝트 채팅</h1>
          {unreadCount > 0 && (
            <span className="ml-2 rounded-full bg-destructive px-1.5 py-0.5 text-[10px] font-semibold leading-none text-on-primary">
              {unreadCount > 9 ? "9+" : unreadCount}
            </span>
          )}
        </div>

        <div ref={scrollRef} className="flex-1 overflow-y-auto px-5 py-4">
          {nextCursor && (
            <div className="mb-3 text-center">
              <button
                onClick={() => void loadOlder()}
                disabled={loadingMore}
                className="text-xs font-medium text-primary hover:opacity-70 disabled:opacity-50"
              >
                {loadingMore ? "불러오는 중..." : "이전 메시지 더 보기"}
              </button>
              {loadMoreError && <p className="mt-1 text-xs text-destructive">{loadMoreError}</p>}
            </div>
          )}
          <div className="flex flex-col">
            {messages.map((msg, i) => {
              const prev = messages[i - 1];
              const showHeader = !prev || prev.authorUserId !== msg.authorUserId;
              const name = nicknameOf(msg.authorUserId);
              const time = new Date(msg.createdAt).toLocaleTimeString("ko-KR", {
                hour: "2-digit",
                minute: "2-digit",
              });
              return (
                <div
                  key={msg.id}
                  className={"flex gap-3 rounded-md px-2 py-0.5 hover:bg-border/20 " + (showHeader ? "mt-3" : "")}
                >
                  <div className="w-9 shrink-0 text-right">
                    {showHeader ? (
                      <span className="flex h-9 w-9 items-center justify-center rounded-full avatar-ring text-xs font-semibold text-foreground">
                        {initials(name)}
                      </span>
                    ) : (
                      <span className="hidden text-[10px] text-placeholder group-hover:inline-block">
                        {time}
                      </span>
                    )}
                  </div>
                  <div className="min-w-0 flex-1">
                    {showHeader && (
                      <div className="flex items-baseline gap-2">
                        <span className="text-sm font-semibold text-foreground">{name}</span>
                        <span className="text-[10px] text-placeholder">{time}</span>
                      </div>
                    )}
                    <p className="whitespace-pre-wrap text-sm text-foreground">{msg.content}</p>
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
              onKeyDown={(e) => e.key === "Enter" && void send()}
              disabled={sending}
              placeholder="#프로젝트 채팅에 메시지 보내기"
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

      {/* 멤버 목록 — 실 presence API가 없어 ACTIVE 멤버를 하나의 목록으로 보여준다 */}
      <aside className="hidden w-52 shrink-0 flex-col gap-4 overflow-y-auto border-l border-border bg-surface px-3 py-4 lg:flex">
        <div>
          <p className="px-1 pb-1.5 text-[11px] font-semibold uppercase tracking-wide text-muted">
            구성원 — {members.length}
          </p>
          {members.map((member) => (
            <div
              key={member.id}
              className="flex items-center gap-2 rounded-md px-1 py-1.5 hover:bg-border/20"
            >
              <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full avatar-ring text-[11px] font-semibold text-foreground">
                {initials(member.user.nickname)}
              </span>
              <span className="truncate text-sm text-foreground">
                {member.user.nickname}
                {member.user.id === user?.id && <span className="ml-1 text-xs text-muted">(나)</span>}
              </span>
            </div>
          ))}
          {members.length === 0 && <p className="px-1 text-xs text-muted">구성원이 없습니다.</p>}
        </div>
      </aside>
    </div>
  );
}
