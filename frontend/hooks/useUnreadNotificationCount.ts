"use client";

/*
 * 현재 회사 기준 unread 알림 개수 — RailShell/HubOverlay의 "알림" nav 배지에 쓴다.
 * WBS 4.8.2. 조회 실패는 배지를 숨기는 정도로만 처리하고 nav 자체를 막지 않는다
 * (usePendingInvitationCount와 동일 패턴).
 *
 * React Query 도입(이슈 #217): companyId를 queryKey에 포함해 회사별로 캐시를
 * 분리한다 — enabled로 companyId 없을 때 요청 자체를 안 보낸다(이전엔
 * useEffect 안에서 조건 분기로 처리).
 */
import { useQuery } from "@tanstack/react-query";
import { notificationService } from "@/services/notification.service";

export function useUnreadNotificationCount(companyId: string | number | undefined): number {
  const { data } = useQuery({
    queryKey: ["notification-unread-count", companyId],
    queryFn: async () => {
      const { data } = await notificationService.unreadCount({ companyId: companyId! });
      return data.data.unreadCount;
    },
    enabled: companyId !== undefined,
  });

  return data ?? 0;
}
