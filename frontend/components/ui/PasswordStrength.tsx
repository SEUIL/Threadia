/*
 * PasswordStrength — components/ui/PasswordStrength.tsx
 *
 * 역할:
 *   비밀번호 입력값의 강도를 4칸 바 + 텍스트로 표시한다.
 *   PasswordField 의 showStrength 옵션에서 사용한다.
 *
 * 판정 기준 (충족 개수 = score, 0~4):
 *   - 8자 이상 / 영문 포함 / 숫자 포함 / 특수문자 포함
 *   score 0~1 → 약함(destructive), 2~3 → 보통(warning), 4 → 강함(success)
 *
 * 스타일 규칙:
 *   - 색은 상태 토큰(destructive/warning/success)만 사용. hex 하드코딩 금지.
 *   - 값이 비어 있으면 아무것도 렌더하지 않는다.
 */
export function PasswordStrength({ value }: { value: string }) {
  if (!value) return null;

  const checks = [
    value.length >= 8,
    /[a-zA-Z]/.test(value),
    /[0-9]/.test(value),
    /[^a-zA-Z0-9]/.test(value),
  ];
  const score = checks.filter(Boolean).length; // 0~4

  const level =
    score <= 1
      ? { label: "약함", bar: "bg-destructive", text: "text-destructive" }
      : score <= 3
        ? { label: "보통", bar: "bg-warning", text: "text-warning" }
        : { label: "강함", bar: "bg-success", text: "text-success" };

  return (
    <div className="mt-1">
      <div className="flex gap-1.5">
        {[0, 1, 2, 3].map((i) => (
          <span
            key={i}
            className={
              "h-1 flex-1 rounded-full transition-colors duration-normal " +
              (i < score ? level.bar : "bg-border")
            }
          />
        ))}
      </div>
      <p className={"mt-1 text-xs font-medium " + level.text}>
        비밀번호 강도: {level.label}
      </p>
    </div>
  );
}
