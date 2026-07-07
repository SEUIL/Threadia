/*
 * DM 방 목업 — dm/layout.tsx(아이콘 레일)와 dm/[roomId]/page.tsx가 공유한다.
 *
 * ⚠️ MOCK: DM 방 목록 API(`GET /companies/{companyId}/direct-messages/rooms`)가
 * 없어서 S14(구성원 관리)와 동일한 목업 구성원 목록으로 방을 흉내낸다.
 * 본인(정철진)은 DM 대상에서 제외했다.
 */
export type DmRoom = { roomId: string; name: string };

export const MOCK_ROOMS: DmRoom[] = [
  { roomId: "u2", name: "김민정" },
  { roomId: "u3", name: "여도현" },
  { roomId: "u4", name: "황스일" },
];
