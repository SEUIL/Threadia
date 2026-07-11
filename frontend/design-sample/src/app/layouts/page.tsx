"use client";

// 레이아웃 구조 실험 갤러리
// - 목적: "사이드바+상단바+카드그리드" 안전 조합에서 벗어난 구조 3가지를 실제로 조작해보고 비교한다.
// - 색/폰트는 이미 확정된 Aurora Nightfall 조합(o) + Pretendard/Geist를 그대로 쓴다.
// - 각 개념은 Threadia의 실제 화면(이슈 보드 / 허브)을 가정한 목업 데이터로 채운다.

import { useMemo, useRef, useState } from "react";

const FONT_FAMILY =
  'var(--font-geist-sans), "Pretendard Variable", system-ui, sans-serif';

// Aurora Nightfall 팔레트 고정값
const C = {
  navy: "#0e1633",
  surface: "#f4f7fb",
  teal: "#0fb9b1",
  aqua: "#5eead4",
  violet: "#7c5cff",
};

type Concept = {
  id: string;
  label: string;
  tagline: string;
  breaks: string; // 어떤 "안전한 조합"을 깨는지
};

const concepts: Concept[] = [
  {
    id: "rail",
    label: "A. 커맨드 레일",
    tagline: "상단바 제거 + 오버레이 디테일 + 떠 있는 커맨드바",
    breaks:
      "고정 상단바(로고+검색+아바타)를 없애고, 디테일 패널은 레이아웃을 밀지 않고 위에 뜬다. 액션은 하단 중앙 플로팅 커맨드바로 이동.",
  },
  {
    id: "lane",
    label: "B. 레인 보드",
    tagline: "세로 사이드바 최소화 + 수평 스크롤 레인 + 이종 카드 혼합",
    breaks:
      "사이드바는 아이콘만 남는 40px 레일로 고정. 카드그리드 대시보드 대신 수평 스크롤 레인 위에 이슈·문서·채팅 카드가 한 레인에 섞여서 흐른다.",
  },
  {
    id: "graph",
    label: "C. 그래프 캔버스",
    tagline: "그리드 자체를 포기 — 팬/줌 가능한 자유배치 캔버스",
    breaks:
      "Threadia의 실제 차별점(이슈·문서·메시지를 쓰레드/그래프로 연결)을 레이아웃으로 그대로 구현. 카드가 좌표에 놓이고 실선으로 연결된다.",
  },
  {
    id: "rail-graph",
    label: "D. 레일 + 그래프",
    tagline: "A의 사이드바(레일+플라이아웃) + C의 그래프 캔버스 조합",
    breaks:
      "A에서 마음에 든 부분(상단바 제거, hover 플라이아웃 레일, 하단 플로팅 커맨드바)은 그대로 두고, 리스트였던 메인 콘텐츠를 C의 팬/줌 그래프 캔버스로 교체했다.",
  },
];

// ---------------------------------------------------------------------------
// 공용 목업 데이터
// ---------------------------------------------------------------------------
type ItemKind = "issue" | "doc" | "chat";
type Item = {
  id: string;
  kind: ItemKind;
  title: string;
  meta: string;
  x?: number;
  y?: number;
};

const items: Item[] = [
  { id: "i1", kind: "issue", title: "로그인 리다이렉트 버그", meta: "긴급 · 정철진", x: 60, y: 80 },
  { id: "i2", kind: "issue", title: "비밀번호 재설정 E2E", meta: "완료 · 여도현", x: 320, y: 40 },
  { id: "d1", kind: "doc", title: "인증 흐름 설계 문서", meta: "문서 · v1.2", x: 340, y: 220 },
  { id: "i3", kind: "issue", title: "허브 3D 아바타 채팅", meta: "진행중 · 정철진", x: 620, y: 100 },
  { id: "c1", kind: "chat", title: "#sprint1-frontend", meta: "메시지 24", x: 600, y: 300 },
  { id: "d2", kind: "doc", title: "ERD v2 변경 이력", meta: "문서 · 검토중", x: 860, y: 60 },
  { id: "i4", kind: "issue", title: "invitation lookup API", meta: "계획 · 여도현", x: 880, y: 260 },
];

const edges: [string, string][] = [
  ["i1", "d1"],
  ["i2", "d1"],
  ["i3", "c1"],
  ["i3", "i2"],
  ["d2", "i4"],
  ["c1", "i4"],
];

