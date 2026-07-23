"use client";

/*
 * S11 — 알림
 *
 * 접근 사용자: A/P/C. 화면정의서(docs/03-design/화면정의서.md §S11) 기준
 * 회사 전체 알림 목록(읽음 처리, 모두 읽음, 클릭 이동). WBS 4.8.2 —
 * 4.8.1 Backend(PR #147)의 실 API로 연동했다.
 *
 * "MVP는 GLOBAL scope만 허용. PROJECT scope는 보류"라서 이 화면은 회사
 * 단위(`/[companyId]/notifications`)로 둔다 — `[projectId]/notifications`
 * 스켈레톤은 보류된 PROJECT scope 자리라 그대로 둔다.
 *
 * 이번 slice에 반영하지 않은 것:
 *   - "알림 설정"(전체/이벤트별 on/off) — backend에 notification-preferences
 *     컨트롤러 자체가 없다(NOTI-02/03, WBS 4.8.1 범위 밖). 이전 mock의 설정
 *     섹션은 껐다.
 *
 * "알림 항목 클릭 이동"은 대상 타입마다 다르게 처리한다. 알림 응답의 target은
 * {type, id}만 주고 companyId/projectId를 안 준다(NotificationEvent 엔티티에는
 * 있는데 DTO에 안 실려 있다). 이 페이지는 company-scoped라 companyId는 route
 * param으로 알 수 있고, DIRECT_MESSAGE_ROOM/PROJECT는 target.id만으로 경로를
 * 만들 수 있다. DOCUMENT/THREAD/MESSAGE/SPRINT/ISSUE는 클릭 시 해당 리소스를
 * 한 번 더 조회해 projectId를 얻는다. ISSUE는 이슈 #176/PR #177로 `IssueResponse`에
 * `projectId`가 추가되면서 이제 가능해졌다 — 처음 이 PR을 낼 때는 그 필드가
 * 없어서 이동 불가였다.
 */
