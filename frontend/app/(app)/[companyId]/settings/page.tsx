"use client";

/*
 * S13 — 회사 관리
 *
 * 접근 사용자: C(COMPANY_ADMIN)만. 화면정의서(docs/03-design/화면정의서.md §S13) 기준
 * "관리 모드"만 구현한다 — 회사 정보 카드(수정 가능) + 통계 + 부서 목록(CRUD) +
 * "구성원 관리" 버튼(S14 이동).
 *
 * TODO(Sprint 2, 낮은 우선순위): "생성 모드"(회사가 아예 없을 때 POST /companies로
 * 최초 생성하는 폼)는 아직 미구현. 화면정의서도 이걸 "MVP/Sprint 2 reference"로만
 * 표시하고 Sprint 1.5 invitation 검증엔 필요 없다고 명시해서 우선순위를 낮췄다.
 *
 * ⚠️ MOCK: 백엔드 회사/부서 API(`POST/GET/PATCH /companies`, `/departments`)가
 * 아직 없어서 전부 로컬 state로 흉내낸다. 실제 연동 시 이 파일의 MOCK_* 상수와
 * 로컬 state 갱신 로직을 API 호출로 교체하면 된다.
 */
import { useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { TextField } from "@/components/ui/TextField";

type CompanyInfo = {
  name: string;
  representativeName: string;
  description: string;
  industryCategory: string;
};

type Department = { id: string; name: string; memberCount: number };

type ProjectStatus = "PLANNING" | "ACTIVE" | "PAUSED" | "COMPLETED" | "ARCHIVED";

const STATUS_LABEL: Record<ProjectStatus, string> = {
  PLANNING: "기획중",
  ACTIVE: "진행중",
  PAUSED: "일시중지",
  COMPLETED: "완료",
  ARCHIVED: "보관됨",
};

const MOCK_COMPANY: CompanyInfo = {
  name: "Acorn Labs",
  representativeName: "정철진",
  description: "협업도구 스타트업",
  industryCategory: "IT/소프트웨어",
};

const MOCK_STATS_BY_STATUS: Record<ProjectStatus, number> = {
  PLANNING: 2,
  ACTIVE: 4,
  PAUSED: 1,
  COMPLETED: 1,
  ARCHIVED: 0,
};

const MOCK_MEMBER_COUNT = 25;

const MOCK_DEPARTMENTS: Department[] = [
  { id: "d1", name: "개발팀", memberCount: 12 },
  { id: "d2", name: "디자인팀", memberCount: 5 },
  { id: "d3", name: "기획팀", memberCount: 8 },
];

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

  const [company, setCompany] = useState<CompanyInfo>(MOCK_COMPANY);
  const [draft, setDraft] = useState<CompanyInfo>(MOCK_COMPANY);
  const [editing, setEditing] = useState(false);

  const [departments, setDepartments] = useState<Department[]>(MOCK_DEPARTMENTS);
  const [newDeptName, setNewDeptName] = useState("");

  const totalProjects = Object.values(MOCK_STATS_BY_STATUS).reduce((a, b) => a + b, 0);

  const startEdit = () => {
    setDraft(company);
    setEditing(true);
  };

  const saveEdit = () => {
    // TODO: PATCH /api/v1/companies/{companyId}
    setCompany(draft);
    setEditing(false);
  };

  const addDepartment = () => {
    const name = newDeptName.trim();
    if (!name) return;
    // TODO: POST /api/v1/companies/{companyId}/departments
    setDepartments((prev) => [...prev, { id: crypto.randomUUID(), name, memberCount: 0 }]);
    setNewDeptName("");
  };

  const removeDepartment = (id: string) => {
    // TODO: DELETE /api/v1/departments/{departmentId} — 소속 멤버는 department_id=NULL 처리
    setDepartments((prev) => prev.filter((d) => d.id !== id));
  };

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

        {editing ? (
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
                className="rounded-md btn-gradient px-3 py-1.5 text-sm font-semibold text-primary-foreground transition-colors duration-fast"
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
              <dd className="mt-0.5 font-medium text-foreground">{company.representativeName}</dd>
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
          <StatCard label="구성원" value={MOCK_MEMBER_COUNT} />
          <StatCard label="부서" value={departments.length} />
          <StatCard label="진행중 프로젝트" value={MOCK_STATS_BY_STATUS.ACTIVE} />
        </div>
        <div className="mt-3 flex flex-wrap gap-2">
          {(Object.keys(MOCK_STATS_BY_STATUS) as ProjectStatus[]).map((status) => (
            <span
              key={status}
              className="rounded-full border border-border bg-surface px-3 py-1 text-xs text-muted"
            >
              {STATUS_LABEL[status]} {MOCK_STATS_BY_STATUS[status]}
            </span>
          ))}
        </div>
      </section>

      {/* 부서 목록 */}
      <section className="mt-6 rounded-xl border border-border bg-surface p-6">
        <h2 className="text-sm font-semibold text-foreground">부서</h2>
        <ul className="mt-3 divide-y divide-border">
          {departments.map((dept) => (
            <li
              key={dept.id}
              className="flex items-center justify-between px-1 py-2.5"
            >
              <span className="text-sm font-medium text-foreground">{dept.name}</span>
              <div className="flex items-center gap-3">
                <span className="text-xs text-muted">{dept.memberCount}명</span>
                <button
                  onClick={() => removeDepartment(dept.id)}
                  className="text-xs font-medium text-destructive transition-colors duration-fast hover:opacity-70"
                >
                  삭제
                </button>
              </div>
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
            className="rounded-lg btn-gradient px-4 py-2 text-sm font-semibold text-primary-foreground transition-colors duration-fast"
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
