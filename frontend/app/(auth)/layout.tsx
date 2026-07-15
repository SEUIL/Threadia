"use client";

/*
 * Auth Layout — app/(auth)/layout.tsx
 *
 * 역할:
 *   로그인·회원가입·비밀번호 재설정 화면의 공통 레이아웃.
 *
 * 의존성:
 *   - 부모: app/layout.tsx (폰트·globals.css 이미 적용됨)
 *   - 자식: login / signup / forgot-password / reset-password page.tsx
 *
 * 레이아웃 구조:
 *   wrapper (min-h-screen, flex-col)
 *     header  : 브랜드 로고 + 서비스명
 *     main    : 카드 중앙 배치 (flex, items-center, justify-center)
 *     footer  : 저작권
 *
 * 인트로 연출(진입점 첫 화면):
 *   Aurora 블롭 → 로고(배지→워드마크) 순으로 framer-motion이 짧게(<1s) 등장시킨다.
 *   카드는 건드리지 않는다 — AuthCard.tsx가 이미 소유한 CSS `animate-enter-delayed`
 *   (--duration-fast=120ms 지연 후 등장)가 이 순서 뒤에 자연스럽게 이어진다.
 *   useReducedMotion()일 때는 전부 생략하고 최종 상태로 즉시 렌더한다.
 *
 * 스타일 규칙:
 *   style={{ }} 사용 금지 — 단, framer-motion이 구동하는 opacity/transform 값은 예외
 *   ([[InvitationEnvelope]] 등 다른 framer-motion 컴포넌트와 동일 원칙).
 *   나머지는 globals.css @theme 에서 생성된 클래스만 사용.
 *
 * 반응형:
 *   모바일  (< 640px) : px-4
 *   태블릿+ (≥ 640px) : px-6
 *   카드 최대 너비: max-w-[480px] (--layout-content-width 기준)
 */
import { motion, useReducedMotion, type Variants } from "framer-motion";

// 블롭별 최종 opacity(정적일 때와 동일한 은은한 값) — 인트로에서 0에서 여기로 페이드인.
// visible에서 무조건 1로 가면 블롭이 원래보다 훨씬 진해지므로 custom으로 개별 지정한다.
const AURORA_TARGET_OPACITY = [0.15, 0.1, 0.1];

const auroraVariants: Variants = {
  hidden: { opacity: 0, scale: 0.92, y: -12 },
  visible: (index: number) => ({
    opacity: AURORA_TARGET_OPACITY[index],
    scale: 1,
    y: 0,
    transition: { duration: 0.8, delay: index * 0.08, ease: "easeOut" },
  }),
};

const logoContainerVariants: Variants = {
  hidden: {},
  visible: { transition: { staggerChildren: 0.08 } },
};

const logoItemVariants: Variants = {
  hidden: { opacity: 0, y: -6, scale: 0.85 },
  visible: { opacity: 1, y: 0, scale: 1, transition: { duration: 0.35, ease: "easeOut" } },
};

export default function AuthLayout({ children }: { children: React.ReactNode }) {
  const reduceMotion = useReducedMotion();
  return (
    <div className="relative flex min-h-screen flex-col overflow-hidden bg-background px-4 sm:px-6">

      {/*
       * Aurora 배경 장식 — '흐르는 빛의 커튼' 방식
       * ─ 코너에 원을 박는 대신, 가로로 넓게 늘인 타원 그라데이션을 상·하단에 겹쳐
       *   teal→aqua→violet 이 자연스럽게 블렌딩되는 오로라(북극광) 느낌을 낸다.
       * ─ 색은 그라데이션 stop(from/via/to)으로 aurora 시멘틱 토큰만 사용.
       * ─ rounded-[50%] + w 초과 + blur-3xl 로 경계 없이 흐르게 처리.
       * ─ 스택 순서: wrapper bg(맨 뒤) → 이 레이어(z-0) → 콘텐츠(z-10).
       * ─ pointer-events-none / aria-hidden: 순수 장식(클릭 통과·스크린리더 무시).
       */}
      <div aria-hidden className="pointer-events-none absolute inset-0 z-0 overflow-hidden">
        {/* 상단 커튼 — teal→aqua→violet 가로 흐름 */}
        <motion.div
          custom={0}
          initial={reduceMotion ? false : "hidden"}
          animate="visible"
          variants={auroraVariants}
          style={{ opacity: reduceMotion ? AURORA_TARGET_OPACITY[0] : undefined }}
          className="absolute -top-48 left-1/2 h-[42rem] w-[160%] -translate-x-1/2 rounded-[50%] bg-gradient-to-r from-aurora-1 via-aurora-3 to-aurora-2 opacity-15 blur-3xl"
        />
        {/* 상단 커튼 2 — 반대 방향으로 겹쳐 색이 섞이게 */}
        <motion.div
          custom={1}
          initial={reduceMotion ? false : "hidden"}
          animate="visible"
          variants={auroraVariants}
          style={{ opacity: reduceMotion ? AURORA_TARGET_OPACITY[1] : undefined }}
          className="absolute -top-32 left-1/2 h-[36rem] w-[140%] -translate-x-1/2 rounded-[50%] bg-gradient-to-l from-aurora-2 via-transparent to-aurora-1 opacity-10 blur-3xl"
        />
        {/* 하단 은은한 잔광 */}
        <motion.div
          custom={2}
          initial={reduceMotion ? false : "hidden"}
          animate="visible"
          variants={auroraVariants}
          style={{ opacity: reduceMotion ? AURORA_TARGET_OPACITY[2] : undefined }}
          className="absolute -bottom-56 left-1/2 h-[36rem] w-[150%] -translate-x-1/2 rounded-[50%] bg-gradient-to-r from-aurora-3 via-aurora-1 to-aurora-3 opacity-10 blur-3xl"
        />
      </div>

      {/* 콘텐츠 레이어 — z-10 으로 aurora 배경 위에 배치 */}
      <div className="relative z-10 flex flex-1 flex-col">

        {/* 브랜드 헤더 — 추후 <Image> 로고로 교체 */}
        <header className="pt-10 pb-6 text-center">
          <motion.div
            initial={reduceMotion ? false : "hidden"}
            animate="visible"
            variants={logoContainerVariants}
            className="inline-flex items-center justify-center gap-2"
          >
            <motion.span
              variants={logoItemVariants}
              className="grid h-9 w-9 place-items-center rounded-lg text-base font-extrabold bg-gradient-to-br from-primary to-accent text-primary-foreground shadow-sm"
            >
              T
            </motion.span>
            <motion.span variants={logoItemVariants} className="text-xl font-bold text-gradient-brand">
              Threadia
            </motion.span>
          </motion.div>
        </header>

        {/*
         * main: flex(row 기본) + items-center(세로 중앙) + justify-center(가로 중앙)
         * flex-1 로 header/footer 제외 나머지 높이를 모두 차지한 뒤 카드를 중앙에 배치.
         * py-8 : 카드가 header/footer 에 붙지 않도록 최소 여백 확보.
         */}
        <main className="flex flex-1 items-center justify-center py-8">
          <div className="w-full max-w-[480px]">
            {children}
          </div>
        </main>

        <footer className="py-8 text-center text-xs text-muted">
          © {new Date().getFullYear()} Threadia. All rights reserved.
        </footer>

      </div>

    </div>
  );
}