const kindColor: Record<ItemKind, string> = {
  issue: C.teal,
  doc: C.violet,
  chat: C.aqua,
};
const kindLabel: Record<ItemKind, string> = {
  issue: "이슈",
  doc: "문서",
  chat: "채팅",
};
const allKinds: ItemKind[] = ["issue", "doc", "chat"];

export default function LayoutLab() {
  const [active, setActive] = useState("rail");

  return (
    <div
      className="flex min-h-screen bg-zinc-100"
      style={{ fontFamily: FONT_FAMILY }}
    >
      {/* 좌측: 개념 스위처 (팔레트 페이지와 동일 패턴) */}
      <aside className="sticky top-0 flex h-screen w-80 shrink-0 flex-col border-r border-zinc-200 bg-white">
        <div className="border-b border-zinc-200 px-4 py-3">
          <h1 className="text-sm font-bold text-zinc-800">레이아웃 구조 실험</h1>
          <p className="text-xs text-zinc-400">
            &ldquo;안전한 조합&rdquo; 타파 · 구조 3안
          </p>
        </div>
        <div className="flex-1 overflow-y-auto p-3">
          {concepts.map((c) => {
            const isActive = c.id === active;
            return (
              <button
                key={c.id}
                onClick={() => setActive(c.id)}
                className={`mb-2 w-full rounded-xl border px-3 py-3 text-left transition ${
                  isActive
                    ? "border-zinc-900 bg-zinc-900 text-white"
                    : "border-zinc-200 bg-white text-zinc-700 hover:border-zinc-400"
                }`}
              >
                <div className="text-sm font-bold">{c.label}</div>
                <div
                  className={`mt-1 text-xs ${isActive ? "text-white/70" : "text-zinc-400"}`}
                >
                  {c.tagline}
                </div>
              </button>
            );
          })}
        </div>
        <div className="border-t border-zinc-200 p-3 text-[11px] leading-relaxed text-zinc-400">
          각 안이 구체적으로 어떤 &ldquo;안전한 조합&rdquo;을 깨는지는 카드 아래 설명을
          참고하세요. 색·폰트는 이미 확정한 Aurora Nightfall 그대로입니다.
        </div>
      </aside>

      {/* 우측: 선택한 개념의 실제 프로토타입 */}
      <main className="min-w-0 flex-1 overflow-hidden">
        <div className="border-b border-zinc-200 bg-white px-6 py-3 text-xs text-zinc-500">
          {concepts.find((c) => c.id === active)?.breaks}
        </div>
        <div className="h-[calc(100vh-45px)]">
          {active === "rail" && <CommandRailConcept />}
          {active === "lane" && <LaneBoardConcept />}
          {active === "graph" && <GraphCanvasConcept />}
          {active === "rail-graph" && <RailGraphConcept />}
        </div>
      </main>
    </div>
  );
}

// ---------------------------------------------------------------------------
// A. 커맨드 레일 — 상단바 제거 + 오버레이 디테일 + 플로팅 커맨드바
// ---------------------------------------------------------------------------
// 아이콘 전용 레일 — hover 시 플라이아웃으로 확장 (레이아웃을 밀지 않음). A/D 공용.
function RailNav({ activeLabel = "이슈" }: { activeLabel?: string }) {
  const [railOpen, setRailOpen] = useState(false);
  const navLabels = ["허브", "이슈", "문서", "채팅"];

  return (
    <div
      onMouseEnter={() => setRailOpen(true)}
      onMouseLeave={() => setRailOpen(false)}
      className="relative z-20 flex h-full w-16 shrink-0 flex-col items-center gap-4 border-r py-5"
      style={{ borderColor: "rgba(14,22,51,0.08)", background: C.navy }}
    >
      {navLabels.map((label) => (
        <div
          key={label}
          className="grid h-9 w-9 place-items-center rounded-lg text-xs font-bold text-white"
          style={{ background: label === activeLabel ? C.teal : "rgba(255,255,255,0.08)" }}
          title={label}
        >
          {label[0]}
        </div>
      ))}

      {/* 플라이아웃 — position absolute, transform + opacity로만 전환 (motion 차별화) */}
      <div
        className="absolute left-16 top-0 h-full w-52 origin-left rounded-r-2xl py-5 shadow-2xl transition-all duration-300 ease-out"
        style={{
          background: C.navy,
          transform: railOpen ? "scaleX(1)" : "scaleX(0)",
          opacity: railOpen ? 1 : 0,
          pointerEvents: railOpen ? "auto" : "none",
        }}
      >
        <div className="px-4 text-[11px] font-semibold uppercase tracking-wide text-white/40">
          Threadia
        </div>
        {["허브", "이슈 보드", "문서", "채팅"].map((label) => (
          <div
            key={label}
            className="mx-2 mt-1 rounded-lg px-3 py-2 text-sm font-medium text-white"
            style={{ background: label.includes(activeLabel) ? "rgba(255,255,255,0.08)" : "transparent" }}
          >
            {label}
          </div>
        ))}
      </div>
    </div>
  );
}

