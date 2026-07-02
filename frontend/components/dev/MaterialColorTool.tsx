"use client";

/*
 * MaterialColorTool — components/dev/MaterialColorTool.tsx
 *
 * ⚠️ 개발 전용(DEV ONLY). 모델 재질 색을 실시간으로 바꿔보며 실험하는 도구다.
 *    색 확정 후 이 파일과 사용처를 제거하고, 확정 색은 로드 시 코드로 덮어쓴다.
 *
 * 동작:
 *   - SceneCapture: Canvas 안에 두어 실제 THREE.Scene 을 ref 로 잡아 패널에 넘긴다.
 *   - MaterialColorPanel: Canvas 밖 HTML. "재질 스캔"으로 씬의 모든 재질을 이름별로
 *     모아 컬러 피커를 만들고, 색을 바꾸면 같은 이름의 재질 전체에 즉시 적용한다.
 *   - 하단 JSON 박스에 현재 이름→색 맵을 출력 → 확정 값을 그대로 복사해 코드에 반영.
 *
 * 주의:
 *   - useGLTF 캐시 + scene.clone() 은 재질을 공유하므로, 이름 기준 색 변경이
 *     같은 재질을 쓰는 모든 인스턴스에 일괄 적용된다(의도된 동작).
 */
import { RefObject, useEffect, useState } from "react";
import { useThree } from "@react-three/fiber";
import * as THREE from "three";

export function SceneCapture({ sceneRef }: { sceneRef: RefObject<THREE.Scene | null> }) {
  const scene = useThree((s) => s.scene);
  useEffect(() => {
    sceneRef.current = scene;
    // DEV: eval 로 월드 bbox 측정할 수 있게 잠깐 노출 (배치 확정 후 제거)
    (window as unknown as { __scene?: THREE.Scene }).__scene = scene;
  }, [scene, sceneRef]);
  return null;
}

type MatEntry = { name: string; color: string };

function collect(scene: THREE.Scene): MatEntry[] {
  const map = new Map<string, string>();
  scene.traverse((o) => {
    const mesh = o as THREE.Mesh;
    if (!mesh.isMesh) return;
    const mats = Array.isArray(mesh.material) ? mesh.material : [mesh.material];
    mats.forEach((m) => {
      const mm = m as THREE.MeshStandardMaterial;
      if (mm && mm.color) {
        const key = mm.name || "(unnamed)";
        if (!map.has(key)) map.set(key, "#" + mm.color.getHexString());
      }
    });
  });
  return [...map].map(([name, color]) => ({ name, color }));
}

function apply(scene: THREE.Scene, name: string, hex: string) {
  scene.traverse((o) => {
    const mesh = o as THREE.Mesh;
    if (!mesh.isMesh) return;
    const mats = Array.isArray(mesh.material) ? mesh.material : [mesh.material];
    mats.forEach((m) => {
      const mm = m as THREE.MeshStandardMaterial;
      if (mm && mm.color && (mm.name || "(unnamed)") === name) mm.color.set(hex);
    });
  });
}

export function MaterialColorPanel({ sceneRef }: { sceneRef: RefObject<THREE.Scene | null> }) {
  const [mats, setMats] = useState<MatEntry[]>([]);

  const scan = () => {
    if (sceneRef.current) setMats(collect(sceneRef.current));
  };

  // 마운트 직후 한 번 자동 스캔 시도 (모델 로드 타이밍에 따라 버튼으로 재시도)
  useEffect(() => {
    const t = setTimeout(scan, 1500);
    return () => clearTimeout(t);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const onChange = (name: string, hex: string) => {
    if (sceneRef.current) apply(sceneRef.current, name, hex);
    setMats((prev) => prev.map((x) => (x.name === name ? { ...x, color: hex } : x)));
  };

  const json = JSON.stringify(
    Object.fromEntries(mats.map((m) => [m.name, m.color])),
    null,
    2
  );

  return (
    <div className="absolute right-3 top-3 z-10 max-h-[90vh] w-64 overflow-auto rounded-lg bg-foreground/80 p-3 font-mono text-xs text-white">
      <div className="mb-2 flex items-center justify-between">
        <span className="font-bold text-accent-foreground">🎨 재질 색</span>
        <button
          onClick={scan}
          className="rounded bg-primary px-2 py-0.5 text-primary-foreground"
        >
          재질 스캔
        </button>
      </div>

      {mats.length === 0 && <div className="opacity-70">스캔 대기…</div>}

      <ul className="flex flex-col gap-1.5">
        {mats.map((m) => (
          <li key={m.name} className="flex items-center justify-between gap-2">
            <span className="truncate">{m.name}</span>
            <span className="flex items-center gap-1">
              <span className="opacity-70">{m.color}</span>
              <input
                type="color"
                value={m.color}
                onChange={(e) => onChange(m.name, e.target.value)}
                className="h-5 w-6 cursor-pointer border-0 bg-transparent p-0"
              />
            </span>
          </li>
        ))}
      </ul>

      {mats.length > 0 && (
        <>
          <div className="mt-2 opacity-70">확정 값(복사용)</div>
          <textarea
            readOnly
            value={json}
            className="mt-1 h-32 w-full resize-none rounded bg-black/40 p-1 text-[10px]"
          />
        </>
      )}
    </div>
  );
}
