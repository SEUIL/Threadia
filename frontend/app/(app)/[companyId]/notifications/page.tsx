"use client";

/*
 * S11 — 알림
 *
 * 접근 사용자: A/P/C. 화면정의서(docs/03-design/화면정의서.md §S11) 기준
 * 회사 전체 알림 목록(읽음 처리, 모두 읽음, 클릭 이동) + 이벤트별 수신 설정.
 * "MVP는 GLOBAL scope만 허용. PROJECT scope는 보류"라서 이 화면은 회사
 * 단위(`/[companyId]/notifications`)로 둔다 — `[projectId]/notifications`
 * 스켈레톤은 보류된 PROJECT scope 자리라 그대로 둔다.
 *
 * 참고: HubOverlay의 "알림" 상단바 링크가 원래 이 경로(`/[companyId]/notifications`)를
 * 가리키고 있었는데 이 페이지가 없어서 404였다 — S09 dm/page.tsx 때와 같은 종류의
 * 빠진 진입점 버그라 이번에 같이 채운다.
 *
 * ⚠️ MOCK: 알림 API(`GET /notifications`, `/notifications/{recipientId}/read`,
 * `/notifications/read-all`, `GET/PUT /notification-preferences`)가 아직
 * 없어서 로컬 state로 흉내낸다.
 */
import { useState } from "react";
import Link from "next/link";

type NotificationEvent =
  | "ISSUE_ASSIGNED"
  | "ISSUE_STATUS_CHANGED"
  | "THREAD_MENTION"
  | "DOCUMENT_UPDATED"
  | "PROJECT_MESSAGE"
  | "DIRECT_MESSAGE"
  | "MEMBER_ROLE_CHANGED";

type Notification = {
  id: string;
  event: NotificationEvent;
  title: string;
  body: string;
  senderName: string;
  href: string;
  read: boolean;
  createdAt: string;
};

// 화면정의서 "알림 이벤트 MVP 범위" 표 기준(문서 수정 / 문서에서 열린 쓰레드는
// 둘 다 멘션 규칙을 쓰는 THREAD_MENTION·DOCUMENT_UPDATED로 커버)
const EVENT_LABEL: Record<NotificationEvent, string> = {
  ISSUE_ASSIGNED: "이슈 할당",
  ISSUE_STATUS_CHANGED: "이슈 상태 변경",
  THREAD_MENTION: "쓰레드 @멘션",
  DOCUMENT_UPDATED: "문서 수정",
  PROJECT_MESSAGE: "프로젝트 채팅 메시지",
  DIRECT_MESSAGE: "1:1 DM 메시지",
  MEMBER_ROLE_CHANGED: "구성원 추가/권한 변경",
};

const MOCK_NOTIFICATIONS: Notification[] = [
  { id: "n1", event: "ISSUE_ASSIGNED", title: "이슈가 배정됐어요", body: "THR-101 로그인 화면 반응형 깨짐", senderName: "정철진", href: "/demo-company/p1/board", read: false, createdAt: "10분 전" },
  { id: "n2", event: "THREAD_MENTION", title: "쓰레드에서 멘션됐어요", body: "@정철진 이 부분 검토 부탁드려요", senderName: "김민정", href: "/demo-company/p1/board", read: false, createdAt: "32분 전" },
  { id: "n3", event: "DIRECT_MESSAGE", title: "새 DM", body: "감사합니다!", senderName: "김민정", href: "/demo-company/dm/u2", read: false, createdAt: "1시간 전" },
  { id: "n4", event: "PROJECT_MESSAGE", title: "프로젝트 채팅 새 메시지", body: "로고 업로드 S3 연동 아직 안 됐죠?", senderName: "황스일", href: "/demo-company/p1/messages", read: true, createdAt: "3시간 전" },
  { id: "n5", event: "MEMBER_ROLE_CHANGED", title: "권한이 변경됐어요", body: "COMPANY_ADMIN 권한이 부여됐습니다", senderName: "시스템", href: "/demo-company/settings", read: true, createdAt: "어제" },
  { id: "n6", event: "ISSUE_STATUS_CHANGED", title: "이슈 상태가 바뀌었어요", body: "THR-104 칸반 보드 성능 프로파일링 → 진행중", senderName: "정철진", href: "/demo-company/p1/board", read: true, createdAt: "어제" },
];

