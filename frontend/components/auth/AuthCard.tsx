/*
 * AuthCard — components/auth/AuthCard.tsx
 *
 * 역할:
 *   로그인·회원가입·비밀번호 재설정 화면의 공통 카드 컨테이너.
 *   4개 화면에 중복되던 카드 마크업(bg-surface, shadow, padding)을 한 곳으로 모은다.
 *   이후 카드 관련 디테일(accent 라인, 진입 애니메이션 등)은 이 파일에서만 관리한다.
 *
 * 스타일 규칙:
 *   - style={{ }} 금지. globals.css @theme 에서 생성된 Tailwind 클래스만 사용.
 *   - 색상은 시멘틱 토큰(primary/accent 등)만 사용. 팔레트 클래스(bg-palette-*) 직접 사용 금지.
 *
 * 디테일:
 *   - 상단 accent 라인: before pseudo-element 로 처리해 자식 마크업을 오염시키지 않는다.
 *     teal(primary) → violet(accent) → teal(primary) 대칭 그라데이션.
 *   - overflow-hidden 으로 accent 라인을 카드 라운드 모서리에 맞춰 잘라낸다.
 *
 * props:
 *   - children  : 카드 내부 내용
 *   - className  : 추가 클래스 (예: 성공 화면의 "text-center")
 */
export function AuthCard({
  children,
  className = "",
}: {
  children: React.ReactNode;
  className?: string;
}) {
  return (
    <div
      className={
        "relative w-full overflow-hidden rounded-xl bg-surface p-6 shadow-md sm:p-10 " +
        "animate-enter-delayed " +
        "before:absolute before:inset-x-0 before:top-0 before:h-1 before:content-[''] " +
        "before:bg-gradient-to-r before:from-primary before:via-accent before:to-primary " +
        className
      }
    >
      {children}
    </div>
  );
}
