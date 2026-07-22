"use client";

/*
 * IssueFromMessageModal — components/issues/IssueFromMessageModal.tsx
 *
 * S09 프로젝트 채팅의 "메시지 → 이슈 생성" 액션(WBS 4.5.1.2). Backend(WBS 4.5.1.1,
 * PR #214)가 이미 dev에 있다: `POST /projects/{projectId}/issues/from-message`는
 * P/C(PROJECT_ADMIN/COMPANY_ADMIN) 전용이라 일반 멤버가 호출하면 403이다 — 이
 * 모달을 여는 액션 자체를 messages/page.tsx에서 `viewer.canManageProject`인
 * 경우에만 노출한다(호출부 책임).
 *
 * 이 endpoint 자체가 P/C 전용이라, IssueDetailModal처럼 canManage로 필드별
 * disabled를 나누지 않는다 — 열 수 있는 사람은 전부 모든 필드를 쓸 수 있다.
 *
 * 스타일/구조는 IssueDetailModal(Esc 닫기, Tab 트랩, react-hook-form+zod)과
 * 동일한 패턴을 따른다.
 */
import { useEffect, useRef, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Button } from "@/components/ui/Button";
import { invitationErrorMessage } from "@/lib/invitation";
import { issueService } from "@/services/issue.service";
import type { IssuePriority, IssueResponse } from "@/types/issue.types";
import type { MessageResponse } from "@/types/message.types";
import type { ProjectMemberResponse } from "@/types/project-member.types";

const PRIORITY_LABEL: Record<IssuePriority, string> = {
  LOW: "낮음",
  MEDIUM: "보통",
  HIGH: "높음",
  URGENT: "긴급",
};

// 메시지 본문을 제목 기본값으로 쓰되 서버 제목 상한(200자)에 맞춘다.
function titleFromMessage(content: string): string {
  const trimmed = content.trim().replace(/\s+/g, " ");
  return trimmed.length > 200 ? trimmed.slice(0, 197) + "..." : trimmed;
}

const formSchema = z.object({
  title: z.string().trim().min(1, "제목은 필수입니다").max(200, "제목은 200자 이하여야 합니다"),
  description: z.string(),
  priority: z.enum(["LOW", "MEDIUM", "HIGH", "URGENT"]),
  assigneeUserId: z.string(),
  dueDate: z.string(),
});

type FormValues = z.infer<typeof formSchema>;

function IssueFromMessageDialog({
  message,
  members,
  onClose,
  onCreated,
}: {
  message: MessageResponse;
  members: ProjectMemberResponse[];
  onClose: () => void;
  onCreated: (issue: IssueResponse) => void;
}) {
  const [error, setError] = useState<string | null>(null);
  const dialogRef = useRef<HTMLDivElement>(null);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      title: titleFromMessage(message.content),
      description: message.content,
      priority: "MEDIUM",
      assigneeUserId: "",
      dueDate: "",
    },
  });

  // Esc로 닫기 + focus 트랩(IssueDetailModal/ProfileEditModal과 동일 패턴).
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

  const onSubmit = async (values: FormValues) => {
    setError(null);
    try {
      const { data } = await issueService.createFromMessage(message.projectId, {
        messageId: message.id,
        title: values.title,
        description: values.description === "" ? null : values.description,
        priority: values.priority,
        assigneeUserId: values.assigneeUserId === "" ? null : Number(values.assigneeUserId),
        dueDate: values.dueDate === "" ? null : values.dueDate,
      });
      onCreated(data.data.issue);
      onClose();
    } catch (err) {
      setError(invitationErrorMessage(err, "이슈 생성에 실패했습니다."));
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
        aria-labelledby="issue-from-message-modal-title"
        className="relative max-h-[85vh] w-full max-w-[520px] overflow-y-auto rounded-xl bg-surface p-6 shadow-xl animate-enter"
      >
        <h2 id="issue-from-message-modal-title" className="text-lg font-bold text-foreground">
          메시지로 이슈 만들기
        </h2>
        <p className="mt-1 rounded-md bg-border/30 px-3 py-2 text-xs text-muted line-clamp-3">
          {message.content}
        </p>

        {error && <p className="mt-3 text-sm text-destructive">{error}</p>}

        <form onSubmit={handleSubmit(onSubmit)} className="mt-4 flex flex-col gap-4" noValidate>
          <label className="flex flex-col gap-1 text-xs font-medium text-muted">
            제목
            <input
              {...register("title")}
              className="rounded-md border border-border bg-surface px-3 py-2 text-sm text-foreground focus:border-ring focus:outline-none"
            />
            {errors.title && <span className="text-xs text-destructive">{errors.title.message}</span>}
          </label>

          <label className="flex flex-col gap-1 text-xs font-medium text-muted">
            설명
            <textarea
              {...register("description")}
              rows={4}
              placeholder="이슈 설명"
              className="resize-y rounded-md border border-border bg-surface px-3 py-2 text-sm text-foreground placeholder:text-placeholder focus:border-ring focus:outline-none"
            />
          </label>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
            <label className="flex flex-col gap-1 text-xs font-medium text-muted">
              우선순위
              <select
                {...register("priority")}
                className="rounded-md border border-border bg-surface px-2 py-2 text-sm text-foreground focus:border-ring focus:outline-none"
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
                className="rounded-md border border-border bg-surface px-2 py-2 text-sm text-foreground focus:border-ring focus:outline-none"
              >
                <option value="">미지정</option>
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
                className="rounded-md border border-border bg-surface px-2 py-2 text-sm text-foreground focus:border-ring focus:outline-none"
              />
            </label>
          </div>

          <div className="flex justify-end gap-2 pt-1">
            <button
              type="button"
              onClick={onClose}
              className="rounded-md px-3 py-1.5 text-sm font-medium text-muted transition-colors duration-fast hover:bg-primary-subtle"
            >
              취소
            </button>
            {/* Button 기본 클래스의 w-full이 utility 정의 순서상 뒤의 className을 이길 수 있어
                (IssueDetailModal도 같은 패턴) !important로 확실히 덮어쓴다 — 실측으로
                확인(w-auto만으로는 418px로 렌더돼 옆 "취소" 버튼이 찌그러짐). */}
            <Button type="submit" loading={isSubmitting} className="!w-auto px-4 py-1.5">
              이슈 생성
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}

export function IssueFromMessageModal({
  message,
  members,
  onClose,
  onCreated,
}: {
  /** null이면 닫힌 상태 — 폼 자체를 마운트하지 않는다. */
  message: MessageResponse | null;
  members: ProjectMemberResponse[];
  onClose: () => void;
  onCreated: (issue: IssueResponse) => void;
}) {
  if (!message) return null;
  return (
    <IssueFromMessageDialog message={message} members={members} onClose={onClose} onCreated={onCreated} />
  );
}
