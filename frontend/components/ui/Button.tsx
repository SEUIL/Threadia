/*
 * Button — components/ui/Button.tsx
 *
 * 역할:
 *   auth 화면의 기본 primary 액션 버튼. loading 상태 스피너를 내장한다.
 *   Day 4 API 연동 시 loading prop 만 넘기면 제출 중 상태가 표현된다.
 *
 * 디테일:
 *   - loading=true → 버튼 disabled, aria-busy, 스피너 + "처리 중…" 표시.
 *   - 스피너: border-t-transparent + animate-spin(Tailwind 기본 유틸)로 구현.
 *
 * 스타일 규칙:
 *   - style={{ }} 금지. 색은 primary 계열 시멘틱 토큰만 사용.
 */
interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  loading?: boolean;
}

export function Button({
  loading = false,
  children,
  disabled,
  className = "",
  ...props
}: ButtonProps) {
  return (
    <button
      disabled={loading || disabled}
      aria-busy={loading}
      className={
        "inline-flex w-full items-center justify-center gap-2 rounded-lg bg-primary py-3 " +
        "text-sm font-semibold text-primary-foreground transition-colors duration-normal " +
        "hover:bg-primary-hover active:bg-primary-active " +
        "disabled:cursor-not-allowed disabled:opacity-70 " +
        className
      }
      {...props}
    >
      {loading && (
        <span
          className="h-4 w-4 animate-spin rounded-full border-2 border-current border-t-transparent"
          aria-hidden="true"
        />
      )}
      {loading ? "처리 중…" : children}
    </button>
  );
}
