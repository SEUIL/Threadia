"use client";

/*
 * S03 — 메인 허브 (개발 중)
 *
 * 현재: 방 모듈(OfficeDesk 8개, 4x2 격자, 전부 같은 방향) 배치 실험 중.
 *   - 방 모듈 고정 수용인원: 8명(4x2). 팀원 수는 좌석 점유 여부만 바꾸고
 *     방 크기·장식 배치는 항상 고정한다.
 *   - 팀원이 8명을 넘으면 같은 방 모듈을 옆으로 증축한다(층을 새로 쌓지 않음).
 * 다음: 건물/층 구조, role 분기, 카메라 전환(Overview↔Floor), 캐릭터/조명 연출.
 *
 * ⚠️ DEV DEBUG: DebugHelpers / CameraTracker / MaterialColorPanel 은 개발 보조용.
 *    3D 작업 마무리 시 components/dev/* 와 함께 제거한다.
 */
import { Suspense, useRef } from "react";
import { Canvas } from "@react-three/fiber";
import { OrbitControls, useGLTF } from "@react-three/drei";
import * as THREE from "three";
import { OfficeDesk } from "@/components/hub/OfficeDesk";
import { DebugHelpers, CameraTracker } from "@/components/dev/SceneDebug";
import { SceneCapture, MaterialColorPanel } from "@/components/dev/MaterialColorTool";

const MODEL_BASE = "/models/kenney-furniture-kit";

// 방 모듈 격자 실험값 — 배치 확인 후 조정
const GRID_COLS = 4;
const GRID_ROWS = 2;
const COL_SPACING = 1.0; // OfficeDesk 폭(1.0m)과 동일 → 옆 책상과 맞닿게
const ROW_SPACING = 2.2; // 두 줄 사이 통로 확보용

// 책상 블록 실측: x[0.9,4.9](4m) z[-4.53,-1.3](3.23m).
// 장식(화분·러그 등)과 입구 쪽 로비 공간을 위해 여유를 두고 방 크기를 잡는다.
const FLOOR_X_START = -1; // x: [-1, 6] = 7m, 책상 좌우 여유 1.9m/1.1m
const FLOOR_X_TILES = 7;
const FLOOR_Z_START = 1; // z: [1, -7] = 8m, 입구 쪽 로비 2.3m + 뒤쪽 여유 2.47m
const FLOOR_Z_TILES = 8;

function Floor() {
  const { scene } = useGLTF(`${MODEL_BASE}/floorFull.glb`);
  const tiles = [];
  for (let i = 0; i < FLOOR_X_TILES; i++) {
    for (let j = 0; j < FLOOR_Z_TILES; j++) {
      tiles.push(
        <primitive
          key={`${i}-${j}`}
          object={scene.clone()}
          position={[FLOOR_X_START + i, 0, FLOOR_Z_START - j]}
        />
      );
    }
  }
  return <>{tiles}</>;
}

// wall.glb 실측: x[0,1] z[-0.05,0] (길이 1m, 두께 0.05m, 로컬 x축을 따라 뻗음).
// 회전 없이 X축으로 이어붙이면 앞/뒤 벽, 90도 회전하면 Z축을 따라 좌/우 벽이 된다.
// 코너 전용 모델(wallCorner)·출입구(wallDoorway)는 1차 확인 후 추가한다.
const DOORWAY_RIGHT_WALL_J = 6; // 오른쪽 벽에서 출입구로 교체할 세그먼트(뒤쪽 벽에서 약 1~2m)

// 왼쪽 스트립(x[-1,2])을 가로지르는 칸막이 벽 — 라운지 룸(z<DIVIDER_Z)과
// 키친/로비존(z>DIVIDER_Z)을 나눈다.
const DIVIDER_X_START = -1;
const DIVIDER_X_TILES = 3; // x: [-1, 2]
const DIVIDER_Z = -4;
const DIVIDER_DOORWAY_I = 1; // 가운데 칸(x[0,1])을 출입구로

