"use client";

/*
 * HubAvatar — components/hub/HubAvatar.tsx
 *
 * 허브 온라인 상태 표시(WBS 밖 개인 R&D — 실시간 채팅+DB 저장은 프로덕션
 * 호스팅 이후 스키마 마이그레이션 비용이 커서 폐기했고, 이 컴포넌트는 그
 * 작업에서 DB 의존 없는 부분만 가져와 다시 만든 것이다) 후속으로 만드는
 * 아주 단순한 캐릭터다. GLTF 모델을 로드하지 않고 Three.js 기본
 * geometry(sphere/cone)만 쓴다 — 에셋 로딩이 없고 폴리곤 수도 적어서, 회사
 * 인원이 늘어도(팀 규모 기준) 성능 부담이 거의 없다. geometry는 React가
 * 각 인스턴스마다 새로 만들지만 원시 도형이라 비용이 무시할 만한 수준이다
 * (GLTF 기반 오피스 가구와 다른 점).
 *
 * 몸통 하나(타원체) + 역할별 모자 + 눈/입 + 상시 닉네임 태그만 그린다:
 *   - COMPANY_ADMIN: 금색 고깔(실루엣만으로 구분되는 용도, 왕관 정교화는 범위 밖)
 *   - MEMBER: 회색 반구 캡
 *
 * 정면(얼굴) 방향 — 의자→모니터 world 벡터 실측값(chairDesk/computerScreen
 * world position, 2026-07-21: 의자 (3.05,-2.2) → 모니터 (3.42,-2.6), 델타
 * (0.37,-0.40))을 처음 시도로 넣었더니 브라우저에서 육안으로 봤을 때 실제
 * 모니터 방향과 안 맞아서(2026-07-21, 사용자 육안 확인) FACE_ROTATION_Y의
 * z 성분을 -0.01로 다시 조정했다 — 이 값은 계산값이 아니라 눈으로 보고
 * 맞춘 값이다. 책상 블록 8석이 전부 같은 강체(rigid) 배치라 좌석마다
 * 방향이 동일하므로 이 각도 하나로 모든 좌석에 그대로 쓴다 — 좌석 배치가
 * 회전하는 방(다른 층 등)이 추가되면 그때 다시 확인 필요.
 *
 * 닉네임 태그: 원래 브랜치는 채팅 말풍선(HubSpeechBubble) 안에만 닉네임을
 * 넣어서 메시지가 없으면 닉네임 자체가 안 보였다. 채팅을 폐기하면서
 * "누가 온라인인지 닉네임으로 항상 보여야 한다"는 요구가 새로 생겨서,
 * 말풍선과 같은 drei `<Html center>` 기법(카메라를 향해 항상 정면으로
 * 보이고, 어느 각도에서 봐도 글자가 안 뒤집힌다)으로 상시 라벨을 새로 만들었다.
 */
import * as THREE from "three";
import { Html } from "@react-three/drei";

export type HubAvatarRole = "COMPANY_ADMIN" | "MEMBER";

// 사용자마다 몸통 색을 다르게 해서 여러 명이 나란히 있어도 구분되게 한다.
// userId 기반 결정론적 선택이라 매 렌더마다 같은 사람은 항상 같은 색이다.
const BODY_COLORS = ["#6c8dfa", "#f2a35c", "#5cc6a0", "#e07bb0", "#8b7cf0", "#f2c94c", "#5cb8e0", "#f27c7c"];

function colorForUserId(userId: number) {
  return BODY_COLORS[Math.abs(userId) % BODY_COLORS.length];
}

// 의자 좌판 높이 실측(2026-07-21, chairDesk.glb 다리 상단 world y=0.24, 방석
// 두께 추정 포함) 기준으로 몸통 크기를 다시 잡았다 — 이전 버전(반지름 0.22)은
// 의자 폭(실측 0.31m)보다 아바타가 더 넓어서 의자 위에 앉은 게 아니라 책상까지
// 뒤덮는 것처럼 보였다. 아래 상수들은 전부 이 몸통 크기 기준으로 비례 계산한
// 값이라, 몸통을 다시 조정하면 모자/눈/입도 같이 옮겨야 한다.
const BODY_RADIUS = 0.13;
const BODY_SCALE: [number, number, number] = [1, 1.4, 0.9];
const BODY_HALF_HEIGHT = BODY_RADIUS * BODY_SCALE[1]; // 0.182
const BODY_CENTER_Y = 0.27 + BODY_HALF_HEIGHT; // 좌판 높이(0.27) 위에 앉힌다
const BODY_TOP_Y = BODY_CENTER_Y + BODY_HALF_HEIGHT;
const FACE_Z = BODY_RADIUS * BODY_SCALE[2]; // 몸통 앞면(+Z)

