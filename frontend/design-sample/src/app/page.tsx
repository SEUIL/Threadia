"use client";

// 색 조합 풀스크린 미리보기
// - 색 값은 src/styles/palettes.css 의 CSS 변수로 관리합니다.
// - 선택한 조합의 변수를 목업 전체(--bg / --text / --accent)에 매핑해 적용합니다.

import { useState } from "react";

// 폰트는 확정되어 고정 적용 (영문 Geist + 한글 Pretendard).
// 다른 폰트로 바꾸려면 이 값만 수정하세요. 로딩: app/fonts.ts, styles/korean-fonts.css
const FONT_FAMILY =
  'var(--font-geist-sans), "Pretendard Variable", system-ui, sans-serif';

type Combo = {
  id: string;
  hexes: string[]; // 3색 또는 4색
  name?: string; // 표시 이름 (없으면 id)
  direct?: boolean; // true면 CSS 변수 대신 hex 직접 적용 (레퍼런스 추가분)
  // 색 역할을 직접 지정 (자동 배정 대신). hexes 인덱스로 지정.
  roles?: { light: number; dark: number; accent: number; accent2: number };
  final?: boolean; // 최종 확정 표시
};

const combos: Combo[] = [
  { id: "a", hexes: ["#ea5c2a", "#1e1e1e", "#fcfaff"] },
  { id: "b", hexes: ["#f6042e", "#020005", "#ffae2e"] },
  { id: "c", hexes: ["#fe7902", "#292929", "#fffff5"] },
  { id: "d", hexes: ["#cc001e", "#0e0e0e", "#f0f4f5"], name: "오리지널" },
  { id: "e", hexes: ["#ff4628", "#202020", "#b8c8d7"] },
  { id: "f", hexes: ["#e8294a", "#f5ede4", "#2c1810"], name: "오리지널" },
  { id: "g", hexes: ["#00457f", "#edf5fa", "#ffa723"] },
  { id: "h", hexes: ["#51aeec", "#f8fbff", "#ffc0df"] },
  { id: "i", hexes: ["#294aff", "#ecfcff", "#37ffa9"] },
  { id: "j", hexes: ["#1f3a60", "#3582b8", "#f7cf59"] },
  { id: "k", hexes: ["#1f41b0", "#f4f4f4", "#e34054"] },
  { id: "l", hexes: ["#2aa6ff", "#b8e2ff", "#667391"] },
  { id: "m", hexes: ["#8a4fff", "#f5f1ff", "#ff9ad5"] },
  { id: "n", hexes: ["#6d28d9", "#f7f0ff", "#eba4fe"], name: "오리지널" },
  // 4색 버전 (정의만 유지, 숨김)
  { id: "n4", hexes: ["#6d28d9", "#f7f0ff", "#eba4fe", "#5eead4"] },
  { id: "d4", hexes: ["#cc001e", "#0e0e0e", "#f0f4f5", "#f2b705"] },
  { id: "f4", hexes: ["#e8294a", "#f5ede4", "#2c1810", "#d99a2b"] },
  // 계열별 변형안 1차 (CSS 변수 관리)
  { id: "n1", hexes: ["#6600ca", "#f3effb", "#b23bee"], name: "Plasma" },
  { id: "n2", hexes: ["#560591", "#f5f0fa", "#a400b8"], name: "Grape" },
  { id: "n3", hexes: ["#734f96", "#f7f3ff", "#b57edc"], name: "Soft Lilac" },
  { id: "d1", hexes: ["#e0474c", "#111111", "#ffffff"], name: "Signal Red" },
  { id: "d2", hexes: ["#b11a21", "#0a0a0a", "#f2f2f2"], name: "Crimson" },
  { id: "d3", hexes: ["#ff3b30", "#1a1a1a", "#f5f5f5"], name: "Scarlet" },
  { id: "f1", hexes: ["#e2725b", "#f7e6ca", "#2c1810"], name: "Terracotta" },
  { id: "f2", hexes: ["#9b2948", "#f5ede4", "#2c1810"], name: "Burgundy" },
  { id: "f3", hexes: ["#d47e30", "#fffdd0", "#3a2418"], name: "Cinnamon" },
  // 계열별 변형안 2차 (레퍼런스 추가 · hex 직접 적용)
  { id: "n5", hexes: ["#8e5bff", "#f2c7ff", "#2f2547"], name: "Orchid Glow", direct: true },
  { id: "n6", hexes: ["#8d6bff", "#eee6ff", "#2c2347"], name: "Amethyst", direct: true },
  { id: "n7", hexes: ["#6b39d6", "#e2d2ff", "#2a0f4a"], name: "Royal", direct: true },
  { id: "n8", hexes: ["#7a3ccf", "#e0ccff", "#0f0a18"], name: "Velvet Night", direct: true },
  { id: "n9", hexes: ["#8a63c6", "#ebddf8", "#4a2b73"], name: "Iris & Cream", direct: true },
  { id: "n10", hexes: ["#7a4db8", "#ede2ff", "#2b173f"], name: "Plumberry", direct: true },
  { id: "n11", hexes: ["#8a2be2", "#f6e7ff", "#111018"], name: "Neon Pop", direct: true },
  { id: "n12", hexes: ["#5b2c83", "#d7c5ff", "#1e132b"], name: "Night Bloom", direct: true },
  { id: "d5", hexes: ["#b11226", "#f2f2f4", "#111114"], name: "Ruby Noir", direct: true },
  { id: "d6", hexes: ["#d61c4e", "#f2f2f2", "#1c1c1e"], name: "Studio Scarlet", direct: true },
  { id: "d7", hexes: ["#e11d48", "#f9fafb", "#111827"], name: "Streetwear", direct: true },
  { id: "d8", hexes: ["#c1123a", "#edf2f4", "#2b2d42"], name: "Modern Maroon", direct: true },
  { id: "d9", hexes: ["#c01f3b", "#f8f1e5", "#1b1b1d"], name: "Opera", direct: true },
  { id: "d10", hexes: ["#d9381e", "#f5f5f5", "#252525"], name: "Vermilion", direct: true },
  { id: "d11", hexes: ["#dc143c", "#ffffff", "#1a1a1a"], name: "Crimson Classic", direct: true },
  { id: "f5", hexes: ["#c75b3c", "#f3e2d2", "#2f2a28"], name: "Sunbaked Adobe", direct: true },
  { id: "f6", hexes: ["#d07a55", "#faf2e9", "#4a403b"], name: "Desert Linen", direct: true },
  { id: "f7", hexes: ["#b84e34", "#f6d6bf", "#1f2a33"], name: "Copper Canyon", direct: true },
  { id: "f8", hexes: ["#c25b45", "#f7e5dd", "#1e1b22"], name: "Terracotta Twilight", direct: true },
  { id: "f9", hexes: ["#b64d3c", "#f3d7d1", "#3b2b2a"], name: "Rosewood Spice", direct: true },
  { id: "f10", hexes: ["#c73a4a", "#f8f0e7", "#8c1f2c"], name: "Strawberry", direct: true },
  { id: "f11", hexes: ["#a92c3a", "#faf2ea", "#4f141a"], name: "Vintage Vanity", direct: true },
  { id: "f12", hexes: ["#b13b2f", "#fdf2e3", "#6a2018"], name: "Paprika Whip", direct: true },
  // 시스템 팔레트 (2026 SaaS 트렌드 · CSS 변수 관리)
  // 최종 확정. 역할 고정: 라이트=surface(흰), 다크=navy(텍스트/다크모드 배경),
  // 강조=teal(primary action), 보조강조=violet(pop). aqua(3)는 히어로 글로우에 사용.
  {
    id: "o",
    hexes: ["#0e1633", "#f4f7fb", "#0fb9b1", "#5eead4", "#7c5cff"],
    name: "Aurora Nightfall",
    roles: { light: 1, dark: 0, accent: 2, accent2: 4 },
    final: true,
  },
  { id: "p", hexes: ["#0f172a", "#f1f5f9", "#64748b", "#7c3aed"], name: "Slate System" },
  { id: "q", hexes: ["#0a0a0a", "#fafafa", "#18d6c8", "#a855f7"], name: "Mono + Aurora" },
];