// 플로팅 커맨드바 — 고정 상단바 대신 하단 중앙에. A/D 공용.
// onQueryChange 를 넘기면(D) 실제 검색 인풋으로, 안 넘기면(A) 정적 텍스트로 표시한다.
function FloatingCommandBar({
  primaryLabel,
  query,
  onQueryChange,
}: {
  primaryLabel: string;
  query?: string;
  onQueryChange?: (value: string) => void;
}) {
  return (
    <div className="pointer-events-none absolute inset-x-0 bottom-6 flex justify-center">
      <div
        className="pointer-events-auto flex items-center gap-3 rounded-full px-5 py-3 shadow-xl"
        style={{ background: C.navy }}
      >
        {onQueryChange ? (
          <input
            value={query}
            onChange={(e) => onQueryChange(e.target.value)}
            placeholder="제목으로 검색"
            className="w-40 bg-transparent text-sm text-white placeholder-white/40 outline-none"
          />
        ) : (
          <span className="text-sm text-white/50">⌘K 검색 · 새 이슈 · 필터</span>
        )}
        <span className="h-6 w-px" style={{ background: "rgba(255,255,255,0.15)" }} />
        <button
          className="rounded-full px-3 py-1 text-xs font-semibold text-white"
          style={{ background: C.teal }}
        >
          {primaryLabel}
        </button>
      </div>
    </div>
  );
}

// 노드 타입 필터 칩 — 타입별 색(kindColor)과 그대로 매칭. D 전용.
function TypeFilterChips({
  activeKinds,
  onToggle,
}: {
  activeKinds: ItemKind[];
  onToggle: (kind: ItemKind) => void;
}) {
  return (
    <div className="absolute left-4 top-4 z-10 flex gap-2">
      {allKinds.map((kind) => {
        const isActive = activeKinds.includes(kind);
        return (
          <button
            key={kind}
            onClick={() => onToggle(kind)}
            className="flex items-center gap-1.5 rounded-full border px-3 py-1.5 text-xs font-semibold shadow-sm transition"
            style={{
              background: isActive ? kindColor[kind] : "#ffffff",
              color: isActive ? "#ffffff" : "#7a8296",
              borderColor: isActive ? kindColor[kind] : "rgba(14,22,51,0.12)",
            }}
          >
            <span
              className="h-2 w-2 rounded-full"
              style={{ background: isActive ? "#ffffff" : kindColor[kind] }}
            />
            {kindLabel[kind]}
          </button>
        );
      })}
    </div>
  );
}

