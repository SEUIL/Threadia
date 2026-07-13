"use client";

/*
 * CityBlocks — components/hub/CityBlocks.tsx
 *
 * 허브 배경을 "격자형 도심(#)"으로 만드는 컴포넌트. OfficeFloor가 차지한 정중앙
 * 블록만 비우고, 나머지 블록에 인도 패드 + 박스 건물 1개씩을 올린다.
 * "도로"는 따로 스트립을 그리지 않고 큰 아스팔트 평면 위에 인도 패드를 셀 크기보다
 * 작게 얹어서 패드 사이 빈틈이 자연히 도로처럼 보이게 한다 — 실제 도로 스트립을
 * 모델링하는 것보다 훨씬 간단하면서 결과는 동일하다.
 *
 * 좌표 기준: OfficeFloor(기본 position [0,0,0])의 실제 바닥 범위가
 * x:[-1,6] z:[1,-7](OfficeFloor.tsx FLOOR_X_START/FLOOR_X_TILES 등 기준)라서,
 * 격자 중심을 월드 원점이 아니라 그 중심((2.5, -3))에 맞춘다.
 *
 * 건물 폭/깊이/높이/색은 invitation 봉투 기능에서 만든 seededRandom(블록 인덱스를
 * 시드로)으로 고정한다 — 리렌더돼도 스카이라인이 안 흔들린다.
 */
import { Instance, Instances } from "@react-three/drei";
import { seededRandom } from "@/lib/seededRandom";

const GRID_SIZE = 5; // 5x5 블록, 가운데 블록은 오피스 자리라 비운다
const CENTER_INDEX = Math.floor(GRID_SIZE / 2);
const CELL_SIZE = 12; // 블록 하나가 차지하는 칸 크기(도로 포함)
const PAD_SIZE = 8; // 인도 패드 한 변 — CELL_SIZE보다 작아야 틈이 도로로 보인다
const PAD_HEIGHT = 0.15;

const OFFICE_CENTER_X = 2.5;
const OFFICE_CENTER_Z = -3;

const ROAD_COLOR = "#4b4f54";
const PAD_COLOR = "#d6d3cd"; // OfficeFloor.tsx의 바닥(FLOOR_COLOR_OVERRIDES.wood)과 같은 톤

// 건물 폭/깊이/높이 범위와 색 팔레트 — "모던 콘크리트" 계열 뉴트럴 톤만 사용
const BUILDING_MIN_SIZE = 3;
const BUILDING_MAX_SIZE = 6;
const BUILDING_MIN_HEIGHT = 3;
const BUILDING_MAX_HEIGHT = 12; // 16이면 첫 번째 링에서 카메라와 너무 가까워 답답해 보임
const BUILDING_COLORS = ["#e4e1da", "#c7c4bd", "#b8bcc0", "#a7a29a", "#d9d6ce"];

function blockCenter(i: number, j: number): [number, number] {
  return [
    OFFICE_CENTER_X + (i - CENTER_INDEX) * CELL_SIZE,
    OFFICE_CENTER_Z + (j - CENTER_INDEX) * CELL_SIZE,
  ];
}

// seededRandom은 [-1,1)을 반환하므로 [min,max) 범위로 옮긴다.
function seededRange(seed: number, channel: number, min: number, max: number) {
  return min + ((seededRandom(seed, channel) + 1) / 2) * (max - min);
}

export function CityBlocks() {
  const roadSize = GRID_SIZE * CELL_SIZE + CELL_SIZE; // 격자보다 한 칸 더 여유

  const blocks: { x: number; z: number; seed: number }[] = [];
  for (let i = 0; i < GRID_SIZE; i++) {
    for (let j = 0; j < GRID_SIZE; j++) {
      if (i === CENTER_INDEX && j === CENTER_INDEX) continue; // 오피스 자리는 비움
      const [x, z] = blockCenter(i, j);
      blocks.push({ x, z, seed: i * GRID_SIZE + j });
    }
  }

  return (
    <group>
      {/* 아스팔트 바닥 — 격자 전체를 덮는 평면 하나(도로 역할) */}
      <mesh rotation={[-Math.PI / 2, 0, 0]} position={[OFFICE_CENTER_X, 0, OFFICE_CENTER_Z]}>
        <planeGeometry args={[roadSize, roadSize]} />
        <meshStandardMaterial color={ROAD_COLOR} />
      </mesh>

      {/* 인도 패드 — 블록마다 하나, 전부 동일 geometry라 Instances로 묶어 드로우콜을 아낀다 */}
      <Instances limit={GRID_SIZE * GRID_SIZE}>
        <boxGeometry args={[PAD_SIZE, PAD_HEIGHT, PAD_SIZE]} />
        <meshStandardMaterial color={PAD_COLOR} />
        {blocks.map(({ x, z }, index) => (
          <Instance key={index} position={[x, PAD_HEIGHT / 2, z]} />
        ))}
      </Instances>

      {/* 건물 — 패드 위에 블록당 1개. 단위 박스(1x1x1)를 인스턴스마다 scale로 늘려
          쓰므로 크기가 제각각이어도 geometry는 하나만 공유한다(드로우콜 1개 유지). */}
      <Instances limit={GRID_SIZE * GRID_SIZE}>
        <boxGeometry args={[1, 1, 1]} />
        <meshStandardMaterial />
        {blocks.map(({ x, z, seed }, index) => {
          const width = seededRange(seed, 10, BUILDING_MIN_SIZE, BUILDING_MAX_SIZE);
          const depth = seededRange(seed, 11, BUILDING_MIN_SIZE, BUILDING_MAX_SIZE);
          const height = seededRange(seed, 12, BUILDING_MIN_HEIGHT, BUILDING_MAX_HEIGHT);
          const colorIndex = Math.min(
            BUILDING_COLORS.length - 1,
            Math.floor(seededRange(seed, 13, 0, BUILDING_COLORS.length))
          );
          return (
            <Instance
              key={index}
              position={[x, PAD_HEIGHT + height / 2, z]}
              scale={[width, height, depth]}
              color={BUILDING_COLORS[colorIndex]}
            />
          );
        })}
      </Instances>
    </group>
  );
}