// 탐색 과정 전체 아카이브 (진행 순서대로).
const groups = [
  {
    key: "init",
    label: "1. 초기 조사 (직접 수집)",
    ids: ["a", "b", "c", "e", "g", "h", "i", "j", "k", "l", "m"],
  },
  {
    key: "n",
    label: "2. 보라 · 라벤더",
    ids: ["n", "n1", "n2", "n3", "n5", "n6", "n7", "n8", "n9", "n10", "n11", "n12"],
  },
  {
    key: "d",
    label: "3. 레드 · 블랙",
    ids: ["d", "d1", "d2", "d3", "d5", "d6", "d7", "d8", "d9", "d10", "d11"],
  },
  {
    key: "f",
    label: "4. 웜 에디토리얼",
    ids: ["f", "f1", "f2", "f3", "f5", "f6", "f7", "f8", "f9", "f10", "f11", "f12"],
  },
  {
    key: "exp4",
    label: "5. 4색 실험 (보류)",
    ids: ["n4", "d4", "f4"],
  },
  {
    key: "sys",
    label: "6. 시스템 (SaaS)",
    ids: ["o", "p", "q"],
  },
];
const getCombo = (id: string) => combos.find((c) => c.id === id)!;

function hexToRgb(hex: string) {
  const h = hex.replace("#", "");
  return {
    r: parseInt(h.slice(0, 2), 16),
    g: parseInt(h.slice(2, 4), 16),
    b: parseInt(h.slice(4, 6), 16),
  };
}

