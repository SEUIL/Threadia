"use client";

/*
 * 문의 상세 — 일반 사용자용. GET /api/v1/support-cases/{supportCaseId}(reporter
 * 본인만) 기준. 내부 메모/상태 변경/공개 답변 작성은 platform admin 전용이라
 * (§10-A) 이 화면엔 없다 — 공개 답변 조회만 한다.
 */
import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { supportCaseService } from "@/services/support-case.service";
import { invitationErrorMessage } from "@/lib/invitation";
import {
  SUPPORT_CASE_STATUS_CLASS,
  SUPPORT_CASE_STATUS_LABEL,
  SUPPORT_CASE_TYPE_LABEL,
} from "@/lib/support-case";
import type { PublicReplyResponse, SupportCaseResponse } from "@/types/support-case.types";

type LoadState = "loading" | "ready" | "error";

export default function SupportCaseDetailPage() {
  const params = useParams<{ id: string }>();
  const supportCaseId = params.id;

  const [supportCase, setSupportCase] = useState<SupportCaseResponse | null>(null);
  const [publicReplies, setPublicReplies] = useState<PublicReplyResponse[]>([]);
  const [state, setState] = useState<LoadState>("loading");
  const [loadError, setLoadError] = useState<string | null>(null);

  const fetchDetail = useCallback(async () => {
    setState("loading");
    setLoadError(null);
    try {
      const { data } = await supportCaseService.getOwn(supportCaseId);
      setSupportCase(data.data.supportCase);
      setPublicReplies(data.data.publicReplies);
      setState("ready");
    } catch (err) {
      setLoadError(invitationErrorMessage(err, "문의 상세를 불러오지 못했습니다."));
      setState("error");
    }
  }, [supportCaseId]);

  useEffect(() => {
    (async () => {
      await fetchDetail();
    })();
  }, [fetchDetail]);

  if (state === "loading") {
    return <div className="mx-auto max-w-3xl px-6 py-10 text-sm text-muted">불러오는 중...</div>;
  }

  if (state === "error" || !supportCase) {
    return (
      <div className="mx-auto max-w-3xl px-6 py-10">
        <Link href="/support" className="text-sm text-muted transition-colors duration-fast hover:text-primary">
          ← 목록으로
        </Link>
        <p className="mt-3 text-sm text-destructive">{loadError}</p>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-3xl px-6 py-10">
      <Link href="/support" className="text-sm text-muted transition-colors duration-fast hover:text-primary">
        ← 목록으로
      </Link>

      <div className="mt-3 flex items-start justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <span className="rounded-full bg-border px-2 py-0.5 text-xs font-medium text-muted">
              {SUPPORT_CASE_TYPE_LABEL[supportCase.type]}
            </span>
            <h1 className="text-lg font-bold text-foreground">{supportCase.title}</h1>
          </div>
          <p className="mt-1 text-xs text-muted">
            {new Date(supportCase.createdAt).toLocaleString("ko-KR")}
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

      <section className="mt-6 rounded-xl border border-border bg-surface p-5">
        <h2 className="text-sm font-semibold text-foreground">답변 ({publicReplies.length})</h2>
        <ul className="mt-3 space-y-3">
          {publicReplies.map((reply) => (
            <li key={reply.id} className="rounded-lg border border-border px-4 py-3">
              <p className="text-xs text-muted">
                운영팀 · {new Date(reply.createdAt).toLocaleString("ko-KR")}
              </p>
              <p className="mt-1 whitespace-pre-wrap text-sm text-foreground">{reply.body}</p>
            </li>
          ))}
          {publicReplies.length === 0 && (
            <p className="text-xs text-muted">아직 답변이 등록되지 않았습니다.</p>
          )}
        </ul>
      </section>
    </div>
  );
}
