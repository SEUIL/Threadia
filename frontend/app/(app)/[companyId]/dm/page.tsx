"use client";

/*
 * S09 — 채팅·DM 인덱스
 *
 * 예전엔 여기가 직사각형 방 목록 화면이었다. 이제 좌측 아이콘 레일(dm/layout.tsx)이
 * 그 역할을 대신하므로, `/dm`으로 바로 들어오면 첫 번째 대화방으로 즉시
 * 리다이렉트해서 "목록 화면 → 방 선택 → 대화 화면" 2단계를 거치지 않게 한다.
 */
import { useEffect } from "react";
import { useRouter, useParams } from "next/navigation";
import { MOCK_ROOMS } from "./mockRooms";

export default function DmIndexPage() {
  const router = useRouter();
  const { companyId } = useParams<{ companyId: string }>();

  useEffect(() => {
    const firstRoom = MOCK_ROOMS[0];
    if (firstRoom) {
      router.replace(`/${companyId}/dm/${firstRoom.roomId}`);
    }
  }, [companyId, router]);

  return null;
}
