"use client";

/*
 * S09 — 프로젝트 채팅
 *
 * 접근 사용자: A/P/C. 화면정의서(docs/03-design/화면정의서.md §S09) 기준
 * 프로젝트 단위 채팅. DM(`dm/[roomId]`)과 달리 메시지에서 이슈를 바로 만들 수
 * 있다("프로젝트 채팅 메시지 → 이슈 생성 가능" / DM은 `DM_MESSAGE_CANNOT_CREATE_ISSUE`
 * 로 금지). 메시지는 삭제 불가.
 *
 * ⚠️ MOCK: 메시지 API(`GET/POST /projects/{projectId}/messages`)와 메시지→이슈
 * 생성 API(`POST /projects/{projectId}/issues/from-message`)가 아직 없어서
 * 로컬 state로 흉내낸다. 실제로는 S05 칸반 보드의 이슈 목록과 저장소를 공유해야
 * 하지만 지금은 두 화면이 각자 목업 state를 갖고 있어 이 화면에서 만든 이슈가
 * 보드에 실제로 나타나지는 않는다(연동은 Sprint 2 백엔드 이후).
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
  convertedToIssue: boolean;
};

const MOCK_CURRENT_USER = "정철진";

const MOCK_MESSAGES: Message[] = [
  { id: "m1", authorName: "여도현", text: "허브 상단바 알림 뱃지 API 아직 안 붙었네요, 이슈로 뺄게요", createdAt: "09:12", isMine: false, convertedToIssue: false },
  { id: "m2", authorName: "정철진", text: "넵 확인했습니다", createdAt: "09:13", isMine: true, convertedToIssue: false },
  { id: "m3", authorName: "황스일", text: "로고 업로드 S3 연동 아직 안 됐죠?", createdAt: "09:20", isMine: false, convertedToIssue: false },
];

export default function ProjectMessagesPage() {
  const params = useParams<{ companyId: string; projectId: string }>();

  const [messages, setMessages] = useState<Message[]>(MOCK_MESSAGES);
  const [draft, setDraft] = useState("");

  const send = () => {
    const text = draft.trim();
    if (!text) return;
    // TODO: POST /api/v1/projects/{projectId}/messages
    setMessages((prev) => [
      ...prev,
      {
        id: crypto.randomUUID(),
        authorName: MOCK_CURRENT_USER,
        text,
        createdAt: new Date().toLocaleTimeString("ko-KR", { hour: "2-digit", minute: "2-digit" }),
        isMine: true,
        convertedToIssue: false,
      },
    ]);
    setDraft("");
  };

  const convertToIssue = (id: string) => {
    // TODO: POST /api/v1/projects/{projectId}/issues/from-message
    setMessages((prev) => prev.map((m) => (m.id === id ? { ...m, convertedToIssue: true } : m)));
  };

  return (
    <div className="mx-auto flex h-full max-w-2xl flex-col px-6 py-6">
      <div className="border-b border-border pb-4">
        <Link
          href={`/${params.companyId}/projects`}
          className="text-sm text-muted transition-colors duration-fast hover:text-primary"
        >
          ← 프로젝트 목록
        </Link>
        <h1 className="mt-2 text-lg font-bold text-foreground">프로젝트 채팅</h1>
        <p className="mt-0.5 text-sm text-muted">프로젝트 {params.projectId}</p>
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
                <div className="flex items-center gap-2">
                  <span className="text-[10px] text-placeholder">{msg.createdAt}</span>
                  {msg.convertedToIssue ? (
                    <span className="text-[10px] font-medium text-primary">이슈로 등록됨</span>
                  ) : (
                    <button
                      onClick={() => convertToIssue(msg.id)}
                      className="text-[10px] font-medium text-muted transition-colors duration-fast hover:text-primary"
                    >
                      + 이슈로 만들기
                    </button>
                  )}
                </div>
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
          placeholder="메시지 보내기"
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
