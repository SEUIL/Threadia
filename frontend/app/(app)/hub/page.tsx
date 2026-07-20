"use client";

/*
 * S03 — 메인 허브 (개발 중)
 *
 * 현재: 사무실 한 채(OfficeFloor 컴포넌트 — 책상 8석 격자, 라운지존, 탕비실)를
 *   건물 원점에 1개 배치.
 *   - 방 모듈 고정 수용인원: 8명(4x2). 팀원 수는 좌석 점유 여부만 바꾸고
 *     방 크기·장식 배치는 항상 고정한다.
 *   - 팀원이 8명을 넘으면 같은 방 모듈을 옆으로 증축한다(층을 새로 쌓지 않음).
 * OfficeFloor는 position/rotation prop을 받는 재사용 컴포넌트로 분리해뒀다 —
 *   건물/층 구조(층 = 프로젝트 개수만큼)로 확장할 때 이 컴포넌트를 필요한 만큼
 *   인스턴스화하면 된다. 드로우콜 최적화(InstancedMesh/BatchedMesh)는 아직 안 함 —
 *   층당 가구 개수가 적어 지금 규모에서는 불필요하다는 게 기술연구 문서 결론.
 *   (docs/08-members/member-정철진/기술-연구/3d-오피스-렌더링-연구.md §2-2, §3)
 * 다음: 빈 뒷방 용도 결정, 건물/층 구조 실제 적용, role 분기, 카메라 전환
 *   (Overview↔Floor), 캐릭터/조명 연출.
 *
 * HubOverlay: 3D 씬 위에 얹는 HTML 상단바+좌측메뉴(S04/S09/S11/S13 이동).
 *   오브젝트 클릭으로 이동하는 방식은 다음 범위(아직 미구현).
 *
 * 소속 회사 없음(WBS 3.2.1): companyMemberships가 비어있으면 3D 오피스 대신
 * 안내 화면을 보여준다. 화면정의서 §S03 설계 메모 기준 — 회사 생성 폼은
 * 아직 스코프 미확정(설계문서 §9)이라 이번엔 초대 확인 안내만 제공한다.
 * HubOverlay(레일)는 이 상태에서도 그대로 렌더한다 — "받은 초대"/로그아웃은
 * 계속 필요하다.
 *
 * WebGL 미지원 fallback (WBS 3.2.5 QA 지적, PR #62): WebGL context 생성이
 * 실패하는 환경에서 `<Canvas>`를 그냥 렌더링하면 fallback 없이 화면이
 * 깨진다. 마운트 전 `isWebGLAvailable()`로 먼저 확인하고, 혹시 모를 마운트
 * 후 실패(context 유실 등)는 `HubSceneErrorBoundary`로 한 번 더 막는다.
 */
import { Suspense, useEffect, useState } from "react";
import { Canvas } from "@react-three/fiber";
import { OrbitControls } from "@react-three/drei";
import Link from "next/link";
import { OfficeFloor } from "@/components/hub/OfficeFloor";
import { CityBlocks } from "@/components/hub/CityBlocks";
import {
  HubBuildings,
  SUBURBAN_MODELS,
  TREE_MODELS,
  type Plot,
} from "@/components/hub/HubBuildings";
import { DebugDirections } from "@/components/hub/DebugDirections";
import { HubOverlay } from "@/components/hub/HubOverlay";
import { HubSceneErrorBoundary } from "@/components/hub/HubSceneErrorBoundary";
import { isWebGLAvailable } from "@/components/hub/webglSupport";
import { CreateCompanyForm } from "@/components/company/CreateCompanyForm";
import { useAuth } from "@/contexts/AuthContext";

// 건물 바깥 허공(Canvas scene background) — 은은한 뉴트럴 하늘톤. 아래 fog와 반드시
// 같은 값을 써야 격자 도심(CityBlocks) 먼 쪽이 이 색으로 이음매 없이 스며든다.
const SCENE_BACKGROUND = "#c9d2d6";
// fog 근/원 거리 — 이 밖은 SCENE_BACKGROUND로 완전히 덮인다(격자 "끝"이 안 보이게).
const FOG_NEAR = 25;
const FOG_FAR = 70;
// 개발용 방향지시물(중앙+동서남북). 3D 배치 작업할 때 방향/좌표 기준으로 쓰려고
// 남겨둔 도구다 — 평소엔 false로 꺼두고, 씬에 뭔가 배치할 일이 생기면 잠깐 켠다.
const SHOW_DEBUG_DIRECTIONS = false;

