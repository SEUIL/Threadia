"use client";

/*
 * S09 — 1:1 DM (채팅방)
 *
 * 접근 사용자: A/P/C. 화면정의서(docs/03-design/화면정의서.md §S09) 기준
 * 회사 구성원과의 1:1 DM 채팅. 설계 메모 반영:
 *   - DM 메시지는 이슈 생성 불가(`DM_MESSAGE_CANNOT_CREATE_ISSUE`) — 프로젝트
 *     채팅(`[projectId]/messages`)과 달리 "이슈로 만들기" 액션이 없다.
 *   - 메시지는 삭제 불가.
 *   - 다자간 DM, read receipt는 MVP 제외 — unread count만 다룬다(방 진입 시 0으로 갱신).
 *
 * ⚠️ 메시지 목록 스타일을 프로젝트 채팅(`[projectId]/messages`)과 같은 디스코드
 * 참조 기준으로 맞췄다(2026-07-06) — 말풍선 대신 아바타+이름+시각 헤더 뒤에
 * 본문이 이어지고, 연속된 같은 작성자 메시지는 헤더를 생략한다. DM은 1:1이라
 * 채널 목록/멤버 목록은 두지 않는다 — 대신 좌측 아이콘 레일(`dm/layout.tsx`)
 * 로 대화 상대를 전환한다("← 목록" 뒤로가기 링크는 그 레일이 대체해서 없앴다).
 *
 * ⚠️ MOCK: DM 메시지 API(`GET/POST /direct-messages/rooms/{roomId}/messages`,
 * mark-read)가 아직 없어서 로컬 state로 흉내낸다. 상대방 이름은 dm/mockRooms.ts
 * (아이콘 레일과 공유하는 목업 방 목록)에서 찾는다.
 */
import { useState } from "react";
import { useParams } from "next/navigation";
import { MOCK_ROOMS } from "../mockRooms";

type Message = {
  id: string;
  authorName: string;
  text: string;
  createdAt: string;
  isMine: boolean;
};

const MOCK_CURRENT_USER = "정철진";

const MOCK_MESSAGES: Message[] = [
  { id: "m1", authorName: "김민정", text: "안녕하세요, THR-101 반응형 이슈 확인 부탁드려요", createdAt: "10:02", isMine: false },
  { id: "m2", authorName: "김민정", text: "급한 건 아니고 시간 되실 때 봐주세요", createdAt: "10:02", isMine: false },
  { id: "m3", authorName: "정철진", text: "네 지금 바로 볼게요", createdAt: "10:03", isMine: true },
  { id: "m4", authorName: "김민정", text: "감사합니다!", createdAt: "10:03", isMine: false },
];

function initials(name: string) {
  return name.slice(0, 1);
}

export default function DmRoomPage() {
  const params = useParams<{ companyId: string; roomId: string }>();
  const peerName = MOCK_ROOMS.find((r) => r.roomId === params.roomId)?.name ?? params.roomId;

  const [messages, setMessages] = useState<Message[]>(MOCK_MESSAGES);
  const [draft, setDraft] = useState("");

  const send = () => {
    const text = draft.trim();
    if (!text) return;
    // TODO: POST /api/v1/direct-messages/rooms/{roomId}/messages
    setMessages((prev) => [
      ...prev,
      {
        id: crypto.randomUUID(),
        authorName: MOCK_CURRENT_USER,
        text,
        createdAt: new Date().toLocaleTimeString("ko-KR", { hour: "2-digit", minute: "2-digit" }),
        isMine: true,
      },
    ]);
    setDraft("");
  };

  return (
    <div className="flex h-full flex-col">
      <div className="flex h-14 shrink-0 items-center gap-2 border-b border-border px-5">
        <span className="flex h-7 w-7 items-center justify-center rounded-full avatar-ring text-xs font-semibold text-foreground">
          {initials(peerName)}
        </span>
        <h1 className="text-sm font-bold text-foreground">{peerName}</h1>
      </div>

      <div className="flex-1 overflow-y-auto px-5 py-4">
        <div className="flex flex-col">
          {messages.map((msg, i) => {
            const prev = messages[i - 1];
            const showHeader = !prev || prev.authorName !== msg.authorName;
            return (
              <div
                key={msg.id}
                className={"group flex gap-3 rounded-md px-2 py-0.5 hover:bg-border/20 " + (showHeader ? "mt-3" : "")}
              >
                <div className="w-9 shrink-0 text-right">
                  {showHeader ? (
                    <span className="flex h-9 w-9 items-center justify-center rounded-full avatar-ring text-xs font-semibold text-foreground">
                      {initials(msg.authorName)}
                    </span>
                  ) : (
                    <span className="hidden text-[10px] text-placeholder group-hover:inline-block">
                      {msg.createdAt}
                    </span>
                  )}
                </div>
                <div className="min-w-0 flex-1">
                  {showHeader && (
                    <div className="flex items-baseline gap-2">
                      <span className="text-sm font-semibold text-foreground">{msg.authorName}</span>
                      <span className="text-[10px] text-placeholder">{msg.createdAt}</span>
                    </div>
                  )}
                  <p className="text-sm text-foreground">{msg.text}</p>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      <div className="px-5 pb-5">
        <div className="flex items-center gap-2 rounded-xl bg-surface px-3 py-2 shadow-sm">
          <input
            aria-label="메시지 입력"
            value={draft}
            onChange={(e) => setDraft(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && send()}
            placeholder={`${peerName}에게 메시지 보내기`}
            className="flex-1 bg-transparent text-sm text-foreground placeholder:text-placeholder focus:outline-none"
          />
          <button
            onClick={send}
            className="shrink-0 rounded-lg btn-gradient px-3 py-1.5 text-xs font-semibold text-primary-foreground transition-colors duration-fast"
          >
            전송
          </button>
        </div>
      </div>
    </div>
  );
}
