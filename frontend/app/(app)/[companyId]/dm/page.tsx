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
 *
 * React Query 마이그레이션(이슈 #217): 이 페이지는 dm/layout.tsx가 이미 렌더
 * 중인 `children`이라, 예전엔 layout과 이 페이지가 같은 room 목록을 각자
 * fetch했다(cancelled ref로 언마운트 후 setState/redirect만 막았을 뿐, 요청
 * 자체는 중복이었다). dm/[roomId]/page.tsx와 마찬가지로 layout과 같은
 * useDmRoomsQuery(같은 queryKey)를 쓰면 React Query가 dedup해서 요청이 한 번만
 * 나가고, 언마운트 후 late redirect 문제도 React Query의 mount 상태 추적으로
 * 자연히 해결된다(언마운트되면 이 컴포넌트의 렌더 자체가 없어지므로 router.replace
 * 호출부가 아예 실행되지 않는다 — cancelled ref가 하던 일을 컴포넌트 생명주기가
 * 대신한다).
 */
import { useEffect } from "react";
import { useRouter, useParams } from "next/navigation";
import { useDmRoomsQuery } from "@/hooks/useDmQueries";

export default function DmIndexPage() {
  const router = useRouter();
  const { companyId } = useParams<{ companyId: string }>();
  const roomsQuery = useDmRoomsQuery(companyId);
  const firstRoomId = roomsQuery.data?.[0]?.id;

  useEffect(() => {
    if (firstRoomId !== undefined) {
      router.replace(`/${companyId}/dm/${firstRoomId}`);
    }
  }, [companyId, firstRoomId, router]);

  if (roomsQuery.isLoading || firstRoomId !== undefined) return null;

  return (
    <div className="flex h-full items-center justify-center px-6 text-center">
      <p className="text-sm text-muted">
        {roomsQuery.isError
          ? "대화 목록을 불러오지 못했습니다."
          : "아직 대화가 없습니다. 왼쪽 + 버튼으로 새 대화를 시작해보세요."}
      </p>
    </div>
  );
}
