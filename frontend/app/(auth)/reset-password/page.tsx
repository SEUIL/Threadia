"use client";

/*
 * 비밀번호 재설정 — app/(auth)/reset-password/page.tsx
 *
 * URL 파라미터: ?token=... (이메일 링크에서 전달, 백엔드
 * threadia.password-reset.url-prefix 기본값이 이 경로를 가리킨다)
 * 화면 상태: idle(폼) → done(변경 완료)
 *
 * 보안: 재설정 성공 시 해당 사용자의 모든 refresh_tokens revoke (백엔드 처리)
 */

import { Suspense, useState } from "react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { isAxiosError } from "axios";
import { AuthCard } from "@/components/auth/AuthCard";
import { PasswordField } from "@/components/ui/PasswordField";
import { Button } from "@/components/ui/Button";
import { authService } from "@/services/auth.service";
import type { ApiError } from "@/types/auth.types";

const resetPasswordSchema = z
  .object({
    newPassword: z
      .string()
      .min(8, "비밀번호는 8자 이상이어야 합니다")
      .max(72)
      .regex(/[A-Za-z]/, "영문을 포함해야 합니다")
      .regex(/[0-9]/, "숫자를 포함해야 합니다"),
    confirmPassword: z.string().min(1, "비밀번호를 한 번 더 입력하세요"),
  })
  .refine((values) => values.newPassword === values.confirmPassword, {
    message: "비밀번호가 일치하지 않습니다",
    path: ["confirmPassword"],
  });

type ResetPasswordFormValues = z.infer<typeof resetPasswordSchema>;

function ResetPasswordForm() {
  const token = useSearchParams().get("token");
  const [done, setDone] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ResetPasswordFormValues>({ resolver: zodResolver(resetPasswordSchema) });

  const onSubmit = async (values: ResetPasswordFormValues) => {
    if (!token) return;
    setError(null);
    try {
      await authService.passwordResetConfirm({ token, newPassword: values.newPassword });
      setDone(true);
    } catch (err) {
      if (isAxiosError<ApiError>(err) && err.response) {
        setError(err.response.data.error.message);
      } else {
        setError("비밀번호 변경에 실패했습니다. 잠시 후 다시 시도해주세요.");
      }
    }
  };

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

  if (!token) {
    return (
      <AuthCard className="text-center">
        <h1 className="text-xl font-bold text-foreground">유효하지 않은 링크입니다</h1>
        <p className="mt-3 text-sm text-muted leading-relaxed">
          비밀번호 재설정 링크가 올바르지 않거나 만료되었습니다.<br />
          다시 요청해주세요.
        </p>
        <Link
          href="/forgot-password"
          className="mt-8 block w-full rounded-lg btn-gradient py-3 text-sm font-semibold text-primary-foreground text-center transition-colors duration-normal"
        >
          재설정 다시 요청하기
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

      <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-5" noValidate>
        <PasswordField
          id="newPassword"
          label="새 비밀번호"
          autoComplete="new-password"
          placeholder="새 비밀번호를 입력하세요"
          showStrength
          hint={errors.newPassword?.message ?? "영문, 숫자를 포함하여 8자 이상 입력하세요"}
          {...register("newPassword")}
        />

        <PasswordField
          id="confirmPassword"
          label="새 비밀번호 확인"
          autoComplete="new-password"
          placeholder="비밀번호를 한 번 더 입력하세요"
          hint={errors.confirmPassword?.message}
          {...register("confirmPassword")}
        />

        {error && (
          <p role="alert" className="text-sm text-destructive">
            {error}
          </p>
        )}

        <Button type="submit" loading={isSubmitting}>비밀번호 변경하기</Button>
      </form>
    </AuthCard>
  );
}

export default function ResetPasswordPage() {
  return (
    <Suspense fallback={null}>
      <ResetPasswordForm />
    </Suspense>
  );
}
