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

const FLOOR_X_START = -1;
const FLOOR_X_TILES = 6;
const FLOOR_Z_TILES = 7; // z: 0 ~ -7

function Floor() {
  const { scene } = useGLTF(`${MODEL_BASE}/floorFull.glb`);
  const tiles = [];
  for (let i = 0; i < FLOOR_X_TILES; i++) {
    for (let j = 0; j < FLOOR_Z_TILES; j++) {
      tiles.push(
        <primitive
          key={`${i}-${j}`}
          object={scene.clone()}
          position={[FLOOR_X_START + i, 0, -j - 1]}
        />
      );
    }
  }
  return <>{tiles}</>;
}

function DeskGrid() {
  const desks = [];
  for (let row = 0; row < GRID_ROWS; row++) {
    for (let col = 0; col < GRID_COLS; col++) {
      desks.push(
        <OfficeDesk
          key={`${row}-${col}`}
          position={[col * COL_SPACING, 0, -row * ROW_SPACING]}
        />
      );
    }
  }
  return <>{desks}</>;
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
          <DeskGrid />
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
