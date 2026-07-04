"use client";

/*
 * S07 — 문서·회의록 목록 / 그래프 뷰
 *
 * 접근 사용자: A/P/C. 화면정의서(docs/03-design/화면정의서.md §S07) 기준
 * 목록 뷰(문서/회의록 목록, 열기, 새 문서, 휴지통)와 그래프 뷰(문서·이슈
 * 연결관계 노드/엣지, 노드 클릭 시 화면 이동 없이 우측 서랍 패널 미리보기)를
 * 탭으로 전환한다.
 *
 * 설계 메모 반영:
 *   - 탭 전환 시 그래프 캔버스가 리로드되지 않도록, 두 뷰를 항상 마운트해두고
 *     `hidden` 클래스로만 토글한다(언마운트 안 함 → 상태 보존).
 *   - 그래프는 별도 라이브러리 없이(react-flow 등 신규 의존성 추가 없이)
 *     노드 개수가 적은(목업 기준) 상황에 맞춰 SVG로 직접 그렸다. 실제 문서/이슈
 *     규모가 커지면 전용 그래프 라이브러리 도입을 재검토해야 한다.
 *
 * db/schema.sql 기준 document_type enum: DOC/MEETING_NOTE.
 *
 * ⚠️ MOCK: 문서/그래프 API(`GET /projects/{projectId}/documents`,
 * `/documents/graph`, `/documents/{id}/trash`)가 아직 없어서 로컬 state로
 * 흉내낸다.
 */
