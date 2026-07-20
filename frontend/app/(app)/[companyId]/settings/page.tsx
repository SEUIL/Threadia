"use client";

/*
 * S13 — 회사 관리
 *
 * 접근 사용자: C(COMPANY_ADMIN)만. 화면정의서(docs/03-design/화면정의서.md §S13) 기준
 * "관리 모드"만 구현한다 — 회사 정보 카드(수정 가능) + 통계 + 부서 목록(CRUD) +
 * "구성원 관리" 버튼(S14 이동).
 *
 * TODO(Sprint 2, 낮은 우선순위): "생성 모드"(회사가 아예 없을 때 POST /companies로
 * 최초 생성하는 폼)는 아직 미구현. 설계문서(sprint2-3.2-company-frontend-rbac-
 * 사전조사.md §4, §9)에서 스코프 미확정으로 보류했다.
 *
 * WBS 3.2.2 실 연동: company.service/department.service 사용. 이 화면 진입은
 * RailShell이 COMPANY_ADMIN에게만 "회사 관리" 메뉴를 보여주는 것으로만
 * 막혀 있고(Phase 3), URL 직접 접근 자체를 막는 라우트 가드는 없다 —
 * MEMBER가 직접 들어오면 회사 정보(GET)는 보이고 통계/부서(admin 전용)는
 * 403으로 실패해 에러 화면으로 처리된다. 실제 데이터 유출은 없다(서버가
 * 최종 방어).
 *
 * 로고: `POST /{companyId}/logo`(API-명세서 §5)는 파일 업로드가 아니라
 * "로고 URL 저장" 전용 endpoint다(백엔드에 별도 파일 스토리지가 없음,
 * `CompanyLogoRequest`가 `@URL` 문자열만 받는다) — 코드-WBS 감사 중 이 endpoint가
 * 프론트에서 전혀 안 쓰이고 있는 걸 발견해 여기 연결한다. `update()`(전체
 * PATCH)와 별개 액션이라 이름/설명 저장 폼과 분리된 자체 입력·저장을 둔다.
 */
import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { TextField } from "@/components/ui/TextField";
import { companyService } from "@/services/company.service";
import { departmentService } from "@/services/department.service";
import { invitationErrorMessage } from "@/lib/invitation";
import type { CompanyResponse, CompanyStatsData, DepartmentResponse } from "@/types/company.types";

type CompanyDraft = {
  name: string;
  representativeName: string;
  description: string;
  industryCategory: string;
};

type ProjectStatus = "PLANNING" | "ACTIVE" | "PAUSED" | "COMPLETED" | "ARCHIVED";

const STATUS_LABEL: Record<ProjectStatus, string> = {
  PLANNING: "기획중",
  ACTIVE: "진행중",
  PAUSED: "일시중지",
  COMPLETED: "완료",
  ARCHIVED: "보관됨",
};

type LoadState = "loading" | "ready" | "error";

function toDraft(company: CompanyResponse): CompanyDraft {
  return {
    name: company.name,
    representativeName: company.representativeName ?? "",
    description: company.description ?? "",
    industryCategory: company.industryCategory ?? "",
  };
}

function StatCard({ label, value }: { label: string; value: number }) {
  return (
    <div className="rounded-lg border border-border bg-surface p-4">
      <p className="text-xs text-muted">{label}</p>
      <p className="mt-1 text-2xl font-bold text-foreground">{value}</p>
    </div>
  );
}

