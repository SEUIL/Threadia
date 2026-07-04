"use client";

/*
 * S08 — 문서 편집기 / 그래프 뷰
 *
 * 접근 사용자: A/P/C. 화면정의서(docs/03-design/화면정의서.md §S08) 기준
 * 제목/유형/Markdown 본문 편집, 저장, 미리보기, 관련 이슈 연결(양방향),
 * 문서 쓰레드 열기(S10), 휴지통 처리를 구현한다.
 *
 * 설계 메모 반영:
 *   - 문서 단독 논의 UI는 두지 않는다 — 논의는 "쓰레드 열기"로 S10을 연다.
 *   - `docId === "new"`면 신규 작성 모드(S07의 "+ 새 문서" 진입점)로 처리한다.
 *
 * ⚠️ MOCK: 문서 API(`GET/PATCH /documents/{documentId}`,
 * `POST /projects/{projectId}/documents`, document-issue-links,
 * `POST /projects/{projectId}/threads`)가 아직 없어서 로컬 state로 흉내낸다.
 * Markdown 렌더링도 별도 라이브러리(react-markdown 등) 추가 없이 헤더(#)·
 * 굵게(**)·목록(-)만 처리하는 최소 파서로 미리보기를 보여준다 — 실제 저장되는
 * 값은 원본 텍스트 그대로라 라이브러리 도입 시 그대로 호환된다.
 */
import { useMemo, useState } from "react";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";

type DocType = "DOC" | "MEETING_NOTE";

const MOCK_DOC = {
  title: "회사/프로젝트 API 설계",
  type: "DOC" as DocType,
  content: "# API 설계 메모\n\n- 회사 생성은 **COMPANY_ADMIN**만 가능\n- 프로젝트는 회사 소속으로 생성\n\n## 남은 작업\n- 부서 API 확정",
  linkedIssueKeys: ["THR-101", "THR-102"],
};

// 아주 단순한 마크다운 → JSX 변환. #/##/**/- 만 처리한다(라이브러리 미도입).
function renderMarkdownPreview(text: string) {
  const lines = text.split("\n");
  return lines.map((line, i) => {
    const bolded = line.split(/(\*\*[^*]+\*\*)/g).map((part, j) =>
      part.startsWith("**") && part.endsWith("**") ? (
        <strong key={j}>{part.slice(2, -2)}</strong>
      ) : (
        part
      )
    );
    if (line.startsWith("## ")) {
      return (
        <h3 key={i} className="mt-3 text-sm font-bold text-foreground">
          {line.slice(3)}
        </h3>
      );
    }
    if (line.startsWith("# ")) {
      return (
        <h2 key={i} className="mt-4 text-base font-bold text-foreground">
          {line.slice(2)}
        </h2>
      );
    }
    if (line.startsWith("- ")) {
      return (
        <li key={i} className="ml-4 list-disc text-sm text-foreground">
          {bolded.length > 1 ? line.slice(2).split(/(\*\*[^*]+\*\*)/g).map((p, j) => (p.startsWith("**") ? <strong key={j}>{p.slice(2, -2)}</strong> : p)) : line.slice(2)}
        </li>
      );
    }
    if (line.trim() === "") return <div key={i} className="h-2" />;
    return (
      <p key={i} className="text-sm text-foreground">
        {bolded}
      </p>
    );
  });
}

