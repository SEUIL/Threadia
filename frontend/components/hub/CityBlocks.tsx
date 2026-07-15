"use client";

/*
 * CityBlocks — components/hub/CityBlocks.tsx
 *
 * 허브 배경의 도로. 두 부분으로 나뉜다:
 *   1) 외곽 루프 — 오피스를 감싸는 비대칭 사각 루프(코너 4곳 커브 + 변 4개 직선).
 *      직선이 커브와 맞물리게 lateralOffset=0.5를 준다(사용자가 실측으로 확정).
 *   2) 내부 관통 도로 — 오피스 옆을 가로지르는 가로 2개(H1,H2) + 세로 1개(V).
 *      외곽 루프와 만나는 지점, 서로 만나는 지점에 교차로 에셋을 끼운다.
 *
 * 모듈 크기(Node로 GLTF 노드 트랜스폼까지 적용한 실제 world bounding box 직접
 * 계산 — accessor bounding box만 보면 틀린다):
 *   - road-straight.glb / road-crossroad / road-intersection: 1×1 유닛
 *   - road-curve.glb: 2×2 유닛
 *
 * ★ 교차로를 빈틈없이 끼우는 핵심: 모든 도로를 같은 격자(타일 중심 소수부 .5)에
 *   올린다. 외곽 변은 lateralOffset 0.5 때문에 중심선이 소수부 .5이고, 내부 도로도
 *   시작 좌표를 .5로 맞춘다. 그러면 교차점이 항상 타일 중심에 떨어져서, 그 자리
 *   타일을 skip(RoadStraightRun.skipIndices)하고 같은 높이로 교차로를 끼우면 딱
 *   맞는다. 예전엔 내부(.6)/외곽(.5) 격자가 0.1 어긋나 틈이 생겼었다.
 *
 * 좌표 기준: OfficeFloor(기본 position [0,0,0]) 바닥 범위 x:[-1,6] z:[1,-7].
 * 방향(방향지시물 실측): 동 E=+X / 서 W=-X / 북 N=-Z / 남 S=+Z.
 */
 /*
  메모 : 76f73da 커밋이후 작업
  - 내부 도로 위치값 및 길이를 사용자가 브라우저를 직접 육안으로 관찰하며 조정함
  - 이후 작업 각각 도로가 겹치는 부분 마다 알맞은 교차로 에셋을 배치하여 자연스럽게
    도로가 연결되는 효과를 주어야함

  깨달은점
  - 앞으로 3D 모델 작업시 위치값을 표시하는 무언가를 먼저 브라우저에 배치할것
  - ex)동서남북 표시 & 중앙 표시
  - 정확한 위치지시를 함에도 AI가 못알아 듣는 경우가 너무 많음
    따라서 아예 방향지시기를 임시로 두어 그걸 기준으로 작업하는게 맞을듯함
  */
import { useGLTF } from "@react-three/drei";
import { RoadStraightRun } from "@/components/hub/RoadStraightRun";

const CURVE_MODEL_PATH = "/models/kenney-city-kit-roads/road-curve.glb";
const CROSSROAD_MODEL_PATH = "/models/kenney-city-kit-roads/road-crossroad.glb"; // 4면 사거리
const INTERSECTION_MODEL_PATH = "/models/kenney-city-kit-roads/road-intersection.glb"; // T자
const CURVE_HALF = 1; // road-curve.glb 실측 2x2 유닛의 절반
const STRAIGHT_SIZE = 1; // road-straight.glb 실측 1x1 유닛

const OFFICE_CENTER_X = 2.5;
const OFFICE_CENTER_Z = -3;

// 외곽 루프 네 모서리(월드 유닛). 오피스 바닥 범위보다 바깥에서 비대칭으로 잡았다.
// (이름 NW/NE/…는 축 방향과 안 맞을 수 있어 좌표로만 쓴다.)
const LOOP_CORNERS: { x: number; z: number }[] = [
  { x: OFFICE_CENTER_X - 8.5, z: OFFICE_CENTER_Z + 8 }, // (-6, 5)
  { x: OFFICE_CENTER_X + 10.5, z: OFFICE_CENTER_Z + 8 }, // (13, 5)
  { x: OFFICE_CENTER_X + 10.5, z: OFFICE_CENTER_Z - 9 }, // (13, -12)
  { x: OFFICE_CENTER_X - 8.5, z: OFFICE_CENTER_Z - 9 }, // (-6, -12)
];

const CURVE_ROTATION_OFFSET = Math.PI; // road-curve.glb 기본 배향 보정(사용자 확정)
const CURVE_STRAIGHT_GAP = 0; // 커브-직선 이음매: 틈이면 음수, 겹치면 양수
const STRAIGHT_LATERAL_OFFSET = 0.5; // 외곽 직선이 커브에 맞물리는 보정(사용자 확정)

