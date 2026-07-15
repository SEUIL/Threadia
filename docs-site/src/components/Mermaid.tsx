"use client";

import { useEffect, useId, useRef } from "react";

export default function Mermaid({ chart }: { chart: string }) {
  const ref = useRef<HTMLDivElement>(null);
  const id = useId().replace(/:/g, "");

  useEffect(() => {
    let cancelled = false;

    async function render() {
      const mermaid = (await import("mermaid")).default;
      mermaid.initialize({ startOnLoad: false, theme: "default" });
      try {
        const { svg } = await mermaid.render(`mermaid-${id}`, chart);
        if (!cancelled && ref.current) {
          ref.current.innerHTML = svg;
        }
      } catch (err) {
        if (!cancelled && ref.current) {
          ref.current.textContent = `Mermaid 렌더링 실패: ${String(err)}`;
        }
      }
    }

    render();
    return () => {
      cancelled = true;
    };
  }, [chart, id]);

  return <div className="mermaid-diagram" ref={ref} />;
}