import { useCallback, useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { HubLink } from "@/components/company/HubLink";
import { invitationErrorMessage } from "@/lib/invitation";
import { documentService } from "@/services/document.service";
import { issueService } from "@/services/issue.service";
import { messageService } from "@/services/message.service";
import { notificationService } from "@/services/notification.service";
import { sprintService } from "@/services/sprint.service";
import { threadService } from "@/services/thread.service";
import type { NotificationItem, NotificationTarget, NotificationType } from "@/types/notification.types";

type LoadState = "loading" | "ready" | "error";

const TYPE_LABEL: Record<NotificationType, string> = {
  ISSUE_ASSIGNED: "이슈 할당",
  ISSUE_STATUS_CHANGED: "이슈 상태 변경",
  THREAD_COMMENTED: "쓰레드 댓글",
  MENTIONED: "쓰레드 @멘션",
  DOCUMENT_UPDATED: "문서 수정",
  DOCUMENT_LINKED_TO_ISSUE: "문서-이슈 연결",
  SPRINT_STARTED: "스프린트 시작",
  SPRINT_ENDED: "스프린트 종료",
  PROJECT_MESSAGE: "프로젝트 채팅 메시지",
  DIRECT_MESSAGE: "1:1 DM 메시지",
  MEMBER_ADDED: "구성원 추가",
  ROLE_CHANGED: "권한 변경",
};

export default function NotificationsPage() {
  const params = useParams<{ companyId: string }>();
  const router = useRouter();

  const [pageState, setPageState] = useState<LoadState>("loading");
  const [pageError, setPageError] = useState<string | null>(null);
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [nextCursor, setNextCursor] = useState<string | null>(null);
  const [loadingMore, setLoadingMore] = useState(false);
  const [showUnreadOnly, setShowUnreadOnly] = useState(false);
  const [markingAll, setMarkingAll] = useState(false);
  const [navigatingId, setNavigatingId] = useState<number | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  const load = useCallback(
    async (unreadOnly: boolean) => {
      setPageState("loading");
      setPageError(null);
      try {
        const { data } = await notificationService.list({ companyId: params.companyId, unreadOnly });
        setNotifications(data.data);
        setNextCursor(data.page.hasNext ? data.page.cursor : null);
        setPageState("ready");
      } catch (error) {
        setPageError(invitationErrorMessage(error, "알림을 불러오지 못했습니다."));
        setPageState("error");
      }
    },
    [params.companyId]
  );

  useEffect(() => {
    (async () => {
      await load(showUnreadOnly);
    })();
  }, [load, showUnreadOnly]);

  const loadMore = async () => {
    if (!nextCursor || loadingMore) return;
    setLoadingMore(true);
    try {
      const { data } = await notificationService.list({
        companyId: params.companyId,
        unreadOnly: showUnreadOnly,
        cursor: nextCursor,
      });
      setNotifications((prev) => [...prev, ...data.data]);
      setNextCursor(data.page.hasNext ? data.page.cursor : null);
    } catch (error) {
      setActionError(invitationErrorMessage(error, "알림을 더 불러오지 못했습니다."));
    } finally {
      setLoadingMore(false);
    }
  };

  const markRead = async (item: NotificationItem) => {
    if (item.readAt) return;
    try {
      const { data } = await notificationService.markRead(item.notificationRecipientId);
      setNotifications((prev) =>
        prev.map((n) =>
          n.notificationRecipientId === item.notificationRecipientId ? { ...n, readAt: data.data.readAt } : n
        )
      );
    } catch (error) {
      setActionError(invitationErrorMessage(error, "읽음 처리에 실패했습니다."));
    }
  };

  const markAllRead = async () => {
    if (markingAll) return;
    setMarkingAll(true);
    setActionError(null);
    try {
      await notificationService.markAllRead({ companyId: params.companyId });
      await load(showUnreadOnly);
    } catch (error) {
      setActionError(invitationErrorMessage(error, "모두 읽음 처리에 실패했습니다."));
    } finally {
      setMarkingAll(false);
    }
  };

  // DOCUMENT/THREAD/MESSAGE는 해당 리소스를 조회해야 projectId를 알 수 있다.
  const resolveHref = async (target: NotificationTarget): Promise<string | null> => {
    const companyId = params.companyId;
    switch (target.type) {
      case "DIRECT_MESSAGE_ROOM":
        return `/${companyId}/dm/${target.id}`;
      case "PROJECT":
        return `/${companyId}/${target.id}/board`;
      case "SPRINT": {
        const { data } = await sprintService.detail(target.id);
        return `/${companyId}/${data.data.sprint.projectId}/backlog`;
      }
      case "DOCUMENT": {
        const { data } = await documentService.detail(target.id);
        return `/${companyId}/${data.data.document.projectId}/docs/${target.id}`;
      }
      case "THREAD": {
        const { data } = await threadService.detail(target.id);
        return `/${companyId}/${data.data.thread.projectId}/threads/${target.id}`;
      }
      case "MESSAGE": {
        const { data } = await messageService.detail(target.id);
        return `/${companyId}/${data.data.message.projectId}/messages`;
      }
      case "ISSUE": {
        // 이슈 #176/PR #177로 IssueResponse에 projectId가 추가되면서 가능해졌다.
        const { data } = await issueService.detail(target.id);
        return `/${companyId}/${data.data.issue.projectId}/board`;
      }
      default:
        return null;
    }
  };

  const openNotification = async (item: NotificationItem) => {
    if (navigatingId !== null) return;
    setActionError(null);
    await markRead(item);
    setNavigatingId(item.notificationRecipientId);
    try {
      const href = await resolveHref(item.target);
      if (href) {
        router.push(href);
      } else {
        setActionError("이 알림은 아직 바로 이동할 수 없습니다.");
      }
    } catch {
      setActionError("이동할 화면을 찾지 못했습니다.");
    } finally {
      setNavigatingId(null);
    }
  };

  const unreadCount = notifications.filter((n) => !n.readAt).length;

  return (
    <div className="mx-auto max-w-2xl px-6 py-10">
      <HubLink companyId={params.companyId} className="text-sm text-muted transition-colors duration-fast hover:text-primary">
        ← 허브로
      </HubLink>
      <div className="mt-3 flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-foreground">알림</h1>
          <p className="mt-1 text-sm text-muted">읽지 않은 알림 {unreadCount}개</p>
        </div>
        <button
          onClick={() => void markAllRead()}
          disabled={markingAll}
          className="rounded-md px-3 py-1.5 text-sm font-medium text-primary transition-colors duration-fast hover:bg-primary-subtle disabled:opacity-50"
        >
          {markingAll ? "처리 중..." : "모두 읽음"}
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

      {actionError && <p className="mt-3 text-sm text-destructive">{actionError}</p>}

      {pageState === "loading" && <p className="mt-6 text-sm text-muted">불러오는 중...</p>}

      {pageState === "error" && (
        <div className="mt-6 rounded-xl border border-border bg-surface p-5">
          <p className="text-sm text-destructive">{pageError}</p>
          <button
            onClick={() => void load(showUnreadOnly)}
            className="mt-3 rounded-md border border-border px-3 py-1.5 text-xs font-medium text-foreground transition-colors duration-fast hover:bg-primary-subtle"
          >
            다시 시도
          </button>
        </div>
      )}

      {pageState === "ready" && (
        <>
          <ul className="mt-4 flex flex-col gap-2">
            {notifications.map((n) => {
              const read = n.readAt !== null;
              const busy = navigatingId === n.notificationRecipientId;
              return (
                <li key={n.notificationRecipientId}>
                  <button
                    type="button"
                    onClick={() => void openNotification(n)}
                    disabled={busy}
                    className={
                      "flex w-full items-start gap-3 rounded-xl px-4 py-3 text-left shadow-sm transition-shadow duration-fast hover:shadow-md disabled:opacity-60 " +
                      (read ? "bg-surface" : "bg-primary-subtle")
                    }
                  >
                    {!read && <span className="mt-1.5 h-2 w-2 shrink-0 rounded-full bg-primary" aria-hidden="true" />}
                    <div className={read ? "ml-5" : ""}>
                      <div className="flex items-center gap-2">
                        <span className="rounded-full bg-border px-2 py-0.5 text-[10px] font-medium text-muted">
                          {TYPE_LABEL[n.type]}
                        </span>
                        <span className="text-xs text-placeholder">
                          {new Date(n.createdAt).toLocaleString("ko-KR")}
                        </span>
                      </div>
                      <p className="mt-1 text-sm font-semibold text-foreground">{n.title}</p>
                      <p className="text-xs text-muted">{n.body}</p>
                    </div>
                  </button>
                </li>
              );
            })}
            {notifications.length === 0 && (
              <li className="rounded-xl border border-dashed border-border px-4 py-10 text-center text-sm text-muted">
                알림이 없습니다.
              </li>
            )}
          </ul>

          {nextCursor && (
            <div className="mt-4 text-center">
              <button
                onClick={() => void loadMore()}
                disabled={loadingMore}
                className="text-sm font-medium text-primary hover:opacity-70 disabled:opacity-50"
              >
                {loadingMore ? "불러오는 중..." : "더 보기"}
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
}
