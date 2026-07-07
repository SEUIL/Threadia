"use client";

/*
 * S07 — 문서·회의록 목록
 *
 * 접근 사용자: A/P/C. 화면정의서(docs/03-design/화면정의서.md §S07) 기준
 * 문서/회의록 목록, 열기, 새 문서, 휴지통을 구현한다.
 *
 * ⚠️ 원래 이 화면에 "그래프 뷰" 탭이 있었다 — 문서·이슈·채팅 통합 그래프로
 * 확장하면서 프로젝트 진입 시 기본으로 보이는 별도 화면(../graph/page.tsx)
 * 으로 승격했다. 목업 타입/데이터(DocItem, MOCK_DOCS 등)는 그래프 화면과
 * ./mockGraphData.ts를 공유하지만, 이 화면은 자체 useState로 휴지통 이동/복원
 * 등 상태를 관리하므로 그래프 화면과 실시간으로 동기화되지는 않는다(실 API
 * 연동 전까지의 알려진 제약).
 *
 * db/schema.sql 기준 document_type enum: DOC/MEETING_NOTE.
 *
 * ⚠️ MOCK: 문서 API(`GET /projects/{projectId}/documents`,
 * `/documents/{id}/trash`)가 아직 없어서 로컬 state로 흉내낸다.
 */
import { useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { MOCK_DOCS, TYPE_LABEL, type DocItem } from "../graph/mockGraphData";

export default function DocsPage() {
  const params = useParams<{ companyId: string; projectId: string }>();
  const { companyId, projectId } = params;

  const [docs, setDocs] = useState<DocItem[]>(MOCK_DOCS);
  const [showTrash, setShowTrash] = useState(false);

  const activeDocs = docs.filter((d) => !d.trashedAt);
  const trashedDocs = docs.filter((d) => d.trashedAt);

  const trashDoc = (id: string) => {
    // TODO: POST /api/v1/documents/{documentId}/trash
    setDocs((prev) => prev.map((d) => (d.id === id ? { ...d, trashedAt: new Date().toISOString() } : d)));
  };

  const restoreDoc = (id: string) => {
    // TODO: POST /api/v1/documents/{documentId}/restore
    setDocs((prev) => prev.map((d) => (d.id === id ? { ...d, trashedAt: null } : d)));
  };

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

      <ul className="mt-4 flex flex-col gap-2">
        {activeDocs.map((doc) => (
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
                {doc.authorName} · {doc.updatedAt} · 연결 이슈 {doc.linkedIssueKeys.length}개
              </p>
            </Link>
            <button
              onClick={() => trashDoc(doc.id)}
              className="text-xs font-medium text-destructive transition-colors duration-fast hover:opacity-70"
            >
              휴지통으로
            </button>
          </li>
        ))}
        {activeDocs.length === 0 && (
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
              <button
                onClick={() => restoreDoc(doc.id)}
                className="text-xs font-medium text-primary hover:opacity-70"
              >
                복원
              </button>
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
