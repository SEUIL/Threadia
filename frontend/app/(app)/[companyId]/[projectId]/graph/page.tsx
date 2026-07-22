"use client";

/*
 * 프로젝트 그래프 — 문서·이슈 연결 그래프 뷰 (S07 그래프 뷰)
 *
 * 원래 docs/page.tsx(S07) 안의 "그래프 뷰" 탭이었다. 프로젝트에 들어왔을 때
 * 가장 먼저 보이는 화면(프로젝트 탭의 첫 번째, 프로젝트 카드의 기본 진입점)
 * 으로 승격해서 화면 전체를 채우는 메인 화면으로 다시 잡았다.
 *
 * WBS 4.6.2 — 4.6.1 Backend(PR #101)의 `GET /projects/{projectId}/documents/graph`
 * 로 연동했다. 응답은 DOCUMENT/ISSUE node와 documentId→issueId edge만 주므로
 * (API-명세서 §13.6) 채팅 노드 통합(이전 mock에 있던 "chat" kind)은 제외했다
 * — 메시지→이슈 연결(LINK-03, WBS 4.5)이 구현되면 별도로 다시 반영한다.
 */
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { invitationErrorMessage } from "@/lib/invitation";
import { documentService } from "@/services/document.service";
import {
  ALL_KINDS,
  CARD_ANCHOR_Y,
  CARD_WIDTH,
  GRAPH_CANVAS_HEIGHT,
  GRAPH_CANVAS_WIDTH,
  KIND_CHIP_CLASS,
  KIND_LABEL,
  KIND_RING_CLASS,
  layoutColumn,
  type GraphNode,
  type NodeKind,
} from "./graphLayout";

type LoadState = "loading" | "ready" | "error";

const STATUS_LABEL: Record<string, string> = {
  BACKLOG: "백로그",
  TODO: "할일",
  IN_PROGRESS: "진행중",
  IN_REVIEW: "검토중",
  DONE: "완료",
};