import { useMemo, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";

type DocType = "DOC" | "MEETING_NOTE";
type Tab = "list" | "graph";

type DocItem = {
  id: string;
  title: string;
  type: DocType;
  authorName: string;
  updatedAt: string;
  linkedIssueKeys: string[];
  trashedAt: string | null;
};

const TYPE_LABEL: Record<DocType, string> = {
  DOC: "문서",
  MEETING_NOTE: "회의록",
};

const MOCK_DOCS: DocItem[] = [
  { id: "d1", title: "Sprint 2 킥오프 회의록", type: "MEETING_NOTE", authorName: "정철진", updatedAt: "2026-06-29", linkedIssueKeys: ["THR-101"], trashedAt: null },
  { id: "d2", title: "회사/프로젝트 API 설계", type: "DOC", authorName: "여도현", updatedAt: "2026-07-01", linkedIssueKeys: ["THR-101", "THR-102"], trashedAt: null },
  { id: "d3", title: "칸반 보드 UX 노트", type: "DOC", authorName: "김민정", updatedAt: "2026-07-02", linkedIssueKeys: [], trashedAt: null },
  { id: "d4", title: "S3 연동 조사", type: "DOC", authorName: "황스일", updatedAt: "2026-07-02", linkedIssueKeys: ["THR-103"], trashedAt: null },
];

// 그래프 뷰용 미리보기 정보(이슈 노드) — 실제로는 documents/graph API가 내려줘야 함
const ISSUE_PREVIEW: Record<string, { title: string; status: string }> = {
  "THR-101": { title: "로그인 화면 반응형 깨짐", status: "검토중" },
  "THR-102": { title: "허브 상단바 알림 뱃지 API 연동", status: "할일" },
  "THR-103": { title: "회사 로고 업로드 S3 연동", status: "백로그" },
};

type GraphNode = { id: string; kind: "doc" | "issue"; label: string; x: number; y: number };

export default function DocsPage() {
  const params = useParams<{ companyId: string; projectId: string }>();
  const { companyId, projectId } = params;

  const [docs, setDocs] = useState<DocItem[]>(MOCK_DOCS);
  const [tab, setTab] = useState<Tab>("list");
  const [showTrash, setShowTrash] = useState(false);
  const [selectedNode, setSelectedNode] = useState<GraphNode | null>(null);

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

  // 노드 배치: 문서는 왼쪽 열, 이슈는 오른쪽 열에 세로로 나열하는 간단한 2열 레이아웃.
  const { nodes, edges } = useMemo(() => {
    const issueKeys = Array.from(new Set(activeDocs.flatMap((d) => d.linkedIssueKeys)));
    const docNodes: GraphNode[] = activeDocs.map((d, i) => ({
      id: `doc:${d.id}`,
      kind: "doc",
      label: d.title,
      x: 90,
      y: 60 + i * 70,
    }));
    const issueNodes: GraphNode[] = issueKeys.map((key, i) => ({
      id: `issue:${key}`,
      kind: "issue",
      label: key,
      x: 430,
      y: 60 + i * 70,
    }));
    const edgeList = activeDocs.flatMap((d) =>
      d.linkedIssueKeys.map((key) => ({ from: `doc:${d.id}`, to: `issue:${key}` }))
    );
    return { nodes: [...docNodes, ...issueNodes], edges: edgeList };
  }, [activeDocs]);

  const findNode = (id: string) => nodes.find((n) => n.id === id);

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
          className="rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground transition-colors duration-fast hover:bg-primary-hover"
        >
          + 새 문서
        </Link>
      </div>

      {/* 탭 */}
      <div className="mt-4 flex gap-1 rounded-lg bg-border/40 p-1 w-fit">
        {(["list", "graph"] as Tab[]).map((t) => (
          <button
            key={t}
            onClick={() => setTab(t)}
            className={
              "rounded-md px-3 py-1.5 text-sm font-medium transition-colors duration-fast " +
              (tab === t ? "bg-surface text-primary shadow-sm" : "text-muted hover:text-foreground")
            }
          >
            {t === "list" ? "목록 뷰" : "그래프 뷰"}
          </button>
        ))}
      </div>

      {/* 목록 뷰 — hidden으로만 토글, 언마운트하지 않음 */}
      <div className={tab === "list" ? "" : "hidden"}>
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
              <li
                key={doc.id}
                className="flex items-center justify-between px-3 py-2 text-sm"
              >
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

      {/* 그래프 뷰 — hidden으로만 토글, 언마운트하지 않아서 selectedNode 등 상태가 유지됨 */}
      <div className={tab === "graph" ? "mt-4 flex gap-4" : "hidden"}>
        <svg
          viewBox="0 0 520 320"
          className="h-80 flex-1 rounded-xl border border-border bg-surface"
        >
          {edges.map((edge, i) => {
            const from = findNode(edge.from);
            const to = findNode(edge.to);
            if (!from || !to) return null;
            return (
              <line
                key={i}
                x1={from.x}
                y1={from.y}
                x2={to.x}
                y2={to.y}
                stroke="var(--color-border)"
                strokeWidth={2}
              />
            );
          })}
          {nodes.map((node) => (
            <g
              key={node.id}
              onClick={() => setSelectedNode(node)}
              className="cursor-pointer"
            >
              <circle
                cx={node.x}
                cy={node.y}
                r={28}
                className={
                  node.kind === "doc"
                    ? selectedNode?.id === node.id
                      ? "fill-primary"
                      : "fill-primary/70"
                    : selectedNode?.id === node.id
                      ? "fill-accent"
                      : "fill-accent/70"
                }
              />
              <text
                x={node.x}
                y={node.y + 45}
                textAnchor="middle"
                className="fill-foreground text-[10px]"
              >
                {node.label.length > 14 ? node.label.slice(0, 14) + "…" : node.label}
              </text>
            </g>
          ))}
        </svg>

        {/* 우측 서랍 패널 — 화면 이동 없이 미리보기 */}
        <div className="w-56 shrink-0 rounded-xl border border-border bg-surface p-4">
          {selectedNode ? (
            selectedNode.kind === "doc" ? (
              (() => {
                const doc = activeDocs.find((d) => `doc:${d.id}` === selectedNode.id);
                if (!doc) return null;
                return (
                  <div>
                    <span className="rounded-full bg-border px-2 py-0.5 text-[10px] font-medium text-muted">
                      {TYPE_LABEL[doc.type]}
                    </span>
                    <p className="mt-2 text-sm font-semibold text-foreground">{doc.title}</p>
                    <p className="mt-1 text-xs text-muted">{doc.authorName} · {doc.updatedAt}</p>
                    <p className="mt-2 text-xs text-muted">연결 이슈: {doc.linkedIssueKeys.join(", ") || "없음"}</p>
                    <Link
                      href={`/${companyId}/${projectId}/docs/${doc.id}`}
                      className="mt-3 inline-block text-xs font-medium text-primary hover:opacity-70"
                    >
                      문서 열기 →
                    </Link>
                  </div>
                );
              })()
            ) : (
              (() => {
                const key = selectedNode.label;
                const preview = ISSUE_PREVIEW[key];
                return (
                  <div>
                    <span className="rounded-full bg-accent-subtle px-2 py-0.5 text-[10px] font-medium text-accent">
                      이슈
                    </span>
                    <p className="mt-2 font-mono text-xs text-muted">{key}</p>
                    <p className="text-sm font-semibold text-foreground">{preview?.title ?? "-"}</p>
                    <p className="mt-1 text-xs text-muted">상태: {preview?.status ?? "-"}</p>
                    <Link
                      href={`/${companyId}/${projectId}/board`}
                      className="mt-3 inline-block text-xs font-medium text-primary hover:opacity-70"
                    >
                      보드에서 보기 →
                    </Link>
                  </div>
                );
              })()
            )
          ) : (
            <p className="text-xs text-muted">노드를 클릭하면 미리보기가 여기 표시됩니다.</p>
          )}
        </div>
      </div>
    </div>
  );
}
