"use client";

/*
 * HubBuildings — components/hub/HubBuildings.tsx
 *
 * 도로로 나뉜 빈 구역(plot)에 Kenney City Kit Commercial 빌딩을 시드 기반으로
 * 들쭉날쭉하게 채운다(도심 스카이라인). plot은 도로/오피스와 안 겹치는 사각형
 * 영역이고, 그 안을 격자로 훑으며 각 칸에 건물 종류/회전/미세 위치를 seededRandom
 * (초대 봉투에서 만든 결정적 난수)으로 골라 배치한다 — 리렌더에도 안 흔들린다.
 *
 * 건물 풋프린트는 실측상 대략 1~1.6 유닛, 마천루는 높이 ~2.9. 종류를 섞으면
 * 높이가 자연히 들쭉날쭉해진다. 좌표/방향은 CityBlocks와 같은 월드 기준
 * (동 E=+X / 서 W=-X / 북 N=-Z / 남 S=+Z).
 */
import { useGLTF } from "@react-three/drei";
import { seededRandom } from "@/lib/seededRandom";

const BASE = "/models/kenney-city-kit-commercial";

// 스카이라인용 건물 후보 — 중층 빌딩 + 마천루 섞음.
const MODELS = [
  "building-a",
  "building-c",
  "building-e",
  "building-g",
  "building-i",
  "building-k",
  "building-m",
  "building-skyscraper-a",
  "building-skyscraper-c",
  "building-skyscraper-e",
];

MODELS.forEach((m) => useGLTF.preload(`${BASE}/${m}.glb`));

export type Plot = { xMin: number; xMax: number; zMin: number; zMax: number };

function seededRange(seed: number, ch: number, min: number, max: number) {
  return min + ((seededRandom(seed, ch) + 1) / 2) * (max - min);
}
function seededPick<T>(seed: number, ch: number, arr: T[]): T {
  const i = Math.floor(((seededRandom(seed, ch) + 1) / 2) * arr.length) % arr.length;
  return arr[i];
}

export function HubBuildings({
  plots,
  spacing = 2,
  seed = 0,
}: {
  plots: Plot[];
  spacing?: number;
  seed?: number;
}) {
  // 경로 배열을 한 번에 로드(useGLTF는 배열을 받으면 결과 배열을 돌려준다) —
  // map 안에서 훅을 호출하지 않도록.
  const gltfs = useGLTF(MODELS.map((m) => `${BASE}/${m}.glb`));
  const sceneByName: Record<string, (typeof gltfs)[number]["scene"]> = Object.fromEntries(
    MODELS.map((m, i) => [m, gltfs[i].scene])
  );

  const items: React.ReactNode[] = [];
  let idx = 0;
  for (const p of plots) {
    const cols = Math.max(1, Math.floor((p.xMax - p.xMin) / spacing));
    const rows = Math.max(1, Math.floor((p.zMax - p.zMin) / spacing));
    for (let c = 0; c < cols; c++) {
      for (let r = 0; r < rows; r++) {
        const s = seed * 1000 + idx++;
        const cx = p.xMin + spacing / 2 + c * spacing + seededRange(s, 1, -0.3, 0.3);
        const cz = p.zMin + spacing / 2 + r * spacing + seededRange(s, 2, -0.3, 0.3);
        const model = seededPick(s, 3, MODELS);
        const rot = Math.floor(((seededRandom(s, 4) + 1) / 2) * 4) * (Math.PI / 2);
        items.push(
          <primitive
            key={s}
            object={sceneByName[model].clone()}
            position={[cx, 0, cz]}
            rotation={[0, rot, 0]}
          />
        );
      }
    }
  }

  return <group>{items}</group>;
}