export default function ProjectGraphPage() {
  const params = useParams<{ companyId: string; projectId: string }>();
  const { companyId, projectId } = params;

  const [pageState, setPageState] = useState<LoadState>("loading");
  const [pageError, setPageError] = useState<string | null>(null);
  const [allNodes, setAllNodes] = useState<GraphNode[]>([]);
  const [allEdges, setAllEdges] = useState<{ from: string; to: string }[]>([]);

  const [selectedNode, setSelectedNode] = useState<GraphNode | null>(null);
  const [activeKinds, setActiveKinds] = useState<NodeKind[]>(ALL_KINDS);
  const [query, setQuery] = useState("");

  // 캔버스를 스크롤바 대신 드래그(pan)로 이동하고, 휠로 확대/축소한다. 빈
  // 캔버스 영역에서 mousedown이 시작된 경우에만 pan하고, 노드 카드 위에서
  // 시작되면 노드 자체를 드래그해서 옮길 수 있다(따로 아래 onNodeMouseDown).
  const [pan, setPan] = useState({ x: 0, y: 0 });
  const [scale, setScale] = useState(1);
  const [isDragging, setIsDragging] = useState(false);
  const dragStart = useRef<{ x: number; y: number } | null>(null);
  const canvasSurfaceRef = useRef<HTMLDivElement | null>(null);

  // 노드별 위치 — layoutColumn() 계산값으로 초기화한 뒤, 드래그로 옮기면
  // 여기 override된 값을 우선 사용한다.
  const [positions, setPositions] = useState<Record<string, { x: number; y: number }>>({});
  const draggingNode = useRef<{
    id: string;
    startX: number;
    startY: number;
    origX: number;
    origY: number;
    moved: boolean;
  } | null>(null);

  const load = useCallback(async () => {
    try {
      const { data } = await documentService.graph(projectId);
      setPageError(null);

      const docNodesRaw = data.data.nodes.filter((n) => n.type === "DOCUMENT");
      const issueNodesRaw = data.data.nodes.filter((n) => n.type === "ISSUE");
      const docPositions = layoutColumn("doc", docNodesRaw.length);
      const issuePositions = layoutColumn("issue", issueNodesRaw.length);

      const docNodes: GraphNode[] = docNodesRaw.map((n, i) => ({
        id: `doc:${n.id}`,
        kind: "doc",
        label: n.title,
        issueKey: null,
        status: null,
        refId: n.id,
        ...docPositions[i],
      }));
      const issueNodes: GraphNode[] = issueNodesRaw.map((n, i) => ({
        id: `issue:${n.id}`,
        kind: "issue",
        label: n.issueKey ?? n.title,
        issueKey: n.issueKey,
        status: n.status,
        refId: n.id,
        ...issuePositions[i],
      }));

      const edgeList = data.data.edges.map((e) => ({
        from: `doc:${e.sourceDocumentId}`,
        to: `issue:${e.targetIssueId}`,
      }));

      setAllNodes([...docNodes, ...issueNodes]);
      setAllEdges(edgeList);
      setPageState("ready");
    } catch (error) {
      setPageError(invitationErrorMessage(error, "그래프 데이터를 불러오지 못했습니다."));
      setPageState("error");
    }
  }, [projectId]);

  useEffect(() => {
    (async () => {
      await load();
    })();
  }, [load]);

  const onCanvasMouseDown = (e: React.MouseEvent<HTMLDivElement>) => {
    if (e.target !== canvasSurfaceRef.current) return; // 노드 카드 위에서는 pan 시작 안 함
    dragStart.current = { x: e.clientX - pan.x, y: e.clientY - pan.y };
    setIsDragging(true);
  };
  const onCanvasMouseMove = (e: React.MouseEvent<HTMLDivElement>) => {
    if (draggingNode.current) {
      const dn = draggingNode.current;
      const dx = (e.clientX - dn.startX) / scale;
      const dy = (e.clientY - dn.startY) / scale;
      if (Math.abs(dx) > 3 || Math.abs(dy) > 3) dn.moved = true;
      setPositions((prev) => ({ ...prev, [dn.id]: { x: dn.origX + dx, y: dn.origY + dy } }));
      return;
    }
    if (!dragStart.current) return;
    setPan({ x: e.clientX - dragStart.current.x, y: e.clientY - dragStart.current.y });
  };
  const stopDragging = () => {
    dragStart.current = null;
    setIsDragging(false);
    if (draggingNode.current) {
      const dn = draggingNode.current;
      draggingNode.current = null;
      if (!dn.moved) {
        const node = findNode(dn.id);
        if (node) setSelectedNode(node);
      }
    }
  };

  const onNodeMouseDown = (e: React.MouseEvent, node: GraphNode) => {
    e.stopPropagation(); // 캔버스 pan이 같이 시작되지 않도록
    const pos = positions[node.id] ?? { x: node.x, y: node.y };
    draggingNode.current = {
      id: node.id,
      startX: e.clientX,
      startY: e.clientY,
      origX: pos.x,
      origY: pos.y,
      moved: false,
    };
  };

  // 마우스 커서가 가리키는 지점을 고정한 채 확대/축소한다.
  const onCanvasWheel = (e: React.WheelEvent<HTMLDivElement>) => {
    e.preventDefault();
    const rect = e.currentTarget.getBoundingClientRect();
    const cursorX = e.clientX - rect.left;
    const cursorY = e.clientY - rect.top;
    const canvasX = (cursorX - pan.x) / scale;
    const canvasY = (cursorY - pan.y) / scale;

    const nextScale = Math.min(2, Math.max(0.4, scale + (e.deltaY > 0 ? -0.1 : 0.1)));
    setPan({ x: cursorX - canvasX * nextScale, y: cursorY - canvasY * nextScale });
    setScale(nextScale);
  };

  // 타입 필터 + 제목/이름 검색(AND 조건) — 캔버스 크기(viewBox)는 전체 노드
  // 기준으로 고정해 필터를 켜고 끌 때 화면이 튀지 않게 한다.
  const { nodes, edges } = useMemo(() => {
    const q = query.trim().toLowerCase();
    const visible = allNodes.filter(
      (n) => activeKinds.includes(n.kind) && n.label.toLowerCase().includes(q)
    );
    const visibleIds = new Set(visible.map((n) => n.id));
    const visibleEdges = allEdges.filter((e) => visibleIds.has(e.from) && visibleIds.has(e.to));
    return { nodes: visible, edges: visibleEdges };
  }, [allNodes, allEdges, activeKinds, query]);

  const findNode = (id: string) => allNodes.find((n) => n.id === id);

  // 드래그로 옮긴 적 없는 노드는 layoutColumn() 계산 위치를 그대로 쓴다.
  const getPos = (node: GraphNode) => positions[node.id] ?? { x: node.x, y: node.y };

  const toggleKind = (kind: NodeKind) =>
    setActiveKinds((prev) => (prev.includes(kind) ? prev.filter((k) => k !== kind) : [...prev, kind]));

  // 드래그로 옮긴 위치를 전부 지워서 layoutColumn() 계산 위치로 되돌리고,
  // pan/zoom도 기본값으로 리셋한다.
  const resetLayout = () => {
    setPositions({});
    setPan({ x: 0, y: 0 });
    setScale(1);
  };

  if (pageState === "loading") {
    return <div className="px-6 py-8 text-sm text-muted">그래프를 불러오는 중...</div>;
  }

  if (pageState === "error") {
    return (
      <div className="px-6 py-8">
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
    <div className="flex h-full flex-col px-6 py-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-foreground">그래프</h1>
          <p className="mt-1 text-sm text-muted">
            프로젝트 {projectId} · 문서와 이슈가 어떻게 연결돼 있는지 한눈에 봅니다.
          </p>
        </div>
      </div>

      {/* 타입 필터 칩 + 검색 — AND 조건으로 함께 적용 */}
      <div className="mt-4 flex flex-wrap items-center gap-2">
        {ALL_KINDS.map((kind) => {
          const active = activeKinds.includes(kind);
          return (
            <button
              key={kind}
              onClick={() => toggleKind(kind)}
              className={
                "flex items-center gap-1.5 rounded-full border px-3 py-1 text-xs font-semibold transition-colors duration-fast " +
                (active ? KIND_CHIP_CLASS[kind] + " border-transparent" : "border-border text-muted hover:text-foreground")
              }
            >
              {KIND_LABEL[kind]}
            </button>
          );
        })}
        <input
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="제목/이슈 키로 검색"
          className="ml-auto w-56 rounded-full border border-border bg-surface px-3 py-1.5 text-xs text-foreground placeholder-placeholder outline-none focus:border-ring"
        />
        <button
          onClick={resetLayout}
          title="드래그로 옮긴 위치와 확대/축소를 초기 배치로 되돌립니다"
          className="shrink-0 rounded-full border border-border px-3 py-1.5 text-xs font-semibold text-muted transition-colors duration-fast hover:text-foreground"
        >
          자동 정렬
        </button>
      </div>

      {/* 캔버스 — 화면 전체를 채우는 메인 그래프. 카드형 노드 + 점선 연결 */}
      <div className="mt-4 flex min-h-0 flex-1 gap-4">
        <div
          className="relative min-h-0 flex-1 select-none overflow-hidden rounded-xl border border-border"
          style={{
            backgroundImage:
              "radial-gradient(circle at 1px 1px, var(--color-border) 1px, transparent 0)",
            backgroundSize: "22px 22px",
            backgroundColor: "var(--color-surface)",
            cursor: isDragging ? "grabbing" : "grab",
          }}
          onMouseDown={onCanvasMouseDown}
          onMouseMove={onCanvasMouseMove}
          onMouseUp={stopDragging}
          onMouseLeave={stopDragging}
          onWheel={onCanvasWheel}
        >
          <div
            ref={canvasSurfaceRef}
            className="absolute"
            style={{
              width: GRAPH_CANVAS_WIDTH,
              height: GRAPH_CANVAS_HEIGHT,
              transform: `translate(${pan.x}px, ${pan.y}px) scale(${scale})`,
              transformOrigin: "0 0",
            }}
          >
            <svg
              className="pointer-events-none absolute inset-0"
              width={GRAPH_CANVAS_WIDTH}
              height={GRAPH_CANVAS_HEIGHT}
            >
              {edges.map((edge, i) => {
                const from = findNode(edge.from);
                const to = findNode(edge.to);
                if (!from || !to) return null;
                const fromPos = getPos(from);
                const toPos = getPos(to);
                return (
                  <line
                    key={i}
                    x1={fromPos.x + CARD_WIDTH / 2}
                    y1={fromPos.y + CARD_ANCHOR_Y}
                    x2={toPos.x + CARD_WIDTH / 2}
                    y2={toPos.y + CARD_ANCHOR_Y}
                    stroke="var(--color-accent)"
                    strokeWidth={1.5}
                    strokeOpacity={0.35}
                    strokeDasharray="4 3"
                  />
                );
              })}
            </svg>

            {nodes.map((node) => {
              const pos = getPos(node);
              return (
                <div
                  key={node.id}
                  role="button"
                  tabIndex={0}
                  onMouseDown={(e) => onNodeMouseDown(e, node)}
                  onKeyDown={(e) => {
                    if (e.key === "Enter" || e.key === " ") setSelectedNode(node);
                  }}
                  className={
                    "absolute cursor-grab rounded-xl border border-border bg-surface p-3 text-left shadow-sm transition hover:shadow-md active:cursor-grabbing " +
                    (selectedNode?.id === node.id ? "ring-2 " + KIND_RING_CLASS[node.kind] : "")
                  }
                  style={{ left: pos.x, top: pos.y, width: CARD_WIDTH }}
                >
                  <span
                    className={"rounded-full px-2 py-0.5 text-[10px] font-bold " + KIND_CHIP_CLASS[node.kind]}
                  >
                    {KIND_LABEL[node.kind]}
                  </span>
                  <p className="mt-2 truncate text-xs font-semibold text-foreground">{node.label}</p>
                </div>
              );
            })}

            {nodes.length === 0 && (
              <p className="absolute inset-0 grid place-items-center text-xs text-muted">
                조건에 맞는 노드가 없습니다
              </p>
            )}
          </div>
        </div>

        {/* 우측 서랍 패널 — 화면 이동 없이 미리보기 (문서/이슈 2분기) */}
        <div className="w-64 shrink-0 overflow-y-auto rounded-xl border border-border bg-surface p-4">
          {selectedNode && !nodes.some((n) => n.id === selectedNode.id) ? (
            <p className="text-xs text-muted">선택한 노드가 현재 필터 조건에서 숨겨졌습니다.</p>
          ) : selectedNode ? (
            selectedNode.kind === "doc" ? (
              <div>
                <span className="rounded-full bg-primary-subtle px-2 py-0.5 text-[10px] font-medium text-primary">
                  문서
                </span>
                <p className="mt-2 text-sm font-semibold text-foreground">{selectedNode.label}</p>
                <Link
                  href={`/${companyId}/${projectId}/docs/${selectedNode.refId}`}
                  className="mt-3 inline-block text-xs font-medium text-primary hover:opacity-70"
                >
                  문서 열기 →
                </Link>
              </div>
            ) : (
              <div>
                <span className="rounded-full bg-accent-subtle px-2 py-0.5 text-[10px] font-medium text-accent">
                  이슈
                </span>
                <p className="mt-2 font-mono text-xs text-muted">{selectedNode.issueKey}</p>
                <p className="mt-1 text-xs text-muted">
                  상태: {(selectedNode.status && STATUS_LABEL[selectedNode.status]) ?? "-"}
                </p>
                <Link
                  href={`/${companyId}/${projectId}/board`}
                  className="mt-3 inline-block text-xs font-medium text-primary hover:opacity-70"
                >
                  보드에서 보기 →
                </Link>
              </div>
            )
          ) : (
            <p className="text-xs text-muted">노드를 클릭하면 미리보기가 여기 표시됩니다.</p>
          )}
        </div>
      </div>
    </div>
  );
}
