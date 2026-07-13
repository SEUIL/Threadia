"use client";

/*
 * CityBlocks — components/hub/CityBlocks.tsx
 *
 * 허브 배경의 도로. 두 부분으로 나뉜다:
 *   1) 외곽 루프 — 오피스를 감싸는 비대칭 사각 루프(코너 4곳 커브 + 변 4개 직선).
 *      직선이 커브와 맞물리게 lateralOffset=0.5를 준다(사용자가 실측으로 확정).
 *   2) 내부 십자 도로 — 오피스 북쪽/동쪽에 붙는 가로·세로 관통 도로(사용자가
 *      스크린샷에 빨간 선으로 지정). 커브에 이어붙이지 않고 중앙 정렬(offset 0)된
 *      독립 직선으로 그냥 가로지른다. 외곽 루프와 만나는 4점은 살짝 겹치는데,
 *      coplanar z-fighting을 피하려고 내부 도로만 y를 아주 살짝(INTERIOR_Y) 띄운다.
 *
 * 모듈 크기(Node로 GLTF 노드 트랜스폼까지 적용한 실제 world bounding box 직접
 * 계산 — accessor bounding box만 보면 틀린다):
 *   - road-straight.glb: 1×1 유닛 (RoadStraightRun이 TILE_SPACING=1로 이어붙임)
 *   - road-curve.glb:    2×2 유닛
 *
 * ★ 왜 lateralOffset을 외곽/내부가 다르게 쓰나: 0.5는 "직선을 커브 도로 중심선에
 *   맞추려고" 준 보정값이다. 커브에 이어지지 않는 내부 도로는 이 보정이 필요 없어
 *   0(중앙 정렬)을 쓴다. 이걸 전부 0.5로 통일했다가 교차 지점이 다 어긋났었다.
 *
 * 좌표 기준: OfficeFloor(기본 position [0,0,0]) 바닥 범위 x:[-1,6] z:[1,-7].
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
const CURVE_HALF = 1; // road-curve.glb 실측 2x2 유닛의 절반
const STRAIGHT_SIZE = 1; // road-straight.glb 실측 1x1 유닛

const OFFICE_CENTER_X = 2.5;
const OFFICE_CENTER_Z = -3;

// 외곽 루프 네 모서리(월드 유닛). 오피스 바닥 범위보다 바깥에서 비대칭으로 잡았다.
const LOOP_CORNERS: { x: number; z: number }[] = [
  { x: OFFICE_CENTER_X - 8.5, z: OFFICE_CENTER_Z + 8 }, // NW
  { x: OFFICE_CENTER_X + 10.5, z: OFFICE_CENTER_Z + 8 }, // NE
  { x: OFFICE_CENTER_X + 10.5, z: OFFICE_CENTER_Z - 9 }, // SE
  { x: OFFICE_CENTER_X - 8.5, z: OFFICE_CENTER_Z - 9 }, // SW
];

const CURVE_ROTATION_OFFSET = Math.PI; // road-curve.glb 기본 배향 보정(사용자 확정)
const CURVE_STRAIGHT_GAP = 0; // 커브-직선 이음매: 틈이면 음수, 겹치면 양수
const STRAIGHT_LATERAL_OFFSET = 0.5; // 외곽 직선이 커브에 맞물리는 보정(사용자 확정)

// 내부 십자 도로 — 오피스 북쪽 벽(z=1)/동쪽 벽(x=6) 바로 바깥에 붙인다.
const INTERIOR_STREET_X = 6.6;
const INTERIOR_STREET_Z = 1.6;
const INTERIOR_Y = 0.01; // 외곽과 겹치는 지점 z-fighting 방지용 미세 높이

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

export function CityBlocks() {
  const { scene: curveScene } = useGLTF(CURVE_MODEL_PATH);
  const edges = buildEdges();

  // 내부 가로 도로: 서쪽 변(x=NW.x)에서 동쪽 변(x=NE.x)까지 +x로 관통.
  const westX = LOOP_CORNERS[0].x;
  const eastX = LOOP_CORNERS[1].x;
  const horizontalCount = Math.round(eastX - westX);
  // 내부 세로 도로: 북쪽 변(z=NW.z)에서 남쪽 변(z=SW.z)까지 -z로 관통.
  const northZ = LOOP_CORNERS[0].z;
  const southZ = LOOP_CORNERS[3].z;
  const verticalCount = Math.round(northZ - southZ);

  return (
    <group>
      {/* 외곽 루프 — 직선 변 */}
      {edges.map((edge, index) => (
        <RoadStraightRun
          key={`edge-${index}`}
          count={edge.count}
          position={[edge.startX, 0, edge.startZ]}
          rotationY={edge.rotationY}
          lateralOffset={STRAIGHT_LATERAL_OFFSET}
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
      {/* 내부 가로 관통 도로 (중앙 정렬, offset 0) */}
      <RoadStraightRun
        count={horizontalCount}
        position={[westX + 0.5, INTERIOR_Y, INTERIOR_STREET_Z]}
        rotationY={0}
        lateralOffset={0}
      />
      {/* 내부 세로 관통 도로 (중앙 정렬, offset 0) */}
      <RoadStraightRun
        count={verticalCount}
        position={[INTERIOR_STREET_X, INTERIOR_Y, northZ - 0.5]}
        rotationY={Math.PI / 2}
        lateralOffset={0}
      />
      {/* 내부 세로 관통 도로 2번째 — 오피스 출입문(남쪽 벽) 기준 오른쪽(동쪽)으로
          하나 더, 같은 방식(중앙 정렬, offset 0)으로 평행하게 추가. */}
      <RoadStraightRun
        count={verticalCount - 4}
        position={[-INTERIOR_STREET_X +1, INTERIOR_Y, -northZ * 1.55]}
        rotationY={Math.PI * 2}
        lateralOffset={0}
      />
    </group>
  );
}
