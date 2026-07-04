"use client";

/*
 * S04 — 프로젝트 목록
 *
 * 접근 사용자: A/P/C. 화면정의서(docs/03-design/화면정의서.md §S04) 기준
 * 프로젝트 카드 목록(이름/상태/PM/멤버 수/이슈 수) + 새 프로젝트 생성(C만) +
 * 카드 클릭 시 S05 칸반 보드로 이동.
 *
 * 설계 메모 반영:
 *   - A는 "+ 새 프로젝트" 버튼을 보지 않는다.
 *   - C는 회사 전체 프로젝트, A/P는 소속 프로젝트만 보는 게 원칙이지만
 *     실제 필터링은 백엔드 `GET /companies/{companyId}/projects` 응답이
 *     이미 역할별로 걸러서 내려주므로 프론트는 받은 목록을 그대로 렌더링하면 된다.
 *     지금은 mock이라 별도 필터링 없이 전체를 보여준다.
 *
 * ⚠️ MOCK: 프로젝트 API(`GET/POST /companies/{companyId}/projects`)가 아직
 * 없어서 로컬 state로 흉내낸다. HubOverlay와 동일하게 role은 상수로 고정.
 */
import { useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";

type Role = "A" | "P" | "C";
type ProjectStatus = "PLANNING" | "ACTIVE" | "PAUSED" | "COMPLETED" | "ARCHIVED";

type Project = {
  id: string;
  name: string;
  projectKey: string;
  status: ProjectStatus;
  pmName: string;
  memberCount: number;
  issueCount: number;
};

// TODO(Sprint 2): /api/v1/auth/me 연동 후 실제 로그인 사용자 role로 교체 (HubOverlay와 동일)
const MOCK_ROLE: Role = "C";

const STATUS_LABEL: Record<ProjectStatus, string> = {
  PLANNING: "기획중",
  ACTIVE: "진행중",
  PAUSED: "일시중지",
  COMPLETED: "완료",
  ARCHIVED: "보관됨",
};

const STATUS_CLASS: Record<ProjectStatus, string> = {
  PLANNING: "bg-accent-subtle text-accent",
  ACTIVE: "bg-success/15 text-success",
  PAUSED: "bg-warning/15 text-warning",
  COMPLETED: "bg-border text-muted",
  ARCHIVED: "bg-border text-muted",
};

const MOCK_PROJECTS: Project[] = [
  { id: "p1", name: "Threadia MVP", projectKey: "THR", status: "ACTIVE", pmName: "정철진", memberCount: 6, issueCount: 42 },
  { id: "p2", name: "사내 디자인 시스템", projectKey: "DSY", status: "ACTIVE", pmName: "김민정", memberCount: 3, issueCount: 18 },
  { id: "p3", name: "온보딩 자동화", projectKey: "ONB", status: "PLANNING", pmName: "여도현", memberCount: 4, issueCount: 5 },
  { id: "p4", name: "레거시 정리", projectKey: "LGC", status: "PAUSED", pmName: "황스일", memberCount: 2, issueCount: 11 },
  { id: "p5", name: "2025 회고", projectKey: "RTR", status: "COMPLETED", pmName: "정철진", memberCount: 6, issueCount: 30 },
];

export default function ProjectsPage() {
  const params = useParams<{ companyId: string }>();
  const companyId = params.companyId;

  const [projects, setProjects] = useState<Project[]>(MOCK_PROJECTS);
  const [statusFilter, setStatusFilter] = useState<ProjectStatus | "ALL">("ALL");
  const [creating, setCreating] = useState(false);
  const [newName, setNewName] = useState("");
  const [newKey, setNewKey] = useState("");

  const visibleProjects = projects.filter(
    (p) => statusFilter === "ALL" || p.status === statusFilter
  );

  const createProject = () => {
    const name = newName.trim();
    const key = newKey.trim().toUpperCase();
    if (!name || !key) return;
    // TODO: POST /api/v1/companies/{companyId}/projects
    setProjects((prev) => [
      ...prev,
      {
        id: crypto.randomUUID(),
        name,
        projectKey: key,
        status: "PLANNING",
        pmName: "정철진",
        memberCount: 1,
        issueCount: 0,
      },
    ]);
    setNewName("");
    setNewKey("");
    setCreating(false);
  };

  return (
    <div className="mx-auto max-w-4xl px-6 py-10">
      <Link href="/hub" className="text-sm text-muted transition-colors duration-fast hover:text-primary">
        ← 허브로
      </Link>
      <div className="mt-3 flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-foreground">프로젝트</h1>
          <p className="mt-1 text-sm text-muted">
            {MOCK_ROLE === "C" ? "회사 전체 프로젝트입니다." : "소속된 프로젝트입니다."}
          </p>
        </div>
        {/* 화면정의서: A는 새 프로젝트 버튼을 보지 않는다 */}
        {MOCK_ROLE !== "A" && (
          <button
            onClick={() => setCreating((v) => !v)}
            className="rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground transition-colors duration-fast hover:bg-primary-hover"
          >
            + 새 프로젝트
          </button>
        )}
      </div>

      {creating && (
        <section className="mt-4 rounded-xl border border-border bg-surface p-5 shadow-sm">
          <h2 className="text-sm font-semibold text-foreground">새 프로젝트</h2>
          <div className="mt-3 flex flex-wrap gap-2">
            <input
              aria-label="프로젝트 이름"
              value={newName}
              onChange={(e) => setNewName(e.target.value)}
              placeholder="프로젝트 이름"
              className="flex-1 rounded-lg border border-border bg-surface px-3 py-2 text-sm text-foreground placeholder:text-placeholder focus:border-ring focus:outline-none"
            />
            <input
              aria-label="프로젝트 키"
              value={newKey}
              onChange={(e) => setNewKey(e.target.value)}
              placeholder="키 (예: THR)"
              maxLength={10}
              className="w-32 rounded-lg border border-border bg-surface px-3 py-2 text-sm uppercase text-foreground placeholder:text-placeholder focus:border-ring focus:outline-none"
            />
            <button
              onClick={createProject}
              className="rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground transition-colors duration-fast hover:bg-primary-hover"
            >
              생성
            </button>
            <button
              onClick={() => setCreating(false)}
              className="rounded-lg px-3 py-2 text-sm font-medium text-muted transition-colors duration-fast hover:bg-primary-subtle"
            >
              취소
            </button>
          </div>
        </section>
      )}

      {/* 상태 필터 */}
      <div className="mt-6 flex flex-wrap gap-2">
        {(["ALL", "PLANNING", "ACTIVE", "PAUSED", "COMPLETED", "ARCHIVED"] as const).map((status) => (
          <button
            key={status}
            onClick={() => setStatusFilter(status)}
            className={
              "rounded-full border px-3 py-1 text-xs font-medium transition-colors duration-fast " +
              (statusFilter === status
                ? "border-primary bg-primary-subtle text-primary"
                : "border-border bg-surface text-muted hover:bg-primary-subtle")
            }
          >
            {status === "ALL" ? "전체" : STATUS_LABEL[status]}
          </button>
        ))}
      </div>

      {/* 프로젝트 카드 목록 */}
      <div className="mt-4 grid grid-cols-1 gap-3 sm:grid-cols-2">
        {visibleProjects.map((project) => (
          <Link
            key={project.id}
            href={`/${companyId}/${project.id}/board`}
            className="flex flex-col gap-3 rounded-xl border border-border bg-surface p-5 shadow-sm transition-colors duration-fast hover:border-primary hover:bg-primary-subtle"
          >
            <div className="flex items-start justify-between">
              <div>
                <span className="text-xs font-mono text-muted">{project.projectKey}</span>
                <h3 className="text-sm font-semibold text-foreground">{project.name}</h3>
              </div>
              <span className={`rounded-full px-2.5 py-0.5 text-xs font-medium ${STATUS_CLASS[project.status]}`}>
                {STATUS_LABEL[project.status]}
              </span>
            </div>
            <div className="flex items-center gap-4 text-xs text-muted">
              <span>PM {project.pmName}</span>
              <span>멤버 {project.memberCount}명</span>
              <span>이슈 {project.issueCount}개</span>
            </div>
          </Link>
        ))}
        {visibleProjects.length === 0 && (
          <div className="col-span-full rounded-xl border border-dashed border-border px-4 py-10 text-center text-sm text-muted">
            해당 상태의 프로젝트가 없습니다.
          </div>
        )}
      </div>
    </div>
  );
}