// 북쪽 구역(H2 도로 z=-7.5 ~ 북쪽 외곽변 z=-12.5). V 도로(x=6.5)가 가로질러서
// 서/동 두 블록으로 나눈다(도로·오피스와 안 겹치게 여백 둠).
const NORTH_ZONE_PLOTS: Plot[] = [
  { xMin: -6, xMax: 6, zMin: -12, zMax: -8 }, // V 서쪽
  { xMin: 7, xMax: 13, zMin: -12, zMax: -8 }, // V 동쪽
];

// 동쪽 구역(동 마커 자리) — V 도로 동쪽, H2~H1 사이 중앙 밴드만. 북동쪽(z<-8)은
// 북쪽 구역, 남동쪽(z>2)은 아래 남쪽 구역에서 다룬다.
const EAST_ZONE_PLOTS: Plot[] = [
  { xMin: 7, xMax: 13, zMin: -7, zMax: 1 }, // 중앙-동쪽(H2~H1)
];

// 서쪽 구역(서 마커 자리) — 서쪽 외곽변~오피스 서벽, H2~H1 사이 중앙 밴드.
const WEST_ZONE_PLOTS: Plot[] = [
  { xMin: -6, xMax: -1.5, zMin: -7, zMax: 1 }, // 중앙-서쪽(오피스 서쪽)
];

// 남쪽 구역(남 마커 자리) — H1~남쪽 외곽변 사이 전폭. V 도로로 서/동 분할.
const SOUTH_ZONE_PLOTS: Plot[] = [
  { xMin: -6, xMax: 6, zMin: 2, zMax: 5 }, // V 서쪽
  { xMin: 7, xMax: 13, zMin: 2, zMax: 5 }, // V 동쪽
];

function NoCompanyNotice() {
  return (
    <div className="flex h-full w-full items-center justify-center overflow-y-auto bg-chrome px-6 py-10">
      <div className="w-full max-w-sm rounded-xl border border-chrome-foreground/10 bg-chrome-hover/50 p-8 text-center">
        <h1 className="text-lg font-bold text-chrome-foreground">아직 소속된 회사가 없습니다</h1>
        <p className="mt-2 text-sm text-chrome-muted">
          새 회사를 만들거나, 이미 받은 초대가 있다면 확인해주세요.
        </p>

        <CreateCompanyForm />

        <div className="mt-5 border-t border-chrome-foreground/10 pt-5">
          <Link
            href="/invitations"
            className="inline-block rounded-lg btn-gradient px-4 py-2 text-sm font-semibold text-primary-foreground transition-colors duration-fast"
          >
            받은 초대 확인하기
          </Link>
        </div>
      </div>
    </div>
  );
}

function WebGLUnavailableNotice() {
  return (
    <div className="flex h-full w-full items-center justify-center bg-chrome px-6">
      <div className="max-w-sm rounded-xl border border-chrome-foreground/10 bg-chrome-hover/50 p-8 text-center">
        <h1 className="text-lg font-bold text-chrome-foreground">3D 허브를 표시할 수 없습니다</h1>
        <p className="mt-2 text-sm text-chrome-muted">
          이 브라우저 또는 환경에서 WebGL을 사용할 수 없습니다. 다른 브라우저나 기기에서
          다시 시도해주세요. 다른 메뉴는 왼쪽 레일에서 계속 이용할 수 있습니다.
        </p>
      </div>
    </div>
  );
}

