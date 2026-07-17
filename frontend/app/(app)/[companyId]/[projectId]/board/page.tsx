"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import {
  DndContext,
  useDraggable,
  useDroppable,
  useSensor,
  useSensors,
  PointerSensor,
  type DragEndEvent,
} from "@dnd-kit/core";
import { CSS } from "@dnd-kit/utilities";
import { invitationErrorMessage } from "@/lib/invitation";
import { issueService } from "@/services/issue.service";
import { projectService } from "@/services/project.service";
import { projectMemberService } from "@/services/project-member.service";
import { useAuth } from "@/contexts/AuthContext";
import { IssueDetailModal } from "@/components/issues/IssueDetailModal";
import type { IssueResponse, IssueStatus } from "@/types/issue.types";
import type { ProjectViewerData } from "@/types/project.types";
import type { ProjectMemberResponse } from "@/types/project-member.types";

type Role = "A" | "P" | "C";
type LoadState = "loading" | "ready" | "error";

const COLUMNS: { status: IssueStatus; label: string }[] = [
  { status: "BACKLOG", label: "백로그" },
  { status: "TODO", label: "할일" },
  { status: "IN_PROGRESS", label: "진행중" },
  { status: "IN_REVIEW", label: "검토중" },
  { status: "DONE", label: "완료" },
];

const A_ALLOWED_STATUSES: IssueStatus[] = ["BACKLOG", "TODO", "IN_PROGRESS", "IN_REVIEW"];

function viewerRole(viewer: ProjectViewerData): Role {
  if (viewer.companyRole === "COMPANY_ADMIN") return "C";
  return viewer.projectRole === "PROJECT_ADMIN" ? "P" : "A";
}

function statusOptionsFor(role: Role): IssueStatus[] {
  return role === "A" ? A_ALLOWED_STATUSES : COLUMNS.map((column) => column.status);
}

function DroppableColumn({
  status,
  disabled,
  children,
}: {
  status: IssueStatus;
  disabled: boolean;
  children: React.ReactNode;
}) {
  const { setNodeRef, isOver } = useDroppable({ id: status, disabled });
  return (
    <div
      ref={setNodeRef}
      className={
        "flex min-h-24 flex-col gap-2 rounded-xl p-2 transition-colors duration-fast " +
        (isOver && !disabled ? "bg-primary-subtle" : "bg-border/30")
      }
    >
      {children}
    </div>
  );
}

function DraggableIssueCard({
  id,
  disabled,
  children,
}: {
  id: number;
  disabled: boolean;
  children: React.ReactNode;
}) {
  const { attributes, listeners, setNodeRef, transform, isDragging } = useDraggable({ id, disabled });
  return (
    <div
      ref={setNodeRef}
      {...listeners}
      {...attributes}
      style={{ transform: transform ? CSS.Translate.toString(transform) : undefined }}
      className={
        "rounded-lg bg-surface p-3 shadow-sm " +
        (disabled ? "cursor-not-allowed opacity-60" : "cursor-grab active:cursor-grabbing") +
        (isDragging ? " z-raised opacity-50" : "")
      }
    >
      {children}
    </div>
  );
}

