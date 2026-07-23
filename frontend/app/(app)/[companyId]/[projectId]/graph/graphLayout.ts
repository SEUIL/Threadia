/*
 * 그래프 화면(문서/이슈 연결)의 카드 배치 상수와 유틸리티.
 *
 * WBS 4.6.2 — 이전에는 이 파일이 mockGraphData.ts로, 문서/채팅 목업 데이터도
 * 함께 들고 있었다. 실 API(`GET /projects/{projectId}/documents/graph`)는
 * DOCUMENT/ISSUE node만 반환하므로(API-명세서 §13.6) 채팅 노드 통합은 범위
 * 밖이다 — 메시지→이슈 연결(LINK-03, WBS 4.5)이 별도로 구현된 뒤 그래프에도
 * 반영해야 한다.
 */

export type NodeKind = "doc" | "issue";

export const KIND_LABEL: Record<NodeKind, string> = {
  doc: "문서",
  issue: "이슈",
};

// 노드 배지/칩 색 — doc은 기존부터 primary(teal), issue는 기존부터
// accent(violet)를 써왔다(이 화면의 기존 관례).
export const KIND_CHIP_CLASS: Record<NodeKind, string> = {
  doc: "bg-primary text-primary-foreground",
  issue: "bg-accent text-accent-foreground",
};
// 선택된 카드 강조 링 색 — 배지 색과 동일 계열.
export const KIND_RING_CLASS: Record<NodeKind, string> = {
  doc: "ring-primary",
  issue: "ring-accent",
};
export const ALL_KINDS: NodeKind[] = ["doc", "issue"];

export type GraphNode = {
  id: string;
  kind: NodeKind;
  label: string;
  x: number;
  y: number;
  issueKey: string | null;
  status: string | null;
  refId: number;
};

// 카드형 노드 크기 — 원(circle) 대신 배지+제목이 있는 카드로 그린다(2026-07-06
// 디자인 변경, 스크린샷 레퍼런스 반영). x/y는 카드의 좌상단 좌표다.
export const CARD_WIDTH = 190;
export const CARD_ANCHOR_Y = 34; // 연결선이 붙는 카드 세로 중심 지점(대략치)

// 종류별 열 배치 — 문서/이슈를 각각 세로로 나열하되, 한 열에 카드가
// ROWS_PER_SUBCOLUMN개를 넘으면 옆으로 서브열을 만들어 무한정 아래로만
// 길어지지 않게 한다(카드 수가 늘어나도 캔버스 높이가 고정 유지됨).
const DOC_BASE_X = 40;
const ROWS_PER_SUBCOLUMN = 6;
const ROW_HEIGHT = 100;
const SUBCOLUMN_GAP = CARD_WIDTH + 20;
const ROW_START_Y = 40;
// 문서 그룹의 마지막 서브열과 이슈 그룹의 첫 서브열 사이 여백. 문서가
// ROWS_PER_SUBCOLUMN개 이하(서브열 1개)일 때 기존 고정 배치(문서 baseX=40,
// 이슈 baseX=380)와 정확히 같은 간격이 나오도록 역산한 값이다.
const GROUP_GAP = 150;

function subcolumnsNeeded(count: number): number {
  return Math.max(1, Math.ceil(count / ROWS_PER_SUBCOLUMN));
}

function columnPositions(baseX: number, count: number): { x: number; y: number }[] {
  return Array.from({ length: count }, (_, i) => {
    const row = i % ROWS_PER_SUBCOLUMN;
    const col = Math.floor(i / ROWS_PER_SUBCOLUMN);
    return { x: baseX + col * SUBCOLUMN_GAP, y: ROW_START_Y + row * ROW_HEIGHT };
  });
}

/*
 * 문서 카드가 ROWS_PER_SUBCOLUMN개를 넘어 서브열이 2개 이상 생기면, 이슈 열의
 * 시작 x가 고정값(380)이라 문서의 두 번째 서브열과 겹치는 문제가 있었다(그래프
 * 화면 QA 스크린샷, 2026-07-23). 이슈 열 시작 위치를 "문서 열이 실제로 차지하는
 * 폭 + 여백"으로 다시 계산해서 문서 개수와 무관하게 항상 겹치지 않게 한다.
 */
export function layoutGraphColumns(docCount: number, issueCount: number) {
  const docSubcolumns = subcolumnsNeeded(docCount);
  const issueBaseX = DOC_BASE_X + (docSubcolumns - 1) * SUBCOLUMN_GAP + CARD_WIDTH + GROUP_GAP;
  const issueSubcolumns = subcolumnsNeeded(issueCount);

  return {
    docPositions: columnPositions(DOC_BASE_X, docCount),
    issuePositions: columnPositions(issueBaseX, issueCount),
    canvasWidth: issueBaseX + (issueSubcolumns - 1) * SUBCOLUMN_GAP + CARD_WIDTH + 40,
  };
}

export const GRAPH_CANVAS_HEIGHT = ROW_START_Y + ROWS_PER_SUBCOLUMN * ROW_HEIGHT;
