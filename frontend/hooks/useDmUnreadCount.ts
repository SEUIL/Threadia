"use client";

/*
 * 전체 DM unread 개수 — RailShell/HubOverlay의 "채팅·DM" nav 배지에 쓴다.
 * useUnreadNotificationCount와 동일 패턴(WBS 4.8.2). 이전엔 backend가 unread
 * count를 안 줘서(WBS 4.7.3.2 이슈 #174) MOCK_DM_UNREAD_COUNT 상수로 남겨뒀는데,
 * 그 이슈가 해결돼 실 API로 교체한다. 조회 실패는 배지를 숨기는 정도로만
 * 처리하고 nav 자체를 막지 않는다.
 *
 * React Query 도입(이슈 #217): RailShell과 HubOverlay가 동시에 이 훅을
 * 호출해도(둘 다 마운트되는 경우는 없지만, 레이아웃 전환 순간엔 겹칠 수
 * 있다) 같은 queryKey를 공유해 요청이 dedup된다 — 이전엔 각자 useEffect로
 * fetch-on-mount 했다.
 */
import { useQuery } from "@tanstack/react-query";
import { directMessageService } from "@/services/direct-message.service";

export function useDmUnreadCount(): number {
  const { data } = useQuery({
    queryKey: ["dm-unread-count"],
    queryFn: async () => {
      const { data } = await directMessageService.unreadCount();
      return data.data.unreadCount;
    },
  });

  return data ?? 0;
}
