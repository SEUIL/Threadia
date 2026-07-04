"use client";

/*
 * 비밀번호 찾기 — app/(auth)/forgot-password/page.tsx
 *
 * 화면 상태: idle(폼) → success(발송 완료)
 *
 */

import { useState } from "react";
import Link from "next/link";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { isAxiosError } from "axios";
import { AuthCard } from "@/components/auth/AuthCard";
import { TextField } from "@/components/ui/TextField";
import { Button } from "@/components/ui/Button";
import { authService } from "@/services/auth.service";
import type { ApiError } from "@/types/auth.types";

const forgotPasswordSchema = z.object({
  email: z.string().min(1, "이메일을 입력하세요").email("이메일 형식이 올바르지 않습니다"),
});

type ForgotPasswordFormValues = z.infer<typeof forgotPasswordSchema>;

export default function ForgotPasswordPage() {
  const [sent, setSent] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ForgotPasswordFormValues>({ resolver: zodResolver(forgotPasswordSchema) });

  const onSubmit = async (values: ForgotPasswordFormValues) => {
    setError(null);
    try {
      await authService.passwordResetRequest({ email: values.email });
      setSent(true);
    } catch (err) {
      if (isAxiosError<ApiError>(err) && err.response) {
        setError(err.response.data.error.message);
      } else {
        setError("재설정 링크 발송에 실패했습니다. 잠시 후 다시 시도해주세요.");
      }
    }
  };

  if (sent) {
    return (
      <AuthCard className="text-center">
        <div className="mx-auto mb-6 grid h-14 w-14 place-items-center rounded-full bg-primary-subtle">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" className="text-primary" aria-hidden="true">
            <polyline points="20 6 9 17 4 12" />
          </svg>
        </div>
        <h1 className="text-xl font-bold text-foreground">이메일을 확인해주세요</h1>
        <p className="mt-3 text-sm text-muted leading-relaxed">
          비밀번호 재설정 링크를 이메일로 전송했습니다.<br />
          메일이 오지 않으면 스팸함을 확인해주세요.
        </p>
        <Link
          href="/login"
          className="mt-8 block w-full rounded-lg btn-gradient py-3 text-sm font-semibold text-primary-foreground text-center transition-colors duration-normal"
        >
          로그인으로 돌아가기
        </Link>
      </AuthCard>
    );
  }

  return (
    <AuthCard>
      <div className="mb-8 text-center">
        <h1 className="text-2xl font-bold text-foreground">비밀번호 찾기</h1>
        <p className="mt-2 text-sm text-muted leading-relaxed">
          가입한 이메일을 입력하면<br />재설정 링크를 보내드립니다
        </p>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-5" noValidate>
        <TextField
          id="email"
          label="이메일"
          type="email"
          autoComplete="email"
          placeholder="가입한 이메일을 입력하세요"
          hint={errors.email?.message}
          {...register("email")}
        />

        {error && <p className="text-sm text-destructive">{error}</p>}

        <Button type="submit" loading={isSubmitting}>재설정 링크 받기</Button>
      </form>

      <p className="mt-6 text-center text-sm">
        <Link href="/login" className="font-semibold text-primary hover:text-primary-hover transition-colors duration-fast">
          ← 로그인으로 돌아가기
        </Link>
      </p>
    </AuthCard>
  );
}
