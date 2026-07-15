"use client";

/*
 * InvitationEnvelope — components/invitations/InvitationEnvelope.tsx
 *
 * "미확인 더미" 한 칸을 이루는 봉투 하나. 3단계로 움직인다:
 *   1. falling — mount 시 화면 위에서 스프링으로 떨어져 쌓인다.
 *   2. landed  — 클릭 대기. 여러 개면 seededRandom(invitation.id)로 회전/오프셋을
 *                결정적으로 고정해 "아무렇게나 쌓인" 느낌을 내되 리렌더에는 안 흔들린다.
 *   3. opening — 클릭 시 덮개(flap)가 rotateX로 열리고, 그 뒤 편지지 모서리가
 *                슬라이드된다. 일정 시간 뒤 onOpen 콜백으로 부모에 알려 더미에서
 *                제거하고 InvitationLetter로 넘긴다 — 이때 layoutId가 같아서
 *                framer-motion이 봉투 속 편지지 → 카드로 자동 모핑한다.
 *
 * prefers-reduced-motion: globals.css의 규칙은 CSS transition/animation만
 * 무력화하고 framer-motion의 인라인 스타일 기반 모션은 안 잡히므로, 여기서
 * useReducedMotion()으로 직접 낙하 애니메이션을 생략한다.
 */
import { useEffect, useState } from "react";
import { motion, useReducedMotion } from "framer-motion";
import { seededRandom } from "@/lib/seededRandom";
import type { InvitationResponse } from "@/types/invitation.types";

type EnvelopePhase = "falling" | "landed" | "opening";

const FALL_STAGGER_SEC = 0.12;
const LETTER_SLIDE_DELAY_SEC = 0.28;
const REMOVE_AFTER_MS = 900;

export function InvitationEnvelope({
  invitation,
  index,
  onOpen,
}: {
  invitation: InvitationResponse;
  index: number;
  onOpen: (id: number) => void;
}) {
  const reduceMotion = useReducedMotion();
  const [phase, setPhase] = useState<EnvelopePhase>(reduceMotion ? "landed" : "falling");
  const rotate = seededRandom(invitation.id, 1) * 8;
  const offsetX = seededRandom(invitation.id, 2) * 14;

  useEffect(() => {
    if (phase !== "opening") return;
    const timer = setTimeout(() => onOpen(invitation.id), REMOVE_AFTER_MS);
    return () => clearTimeout(timer);
  }, [phase, invitation.id, onOpen]);

  const handleClick = () => {
    if (phase === "landed") setPhase("opening");
  };

  return (
    <motion.div
      className="absolute left-1/2 top-0"
      initial={reduceMotion ? false : { y: -220, x: offsetX, rotate: rotate * 2.5, opacity: 0 }}
      animate={{ y: 0, x: offsetX, rotate, opacity: 1 }}
      transition={
        reduceMotion
          ? { duration: 0 }
          : { type: "spring", stiffness: 320, damping: 20, delay: index * FALL_STAGGER_SEC }
      }
      onAnimationComplete={() => setPhase((p) => (p === "falling" ? "landed" : p))}
    >
      <button
        type="button"
        onClick={handleClick}
        disabled={phase === "opening"}
        aria-label={`${invitation.companyName} 초대 봉투 열기`}
        className="relative block -translate-x-1/2 cursor-pointer [perspective:600px]"
      >
        <div className="relative h-28 w-44 overflow-hidden rounded-lg bg-surface shadow-xl">
          {/* 항공우편 봉투 특유의 빨강/네이비 투톤 대시 테두리. Tailwind border-dashed는
              한 가지 색만 지원해서, 같은 dasharray를 절반만큼 어긋나게 겹친 두 SVG
              stroke로 재현한다 — 색상은 브랜드 토큰이 아니라 사용자가 지정한 항공우편
              배색을 그대로 쓴다(명시적 예외). */}
          <svg viewBox="0 0 176 112" className="pointer-events-none absolute inset-0 h-full w-full">
            <rect x="2" y="2" width="172" height="108" rx="10" fill="none" stroke="#d0212f" strokeWidth="2.5" strokeDasharray="7 7" />
            <rect x="2" y="2" width="172" height="108" rx="10" fill="none" stroke="#16214d" strokeWidth="2.5" strokeDasharray="7 7" strokeDashoffset="7" />
          </svg>

          {/* 우표 자리 */}
          <div className="absolute right-3 top-[4.4rem] h-5 w-7 rounded-sm border border-border bg-background" />
          {/* 주소 줄 */}
          <div className="absolute inset-x-4 bottom-4 flex flex-col gap-1.5">
            <div className="h-[3px] w-2/3 rounded-full bg-border" />
            <div className="h-[3px] w-1/2 rounded-full bg-border" />
          </div>

          {/* 편지지 모서리 — 열릴 때 위로 슬라이드하며 InvitationLetter로 모핑 */}
          <motion.div
            layoutId={`invitation-letter-${invitation.id}`}
            className="absolute inset-x-3 top-3 h-16 rounded-md bg-background shadow-sm"
            animate={phase === "opening" ? { y: -30 } : { y: 0 }}
            transition={{
              duration: 0.35,
              delay: phase === "opening" ? LETTER_SLIDE_DELAY_SEC : 0,
              ease: "easeOut",
            }}
          />

          {/* 덮개(flap) — 같은 투톤 대시 윤곽선을 가진 삼각형 SVG를 통째로 rotateX */}
          <motion.svg
            viewBox="0 0 176 64"
            className="absolute inset-x-0 top-0 h-16 w-44 origin-top [backface-visibility:hidden]"
            animate={{ rotateX: phase === "opening" ? -170 : 0 }}
            transition={{ duration: 0.28, ease: "easeIn" }}
          >
            <polygon points="0,0 176,0 88,54" fill="var(--color-surface)" />
            <path d="M1,1 L88,53 L175,1" fill="none" stroke="#d0212f" strokeWidth="2.5" strokeDasharray="7 7" strokeLinejoin="round" />
            <path d="M1,1 L88,53 L175,1" fill="none" stroke="#16214d" strokeWidth="2.5" strokeDasharray="7 7" strokeDashoffset="7" strokeLinejoin="round" />
          </motion.svg>

          {/* 봉인 배지 */}
          <div className="absolute left-1/2 top-9 h-5 w-5 -translate-x-1/2 rounded-full bg-primary ring-2 ring-surface" />
        </div>
      </button>
    </motion.div>
  );
}