function CommandRailConcept() {
  const [selected, setSelected] = useState<Item | null>(null);
  const issues = items.filter((i) => i.kind === "issue");

  return (
    <div className="relative flex h-full" style={{ background: C.surface }}>
      <RailNav activeLabel="이슈" />

      {/* 메인 — 상단바 없이 콘텐츠가 바로 시작 (여백으로 위계 표현) */}
      <div className="flex-1 overflow-y-auto px-10 py-8">
        <h2 className="text-2xl font-bold" style={{ color: C.navy }}>
          Sprint 1 · 이슈
        </h2>
        <p className="mt-1 text-sm text-zinc-500">
          상단 고정바가 없습니다 — 검색/필터는 아래 플로팅 커맨드바에서.
        </p>

        <div className="mt-6 grid gap-3" style={{ maxWidth: 640 }}>
          {issues.map((it) => (
            <button
              key={it.id}
              onClick={() => setSelected(it)}
              className="rounded-xl border bg-white px-4 py-3 text-left transition hover:-translate-y-0.5 hover:shadow-md"
              style={{ borderColor: "rgba(14,22,51,0.08)" }}
            >
              <div className="text-sm font-semibold" style={{ color: C.navy }}>
                {it.title}
              </div>
              <div className="mt-1 text-xs text-zinc-400">{it.meta}</div>
            </button>
          ))}
        </div>
      </div>

      {/* 오버레이 디테일 패널 — 레이아웃을 밀지 않고 위에서 슬라이드 */}
      <div
        className="absolute inset-0 z-30 transition-opacity duration-300"
        style={{
          background: "rgba(14,22,51,0.35)",
          opacity: selected ? 1 : 0,
          pointerEvents: selected ? "auto" : "none",
        }}
        onClick={() => setSelected(null)}
      />
      <div
        className="absolute right-0 top-0 z-40 h-full w-[380px] bg-white p-6 shadow-2xl transition-transform duration-300 ease-out"
        style={{ transform: selected ? "translateX(0)" : "translateX(100%)" }}
      >
        {selected && (
          <>
            <span
              className="rounded-full px-2 py-0.5 text-[11px] font-bold text-white"
              style={{ background: kindColor[selected.kind] }}
            >
              {kindLabel[selected.kind]}
            </span>
            <h3 className="mt-3 text-lg font-bold" style={{ color: C.navy }}>
              {selected.title}
            </h3>
            <p className="mt-1 text-sm text-zinc-400">{selected.meta}</p>
            <div className="mt-6 rounded-lg border border-dashed p-4 text-xs text-zinc-400">
              (상세 내용 · 댓글 · 연결된 문서/채팅 스레드가 여기 들어감)
            </div>
          </>
        )}
      </div>

      <FloatingCommandBar primaryLabel="+ 새 이슈" />
    </div>
  );
}

// ---------------------------------------------------------------------------
// B. 레인 보드 — 얇은 아이콘 레일 + 수평 스크롤 레인 + 이종 카드 혼합
// ---------------------------------------------------------------------------
const lanes: { key: string; label: string; items: Item[] }[] = [
  { key: "todo", label: "할 일", items: [items[0], items[3]] },
  { key: "doing", label: "진행중", items: [items[1], items[4]] },
  { key: "review", label: "검토", items: [items[2], items[5]] },
  { key: "done", label: "완료", items: [items[6]] },
];

