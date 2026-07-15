"use client";

/*
 * ProfileEditModal — components/profile/ProfileEditModal.tsx
 *
 * 본인 프로필(nickname/avatarUrl) 수정 모달. WBS 4.2.2.
 *
 * 화면정의서에 프로필 편집 전용 화면(S번호)이 없어서 전용 라우트 대신 모달로 뒀다 —
 * 레일 아바타를 누르면 어느 화면에서든 바로 열린다(사용자 결정).
 *
 * 서버 계약(API-명세서 §13.3, PATCH /api/v1/users/me/profile):
 *   - nickname/avatarUrl 중 하나 이상 필요(빈 객체는 400).
 *   - 생략한 필드는 기존 값 보존, avatarUrl: null은 명시적 제거.
 *   - 미지원 필드는 거부(@JsonAnySetter) — 보낼 필드만 담아야 한다.
 * 그래서 이 폼은 **실제로 바뀐 필드만** 골라 보낸다. 바뀐 게 없으면 아예 호출하지
 * 않고 닫는다(빈 body로 400을 맞지 않게).
 *
 * 아바타는 파일 업로드가 아니라 URL 문자열 입력이다(명세서 §4: "프로필 사진은 파일
 * 업로드 API가 아니라 avatarUrl 저장으로 시작한다").
 *
 * 구조: 열림 판정을 바깥(ProfileEditModal)에서 하고 폼 본체(ProfileEditDialog)는
 * 열렸을 때만 마운트한다. 닫으면 언마운트되므로 다시 열 때 defaultValues로 자연히
 * 초기화된다 — effect로 reset/setState 하지 않아도 된다.
 *
 * 스타일 규칙: style={{}} 금지, @theme 시멘틱 토큰 클래스만 사용.
 */
import { useEffect, useState } from "react";
import { useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { isAxiosError } from "axios";
import { TextField } from "@/components/ui/TextField";
import { Button } from "@/components/ui/Button";
import { UserAvatar } from "@/components/profile/UserAvatar";
import { authService } from "@/services/auth.service";
import { useAuth } from "@/contexts/AuthContext";
import type { ApiError, ProfileUpdateRequest, UserSummary } from "@/types/auth.types";

// 서버 검증 기준과 맞춘다(nickname 2~100·공백 불가, avatarUrl 최대 500).
// avatarUrl은 서버가 URL 형식 검증을 하지 않으므로 여기서도 형식은 강제하지 않는다.
const profileSchema = z.object({
  nickname: z
    .string()
    .trim()
    .min(2, "닉네임은 2자 이상이어야 합니다")
    .max(100, "닉네임은 100자 이하여야 합니다"),
  avatarUrl: z.string().trim().max(500, "이미지 주소는 500자 이하여야 합니다"),
});

type ProfileFormValues = z.infer<typeof profileSchema>;

function ProfileEditDialog({ user, onClose }: { user: UserSummary; onClose: () => void }) {
  const { applyUser } = useAuth();
  const [error, setError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    control,
    formState: { errors, isSubmitting },
  } = useForm<ProfileFormValues>({
    resolver: zodResolver(profileSchema),
    defaultValues: { nickname: user.nickname, avatarUrl: user.avatarUrl ?? "" },
  });

  // 저장 전 미리보기용. watch() 대신 useWatch를 쓴다 — watch()가 돌려주는 함수는
  // 안전하게 메모이즈할 수 없어 React Compiler가 이 컴포넌트 최적화를 건너뛴다.
  const previewNickname = useWatch({ control, name: "nickname" });
  const previewAvatarUrl = useWatch({ control, name: "avatarUrl" });

  // Esc로 닫기 — 모달 기본 동작.
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [onClose]);

  const onSubmit = async (values: ProfileFormValues) => {
    setError(null);

    // 바뀐 필드만 담는다(위 주석의 계약 참고).
    const body: ProfileUpdateRequest = {};
    if (values.nickname !== user.nickname) {
      body.nickname = values.nickname;
    }
    // 입력을 비우면 avatar 제거(null), 아니면 그 값. 기존과 같으면 안 보낸다.
    const nextAvatarUrl = values.avatarUrl === "" ? null : values.avatarUrl;
    if (nextAvatarUrl !== (user.avatarUrl ?? null)) {
      body.avatarUrl = nextAvatarUrl;
    }
    if (Object.keys(body).length === 0) {
      onClose();
      return;
    }

    try {
      const { data } = await authService.updateProfile(body);
      applyUser(data.data.user);
      onClose();
    } catch (err) {
      if (isAxiosError<ApiError>(err) && err.response) {
        setError(err.response.data.error.message);
      } else {
        setError("프로필 수정에 실패했습니다. 잠시 후 다시 시도해주세요.");
      }
    }
  };

  return (
    <div className="fixed inset-0 z-modal flex items-center justify-center px-4">
      {/* 딤 배경 — 클릭하면 닫힌다 */}
      <button
        type="button"
        aria-label="닫기"
        onClick={onClose}
        className="absolute inset-0 cursor-default bg-overlay"
      />

      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="profile-modal-title"
        className="relative w-full max-w-[420px] rounded-xl bg-surface p-6 shadow-xl animate-enter"
      >
        <h2 id="profile-modal-title" className="text-lg font-bold text-foreground">
          프로필 수정
        </h2>
        <p className="mt-1 text-sm text-muted">이름과 프로필 사진을 변경할 수 있습니다.</p>

        {/* 현재 입력 기준 미리보기 — 저장 전에 어떻게 보일지 확인 */}
        <div className="mt-5 flex items-center gap-3">
          <UserAvatar
            nickname={previewNickname || user.nickname}
            avatarUrl={previewAvatarUrl || null}
            className="h-12 w-12 text-sm"
          />
          <div className="min-w-0">
            <p className="truncate text-sm font-medium text-foreground">
              {previewNickname || user.nickname}
            </p>
            {/* email은 이 endpoint에서 수정할 수 없다(명세서 §13.3) — 참고용 표시만 */}
            <p className="truncate text-xs text-muted">{user.email}</p>
          </div>
        </div>

        {error && <p className="mt-4 text-sm text-destructive">{error}</p>}

        <form onSubmit={handleSubmit(onSubmit)} className="mt-5 flex flex-col gap-5" noValidate>
          <TextField
            id="profile-nickname"
            label="이름"
            placeholder="팀원들에게 표시될 이름"
            hint={errors.nickname?.message}
            {...register("nickname")}
          />
          <TextField
            id="profile-avatar-url"
            label="프로필 사진 주소"
            placeholder="https://.../profile.png"
            hint={errors.avatarUrl?.message ?? "비워두면 프로필 사진을 사용하지 않습니다"}
            {...register("avatarUrl")}
          />

          <div className="flex justify-end gap-2">
            <button
              type="button"
              onClick={onClose}
              className="rounded-md px-3 py-1.5 text-sm font-medium text-muted transition-colors duration-fast hover:bg-primary-subtle"
            >
              취소
            </button>
            <Button type="submit" loading={isSubmitting} className="w-auto px-4 py-1.5">
              저장
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}

export function ProfileEditModal({ open, onClose }: { open: boolean; onClose: () => void }) {
  const { user } = useAuth();
  // 닫혀 있거나 아직 사용자 정보가 없으면 폼 자체를 마운트하지 않는다(위 구조 주석 참고).
  if (!open || !user) return null;
  return <ProfileEditDialog user={user} onClose={onClose} />;
}
