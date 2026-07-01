/*
 * TextField — components/ui/TextField.tsx
 *
 * 역할:
 *   label + input + (선택)hint 를 묶은 일반 텍스트 입력 필드.
 *   email·nickname 등 password 가 아닌 입력에 사용한다.
 *
 * 디테일:
 *   - focus 강조: wrapper 의 group 에 focus-within 이 걸리면 label 색이 primary 로 전환된다.
 *   - labelRight: label 우측 슬롯 (예: "비밀번호를 잊으셨나요?" 링크).
 *
 * 스타일 규칙:
 *   - style={{ }} 금지. 인풋 기본 스타일은 INPUT_BASE(공통 상수)를 사용한다.
 */
import { INPUT_BASE } from "./inputBase";

interface TextFieldProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label: string;
  hint?: string;
  labelRight?: React.ReactNode;
}

export function TextField({
  label,
  hint,
  labelRight,
  id,
  className = "",
  ...props
}: TextFieldProps) {
  return (
    <div className="group flex flex-col gap-1.5">
      <div className="flex items-center justify-between">
        <label
          htmlFor={id}
          className="text-sm font-medium text-foreground transition-colors duration-fast group-focus-within:text-primary"
        >
          {label}
        </label>
        {labelRight}
      </div>
      <input id={id} className={INPUT_BASE + " " + className} {...props} />
      {hint && <p className="text-xs text-placeholder">{hint}</p>}
    </div>
  );
}