// 키친/로비존을 오른쪽까지 감싸는 세로 벽 — x=2(가로 divider 오른쪽 끝)에서
// 앞 벽(z=1)까지 이어진다.
const MIDDLE_DIVIDER_X = 2;
const MIDDLE_DIVIDER_Z_START = 1;
const MIDDLE_DIVIDER_Z_TILES = 5; // z: [1, -4]
const MIDDLE_DIVIDER_DOORWAY_J = 0; // 로비와 가장 가까운 칸(z[0,1])을 출입구로

// 가로 divider를 오른쪽 벽(x=6)까지 연장 — x=2에서 x=6까지 4칸.
const DIVIDER_EXT_X_START = 2;
const DIVIDER_EXT_X_TILES = 4; // x: [2, 6]
const DIVIDER_EXT_DOORWAY_I = 2; // x[4,5] 칸을 출입구로 (스크린샷 표시 위치 기준)

function RoomWalls() {
  const { scene } = useGLTF(`${MODEL_BASE}/wall.glb`);
  const { scene: doorwayScene } = useGLTF(`${MODEL_BASE}/wallDoorway.glb`);
  const walls = [];

  // 앞 벽 (z = FLOOR_Z_START, 로비 쪽) — X축을 따라 FLOOR_X_TILES 만큼.
  // 안쪽 면이 어두운 색이라 180도 뒤집는다. 뒤집으면 로컬 x[0,1]이 [-1,0]로
  // 반전되므로 같은 자리에 오도록 position.x 에 +1 보정을 더한다.
  for (let i = 0; i < FLOOR_X_TILES; i++) {
    walls.push(
      <primitive
        key={`front-${i}`}
        object={scene.clone()}
        position={[FLOOR_X_START + i + 1, 0, FLOOR_Z_START]}
        rotation={[0, Math.PI, 0]}
      />
    );
  }
  // 뒤 벽 (z = FLOOR_Z_START - FLOOR_Z_TILES)
  for (let i = 0; i < FLOOR_X_TILES; i++) {
    walls.push(
      <primitive
        key={`back-${i}`}
        object={scene.clone()}
        position={[FLOOR_X_START + i, 0, FLOOR_Z_START - FLOOR_Z_TILES]}
        rotation={[0, 0, 0]}
      />
    );
  }
  // 왼쪽 벽 (x = FLOOR_X_START) — 90도 회전해 Z축을 따라
  for (let j = 0; j < FLOOR_Z_TILES; j++) {
    walls.push(
      <primitive
        key={`left-${j}`}
        object={scene.clone()}
        position={[FLOOR_X_START, 0, FLOOR_Z_START - j]}
        rotation={[0, Math.PI / 2, 0]}
      />
    );
  }
  // 오른쪽 벽 (x = FLOOR_X_START + FLOOR_X_TILES).
  // 안쪽 면이 어두운 색이라 -90도(왼쪽 벽의 반대 방향)로 뒤집는다.
  // rotation.y=-Math.PI/2 일 때 로컬(x,z)의 실측 오프셋은 Δx∈[0,0.05], Δz∈[0,1] 이므로
  // (왼쪽 벽의 +Math.PI/2 offset Δz∈[-1,0] 과 부호가 반대) position.z 를 -j-1 로 보정한다.
  // j=DOORWAY_RIGHT_WALL_J 자리는 출입구(wallDoorway)로 교체한다.
  for (let j = 0; j < FLOOR_Z_TILES; j++) {
    const isDoorway = j === DOORWAY_RIGHT_WALL_J;
    walls.push(
      <primitive
        key={`right-${j}`}
        object={(isDoorway ? doorwayScene : scene).clone()}
        position={[FLOOR_X_START + FLOOR_X_TILES, 0, FLOOR_Z_START - j - 1]}
        rotation={[0, -Math.PI / 2, 0]}
      />
    );
  }

  // 왼쪽 스트립(x[-1,2])을 z=DIVIDER_Z 에서 갈라 라운지 룸(z<DIVIDER_Z)과
  // 키친/로비존(z>DIVIDER_Z)으로 나눈다. 앞 벽과 같은 방식으로 180도 뒤집어
  // 나무색 면이 라운지 룸(-z 방향)을 향하게 하고, 중간 칸은 출입구로 둔다.
  for (let i = 0; i < DIVIDER_X_TILES; i++) {
    const isDoorway = i === DIVIDER_DOORWAY_I;
    walls.push(
      <primitive
        key={`divider-${i}`}
        object={(isDoorway ? doorwayScene : scene).clone()}
        position={[DIVIDER_X_START + i + 1, 0, DIVIDER_Z]}
        rotation={[0, Math.PI, 0]}
      />
    );
  }

  // 가로 divider(z=DIVIDER_Z)를 오른쪽 벽(x=6)까지 이어서 방 전체를 앞/뒤로 나눈다.
  // 앞쪽(z>DIVIDER_Z) = 키친/로비존 + 책상존, 뒤쪽(z<DIVIDER_Z) = 빈 뒷방.
  // 기존 divider와 같은 방향(180도, 나무색이 -z/뒷방을 향함)으로 통일한다.
  for (let i = 0; i < DIVIDER_EXT_X_TILES; i++) {
    const isDoorway = i === DIVIDER_EXT_DOORWAY_I;
    walls.push(
      <primitive
        key={`divider-ext-${i}`}
        object={(isDoorway ? doorwayScene : scene).clone()}
        position={[DIVIDER_EXT_X_START + i + 1, 0, DIVIDER_Z]}
        rotation={[0, Math.PI, 0]}
      />
    );
  }

  // 키친/로비존(x[-1,2] z[DIVIDER_Z,1])을 오른쪽까지 완전히 감싸는 세로 벽.
  // x=2(가로 divider 오른쪽 끝, 책상 블록 왼쪽 경계와 동일)에서 앞 벽(z=1)까지 이어져
  // 왼쪽 스트립 전체를 하나의 방으로 만든다. 오른쪽 벽과 같은 방식(-90도)으로 뒤집어
  // 나무색 면이 키친/로비존(-x 방향)을 향하게 한다.
  // 완전히 막으면 메인 출입구(오른쪽 벽)에서 못 들어가는 고립된 방이 되므로
  // 로비와 가장 가까운 칸(j=0, z[0,1])을 출입구로 둔다.
  for (let j = 0; j < MIDDLE_DIVIDER_Z_TILES; j++) {
    const isDoorway = j === MIDDLE_DIVIDER_DOORWAY_J;
    walls.push(
      <primitive
        key={`middle-divider-${j}`}
        object={(isDoorway ? doorwayScene : scene).clone()}
        position={[MIDDLE_DIVIDER_X, 0, MIDDLE_DIVIDER_Z_START - j - 1]}
        rotation={[0, -Math.PI / 2, 0]}
      />
    );
  }

  return <>{walls}</>;
}

