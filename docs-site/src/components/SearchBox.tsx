"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";

type SearchResult = {
  slug: string[];
  title: string;
  tags: string[];
  section: string;
};

export default function SearchBox() {
  const [query, setQuery] = useState("");
  const [results, setResults] = useState<SearchResult[]>([]);
  const [open, setOpen] = useState(false);
  const boxRef = useRef<HTMLDivElement>(null);
  const router = useRouter();

  useEffect(() => {
    if (!query.trim()) {
      setResults([]);
      return;
    }
    const controller = new AbortController();
    fetch(`/api/search?q=${encodeURIComponent(query)}`, {
      signal: controller.signal,
    })
      .then((res) => res.json())
      .then((data) => {
        setResults(data.results ?? []);
        setOpen(true);
      })
      .catch(() => {});
    return () => controller.abort();
  }, [query]);

  useEffect(() => {
    function onClickOutside(e: MouseEvent) {
      if (boxRef.current && !boxRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener("click", onClickOutside);
    return () => document.removeEventListener("click", onClickOutside);
  }, []);

  function goTo(result: SearchResult) {
    setOpen(false);
    setQuery("");
    router.push(`/${result.slug.map(encodeURIComponent).join("/")}`);
  }

  return (
    <div className="search-box-wrapper" ref={boxRef}>
      <input
        type="search"
        placeholder="문서 검색"
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        onFocus={() => results.length > 0 && setOpen(true)}
        className="search-box"
      />
      {open && results.length > 0 && (
        <ul className="search-results">
          {results.map((r) => (
            <li key={r.slug.join("/")}>
              <button onClick={() => goTo(r)} className="search-result-item">
                <span className="search-result-title">{r.title}</span>
                <span className="search-result-section">{r.section}</span>
              </button>
            </li>
          ))}
        </ul>
      )}
      {open && query.trim() && results.length === 0 && (
        <ul className="search-results">
          <li className="search-result-empty">검색 결과 없음</li>
        </ul>
      )}
    </div>
  );
}