function luminance(hex: string) {
  const { r, g, b } = hexToRgb(hex);
  return (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255;
}

function readableText(hex: string) {
  return luminance(hex) > 0.55 ? "#111111" : "#ffffff";
}

// HSL 채도 (강조색을 고를 때 더 선명한 색을 우선)
function saturation(hex: string) {
  const { r, g, b } = hexToRgb(hex);
  const mx = Math.max(r, g, b) / 255;
  const mn = Math.min(r, g, b) / 255;
  if (mx === mn) return 0;
  const l = (mx + mn) / 2;
  const d = mx - mn;
  return l > 0.5 ? d / (2 - mx - mn) : d / (mx + mn);
}

type RoleMap = { light: number; dark: number; accent: number; accent2: number };

// 색 역할 결정. roles 가 있으면 그대로 사용(명시 지정), 없으면 자동 배정:
// 밝은 색=배경, 어두운 색=글자, 남은 중간색 중 채도 높은 쪽=강조, 나머지=보조강조.
// invert=true 면 배경/글자를 뒤집습니다.
// colorRef(i): i번째 색을 실제로 적용할 때 쓸 값 (프리셋은 CSS 변수, 커스텀은 hex)
function assignRoles(
  hexes: string[],
  colorRef: (i: number) => string,
  invert: boolean,
  roles?: RoleMap
) {
  let lightI: number, darkI: number, accentI: number, accent2I: number;
  if (roles) {
    ({ light: lightI, dark: darkI, accent: accentI, accent2: accent2I } = roles);
  } else {
    const items = hexes.map((hex, i) => ({
      i,
      lum: luminance(hex),
      sat: saturation(hex),
    }));
    const byLum = [...items].sort((a, b) => b.lum - a.lum);
    lightI = byLum[0].i;
    darkI = byLum[byLum.length - 1].i;
    // 배경/글자를 제외한 중간색들 중 채도 높은 순으로 강조 배정
    const mids = byLum.slice(1, byLum.length - 1).sort((a, b) => b.sat - a.sat);
    accentI = mids[0]?.i ?? lightI;
    accent2I = mids[1]?.i ?? accentI;
  }
  const bgI = invert ? darkI : lightI;
  const textI = invert ? lightI : darkI;
  return {
    bg: colorRef(bgI),
    text: colorRef(textI),
    accent: colorRef(accentI),
    accentText: readableText(hexes[accentI]),
    accent2: colorRef(accent2I),
    accent2Text: readableText(hexes[accent2I]),
    softText: invert ? "rgba(255,255,255,0.72)" : "rgba(0,0,0,0.62)",
    border: invert ? "rgba(255,255,255,0.16)" : "rgba(0,0,0,0.1)",
  };
}

export default function Home() {
  const [activeId, setActiveId] = useState("o"); // 최종 확정: Aurora Nightfall
  const [invert, setInvert] = useState(false);
  // 커스텀 색 (직접 선택) — 기본값은 보라 오로라
  const [customColors, setCustomColors] = useState<[string, string, string]>([
    "#7c3aed",
    "#f5f3ff",
    "#22d3ee",
  ]);

  const isCustom = activeId === "custom";
  const activeCombo = isCustom ? null : getCombo(activeId);
  // 현재 팔레트의 hex (휘도 계산·색 라벨용)
  const hexes: string[] = isCustom ? customColors : activeCombo!.hexes;
  // 실제 적용값: CSS 변수 관리분은 var(), 커스텀·레퍼런스 추가분은 hex 직접
  const colorRef = (i: number) =>
    isCustom
      ? customColors[i]
      : activeCombo!.direct
        ? activeCombo!.hexes[i]
        : `var(--combo-${activeId}-${i + 1})`;
  const paletteLabel = isCustom
    ? "커스텀"
    : `${activeId.toUpperCase()} · ${activeCombo!.name ?? ""}`.trim();
  const paletteShort = isCustom ? "★" : activeId.toUpperCase();

  const setCustomAt = (i: number, value: string) =>
    setCustomColors((prev) => {
      const next = [...prev] as [string, string, string];
      next[i] = value;
      return next;
    });

  const role = assignRoles(
    hexes,
    colorRef,
    invert,
    isCustom ? undefined : activeCombo!.roles
  );
  const fontFamily = FONT_FAMILY;
  const totalCount = groups.reduce((n, g) => n + g.ids.length, 0);

  const features = [
    { title: "빠른 적용", desc: "CSS 변수만 바꾸면 전체 화면에 즉시 반영됩니다." },
    { title: "일관된 톤", desc: "한 조합의 색으로 배경·글자·강조를 구성합니다." },
    { title: "쉬운 비교", desc: "위에서 조합을 전환하며 느낌을 바로 확인하세요." },
  ];

  return (
    <div className="flex min-h-screen bg-zinc-100 font-sans">
      {/* 좌측 탐색 사이드바 (전체 과정 아카이브) */}
      <aside className="sticky top-0 flex h-screen w-72 shrink-0 flex-col border-r border-zinc-200 bg-white">
        <div className="flex items-center justify-between border-b border-zinc-200 px-4 py-3">
          <div>
            <h1 className="text-sm font-bold text-zinc-800">색 조합 아카이브</h1>
            <p className="text-xs text-zinc-400">탐색 과정 전체 · {totalCount}개</p>
          </div>
          <button
            onClick={() => setInvert((v) => !v)}
            className="rounded-full border border-zinc-200 bg-white px-2.5 py-1 text-xs font-medium text-zinc-600 hover:border-zinc-400"
          >
            {invert ? "라이트" : "다크"}
          </button>
        </div>

        <div className="flex-1 overflow-y-auto px-2 py-3">
          {groups.map((g) => (
            <div key={g.key} className="mb-3">
              <div className="flex items-center gap-1 px-2 pb-1">
                <span className="text-[11px] font-semibold uppercase tracking-wide text-zinc-400">
                  {g.label}
                </span>
                <span className="text-[11px] text-zinc-300">
                  ({g.ids.length})
                </span>
              </div>
              <div className="space-y-0.5">
                {g.ids.map((id) => {
                  const c = getCombo(id);
                  const isActive = c.id === activeId;
                  return (
                    <button
                      key={c.id}
                      onClick={() => setActiveId(c.id)}
                      title={`${c.id.toUpperCase()} · ${c.name ?? ""}`}
                      className={`flex w-full items-center gap-2 rounded-md px-2 py-1.5 text-left text-xs transition ${
                        isActive
                          ? "bg-zinc-900 text-white"
                          : "text-zinc-600 hover:bg-zinc-100"
                      }`}
                    >
                      <span className="flex shrink-0">
                        {c.hexes.map((hex, i) => (
                          <span
                            key={i}
                            className="h-3.5 w-3.5 rounded-full ring-1 ring-black/10"
                            style={{
                              background: hex,
                              marginLeft: i === 0 ? 0 : -4,
                            }}
                          />
                        ))}
                      </span>
                      <span className="w-7 shrink-0 font-bold">
                        {c.id.toUpperCase()}
                      </span>
                      <span className="min-w-0 flex-1 truncate opacity-80">
                        {c.name ?? ""}
                      </span>
                      {c.final && (
                        <span
                          className={`shrink-0 rounded px-1 text-[10px] font-bold ${
                            isActive
                              ? "bg-white/20 text-white"
                              : "bg-emerald-100 text-emerald-700"
                          }`}
                        >
                          ✓
                        </span>
                      )}
                    </button>
                  );
                })}
              </div>
            </div>
          ))}

          {/* 커스텀 */}
          <div className="mb-3">
            <div className="px-2 pb-1 text-[11px] font-semibold uppercase tracking-wide text-zinc-400">
              커스텀
            </div>
            <button
              onClick={() => setActiveId("custom")}
              className={`flex w-full items-center gap-2 rounded-md px-2 py-1.5 text-left text-xs transition ${
                isCustom
                  ? "bg-zinc-900 text-white"
                  : "text-zinc-600 hover:bg-zinc-100"
              }`}
            >
              <span className="flex shrink-0">
                {customColors.map((hex, i) => (
                  <span
                    key={i}
                    className="h-3.5 w-3.5 rounded-full ring-1 ring-black/10"
                    style={{ background: hex, marginLeft: i === 0 ? 0 : -4 }}
                  />
                ))}
              </span>
              직접 선택
            </button>
            {isCustom && (
              <div className="mt-2 space-y-1.5 px-2">
                {customColors.map((hex, i) => (
                  <div key={i} className="flex items-center gap-2">
                    <input
                      type="color"
                      value={hex}
                      onChange={(e) => setCustomAt(i, e.target.value)}
                      className="h-7 w-9 cursor-pointer rounded border border-zinc-200 bg-white p-0.5"
                      aria-label={`색 ${i + 1} 선택`}
                    />
                    <input
                      type="text"
                      value={hex}
                      onChange={(e) => {
                        let v = e.target.value.trim();
                        if (v && !v.startsWith("#")) v = "#" + v;
                        setCustomAt(i, v);
                      }}
                      spellCheck={false}
                      className="w-24 rounded border border-zinc-200 px-1.5 py-1 font-mono text-xs uppercase text-zinc-700 focus:border-zinc-400 focus:outline-none"
                    />
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </aside>

      {/* 우측 미리보기 */}
      <main
        className="min-w-0 flex-1"
        style={{ background: role.bg, color: role.text, fontFamily }}
      >
        <div className="mx-auto min-h-screen max-w-5xl px-6">
          {/* 네비게이션 */}
          <nav className="flex items-center justify-between py-6">
            <div className="flex items-center gap-2">
              <span
                className="grid h-8 w-8 place-items-center rounded-lg text-sm font-black"
                style={{ background: role.accent, color: role.accentText }}
              >
                {paletteShort}
              </span>
              <span className="text-lg font-bold">Brandname</span>
            </div>
            <div
              className="hidden items-center gap-7 text-sm font-medium sm:flex"
              style={{ color: role.softText }}
            >
              <span>제품</span>
              <span>가격</span>
              <span>문서</span>
              <span>회사소개</span>
            </div>
            <button
              className="rounded-full px-4 py-2 text-sm font-semibold"
              style={{ background: role.accent, color: role.accentText }}
            >
              시작하기
            </button>
          </nav>

          {/* 히어로 */}
          <section className="grid items-center gap-10 py-16 lg:grid-cols-2 lg:py-24">
            <div>
              <span
                className="inline-block rounded-full px-3 py-1 text-xs font-semibold"
                style={{
                  background: role.accent,
                  color: role.accentText,
                }}
              >
                {paletteLabel}
              </span>
              <h1 className="mt-5 text-4xl font-extrabold leading-tight tracking-tight sm:text-5xl lg:text-6xl">
                이 색 조합이
                <br />
                실제 화면에선
                <br />
                <span style={{ color: role.accent }}>이런 느낌</span>입니다.
              </h1>
              <p
                className="mt-6 max-w-md text-lg"
                style={{ color: role.softText }}
              >
                배경·글자·강조 색을 한 조합의 3~4색으로 구성했습니다. 위에서
                조합을 전환하며 분위기를 비교해 보세요.
              </p>
              <div className="mt-8 flex flex-wrap gap-3">
                <button
                  className="rounded-xl px-6 py-3 text-base font-semibold"
                  style={{ background: role.accent, color: role.accentText }}
                >
                  메인 액션
                </button>
                <button
                  className="rounded-xl border-2 px-6 py-3 text-base font-semibold"
                  style={{ borderColor: role.accent2, color: role.accent2 }}
                >
                  보조 액션
                </button>
              </div>
            </div>

            {/* 히어로 비주얼 */}
            <div
              className="relative aspect-[4/3] overflow-hidden rounded-3xl"
              style={{
                background: `linear-gradient(135deg, ${colorRef(0)}, ${colorRef(1)})`,
              }}
            >
              <div
                className="absolute -right-10 -top-10 h-48 w-48 rounded-full opacity-70 blur-2xl"
                style={{ background: colorRef(2) }}
              />
              {hexes.length > 3 && (
                <div
                  className="absolute -bottom-12 -left-8 h-44 w-44 rounded-full opacity-70 blur-2xl"
                  style={{ background: colorRef(3) }}
                />
              )}
              <div
                className="absolute bottom-6 left-6 right-6 rounded-2xl p-5 backdrop-blur"
                style={{
                  background: "rgba(255,255,255,0.14)",
                  border: "1px solid rgba(255,255,255,0.25)",
                  color: "#fff",
                }}
              >
                <div className="text-sm font-semibold opacity-90">
                  미리보기 카드
                </div>
                <div className="mt-2 flex gap-2">
                  {hexes.map((hex, i) => (
                    <span
                      key={i}
                      className="rounded-md px-2 py-1 font-mono text-xs"
                      style={{
                        background: colorRef(i),
                        color: readableText(hex),
                      }}
                    >
                      {hex.toUpperCase()}
                    </span>
                  ))}
                </div>
              </div>
            </div>
          </section>

          {/* 기능 카드 */}
          <section className="grid gap-5 pb-20 sm:grid-cols-3">
            {features.map((f, i) => (
              <div
                key={i}
                className="rounded-2xl border p-6"
                style={{ borderColor: role.border }}
              >
                <div
                  className="grid h-10 w-10 place-items-center rounded-xl text-base font-black"
                  style={
                    i === 1
                      ? { background: role.accent2, color: role.accent2Text }
                      : { background: role.accent, color: role.accentText }
                  }
                >
                  {i + 1}
                </div>
                <h3 className="mt-4 text-lg font-bold">{f.title}</h3>
                <p className="mt-1.5 text-sm" style={{ color: role.softText }}>
                  {f.desc}
                </p>
              </div>
            ))}
          </section>

          {/* CTA 배너 */}
          <section
            className="mb-20 flex flex-col items-center gap-4 rounded-3xl px-8 py-14 text-center"
            style={{ background: role.accent, color: role.accentText }}
          >
            <h2 className="text-2xl font-extrabold sm:text-3xl">
              마음에 드는 조합을 찾으셨나요?
            </h2>
            <p className="max-w-md text-sm opacity-90">
              회의에서 바로 이 화면으로 색감을 공유하고 의견을 모아보세요.
            </p>
            <button
              className="rounded-xl px-6 py-3 text-base font-semibold"
              style={{ background: role.bg, color: role.text }}
            >
              이 조합으로 진행
            </button>
          </section>
        </div>
      </main>
    </div>
  );
}
