"use client";

/*
 * 내가 받은 PENDING 초대 개수 — RailShell/HubOverlay의 "받은 초대" nav 배지에 쓴다.
 * 조회 실패는 배지를 숨기는 정도로만 처리하고 nav 자체를 막지 않는다.
 */
import { useEffect, useState } from "react";
import { invitationService } from "@/services/invitation.service";
import { isInvitationExpired } from "@/lib/invitation";

export function usePendingInvitationCount(): number {
  const [count, setCount] = useState(0);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const { data } = await invitationService.listMine();
        if (cancelled) return;
        const pending = data.data.invitations.filter(
          (invitation) => invitation.status === "PENDING" && !isInvitationExpired(invitation)
        ).length;
        setCount(pending);
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
