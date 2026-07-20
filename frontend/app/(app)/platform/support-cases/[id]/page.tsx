"use client";

/*
 * S15 문의·신고 상세 (WBS 4.10.5)
 *
 * 상세(supportCase+publicReplies)와 internal note는 backend가 서로 다른
 * endpoint로 분리해 내려준다(§10-A.2) — 두 번 fetch한다. 모든 mutation
 * (공개답변/내부메모 작성, 상태변경)은 body와 별개인 reason(3~500자)이
 * 필수라 공용 입력창 하나로 세 액션이 함께 쓴다.
 */
import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { platformSupportCaseService } from "@/services/platform-support-case.service";
import { platformErrorMessage, isValidPlatformReason } from "@/lib/platform";
import {
  SUPPORT_CASE_STATUS_CLASS,
  SUPPORT_CASE_STATUS_LABEL,
  SUPPORT_CASE_TYPE_LABEL,
} from "@/lib/support-case";
import { SUPPORT_CASE_STATUS_TRANSITIONS as TRANSITIONS } from "@/types/support-case.types";
import type {
  InternalNoteResponse,
  PublicReplyResponse,
  SupportCaseResponse,
  SupportCaseStatus,
} from "@/types/support-case.types";

type LoadState = "loading" | "ready" | "error";

