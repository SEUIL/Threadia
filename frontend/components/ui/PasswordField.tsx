"use client";

/*
 * PasswordField — components/ui/PasswordField.tsx
 *
 * 역할:
 *   비밀번호 입력 필드. show/hide 토글과 (선택)강도 미터를 포함한다.
 *   login / signup / reset-password 에서 공통 사용한다.
 *
 * 디테일:
 *   - show/hide 토글: 눈 아이콘 버튼으로 input type 을 password ↔ text 전환.
 *   - focus 강조: TextField 와 동일하게 group-focus-within 으로 label 색 전환.
 *   - showStrength: PasswordStrength 미터 표시. 내부 value state 로 실시간 판정.
 *
 * Day 4 연동 메모:
 *   react-hook-form register 를 {...props} 로 넘기면 register 의 onChange 도
 *   함께 호출된다(내부 setValue + props.onChange 합성). 별도 수정 불필요.
 *
 * 스타일 규칙:
 *   - style={{ }} 금지. 인풋 기본 스타일은 INPUT_BASE(공통 상수)를 사용한다.
 *   - 토글 버튼 자리 확보를 위해 input 에 pr-12 를 덧붙인다.
 */
import { useState } from "react";
import { INPUT_BASE } from "./inputBase";
import { PasswordStrength } from "./PasswordStrength";

interface PasswordFieldProps
  extends React.InputHTMLAttributes<HTMLInputElement> {
  label: string;
  hint?: string;
  labelRight?: React.ReactNode;
  showStrength?: boolean;
}

export function PasswordField({
  label,
  hint,
  labelRight,
  showStrength = false,
  id,
  className = "",
  onChange,
  ...props
}: PasswordFieldProps) {
  const [visible, setVisible] = useState(false);
  const [value, setValue] = useState("");

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

      <div className="relative">
        <input
          id={id}
          type={visible ? "text" : "password"}
          className={INPUT_BASE + " pr-12 " + className}
          onChange={(e) => {
            setValue(e.target.value);
            onChange?.(e);
          }}
          {...props}
        />
        <button
          type="button"
          onClick={() => setVisible((v) => !v)}
          aria-label={visible ? "비밀번호 숨기기" : "비밀번호 표시"}
          className="absolute right-2 top-1/2 grid h-8 w-8 -translate-y-1/2 place-items-center rounded-md text-muted transition-colors duration-fast hover:text-foreground"
        >
          {visible ? (
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <path d="M9.88 9.88a3 3 0 1 0 4.24 4.24" />
              <path d="M10.73 5.08A10.43 10.43 0 0 1 12 5c7 0 10 7 10 7a13.16 13.16 0 0 1-1.67 2.68" />
              <path d="M6.61 6.61A13.526 13.526 0 0 0 2 12s3 7 10 7a9.74 9.74 0 0 0 5.39-1.61" />
              <line x1="2" x2="22" y1="2" y2="22" />
            </svg>
          ) : (
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7Z" />
              <circle cx="12" cy="12" r="3" />
            </svg>
          )}
        </button>
      </div>

      {showStrength && <PasswordStrength value={value} />}
      {hint && <p className="text-xs text-placeholder">{hint}</p>}
    </div>
  );
}
