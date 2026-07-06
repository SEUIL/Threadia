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
 * ⚠️ MOCK: DM 메시지 API(`GET/POST /direct-messages/rooms/{roomId}/messages`,
 * mark-read)가 아직 없어서 로컬 state로 흉내낸다. 상대방 이름은 S14의 목업
 * 구성원 목록과 매칭(같은 id 체계: u1~u4).
 */
import { useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";

type Message = {
  id: string;
  authorName: string;
  text: string;
  createdAt: string;
  isMine: boolean;
};

const MOCK_CURRENT_USER = "정철진";

// S14 구성원 목업과 같은 id 체계(u1~u4)
const MOCK_MEMBER_NAMES: Record<string, string> = {
  u1: "정철진",
  u2: "김민정",
  u3: "여도현",
  u4: "황스일",
};

const MOCK_MESSAGES: Message[] = [
  { id: "m1", authorName: "김민정", text: "안녕하세요, THR-101 반응형 이슈 확인 부탁드려요", createdAt: "10:02", isMine: false },
  { id: "m2", authorName: "정철진", text: "네 지금 바로 볼게요", createdAt: "10:03", isMine: true },
  { id: "m3", authorName: "김민정", text: "감사합니다!", createdAt: "10:03", isMine: false },
];

export default function DmRoomPage() {
  const params = useParams<{ companyId: string; roomId: string }>();
  const peerName = MOCK_MEMBER_NAMES[params.roomId] ?? params.roomId;

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
    <div className="mx-auto flex h-full max-w-2xl flex-col px-6 py-6">
      <div className="border-b border-border pb-4">
        <Link
          href={`/${params.companyId}/dm`}
          className="text-sm text-muted transition-colors duration-fast hover:text-primary"
        >
          ← 채팅·DM 목록
        </Link>
        <div className="mt-2 flex items-center gap-3">
          <span className="flex h-9 w-9 items-center justify-center rounded-full avatar-ring text-sm font-semibold">
            {peerName.slice(0, 1)}
          </span>
          <h1 className="text-lg font-bold text-foreground">{peerName}</h1>
        </div>
      </div>

      <div className="flex-1 overflow-y-auto py-4">
        <div className="flex flex-col gap-3">
          {messages.map((msg) => (
            <div key={msg.id} className={"flex " + (msg.isMine ? "justify-end" : "justify-start")}>
              <div className={"max-w-[75%] " + (msg.isMine ? "items-end" : "items-start") + " flex flex-col gap-1"}>
                {!msg.isMine && <span className="text-xs text-muted">{msg.authorName}</span>}
                <div
                  className={
                    "rounded-2xl px-4 py-2 text-sm " +
                    (msg.isMine
                      ? "rounded-br-sm bg-primary text-primary-foreground"
                      : "rounded-bl-sm bg-surface text-foreground shadow-sm")
                  }
                >
                  {msg.text}
                </div>
                <span className="text-[10px] text-placeholder">{msg.createdAt}</span>
              </div>
            </div>
          ))}
        </div>
      </div>

      <div className="flex gap-2 border-t border-border pt-4">
        <input
          aria-label="메시지 입력"
          value={draft}
          onChange={(e) => setDraft(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && send()}
          placeholder={`${peerName}에게 메시지 보내기`}
          className="flex-1 rounded-full border border-border bg-surface px-4 py-2 text-sm text-foreground placeholder:text-placeholder focus:border-ring focus:outline-none"
        />
        <button
          onClick={send}
          className="rounded-full btn-gradient px-4 py-2 text-sm font-semibold text-primary-foreground transition-colors duration-fast"
        >
          전송
        </button>
      </div>
    </div>
  );
}
