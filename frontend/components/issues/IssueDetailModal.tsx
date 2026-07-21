"use client";

/*
 * IssueDetailModal — components/issues/IssueDetailModal.tsx
 *
 * S05 칸반 보드의 "이슈 상세 열기"(화면정의서 §S05 주요 액션). 이슈 상세 조회/수정
 * (ISSUE-02/03)과 휴지통/복원(ISSUE-04)을 한 곳에서 처리한다. WBS 4.4.2.
 *
 * 칸반 카드 안에 폼을 넣지 않고 모달로 뺀 이유: 카드는 draggable이라 입력 요소와
 * 포인터 이벤트가 얽히고, 5열 그리드 안에서는 폼을 놓을 폭이 안 나온다.
 *
 * 서버 계약(API-명세서 §7, §13.6 / backend IssueService):
 *   - PATCH는 **보낸 필드만** 반영한다(controller가 request body의 key 집합으로 판정).
 *     허용되지 않은 필드를 담으면 400이라 바꾼 필드만 보내야 한다.
 *   - 명시적 null은 description/assigneeUserId/dueDate를 clear한다.
 *   - A는 title/description만 수정할 수 있다 — 그 외 필드를 담으면 403이라,
 *     화면에서도 A에게는 나머지를 읽기 전용으로 둔다.
 *   - trash/restore는 P/C 또는 **등록자**에게 허용된다. 등록자 판정에 reporterUserId가
 *     필요해서 이번에 응답에 추가했다.
 *   - trashed 이슈의 상태/내용 변경은 409라 복원 전에는 폼을 잠근다.
 *
 * 연결 문서(화면정의서 §S05 "주요 데이터: 연결 문서" / "주요 액션: 관련 문서
 * 연결/조회"): 문서 쪽 화면(`docs/[docId]/page.tsx`)엔 문서→이슈 연결/해제가
 * 이미 있었지만 반대 방향(이슈 상세에서 연결 문서 조회)이 빠져 있었다 — 코드
 * vs 화면정의서 감사 중 발견. 백엔드(`GET /issues/{issueId}/documents`)와
 * 프론트 서비스 메서드(`documentService.documentsOfIssue`)는 이미 있었는데
 * 호출하는 곳이 없었다. docs 페이지와 같은 패턴(연결 해제는 linkId가 필요해서
 * 프로젝트 전체 링크 목록에서 documentId→linkId 매핑을 만들어 둔다)으로
 * 이 모달에도 추가한다.
 *
 * 구조: 열림 판정을 바깥에서 하고 폼 본체는 열렸을 때만 마운트한다 —
 * 닫으면 언마운트되므로 다시 열 때 defaultValues로 자연히 초기화된다
 * (effect로 reset하지 않아도 된다). ProfileEditModal과 같은 패턴이다.
 *
 * 스타일 규칙: style={{}} 금지, @theme 시멘틱 토큰 클래스만 사용.
 */
import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Button } from "@/components/ui/Button";
import { invitationErrorMessage } from "@/lib/invitation";
import { documentService } from "@/services/document.service";
import { issueService } from "@/services/issue.service";
import type { DocumentResponse } from "@/types/document.types";
import type { IssuePriority, IssueResponse, IssueUpdateRequest } from "@/types/issue.types";
import type { ProjectMemberResponse } from "@/types/project-member.types";

const PRIORITY_LABEL: Record<IssuePriority, string> = {
  LOW: "낮음",
  MEDIUM: "보통",
  HIGH: "높음",
  URGENT: "긴급",
};

// title은 서버 검증(필수, trim 후 200자)과 맞춘다. 나머지는 서버가 형식을 강제하지 않는다.
const issueSchema = z.object({
  title: z.string().trim().min(1, "제목은 필수입니다").max(200, "제목은 200자 이하여야 합니다"),
  description: z.string(),
  priority: z.enum(["LOW", "MEDIUM", "HIGH", "URGENT"]),
  assigneeUserId: z.string(),
  dueDate: z.string(),
});