export default function HubPage() {
  const { companyMemberships } = useAuth();
  const [webglSupported, setWebglSupported] = useState<boolean | null>(null);

  // 마운트 시 1회 체크. invitations/page.tsx와 동일하게 인라인 IIFE로 감싼다
  // (직접 호출하면 react-hooks/set-state-in-effect에 걸린다).
  useEffect(() => {
    (() => {
      setWebglSupported(isWebGLAvailable());
    })();
  }, []);

  if (companyMemberships.length === 0) {
    return (
      <div className="relative h-screen w-full">
        <NoCompanyNotice />
        <HubOverlay />
      </div>
    );
  }

  if (webglSupported === false) {
    return (
      <div className="relative h-screen w-full">
        <WebGLUnavailableNotice />
        <HubOverlay />
      </div>
    );
  }

  // webglSupported === null인 첫 렌더(마운트 전 체크 중)에는 배경만 깔고
  // Canvas는 아직 시도하지 않는다 — SSR과의 hydration mismatch를 피하기 위함.
  return (
    <div className="relative h-screen w-full" style={{ backgroundColor: SCENE_BACKGROUND }}>
      {webglSupported && (
        <HubSceneErrorBoundary fallback={<WebGLUnavailableNotice />}>
          {/*
           * flat: R3F 기본 ACES Filmic 톤매핑을 끈다. 안 끄면 밝은 재질(흰 벽 등)이
           * 중간 회색으로 눌린다(픽셀 실측으로 확인).
           * ambientLight를 3까지 올려서 흰 벽을 살렸더니 명암 대비가 사라져 평면적으로
           * 보인다는 피드백을 받아, 조명을 3개로 나눴다 — 은은한 반구광(하늘/바닥 톤 차이로
           * 미세한 그라데이션) + 주광(기존 방향) + 반대편에서 약하게 채우는 보조광.
           * 이러면 방향광을 못 받는 벽면도 보조광으로 밝기는 확보하면서, 주광 반대쪽
           * 면과의 명암 차이(입체감)는 유지된다.
           */}
          {/* 카메라 초기 위치는 사무실이 화면에 크게 보이도록 가깝게 두되, target(오피스
              중심)에서 수평거리 9 유닛 안(= CityBlocks 첫 번째 링 건물의 최소 근접
              거리, 12 - 최대 폭/깊이 절반 3) 으로 맞춘다 — 이 반경 안에서는 건물이 아무리
              크게 생성돼도 절대 침범할 수 없어 카메라가 건물 속에 파묻히는 문제가 다시
              생기지 않는다(예전 [4,8,8]은 수평거리 10.7로 이 반경을 살짝 벗어나 있었다). */}
          <Canvas flat camera={{ position: [3.6, 10, 4.3], fov: 50 }}>
            <color attach="background" args={[SCENE_BACKGROUND]} />
            <fog attach="fog" args={[SCENE_BACKGROUND, FOG_NEAR, FOG_FAR]} />
            <hemisphereLight args={["#ffffff", "#c7c4bd", 1.1]} />
            <directionalLight position={[5, 8, 3]} intensity={1.4} />
            <directionalLight position={[-5, 6, -4]} intensity={0.6} />
            <Suspense fallback={null}>
              <OfficeFloor />
              <CityBlocks />
              <HubBuildings plots={NORTH_ZONE_PLOTS} seed={1} />
              <HubBuildings plots={EAST_ZONE_PLOTS} seed={2} />
              <HubBuildings
                plots={WEST_ZONE_PLOTS}
                models={SUBURBAN_MODELS}
                treeModels={TREE_MODELS}
                treeChance={0.35}
                seed={3}
              />
              <HubBuildings
                plots={SOUTH_ZONE_PLOTS}
                models={SUBURBAN_MODELS}
                treeModels={TREE_MODELS}
                treeChance={0.35}
                spacing={1.5}
                seed={4}
              />
              {SHOW_DEBUG_DIRECTIONS && <DebugDirections />}
            </Suspense>
            {/* minDistance/maxDistance/maxPolarAngle: 격자 도심의 "끝"이나 바닥 밑이
                보이지 않게 카메라 범위를 제한한다. minDistance=5는
                maxPolarAngle(1.3rad)에서도 수평거리가 안전 반경(9 유닛, 위 Canvas
                주석 참고) 밑으로 유지되도록 잡은 값이라 줌인해도 건물과 안 부딪힌다.
                기본값은 사무실이 크게 보이게 가깝게 두고, 도심 전경은 사용자가 휠로
                직접 줌아웃해서 보게 한다. */}
            <OrbitControls
              target={[2, 0, -2.5]}
              minDistance={5}
              maxDistance={60}
              maxPolarAngle={1.3}
            />
          </Canvas>
        </HubSceneErrorBoundary>
      )}
      <HubOverlay />
    </div>
  );
}
