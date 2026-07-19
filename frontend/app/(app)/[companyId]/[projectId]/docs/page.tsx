"use client";

/*
 * S07 — 문서·회의록 목록
 *
 * 접근 사용자: A/P/C. 화면정의서(docs/03-design/화면정의서.md §S07) 기준
 * 문서/회의록 목록, 열기, 새 문서, 휴지통을 구현한다.
 *
 * WBS 4.6.2 — 4.6.1 Backend(PR #101)의 실 API로 연동했다. 그래프 뷰는
 * ../graph/page.tsx로 승격되어 있어 이 화면은 목록/휴지통만 다룬다.
 *
 * 휴지통 이동 권한은 API 계약상 "작성자 또는 P/C"라 문서별로 버튼 노출을
 * 다르게 한다(board/page.tsx의 viewerRole 패턴과 동일).
 */
import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { useAuth } from "@/contexts/AuthContext";
import { invitationErrorMessage } from "@/lib/invitation";
import { documentService } from "@/services/document.service";
import { projectService } from "@/services/project.service";
import type { DocumentResponse, DocumentType } from "@/types/document.types";

type Role = "A" | "P" | "C";
type LoadState = "loading" | "ready" | "error";

const TYPE_LABEL: Record<DocumentType, string> = {
  DOC: "문서",
  MEETING_NOTE: "회의록",
};

function viewerRole(viewer: { companyRole: string | null; projectRole: string | null }): Role {
  if (viewer.companyRole === "COMPANY_ADMIN") return "C";
  return viewer.projectRole === "PROJECT_ADMIN" ? "P" : "A";
}

export default function DocsPage() {
  const params = useParams<{ companyId: string; projectId: string }>();
  const { companyId, projectId } = params;
  const { user } = useAuth();

  const [pageState, setPageState] = useState<LoadState>("loading");
  const [pageError, setPageError] = useState<string | null>(null);
  const [docs, setDocs] = useState<DocumentResponse[]>([]);
  const [trashedDocs, setTrashedDocs] = useState<DocumentResponse[]>([]);
  const [role, setRole] = useState<Role | null>(null);
  const [showTrash, setShowTrash] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<number | null>(null);

  const load = useCallback(async () => {
    try {
      const [projectResponse, activeResponse, trashedResponse] = await Promise.all([
        projectService.detail(projectId),
        documentService.list(projectId),
        documentService.list(projectId, { includeTrashed: true }),
      ]);
      setPageError(null);
      setRole(viewerRole(projectResponse.data.data.viewer));
      setDocs(activeResponse.data.data.documents);
      setTrashedDocs(
        trashedResponse.data.data.documents.filter((doc) => doc.trashedAt !== null)
      );
      setPageState("ready");
    } catch (error) {
      setPageError(invitationErrorMessage(error, "문서 목록을 불러오지 못했습니다."));
      setPageState("error");
    }
  }, [projectId]);

  useEffect(() => {
    (async () => {
      await load();
    })();
  }, [load]);

  const canTrash = (doc: DocumentResponse) =>
    role === "P" || role === "C" || doc.authorUserId === user?.id;

  const trashDoc = async (doc: DocumentResponse) => {
    if (busyId !== null) return;
    setActionError(null);
    setBusyId(doc.id);
    try {
      await documentService.trash(doc.id);
      await load();
    } catch (error) {
      setActionError(invitationErrorMessage(error, "문서를 휴지통으로 이동하지 못했습니다."));
    } finally {
      setBusyId(null);
    }
  };

  const restoreDoc = async (doc: DocumentResponse) => {
    if (busyId !== null) return;
    setActionError(null);
    setBusyId(doc.id);
    try {
      await documentService.restore(doc.id);
      await load();
    } catch (error) {
      setActionError(invitationErrorMessage(error, "문서를 복원하지 못했습니다."));
    } finally {
      setBusyId(null);
    }
  };

  if (pageState === "loading") {
    return <div className="px-6 py-8 text-sm text-muted">문서 목록을 불러오는 중...</div>;
  }

  if (pageState === "error") {
    return (
      <div className="mx-auto max-w-3xl px-6 py-10">
        <Link
          href={`/${companyId}/projects`}
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
    <div className="mx-auto max-w-3xl px-6 py-10">
      <Link
        href={`/${companyId}/projects`}
        className="text-sm text-muted transition-colors duration-fast hover:text-primary"
      >
        ← 프로젝트 목록
      </Link>
      <div className="mt-3 flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-foreground">문서·회의록</h1>
          <p className="mt-1 text-sm text-muted">프로젝트 {projectId}</p>
        </div>
        <Link
          href={`/${companyId}/${projectId}/docs/new`}
          className="rounded-lg btn-gradient px-4 py-2 text-sm font-semibold text-primary-foreground transition-colors duration-fast"
        >
          + 새 문서
        </Link>
      </div>

      {actionError && <p className="mt-4 text-sm text-destructive">{actionError}</p>}

      <ul className="mt-4 flex flex-col gap-2">
        {docs.map((doc) => (
          <li
            key={doc.id}
            className="flex items-center justify-between rounded-xl bg-surface px-4 py-3 shadow-sm transition-shadow duration-fast hover:shadow-md"
          >
            <Link href={`/${companyId}/${projectId}/docs/${doc.id}`} className="flex-1">
              <div className="flex items-center gap-2">
                <span className="rounded-full bg-border px-2 py-0.5 text-[10px] font-medium text-muted">
                  {TYPE_LABEL[doc.type]}
                </span>
                <p className="text-sm font-semibold text-foreground">{doc.title}</p>
              </div>
              <p className="mt-1 text-xs text-muted">
                {new Date(doc.updatedAt).toLocaleDateString("ko-KR")} 수정
              </p>
            </Link>
            {canTrash(doc) && (
              <button
                onClick={() => void trashDoc(doc)}
                disabled={busyId === doc.id}
                className="text-xs font-medium text-destructive transition-colors duration-fast hover:opacity-70 disabled:opacity-50"
              >
                휴지통으로
              </button>
            )}
          </li>
        ))}
        {docs.length === 0 && (
          <li className="rounded-xl border border-dashed border-border px-4 py-10 text-center text-sm text-muted">
            문서가 없습니다.
          </li>
        )}
      </ul>

      <button
        onClick={() => setShowTrash((v) => !v)}
        className="mt-4 text-sm font-medium text-muted transition-colors duration-fast hover:text-primary"
      >
        휴지통 ({trashedDocs.length})
      </button>
      {showTrash && (
        <ul className="mt-2 divide-y divide-border rounded-lg border border-border">
          {trashedDocs.map((doc) => (
            <li key={doc.id} className="flex items-center justify-between px-3 py-2 text-sm">
              <span>{doc.title}</span>
              {canTrash(doc) && (
                <button
                  onClick={() => void restoreDoc(doc)}
                  disabled={busyId === doc.id}
                  className="text-xs font-medium text-primary hover:opacity-70 disabled:opacity-50"
                >
                  복원
                </button>
              )}
            </li>
          ))}
          {trashedDocs.length === 0 && (
            <li className="text-center text-xs text-muted">휴지통이 비어 있습니다.</li>
          )}
        </ul>
      )}
    </div>
  );
}