type IssueFormValues = z.infer<typeof issueSchema>;

function IssueDetailDialog({
  issue,
  canManage,
  isReporter,
  members,
  onClose,
  onUpdated,
}: {
  issue: IssueResponse;
  canManage: boolean;
  isReporter: boolean;
  members: ProjectMemberResponse[];
  onClose: () => void;
  onUpdated: (issue: IssueResponse) => void;
}) {
  const { companyId, projectId } = useParams<{ companyId: string; projectId: string }>();
  const [error, setError] = useState<string | null>(null);
  const [trashBusy, setTrashBusy] = useState(false);
  const dialogRef = useRef<HTMLDivElement>(null);
  const trashed = issue.trashedAt !== null;
  // 서버 규칙 그대로 — P/C 또는 등록자.
  const canTrash = canManage || isReporter;

  const [linkedDocuments, setLinkedDocuments] = useState<DocumentResponse[]>([]);
  const [linkIdByDocumentId, setLinkIdByDocumentId] = useState<Record<number, number>>({});
  const [projectDocuments, setProjectDocuments] = useState<DocumentResponse[]>([]);
  const [pickedDocumentId, setPickedDocumentId] = useState("");
  const [docLinkError, setDocLinkError] = useState<string | null>(null);
  const [docLinkBusy, setDocLinkBusy] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<IssueFormValues>({
    resolver: zodResolver(issueSchema),
    defaultValues: {
      title: issue.title,
      description: issue.description ?? "",
      priority: issue.priority,
      assigneeUserId: issue.assigneeUserId === null ? "" : String(issue.assigneeUserId),
      dueDate: issue.dueDate ?? "",
    },
  });

  // Esc로 닫기 + focus 관리(ProfileEditModal과 동일). 열 때 안으로 옮기고, Tab을
  // 모달 안에 가두고, 닫을 때 trigger로 되돌린다.
  useEffect(() => {
    const trigger = document.activeElement as HTMLElement | null;
    dialogRef.current?.querySelector<HTMLElement>("input, textarea")?.focus();

    const focusables = () =>
      Array.from(
        dialogRef.current?.querySelectorAll<HTMLElement>(
          "button, input, textarea, select, [href], [tabindex]:not([tabindex='-1'])",
        ) ?? [],
      ).filter((el) => !el.hasAttribute("disabled"));

    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        onClose();
        return;
      }
      if (e.key !== "Tab") return;
      const els = focusables();
      if (els.length === 0) return;
      const first = els[0];
      const last = els[els.length - 1];
      const active = document.activeElement;
      if (!dialogRef.current?.contains(active)) {
        e.preventDefault();
        (e.shiftKey ? last : first).focus();
        return;
      }
      if (!e.shiftKey && active === last) {
        e.preventDefault();
        first.focus();
      } else if (e.shiftKey && active === first) {
        e.preventDefault();
        last.focus();
      }
    };

    window.addEventListener("keydown", onKey);
    return () => {
      window.removeEventListener("keydown", onKey);
      trigger?.focus?.();
    };
  }, [onClose]);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const [documentsOfIssueRes, linksRes, projectDocumentsRes] = await Promise.all([
          documentService.documentsOfIssue(issue.id),
          documentService.listLinks(projectId),
          documentService.list(projectId),
        ]);
        if (cancelled) return;
        setLinkedDocuments(documentsOfIssueRes.data.data.documents);
        const map: Record<number, number> = {};
        for (const link of linksRes.data.data.links) {
          if (link.issueId === issue.id) map[link.documentId] = link.id;
        }
        setLinkIdByDocumentId(map);
        setProjectDocuments(projectDocumentsRes.data.data.documents);
      } catch {
        if (!cancelled) setDocLinkError("연결 문서를 불러오지 못했습니다.");
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [issue.id, projectId]);

  const unlinkedProjectDocuments = projectDocuments.filter(
    (doc) => doc.trashedAt === null && !linkedDocuments.some((linked) => linked.id === doc.id)
  );

  const linkDocument = async () => {
    const documentId = Number(pickedDocumentId);
    if (!documentId || docLinkBusy) return;
    setDocLinkError(null);
    setDocLinkBusy(true);
    try {
      await documentService.createLink(projectId, { documentId, issueId: issue.id });
      const doc = projectDocuments.find((item) => item.id === documentId);
      if (doc) setLinkedDocuments((prev) => [...prev, doc]);
      const linksResponse = await documentService.listLinks(projectId);
      const map: Record<number, number> = {};
      for (const link of linksResponse.data.data.links) {
        if (link.issueId === issue.id) map[link.documentId] = link.id;
      }
      setLinkIdByDocumentId(map);
      setPickedDocumentId("");
    } catch (err) {
      setDocLinkError(invitationErrorMessage(err, "문서 연결에 실패했습니다."));
    } finally {
      setDocLinkBusy(false);
    }
  };

  const unlinkDocument = async (documentId: number) => {
    const linkId = linkIdByDocumentId[documentId];
    if (!linkId || docLinkBusy) return;
    setDocLinkError(null);
    setDocLinkBusy(true);
    try {
      await documentService.deleteLink(linkId);
      setLinkedDocuments((prev) => prev.filter((doc) => doc.id !== documentId));
      setLinkIdByDocumentId((prev) => {
        const next = { ...prev };
        delete next[documentId];
        return next;
      });
    } catch (err) {
      setDocLinkError(invitationErrorMessage(err, "문서 연결 해제에 실패했습니다."));
    } finally {
      setDocLinkBusy(false);
    }
  };

  const onSubmit = async (values: IssueFormValues) => {
    setError(null);

    // 바뀐 필드만 담는다(위 계약 주석 참고).
    const body: IssueUpdateRequest = {};
    if (values.title !== issue.title) body.title = values.title;

    const nextDescription = values.description === "" ? null : values.description;
    if (nextDescription !== (issue.description ?? null)) body.description = nextDescription;

    // A는 아래 필드를 담기만 해도 403이라 아예 보내지 않는다.
    if (canManage) {
      if (values.priority !== issue.priority) body.priority = values.priority;

      const nextAssignee = values.assigneeUserId === "" ? null : Number(values.assigneeUserId);
      if (nextAssignee !== (issue.assigneeUserId ?? null)) body.assigneeUserId = nextAssignee;

      const nextDueDate = values.dueDate === "" ? null : values.dueDate;
      if (nextDueDate !== (issue.dueDate ?? null)) body.dueDate = nextDueDate;
    }

    if (Object.keys(body).length === 0) {
      onClose();
      return;
    }

    try {
      const { data } = await issueService.update(issue.id, body);
      onUpdated(data.data.issue);
      onClose();
    } catch (err) {
      setError(invitationErrorMessage(err, "이슈 수정에 실패했습니다."));
    }
  };

  const toggleTrash = async () => {
    setError(null);
    setTrashBusy(true);
    try {
      const { data } = trashed
        ? await issueService.restore(issue.id)
        : await issueService.trash(issue.id);
      onUpdated(data.data.issue);
      onClose();
    } catch (err) {
      setError(
        invitationErrorMessage(err, trashed ? "이슈 복원에 실패했습니다." : "휴지통 이동에 실패했습니다.")
      );
    } finally {
      setTrashBusy(false);
    }
  };

  return (
    <div className="fixed inset-0 z-modal flex items-center justify-center px-4">
      <button
        type="button"
        aria-label="닫기"
        onClick={onClose}
        className="absolute inset-0 cursor-default bg-overlay"
      />

      <div
        ref={dialogRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby="issue-modal-title"
        className="relative max-h-[85vh] w-full max-w-[520px] overflow-y-auto rounded-xl bg-surface p-6 shadow-xl animate-enter"
      >
        <div className="flex items-start justify-between gap-3">
          <div className="min-w-0">
            <span className="font-mono text-xs text-muted">{issue.issueKey}</span>
            <h2 id="issue-modal-title" className="text-lg font-bold text-foreground">
              이슈 상세
            </h2>
          </div>
          {trashed && (
            <span className="shrink-0 rounded-full bg-destructive-subtle px-2.5 py-0.5 text-xs font-medium text-destructive">
              휴지통
            </span>
          )}
        </div>

        {/* trashed 이슈는 서버가 상태/내용 변경을 409로 막는다 — 복원 먼저 하도록 안내한다 */}
        {trashed && (
          <p className="mt-3 rounded-md bg-destructive-subtle px-3 py-2 text-xs text-destructive">
            휴지통에 있는 이슈는 수정할 수 없습니다. 복원 후 수정해 주세요.
          </p>
        )}

        {error && <p className="mt-3 text-sm text-destructive">{error}</p>}

        <form onSubmit={handleSubmit(onSubmit)} className="mt-4 flex flex-col gap-4" noValidate>
          <label className="flex flex-col gap-1 text-xs font-medium text-muted">
            제목
            <input
              {...register("title")}
              disabled={trashed}
              className="rounded-md border border-border bg-surface px-3 py-2 text-sm text-foreground focus:border-ring focus:outline-none disabled:cursor-not-allowed disabled:opacity-60"
            />
            {errors.title && <span className="text-xs text-destructive">{errors.title.message}</span>}
          </label>

          <label className="flex flex-col gap-1 text-xs font-medium text-muted">
            설명
            <textarea
              {...register("description")}
              rows={4}
              disabled={trashed}
              placeholder="이슈 설명"
              className="resize-y rounded-md border border-border bg-surface px-3 py-2 text-sm text-foreground placeholder:text-placeholder focus:border-ring focus:outline-none disabled:cursor-not-allowed disabled:opacity-60"
            />
          </label>

          {/* A는 아래 세 필드를 수정할 수 없다(서버가 403) — 읽기 전용으로 둔다 */}
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
            <label className="flex flex-col gap-1 text-xs font-medium text-muted">
              우선순위
              <select
                {...register("priority")}
                disabled={!canManage || trashed}
                className="rounded-md border border-border bg-surface px-2 py-2 text-sm text-foreground focus:border-ring focus:outline-none disabled:cursor-not-allowed disabled:opacity-60"
              >
                {(Object.keys(PRIORITY_LABEL) as IssuePriority[]).map((priority) => (
                  <option key={priority} value={priority}>
                    {PRIORITY_LABEL[priority]}
                  </option>
                ))}
              </select>
            </label>

            <label className="flex flex-col gap-1 text-xs font-medium text-muted">
              담당자
              <select
                {...register("assigneeUserId")}
                disabled={!canManage || trashed}
                className="rounded-md border border-border bg-surface px-2 py-2 text-sm text-foreground focus:border-ring focus:outline-none disabled:cursor-not-allowed disabled:opacity-60"
              >
                <option value="">미지정</option>
                {/* 담당자는 ACTIVE 프로젝트 멤버여야 한다(서버 검증) */}
                {members.map((member) => (
                  <option key={member.id} value={String(member.user.id)}>
                    {member.user.nickname}
                  </option>
                ))}
              </select>
            </label>

            <label className="flex flex-col gap-1 text-xs font-medium text-muted">
              마감일
              <input
                type="date"
                {...register("dueDate")}
                disabled={!canManage || trashed}
                className="rounded-md border border-border bg-surface px-2 py-2 text-sm text-foreground focus:border-ring focus:outline-none disabled:cursor-not-allowed disabled:opacity-60"
              />
            </label>
          </div>

          {!canManage && !trashed && (
            <p className="text-xs text-muted">
              우선순위·담당자·마감일은 프로젝트 관리자만 변경할 수 있습니다.
            </p>
          )}

          <div className="flex flex-col gap-2 border-t border-border pt-3">
            <p className="text-xs font-medium text-muted">연결 문서</p>
            {docLinkError && <p className="text-xs text-destructive">{docLinkError}</p>}
            {linkedDocuments.length > 0 && (
              <ul className="flex flex-col gap-1">
                {linkedDocuments.map((doc) => (
                  <li key={doc.id} className="flex items-center justify-between gap-2">
                    <Link
                      href={`/${companyId}/${projectId}/docs/${doc.id}`}
                      className="min-w-0 truncate text-sm text-primary hover:underline"
                    >
                      {doc.title}
                    </Link>
                    {!trashed && (
                      <button
                        type="button"
                        onClick={() => void unlinkDocument(doc.id)}
                        disabled={docLinkBusy}
                        className="shrink-0 text-xs text-muted transition-colors duration-fast hover:text-destructive disabled:opacity-50"
                      >
                        연결 해제
                      </button>
                    )}
                  </li>
                ))}
              </ul>
            )}
            {linkedDocuments.length === 0 && (
              <p className="text-xs text-placeholder">연결된 문서가 없습니다.</p>
            )}
            {!trashed && (
              <div className="flex gap-2">
                <select
                  aria-label="연결할 문서"
                  value={pickedDocumentId}
                  onChange={(e) => setPickedDocumentId(e.target.value)}
                  disabled={docLinkBusy}
                  className="min-w-0 flex-1 rounded-md border border-border bg-surface px-2 py-1.5 text-xs text-foreground focus:border-ring focus:outline-none disabled:opacity-60"
                >
                  <option value="">문서 선택</option>
                  {unlinkedProjectDocuments.map((doc) => (
                    <option key={doc.id} value={doc.id}>
                      {doc.title}
                    </option>
                  ))}
                </select>
                <button
                  type="button"
                  onClick={() => void linkDocument()}
                  disabled={docLinkBusy || !pickedDocumentId}
                  className="shrink-0 rounded-md border border-border px-3 py-1.5 text-xs font-medium text-foreground transition-colors duration-fast hover:bg-primary-subtle disabled:cursor-not-allowed disabled:opacity-50"
                >
                  연결
                </button>
              </div>
            )}
          </div>

          <div className="flex items-center justify-between gap-2 pt-1">
            <div className="flex items-center gap-3">
              {/* 서버 규칙 그대로 P/C 또는 등록자에게만 노출 */}
              {canTrash && (
                <button
                  type="button"
                  onClick={toggleTrash}
                  disabled={trashBusy}
                  className={
                    "rounded-md px-3 py-1.5 text-sm font-medium transition-colors duration-fast disabled:cursor-not-allowed disabled:opacity-50 " +
                    (trashed
                      ? "border border-border text-foreground hover:bg-primary-subtle"
                      : "text-destructive hover:bg-destructive-subtle")
                  }
                >
                  {trashed ? "복원" : "휴지통으로"}
                </button>
              )}
              <Link
                href={`/${companyId}/${projectId}/threads/open?targetType=ISSUE&targetId=${issue.id}`}
                className="text-xs font-medium text-primary hover:opacity-70"
              >
                쓰레드 열기 →
              </Link>
            </div>

            <div className="flex gap-2">
              <button
                type="button"
                onClick={onClose}
                className="rounded-md px-3 py-1.5 text-sm font-medium text-muted transition-colors duration-fast hover:bg-primary-subtle"
              >
                닫기
              </button>
              {!trashed && (
                <Button type="submit" loading={isSubmitting} className="w-auto px-4 py-1.5">
                  저장
                </Button>
              )}
            </div>
          </div>
        </form>
      </div>
    </div>
  );
}

export function IssueDetailModal({
  issue,
  canManage,
  isReporter,
  members,
  onClose,
  onUpdated,
}: {
  /** null이면 닫힌 상태 — 폼 자체를 마운트하지 않는다(위 구조 주석 참고). */
  issue: IssueResponse | null;
  canManage: boolean;
  isReporter: boolean;
  members: ProjectMemberResponse[];
  onClose: () => void;
  onUpdated: (issue: IssueResponse) => void;
}) {
  if (!issue) return null;
  return (
    <IssueDetailDialog
      issue={issue}
      canManage={canManage}
      isReporter={isReporter}
      members={members}
      onClose={onClose}
      onUpdated={onUpdated}
    />
  );
}
