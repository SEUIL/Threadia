"use client";

/*
 * S03 — 메인 허브 (개발 중)
 *
 * 현재: 확정된 1인 워크스페이스(OfficeDesk)를 3x3 바닥 위에 배치한 상태.
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
const ROOM_TILES = 3; // 3x3 = 3m x 3m 방

function Floor() {
  const { scene } = useGLTF(`${MODEL_BASE}/floorFull.glb`);
  const tiles = [];
  for (let i = 0; i < ROOM_TILES; i++) {
    for (let j = 0; j < ROOM_TILES; j++) {
      tiles.push(
        <primitive key={`${i}-${j}`} object={scene.clone()} position={[i, 0, -j - 1]} />
      );
    }
  }
  return <>{tiles}</>;
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

      <Canvas camera={{ position: [4, 4, 4], fov: 50 }}>
        <ambientLight intensity={0.7} />
        <directionalLight position={[5, 6, 3]} intensity={1} />
        <Suspense fallback={null}>
          <Floor />
          <OfficeDesk />
        </Suspense>

        {/* ⚠️ DEV DEBUG — 제거 예정 */}
        <DebugHelpers />
        <CameraTracker targetRef={camRef} />
        <SceneCapture sceneRef={sceneRef} />

        <OrbitControls target={[1.5, 0, -1.5]} />
      </Canvas>

      {/* ⚠️ DEV DEBUG 색 실험 패널 — 제거 예정 */}
      <MaterialColorPanel sceneRef={sceneRef} />
    </div>
  );
}
