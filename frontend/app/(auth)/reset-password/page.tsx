"use client";

/*
 * 비밀번호 재설정 — app/(auth)/reset-password/page.tsx
 *
 * URL 파라미터: ?token=... (이메일 링크에서 전달)
 * 화면 상태: idle(폼) → done(변경 완료)
 *
 * 보안: 재설정 성공 시 해당 사용자의 모든 refresh_tokens revoke (백엔드 처리)
 *
 * TODO (Day 4):
 *   - useSearchParams() 로 token 읽기
 *   - react-hook-form + zod (비밀번호 확인 일치 검사 포함)
 *   - authService.passwordResetConfirm({ token, newPassword }) 호출
 *   - 토큰 만료/무효 에러 처리
 */

import { useState } from "react";
import Link from "next/link";
import { AuthCard } from "@/components/auth/AuthCard";
import { PasswordField } from "@/components/ui/PasswordField";
import { Button } from "@/components/ui/Button";

export default function ResetPasswordPage() {
  const [done, setDone] = useState(false);

  if (done) {
    return (
      <AuthCard className="text-center">
        <div className="mx-auto mb-6 grid h-14 w-14 place-items-center rounded-full bg-primary-subtle">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" className="text-primary" aria-hidden="true">
            <polyline points="20 6 9 17 4 12" />
          </svg>
        </div>
        <h1 className="text-xl font-bold text-foreground">비밀번호가 변경되었습니다</h1>
        <p className="mt-3 text-sm text-muted leading-relaxed">
          새 비밀번호로 로그인해주세요.<br />
          기존 로그인 세션은 모두 종료되었습니다.
        </p>
        <Link
          href="/login"
          className="mt-8 block w-full rounded-lg btn-gradient py-3 text-sm font-semibold text-primary-foreground text-center transition-colors duration-normal"
        >
          로그인하러 가기
        </Link>
      </AuthCard>
    );
  }

  return (
    <AuthCard>
      <div className="mb-8 text-center">
        <h1 className="text-2xl font-bold text-foreground">새 비밀번호 설정</h1>
        <p className="mt-2 text-sm text-muted">사용할 새 비밀번호를 입력해주세요</p>
      </div>

      <form onSubmit={(e) => { e.preventDefault(); setDone(true); }} className="flex flex-col gap-5" noValidate>

        {/* 새 비밀번호 — 강도 미터 표시 */}
        <PasswordField
          id="newPassword"
          label="새 비밀번호"
          autoComplete="new-password"
          placeholder="새 비밀번호를 입력하세요"
          showStrength
          hint="영문, 숫자를 포함하여 8자 이상 입력하세요"
        />

        {/* 비밀번호 확인 */}
        <PasswordField
          id="confirmPassword"
          label="새 비밀번호 확인"
          autoComplete="new-password"
          placeholder="비밀번호를 한 번 더 입력하세요"
        />

        {/* {error && <p className="text-sm text-destructive">{error}</p>} */}

        <Button type="submit">비밀번호 변경하기</Button>
      </form>
    </AuthCard>
  );
}
