import Link from "next/link";
import Sidebar from "./Sidebar";
import SearchBox from "./SearchBox";

export default function DocsShell({ children }: { children: React.ReactNode }) {
  return (
    <div className="docs-layout">
      <header className="docs-header">
        <Link href="/" className="docs-header-title">
          Threadia Docs
        </Link>
        <SearchBox />
      </header>
      <div className="docs-body">
        <Sidebar />
        <main className="docs-content">{children}</main>
      </div>
    </div>
  );
}
