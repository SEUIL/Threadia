import Link from "next/link";
import { listDocs } from "@/lib/docs";

const SECTION_LABELS: Record<string, string> = {
  "00-project": "00. 프로젝트 공통",
  "02-requirements": "02. 요구사항",
  "03-design": "03. 설계",
  "04-api": "04. API",
  "05-architecture": "05. 아키텍처",
  "06-meetings": "06. 회의록",
  "07-decisions": "07. 결정",
};

export default function Sidebar() {
  const docs = listDocs();

  const sections = new Map<string, typeof docs>();
  for (const doc of docs) {
    const list = sections.get(doc.section) ?? [];
    list.push(doc);
    sections.set(doc.section, list);
  }

  const orderedSectionIds = Object.keys(SECTION_LABELS).filter((id) =>
    sections.has(id)
  );

  return (
    <nav className="sidebar">
      <p className="sidebar-title">Threadia Docs</p>
      {orderedSectionIds.map((sectionId) => (
        <div key={sectionId} className="sidebar-section">
          <p className="sidebar-section-label">{SECTION_LABELS[sectionId]}</p>
          <ul className="sidebar-list">
            {sections.get(sectionId)!.map((doc) => (
              <li key={doc.relativePath}>
                <Link
                  href={`/${doc.slug.map(encodeURIComponent).join("/")}`}
                  className="sidebar-item"
                >
                  {doc.title}
                </Link>
              </li>
            ))}
          </ul>
        </div>
      ))}
    </nav>
  );
}
