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
 * 스타일 규칙:
 *   style={{ }} 사용 금지. globals.css @theme 에서 생성된 클래스만 사용.
 *
 * 반응형:
 *   모바일  (< 640px) : px-4
 *   태블릿+ (≥ 640px) : px-6
 *   카드 최대 너비: max-w-[480px] (--layout-content-width 기준)
 */
export default function AuthLayout({ children }: { children: React.ReactNode }) {
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
        <div className="absolute -top-48 left-1/2 h-[42rem] w-[160%] -translate-x-1/2 rounded-[50%] bg-gradient-to-r from-aurora-1 via-aurora-3 to-aurora-2 opacity-15 blur-3xl" />
        {/* 상단 커튼 2 — 반대 방향으로 겹쳐 색이 섞이게 */}
        <div className="absolute -top-32 left-1/2 h-[36rem] w-[140%] -translate-x-1/2 rounded-[50%] bg-gradient-to-l from-aurora-2 via-transparent to-aurora-1 opacity-10 blur-3xl" />
        {/* 하단 은은한 잔광 */}
        <div className="absolute -bottom-56 left-1/2 h-[36rem] w-[150%] -translate-x-1/2 rounded-[50%] bg-gradient-to-r from-aurora-3 via-aurora-1 to-aurora-3 opacity-10 blur-3xl" />
      </div>

      {/* 콘텐츠 레이어 — z-10 으로 aurora 배경 위에 배치 */}
      <div className="relative z-10 flex flex-1 flex-col">

        {/* 브랜드 헤더 — 추후 <Image> 로고로 교체 */}
        <header className="pt-10 pb-6 text-center">
          <div className="inline-flex items-center justify-center gap-2 animate-enter">
            <span className="grid h-9 w-9 place-items-center rounded-lg text-base font-extrabold bg-gradient-to-br from-primary to-accent text-primary-foreground shadow-sm">
              T
            </span>
            <span className="text-xl font-bold text-gradient-brand">
              Threadia
            </span>
          </div>
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