export default function DocEditorPage() {
  const params = useParams<{ companyId: string; projectId: string; docId: string }>();
  const router = useRouter();
  const { companyId, projectId, docId } = params;
  const isNew = docId === "new";

  const [title, setTitle] = useState(isNew ? "" : MOCK_DOC.title);
  const [type, setType] = useState<DocType>(isNew ? "DOC" : MOCK_DOC.type);
  const [content, setContent] = useState(isNew ? "" : MOCK_DOC.content);
  const [linkedIssues, setLinkedIssues] = useState<string[]>(isNew ? [] : MOCK_DOC.linkedIssueKeys);
  const [newIssueKey, setNewIssueKey] = useState("");
  const [showPreview, setShowPreview] = useState(false);
  const [trashed, setTrashed] = useState(false);
  const [savedAt, setSavedAt] = useState<string | null>(null);

  const preview = useMemo(() => renderMarkdownPreview(content), [content]);

  const save = () => {
    // TODO: isNew면 POST /api/v1/projects/{projectId}/documents,
    // 아니면 PATCH /api/v1/documents/{documentId} (DOCUMENT_UPDATED 알림 후보 생성)
    setSavedAt(new Date().toLocaleTimeString("ko-KR", { hour: "2-digit", minute: "2-digit" }));
    if (isNew) {
      // 실제로는 생성된 documentId로 이동해야 하지만 mock이라 목록으로 돌아간다.
      router.push(`/${companyId}/${projectId}/docs`);
    }
  };

  const linkIssue = () => {
    const key = newIssueKey.trim().toUpperCase();
    if (!key || linkedIssues.includes(key)) return;
    // TODO: POST /api/v1/projects/{projectId}/document-issue-links
    setLinkedIssues((prev) => [...prev, key]);
    setNewIssueKey("");
  };

  const unlinkIssue = (key: string) => {
    // TODO: DELETE /api/v1/document-issue-links/{linkId}
    setLinkedIssues((prev) => prev.filter((k) => k !== key));
  };

  const trashDoc = () => {
    // TODO: POST /api/v1/documents/{documentId}/trash
    setTrashed(true);
  };

  if (trashed) {
    return (
      <div className="mx-auto max-w-2xl px-6 py-10 text-center">
        <p className="text-sm text-muted">문서를 휴지통으로 이동했습니다.</p>
        <Link
          href={`/${companyId}/${projectId}/docs`}
          className="mt-3 inline-block text-sm font-medium text-primary hover:opacity-70"
        >
          문서 목록으로 →
        </Link>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-3xl px-6 py-10">
      <div className="flex items-center justify-between">
        <Link href={`/${companyId}/${projectId}/docs`} className="text-sm text-muted transition-colors duration-fast hover:text-primary">
          ← 문서 목록
        </Link>
        <div className="flex items-center gap-2">
          {savedAt && <span className="text-xs text-muted">{savedAt} 저장됨</span>}
          {!isNew && (
            <button
              onClick={trashDoc}
              className="rounded-md px-3 py-1.5 text-xs font-medium text-destructive hover:opacity-70"
            >
              휴지통으로
            </button>
          )}
          <button
            onClick={save}
            className="rounded-md bg-primary px-4 py-1.5 text-sm font-semibold text-primary-foreground hover:bg-primary-hover"
          >
            {isNew ? "생성" : "저장"}
          </button>
        </div>
      </div>

      <div className="mt-4 flex items-center gap-2">
        <input
          aria-label="문서 제목"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          placeholder="문서 제목"
          className="flex-1 rounded-lg border border-border bg-surface px-3 py-2 text-base font-semibold text-foreground placeholder:text-placeholder focus:border-ring focus:outline-none"
        />
        <select
          aria-label="문서 유형"
          value={type}
          onChange={(e) => setType(e.target.value as DocType)}
          className="rounded-lg border border-border bg-surface px-2 py-2 text-sm text-foreground focus:border-ring focus:outline-none"
        >
          <option value="DOC">문서</option>
          <option value="MEETING_NOTE">회의록</option>
        </select>
      </div>

      {/* 편집/미리보기 탭 */}
      <div className="mt-4 flex gap-1 rounded-lg bg-border/40 p-1 w-fit">
        <button
          onClick={() => setShowPreview(false)}
          className={
            "rounded-md px-3 py-1.5 text-sm font-medium transition-colors duration-fast " +
            (!showPreview ? "bg-surface text-primary shadow-sm" : "text-muted hover:text-foreground")
          }
        >
          편집
        </button>
        <button
          onClick={() => setShowPreview(true)}
          className={
            "rounded-md px-3 py-1.5 text-sm font-medium transition-colors duration-fast " +
            (showPreview ? "bg-surface text-primary shadow-sm" : "text-muted hover:text-foreground")
          }
        >
          미리보기
        </button>
      </div>

      {showPreview ? (
        <div className="mt-3 min-h-64 rounded-xl border border-border bg-surface p-4">
          {content ? preview : <p className="text-sm text-placeholder">내용이 없습니다.</p>}
        </div>
      ) : (
        <textarea
          aria-label="문서 본문"
          value={content}
          onChange={(e) => setContent(e.target.value)}
          placeholder="Markdown으로 작성하세요 (# 헤더, **굵게**, - 목록)"
          rows={14}
          className="mt-3 w-full resize-none rounded-xl border border-border bg-surface p-4 font-mono text-sm text-foreground placeholder:text-placeholder focus:border-ring focus:outline-none"
        />
      )}

      {/* 관련 이슈 연결 */}
      <section className="mt-6 rounded-xl border border-border bg-surface p-5">
        <h2 className="text-sm font-semibold text-foreground">관련 이슈</h2>
        <div className="mt-2 flex flex-wrap gap-2">
          {linkedIssues.map((key) => (
            <span
              key={key}
              className="flex items-center gap-1.5 rounded-full bg-accent-subtle px-3 py-1 text-xs font-medium text-accent"
            >
              {key}
              <button onClick={() => unlinkIssue(key)} aria-label={`${key} 연결 해제`} className="hover:opacity-60">
                ×
              </button>
            </span>
          ))}
          {linkedIssues.length === 0 && <span className="text-xs text-muted">연결된 이슈가 없습니다.</span>}
        </div>
        <div className="mt-3 flex gap-2">
          <input
            aria-label="연결할 이슈 키"
            value={newIssueKey}
            onChange={(e) => setNewIssueKey(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && linkIssue()}
            placeholder="이슈 키 (예: THR-103)"
            className="flex-1 rounded-lg border border-border bg-surface px-3 py-2 text-sm text-foreground placeholder:text-placeholder focus:border-ring focus:outline-none"
          />
          <button
            onClick={linkIssue}
            className="rounded-lg border border-border px-3 py-2 text-sm font-medium text-foreground hover:bg-primary-subtle"
          >
            연결
          </button>
        </div>
      </section>

      {/* 하단 액션: 쓰레드 / 그래프 뷰 */}
      {!isNew && (
        <div className="mt-4 flex gap-4 text-sm">
          <Link
            href={`/${companyId}/${projectId}/threads/doc-${docId}`}
            className="font-medium text-primary hover:opacity-70"
          >
            쓰레드 열기 →
          </Link>
          <Link
            href={`/${companyId}/${projectId}/docs`}
            className="font-medium text-primary hover:opacity-70"
          >
            그래프 뷰에서 보기 →
          </Link>
        </div>
      )}
    </div>
  );
}
