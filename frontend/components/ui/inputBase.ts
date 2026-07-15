/*
 * inputBase — components/ui/inputBase.ts
 *
 * 역할:
 *   TextField / PasswordField 가 공유하는 인풋 기본 클래스 문자열.
 *   input 스타일을 한 곳에서 관리해 중복을 제거한다.
 *
 * 스타일 규칙:
 *   - 색·radius·duration 은 전부 globals.css @theme 토큰 기반 Tailwind 클래스.
 *   - hex/px 하드코딩 금지.
 */
export const INPUT_BASE =
  "w-full rounded-lg border border-border bg-surface px-4 py-3 text-sm text-foreground " +
  "placeholder:text-placeholder outline-none transition-all duration-normal " +
  "focus:border-ring focus:ring-2 focus:ring-primary-subtle";
