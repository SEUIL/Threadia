"use client";

/*
 * S09 — 프로젝트 채팅
 *
 * 접근 사용자: A/P/C. 화면정의서(docs/03-design/화면정의서.md §S09) 기준
 * 프로젝트 단위 채팅. DM(`dm/[roomId]`)과 달리 메시지에서 이슈를 바로 만들 수
 * 있다("프로젝트 채팅 메시지 → 이슈 생성 가능" / DM은 `DM_MESSAGE_CANNOT_CREATE_ISSUE`
 * 로 금지). 메시지는 삭제 불가.
 *
 * ⚠️ 레이아웃을 디스코드 참조 스크린샷 기준으로 다시 잡았다(2026-07-06):
 * 채널 목록(좌) + 메시지 목록(중앙) + 멤버 목록(우) 3단 구성, 말풍선 대신
 * 아바타+이름+시각 헤더 뒤에 본문이 이어지는 flat 메시지 스타일, 연속된
 * 같은 작성자 메시지는 헤더를 생략한다.
 *
 * Threadia는 아직 프로젝트당 채팅 채널이 1개뿐이라(멀티 채널 개념 없음),
 * 좌측 채널 목록은 "# 프로젝트 채팅" 단일 항목만 있는 가짜 목록이다 — 실제
 * 멀티채널 API가 생기면 그때 목록을 채운다.
 *
 * ⚠️ MOCK: 메시지 API(`GET/POST /projects/{projectId}/messages`)와 메시지→이슈
 * 생성 API(`POST /projects/{projectId}/issues/from-message`)가 아직 없어서
 * 로컬 state로 흉내낸다. 실제로는 S05 칸반 보드의 이슈 목록과 저장소를 공유해야
 * 하지만 지금은 두 화면이 각자 목업 state를 갖고 있어 이 화면에서 만든 이슈가
 * 보드에 실제로 나타나지는 않는다(연동은 Sprint 2 백엔드 이후). 멤버 목록의
 * 온라인/오프라인 상태도 실 presence API가 없어서 상수로 고정했다.
 */
import { useState } from "react";
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
  { id: "m2", authorName: "여도현", text: "일단 백로그로 넣어둘게요", createdAt: "09:12", isMine: false, convertedToIssue: false },
  { id: "m3", authorName: "정철진", text: "넵 확인했습니다", createdAt: "09:13", isMine: true, convertedToIssue: false },
  { id: "m4", authorName: "황스일", text: "로고 업로드 S3 연동 아직 안 됐죠?", createdAt: "09:20", isMine: false, convertedToIssue: false },
];

// TODO(Sprint 2): 실 presence API 연동 전까지 온라인/오프라인은 상수로 고정.
const MOCK_MEMBERS = {
  online: [
    { name: "정철진", role: "나" },
    { name: "여도현", role: "" },
  ],
  offline: [
    { name: "황스일", role: "" },
    { name: "김민정", role: "" },
  ],
};

function initials(name: string) {
  return name.slice(0, 1);
}

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
    <div className="flex h-full">
      {/* 채널 목록 — 지금은 단일 채널뿐이라 항목 1개짜리 가짜 목록 */}
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
        <div className="flex items-center gap-2 border-t border-border px-3 py-2.5">
          <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full avatar-ring text-xs font-semibold text-foreground">
            {initials(MOCK_CURRENT_USER)}
          </span>
          <span className="min-w-0 flex-1 truncate text-sm font-medium text-foreground">
            {MOCK_CURRENT_USER}
          </span>
        </div>
      </aside>

      {/* 메시지 영역 */}
      <div className="flex min-w-0 flex-1 flex-col">
        <div className="flex h-14 shrink-0 items-center border-b border-border px-5">
          <span className="mr-1.5 text-muted">#</span>
          <h1 className="text-sm font-bold text-foreground">프로젝트 채팅</h1>
        </div>

        <div className="flex-1 overflow-y-auto px-5 py-4">
          <div className="flex flex-col">
            {messages.map((msg, i) => {
              const prev = messages[i - 1];
              const showHeader = !prev || prev.authorName !== msg.authorName;
              return (
                <div
                  key={msg.id}
                  className={"group relative flex gap-3 rounded-md px-2 py-0.5 hover:bg-border/20 " + (showHeader ? "mt-3" : "")}
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

                  {/* hover 시 노출되는 액션(이슈 전환) */}
                  <div className="absolute right-2 top-0 hidden -translate-y-1/2 items-center gap-1 rounded-md border border-border bg-surface px-1.5 py-1 shadow-sm group-hover:flex">
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
              placeholder="#프로젝트 채팅에 메시지 보내기"
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

      {/* 멤버 목록 */}
      <aside className="hidden w-52 shrink-0 flex-col gap-4 overflow-y-auto border-l border-border bg-surface px-3 py-4 lg:flex">
        <div>
          <p className="px-1 pb-1.5 text-[11px] font-semibold uppercase tracking-wide text-muted">
            온라인 — {MOCK_MEMBERS.online.length}
          </p>
          {MOCK_MEMBERS.online.map((m) => (
            <div key={m.name} className="flex items-center gap-2 rounded-md px-1 py-1.5 hover:bg-border/20">
              <span className="relative flex h-7 w-7 shrink-0 items-center justify-center rounded-full avatar-ring text-[11px] font-semibold text-foreground">
                {initials(m.name)}
                <span className="absolute -bottom-0.5 -right-0.5 h-2.5 w-2.5 rounded-full border-2 border-surface bg-success" />
              </span>
              <span className="truncate text-sm text-foreground">
                {m.name}
                {m.role && <span className="ml-1 text-xs text-muted">({m.role})</span>}
              </span>
            </div>
          ))}
        </div>
        <div>
          <p className="px-1 pb-1.5 text-[11px] font-semibold uppercase tracking-wide text-muted">
            오프라인 — {MOCK_MEMBERS.offline.length}
          </p>
          {MOCK_MEMBERS.offline.map((m) => (
            <div key={m.name} className="flex items-center gap-2 rounded-md px-1 py-1.5 opacity-50 hover:bg-border/20">
              <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full avatar-ring text-[11px] font-semibold text-foreground">
                {initials(m.name)}
              </span>
              <span className="truncate text-sm text-foreground">{m.name}</span>
            </div>
          ))}
        </div>
      </aside>
    </div>
  );
}
