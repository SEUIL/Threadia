"use client";

/*
 * 회원가입 화면 — app/(auth)/signup/page.tsx
 *
 * 필드: email / nickname / password (avatarUrl 은 null 로 전송)
 *
 * TODO (Day 4):
 *   - react-hook-form + zod 연결
 *   - authService.signup() 호출
 *   - 성공 시 /login 으로 router.push
 */

import Link from "next/link";
import { AuthCard } from "@/components/auth/AuthCard";
import { TextField } from "@/components/ui/TextField";
import { PasswordField } from "@/components/ui/PasswordField";
import { Button } from "@/components/ui/Button";

export default function SignupPage() {
  return (
    <AuthCard>

      {/* 제목 */}
      <div className="mb-8 text-center">
        <h1 className="text-2xl font-bold text-foreground">회원가입</h1>
        <p className="mt-2 text-sm text-muted">Threadia 계정을 만들어보세요</p>
      </div>

      {/* 폼 */}
      <form onSubmit={(e) => e.preventDefault()} className="flex flex-col gap-5" noValidate>

        {/* 이메일 */}
        <TextField
          id="email"
          label="이메일"
          type="email"
          autoComplete="email"
          placeholder="example@threadia.com"
        />

        {/* 닉네임 */}
        <TextField
          id="nickname"
          label="닉네임"
          type="text"
          autoComplete="username"
          placeholder="팀원들에게 표시될 이름"
        />

        {/* 비밀번호 — 강도 미터 표시 */}
        <PasswordField
          id="password"
          label="비밀번호"
          autoComplete="new-password"
          placeholder="비밀번호를 입력하세요"
          showStrength
          hint="영문, 숫자를 포함하여 8자 이상 입력하세요"
        />

        {/* 에러 메시지 — Day 4 활성화 */}
        {/* {error && <p className="text-sm text-destructive">{error}</p>} */}

        {/* 가입 버튼 — Day 4에 loading 상태 연결 */}
        <Button type="submit">계정 만들기</Button>
      </form>

      {/* 구분선 */}
      <div className="my-6 flex items-center gap-3">
        <div className="h-px flex-1 bg-border" />
        <span className="text-xs text-placeholder">또는</span>
        <div className="h-px flex-1 bg-border" />
      </div>

      {/* Google OAuth 버튼 */}
      <button
        type="button"
        className="flex w-full items-center justify-center gap-3 rounded-lg border border-border bg-surface py-3 text-sm font-medium text-foreground transition-all duration-normal hover:border-ring hover:shadow-sm"
      >
        <svg width="18" height="18" viewBox="0 0 18 18" aria-hidden="true">
          <path fill="#4285F4" d="M17.64 9.2c0-.637-.057-1.251-.164-1.84H9v3.481h4.844a4.14 4.14 0 0 1-1.796 2.716v2.259h2.908c1.702-1.567 2.684-3.875 2.684-6.615Z"/>
          <path fill="#34A853" d="M9 18c2.43 0 4.467-.806 5.956-2.18l-2.908-2.259c-.806.54-1.837.86-3.048.86-2.344 0-4.328-1.584-5.036-3.711H.957v2.332A8.997 8.997 0 0 0 9 18Z"/>
          <path fill="#FBBC05" d="M3.964 10.71A5.41 5.41 0 0 1 3.682 9c0-.593.102-1.17.282-1.71V4.958H.957A8.996 8.996 0 0 0 0 9c0 1.452.348 2.827.957 4.042l3.007-2.332Z"/>
          <path fill="#EA4335" d="M9 3.58c1.321 0 2.508.454 3.44 1.345l2.582-2.58C13.463.891 11.426 0 9 0A8.997 8.997 0 0 0 .957 4.958L3.964 6.29C4.672 4.163 6.656 3.58 9 3.58Z"/>
        </svg>
        Google로 가입하기
      </button>

      {/* 로그인 링크 */}
      <p className="mt-6 text-center text-sm text-muted">
        이미 계정이 있으신가요?{" "}
        <Link href="/login" className="font-semibold text-primary hover:text-primary-hover transition-colors duration-fast">
          로그인
        </Link>
      </p>
    </AuthCard>
  );
}