// 책상 블록을 오른쪽·앞쪽으로 이동 — 파티션 없는 오른쪽 끝(x=4.9)을 오른쪽 벽(x=6)에
// 붙여서 벽이 파티션 역할을 하게 한다. DESK_BLOCK_X_SHIFT = 6 - 4.9 = 1.1.
// 앞쪽으로도 이동하되 통로 확보를 위해 완전히 벽에 붙이지는 않는다.
const DESK_BLOCK_X_SHIFT = 5.1;
const DESK_BLOCK_Z_SHIFT = 1.4;

// 기존 "가로 2줄x4개" 모양은 그대로 유지한 채, 블록 전체를 그룹으로 감싸
// 통째로 90도 회전시킨다. 개별 책상 위치는 건드리지 않는다.
// 반대 방향(-90도)으로 돌리고, 회전 후 블록이 방 안(x[2,6] z[-7,1]) 뒤쪽
// 빈 공간에 오도록 그룹에 위치 보정을 다시 계산해 더한다.
// 오른쪽 벽(x=6)까지 0.77m 여유가 있었으므로 그만큼 더 밀어 벽에 붙인다.
const DESK_GROUP_ROTATE_X_OFFSET = 2 + 0.77;
const DESK_GROUP_ROTATE_Z_OFFSET = -9;

