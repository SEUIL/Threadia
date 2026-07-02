"use client";

/*
 * SceneDebug — components/dev/SceneDebug.tsx
 *
 * ⚠️ 개발 전용(DEV ONLY). 3D 배치 좌표를 잡기 위한 임시 도구다.
 *    배치가 확정되면 이 파일과 사용처를 통째로 제거한다.
 *
 * 제공:
 *   - DebugHelpers : 씬 안에 바닥 격자(Grid) + 원점 축(axesHelper) + 방향 기즈모(GizmoHelper)
 *   - CameraTracker: useFrame으로 카메라 좌표를 외부 DOM(ref)에 실시간 기록 (리렌더 없음)
 *
 * 좌표 색 규약(three.js 표준): X=빨강, Y=초록, Z=파랑
 */
import { RefObject } from "react";
import { useFrame } from "@react-three/fiber";
import { Grid, GizmoHelper, GizmoViewport } from "@react-three/drei";

export function DebugHelpers() {
  return (
    <>
      {/* 바닥 격자 — cellSize 0.5m, section 1m 단위 */}
      <Grid
        position={[0, 0.06, 0]}
        args={[30, 30]}
        cellSize={0.5}
        cellThickness={0.5}
        cellColor="#9ca3af"
        sectionSize={1}
        sectionThickness={1}
        sectionColor="#7c5cff"
        fadeDistance={40}
        infiniteGrid
      />
      {/* 원점 축 3m */}
      <axesHelper args={[3]} />
      {/* 우하단 방향 기즈모 */}
      <GizmoHelper alignment="bottom-right" margin={[80, 80]}>
        <GizmoViewport
          axisColors={["#ef4444", "#10b981", "#3b82f6"]}
          labelColor="white"
        />
      </GizmoHelper>
    </>
  );
}

export function CameraTracker({ targetRef }: { targetRef: RefObject<HTMLSpanElement | null> }) {
  useFrame(({ camera }) => {
    if (targetRef.current) {
      const p = camera.position;
      targetRef.current.textContent = `${p.x.toFixed(2)}, ${p.y.toFixed(2)}, ${p.z.toFixed(2)}`;
    }
  });
  return null;
}
