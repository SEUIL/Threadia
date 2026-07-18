"use client";

/*
 * S10 — 쓰레드
 *
 * 접근 사용자: A/P/C. 화면정의서(docs/03-design/화면정의서.md §S10) 기준
 * 이슈/문서/메시지에 연결된 대표 Thread 하나의 상세와 댓글을 보여준다.
 * WBS 4.7.1.2 — 4.7.1.1 Backend(PR #139)의 실 API로 연동했다.
 *
 * 화면정의서 설계 메모 중 이번 slice에 반영하지 않은 것:
 *   - "@멘션" — API-명세서 §13.7 WBS 4.7.1.1 정책에 "멘션 파싱·저장·notification/event
 *     발행은 제공하지 않는다"고 명시돼 있어, 댓글은 일반 텍스트로만 작성한다.
 *   - "댓글 작성자 프로필에서 S09 1:1 DM으로 이동" — DM Frontend(WBS 4.7.3.2)가
 *     아직 없어서 이번 slice에서는 제외한다.
 *
 * 컨텍스트 배너는 대상 타입별로 다른 API를 부른다. 문서 상세 조회는 WBS 4.6.2
 * (documents.service.ts, 별도 PR)가 아직 dev에 없어 이 파일 안에서 apiClient로
 * 직접 부른다 — 그 PR이 머지되면 document.service.ts의 detail()로 옮긴다.
 * 하나가 실패해도 쓰레드 자체는 보이게 컨텍스트 로드는 별도 상태로 분리했다
 * (board.tsx의 members 분리와 동일 패턴).
 */