function DeskGrid() {
  const desks = [];
  for (let row = 0; row < GRID_ROWS; row++) {
    for (let col = 0; col < GRID_COLS; col++) {
      desks.push(
        <OfficeDesk
          key={`${row}-${col}`}
          position={[
            col * COL_SPACING + DESK_BLOCK_X_SHIFT,
            0,
            -row * ROW_SPACING + DESK_BLOCK_Z_SHIFT,
          ]}
        />
      );
    }
  }
  return (
    <group
      position={[DESK_GROUP_ROTATE_X_OFFSET, 0, DESK_GROUP_ROTATE_Z_OFFSET]}
      rotation={[0, -Math.PI / 2, 0]}
    >
      {desks}
    </group>
  );
}

// 라운지 존 — 책상 블록 뒤쪽 공터(x[2,6] z[-7,-3.23], 약 4m x 3.8m)에 배치.
// 실측: loungeSofaCorner 0.98x0.98(L자, 모서리 원점), tableCoffee 0.66x0.4(중심 근처 원점),
// rugRectangle 1.57x0.92(모서리 원점).
function LoungeArea() {
  const { scene: rug } = useGLTF(`${MODEL_BASE}/rugRectangle.glb`);
  const { scene: sofaCorner } = useGLTF(`${MODEL_BASE}/loungeSofaCorner.glb`);
  const { scene: table } = useGLTF(`${MODEL_BASE}/tableCoffee.glb`);
  const { scene: plant } = useGLTF(`${MODEL_BASE}/pottedPlant.glb`);

  return (
    <group>
      {/*
       * 세트 전체를 왼쪽 위 코너로 이동 (2026-07-02).
       * 카메라 탑뷰 스크린샷 + 그리드선 보정으로 목표 지점을 world(x≈0, z≈-5.6) 로 추정,
       * 기존 소파 기준점(2.3,-5.9) 대비 델타(Δx=-2.3, Δz=+0.3)를 4개 오브젝트에 동일 적용해
       * 상대 배치(모양)는 그대로 유지한다.
       */}
      <primitive object={rug.clone()} position={[0, 0.01, -5.6]} />
      <primitive object={sofaCorner.clone()} position={[0, 0.05, -5.6]} />
      <primitive object={table.clone()} position={[0, 0.05, -5.5]} />
      <primitive object={plant.clone()} position={[-0.4, 0.05, -6.4]} />
    </group>
  );
}

