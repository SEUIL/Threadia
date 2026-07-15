"use client";

/*
 * OfficeDesk — components/hub/OfficeDesk.tsx
 *
 * S03 메인 허브의 "1인 워크스페이스" 세트. 책상·의자·모니터·키보드·마우스·책·
 * ㄴ자 파티션을 하나의 group 으로 묶은 재사용 컴포넌트다.
 * position/rotation prop 으로 층(사무실) 안 어디에나 배치·복제할 수 있다.
 *
 * 좌표/스케일은 배치 실험(hub 페이지 dev 도구)으로 확정한 값이다. 그룹 로컬 기준.
 * 색은 재질 이름 기준으로 override 한다(Kenney 모델은 텍스처 없이 재질 색만 사용).
 *
 * 자산: Kenney Furniture Kit (CC0) — /public/models/kenney-furniture-kit
 */
import { useMemo } from "react";
import { useGLTF } from "@react-three/drei";
import * as THREE from "three";

const MODEL_BASE = "/models/kenney-furniture-kit";

type Vec3 = [number, number, number];

// 재질 이름 → 확정 색 (dev 색 실험으로 결정). 없는 재질은 모델 기본색 유지.
const COLOR_OVERRIDES: Record<string, string> = {
  wood: "#d8b082",
};

// 세트 구성 — 그룹 로컬 좌표. 배치 실험으로 확정한 값.
const PARTS: { file: string; position: Vec3; rotation: Vec3; scale?: Vec3 }[] = [
  { file: "desk.glb", position: [1.1, 0.05, -1.9], rotation: [0, 0, 0] },
  { file: "chairDesk.glb", position: [1.7, 0.05, -1.68], rotation: [0, Math.PI, 0] },
  { file: "computerScreen.glb", position: [1.3, 0.43, -2.05], rotation: [0, 0, 0] },
  { file: "computerKeyboard.glb", position: [1.33, 0.43, -1.98], rotation: [0, 0, 0] },
  { file: "computerMouse.glb", position: [1.68, 0.43, -1.96], rotation: [0, 0, 0] },
  { file: "books.glb", position: [1.13, 0.43, -2.0], rotation: [0, 0, 0] },
  { file: "wall.glb", position: [0.9, 0.05, -2.3], rotation: [0, 0, 0], scale: [1, 0.75, 0.6] }, // 뒤 파티션
  { file: "wall.glb", position: [0.9, 0.05, -2.3], rotation: [0, -Math.PI / 2, 0], scale: [1, 0.75, 0.6] }, // 왼쪽 파티션
];

function applyColors(material: THREE.Material): THREE.Material {
  const std = material as THREE.MeshStandardMaterial;
  const hex = std.name ? COLOR_OVERRIDES[std.name] : undefined;
  if (hex && std.color) {
    const cloned = std.clone();
    cloned.color.set(hex);
    return cloned;
  }
  return material;
}

function Part({ file, position, rotation, scale }: (typeof PARTS)[number]) {
  const { scene } = useGLTF(`${MODEL_BASE}/${file}`);
  const obj = useMemo(() => {
    const clone = scene.clone(true);
    clone.traverse((o) => {
      const mesh = o as THREE.Mesh;
      if (!mesh.isMesh) return;
      mesh.material = Array.isArray(mesh.material)
        ? mesh.material.map(applyColors)
        : applyColors(mesh.material);
    });
    return clone;
  }, [scene]);
  return (
    <primitive object={obj} position={position} rotation={rotation} scale={scale ?? [1, 1, 1]} />
  );
}

export function OfficeDesk({
  position = [0, 0, 0],
  rotation = [0, 0, 0],
}: {
  position?: Vec3;
  rotation?: Vec3;
}) {
  return (
    <group position={position} rotation={rotation}>
      {PARTS.map((p, i) => (
        <Part key={i} {...p} />
      ))}
    </group>
  );
}

// 세트에서 쓰는 모델 미리 로드
PARTS.forEach((p) => useGLTF.preload(`${MODEL_BASE}/${p.file}`));