export default function NotificationsPage() {
  const [notifications, setNotifications] = useState<Notification[]>(MOCK_NOTIFICATIONS);
  const [showUnreadOnly, setShowUnreadOnly] = useState(false);

  const [globalEnabled, setGlobalEnabled] = useState(true);
  const [eventPrefs, setEventPrefs] = useState<Record<NotificationEvent, boolean>>({
    ISSUE_ASSIGNED: true,
    ISSUE_STATUS_CHANGED: true,
    THREAD_MENTION: true,
    DOCUMENT_UPDATED: true,
    PROJECT_MESSAGE: true,
    DIRECT_MESSAGE: true,
    MEMBER_ROLE_CHANGED: true,
  });

  const unreadCount = notifications.filter((n) => !n.read).length;
  const visibleNotifications = showUnreadOnly ? notifications.filter((n) => !n.read) : notifications;

  const markRead = (id: string) => {
    // TODO: POST /api/v1/notifications/{recipientId}/read
    setNotifications((prev) => prev.map((n) => (n.id === id ? { ...n, read: true } : n)));
  };

  const markAllRead = () => {
    // TODO: POST /api/v1/notifications/read-all
    setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
  };

  const toggleEventPref = (event: NotificationEvent) => {
    // TODO: PUT /api/v1/notification-preferences
    setEventPrefs((prev) => ({ ...prev, [event]: !prev[event] }));
  };

  return (
    <div className="mx-auto max-w-2xl px-6 py-10">
      <Link href="/hub" className="text-sm text-muted transition-colors duration-fast hover:text-primary">
        ← 허브로
      </Link>
      <div className="mt-3 flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-foreground">알림</h1>
          <p className="mt-1 text-sm text-muted">읽지 않은 알림 {unreadCount}개</p>
        </div>
        <button
          onClick={markAllRead}
          className="rounded-md px-3 py-1.5 text-sm font-medium text-primary transition-colors duration-fast hover:bg-primary-subtle"
        >
          모두 읽음
        </button>
      </div>

      <label className="mt-4 flex items-center gap-2 text-sm text-muted">
        <input
          type="checkbox"
          checked={showUnreadOnly}
          onChange={(e) => setShowUnreadOnly(e.target.checked)}
          className="h-4 w-4 rounded border-border accent-primary"
        />
        읽지 않은 알림만 보기
      </label>

      <ul className="mt-4 flex flex-col gap-2">
        {visibleNotifications.map((n) => (
          <li key={n.id}>
            <Link
              href={n.href}
              onClick={() => markRead(n.id)}
              className={
                "flex items-start gap-3 rounded-xl px-4 py-3 shadow-sm transition-shadow duration-fast hover:shadow-md " +
                (n.read ? "bg-surface" : "bg-primary-subtle")
              }
            >
              {!n.read && <span className="mt-1.5 h-2 w-2 shrink-0 rounded-full bg-primary" aria-hidden="true" />}
              <div className={n.read ? "ml-5" : ""}>
                <div className="flex items-center gap-2">
                  <span className="rounded-full bg-border px-2 py-0.5 text-[10px] font-medium text-muted">
                    {EVENT_LABEL[n.event]}
                  </span>
                  <span className="text-xs text-placeholder">{n.createdAt}</span>
                </div>
                <p className="mt-1 text-sm font-semibold text-foreground">{n.title}</p>
                <p className="text-xs text-muted">{n.senderName} · {n.body}</p>
              </div>
            </Link>
          </li>
        ))}
        {visibleNotifications.length === 0 && (
          <li className="rounded-xl border border-dashed border-border px-4 py-10 text-center text-sm text-muted">
            알림이 없습니다.
          </li>
        )}
      </ul>

      {/* 알림 설정 */}
      <section className="mt-8 rounded-xl border border-border bg-surface p-5">
        <div className="flex items-center justify-between">
          <h2 className="text-sm font-semibold text-foreground">알림 설정</h2>
          <label className="flex items-center gap-2 text-sm text-foreground">
            전체 알림
            <input
              type="checkbox"
              checked={globalEnabled}
              onChange={(e) => setGlobalEnabled(e.target.checked)}
              className="h-4 w-4 rounded border-border accent-primary"
            />
          </label>
        </div>

        <ul className={"mt-3 flex flex-col gap-2 " + (!globalEnabled ? "opacity-50" : "")}>
          {(Object.keys(EVENT_LABEL) as NotificationEvent[]).map((event) => (
            <li key={event} className="flex items-center justify-between text-sm text-foreground">
              {EVENT_LABEL[event]}
              <input
                type="checkbox"
                disabled={!globalEnabled}
                checked={eventPrefs[event]}
                onChange={() => toggleEventPref(event)}
                className="h-4 w-4 rounded border-border accent-primary disabled:cursor-not-allowed"
              />
            </li>
          ))}
        </ul>
      </section>
    </div>
  );
}
