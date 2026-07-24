"use client";

/*
 * 문의하기 — 일반 사용자용 support case 생성/본인 목록.
 *
 * company-agnostic 경로(/invitations, /platform과 같은 패턴) — support case는
 * 회사 소속과 무관하게 로그인 사용자 본인 것만 다룬다(API-명세서 §10-A).
 *
 * 코드 vs API-명세서 감사 중 발견: `POST/GET /api/v1/support-cases`(인증 ACTIVE
 * 사용자 누구나 호출 가능, platform admin 권한 불필요)가 완전히 문서화·구현돼
 * 있는데 프론트엔 관리자용 화면(/platform/support-cases)만 있고 일반 사용자가
 * 문의를 넣을 화면이 아예 없었다.
 *
 * REPORT(신고)는 targetType이 필수인데 그걸 채울 "신고하기" 진입점(이슈/문서
 * 등 콘텐츠 화면의 신고 액션)이 아직 어디에도 없어서, 이번 슬라이스는 일반
 * 문의(INQUIRY) 생성만 다룬다 — REPORT 생성 UI는 범위 밖.
 */
import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { supportCaseService } from "@/services/support-case.service";
import { invitationErrorMessage } from "@/lib/invitation";
import {
  SUPPORT_CASE_STATUS_CLASS,
  SUPPORT_CASE_STATUS_LABEL,
  SUPPORT_CASE_TYPE_LABEL,
} from "@/lib/support-case";
import type { SupportCaseResponse } from "@/types/support-case.types";

type LoadState = "loading" | "ready" | "error";

export default function SupportCasesPage() {
  const [cases, setCases] = useState<SupportCaseResponse[]>([]);
  const [state, setState] = useState<LoadState>("loading");
  const [loadError, setLoadError] = useState<string | null>(null);

  const [title, setTitle] = useState("");
  const [body, setBody] = useState("");
  const [reason, setReason] = useState("");
  const [createBusy, setCreateBusy] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);

  const fetchCases = useCallback(async () => {
    setState("loading");
    setLoadError(null);
    try {
      const { data } = await supportCaseService.listOwn();
      setCases(data.data.supportCases);
      setState("ready");
    } catch (err) {
      setLoadError(invitationErrorMessage(err, "문의 목록을 불러오지 못했습니다."));
      setState("error");
    }
  }, []);

  useEffect(() => {
    (async () => {
      await fetchCases();
    })();
  }, [fetchCases]);

  const submit = async () => {
    setCreateError(null);
    if (!title.trim() || !body.trim()) {
      setCreateError("제목과 내용을 입력해 주세요.");
      return;
    }
    const trimmedReason = reason.trim();
    if (trimmedReason.length < 3 || trimmedReason.length > 500) {
      setCreateError("문의 사유는 3~500자로 입력해 주세요.");
      return;
    }
    setCreateBusy(true);
    try {
      const { data } = await supportCaseService.create({
        type: "INQUIRY",
        title: title.trim(),
        body: body.trim(),
        reason: trimmedReason,
      });
      setCases((prev) => [data.data.supportCase, ...prev]);
      setTitle("");
      setBody("");
      setReason("");
    } catch (err) {
      setCreateError(invitationErrorMessage(err, "문의 등록에 실패했습니다."));
    } finally {
      setCreateBusy(false);
    }
  };

  return (
    <div className="mx-auto max-w-3xl px-6 py-10">
      <Link href="/hub" className="text-sm text-muted transition-colors duration-fast hover:text-primary">
        ← 허브로
      </Link>
      <div className="mt-3">
        <h1 className="text-xl font-bold text-foreground">문의하기</h1>
        <p className="mt-1 text-sm text-muted">궁금한 점이나 문제를 남기면 운영팀이 확인 후 답변합니다.</p>
      </div>

      <section className="mt-6 rounded-xl border border-border bg-surface p-6">
        <h2 className="text-sm font-semibold text-foreground">새 문의</h2>
        <div className="mt-3 flex flex-col gap-3">
          <input
            aria-label="문의 제목"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="제목"
            disabled={createBusy}
            className="rounded-lg border border-border bg-surface px-3 py-2 text-sm text-foreground placeholder:text-placeholder focus:border-ring focus:outline-none disabled:opacity-50"
          />
          <textarea
            aria-label="문의 내용"
            value={body}
            onChange={(e) => setBody(e.target.value)}
            placeholder="문의 내용을 자세히 적어 주세요."
            rows={4}
            disabled={createBusy}
            className="resize-y rounded-lg border border-border bg-surface px-3 py-2 text-sm text-foreground placeholder:text-placeholder focus:border-ring focus:outline-none disabled:opacity-50"
          />
          <input
            aria-label="문의 사유"
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            placeholder="문의 사유(3~500자, 접수 기록용으로 짧게)"
            disabled={createBusy}
            className="rounded-lg border border-border bg-surface px-3 py-2 text-sm text-foreground placeholder:text-placeholder focus:border-ring focus:outline-none disabled:opacity-50"
          />
          {createError && <p className="text-xs text-destructive">{createError}</p>}
          <button
            onClick={() => void submit()}
            disabled={createBusy}
            className="self-end rounded-lg btn-gradient px-4 py-2 text-sm font-semibold text-primary-foreground transition-colors duration-fast disabled:opacity-50"
          >
            {createBusy ? "등록 중..." : "문의 등록"}
          </button>
        </div>
      </section>

      <section className="mt-6 rounded-xl border border-border bg-surface p-6">
        <h2 className="text-sm font-semibold text-foreground">내 문의 ({cases.length})</h2>

        {state === "loading" && <p className="mt-4 text-xs text-muted">불러오는 중...</p>}
        {state === "error" && <p className="mt-4 text-xs text-destructive">{loadError}</p>}

        {state === "ready" && (
          <ul className="mt-4 divide-y divide-border">
            {cases.map((c) => (
              <li key={c.id}>
                <Link
                  href={`/support/${c.id}`}
                  className="flex items-center justify-between gap-3 px-1 py-3 transition-colors duration-fast hover:bg-primary-subtle"
                >
                  <div className="min-w-0">
                    <div className="flex items-center gap-2">
                      <span className="rounded-full bg-border px-2 py-0.5 text-xs font-medium text-muted">
                        {SUPPORT_CASE_TYPE_LABEL[c.type]}
                      </span>
                      <p className="truncate text-sm font-medium text-foreground">{c.title}</p>
                    </div>
                    <p className="mt-1 text-xs text-muted">{new Date(c.createdAt).toLocaleString("ko-KR")}</p>
                  </div>
                  <span
                    className={`shrink-0 rounded-full px-2.5 py-0.5 text-xs font-medium ${SUPPORT_CASE_STATUS_CLASS[c.status]}`}
                  >
                    {SUPPORT_CASE_STATUS_LABEL[c.status]}
                  </span>
                </Link>
              </li>
            ))}
            {cases.length === 0 && (
              <li className="px-1 py-6 text-center text-sm text-muted">등록한 문의가 없습니다.</li>
            )}
          </ul>
        )}
      </section>
    </div>
  );
}