export default function PlatformSupportCaseDetailPage() {
  const params = useParams<{ id: string }>();
  const supportCaseId = params.id;

  const [supportCase, setSupportCase] = useState<SupportCaseResponse | null>(null);
  const [publicReplies, setPublicReplies] = useState<PublicReplyResponse[]>([]);
  const [internalNotes, setInternalNotes] = useState<InternalNoteResponse[]>([]);
  const [state, setState] = useState<LoadState>("loading");
  const [loadError, setLoadError] = useState<string | null>(null);

  const [replyBody, setReplyBody] = useState("");
  const [replyReason, setReplyReason] = useState("");
  const [replyBusy, setReplyBusy] = useState(false);
  const [replyError, setReplyError] = useState<string | null>(null);

  const [noteBody, setNoteBody] = useState("");
  const [noteReason, setNoteReason] = useState("");
  const [noteBusy, setNoteBusy] = useState(false);
  const [noteError, setNoteError] = useState<string | null>(null);

  const [statusTarget, setStatusTarget] = useState<SupportCaseStatus | "">("");
  const [statusReason, setStatusReason] = useState("");
  const [statusBusy, setStatusBusy] = useState(false);
  const [statusError, setStatusError] = useState<string | null>(null);

  const fetchAll = useCallback(async () => {
    setState("loading");
    setLoadError(null);
    try {
      const [detailRes, notesRes] = await Promise.all([
        platformSupportCaseService.get(supportCaseId),
        platformSupportCaseService.listInternalNotes(supportCaseId),
      ]);
      setSupportCase(detailRes.data.data.supportCase);
      setPublicReplies(detailRes.data.data.publicReplies);
      setInternalNotes(notesRes.data.data.internalNotes);
      setState("ready");
    } catch (err) {
      setLoadError(platformErrorMessage(err, "문의·신고 상세를 불러오지 못했습니다."));
      setState("error");
    }
  }, [supportCaseId]);

  useEffect(() => {
    (async () => {
      await fetchAll();
    })();
  }, [fetchAll]);

  const submitReply = async () => {
    setReplyError(null);
    if (!replyBody.trim()) return setReplyError("답변 내용을 입력해 주세요.");
    if (!isValidPlatformReason(replyReason)) return setReplyError("사유는 3~500자로 입력해 주세요.");
    setReplyBusy(true);
    try {
      const { data } = await platformSupportCaseService.createPublicReply(supportCaseId, {
        body: replyBody.trim(),
        reason: replyReason.trim(),
      });
      setPublicReplies((prev) => [...prev, data.data]);
      setReplyBody("");
      setReplyReason("");
    } catch (err) {
      setReplyError(platformErrorMessage(err, "답변 작성에 실패했습니다."));
    } finally {
      setReplyBusy(false);
    }
  };

  const submitNote = async () => {
    setNoteError(null);
    if (!noteBody.trim()) return setNoteError("메모 내용을 입력해 주세요.");
    if (!isValidPlatformReason(noteReason)) return setNoteError("사유는 3~500자로 입력해 주세요.");
    setNoteBusy(true);
    try {
      const { data } = await platformSupportCaseService.createInternalNote(supportCaseId, {
        body: noteBody.trim(),
        reason: noteReason.trim(),
      });
      setInternalNotes((prev) => [...prev, data.data]);
      setNoteBody("");
      setNoteReason("");
    } catch (err) {
      setNoteError(platformErrorMessage(err, "내부 메모 작성에 실패했습니다."));
    } finally {
      setNoteBusy(false);
    }
  };

  const submitStatus = async () => {
    setStatusError(null);
    if (!statusTarget) return setStatusError("변경할 상태를 선택해 주세요.");
    if (!isValidPlatformReason(statusReason)) return setStatusError("사유는 3~500자로 입력해 주세요.");
    setStatusBusy(true);
    try {
      const { data } = await platformSupportCaseService.updateStatus(supportCaseId, {
        status: statusTarget,
        reason: statusReason.trim(),
      });
      setSupportCase(data.data);
      setStatusTarget("");
      setStatusReason("");
    } catch (err) {
      setStatusError(platformErrorMessage(err, "상태 변경에 실패했습니다."));
    } finally {
      setStatusBusy(false);
    }
  };

  if (state === "loading") return <p className="text-sm text-muted">불러오는 중...</p>;
  if (state === "error" || !supportCase) {
    return (
      <div>
        <p className="text-sm text-destructive">{loadError}</p>
        <Link href="/platform/support-cases" className="mt-3 inline-block text-sm text-primary hover:underline">
          ← 목록으로
        </Link>
      </div>
    );
  }

  const availableTransitions = TRANSITIONS[supportCase.status];

  return (
    <div>
      <Link href="/platform/support-cases" className="text-sm text-muted transition-colors duration-fast hover:text-primary">
        ← 목록으로
      </Link>

      <div className="mt-3 flex items-start justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <span className="rounded-full bg-border px-2 py-0.5 text-xs font-medium text-muted">
              {SUPPORT_CASE_TYPE_LABEL[supportCase.type]}
            </span>
            <h2 className="text-lg font-semibold text-foreground">{supportCase.title}</h2>
          </div>
          <p className="mt-1 text-xs text-muted">
            #{supportCase.id} · reporter #{supportCase.reporterUserId} ·{" "}
            {new Date(supportCase.createdAt).toLocaleString("ko-KR")}
            {supportCase.targetType && ` · target ${supportCase.targetType}${supportCase.targetId ? ` #${supportCase.targetId}` : ""}`}
          </p>
        </div>
        <span
          className={`shrink-0 rounded-full px-2.5 py-0.5 text-xs font-medium ${SUPPORT_CASE_STATUS_CLASS[supportCase.status]}`}
        >
          {SUPPORT_CASE_STATUS_LABEL[supportCase.status]}
        </span>
      </div>

      <p className="mt-4 whitespace-pre-wrap rounded-xl border border-border bg-surface p-4 text-sm text-foreground">
        {supportCase.body}
      </p>

      {/* 상태 변경 */}
      <section className="mt-6 rounded-xl border border-border bg-surface p-5">
        <h3 className="text-sm font-semibold text-foreground">상태 변경</h3>
        {availableTransitions.length === 0 ? (
          <p className="mt-2 text-xs text-muted">종료된 case는 더 이상 상태를 바꿀 수 없습니다.</p>
        ) : (
          <div className="mt-3 space-y-2">
            <div className="flex gap-2">
              <select
                aria-label="변경할 상태"
                value={statusTarget}
                onChange={(e) => setStatusTarget(e.target.value as SupportCaseStatus)}
                disabled={statusBusy}
                className="rounded-md border border-border bg-surface px-2 py-1.5 text-xs text-foreground focus:border-ring focus:outline-none disabled:opacity-50"
              >
                <option value="">상태 선택</option>
                {availableTransitions.map((s) => (
                  <option key={s} value={s}>
                    {SUPPORT_CASE_STATUS_LABEL[s]}
                  </option>
                ))}
              </select>
              <input
                aria-label="상태 변경 사유"
                value={statusReason}
                onChange={(e) => setStatusReason(e.target.value)}
                placeholder="변경 사유(3~500자)"
                disabled={statusBusy}
                className="flex-1 rounded-md border border-border bg-surface px-3 py-1.5 text-xs text-foreground placeholder:text-placeholder focus:border-ring focus:outline-none disabled:opacity-50"
              />
              <button
                onClick={submitStatus}
                disabled={statusBusy}
                className="rounded-md btn-gradient px-3 py-1.5 text-xs font-semibold text-primary-foreground disabled:opacity-50"
              >
                변경
              </button>
            </div>
            {statusError && <p className="text-xs text-destructive">{statusError}</p>}
          </div>
        )}
      </section>

      {/* 공개 답변 */}
      <section className="mt-6 rounded-xl border border-border bg-surface p-5">
        <h3 className="text-sm font-semibold text-foreground">공개 답변 ({publicReplies.length})</h3>
        <ul className="mt-3 space-y-3">
          {publicReplies.map((reply) => (
            <li key={reply.id} className="rounded-lg border border-border px-4 py-3">
              <p className="text-xs text-muted">
                관리자 #{reply.authorUserId} ({reply.authorRole}) · {new Date(reply.createdAt).toLocaleString("ko-KR")}
              </p>
              <p className="mt-1 whitespace-pre-wrap text-sm text-foreground">{reply.body}</p>
            </li>
          ))}
          {publicReplies.length === 0 && <p className="text-xs text-muted">아직 등록된 공개 답변이 없습니다.</p>}
        </ul>

        <div className="mt-4 space-y-2 border-t border-border pt-4">
          <textarea
            aria-label="공개 답변 내용"
            value={replyBody}
            onChange={(e) => setReplyBody(e.target.value)}
            placeholder="사용자에게 노출될 답변 내용"
            rows={3}
            disabled={replyBusy}
            className="w-full rounded-md border border-border bg-surface px-3 py-2 text-sm text-foreground placeholder:text-placeholder focus:border-ring focus:outline-none disabled:opacity-50"
          />
          <div className="flex gap-2">
            <input
              aria-label="공개 답변 작성 사유"
              value={replyReason}
              onChange={(e) => setReplyReason(e.target.value)}
              placeholder="작성 사유(3~500자)"
              disabled={replyBusy}
              className="flex-1 rounded-md border border-border bg-surface px-3 py-1.5 text-xs text-foreground placeholder:text-placeholder focus:border-ring focus:outline-none disabled:opacity-50"
            />
            <button
              onClick={submitReply}
              disabled={replyBusy}
              className="rounded-md btn-gradient px-4 py-1.5 text-xs font-semibold text-primary-foreground disabled:opacity-50"
            >
              답변 등록
            </button>
          </div>
          {replyError && <p className="text-xs text-destructive">{replyError}</p>}
        </div>
      </section>

      {/* 내부 메모 */}
      <section className="mt-6 rounded-xl border border-border bg-surface p-5">
        <h3 className="text-sm font-semibold text-foreground">내부 메모 ({internalNotes.length})</h3>
        <p className="mt-1 text-xs text-muted">관리자에게만 보이며 신고자에게 노출되지 않습니다.</p>
        <ul className="mt-3 space-y-3">
          {internalNotes.map((note) => (
            <li key={note.id} className="rounded-lg border border-dashed border-border px-4 py-3">
              <p className="text-xs text-muted">
                관리자 #{note.authorUserId} ({note.authorRole}) · {new Date(note.createdAt).toLocaleString("ko-KR")}
              </p>
              <p className="mt-1 whitespace-pre-wrap text-sm text-foreground">{note.body}</p>
            </li>
          ))}
          {internalNotes.length === 0 && <p className="text-xs text-muted">아직 등록된 내부 메모가 없습니다.</p>}
        </ul>

        <div className="mt-4 space-y-2 border-t border-border pt-4">
          <textarea
            aria-label="내부 메모 내용"
            value={noteBody}
            onChange={(e) => setNoteBody(e.target.value)}
            placeholder="관리자 전용 메모"
            rows={3}
            disabled={noteBusy}
            className="w-full rounded-md border border-border bg-surface px-3 py-2 text-sm text-foreground placeholder:text-placeholder focus:border-ring focus:outline-none disabled:opacity-50"
          />
          <div className="flex gap-2">
            <input
              aria-label="내부 메모 작성 사유"
              value={noteReason}
              onChange={(e) => setNoteReason(e.target.value)}
              placeholder="작성 사유(3~500자)"
              disabled={noteBusy}
              className="flex-1 rounded-md border border-border bg-surface px-3 py-1.5 text-xs text-foreground placeholder:text-placeholder focus:border-ring focus:outline-none disabled:opacity-50"
            />
            <button
              onClick={submitNote}
              disabled={noteBusy}
              className="rounded-md btn-gradient px-4 py-1.5 text-xs font-semibold text-primary-foreground disabled:opacity-50"
            >
              메모 등록
            </button>
          </div>
          {noteError && <p className="text-xs text-destructive">{noteError}</p>}
        </div>
      </section>
    </div>
  );
}
