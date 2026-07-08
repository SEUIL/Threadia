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
 */
import { Suspense } from "react";
import { Canvas } from "@react-three/fiber";
import { OrbitControls } from "@react-three/drei";
import Link from "next/link";
import { OfficeFloor } from "@/components/hub/OfficeFloor";
import { HubOverlay } from "@/components/hub/HubOverlay";
import { useAuth } from "@/contexts/AuthContext";

// 건물 바깥 허공(Canvas scene background) — 브랜드 teal 대신 검정으로 변경.
const SCENE_BACKGROUND = "#000000";

function NoCompanyNotice() {
  return (
    <div className="flex h-full w-full items-center justify-center bg-chrome px-6">
      <div className="max-w-sm rounded-xl border border-chrome-foreground/10 bg-chrome-hover/50 p-8 text-center">
        <h1 className="text-lg font-bold text-chrome-foreground">아직 소속된 회사가 없습니다</h1>
        <p className="mt-2 text-sm text-chrome-muted">
          회사 관리자에게 초대를 요청하거나, 이미 받은 초대가 있다면 확인해주세요.
        </p>
        <Link
          href="/invitations"
          className="mt-5 inline-block rounded-lg btn-gradient px-4 py-2 text-sm font-semibold text-primary-foreground transition-colors duration-fast"
        >
          받은 초대 확인하기
        </Link>
      </div>
    </div>
  );
}

export default function HubPage() {
  const { companyMemberships } = useAuth();

  if (companyMemberships.length === 0) {
    return (
      <div className="relative h-screen w-full">
        <NoCompanyNotice />
        <HubOverlay />
      </div>
    );
  }

  return (
    <div className="relative h-screen w-full">
      {/*
       * flat: R3F 기본 ACES Filmic 톤매핑을 끈다. 안 끄면 밝은 재질(흰 벽 등)이
       * 중간 회색으로 눌린다(픽셀 실측으로 확인).
       * ambientLight를 3까지 올려서 흰 벽을 살렸더니 명암 대비가 사라져 평면적으로
       * 보인다는 피드백을 받아, 조명을 3개로 나눴다 — 은은한 반구광(하늘/바닥 톤 차이로
       * 미세한 그라데이션) + 주광(기존 방향) + 반대편에서 약하게 채우는 보조광.
       * 이러면 방향광을 못 받는 벽면도 보조광으로 밝기는 확보하면서, 주광 반대쪽
       * 면과의 명암 차이(입체감)는 유지된다.
       */}
      <Canvas flat camera={{ position: [4, 8, 8], fov: 50 }}>
        <color attach="background" args={[SCENE_BACKGROUND]} />
        <hemisphereLight args={["#ffffff", "#c7c4bd", 1.1]} />
        <directionalLight position={[5, 8, 3]} intensity={1.4} />
        <directionalLight position={[-5, 6, -4]} intensity={0.6} />
        <Suspense fallback={null}>
          <OfficeFloor />
        </Suspense>
        <OrbitControls target={[2, 0, -2.5]} />
      </Canvas>
      <HubOverlay />
    </div>
  );
}