export default function BoardPage() {
  const params = useParams<{ companyId: string; projectId: string }>();
  const { user } = useAuth();
  const [pageState, setPageState] = useState<LoadState>("loading");
  const [pageError, setPageError] = useState<string | null>(null);
  const [issues, setIssues] = useState<IssueResponse[]>([]);
  const [role, setRole] = useState<Role | null>(null);
  const [members, setMembers] = useState<ProjectMemberResponse[]>([]);
  const [expandedId, setExpandedId] = useState<number | null>(null);
  // 상세 모달로 연 이슈. 카드 확장(expandedId)은 상태 변경용으로 그대로 둔다.
  const [detailId, setDetailId] = useState<number | null>(null);
  const [creating, setCreating] = useState(false);
  const [newTitle, setNewTitle] = useState("");
  const [createError, setCreateError] = useState<string | null>(null);
  const [statusError, setStatusError] = useState<string | null>(null);
  const [statusBusyId, setStatusBusyId] = useState<number | null>(null);
  // 휴지통 — 기본 목록은 includeTrashed=false라 trashed 이슈가 안 내려온다.
  // 복원하려면 따로 볼 수 있어야 해서 토글 + 별도 조회로 둔다(칸반 컬럼과 섞지 않는다).
  const [trashOpen, setTrashOpen] = useState(false);
  const [trashedIssues, setTrashedIssues] = useState<IssueResponse[]>([]);
  const [trashState, setTrashState] = useState<LoadState>("loading");
  const [trashError, setTrashError] = useState<string | null>(null);

  const loadBoard = useCallback(async () => {
    try {
      const [projectResponse, issuesResponse] = await Promise.all([
        projectService.detail(params.projectId),
        issueService.list(params.projectId),
      ]);
      setPageError(null);
      setRole(viewerRole(projectResponse.data.data.viewer));
      setIssues(issuesResponse.data.data.issues);
      setPageState("ready");
    } catch (error) {
      setPageError(invitationErrorMessage(error, "이슈 목록을 불러오지 못했습니다."));
      setPageState("error");
    }
  }, [params.projectId]);

  // 담당자 select용. 보드 본체와 분리해서 이것만 실패해도 화면이 죽지 않게 한다
  // (S12에서 나온 F-3 회귀와 같은 이유). 실패하면 담당자 목록만 비어 보인다.
  const loadMembers = useCallback(async () => {
    try {
      const { data } = await projectMemberService.list(params.projectId, { status: "ACTIVE" });
      setMembers(data.data.projectMembers);
    } catch {
      setMembers([]);
    }
  }, [params.projectId]);

  /**
   * 휴지통 목록. `includeTrashed=true`는 active와 trashed를 **함께** 주므로
   * (API-명세서 §7) trashed만 걸러낸다. 보드 본체와 분리해 이것만 실패해도
   * 칸반은 그대로 보이게 한다.
   */
  const loadTrashed = useCallback(async () => {
    setTrashState("loading");
    setTrashError(null);
    try {
      const { data } = await issueService.list(params.projectId, { includeTrashed: true });
      setTrashedIssues(data.data.issues.filter((issue) => issue.trashedAt !== null));
      setTrashState("ready");
    } catch (error) {
      setTrashError(invitationErrorMessage(error, "휴지통을 불러오지 못했습니다."));
      setTrashState("error");
    }
  }, [params.projectId]);

  useEffect(() => {
    (async () => {
      await Promise.all([loadBoard(), loadMembers()]);
    })();
  }, [loadBoard, loadMembers]);

  // 펼칠 때만 부른다 — 닫혀 있으면 굳이 조회하지 않는다.
  useEffect(() => {
    if (!trashOpen) return;
    (async () => {
      await loadTrashed();
    })();
  }, [trashOpen, loadTrashed]);

  const allowedStatuses = useMemo(() => (role ? statusOptionsFor(role) : []), [role]);
  // id로 들고 목록에서 찾는다 — 이슈 객체를 복사해두면 상태 변경 후 모달이 옛 값을 보여준다.
  // 휴지통에서 연 이슈는 issues(active)에 없으므로 trashedIssues도 함께 본다.
  const detailIssue = useMemo(
    () =>
      issues.find((issue) => issue.id === detailId) ??
      trashedIssues.find((issue) => issue.id === detailId) ??
      null,
    [issues, trashedIssues, detailId]
  );
  const sensors = useSensors(useSensor(PointerSensor, { activationConstraint: { distance: 8 } }));

  const changeStatus = async (issue: IssueResponse, nextStatus: IssueStatus) => {
    if (statusBusyId !== null || issue.status === nextStatus || !allowedStatuses.includes(nextStatus)) return;

    setStatusError(null);
    setStatusBusyId(issue.id);
    try {
      const { data } = await issueService.updateStatus(issue.id, { status: nextStatus });
      setIssues((current) => current.map((item) => (item.id === issue.id ? data.data.issue : item)));
    } catch (error) {
      setStatusError(invitationErrorMessage(error, "이슈 상태 변경에 실패했습니다."));
    } finally {
      setStatusBusyId(null);
    }
  };

  /**
   * 상세 모달의 수정/휴지통/복원 결과를 두 목록에 반영한다.
   * 칸반(active)과 휴지통은 서로 다른 조회라, trash/restore 때 이슈가 목록 사이를 옮겨간다.
   */
  const handleIssueUpdated = (updated: IssueResponse) => {
    const trashed = updated.trashedAt !== null;
    setIssues((current) =>
      trashed
        ? current.filter((item) => item.id !== updated.id)
        : current.some((item) => item.id === updated.id)
          ? current.map((item) => (item.id === updated.id ? updated : item))
          : // 복원된 이슈는 칸반에 없던 상태라 새로 넣는다.
            [...current, updated]
    );
    setTrashedIssues((current) =>
      trashed
        ? current.some((item) => item.id === updated.id)
          ? current.map((item) => (item.id === updated.id ? updated : item))
          : [...current, updated]
        : current.filter((item) => item.id !== updated.id)
    );
  };

  const handleDragEnd = (event: DragEndEvent) => {
    const { active, over } = event;
    if (!over) return;
    const issue = issues.find((item) => item.id === active.id);
    const nextStatus = COLUMNS.find((column) => column.status === over.id)?.status;
    if (issue && nextStatus) void changeStatus(issue, nextStatus);
  };

  const createIssue = async () => {
    const title = newTitle.trim();
    if (!title || creating) return;

    setCreateError(null);
    setCreating(true);
    try {
      const { data } = await issueService.create(params.projectId, { title });
      setIssues((current) => [...current, data.data.issue]);
      setNewTitle("");
    } catch (error) {
      setCreateError(invitationErrorMessage(error, "이슈 생성에 실패했습니다."));
    } finally {
      setCreating(false);
    }
  };

  if (pageState === "loading") {
    return <div className="px-6 py-8 text-sm text-muted">이슈 목록을 불러오는 중...</div>;
  }

  if (pageState === "error" || !role) {
    return (
      <div className="px-6 py-8">
        <Link
          href={`/${params.companyId}/projects`}
          className="text-sm text-muted transition-colors duration-fast hover:text-primary"
        >
          ← 프로젝트 목록
        </Link>
        <div className="mt-4 rounded-lg border border-destructive/30 bg-destructive-subtle px-4 py-6 text-center text-sm text-destructive">
          {pageError}
        </div>
      </div>
    );
  }

  return (
    <div className="px-6 py-8">
      <Link
        href={`/${params.companyId}/projects`}
        className="text-sm text-muted transition-colors duration-fast hover:text-primary"
      >
        ← 프로젝트 목록
      </Link>
      <div className="mt-3">
        <h1 className="text-xl font-bold text-foreground">칸반 보드</h1>
        <p className="mt-1 text-sm text-muted">프로젝트 {params.projectId}</p>
      </div>

      <section className="mt-4 rounded-xl border border-border bg-surface p-4">
        <label className="text-sm font-semibold text-foreground" htmlFor="new-issue-title">
          새 이슈
        </label>
        <div className="mt-2 flex flex-wrap gap-2">
          <input
            id="new-issue-title"
            aria-label="새 이슈 제목"
            value={newTitle}
            onChange={(event) => setNewTitle(event.target.value)}
            onKeyDown={(event) => event.key === "Enter" && void createIssue()}
            disabled={creating}
            placeholder="이슈 제목"
            className="min-w-48 flex-1 rounded-md border border-border bg-surface px-3 py-2 text-sm text-foreground placeholder:text-placeholder focus:border-ring focus:outline-none disabled:opacity-50"
          />
          <button
            onClick={() => void createIssue()}
            disabled={creating || !newTitle.trim()}
            className="rounded-md btn-gradient px-3 py-2 text-sm font-semibold text-primary-foreground disabled:cursor-not-allowed disabled:opacity-50"
          >
            {creating ? "추가 중..." : "이슈 추가"}
          </button>
        </div>
        {createError && <p className="mt-2 text-xs text-destructive">{createError}</p>}
      </section>

      {statusError && <p className="mt-4 text-sm text-destructive">{statusError}</p>}

      <DndContext id="kanban-board" sensors={sensors} onDragEnd={handleDragEnd}>
        <div className="mt-6 grid grid-cols-1 gap-4 md:grid-cols-5">
          {COLUMNS.map((column) => {
            const columnIssues = issues.filter((issue) => issue.status === column.status);
            const statusUnavailable = !allowedStatuses.includes(column.status);
            return (
              <div key={column.status} className="flex flex-col gap-2">
                <div className="flex items-center justify-between px-1">
                  <h2 className="text-sm font-semibold text-foreground">{column.label}</h2>
                  <span className="text-xs text-muted">{columnIssues.length}</span>
                </div>

                <DroppableColumn status={column.status} disabled={statusUnavailable || statusBusyId !== null}>
                  {columnIssues.map((issue) => {
                    const expanded = expandedId === issue.id;
                    const issueIsBusy = statusBusyId === issue.id;
                    return (
                      <DraggableIssueCard key={issue.id} id={issue.id} disabled={statusBusyId !== null}>
                        <button
                          onClick={() => setExpandedId(expanded ? null : issue.id)}
                          className="w-full text-left"
                        >
                          <span className="font-mono text-[10px] text-muted">{issue.issueKey}</span>
                          <p className="mt-1 text-sm font-medium text-foreground">{issue.title}</p>
                        </button>

                        {expanded && (
                          <div className="mt-3 flex flex-col gap-2 border-t border-border pt-3">
                            <button
                              type="button"
                              onClick={() => setDetailId(issue.id)}
                              className="self-start rounded-md border border-border px-2 py-1 text-xs font-medium text-foreground transition-colors duration-fast hover:bg-primary-subtle"
                            >
                              상세 열기
                            </button>
                            <label className="text-xs text-muted">
                              상태
                              <select
                                aria-label="상태 변경"
                                value={issue.status}
                                disabled={issueIsBusy}
                                onChange={(event) => void changeStatus(issue, event.target.value as IssueStatus)}
                                className="ml-1.5 rounded-md border border-border bg-surface px-1.5 py-1 text-xs text-foreground focus:border-ring focus:outline-none disabled:cursor-not-allowed disabled:opacity-50"
                              >
                                {allowedStatuses.map((status) => (
                                  <option key={status} value={status}>
                                    {COLUMNS.find((columnOption) => columnOption.status === status)?.label}
                                  </option>
                                ))}
                                {!allowedStatuses.includes(issue.status) && (
                                  <option value={issue.status}>
                                    {COLUMNS.find((columnOption) => columnOption.status === issue.status)?.label}
                                  </option>
                                )}
                              </select>
                            </label>
                          </div>
                        )}
                      </DraggableIssueCard>
                    );
                  })}
                </DroppableColumn>
              </div>
            );
          })}
        </div>
      </DndContext>

      {/* 휴지통 — 기본 목록에 안 나오는 trashed 이슈를 열어보고 복원하는 유일한 경로다(ISSUE-04) */}
      <section className="mt-6 rounded-xl border border-border bg-surface p-4">
        <button
          type="button"
          onClick={() => setTrashOpen((open) => !open)}
          aria-expanded={trashOpen}
          className="flex w-full items-center justify-between text-left"
        >
          <span className="text-sm font-semibold text-foreground">
            휴지통{trashOpen && trashState === "ready" && ` (${trashedIssues.length})`}
          </span>
          <span className="text-xs text-muted">{trashOpen ? "접기" : "펼치기"}</span>
        </button>

        {trashOpen && (
          <div className="mt-3">
            {trashState === "loading" && <p className="text-xs text-muted">불러오는 중...</p>}
            {trashState === "error" && <p className="text-xs text-destructive">{trashError}</p>}
            {trashState === "ready" && (
              <ul className="divide-y divide-border">
                {trashedIssues.map((issue) => (
                  <li key={issue.id} className="flex items-center justify-between gap-2 py-2.5">
                    <span className="min-w-0 truncate text-sm text-foreground">
                      <span className="mr-2 font-mono text-xs text-muted">{issue.issueKey}</span>
                      {issue.title}
                    </span>
                    <button
                      type="button"
                      onClick={() => setDetailId(issue.id)}
                      className="shrink-0 rounded-md border border-border px-2 py-1 text-xs font-medium text-foreground transition-colors duration-fast hover:bg-primary-subtle"
                    >
                      상세 열기
                    </button>
                  </li>
                ))}
                {trashedIssues.length === 0 && (
                  <li className="py-2.5 text-center text-xs text-muted">휴지통이 비어 있습니다.</li>
                )}
              </ul>
            )}
          </div>
        )}
      </section>

      <IssueDetailModal
        issue={detailIssue}
        canManage={role !== null && role !== "A"}
        isReporter={detailIssue?.reporterUserId != null && detailIssue.reporterUserId === user?.id}
        members={members}
        onClose={() => setDetailId(null)}
        onUpdated={handleIssueUpdated}
      />
    </div>
  );
}
