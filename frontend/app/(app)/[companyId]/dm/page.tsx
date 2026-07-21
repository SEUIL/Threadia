"use client";

/*
 * S09 — 채팅·DM 인덱스
 *
 * 예전엔 여기가 직사각형 방 목록 화면이었다. 이제 좌측 아이콘 레일(dm/layout.tsx)이
 * 그 역할을 대신하므로, `/dm`으로 바로 들어오면 첫 번째 대화방으로 즉시
 * 리다이렉트해서 "목록 화면 → 방 선택 → 대화 화면" 2단계를 거치지 않게 한다.
 */
import { useEffect, useState } from "react";
import { useRouter, useParams } from "next/navigation";
import { invitationErrorMessage } from "@/lib/invitation";
import { directMessageService } from "@/services/direct-message.service";

export default function DmIndexPage() {
  const router = useRouter();
  const { companyId } = useParams<{ companyId: string }>();
  const [error, setError] = useState<string | null>(null);
  const [empty, setEmpty] = useState(false);

  useEffect(() => {
    let active = true;
    void (async () => {
      try {
        const { data } = await directMessageService.listRooms(companyId);
        if (!active) return;
        const firstRoom = data.data.rooms[0];
        if (firstRoom) router.replace(`/${companyId}/dm/${firstRoom.id}`);
        else setEmpty(true);
      } catch (loadError) {
        if (active) setError(invitationErrorMessage(loadError, "DM 목록을 불러오지 못했습니다."));
      }
    })();
    return () => {
      active = false;
    };
  }, [companyId, router]);

  if (error) return <div className="px-6 py-8 text-sm text-destructive">{error}</div>;
  if (empty) return <div className="px-6 py-8 text-sm text-muted">아직 대화방이 없습니다.</div>;
  return <div className="px-6 py-8 text-sm text-muted">DM 목록을 불러오는 중...</div>;
}
