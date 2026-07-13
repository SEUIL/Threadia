"use client";

/*
 * RoadStraightRun — components/hub/RoadStraightRun.tsx
 *
 * road-straight.glb(Kenney City Kit Roads)를 count개 이어붙여 일자 도로를 만드는
 * 재사용 컴포넌트. 이전 버전은 GLB accessor bounding box(2x2x2)만 보고 간격/회전을
 * 가정했다가 실제로는 도로가 끊기고 방향도 반대로 나왔다 — 이번엔 그 가정을 버리고
 * 이 컴포넌트 하나만 독립적으로 렌더링해서 간격(TILE_SPACING)과 회전
 * (ROTATION_Y)을 스크린샷으로 실측하며 맞춘다. 맞춰지면 CityBlocks의 루프에
 * 다시 조립한다.
 *
 * 로컬 좌표계: 이 컴포넌트의 로컬 +X 방향으로 타일을 이어붙인다. 배치할 때는
 * group에 position/rotation을 얹어서 원하는 위치·방향으로 돌려 쓴다.
 */
import { useGLTF } from "@react-three/drei";

const MODEL_PATH = "/models/kenney-city-kit-roads/road-straight.glb";

// 실측 완료(Node로 GLTF 노드 트랜스폼까지 적용한 world bounding box 직접 계산):
// road-straight.glb는 accessor bounding box(2x2x2)와 달리 실제로는 1x1 유닛
// 풋프린트다(road-curve.glb는 실측해도 2x2 유닛이 맞음 — 도로 종류마다 모듈
// 크기가 다르다). 회전은 기본 배향이 로컬 Z축이라 90도 돌려야 이어붙이는
// +X 방향과 맞는다.
const TILE_SPACING = 1;
const ROTATION_Y =  Math.PI * 2;

export function RoadStraightRun({
  count,
  position = [0, 0, 0],
  rotationY = 0,
  lateralOffset = 0,
}: {
  count: number;
  position?: [number, number, number];
  rotationY?: number;
  /** 진행 방향(로컬 +X) 기준 좌우로 도로를 밀 때 쓴다 — 커브랑 옆으로 안 맞을 때 조정. */
  lateralOffset?: number;
}) {
  const { scene } = useGLTF(MODEL_PATH);

  return (
    <group position={position} rotation={[0, rotationY, 0]}>
      {Array.from({ length: count }, (_, i) => (
        <primitive
          key={i}
          object={scene.clone()}
          position={[i * TILE_SPACING, 0, lateralOffset]}
          rotation={[0, ROTATION_Y, 0]}
        />
      ))}
    </group>
  );
}
