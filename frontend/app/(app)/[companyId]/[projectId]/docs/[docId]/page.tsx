"use client";

/*
 * S08 — 문서 편집기 / 그래프 뷰
 *
 * 접근 사용자: A/P/C. 화면정의서(docs/03-design/화면정의서.md §S08) 기준
 * 제목/유형/Markdown 본문 편집, 저장, 미리보기, 관련 이슈 연결(양방향),
 * 문서 쓰레드 열기(S10), 휴지통 처리를 구현한다.
 *
 * WBS 4.6.2 — 4.6.1 Backend(PR #101)의 실 API로 연동했다.
 *   - 이슈 연결은 issueKey 자유 입력이 아니라 프로젝트 이슈 목록에서 골라
 *     document-issue-links API가 요구하는 숫자 issueId로 생성한다.
 *   - 연결 해제(DELETE /document-issue-links/{linkId})는 linkId가 필요해서
 *     프로젝트 전체 링크 목록(GET .../document-issue-links)에서 documentId
 *     기준으로 issueId → linkId 매핑을 만들어 둔다.
 *   - PATCH는 partial update가 아니라 title/type/content 전체를 다시 보낸다
 *     (API-명세서 §13.6, optimistic locking은 Sprint 3 decision-needed).
 *
 * 설계 메모 반영:
 *   - 문서 단독 논의 UI는 두지 않는다 — 논의는 "쓰레드 열기"로 S10을 연다
 *     (Thread frontend는 4.7.1.2로 별도 관리 중이라 기존 mock 경로를 유지한다).
 *   - `docId === "new"`면 신규 작성 모드(S07의 "+ 새 문서" 진입점)로 처리한다.
 *
 * Markdown 렌더링은 별도 라이브러리(react-markdown 등) 추가 없이 헤더(#)·
 * 굵게(**)·목록(-)만 처리하는 최소 파서로 미리보기를 보여준다 — 실제 저장되는
 * 값은 원본 텍스트 그대로라 라이브러리 도입 시 그대로 호환된다.
 */
import { useCallback, useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useAuth } from "@/contexts/AuthContext";
import { invitationErrorMessage } from "@/lib/invitation";
import { documentService } from "@/services/document.service";
import { issueService } from "@/services/issue.service";
import { projectService } from "@/services/project.service";
import type { DocumentType } from "@/types/document.types";
import type { IssueResponse } from "@/types/issue.types";

type Role = "A" | "P" | "C";
type LoadState = "loading" | "ready" | "error";

