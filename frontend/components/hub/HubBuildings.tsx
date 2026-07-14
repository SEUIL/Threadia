"use client";

/*
 * HubBuildings — components/hub/HubBuildings.tsx
 *
 * 도로로 나뉜 빈 구역(plot)을 Kenney City Kit 에셋으로 시드 기반 채운다.
 * plot은 도로/오피스와 안 겹치는 사각형이고, 그 안을 격자로 훑으며 각 칸에
 * 건물 종류/회전/미세 위치를 seededRandom(초대 봉투에서 만든 결정적 난수)으로
 * 골라 배치한다 — 리렌더에도 안 흔들린다.
 *
 * models로 어떤 세트를 쓸지 정한다:
 *   - COMMERCIAL_MODELS: 도심 빌딩(중층+마천루) → 들쭉날쭉 스카이라인
 *   - SUBURBAN_MODELS:   주택 → 아기자기한 주택가
 * treeModels를 주면 일부 칸을 나무(작은 군집)로 대체해 녹지를 섞는다.
 *
 * 좌표/방향은 CityBlocks와 같은 월드 기준(동 E=+X / 서 W=-X / 북 N=-Z / 남 S=+Z).
 */
import { useGLTF } from "@react-three/drei";
import { seededRandom } from "@/lib/seededRandom";

const COMMERCIAL_BASE = "/models/kenney-city-kit-commercial";
const SUBURBAN_BASE = "/models/kenney-city-kit-suburban";

// 도심 빌딩(중층 + 마천루).
export const COMMERCIAL_MODELS = [
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
].map((m) => `${COMMERCIAL_BASE}/${m}.glb`);

// 주택.
export const SUBURBAN_MODELS = [
  "building-type-a",
  "building-type-b",
  "building-type-d",
  "building-type-f",
  "building-type-h",
  "building-type-j",
  "building-type-l",
  "building-type-n",
  "building-type-p",
  "building-type-r",
].map((m) => `${SUBURBAN_BASE}/${m}.glb`);

// 나무.
export const TREE_MODELS = ["tree-large", "tree-small"].map((m) => `${SUBURBAN_BASE}/${m}.glb`);

export type Plot = { xMin: number; xMax: number; zMin: number; zMax: number };

function seededRange(seed: number, ch: number, min: number, max: number) {
  return min + ((seededRandom(seed, ch) + 1) / 2) * (max - min);
}
function seededPickIndex(seed: number, ch: number, len: number) {
  return Math.floor(((seededRandom(seed, ch) + 1) / 2) * len) % len;
}

export function HubBuildings({
  plots,
  models = COMMERCIAL_MODELS,
  treeModels = [],
  treeChance = 0,
  spacing = 2,
  seed = 0,
}: {
  plots: Plot[];
  models?: string[];
  treeModels?: string[];
  /** 각 칸이 나무 군집이 될 확률(0~1). treeModels가 있을 때만 의미. */
  treeChance?: number;
  spacing?: number;
  seed?: number;
}) {
  // 건물+나무 경로를 한 번에 로드(useGLTF는 배열을 받으면 결과 배열을 준다).
  const allPaths = [...models, ...treeModels];
  const gltfs = useGLTF(allPaths);
  const buildingScenes = gltfs.slice(0, models.length).map((g) => g.scene);
  const treeScenes = gltfs.slice(models.length).map((g) => g.scene);

  const items: React.ReactNode[] = [];
  let idx = 0;
  for (const p of plots) {
    const cols = Math.max(1, Math.floor((p.xMax - p.xMin) / spacing));
    const rows = Math.max(1, Math.floor((p.zMax - p.zMin) / spacing));
    for (let c = 0; c < cols; c++) {
      for (let r = 0; r < rows; r++) {
        const s = seed * 1000 + idx++;
        const cellX = p.xMin + spacing / 2 + c * spacing;
        const cellZ = p.zMin + spacing / 2 + r * spacing;

        const isTreeCell =
          treeScenes.length > 0 && (seededRandom(s, 0) + 1) / 2 < treeChance;

        if (isTreeCell) {
          // 나무 군집 2그루.
          for (let t = 0; t < 2; t++) {
            const tx = cellX + seededRange(s, 10 + t, -0.5, 0.5);
            const tz = cellZ + seededRange(s, 20 + t, -0.5, 0.5);
            const tree = treeScenes[seededPickIndex(s, 30 + t, treeScenes.length)];
            items.push(
              <primitive key={`${s}-t${t}`} object={tree.clone()} position={[tx, 0, tz]} />
            );
          }
        } else {
          const bx = cellX + seededRange(s, 1, -0.3, 0.3);
          const bz = cellZ + seededRange(s, 2, -0.3, 0.3);
          const building = buildingScenes[seededPickIndex(s, 3, buildingScenes.length)];
          const rot = Math.floor(((seededRandom(s, 4) + 1) / 2) * 4) * (Math.PI / 2);
          items.push(
            <primitive
              key={s}
              object={building.clone()}
              position={[bx, 0, bz]}
              rotation={[0, rot, 0]}
            />
          );
        }
      }
    }
  }

  return <group>{items}</group>;
}
