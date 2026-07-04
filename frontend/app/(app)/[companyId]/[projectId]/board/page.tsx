"use client";

/*
 * S05 — 칸반 보드
 *
 * 접근 사용자: A/P/C. 화면정의서(docs/03-design/화면정의서.md §S05) 기준
 * 5개 고정 컬럼(백로그/할일/진행중/검토중/완료), 이슈 카드(담당자/우선순위/
 * 마감일), 이슈 생성, 카드 클릭 시 인라인 상세(설명/필드 수정), 상태 변경,
 * 휴지통 이동/복원을 구현한다.
 *
 * 역할별 제약 (화면정의서 §S05 "역할별 상태 이동 권한" + API 매핑 메모):
 *   - A: 상태 변경은 BACKLOG→TODO→IN_PROGRESS→IN_REVIEW 범위만(DONE 불가),
 *     수정은 title/description만 가능(담당자/우선순위/마감일은 P/C만).
 *   - P/C: 상태 전체(DONE 포함) 변경 가능, 모든 필드 수정 가능.
 *   - 휴지통 이동은 P/C 또는 본인이 등록한 이슈만 가능.
 *
 * 카드 이동은 @dnd-kit(core+utilities)로 드래그앤드롭 구현. 컬럼 간 이동은
 * 드래그로, 접근성이 필요한 경우(키보드/스크린리더)를 위해 카드 상세의
 * "상태 변경" select도 그대로 남겨뒀다 — 둘 다 같은 updateStatus 경로를 탄다.
 * 드래그 판정 오차(distance: 8px) 이하 클릭은 드래그로 취급하지 않아 카드
 * 클릭→상세 펼치기와 충돌하지 않는다.
 *
 * ⚠️ MOCK: 이슈 API(`GET/POST /projects/{projectId}/issues`,
 * `PATCH /issues/{issueId}(/status)`, `POST /issues/{issueId}/trash`)가
 * 아직 없어서 로컬 state로 흉내낸다. role/현재 사용자도 HubOverlay와 동일하게
 * 상수로 고정.
 */
import { useMemo, useState } from "react";
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

type Role = "A" | "P" | "C";
type IssueStatus = "BACKLOG" | "TODO" | "IN_PROGRESS" | "IN_REVIEW" | "DONE";
type Priority = "LOW" | "MEDIUM" | "HIGH" | "URGENT";

type Issue = {
  id: string;
  issueKey: string;
  title: string;
  description: string;
  status: IssueStatus;
  priority: Priority;
  assigneeName: string | null;
  dueDate: string | null;
  reporterName: string;
  trashedAt: string | null;
};

// TODO(Sprint 2): /api/v1/auth/me 연동 후 실제 로그인 사용자로 교체 (HubOverlay와 동일)
const MOCK_ROLE: Role = "C";
const MOCK_CURRENT_USER = "정철진";

const COLUMNS: { status: IssueStatus; label: string }[] = [
  { status: "BACKLOG", label: "백로그" },
  { status: "TODO", label: "할일" },
  { status: "IN_PROGRESS", label: "진행중" },
  { status: "IN_REVIEW", label: "검토중" },
  { status: "DONE", label: "완료" },
];

// A는 DONE으로/DONE에서 이동 불가(허용 범위 4개만)
const A_ALLOWED_STATUSES: IssueStatus[] = ["BACKLOG", "TODO", "IN_PROGRESS", "IN_REVIEW"];

const PRIORITY_LABEL: Record<Priority, string> = {
  LOW: "낮음",
  MEDIUM: "보통",
  HIGH: "높음",
  URGENT: "긴급",
};

const PRIORITY_CLASS: Record<Priority, string> = {
  LOW: "bg-border text-muted",
  MEDIUM: "bg-accent-subtle text-accent",
  HIGH: "bg-warning/15 text-warning",
  URGENT: "bg-destructive-subtle text-destructive",
};

const MOCK_ISSUES: Issue[] = [
  { id: "i1", issueKey: "THR-101", title: "로그인 화면 반응형 깨짐", description: "모바일 너비에서 카드가 잘림", status: "IN_REVIEW", priority: "HIGH", assigneeName: "김민정", dueDate: "2026-07-05", reporterName: "정철진", trashedAt: null },
  { id: "i2", issueKey: "THR-102", title: "허브 상단바 알림 뱃지 API 연동", description: "", status: "TODO", priority: "MEDIUM", assigneeName: "여도현", dueDate: "2026-07-10", reporterName: "정철진", trashedAt: null },
  { id: "i3", issueKey: "THR-103", title: "회사 로고 업로드 S3 연동", description: "", status: "BACKLOG", priority: "LOW", assigneeName: null, dueDate: null, reporterName: "황스일", trashedAt: null },
  { id: "i4", issueKey: "THR-104", title: "칸반 보드 성능 프로파일링", description: "이슈 200개 기준 렌더 측정", status: "IN_PROGRESS", priority: "URGENT", assigneeName: "정철진", dueDate: "2026-07-04", reporterName: "정철진", trashedAt: null },
  { id: "i5", issueKey: "THR-105", title: "회원가입 이메일 인증 플로우", description: "", status: "DONE", priority: "MEDIUM", assigneeName: "김민정", dueDate: null, reporterName: "김민정", trashedAt: null },
];