// 실측된 의자→모니터 world 방향 벡터(dx=0.37, dz=-0.40)를 향해 로컬 +Z(얼굴)가
// 향하도록 돌리는 각도.
const FACE_ROTATION_Y = Math.atan2(0.37, -0.01);

// 닉네임 태그 높이 — 모자(관리자 고깔/일반 캡) 중 가장 높은 지점(약 0.77)보다
// 여유 있게 위에 띄운다. x/z는 0으로 둬서(Y축 위) FACE_ROTATION_Y가 걸려도
// 위치가 안 틀어진다(Y축 회전은 Y축 위의 점을 그대로 둔다).
const TAG_Y = 0.95;

function Hat({ role }: { role: HubAvatarRole }) {
  if (role === "COMPANY_ADMIN") {
    return (
      <mesh position={[0, BODY_TOP_Y + 0.06, 0]}>
        <coneGeometry args={[0.09, 0.16, 16]} />
        <meshStandardMaterial color="#f4c430" />
      </mesh>
    );
  }
  return (
    <mesh position={[0, BODY_TOP_Y - 0.02, 0]}>
      <sphereGeometry args={[0.11, 16, 12, 0, Math.PI * 2, 0, Math.PI / 2]} />
      <meshStandardMaterial color="#4b5563" />
    </mesh>
  );
}

export function HubAvatar({
  position,
  role,
  userId,
  nickname,
}: {
  position: [number, number, number];
  role: HubAvatarRole;
  userId: number;
  nickname: string;
}) {
  const bodyColor = colorForUserId(userId);

  return (
    <group position={position} rotation={[0, FACE_ROTATION_Y, 0]}>
      {/* 몸통 — 구를 세로로 늘이고 앞뒤로 살짝 눌러 타원체로 만든다 */}
      <mesh position={[0, BODY_CENTER_Y, 0]} scale={BODY_SCALE}>
        <sphereGeometry args={[BODY_RADIUS, 20, 16]} />
        <meshStandardMaterial color={bodyColor} />
      </mesh>

      <Hat role={role} />

      {/* 눈 */}
      <mesh position={[-0.045, BODY_CENTER_Y + BODY_HALF_HEIGHT * 0.35, FACE_Z]}>
        <circleGeometry args={[0.02, 12]} />
        <meshBasicMaterial color="#1f2430" side={THREE.DoubleSide} />
      </mesh>
      <mesh position={[0.045, BODY_CENTER_Y + BODY_HALF_HEIGHT * 0.35, FACE_Z]}>
        <circleGeometry args={[0.02, 12]} />
        <meshBasicMaterial color="#1f2430" side={THREE.DoubleSide} />
      </mesh>

      {/* 입 */}
      <mesh position={[0, BODY_CENTER_Y - BODY_HALF_HEIGHT * 0.05, FACE_Z]}>
        <planeGeometry args={[0.055, 0.015]} />
        <meshBasicMaterial color="#1f2430" side={THREE.DoubleSide} />
      </mesh>

      {/* 닉네임 태그 — HubSpeechBubble과 같은 center + pointerEvents="none" 패턴.
          transform을 안 켜서 3D 회전을 안 따라가고 항상 카메라를 정면으로 본다. */}
      <Html position={[0, TAG_Y, 0]} center pointerEvents="none" zIndexRange={[10, 0]}>
        <span
          className="whitespace-nowrap rounded-full border-l-4 bg-white px-2 py-0.5 text-[10px] font-bold shadow-md"
          style={{ borderLeftColor: bodyColor, color: bodyColor }}
        >
          {nickname}
        </span>
      </Html>
    </group>
  );
}
