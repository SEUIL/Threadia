import { Document } from "flexsearch";
import { getAllDocPages } from "./docs";
import { toBigramText } from "./bigram";

export type SearchResult = {
  slug: string[];
  title: string;
  tags: string[];
  section: string;
};

type IndexedDoc = SearchResult & { id: number; bodyBigram: string };

// flexsearch의 TypeScript 타입 정의가 Document 옵션을 엄격하게 제한해 실제 런타임에서
// 동작하는 단순 필드 배열 설정과 맞지 않는다. 런타임 동작에는 영향 없이 타입만 느슨하게 풀어 쓴다.
let cachedIndex: InstanceType<typeof Document> | null = null;
let cachedDocs: IndexedDoc[] = [];

function buildIndex() {
  const index = new Document({
    document: {
      id: "id",
      index: ["title", "tagsText", "bodyBigram"],
      store: ["slug", "title", "tags", "section"],
    },
  } as ConstructorParameters<typeof Document>[0]);

  const docs = getAllDocPages().map((doc, id) => ({
    id,
    slug: doc.slug,
    title: doc.title,
    tags: doc.tags,
    section: doc.section,
    tagsText: doc.tags.join(" "),
    bodyBigram: toBigramText(doc.body),
  }));

  for (const doc of docs) {
    index.add(doc);
  }

  cachedIndex = index;
  cachedDocs = docs;
  return index;
}

function getIndex() {
  if (!cachedIndex) buildIndex();
  return cachedIndex!;
}

export function search(query: string, limit = 10): SearchResult[] {
  const q = query.trim();
  if (!q) return [];
  const index = getIndex();

  const bigramQuery = toBigramText(q);
  const resultsByField = index.search(q, limit, {
    enrich: true,
  }) as unknown as Array<{ result: Array<{ id: number }> }>;
  const bodyResults = index.search(bigramQuery, limit, {
    enrich: true,
    index: "bodyBigram",
  }) as unknown as Array<{ result: Array<{ id: number }> }>;

  const seen = new Map<number, SearchResult>();
  for (const fieldResult of [...resultsByField, ...bodyResults]) {
    for (const item of fieldResult.result) {
      const id = item.id as number;
      if (seen.has(id)) continue;
      const doc = cachedDocs.find((d) => d.id === id);
      if (!doc) continue;
      seen.set(id, {
        slug: doc.slug,
        title: doc.title,
        tags: doc.tags,
        section: doc.section,
      });
    }
  }

  return Array.from(seen.values()).slice(0, limit);
}
