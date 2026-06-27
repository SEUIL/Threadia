import DocsShell from "@/components/DocsShell";
import { listDocs } from "@/lib/docs";

export default function Home() {
  const docs = listDocs();

  return (
    <DocsShell>
      <h1>Threadia Docs</h1>
      <p>
        Threadia 프로젝트 canonical 문서를 한 곳에서 보는 사이트다. 좌측
        사이드바에서 문서를 선택하면 된다. (<code>01-planning</code>,{" "}
        <code>08-members</code>는 포함하지 않는다.)
      </p>
      <p>현재 {docs.length}개 문서가 연동되어 있다.</p>
    </DocsShell>
  );
}