export default function CompanySettingsPage() {
  const params = useParams<{ companyId: string }>();
  const companyId = params.companyId;

  const [pageState, setPageState] = useState<LoadState>("loading");
  const [pageError, setPageError] = useState<string | null>(null);

  const [company, setCompany] = useState<CompanyResponse | null>(null);
  const [draft, setDraft] = useState<CompanyDraft | null>(null);
  const [editing, setEditing] = useState(false);
  const [editBusy, setEditBusy] = useState(false);
  const [editError, setEditError] = useState<string | null>(null);

  const [logoUrlDraft, setLogoUrlDraft] = useState("");
  const [logoBusy, setLogoBusy] = useState(false);
  const [logoError, setLogoError] = useState<string | null>(null);
  const [brokenLogoUrl, setBrokenLogoUrl] = useState<string | null>(null);

  const [stats, setStats] = useState<CompanyStatsData | null>(null);

  const [departments, setDepartments] = useState<DepartmentResponse[]>([]);
  const [newDeptName, setNewDeptName] = useState("");
  const [deptBusy, setDeptBusy] = useState(false);
  const [deptError, setDeptError] = useState<string | null>(null);

  const loadPage = useCallback(async () => {
    setPageState("loading");
    setPageError(null);
    try {
      const [companyRes, statsRes, departmentsRes] = await Promise.all([
        companyService.get(companyId),
        companyService.stats(companyId),
        departmentService.list(companyId),
      ]);
      setCompany(companyRes.data.data.company);
      setLogoUrlDraft(companyRes.data.data.company.logoUrl ?? "");
      setStats(statsRes.data.data);
      setDepartments(departmentsRes.data.data.departments);
      setPageState("ready");
    } catch (error) {
      setPageError(invitationErrorMessage(error, "회사 정보를 불러오지 못했습니다."));
      setPageState("error");
    }
  }, [companyId]);

  useEffect(() => {
    (async () => {
      await loadPage();
    })();
  }, [loadPage]);

  const startEdit = () => {
    if (!company) return;
    setDraft(toDraft(company));
    setEditError(null);
    setEditing(true);
  };

  const saveEdit = async () => {
    if (!draft) return;
    setEditBusy(true);
    setEditError(null);
    try {
      const { data } = await companyService.update(companyId, {
        name: draft.name,
        representativeName: draft.representativeName || null,
        description: draft.description || null,
        industryCategory: draft.industryCategory || null,
      });
      setCompany(data.data.company);
      setEditing(false);
    } catch (error) {
      setEditError(invitationErrorMessage(error, "회사 정보 수정에 실패했습니다."));
    } finally {
      setEditBusy(false);
    }
  };

  const saveLogo = async () => {
    const logoUrl = logoUrlDraft.trim();
    if (!logoUrl) return;
    setLogoBusy(true);
    setLogoError(null);
    try {
      const { data } = await companyService.updateLogo(companyId, { logoUrl });
      setCompany(data.data.company);
      setBrokenLogoUrl(null);
    } catch (error) {
      setLogoError(invitationErrorMessage(error, "로고 저장에 실패했습니다."));
    } finally {
      setLogoBusy(false);
    }
  };

  const addDepartment = async () => {
    const name = newDeptName.trim();
    if (!name) return;
    setDeptBusy(true);
    setDeptError(null);
    try {
      const { data } = await departmentService.create(companyId, { name });
      setDepartments((prev) => [...prev, data.data.department]);
      setNewDeptName("");
    } catch (error) {
      setDeptError(invitationErrorMessage(error, "부서 추가에 실패했습니다."));
    } finally {
      setDeptBusy(false);
    }
  };

  const removeDepartment = async (departmentId: number) => {
    setDeptError(null);
    try {
      await departmentService.remove(departmentId);
      setDepartments((prev) => prev.filter((d) => d.id !== departmentId));
    } catch (error) {
      setDeptError(invitationErrorMessage(error, "부서 삭제에 실패했습니다."));
    }
  };

  if (pageState === "loading") {
    return <div className="mx-auto max-w-3xl px-6 py-10 text-sm text-muted">불러오는 중...</div>;
  }

  if (pageState === "error" || !company || !stats) {
    return (
      <div className="mx-auto max-w-3xl px-6 py-10">
        <Link href="/hub" className="text-sm text-muted transition-colors duration-fast hover:text-primary">
          ← 허브로
        </Link>
        <div className="mt-6 rounded-xl border border-destructive-subtle bg-surface p-6 text-sm text-destructive">
          {pageError}
        </div>
      </div>
    );
  }

  const totalProjects = stats.projectCount;

  return (
    <div className="mx-auto max-w-3xl px-6 py-10">
      <Link href="/hub" className="text-sm text-muted transition-colors duration-fast hover:text-primary">
        ← 허브로
      </Link>
      <h1 className="mt-3 text-xl font-bold text-foreground">회사 관리</h1>
      <p className="mt-1 text-sm text-muted">{company.name} · 회사 정보, 부서, 프로젝트 현황을 관리합니다.</p>

      {/* 회사 정보 카드 */}
      <section className="mt-6 rounded-xl border border-border bg-surface p-6">
        <div className="flex items-center justify-between">
          <h2 className="text-sm font-semibold text-foreground">회사 정보</h2>
          {!editing && (
            <button
              onClick={startEdit}
              className="rounded-md px-3 py-1.5 text-sm font-medium text-primary transition-colors duration-fast hover:bg-primary-subtle"
            >
              수정
            </button>
          )}
        </div>

        {editError && <p className="mt-2 text-xs text-destructive">{editError}</p>}

        {/* 로고 — 이름/설명 저장 폼과 별개 endpoint(POST /{companyId}/logo)라 저장 버튼도 분리한다. */}
        <div className="mt-4 flex items-center gap-3">
          {logoUrlDraft && logoUrlDraft !== brokenLogoUrl ? (
            // eslint-disable-next-line @next/next/no-img-element -- 임의 외부 호스트를 그대로 허용해야 해서 next/image remotePatterns 설정 범위 밖(UserAvatar와 동일 판단)
            <img
              src={logoUrlDraft}
              alt="회사 로고"
              onError={() => setBrokenLogoUrl(logoUrlDraft)}
              className="h-12 w-12 shrink-0 rounded-lg border border-border object-cover"
            />
          ) : (
            <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-lg border border-dashed border-border text-xs text-muted">
              로고
            </div>
          )}
          <div className="flex flex-1 items-center gap-2">
            <input
              aria-label="회사 로고 URL"
              value={logoUrlDraft}
              onChange={(e) => setLogoUrlDraft(e.target.value)}
              placeholder="https://example.com/logo.png"
              className="flex-1 rounded-lg border border-border bg-surface px-3 py-2 text-sm text-foreground placeholder:text-placeholder focus:border-ring focus:outline-none"
            />
            <button
              onClick={saveLogo}
              disabled={logoBusy || !logoUrlDraft.trim()}
              className="shrink-0 rounded-lg btn-gradient px-3 py-2 text-xs font-semibold text-primary-foreground transition-colors duration-fast disabled:cursor-not-allowed disabled:opacity-50"
            >
              {logoBusy ? "저장 중..." : "로고 저장"}
            </button>
          </div>
        </div>
        {logoError && <p className="mt-2 text-xs text-destructive">{logoError}</p>}

        {editing && draft ? (
          <div className="mt-4 flex flex-col gap-4">
            <TextField
              id="company-name"
              label="회사명"
              value={draft.name}
              onChange={(e) => setDraft({ ...draft, name: e.target.value })}
            />
            <TextField
              id="company-representative"
              label="대표자"
              value={draft.representativeName}
              onChange={(e) => setDraft({ ...draft, representativeName: e.target.value })}
            />
            <TextField
              id="company-description"
              label="회사 소개"
              value={draft.description}
              onChange={(e) => setDraft({ ...draft, description: e.target.value })}
            />
            <TextField
              id="company-industry"
              label="업종/카테고리"
              value={draft.industryCategory}
              onChange={(e) => setDraft({ ...draft, industryCategory: e.target.value })}
            />
            <div className="flex justify-end gap-2">
              <button
                onClick={() => setEditing(false)}
                className="rounded-md px-3 py-1.5 text-sm font-medium text-muted transition-colors duration-fast hover:bg-primary-subtle"
              >
                취소
              </button>
              <button
                onClick={saveEdit}
                disabled={editBusy}
                className="rounded-md btn-gradient px-3 py-1.5 text-sm font-semibold text-primary-foreground transition-colors duration-fast disabled:opacity-50"
              >
                저장
              </button>
            </div>
          </div>
        ) : (
          <dl className="mt-4 grid grid-cols-2 gap-4 text-sm">
            <div>
              <dt className="text-muted">회사명</dt>
              <dd className="mt-0.5 font-medium text-foreground">{company.name}</dd>
            </div>
            <div>
              <dt className="text-muted">대표자</dt>
              <dd className="mt-0.5 font-medium text-foreground">{company.representativeName || "-"}</dd>
            </div>
            <div className="col-span-2">
              <dt className="text-muted">회사 소개</dt>
              <dd className="mt-0.5 font-medium text-foreground">{company.description || "-"}</dd>
            </div>
            <div>
              <dt className="text-muted">업종/카테고리</dt>
              <dd className="mt-0.5 font-medium text-foreground">{company.industryCategory || "-"}</dd>
            </div>
          </dl>
        )}
      </section>

      {/* 통계 */}
      <section className="mt-6">
        <h2 className="text-sm font-semibold text-foreground">회사 현황</h2>
        <div className="mt-3 grid grid-cols-2 gap-3 sm:grid-cols-4">
          <StatCard label="전체 프로젝트" value={totalProjects} />
          <StatCard label="구성원" value={stats.memberCount} />
          <StatCard label="부서" value={stats.departmentCount} />
          <StatCard label="진행중 프로젝트" value={stats.projectStatusCounts.ACTIVE ?? 0} />
        </div>
        <div className="mt-3 flex flex-wrap gap-2">
          {(Object.keys(STATUS_LABEL) as ProjectStatus[]).map((status) => (
            <span
              key={status}
              className="rounded-full border border-border bg-surface px-3 py-1 text-xs text-muted"
            >
              {STATUS_LABEL[status]} {stats.projectStatusCounts[status] ?? 0}
            </span>
          ))}
        </div>
      </section>

      {/* 부서 목록 */}
      <section className="mt-6 rounded-xl border border-border bg-surface p-6">
        <h2 className="text-sm font-semibold text-foreground">부서</h2>
        {deptError && <p className="mt-2 text-xs text-destructive">{deptError}</p>}
        <ul className="mt-3 divide-y divide-border">
          {departments.map((dept) => (
            <li
              key={dept.id}
              className="flex items-center justify-between px-1 py-2.5"
            >
              <span className="text-sm font-medium text-foreground">{dept.name}</span>
              <button
                onClick={() => removeDepartment(dept.id)}
                className="text-xs font-medium text-destructive transition-colors duration-fast hover:opacity-70"
              >
                삭제
              </button>
            </li>
          ))}
          {departments.length === 0 && (
            <li className="rounded-lg border border-dashed border-border px-3 py-4 text-center text-sm text-muted">
              등록된 부서가 없습니다.
            </li>
          )}
        </ul>

        <div className="mt-3 flex gap-2">
          <input
            aria-label="새 부서 이름"
            value={newDeptName}
            onChange={(e) => setNewDeptName(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && addDepartment()}
            placeholder="새 부서 이름"
            className="flex-1 rounded-lg border border-border bg-surface px-3 py-2 text-sm text-foreground placeholder:text-placeholder focus:border-ring focus:outline-none"
          />
          <button
            onClick={addDepartment}
            disabled={deptBusy}
            className="rounded-lg btn-gradient px-4 py-2 text-sm font-semibold text-primary-foreground transition-colors duration-fast disabled:opacity-50"
          >
            추가
          </button>
        </div>
      </section>

      {/* 구성원 관리 이동 — 화면정의서: S13→S14는 자동 전환이 아니라 버튼으로 이동 */}
      <div className="mt-6 flex justify-end">
        <Link
          href={`/${companyId}/members`}
          className="rounded-lg btn-gradient px-4 py-2.5 text-sm font-semibold text-primary-foreground transition-colors duration-fast"
        >
          구성원 관리로 이동
        </Link>
      </div>
    </div>
  );
}
