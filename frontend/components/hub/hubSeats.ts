/*
 * 허브 오피스 8석 좌표 — OfficeFloor.tsx의 DeskGrid(4x2, -90도 회전 그룹)를
 * 손계산 대신 실측(getWorldPosition)으로 확정한 값이다(2026-07-21).
 * 손계산보다 실측이 정확하다는 이 프로젝트의 3D 작업 확립 원칙을 따른다
 * (docs/08-members/member-정철진/기술-연구/3d-오피스-렌더링-연구.md).
 *
 * OfficeFloor가 기본 transform(position=[0,0,0], rotation=[0,0,0])으로 딱
 * 하나만 배치된 현재 상태를 전제로 한 world 좌표다 — 나중에 여러 층/여러
 * 사무실을 쌓는 구조로 바뀌면(hub/page.tsx 주석 참고) 이 값도 다시 실측
 * 필요.
 */
export type Vec3 = [number, number, number];

export const HUB_SEAT_POSITIONS: Vec3[] = [
  [3.05, 0.05, -2.2],
  [3.05, 0.05, -1.2],
  [3.05, 0.05, -0.2],
  [3.05, 0.05, 0.8],
  [5.25, 0.05, -2.2],
  [5.25, 0.05, -1.2],
  [5.25, 0.05, -0.2],
  [5.25, 0.05, 0.8],
];