// 탕비실 — 키친/로비존(x[-1,2] z[-4,1], 3m x 5m)을 채운다.
// 도어 2곳(라운지 쪽 x[0,1] z=-4, 책상존 쪽 x=2 z[0,1])을 막지 않도록
// 서쪽 벽(x=-1)에 조리대를 붙이고 중앙에 테이블+의자를 둔다.
// 자판기/복사기는 Kenney Furniture Kit에 없어 커피머신+냉장고로 대체한다.
function BreakRoomArea() {
  const { scene: fridge } = useGLTF(`${MODEL_BASE}/kitchenFridgeSmall.glb`);
  const { scene: sink } = useGLTF(`${MODEL_BASE}/kitchenSink.glb`);
  const { scene: cabinet } = useGLTF(`${MODEL_BASE}/kitchenCabinet.glb`);
  const { scene: coffeeMachine } = useGLTF(`${MODEL_BASE}/kitchenCoffeeMachine.glb`);
  const { scene: table } = useGLTF(`${MODEL_BASE}/table.glb`);
  const { scene: chair } = useGLTF(`${MODEL_BASE}/chairCushion.glb`);
  const { scene: trash } = useGLTF(`${MODEL_BASE}/trashcan.glb`);
  const { scene: plant } = useGLTF(`${MODEL_BASE}/plantSmall1.glb`);

  return (
    <group>
      {/*
       * 실측 기반 재배치 (2026-07-02). preview_eval로 각 모델의 world bbox를 재보니
       * pivot이 모델마다 다르고(냉장고/싱크대/조리대/커피머신은 "앞쪽 모서리",
       * 쓰레기통/화분은 "중심") 폭도 제각각(싱크대·조리대 0.45m, 커피머신 0.19m)이라
       * 균등 1m 간격으로는 벽을 뚫거나 간격이 벌어졌다. 아래 좌표는 그 실측값으로
       * 서로 맞닿게 다시 계산한 값이다.
       */}
      {/* 서쪽 벽(x=-1, 내벽면 x=-1.0)을 따라 조리대 — 냉장고는 벽에 맞닿게 뒤로 밀고,
          싱크대·조리대는 앞면(x=-0.53)을 맞춰 나란히 배치. 커피머신은 싱크대 위로 올림 */}
      <primitive name="br-fridge" object={fridge.clone()} position={[-0.72, 0.05, -3.3]} rotation={[0, Math.PI / 2, 0]} />
      <primitive name="br-sink" object={sink.clone()} position={[-0.53, 0.05, -2.87]} rotation={[0, Math.PI / 2, 0]} />
      <primitive name="br-cabinet" object={cabinet.clone()} position={[-0.53, 0.05, -2.44]} rotation={[0, Math.PI / 2, 0]} />
      {/* 커피머신 — 조리대(cabinet) 상판 위, 사용자가 수동으로 미세조정한 위치 */}
      <primitive name="br-coffee" object={coffeeMachine.clone()} position={[-0.59, 0.47, -2.49]} rotation={[0, Math.PI / 2, 0]} />

      {/* 중앙 테이블 + 의자 2개 — 의자를 테이블 남/북 변에 맞닿게 배치 */}
      <primitive name="br-table" object={table.clone()} position={[-0.07, 0.05, -0.78]} rotation={[0, 0, 0]} />
      <primitive name="br-chair1" object={chair.clone()} position={[0.25, 0.05, -1.227]} rotation={[0, 0, 0]} />
      <primitive name="br-chair2" object={chair.clone()} position={[0.45, 0.05, -0.78]} rotation={[0, Math.PI, 0]} />

      {/* 화분 — 테이블 위(상판 실측 y=0.377) 모서리에 배치 */}
      <primitive name="br-plant" object={plant.clone()} position={[0.6, 0.377, -1.15]} rotation={[0, 0, 0]} />

      {/* 쓰레기통 — 실측 결과 페달이 로컬 +z(회전 전 기준)로 튀어나와 있음.
          책상존 도어(x=2, z[0,1])가 더 가까운 문이라 그쪽(+x)을 향하도록
          rotation.y=+90도 적용(로컬 +z → 월드 +x) */}
      <primitive name="br-trash" object={trash.clone()} position={[-0.7, 0.05, 0.25]} rotation={[0, Math.PI / 2, 0]} />
    </group>
  );
}

export default function HubPage() {
  const camRef = useRef<HTMLSpanElement>(null);
  const sceneRef = useRef<THREE.Scene | null>(null);

  return (
    <div className="relative h-screen w-full">
      {/* ⚠️ DEV DEBUG 카메라 좌표 — 제거 예정 */}
      <div className="absolute left-3 top-3 z-10 rounded-lg bg-foreground/80 p-3 font-mono text-xs text-white">
        <div className="mb-1 font-bold text-accent-foreground">🛠 SCENE DEBUG</div>
        <div>camera: <span ref={camRef}>-</span></div>
      </div>

      <Canvas camera={{ position: [4, 8, 8], fov: 50 }}>
        <ambientLight intensity={0.7} />
        <directionalLight position={[5, 8, 3]} intensity={1} />
        <Suspense fallback={null}>
          <Floor />
          <RoomWalls />
          <DeskGrid />
          <LoungeArea />
          <BreakRoomArea />
        </Suspense>

        {/* ⚠️ DEV DEBUG — 제거 예정 */}
        <DebugHelpers />
        <CameraTracker targetRef={camRef} />
        <SceneCapture sceneRef={sceneRef} />

        <OrbitControls target={[2, 0, -2.5]} />
      </Canvas>

      {/* ⚠️ DEV DEBUG 색 실험 패널 — 제거 예정 */}
      <MaterialColorPanel sceneRef={sceneRef} />
    </div>
  );
}