function statusOptionsFor(role: Role): IssueStatus[] {
  return role === "A" ? A_ALLOWED_STATUSES : COLUMNS.map((c) => c.status);
}

// 컬럼 = 드롭 대상. isOver일 때 배경을 살짝 강조해 놓을 자리를 표시한다.
function DroppableColumn({ status, children }: { status: IssueStatus; children: React.ReactNode }) {
  const { setNodeRef, isOver } = useDroppable({ id: status });
  return (
    <div
      ref={setNodeRef}
      className={
        "flex flex-col gap-2 rounded-xl p-2 min-h-24 transition-colors duration-fast " +
        (isOver ? "bg-primary-subtle" : "bg-border/30")
      }
    >
      {children}
    </div>
  );
}

// 카드 = 드래그 대상. distance:8px 이하 움직임은 드래그로 안 잡혀서(BoardPage의
// PointerSensor 설정) 내부 버튼 클릭(상세 펼치기)과 충돌하지 않는다.
// transform은 드래그 중 실시간 픽셀 좌표라 디자인 토큰으로 표현할 수 없는
// 유일한 예외로 인라인 style을 쓴다(@dnd-kit 공식 패턴).
function DraggableIssueCard({ id, children }: { id: string; children: React.ReactNode }) {
  const { attributes, listeners, setNodeRef, transform, isDragging } = useDraggable({ id });
  return (
    <div
      ref={setNodeRef}
      {...listeners}
      {...attributes}
      style={{ transform: transform ? CSS.Translate.toString(transform) : undefined }}
      className={
        "rounded-lg bg-surface p-3 shadow-sm cursor-grab active:cursor-grabbing " +
        (isDragging ? "opacity-50 z-raised" : "")
      }
    >
      {children}
    </div>
  );
}

