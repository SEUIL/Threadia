"use client";

/*
 * InvitationLetter — components/invitations/InvitationLetter.tsx
 *
 * 봉투를 연 뒤(또는 이미 처리된 초대가 목록에 바로) 보여줄 편지지 카드.
 * AuthCard.tsx의 상단 accent 라인 패턴(before pseudo-element, teal→violet→teal)을
 * 재사용해 기존 리스트 아이템(border 카드)과는 다른 카드로 새로 디자인했다.
 *
 * layoutId는 InvitationEnvelope 안의 "편지지 모서리" 조각과 값을 맞춰서,
 * 봉투에서 이 카드로 넘어올 때 framer-motion이 자동으로 모양을 모핑한다.
 */
import { motion } from "framer-motion";
import {
  INVITATION_STATUS_CLASS,
  INVITATION_STATUS_LABEL,
  formatExpiresAt,
  invitationRoleLabel,
  invitationScopeLabel,
} from "@/lib/invitation";
import type { InvitationResponse } from "@/types/invitation.types";

export function InvitationLetter({
  invitation,
  expired,
  canRespond,
  busy,
  onAccept,
  onDecline,
}: {
  invitation: InvitationResponse;
  expired: boolean;
  canRespond: boolean;
  busy: boolean;
  onAccept: () => void;
  onDecline: () => void;
}) {
  return (
    <motion.li
      layoutId={`invitation-letter-${invitation.id}`}
      layout
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      transition={{ duration: 0.3, ease: "easeOut" }}
      className={
        "relative overflow-hidden rounded-xl bg-surface p-5 shadow-md " +
        "before:absolute before:inset-x-0 before:top-0 before:h-1 before:content-[''] " +
        "before:bg-gradient-to-r before:from-primary before:via-accent before:to-primary"
      }
    >
      <div className="flex items-start justify-between gap-3">
        <div>
          {/* PROJECT 초대는 회사명만으로 어느 프로젝트인지 알 수 없어 함께 보여준다 */}
          <p className="text-sm font-semibold text-foreground">{invitationScopeLabel(invitation)}</p>
          <p className="mt-1 text-xs text-muted">
            {invitation.inviterNickname} 님이 {invitationRoleLabel(invitation)}(으)로 초대했습니다
          </p>
          <p className="mt-1 text-xs text-muted">{formatExpiresAt(invitation.expiresAt)}</p>
        </div>
        <span
          className={`shrink-0 rounded-full px-2.5 py-0.5 text-xs font-medium ${INVITATION_STATUS_CLASS[invitation.status]}`}
        >
          {expired ? INVITATION_STATUS_LABEL.EXPIRED : INVITATION_STATUS_LABEL[invitation.status]}
        </span>
      </div>

      {canRespond && (
        <div className="mt-4 flex justify-end gap-2">
          <button
            onClick={onDecline}
            disabled={busy}
            className="rounded-md px-3 py-1.5 text-sm font-medium text-muted transition-colors duration-fast hover:bg-primary-subtle disabled:opacity-50"
          >
            거절
          </button>
          <button
            onClick={onAccept}
            disabled={busy}
            className="rounded-md btn-gradient px-3 py-1.5 text-sm font-semibold text-primary-foreground disabled:opacity-50"
          >
            수락
          </button>
        </div>
      )}
    </motion.li>
  );
}
