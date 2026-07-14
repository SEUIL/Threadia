"use client";

/*
 * DebugDirections — components/hub/DebugDirections.tsx
 *
 * ⚠️ 개발용 임시 컴포넌트. 3D 배치 작업 시 방향/축을 눈으로 확인하려고 세우는
 * 방향지시물이다. 오피스 중심(2.5, -3)을 기준으로 중앙 + 동서남북에 색이 다른
 * 기둥과 라벨(방향명 + 월드 축 + 좌표)을 세운다. 작업이 끝나면 page.tsx에서
 * <DebugDirections />를 지우거나 SHOW 플래그를 끄면 된다.
 *
 * 이 씬의 방향-축 대응(이 컴포넌트를 세워 실측 확인하려는 대상):
 *   동(E) = +X / 서(W) = -X / 북(N) = -Z / 남(S) = +Z
 */
import { Html } from "@react-three/drei";

const OFFICE_CENTER_X = 2.5;
const OFFICE_CENTER_Z = -3;
const ARM = 6; // 중앙에서 각 방향 마커까지 거리
const POLE_HEIGHT = 6;

type Marker = {
  key: string;
  label: string;
  color: string;
  x: number;
  z: number;
};

const MARKERS: Marker[] = [
  { key: "C", label: "중앙 C (0,0)", color: "#ffffff", x: OFFICE_CENTER_X, z: OFFICE_CENTER_Z },
  { key: "N", label: "북 N (-Z)", color: "#ef4444", x: OFFICE_CENTER_X, z: OFFICE_CENTER_Z - ARM },
  { key: "S", label: "남 S (+Z)", color: "#3b82f6", x: OFFICE_CENTER_X, z: OFFICE_CENTER_Z + ARM },
  { key: "E", label: "동 E (+X)", color: "#22c55e", x: OFFICE_CENTER_X + ARM, z: OFFICE_CENTER_Z },
  { key: "W", label: "서 W (-X)", color: "#eab308", x: OFFICE_CENTER_X - ARM, z: OFFICE_CENTER_Z },
];

export function DebugDirections() {
  return (
    <group>
      {MARKERS.map((m) => (
        <group key={m.key} position={[m.x, 0, m.z]}>
          {/* 기둥 */}
          <mesh position={[0, POLE_HEIGHT / 2, 0]}>
            <boxGeometry args={[0.15, POLE_HEIGHT, 0.15]} />
            <meshStandardMaterial color={m.color} />
          </mesh>
          {/* 꼭대기 구슬 */}
          <mesh position={[0, POLE_HEIGHT, 0]}>
            <sphereGeometry args={[0.35, 16, 16]} />
            <meshStandardMaterial color={m.color} />
          </mesh>
          {/* 라벨 — DOM 오버레이라 한글 렌더 OK, 항상 카메라를 향한다 */}
          <Html position={[0, POLE_HEIGHT + 0.7, 0]} center distanceFactor={20}>
            <div
              style={{
                background: m.color,
                color: "#0e1633",
                fontWeight: 700,
                fontSize: "12px",
                padding: "2px 6px",
                borderRadius: "4px",
                whiteSpace: "nowrap",
                border: "1px solid rgba(14,22,51,0.3)",
              }}
            >
              {`${m.label}  x=${m.x} z=${m.z}`}
            </div>
          </Html>
        </group>
      ))}
    </group>
  );
}