function viewerRole(viewer: { companyRole: string | null; projectRole: string | null }): Role {
  if (viewer.companyRole === "COMPANY_ADMIN") return "C";
  return viewer.projectRole === "PROJECT_ADMIN" ? "P" : "A";
}

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
          {bolded.length > 1
            ? line
                .slice(2)
                .split(/(\*\*[^*]+\*\*)/g)
                .map((p, j) => (p.startsWith("**") ? <strong key={j}>{p.slice(2, -2)}</strong> : p))
            : line.slice(2)}
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
  const { user } = useAuth();

  const [pageState, setPageState] = useState<LoadState>(isNew ? "ready" : "loading");
  const [pageError, setPageError] = useState<string | null>(null);
  const [role, setRole] = useState<Role | null>(null);
  const [authorUserId, setAuthorUserId] = useState<number | null>(null);

  const [title, setTitle] = useState("");
  const [type, setType] = useState<DocType>("DOC");
  const [content, setContent] = useState("");
  const [linkedIssues, setLinkedIssues] = useState<IssueResponse[]>([]);
  const [linkIdByIssueId, setLinkIdByIssueId] = useState<Record<number, number>>({});
  const [projectIssues, setProjectIssues] = useState<IssueResponse[]>([]);
  const [pickedIssueId, setPickedIssueId] = useState("");

  const [showPreview, setShowPreview] = useState(false);
  const [trashed, setTrashed] = useState(false);
  const [saving, setSaving] = useState(false);
  const [savedAt, setSavedAt] = useState<string | null>(null);
  const [saveError, setSaveError] = useState<string | null>(null);
  const [linkError, setLinkError] = useState<string | null>(null);
  const [linkBusy, setLinkBusy] = useState(false);

  type DocType = DocumentType;

  const load = useCallback(async () => {
    try {
      if (isNew) {
        const projectResponse = await projectService.detail(projectId);
        setRole(viewerRole(projectResponse.data.data.viewer));
        const issuesResponse = await issueService.list(projectId);
        setProjectIssues(issuesResponse.data.data.issues);
        setPageState("ready");
        return;
      }

      const [projectResponse, docResponse, issuesOfDocResponse, linksResponse, projectIssuesResponse] =
        await Promise.all([
          projectService.detail(projectId),
          documentService.detail(docId),
          documentService.issuesOfDocument(docId),
          documentService.listLinks(projectId),
          issueService.list(projectId),
        ]);

      setPageError(null);
      setRole(viewerRole(projectResponse.data.data.viewer));
      const doc = docResponse.data.data.document;
      setTitle(doc.title);
      setType(doc.type);
      setContent(doc.content);
      setAuthorUserId(doc.authorUserId);
      setTrashed(doc.trashedAt !== null);
      setLinkedIssues(issuesOfDocResponse.data.data.issues);
      const map: Record<number, number> = {};
      for (const link of linksResponse.data.data.links) {
        if (link.documentId === doc.id) map[link.issueId] = link.id;
      }
      setLinkIdByIssueId(map);
      setProjectIssues(projectIssuesResponse.data.data.issues);
      setPageState("ready");
    } catch (error) {
      setPageError(invitationErrorMessage(error, "문서를 불러오지 못했습니다."));
      setPageState("error");
    }
  }, [docId, isNew, projectId]);

  useEffect(() => {
    (async () => {
      await load();
    })();
  }, [load]);

  const preview = useMemo(() => renderMarkdownPreview(content), [content]);

  const canTrash = role === "P" || role === "C" || authorUserId === user?.id;

  const unlinkedProjectIssues = useMemo(
    () => projectIssues.filter((issue) => !linkedIssues.some((linked) => linked.id === issue.id)),
    [projectIssues, linkedIssues]
  );

  const save = async () => {
    if (saving || !title.trim() || !content.trim()) return;
    setSaving(true);
    setSaveError(null);
    try {
      if (isNew) {
        const { data } = await documentService.create(projectId, {
          title: title.trim(),
          type,
          content,
        });
        router.push(`/${companyId}/${projectId}/docs/${data.data.document.id}`);
        return;
      }
      const { data } = await documentService.update(docId, { title: title.trim(), type, content });
      setTitle(data.data.document.title);
      setType(data.data.document.type);
      setContent(data.data.document.content);
      setSavedAt(new Date().toLocaleTimeString("ko-KR", { hour: "2-digit", minute: "2-digit" }));
    } catch (error) {
      setSaveError(invitationErrorMessage(error, "문서를 저장하지 못했습니다."));
    } finally {
      setSaving(false);
    }
  };

  const linkIssue = async () => {
    const issueId = Number(pickedIssueId);
    if (!issueId || linkBusy) return;
    setLinkError(null);
    setLinkBusy(true);
    try {
      await documentService.createLink(projectId, { documentId: Number(docId), issueId });
      const issue = projectIssues.find((item) => item.id === issueId);
      if (issue) setLinkedIssues((prev) => [...prev, issue]);
      const linksResponse = await documentService.listLinks(projectId);
      const map: Record<number, number> = {};
      for (const link of linksResponse.data.data.links) {
        if (link.documentId === Number(docId)) map[link.issueId] = link.id;
      }
      setLinkIdByIssueId(map);
      setPickedIssueId("");
    } catch (error) {
      setLinkError(invitationErrorMessage(error, "이슈 연결에 실패했습니다."));
    } finally {
      setLinkBusy(false);
    }
  };

  const unlinkIssue = async (issueId: number) => {
    const linkId = linkIdByIssueId[issueId];
    if (!linkId || linkBusy) return;
    setLinkError(null);
    setLinkBusy(true);
    try {
      await documentService.deleteLink(linkId);
      setLinkedIssues((prev) => prev.filter((issue) => issue.id !== issueId));
      setLinkIdByIssueId((prev) => {
        const next = { ...prev };
        delete next[issueId];
        return next;
      });
    } catch (error) {
      setLinkError(invitationErrorMessage(error, "이슈 연결 해제에 실패했습니다."));
    } finally {
      setLinkBusy(false);
    }
  };

  const trashDoc = async () => {
    if (saving) return;
    setSaveError(null);
    setSaving(true);
    try {
      await documentService.trash(docId);
      setTrashed(true);
    } catch (error) {
      setSaveError(invitationErrorMessage(error, "문서를 휴지통으로 이동하지 못했습니다."));
    } finally {
      setSaving(false);
    }
  };

  if (pageState === "loading") {
    return <div className="mx-auto max-w-3xl px-6 py-10 text-sm text-muted">문서를 불러오는 중...</div>;
  }

  if (pageState === "error") {
    return (
      <div className="mx-auto max-w-3xl px-6 py-10">
        <Link
          href={`/${companyId}/${projectId}/docs`}
          className="text-sm text-muted transition-colors duration-fast hover:text-primary"
        >
          ← 문서 목록
        </Link>
        <div className="mt-4 rounded-lg border border-destructive/30 bg-destructive-subtle px-4 py-6 text-center text-sm text-destructive">
          {pageError}
        </div>
      </div>
    );
  }

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
        <Link
          href={`/${companyId}/${projectId}/docs`}
          className="text-sm text-muted transition-colors duration-fast hover:text-primary"
        >
          ← 문서 목록
        </Link>
        <div className="flex items-center gap-2">
          {savedAt && <span className="text-xs text-muted">{savedAt} 저장됨</span>}
          {!isNew && canTrash && (
            <button
              onClick={() => void trashDoc()}
              disabled={saving}
              className="rounded-md px-3 py-1.5 text-xs font-medium text-destructive hover:opacity-70 disabled:opacity-50"
            >
              휴지통으로
            </button>
          )}
          <button
            onClick={() => void save()}
            disabled={saving || !title.trim() || !content.trim()}
            className="rounded-md btn-gradient px-4 py-1.5 text-sm font-semibold text-primary-foreground disabled:cursor-not-allowed disabled:opacity-50"
          >
            {saving ? "저장 중..." : isNew ? "생성" : "저장"}
          </button>
        </div>
      </div>

      {saveError && <p className="mt-2 text-sm text-destructive">{saveError}</p>}

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

      {/* 관련 이슈 연결 — 신규 문서는 저장 후에만 연결 가능 */}
      {!isNew && (
        <section className="mt-6 rounded-xl border border-border bg-surface p-5">
          <h2 className="text-sm font-semibold text-foreground">관련 이슈</h2>
          <div className="mt-2 flex flex-wrap gap-2">
            {linkedIssues.map((issue) => (
              <span
                key={issue.id}
                className="flex items-center gap-1.5 rounded-full bg-accent-subtle px-3 py-1 text-xs font-medium text-accent"
              >
                {issue.issueKey}
                <button
                  onClick={() => void unlinkIssue(issue.id)}
                  disabled={linkBusy}
                  aria-label={`${issue.issueKey} 연결 해제`}
                  className="hover:opacity-60 disabled:opacity-40"
                >
                  ×
                </button>
              </span>
            ))}
            {linkedIssues.length === 0 && <span className="text-xs text-muted">연결된 이슈가 없습니다.</span>}
          </div>
          <div className="mt-3 flex gap-2">
            <select
              aria-label="연결할 이슈"
              value={pickedIssueId}
              onChange={(e) => setPickedIssueId(e.target.value)}
              className="flex-1 rounded-lg border border-border bg-surface px-3 py-2 text-sm text-foreground focus:border-ring focus:outline-none"
            >
              <option value="">이슈 선택</option>
              {unlinkedProjectIssues.map((issue) => (
                <option key={issue.id} value={issue.id}>
                  {issue.issueKey} · {issue.title}
                </option>
              ))}
            </select>
            <button
              onClick={() => void linkIssue()}
              disabled={!pickedIssueId || linkBusy}
              className="rounded-lg border border-border px-3 py-2 text-sm font-medium text-foreground hover:bg-primary-subtle disabled:opacity-50"
            >
              연결
            </button>
          </div>
          {linkError && <p className="mt-2 text-xs text-destructive">{linkError}</p>}
        </section>
      )}

      {/* 하단 액션: 쓰레드 / 그래프 뷰 */}
      {!isNew && (
        <div className="mt-4 flex gap-4 text-sm">
          <Link
            href={`/${companyId}/${projectId}/threads/open?targetType=DOCUMENT&targetId=${docId}`}
            className="font-medium text-primary hover:opacity-70"
          >
            쓰레드 열기 →
          </Link>
          <Link
            href={`/${companyId}/${projectId}/graph`}
            className="font-medium text-primary hover:opacity-70"
          >
            그래프 뷰에서 보기 →
          </Link>
        </div>
      )}
    </div>
  );
}