const INTERIOR_Y = 0.01; // 내부 도로 높이(외곽과 미세 분리)
const JUNCTION_Y = INTERIOR_Y; // 교차로는 도로와 같은 높이(평평하게 끼움)

// 내부 관통 도로 위치(소수부 .5 격자). V는 세로(x 고정), H1/H2는 가로(z 고정).
const V_X = 6.5; // 세로 관통도로
const H1_Z = 1.5; // 가로 관통도로 1(오피스 남쪽 앞)
const H2_Z = -7.5; // 가로 관통도로 2(오피스 북쪽 뒤)

// 내부 교차점 2곳.
const CROSSROAD_POS: [number, number] = [V_X, H1_Z]; // H1 × V (4면)
const CROSSROAD_ROTATION = 0;
const T_JUNCTION_POS: [number, number] = [V_X, H2_Z]; // H2 동쪽 끝 × V (T자, 막힌 동)
const T_JUNCTION_ROTATION = -Math.PI / 2; // 막힌 쪽=동(사용자 확정)

// 도로 시작 좌표에서 교차점까지 거리를 타일 인덱스로(간격 1, +/-  무관 abs).
function nearestIndex(start: number, target: number) {
  return Math.round(Math.abs(target - start));
}

type Edge = { startX: number; startZ: number; rotationY: number; count: number };

function buildEdges(): Edge[] {
  const edges: Edge[] = [];
  const n = LOOP_CORNERS.length;
  for (let c = 0; c < n; c++) {
    const cur = LOOP_CORNERS[c];
    const next = LOOP_CORNERS[(c + 1) % n];
    const dx = Math.sign(next.x - cur.x);
    const dz = Math.sign(next.z - cur.z);
    const edgeLength = Math.max(Math.abs(next.x - cur.x), Math.abs(next.z - cur.z));
    const usable = edgeLength - CURVE_HALF * 2;
    const count = Math.max(0, Math.round(usable / STRAIGHT_SIZE));
    const startOffset = CURVE_HALF + STRAIGHT_SIZE / 2 + CURVE_STRAIGHT_GAP;
    edges.push({
      startX: cur.x + dx * startOffset,
      startZ: cur.z + dz * startOffset,
      rotationY: c * (Math.PI / 2),
      count,
    });
  }
  return edges;
}

// 외곽 변의 타일 중, 주어진 월드 좌표에 가장 가까운 타일 인덱스. 변의 진행 방향은
// rotationY(0:+x, π/2:-z, π:-x, 3π/2:+z)로 판별한다.
function edgeIndexAt(edge: Edge, x: number, z: number): number {
  const q = (Math.round(edge.rotationY / (Math.PI / 2)) % 4 + 4) % 4;
  if (q === 0) return Math.round(x - edge.startX); // +x
  if (q === 1) return Math.round(edge.startZ - z); // -z
  if (q === 2) return Math.round(edge.startX - x); // -x
  return Math.round(z - edge.startZ); // +z
}