function LaneBoardConcept() {
  return (
    <div className="flex h-full" style={{ background: C.surface }}>
      {/* 항상 40px로 고정된 아이콘 레일 — 확장 없음, hover 효과만 */}
      <div
        className="flex h-full w-10 shrink-0 flex-col items-center gap-3 py-4"
        style={{ background: C.navy }}
      >
        {["H", "I", "D", "C"].map((l) => (
          <div
            key={l}
            className="grid h-7 w-7 cursor-pointer place-items-center rounded-md text-[11px] font-bold text-white/70 transition hover:bg-white/10 hover:text-white"
          >
            {l}
          </div>
        ))}
      </div>

      {/* 수평 스크롤 레인 — 카드그리드 대시보드가 아니라 옆으로 흐르는 레인 */}
      <div className="flex flex-1 gap-4 overflow-x-auto p-6" style={{ scrollSnapType: "x proximity" }}>
        {lanes.map((lane) => (
          <div
            key={lane.key}
            className="flex w-72 shrink-0 flex-col rounded-2xl bg-white/60"
            style={{ scrollSnapAlign: "start" }}
          >
            <div
              className="sticky top-0 rounded-t-2xl px-4 py-3 text-sm font-bold backdrop-blur"
              style={{ color: C.navy, background: "rgba(244,247,251,0.9)" }}
            >
              {lane.label}
              <span className="ml-2 text-xs font-normal text-zinc-400">
                {lane.items.length}
              </span>
            </div>
            <div className="flex-1 space-y-2 px-3 pb-4">
              {lane.items.map((it) => (
                <div
                  key={it.id}
                  className="cursor-grab rounded-xl border bg-white p-3 transition hover:-translate-y-0.5 hover:scale-[1.02] hover:shadow-lg active:cursor-grabbing"
                  style={{ borderColor: "rgba(14,22,51,0.08)" }}
                >
                  <span
                    className="rounded px-1.5 py-0.5 text-[10px] font-bold text-white"
                    style={{ background: kindColor[it.kind] }}
                  >
                    {kindLabel[it.kind]}
                  </span>
                  <div className="mt-2 text-sm font-semibold" style={{ color: C.navy }}>
                    {it.title}
                  </div>
                  <div className="mt-1 text-xs text-zinc-400">{it.meta}</div>
                </div>
              ))}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

// ---------------------------------------------------------------------------
// C. 그래프 캔버스 — 그리드를 포기하고 팬/줌 가능한 자유배치 + 쓰레드 연결선
// (본체는 GraphCanvasBody 로 분리해 D. 레일+그래프 조합에서도 재사용한다)
// ---------------------------------------------------------------------------
function GraphCanvasConcept() {
  return <GraphCanvasBody />;
}

function GraphCanvasBody({
  activeKinds = allKinds,
  query = "",
}: {
  activeKinds?: ItemKind[];
  query?: string;
}) {
  const [pan, setPan] = useState({ x: 0, y: 0 });
  const [scale, setScale] = useState(1);
  const [selected, setSelected] = useState<Item | null>(null);
  const [isDragging, setIsDragging] = useState(false);
  const dragging = useRef<{ x: number; y: number } | null>(null);

  // 필터링된 노드/연결선 — 캔버스 크기(bounds)는 전체 items 기준으로 고정해
  // 필터를 켜고 끌 때 pan/zoom이 튀지 않게 한다.
  const visibleItems = useMemo(
    () =>
      items.filter(
        (it) =>
          activeKinds.includes(it.kind) &&
          it.title.toLowerCase().includes(query.trim().toLowerCase())
      ),
    [activeKinds, query]
  );
  const visibleIds = useMemo(
    () => new Set(visibleItems.map((i) => i.id)),
    [visibleItems]
  );
  const visibleEdges = useMemo(
    () => edges.filter(([a, b]) => visibleIds.has(a) && visibleIds.has(b)),
    [visibleIds]
  );

  const bounds = useMemo(() => {
    const xs = items.map((i) => i.x ?? 0);
    const ys = items.map((i) => i.y ?? 0);
    return {
      minX: Math.min(...xs),
      maxX: Math.max(...xs) + 220,
      minY: Math.min(...ys),
      maxY: Math.max(...ys) + 90,
    };
  }, []);

  const onMouseDown = (e: React.MouseEvent) => {
    dragging.current = { x: e.clientX - pan.x, y: e.clientY - pan.y };
    setIsDragging(true);
  };
  const onMouseMove = (e: React.MouseEvent) => {
    if (!dragging.current) return;
    setPan({ x: e.clientX - dragging.current.x, y: e.clientY - dragging.current.y });
  };
  const onMouseUp = () => {
    dragging.current = null;
    setIsDragging(false);
  };

  const zoom = (delta: number) =>
    setScale((s) => Math.min(1.6, Math.max(0.5, s + delta)));

  const itemById = (id: string) => items.find((i) => i.id === id)!;

  return (
    <div
      className="relative h-full select-none overflow-hidden"
      style={{
        background:
          `radial-gradient(circle at 20px 20px, rgba(14,22,51,0.08) 1px, transparent 0) ${C.surface}`,
        backgroundSize: "24px 24px",
      }}
      onMouseDown={onMouseDown}
      onMouseMove={onMouseMove}
      onMouseUp={onMouseUp}
      onMouseLeave={onMouseUp}
    >
      {/* 캔버스 — 그리드/사이드바 없이 좌표 자유배치. transform으로 pan+zoom */}
      <div
        className="absolute origin-top-left"
        style={{
          transform: `translate(${pan.x}px, ${pan.y}px) scale(${scale})`,
          width: bounds.maxX,
          height: bounds.maxY,
          cursor: isDragging ? "grabbing" : "grab",
        }}
      >
        {/* 연결선 — Threadia의 실제 차별점(쓰레드/그래프 연결)을 레이아웃으로 표현 */}
        <svg
          className="pointer-events-none absolute inset-0"
          width={bounds.maxX}
          height={bounds.maxY}
        >
          {visibleEdges.map(([a, b], i) => {
            const from = itemById(a);
            const to = itemById(b);
            return (
              <line
                key={i}
                x1={(from.x ?? 0) + 90}
                y1={(from.y ?? 0) + 30}
                x2={(to.x ?? 0) + 90}
                y2={(to.y ?? 0) + 30}
                stroke={C.violet}
                strokeWidth={1.5}
                strokeOpacity={0.35}
                strokeDasharray="4 3"
              />
            );
          })}
        </svg>

        {visibleItems.map((it) => (
          <button
            key={it.id}
            onClick={(e) => {
              e.stopPropagation();
              setSelected(it);
            }}
            className="absolute w-44 rounded-xl border bg-white p-3 text-left shadow-sm transition hover:shadow-lg"
            style={{ left: it.x, top: it.y, borderColor: "rgba(14,22,51,0.08)" }}
          >
            <span
              className="rounded px-1.5 py-0.5 text-[10px] font-bold text-white"
              style={{ background: kindColor[it.kind] }}
            >
              {kindLabel[it.kind]}
            </span>
            <div className="mt-2 text-xs font-semibold leading-snug" style={{ color: C.navy }}>
              {it.title}
            </div>
          </button>
        ))}
      </div>

      {/* 줌 컨트롤 */}
      <div className="absolute bottom-4 left-4 flex gap-1 rounded-full bg-white p-1 shadow">
        <button
          onClick={() => zoom(-0.15)}
          className="grid h-8 w-8 place-items-center rounded-full text-sm font-bold text-zinc-500 hover:bg-zinc-100"
        >
          −
        </button>
        <button
          onClick={() => zoom(0.15)}
          className="grid h-8 w-8 place-items-center rounded-full text-sm font-bold text-zinc-500 hover:bg-zinc-100"
        >
          +
        </button>
      </div>

      {/* 미니맵 */}
      <div
        className="absolute bottom-4 right-4 h-24 w-36 rounded-lg border bg-white/80 p-1 backdrop-blur"
        style={{ borderColor: "rgba(14,22,51,0.1)" }}
      >
        <div className="relative h-full w-full overflow-hidden rounded">
          {items.map((it) => (
            <span
              key={it.id}
              className="absolute h-1.5 w-1.5 rounded-full"
              style={{
                left: ((it.x ?? 0) / bounds.maxX) * 100 + "%",
                top: ((it.y ?? 0) / bounds.maxY) * 100 + "%",
                background: kindColor[it.kind],
                opacity: visibleIds.has(it.id) ? 1 : 0.15,
              }}
            />
          ))}
        </div>
      </div>

      {/* 필터 결과가 없을 때 */}
      {visibleItems.length === 0 && (
        <div className="pointer-events-none absolute inset-0 grid place-items-center">
          <span className="rounded-full bg-white px-4 py-2 text-xs text-zinc-400 shadow">
            조건에 맞는 노드가 없습니다
          </span>
        </div>
      )}

      {/* 노드 클릭 시 노드 근처에서 확대되는 플로팅 카드 (transform-origin 모션) */}
      {/* 필터로 가려진 노드의 카드는 닫는다 */}
      {selected && visibleIds.has(selected.id) && (
        <div
          className="absolute z-20 w-72 origin-top-left rounded-2xl bg-white p-5 shadow-2xl"
          style={{
            left: Math.min((selected.x ?? 0) * scale + pan.x + 60, window.innerWidth - 320),
            top: Math.min((selected.y ?? 0) * scale + pan.y + 40, window.innerHeight - 220),
          }}
        >
          <button
            onClick={() => setSelected(null)}
            className="absolute right-3 top-3 text-xs text-zinc-400"
          >
            닫기
          </button>
          <span
            className="rounded-full px-2 py-0.5 text-[11px] font-bold text-white"
            style={{ background: kindColor[selected.kind] }}
          >
            {kindLabel[selected.kind]}
          </span>
          <h3 className="mt-3 text-base font-bold" style={{ color: C.navy }}>
            {selected.title}
          </h3>
          <p className="mt-1 text-sm text-zinc-400">{selected.meta}</p>
        </div>
      )}
    </div>
  );
}

// ---------------------------------------------------------------------------
// D. 레일 + 그래프 — A의 사이드바(레일+플라이아웃+커맨드바) + C의 그래프 캔버스
// ---------------------------------------------------------------------------
function RailGraphConcept() {
  const [query, setQuery] = useState("");
  const [activeKinds, setActiveKinds] = useState<ItemKind[]>(allKinds);

  const toggleKind = (kind: ItemKind) =>
    setActiveKinds((prev) =>
      prev.includes(kind) ? prev.filter((k) => k !== kind) : [...prev, kind]
    );

  return (
    <div className="relative flex h-full" style={{ background: C.surface }}>
      <RailNav activeLabel="허브" />

      <div className="relative min-w-0 flex-1">
        <TypeFilterChips activeKinds={activeKinds} onToggle={toggleKind} />
        <GraphCanvasBody activeKinds={activeKinds} query={query} />
      </div>

      <FloatingCommandBar
        primaryLabel="+ 새 노드"
        query={query}
        onQueryChange={setQuery}
      />
    </div>
  );
}
