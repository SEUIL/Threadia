import fs from "node:fs";
import path from "node:path";

// docs-site/는 Threadia/ 바로 아래에 있으므로 상위 폴더의 docs/를 가리킨다.
const DOCS_ROOT = path.join(process.cwd(), "..", "docs");

// 01-planning(raw archive), 08-members(개인 폴더)는 사이트에 올리지 않는다.
const EXCLUDED_DIRS = new Set(["01-planning", "08-members", "assets"]);

// 메타정보(작성자/태그 등)가 없는 안내/진입점 문서는 콘텐츠 문서가 아니므로 제외한다.
const EXCLUDED_RELATIVE_PATHS = new Set([
  "AGENTS.md",
  "CLAUDE.md",
  "README.md",
  path.join("00-project", "문서-작성-규칙.md"),
]);

export type DocMeta = {
  slug: string[];
  relativePath: string;
  title: string;
  author?: string;
  status?: string;
  tags: string[];
  section: string;
};

export type DocPage = DocMeta & {
  body: string;
};

function walk(dir: string, baseDir: string): string[] {
  const entries = fs.readdirSync(dir, { withFileTypes: true });
  const files: string[] = [];
  for (const entry of entries) {
    if (entry.isDirectory()) {
      if (EXCLUDED_DIRS.has(entry.name)) continue;
      files.push(...walk(path.join(dir, entry.name), baseDir));
    } else if (entry.isFile() && entry.name.endsWith(".md")) {
      const relativePath = path.relative(baseDir, path.join(dir, entry.name));
      if (EXCLUDED_RELATIVE_PATHS.has(relativePath)) continue;
      files.push(relativePath);
    }
  }
  return files;
}

function parseFrontmatter(content: string): {
  fields: Record<string, string>;
  tags: string[];
  rest: string;
} {
  const lines = content.split(/\r?\n/);
  const fields: Record<string, string> = {};
  const tags: string[] = [];
  let i = 1; // 첫 줄은 "---"
  let inTags = false;
  for (; i < lines.length; i++) {
    const line = lines[i];
    if (line.trim() === "---") {
      i++;
      break;
    }
    const tagItem = line.match(/^\s*-\s*(.+)$/);
    if (inTags && tagItem) {
      tags.push(tagItem[1].trim());
      continue;
    }
    const kv = line.match(/^(\w+):\s*(.*)$/);
    if (kv) {
      inTags = kv[1] === "tags";
      if (!inTags) {
        fields[kv[1]] = kv[2].replace(/^"|"$/g, "").trim();
      }
      continue;
    }
    inTags = false;
  }
  return { fields, tags, rest: lines.slice(i).join("\n") };
}

function parseBlockquoteMeta(content: string): {
  fields: Record<string, string>;
  tags: string[];
  rest: string;
} {
  const lines = content.split(/\r?\n/);
  const fields: Record<string, string> = {};
  let tags: string[] = [];
  let i = 0;
  // 메타 블록 앞에 "# 제목" 줄이 먼저 나오는 문서도 있어 둘 다 허용한다.
  let title = "";
  while (i < lines.length) {
    const headingMatch = lines[i].match(/^#\s+(.+?)\s*$/);
    const metaMatch = lines[i].match(/^>\s*\*\*(.+?)\*\*:\s*(.*?)\s*$/);
    if (headingMatch && !title) {
      title = headingMatch[1];
      i++;
      continue;
    }
    if (metaMatch) {
      const key = metaMatch[1].trim();
      const value = metaMatch[2].trim();
      if (key === "태그") {
        tags = value.split(",").map((t) => t.trim()).filter(Boolean);
      } else {
        fields[key] = value;
      }
      i++;
      continue;
    }
    if (lines[i].trim() === "" || lines[i].trim() === "---") {
      i++;
      continue;
    }
    break;
  }
  if (title) fields["제목"] = title;
  return { fields, tags, rest: lines.slice(i).join("\n") };
}

function extractTitle(content: string, fallback: string): string {
  const h1 = content.match(/^#\s+(.+?)\s*$/m);
  return h1 ? h1[1] : fallback;
}

function readDoc(relativePath: string): DocPage {
  const absPath = path.join(DOCS_ROOT, relativePath);
  const raw = fs.readFileSync(absPath, "utf-8");
  const isFrontmatter = raw.startsWith("---");
  const parsed = isFrontmatter ? parseFrontmatter(raw) : parseBlockquoteMeta(raw);
  const slug = relativePath.replace(/\.md$/, "").split(path.sep);
  const fallbackTitle = path.basename(relativePath, ".md");
  const title =
    parsed.fields["title"] ?? parsed.fields["제목"] ?? extractTitle(raw, fallbackTitle);

  return {
    slug,
    relativePath,
    title,
    author: parsed.fields["작성자"] ?? parsed.fields["author"],
    status: parsed.fields["상태"] ?? parsed.fields["status"],
    tags: parsed.tags,
    section: slug[0],
    body: parsed.rest,
  };
}

function toDocMeta(doc: DocPage): DocMeta {
  return {
    slug: doc.slug,
    relativePath: doc.relativePath,
    title: doc.title,
    author: doc.author,
    status: doc.status,
    tags: doc.tags,
    section: doc.section,
  };
}

export function listDocs(): DocMeta[] {
  const files = walk(DOCS_ROOT, DOCS_ROOT).sort();
  return files.map((f) => toDocMeta(readDoc(f)));
}

export function getDocBySlug(slug: string[]): DocPage | null {
  const relativePath = slug.join(path.sep) + ".md";
  const absPath = path.join(DOCS_ROOT, relativePath);
  if (!fs.existsSync(absPath)) return null;
  return readDoc(relativePath);
}

export function getAllDocPages(): DocPage[] {
  const files = walk(DOCS_ROOT, DOCS_ROOT).sort();
  return files.map(readDoc);
}
