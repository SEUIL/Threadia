/*
 * 그래프 화면(문서/이슈/채팅 통합) + 문서 목록 화면이 함께 쓰는 목업 데이터/타입.
 *
 * ⚠️ MOCK: 실 API(documents/graph 등)가 없어서 로컬 상수로 흉내낸다. 이 파일의
 * MOCK_DOCS/MOCK_CHATS는 board/messages 화면의 자체 mock과도 별개 데이터
 * 섬이다 — 실 API 연동 전까지는 화면 간에 서로 반영되지 않는다(예: docs
 * 목록 화면에서 문서를 휴지통으로 보내도 이 그래프 화면엔 반영 안 됨. 각
 * 화면이 독립된 useState로 목업을 들고 있기 때문).
 */

export type DocType = "DOC" | "MEETING_NOTE";
export type NodeKind = "doc" | "issue" | "chat";

export type DocItem = {
  id: string;
  title: string;
  type: DocType;
  authorName: string;
  updatedAt: string;
  linkedIssueKeys: string[];
  trashedAt: string | null;
};

export const TYPE_LABEL: Record<DocType, string> = {
  DOC: "문서",
  MEETING_NOTE: "회의록",
};

export const MOCK_DOCS: DocItem[] = [
  { id: "d1", title: "Sprint 2 킥오프 회의록", type: "MEETING_NOTE", authorName: "정철진", updatedAt: "2026-06-29", linkedIssueKeys: ["THR-101"], trashedAt: null },
  { id: "d2", title: "회사/프로젝트 API 설계", type: "DOC", authorName: "여도현", updatedAt: "2026-07-01", linkedIssueKeys: ["THR-101", "THR-102"], trashedAt: null },
  { id: "d3", title: "칸반 보드 UX 노트", type: "DOC", authorName: "김민정", updatedAt: "2026-07-02", linkedIssueKeys: [], trashedAt: null },
  { id: "d4", title: "S3 연동 조사", type: "DOC", authorName: "황스일", updatedAt: "2026-07-02", linkedIssueKeys: ["THR-103"], trashedAt: null },
];

// 그래프 화면용 미리보기 정보(이슈 노드) — 실제로는 documents/graph API가 내려줘야 함
export const ISSUE_PREVIEW: Record<string, { title: string; status: string }> = {
  "THR-101": { title: "로그인 화면 반응형 깨짐", status: "검토중" },
  "THR-102": { title: "허브 상단바 알림 뱃지 API 연동", status: "할일" },
  "THR-103": { title: "회사 로고 업로드 S3 연동", status: "백로그" },
};

// 그래프 화면용 채팅 노드 목업 — messages/page.tsx의 MOCK_MESSAGES와 별개 데이터 섬.
// 이슈로 전환 여지가 있는(관련 이슈가 있는) 메시지만 그래프에 노드로 올린다.
export const MOCK_CHATS: { id: string; authorName: string; text: string; relatedIssueKey: string }[] = [
  { id: "c1", authorName: "여도현", text: "허브 상단바 알림 뱃지 API 아직 안 붙었네요, 이슈로 뺄게요", relatedIssueKey: "THR-102" },
  { id: "c2", authorName: "황스일", text: "로고 업로드 S3 연동 아직 안 됐죠?", relatedIssueKey: "THR-103" },
];

export const KIND_LABEL: Record<NodeKind, string> = {
  doc: "문서",
  issue: "이슈",
  chat: "채팅",
};

// 노드 배지/칩 색 — doc은 기존부터 primary(teal), issue는 기존부터
// accent(violet)를 써왔다(이 화면의 기존 관례). chat은 aurora-3(aqua)로
// 추가했다. 필터 칩과 노드 배지가 같은 매핑을 쓴다(칩 색 = 노드 색 = 범례).
export const KIND_CHIP_CLASS: Record<NodeKind, string> = {
  doc: "bg-primary text-primary-foreground",
  issue: "bg-accent text-accent-foreground",
  chat: "bg-aurora-3 text-foreground",
};
// 선택된 카드 강조 링 색 — 배지 색과 동일 계열.
export const KIND_RING_CLASS: Record<NodeKind, string> = {
  doc: "ring-primary",
  issue: "ring-accent",
  chat: "ring-aurora-3",
};
export const ALL_KINDS: NodeKind[] = ["doc", "issue", "chat"];

export type GraphNode = { id: string; kind: NodeKind; label: string; x: number; y: number };

// 카드형 노드 크기 — 원(circle) 대신 배지+제목이 있는 카드로 그린다(2026-07-06
// 디자인 변경, 스크린샷 레퍼런스 반영). x/y는 카드의 좌상단 좌표다.
export const CARD_WIDTH = 190;
export const CARD_ANCHOR_Y = 34; // 연결선이 붙는 카드 세로 중심 지점(대략치)

// 종류별 열 배치 — 문서/이슈/채팅을 각각 세로로 나열하되, 한 열에 카드가
// ROWS_PER_SUBCOLUMN개를 넘으면 옆으로 서브열을 만들어 무한정 아래로만
// 길어지지 않게 한다(카드 수가 늘어나도 캔버스 높이가 고정 유지됨).
// ⚠️ 카드 폭(CARD_WIDTH)보다 넉넉하게 열 간격을 잡았다 — 문서/이슈/채팅 각
// 개수가 지금 목업 규모(수개)를 크게 벗어나면(수십 개 이상) 이 근사 배치도
// 전용 그래프 라이브러리로 재검토해야 한다.
export const COLUMN_BASE_X: Record<NodeKind, number> = { doc: 40, issue: 380, chat: 720 };
export const ROWS_PER_SUBCOLUMN = 6;
export const ROW_HEIGHT = 100;
export const SUBCOLUMN_GAP = CARD_WIDTH + 20;
export const ROW_START_Y = 40;

export function layoutColumn(kind: NodeKind, count: number): { x: number; y: number }[] {
  const baseX = COLUMN_BASE_X[kind];
  return Array.from({ length: count }, (_, i) => {
    const row = i % ROWS_PER_SUBCOLUMN;
    const col = Math.floor(i / ROWS_PER_SUBCOLUMN);
    return { x: baseX + col * SUBCOLUMN_GAP, y: ROW_START_Y + row * ROW_HEIGHT };
  });
}

export const GRAPH_CANVAS_WIDTH = COLUMN_BASE_X.chat + CARD_WIDTH + 40;
export const GRAPH_CANVAS_HEIGHT = ROW_START_Y + ROWS_PER_SUBCOLUMN * ROW_HEIGHT;
