import { notFound } from "next/navigation";
import DocsShell from "@/components/DocsShell";
import MarkdownRenderer from "@/components/MarkdownRenderer";
import { getAllDocPages, getDocBySlug } from "@/lib/docs";

export function generateStaticParams() {
  return getAllDocPages().map((doc) => ({ slug: doc.slug }));
}

export default async function DocPage({
  params,
}: {
  params: Promise<{ slug: string[] }>;
}) {
  const { slug } = await params;
  // Next.js catch-all([...slug]) 세그먼트는 단일 동적 세그먼트([id])와 달리
  // 자동으로 URL 디코딩되지 않는다. 직접 디코딩해야 한글 등 비-ASCII 슬러그가 매칭된다.
  const decodedSlug = slug.map((s) => decodeURIComponent(s));
  const doc = getDocBySlug(decodedSlug);
  if (!doc) notFound();

  return (
    <DocsShell>
      <article>
        <p className="doc-meta">
          {doc.author && <span>작성자: {doc.author}</span>}
          {doc.status && <span> · 상태: {doc.status}</span>}
        </p>
        {doc.tags.length > 0 && (
          <p className="doc-tags">
            {doc.tags.map((tag) => (
              <span key={tag} className="tag-chip">
                {tag}
              </span>
            ))}
          </p>
        )}
        <MarkdownRenderer content={doc.body} />
      </article>
    </DocsShell>
  );
}