export default function BoardPage() {
  const params = useParams<{ companyId: string; projectId: string }>();

  const [issues, setIssues] = useState<Issue[]>(MOCK_ISSUES);
  const [expandedId, setExpandedId] = useState<string | null>(null);
  const [showTrash, setShowTrash] = useState(false);
  const [creatingInColumn, setCreatingInColumn] = useState<IssueStatus | null>(null);
  const [newTitle, setNewTitle] = useState("");

  const activeIssues = useMemo(() => issues.filter((i) => !i.trashedAt), [issues]);
  const trashedIssues = useMemo(() => issues.filter((i) => i.trashedAt), [issues]);

  const canEditFullFields = MOCK_ROLE === "P" || MOCK_ROLE === "C";
  const allowedStatuses = statusOptionsFor(MOCK_ROLE);

  const canTrash = (issue: Issue) => canEditFullFields || issue.reporterName === MOCK_CURRENT_USER;

  // 클릭(카드 펼치기)과 드래그를 구분하기 위해 8px 이상 움직여야 드래그로 인식한다.
  const sensors = useSensors(useSensor(PointerSensor, { activationConstraint: { distance: 8 } }));

  const updateIssue = (id: string, patch: Partial<Issue>) => {
    // TODO: PATCH /api/v1/issues/{issueId} 또는 PATCH .../status
    setIssues((prev) => prev.map((i) => (i.id === id ? { ...i, ...patch } : i)));
  };

  // select와 드래그앤드롭 둘 다 이 경로로 상태를 바꿔서, 역할별 허용 범위(allowedStatuses)를
  // 한 곳에서만 검증한다. 허용 안 되는 컬럼에 드롭하면 그냥 무시(카드는 원래 자리로 튕겨나감).
  const changeStatus = (id: string, nextStatus: IssueStatus) => {
    if (!allowedStatuses.includes(nextStatus)) return;
    updateIssue(id, { status: nextStatus });
  };

  const handleDragEnd = (event: DragEndEvent) => {
    const { active, over } = event;
    if (!over) return;
    changeStatus(String(active.id), over.id as IssueStatus);
  };

  const trashIssue = (id: string) => {
    // TODO: POST /api/v1/issues/{issueId}/trash
    setIssues((prev) =>
      prev.map((i) => (i.id === id ? { ...i, trashedAt: new Date().toISOString() } : i))
    );
    setExpandedId(null);
  };

  const restoreIssue = (id: string) => {
    // TODO: POST /api/v1/issues/{issueId}/restore
    setIssues((prev) => prev.map((i) => (i.id === id ? { ...i, trashedAt: null } : i)));
  };

  const createIssue = (status: IssueStatus) => {
    const title = newTitle.trim();
    if (!title) return;
    // TODO: POST /api/v1/projects/{projectId}/issues
    setIssues((prev) => [
      ...prev,
      {
        id: crypto.randomUUID(),
        issueKey: `THR-${100 + prev.length + 1}`,
        title,
        description: "",
        status,
        priority: "MEDIUM",
        assigneeName: null,
        dueDate: null,
        reporterName: MOCK_CURRENT_USER,
        trashedAt: null,
      },
    ]);
    setNewTitle("");
    setCreatingInColumn(null);
  };

  return (
    <div className="px-6 py-8">
      <Link
        href={`/${params.companyId}/projects`}
        className="text-sm text-muted transition-colors duration-fast hover:text-primary"
      >
        ← 프로젝트 목록
      </Link>
      <div className="mt-3 flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-foreground">칸반 보드</h1>
          <p className="mt-1 text-sm text-muted">프로젝트 {params.projectId}</p>
        </div>
        <button
          onClick={() => setShowTrash((v) => !v)}
          className="rounded-md px-3 py-1.5 text-sm font-medium text-muted transition-colors duration-fast hover:bg-primary-subtle"
        >
          휴지통 ({trashedIssues.length})
        </button>
      </div>

      {showTrash && (
        <section className="mt-4 rounded-xl border border-border bg-surface p-5">
          <h2 className="text-sm font-semibold text-foreground">휴지통</h2>
          <ul className="mt-3 divide-y divide-border">
            {trashedIssues.map((issue) => (
              <li
                key={issue.id}
                className="flex items-center justify-between px-1 py-2.5"
              >
                <span className="text-sm text-foreground">
                  <span className="mr-2 font-mono text-xs text-muted">{issue.issueKey}</span>
                  {issue.title}
                </span>
                <button
                  onClick={() => restoreIssue(issue.id)}
                  className="text-xs font-medium text-primary transition-colors duration-fast hover:opacity-70"
                >
                  복원
                </button>
              </li>
            ))}
            {trashedIssues.length === 0 && (
              <li className="text-center text-sm text-muted">휴지통이 비어 있습니다.</li>
            )}
          </ul>
        </section>
      )}

      {/* id 고정: @dnd-kit이 aria-describedby용 id를 자동 생성하는데, 고정 안 하면
          SSR/CSR 렌더 사이 카운터가 어긋나 하이드레이션 경고가 난다. */}
      <DndContext id="kanban-board" sensors={sensors} onDragEnd={handleDragEnd}>
      <div className="mt-6 grid grid-cols-1 gap-4 md:grid-cols-5">
        {COLUMNS.map((col) => {
          const columnIssues = activeIssues.filter((i) => i.status === col.status);
          return (
            <div key={col.status} className="flex flex-col gap-2">
              <div className="flex items-center justify-between px-1">
                <h2 className="text-sm font-semibold text-foreground">{col.label}</h2>
                <span className="text-xs text-muted">{columnIssues.length}</span>
              </div>

              <DroppableColumn status={col.status}>
                {columnIssues.map((issue) => {
                  const expanded = expandedId === issue.id;
                  return (
                    <DraggableIssueCard key={issue.id} id={issue.id}>
                      <button
                        onClick={() => setExpandedId(expanded ? null : issue.id)}
                        className="w-full text-left"
                      >
                        <div className="flex items-center justify-between">
                          <span className="font-mono text-[10px] text-muted">{issue.issueKey}</span>
                          <span className={`rounded-full px-2 py-0.5 text-[10px] font-medium ${PRIORITY_CLASS[issue.priority]}`}>
                            {PRIORITY_LABEL[issue.priority]}
                          </span>
                        </div>
                        <p className="mt-1 text-sm font-medium text-foreground">{issue.title}</p>
                        <div className="mt-1.5 flex items-center justify-between text-xs text-muted">
                          <span>{issue.assigneeName ?? "미배정"}</span>
                          <span>{issue.dueDate ?? ""}</span>
                        </div>
                      </button>

                      {expanded && (
                        <div className="mt-3 flex flex-col gap-2 border-t border-border pt-3">
                          <textarea
                            aria-label="설명"
                            value={issue.description}
                            onChange={(e) => updateIssue(issue.id, { description: e.target.value })}
                            placeholder="설명 없음"
                            rows={2}
                            className="w-full resize-none rounded-md border border-border bg-surface px-2 py-1.5 text-xs text-foreground placeholder:text-placeholder focus:border-ring focus:outline-none"
                          />

                          <div className="flex flex-wrap items-center gap-2">
                            <label className="text-xs text-muted">
                              상태
                              <select
                                aria-label="상태 변경"
                                value={issue.status}
                                onChange={(e) => changeStatus(issue.id, e.target.value as IssueStatus)}
                                className="ml-1.5 rounded-md border border-border bg-surface px-1.5 py-1 text-xs text-foreground focus:border-ring focus:outline-none"
                              >
                                {allowedStatuses.map((s) => (
                                  <option key={s} value={s}>
                                    {COLUMNS.find((c) => c.status === s)?.label}
                                  </option>
                                ))}
                                {/* 현재 상태가 이 역할의 허용 범위 밖이면(A가 DONE 이슈를 보는 경우) 표시만 유지 */}
                                {!allowedStatuses.includes(issue.status) && (
                                  <option value={issue.status}>
                                    {COLUMNS.find((c) => c.status === issue.status)?.label}
                                  </option>
                                )}
                              </select>
                            </label>

                            {canEditFullFields && (
                              <>
                                <input
                                  aria-label="담당자"
                                  value={issue.assigneeName ?? ""}
                                  onChange={(e) => updateIssue(issue.id, { assigneeName: e.target.value || null })}
                                  placeholder="담당자"
                                  className="w-20 rounded-md border border-border bg-surface px-1.5 py-1 text-xs text-foreground placeholder:text-placeholder focus:border-ring focus:outline-none"
                                />
                                <select
                                  aria-label="우선순위"
                                  value={issue.priority}
                                  onChange={(e) => updateIssue(issue.id, { priority: e.target.value as Priority })}
                                  className="rounded-md border border-border bg-surface px-1.5 py-1 text-xs text-foreground focus:border-ring focus:outline-none"
                                >
                                  {(Object.keys(PRIORITY_LABEL) as Priority[]).map((p) => (
                                    <option key={p} value={p}>
                                      {PRIORITY_LABEL[p]}
                                    </option>
                                  ))}
                                </select>
                                <input
                                  aria-label="마감일"
                                  type="date"
                                  value={issue.dueDate ?? ""}
                                  onChange={(e) => updateIssue(issue.id, { dueDate: e.target.value || null })}
                                  className="rounded-md border border-border bg-surface px-1.5 py-1 text-xs text-foreground focus:border-ring focus:outline-none"
                                />
                              </>
                            )}
                          </div>

                          {canTrash(issue) && (
                            <button
                              onClick={() => trashIssue(issue.id)}
                              className="self-end text-xs font-medium text-destructive transition-colors duration-fast hover:opacity-70"
                            >
                              휴지통으로 이동
                            </button>
                          )}
                        </div>
                      )}
                    </DraggableIssueCard>
                  );
                })}

                {creatingInColumn === col.status ? (
                  <div className="flex flex-col gap-1.5 rounded-lg border border-dashed border-primary bg-surface p-2">
                    <input
                      aria-label="새 이슈 제목"
                      autoFocus
                      value={newTitle}
                      onChange={(e) => setNewTitle(e.target.value)}
                      onKeyDown={(e) => e.key === "Enter" && createIssue(col.status)}
                      placeholder="이슈 제목"
                      className="w-full rounded-md border border-border bg-surface px-2 py-1 text-xs text-foreground placeholder:text-placeholder focus:border-ring focus:outline-none"
                    />
                    <div className="flex gap-1.5">
                      <button
                        onClick={() => createIssue(col.status)}
                        className="rounded-md bg-primary px-2 py-1 text-xs font-semibold text-primary-foreground hover:bg-primary-hover"
                      >
                        추가
                      </button>
                      <button
                        onClick={() => setCreatingInColumn(null)}
                        className="rounded-md px-2 py-1 text-xs text-muted hover:bg-primary-subtle"
                      >
                        취소
                      </button>
                    </div>
                  </div>
                ) : (
                  <button
                    onClick={() => {
                      setCreatingInColumn(col.status);
                      setNewTitle("");
                    }}
                    className="rounded-lg px-2 py-1.5 text-left text-xs text-muted transition-colors duration-fast hover:bg-primary-subtle hover:text-primary"
                  >
                    + 이슈 추가
                  </button>
                )}
              </DroppableColumn>
            </div>
          );
        })}
      </div>
      </DndContext>
    </div>
  );
}
