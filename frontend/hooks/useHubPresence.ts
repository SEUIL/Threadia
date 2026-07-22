"use client";

/*
 * 허브 온라인 상태 — "온라인 = 허브 페이지 접속 여부"(정철진 확인, 2026-07-21).
 * 이 훅이 마운트돼 있는 동안(=/hub 페이지에 떠 있는 동안)만 주기적으로
 * heartbeat를 보낸다. 언마운트(페이지 이탈)되면 interval이 멈추고, 백엔드
 * HubPresenceService의 ONLINE_WINDOW(20초)가 지나면 자동으로 오프라인
 * 처리된다 — 별도 "나 나감" 신호를 안 보내도 된다.
 */
import { useEffect, useState } from "react";
import { hubPresenceService } from "@/services/hub-presence.service";

// 백엔드 ONLINE_WINDOW(20초)보다 여유 있게 짧아야 폴링 한 번 실패해도 바로
// 오프라인 처리되지 않는다.
const HEARTBEAT_INTERVAL_MS = 10000;

export function useHubPresence(companyId: number | string | undefined) {
  const [onlineUserIds, setOnlineUserIds] = useState<Set<number>>(new Set());

  useEffect(() => {
    if (companyId === undefined) return;
    let cancelled = false;

    const beat = async () => {
      try {
        const { data } = await hubPresenceService.heartbeat(companyId);
        if (!cancelled) setOnlineUserIds(new Set(data.data.onlineUserIds));
      } catch {
        // 하트비트 실패는 조용히 무시한다 — 다음 주기에 다시 시도하면 되고,
        // 실패했다고 화면에 에러를 띄울 정도로 중요한 요청은 아니다.
      }
    };

    void beat();
    const interval = setInterval(() => void beat(), HEARTBEAT_INTERVAL_MS);
    return () => {
      cancelled = true;
      clearInterval(interval);
    };
  }, [companyId]);

  return onlineUserIds;
}
