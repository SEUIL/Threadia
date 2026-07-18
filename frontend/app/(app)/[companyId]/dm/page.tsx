"use client";

/*
 * S09 — 채팅·DM 인덱스
 *
 * 좌측 아이콘 레일(dm/layout.tsx)이 방 선택 UI 역할을 하므로, `/dm`으로 바로
 * 들어오면 첫 번째 대화방으로 즉시 리다이렉트해서 "목록 화면 → 방 선택 →
 * 대화 화면" 2단계를 거치지 않게 한다.
 *
 * WBS 4.7.3.2 — 실 방 목록(`GET /companies/{companyId}/direct-messages/rooms`)
 * 기준으로 리다이렉트한다. 대화가 하나도 없으면(신규 사용자 등) 리다이렉트할
 * 곳이 없으므로 안내와 함께 레일의 "+" 버튼을 쓰라고 알려준다.
 */
import { useEffect, useState } from "react";
import { useRouter, useParams } from "next/navigation";
import { directMessageService } from "@/services/direct-message.service";

type LoadState = "loading" | "empty" | "error";

export default function DmIndexPage() {
  const router = useRouter();
  const { companyId } = useParams<{ companyId: string }>();
  const [state, setState] = useState<LoadState>("loading");

  useEffect(() => {
    (async () => {
      try {
        const { data } = await directMessageService.listRooms(companyId);
        const firstRoom = data.data.rooms[0];
        if (firstRoom) {
          router.replace(`/${companyId}/dm/${firstRoom.id}`);
          return;
        }
        setState("empty");
      } catch {
        setState("error");
      }
    })();
  }, [companyId, router]);

  if (state === "loading") return null;

  return (
    <div className="flex h-full items-center justify-center px-6 text-center">
      <p className="text-sm text-muted">
        {state === "empty"
          ? "아직 대화가 없습니다. 왼쪽 + 버튼으로 새 대화를 시작해보세요."
          : "대화 목록을 불러오지 못했습니다."}
      </p>
    </div>
  );
}
