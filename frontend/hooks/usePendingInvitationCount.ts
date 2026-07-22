"use client";

/*
 * 내가 받은 PENDING 초대 개수 — RailShell/HubOverlay의 "받은 초대" nav 배지에 쓴다.
 * 조회 실패는 배지를 숨기는 정도로만 처리하고 nav 자체를 막지 않는다.
 *
 * React Query 도입(이슈 #217): 다른 배지 훅과 동일 패턴으로 맞춘다.
 */
import { useQuery } from "@tanstack/react-query";
import { invitationService } from "@/services/invitation.service";
import { isInvitationExpired } from "@/lib/invitation";

export function usePendingInvitationCount(): number {
  const { data } = useQuery({
    queryKey: ["pending-invitation-count"],
    queryFn: async () => {
      const { data } = await invitationService.listMine();
      return data.data.invitations.filter(
        (invitation) => invitation.status === "PENDING" && !isInvitationExpired(invitation)
      ).length;
    },
  });

  return data ?? 0;
}