import { useCallback, useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { useAuth } from "@/contexts/AuthContext";
import apiClient from "@/lib/axios";
import { invitationErrorMessage } from "@/lib/invitation";
import { threadService } from "@/services/thread.service";
import { issueService } from "@/services/issue.service";
import { messageService } from "@/services/message.service";
import { projectService } from "@/services/project.service";
import type { ApiResponse } from "@/types/auth.types";
import type { ThreadCommentResponse, ThreadResponse } from "@/types/thread.types";

type Role = "A" | "P" | "C";
type LoadState = "loading" | "ready" | "error";

const TARGET_TYPE_LABEL: Record<ThreadResponse["targetType"], string> = {
  ISSUE: "이슈",
  DOCUMENT: "문서",
  MESSAGE: "채팅 메시지",
};

type TargetContext = { label: string; href: string } | null;

function viewerRole(viewer: { companyRole: string | null; projectRole: string | null }): Role {
  if (viewer.companyRole === "COMPANY_ADMIN") return "C";
  return viewer.projectRole === "PROJECT_ADMIN" ? "P" : "A";
}

export default function ThreadPage() {
  const params = useParams<{ companyId: string; projectId: string; threadId: string }>();
  const { companyId, projectId, threadId } = params;
  const { user } = useAuth();

  const [pageState, setPageState] = useState<LoadState>("loading");
  const [pageError, setPageError] = useState<string | null>(null);
  const [role, setRole] = useState<Role | null>(null);
  const [thread, setThread] = useState<ThreadResponse | null>(null);
  const [comments, setComments] = useState<ThreadCommentResponse[]>([]);
  const [targetContext, setTargetContext] = useState<TargetContext>(null);

  const [editingTopic, setEditingTopic] = useState(false);
  const [topicDraft, setTopicDraft] = useState("");
  const [topicBusy, setTopicBusy] = useState(false);
  const [topicError, setTopicError] = useState<string | null>(null);

  const [newComment, setNewComment] = useState("");
  const [commentBusy, setCommentBusy] = useState(false);
  const [commentError, setCommentError] = useState<string | null>(null);

  const loadTargetContext = useCallback(async (loadedThread: ThreadResponse) => {
    try {
      if (loadedThread.targetType === "DOCUMENT") {
        const { data } = await apiClient.get<ApiResponse<{ document: { title: string } }>>(
          `/api/v1/documents/${loadedThread.targetId}`
        );
        setTargetContext({
          label: `문서 · ${data.data.document.title}`,
          href: `/${companyId}/${projectId}/docs/${loadedThread.targetId}`,
        });
      } else if (loadedThread.targetType === "ISSUE") {
        const { data } = await issueService.detail(loadedThread.targetId);
        setTargetContext({
          label: `이슈 · ${data.data.issue.issueKey} ${data.data.issue.title}`,
          href: `/${companyId}/${projectId}/board`,
        });
      } else {
        const { data } = await messageService.detail(loadedThread.targetId);
        setTargetContext({
          label: `채팅 메시지 · ${data.data.message.content.slice(0, 40)}`,
          href: `/${companyId}/${projectId}/messages`,
        });
      }
    } catch {
      setTargetContext(null);
    }
  }, [companyId, projectId]);

  const load = useCallback(async () => {
    try {
      const [projectResponse, threadResponse, commentsResponse] = await Promise.all([
        projectService.detail(projectId),
        threadService.detail(threadId),
        threadService.listComments(threadId),
      ]);
      setPageError(null);
      setRole(viewerRole(projectResponse.data.data.viewer));
      setThread(threadResponse.data.data.thread);
      setComments(commentsResponse.data.data.comments);
      setPageState("ready");
      await loadTargetContext(threadResponse.data.data.thread);
    } catch (error) {
      setPageError(invitationErrorMessage(error, "쓰레드를 불러오지 못했습니다."));
      setPageState("error");
    }
  }, [projectId, threadId, loadTargetContext]);

  useEffect(() => {
    (async () => {
      await load();
    })();
  }, [load]);

  const canEditTopic = useMemo(
    () => thread !== null && (role === "P" || role === "C" || thread.createdByUserId === user?.id),
    [thread, role, user?.id]
  );

  const startEditTopic = () => {
    setTopicDraft(thread?.topic ?? "");
    setTopicError(null);
    setEditingTopic(true);
  };

  const saveTopic = async () => {
    if (topicBusy || !thread) return;
    setTopicBusy(true);
    setTopicError(null);
    try {
      const trimmed = topicDraft.trim();
      const { data } = await threadService.updateTopic(thread.id, { topic: trimmed === "" ? null : trimmed });
      setThread(data.data.thread);
      setEditingTopic(false);
    } catch (error) {
      setTopicError(invitationErrorMessage(error, "주제 수정에 실패했습니다."));
    } finally {
      setTopicBusy(false);
    }
  };

  const submitComment = async () => {
    const content = newComment.trim();
    if (!content || commentBusy || !thread) return;
    setCommentBusy(true);
    setCommentError(null);
    try {
      const { data } = await threadService.createComment(thread.id, { content });
      setComments((prev) => [...prev, data.data.comment]);
      setNewComment("");
    } catch (error) {
      setCommentError(invitationErrorMessage(error, "댓글 작성에 실패했습니다."));
    } finally {
      setCommentBusy(false);
    }
  };

  if (pageState === "loading") {
    return <div className="mx-auto max-w-2xl px-6 py-10 text-sm text-muted">쓰레드를 불러오는 중...</div>;
  }

  if (pageState === "error" || !thread) {
    return (
      <div className="mx-auto max-w-2xl px-6 py-10">
        <div className="rounded-lg border border-destructive/30 bg-destructive-subtle px-4 py-6 text-center text-sm text-destructive">
          {pageError}
        </div>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-2xl px-6 py-10">
      {/* 상위 컨텍스트 배너 — 연결된 이슈/문서/메시지를 항상 보여준다(설계 메모) */}
      <div className="rounded-lg border border-border bg-surface px-4 py-2.5 text-xs text-muted">
        {targetContext ? (
          <Link href={targetContext.href} className="font-medium text-primary hover:opacity-70">
            {targetContext.label} →
          </Link>
        ) : (
          <span>{TARGET_TYPE_LABEL[thread.targetType]} #{thread.targetId}</span>
        )}
      </div>

      <div className="mt-4">
        {editingTopic ? (
          <div className="flex items-center gap-2">
            <input
              aria-label="쓰레드 주제"
              value={topicDraft}
              onChange={(e) => setTopicDraft(e.target.value)}
              placeholder="주제 (선택, 비우면 제거)"
              maxLength={200}
              className="flex-1 rounded-lg border border-border bg-surface px-3 py-2 text-base font-semibold text-foreground placeholder:text-placeholder focus:border-ring focus:outline-none"
            />
            <button
              onClick={() => void saveTopic()}
              disabled={topicBusy}
              className="rounded-md btn-gradient px-3 py-1.5 text-sm font-semibold text-primary-foreground disabled:opacity-50"
            >
              저장
            </button>
            <button
              onClick={() => setEditingTopic(false)}
              className="rounded-md px-3 py-1.5 text-sm font-medium text-muted hover:bg-primary-subtle"
            >
              취소
            </button>
          </div>
        ) : (
          <div className="flex items-center gap-2">
            <h1 className="text-xl font-bold text-foreground">{thread.topic ?? "쓰레드"}</h1>
            {canEditTopic && (
              <button
                onClick={startEditTopic}
                className="text-xs font-medium text-primary hover:opacity-70"
              >
                주제 수정
              </button>
            )}
          </div>
        )}
        {topicError && <p className="mt-2 text-xs text-destructive">{topicError}</p>}
      </div>

      {/* 댓글 — 생성 순서 append-only. 수정/삭제 없음(결정 맥락 보존, 설계 메모) */}
      <ul className="mt-6 flex flex-col gap-3">
        {comments.map((comment) => (
          <li key={comment.id} className="rounded-xl border border-border bg-surface p-4">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold text-foreground">사용자 #{comment.authorUserId}</span>
              <span className="text-[10px] text-muted">
                {new Date(comment.createdAt).toLocaleString("ko-KR")}
              </span>
            </div>
            <p className="mt-1.5 whitespace-pre-wrap text-sm text-foreground">{comment.content}</p>
          </li>
        ))}
        {comments.length === 0 && (
          <li className="rounded-xl border border-dashed border-border px-4 py-8 text-center text-sm text-muted">
            아직 댓글이 없습니다.
          </li>
        )}
      </ul>

      <div className="mt-4">
        <textarea
          aria-label="댓글 작성"
          value={newComment}
          onChange={(e) => setNewComment(e.target.value)}
          placeholder="댓글을 입력하세요"
          rows={3}
          className="w-full resize-none rounded-xl border border-border bg-surface p-3 text-sm text-foreground placeholder:text-placeholder focus:border-ring focus:outline-none"
        />
        <div className="mt-2 flex items-center justify-end gap-2">
          {commentError && <p className="mr-auto text-xs text-destructive">{commentError}</p>}
          <button
            onClick={() => void submitComment()}
            disabled={commentBusy || !newComment.trim()}
            className="rounded-md btn-gradient px-4 py-1.5 text-sm font-semibold text-primary-foreground disabled:cursor-not-allowed disabled:opacity-50"
          >
            {commentBusy ? "작성 중..." : "댓글 작성"}
          </button>
        </div>
      </div>
    </div>
  );
}
