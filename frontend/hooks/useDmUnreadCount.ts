"use client";

/*
 * 전체 DM unread 개수 — RailShell/HubOverlay의 "채팅·DM" nav 배지에 쓴다.
 * useUnreadNotificationCount와 동일 패턴(WBS 4.8.2). 이전엔 backend가 unread
 * count를 안 줘서(WBS 4.7.3.2 이슈 #174) MOCK_DM_UNREAD_COUNT 상수로 남겨뒀는데,
 * 그 이슈가 해결돼 실 API로 교체한다. 조회 실패는 배지를 숨기는 정도로만
 * 처리하고 nav 자체를 막지 않는다.
 */
import { useEffect, useState } from "react";
import { directMessageService } from "@/services/direct-message.service";

export function useDmUnreadCount(): number {
  const [count, setCount] = useState(0);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const { data } = await directMessageService.unreadCount();
        if (!cancelled) setCount(data.data.unreadCount);
      } catch {
        if (!cancelled) setCount(0);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  return count;
}