export function CityBlocks() {
  const { scene: curveScene } = useGLTF(CURVE_MODEL_PATH);
  const { scene: crossroadScene } = useGLTF(CROSSROAD_MODEL_PATH);
  const { scene: intersectionScene } = useGLTF(INTERSECTION_MODEL_PATH);
  const edges = buildEdges();

  const westX = LOOP_CORNERS[0].x; // -6
  const eastX = LOOP_CORNERS[1].x; // 13
  const zHi = LOOP_CORNERS[0].z; // 5  (남쪽 +Z 코너)
  const zLo = LOOP_CORNERS[3].z; // -12 (북쪽 -Z 코너)
  const horizontalCount = Math.round(eastX - westX); // 19
  const verticalCount = Math.round(zHi - zLo); // 17

  // 내부 도로 시작 좌표(진행 축). 소수부 .5로 격자 정렬 → 교차점이 타일 중심에 옴.
  const h1StartX = westX + 0.5; // -5.5, +x
  const h2StartX = westX + 0.5; // -5.5, +x
  const vStartZ = zHi - 0.5; // 4.5, -z

  // 외곽 변 중심선(코너 ±0.5 바깥, lateralOffset 반영).
  const eastEdgeX = eastX + STRAIGHT_LATERAL_OFFSET; // 13.5
  const westEdgeX = westX - STRAIGHT_LATERAL_OFFSET; // -6.5
  const southEdgeZ = zHi + STRAIGHT_LATERAL_OFFSET; // 5.5  (+Z/남)
  const northEdgeZ = zLo - STRAIGHT_LATERAL_OFFSET; // -12.5 (-Z/북)

  // 내부 교차점(2곳) 자리 타일 skip.
  const h1Skip = [nearestIndex(h1StartX, CROSSROAD_POS[0])]; // H1 × 사거리
  const vSkip = [
    nearestIndex(vStartZ, CROSSROAD_POS[1]), // V × 사거리
    nearestIndex(vStartZ, T_JUNCTION_POS[1]), // V × T자
  ];
  const h2Skip = [nearestIndex(h2StartX, T_JUNCTION_POS[0])]; // H2 × T자

  // 외곽 루프와 만나는 T자 5곳. pos=외곽 변 중심선(관통도로에 정렬), rotationY는
  // 막힌 방향이 관통도로 반대편이 되게. 내부 T자(-π/2=막힌 동) 기준으로 180° 뒤집으면
  // 막힌 서(π/2). ★ 남/북 막힘(V 끝 2곳)은 90° 회전이라 방향이 애매해 추정값을
  // 넣었으니 브라우저에서 확인 후 보정(안 맞으면 두 값을 서로 바꾸면 됨).
  const outerJunctions: { pos: [number, number]; rotationY: number; edge: number }[] = [
    { pos: [eastEdgeX, H1_Z], rotationY: -Math.PI / 2, edge: 1 }, // H1 동 → 동쪽변(막힌 동)
    { pos: [westEdgeX, H1_Z], rotationY: Math.PI / 2, edge: 3 }, // H1 서 → 서쪽변(막힌 서)
    { pos: [westEdgeX, H2_Z], rotationY: Math.PI / 2, edge: 3 }, // H2 서 → 서쪽변(막힌 서)
    { pos: [V_X, southEdgeZ], rotationY: Math.PI, edge: 0 }, // V 남 → 남쪽변(사용자 확인: 180° 뒤집음)
    { pos: [V_X, northEdgeZ], rotationY: 0, edge: 2 }, // V 북 → 북쪽변(사용자 확인: 180° 뒤집음)
  ];

  // 외곽 변별 skip 타일 인덱스(교차로가 들어갈 자리).
  const edgeSkips: Record<number, number[]> = {};
  for (const j of outerJunctions) {
    const idx = edgeIndexAt(edges[j.edge], j.pos[0], j.pos[1]);
    (edgeSkips[j.edge] ??= []).push(idx);
  }

  return (
    <group>
      {/* 외곽 루프 — 직선 변(교차점 자리 타일 skip) */}
      {edges.map((edge, index) => (
        <RoadStraightRun
          key={`edge-${index}`}
          count={edge.count}
          position={[edge.startX, 0, edge.startZ]}
          rotationY={edge.rotationY}
          lateralOffset={STRAIGHT_LATERAL_OFFSET}
          skipIndices={edgeSkips[index] ?? []}
        />
      ))}
      {/* 외곽 루프 — 코너 커브 */}
      {LOOP_CORNERS.map((corner, index) => (
        <primitive
          key={`corner-${index}`}
          object={curveScene.clone()}
          position={[corner.x, 0, corner.z]}
          rotation={[0, CURVE_ROTATION_OFFSET + index * (Math.PI / 2), 0]}
        />
      ))}
      {/* 내부 가로 관통 도로 H1 (사거리 자리 타일 skip) */}
      <RoadStraightRun
        count={horizontalCount}
        position={[h1StartX, INTERIOR_Y, H1_Z]}
        rotationY={0}
        lateralOffset={0}
        skipIndices={h1Skip}
      />
      {/* 내부 세로 관통 도로 V (사거리·T자 자리 타일 skip) */}
      <RoadStraightRun
        count={verticalCount}
        position={[V_X, INTERIOR_Y, vStartZ]}
        rotationY={Math.PI / 2}
        lateralOffset={0}
        skipIndices={vSkip}
      />
      {/* 내부 가로 관통 도로 H2 (동쪽 끝이 T자에 닿음, T자 자리 타일 skip) */}
      <RoadStraightRun
        count={verticalCount - 4}
        position={[h2StartX, INTERIOR_Y, H2_Z]}
        rotationY={Math.PI * 2}
        lateralOffset={0}
        skipIndices={h2Skip}
      />
      {/* 내부 교차로 — 남쪽 사거리 */}
      <primitive
        object={crossroadScene.clone()}
        position={[CROSSROAD_POS[0], JUNCTION_Y, CROSSROAD_POS[1]]}
        rotation={[0, CROSSROAD_ROTATION, 0]}
      />
      {/* 내부 교차로 — 북쪽 T자 */}
      <primitive
        object={intersectionScene.clone()}
        position={[T_JUNCTION_POS[0], JUNCTION_Y, T_JUNCTION_POS[1]]}
        rotation={[0, T_JUNCTION_ROTATION, 0]}
      />
      {/* 외곽 T자 5곳 */}
      {outerJunctions.map((j, index) => (
        <primitive
          key={`outer-junction-${index}`}
          object={intersectionScene.clone()}
          position={[j.pos[0], JUNCTION_Y, j.pos[1]]}
          rotation={[0, j.rotationY, 0]}
        />
      ))}
    </group>
  );
}
