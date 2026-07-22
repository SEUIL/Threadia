"use client";

/*
 * CreateCompanyForm — components/company/CreateCompanyForm.tsx
 *
 * 소속 회사가 없는 로그인 사용자가 S03 no-company 상태에서 회사를 직접 만드는
 * form. 2026-07-18 결정(`docs/07-decisions/기획문서-변경이력.md` 2026-07-18
 * 항목)으로 `POST /api/v1/companies`의 seed-only 제한을 해제하면서 추가했다.
 *
 * 서버 계약(API-명세서 §5, backend CompanyCreateRequest):
 *   - name만 필수(trim, 최대 150자). representativeName/industryCategory는
 *     선택(각 최대 100자), description은 길이 제한 없음.
 *   - logoUrl은 파일 업로드가 아직 없어 항상 null로 보낸다(로고 업로드는
 *     별도 요청서 범위 밖).
 *   - 응답의 companyMember.role은 항상 COMPANY_ADMIN이다 — 생성자가 자동으로
 *     그 회사의 관리자가 된다.
 *
 * 생성 성공 후에는 AuthContext.companyMemberships가 그대로면 화면이 여전히
 * "소속 회사 없음"으로 보이므로, refreshCompanyMemberships()로 다시 채운 뒤
 * 새 회사의 프로젝트 목록으로 이동한다.
 *
 * ⚠️ 이 폼은 hub/page.tsx의 `bg-chrome`(항상 어두운 표면) 카드 안에서만 쓰인다.
 * `TextField`(components/ui/TextField.tsx)는 label에 `text-foreground`를 쓰는데,
 * 이 토큰은 일반 라이트 표면(bg-surface) 카드용이라 dark chrome 배경에서 거의
 * 안 보일 정도로 대비가 낮았다(이슈 리포트, 2026-07-22 스크린샷). login/signup/
 * settings 등 다른 5곳은 전부 라이트 표면이라 TextField 자체는 그대로 두고, 이
 * 폼에서만 label/hint 색을 chrome 전용 토큰(text-chrome-foreground/chrome-muted)
 * 으로 직접 준다 — 아래 설명 필드가 이미 쓰던 패턴과 통일한 것이다.
 */
import { useState } from "react";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Button } from "@/components/ui/Button";
import { INPUT_BASE } from "@/components/ui/inputBase";
import { invitationErrorMessage } from "@/lib/invitation";
import { companyService } from "@/services/company.service";
import { useAuth } from "@/contexts/AuthContext";

// 서버 검증 기준과 맞춘다(CompanyCreateRequest: name 필수·최대 150, representativeName/
// industryCategory 각 최대 100). description은 서버가 길이를 강제하지 않지만
// 화면 상 과도한 입력을 막기 위해 2000자로 여유 있게 제한한다.
const companySchema = z.object({
  name: z.string().trim().min(1, "회사명은 필수입니다").max(150, "회사명은 150자 이하여야 합니다"),
  representativeName: z.string().trim().max(100, "대표자명은 100자 이하여야 합니다"),
  industryCategory: z.string().trim().max(100, "업종은 100자 이하여야 합니다"),
  description: z.string().trim().max(2000, "설명은 2000자 이하여야 합니다"),
});

type CompanyFormValues = z.infer<typeof companySchema>;

// dark chrome 배경용 label+input — TextField와 마크업은 같지만 색만 chrome 토큰을 쓴다.
function ChromeField({
  id,
  label,
  hint,
  ...props
}: React.InputHTMLAttributes<HTMLInputElement> & { id: string; label: string; hint?: string }) {
  return (
    <div className="group flex flex-col gap-1.5">
      <label
        htmlFor={id}
        className="text-sm font-medium text-chrome-foreground transition-colors duration-fast group-focus-within:text-primary"
      >
        {label}
      </label>
      <input id={id} className={INPUT_BASE} {...props} />
      {hint && <p className="text-xs text-chrome-muted">{hint}</p>}
    </div>
  );
}

export function CreateCompanyForm() {
  const router = useRouter();
  const { refreshCompanyMemberships } = useAuth();
  const [error, setError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<CompanyFormValues>({
    resolver: zodResolver(companySchema),
    defaultValues: { name: "", representativeName: "", industryCategory: "", description: "" },
  });

  const onSubmit = async (values: CompanyFormValues) => {
    setError(null);
    let companyId: number;
    try {
      const { data } = await companyService.create({
        name: values.name,
        representativeName: values.representativeName === "" ? null : values.representativeName,
        industryCategory: values.industryCategory === "" ? null : values.industryCategory,
        description: values.description === "" ? null : values.description,
        logoUrl: null,
      });
      companyId = data.data.company.id;
    } catch (err) {
      setError(invitationErrorMessage(err, "회사 생성에 실패했습니다."));
      return;
    }

    try {
      await refreshCompanyMemberships();
    } catch {
      // 생성 API 자체는 이미 성공했다 — 세션 companyMemberships 재조회만 실패한
      // 것이므로 "생성 실패"로 보이면 안 된다(PR #171 리뷰 반영, invitations
      // page의 동일 패턴 참고).
      setError("회사는 생성됐지만 소속 반영에 실패했습니다. 새로고침 후 다시 확인해주세요.");
      return;
    }
    router.push(`/${companyId}/projects`);
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="mt-5 flex flex-col gap-4 text-left" noValidate>
      <ChromeField
        id="create-company-name"
        label="회사명"
        placeholder="Threadia"
        hint={errors.name?.message}
        {...register("name")}
      />
      <ChromeField
        id="create-company-representative"
        label="대표자명 (선택)"
        hint={errors.representativeName?.message}
        {...register("representativeName")}
      />
      <ChromeField
        id="create-company-industry"
        label="업종 (선택)"
        hint={errors.industryCategory?.message}
        {...register("industryCategory")}
      />
      <div className="group flex flex-col gap-1.5">
        <label
          htmlFor="create-company-description"
          className="text-sm font-medium text-chrome-foreground transition-colors duration-fast group-focus-within:text-primary"
        >
          설명 (선택)
        </label>
        <textarea
          id="create-company-description"
          rows={3}
          className={INPUT_BASE + " resize-none"}
          {...register("description")}
        />
        {errors.description && <p className="text-xs text-destructive">{errors.description.message}</p>}
      </div>

      {error && <p className="text-sm text-destructive">{error}</p>}

      <Button type="submit" loading={isSubmitting} className="w-full">
        회사 생성
      </Button>
    </form>
  );
}
