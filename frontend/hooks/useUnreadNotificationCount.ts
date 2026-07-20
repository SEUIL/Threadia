"use client";

/*
 * 현재 회사 기준 unread 알림 개수 — RailShell/HubOverlay의 "알림" nav 배지에 쓴다.
 * WBS 4.8.2. 조회 실패는 배지를 숨기는 정도로만 처리하고 nav 자체를 막지 않는다
 * (usePendingInvitationCount와 동일 패턴).
 */
import { useEffect, useState } from "react";
import { notificationService } from "@/services/notification.service";

export function useUnreadNotificationCount(companyId: string | number | undefined): number {
  const [count, setCount] = useState(0);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      if (!companyId) {
        if (!cancelled) setCount(0);
        return;
      }
      try {
        const { data } = await notificationService.unreadCount({ companyId });
        if (!cancelled) setCount(data.data.unreadCount);
      } catch {
        if (!cancelled) setCount(0);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [companyId]);

  return count;
}
