"use client";

/*
 * (app)/[companyId]/[projectId] 공통 레이아웃 — 프로젝트 하위 화면(보드/백로그/
 * 문서/채팅/설정) 전환용 탭 내비게이션을 제공한다.
 *
 * ⚠️ 이전에는 이 파일이 빈 통과 래퍼였다 — 프로젝트 카드가 `/board`로만
 * 연결되고, 그 안에서 backlog/docs/messages/settings로 이동할 방법이 화면
 * 자체엔 없었다(문서↔이슈 상호 링크 등 일부 화면간 개별 링크만 존재).
 * RailShell(전역 chrome)은 회사 단위 내비게이션만 제공해서 이 공백을
 * 못 메운다 — 프로젝트 스코프 탭은 이 레이아웃에서 별도로 제공해야 한다.
 *
 * threads/[threadId](S10)는 구현이 끝난 화면이지만 탭에 넣지 않는다 —
 * 화면정의서(§S10) 기준 "연결 화면"이 S05/S08/S09/S11이라, 독립 탭이 아니라
 * 이슈 카드·문서·DM 등에서 컨텍스트를 들고 진입하는 구조로 설계됐다(진입
 * 경로: `threads/open` 리다이렉트, `IssueDetailModal` 등). 프로젝트 단위
 * notifications(S11)는 실제로 아직 내용이 없는 스텁이라(실제 컨텐츠 없이
 * 자리만 있음) 탭에 넣지 않았다 — 화면이 채워지면 탭에 추가한다.
 */
import Link from "next/link";
import { usePathname, useParams } from "next/navigation";

const TABS = [
  { key: "dashboard", label: "홈" },
  { key: "graph", label: "그래프" },
  { key: "board", label: "보드" },
  { key: "backlog", label: "백로그" },
  { key: "docs", label: "문서" },
  { key: "messages", label: "채팅" },
  { key: "search", label: "검색" },
  { key: "settings", label: "설정" },
];

export default function ProjectLayout({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const { companyId, projectId } = useParams<{ companyId: string; projectId: string }>();

  return (
    <div className="flex h-full flex-col">
      <nav className="sticky top-0 z-sticky flex gap-1 border-b border-border bg-background/95 px-6 pt-3 backdrop-blur">
        {TABS.map((tab) => {
          const href = `/${companyId}/${projectId}/${tab.key}`;
          const active = pathname === href || pathname.startsWith(href + "/");
          return (
            <Link
              key={tab.key}
              href={href}
              className={
                "rounded-t-lg px-3 py-2 text-sm font-medium transition-colors duration-fast " +
                (active
                  ? "border-b-2 border-primary text-primary"
                  : "border-b-2 border-transparent text-muted hover:text-foreground")
              }
            >
              {tab.label}
            </Link>
          );
        })}
      </nav>
      <div className="min-h-0 flex-1">{children}</div>
    </div>
  );
}
