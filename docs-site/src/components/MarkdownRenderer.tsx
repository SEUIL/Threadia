import React from "react";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import rehypePrismPlus from "rehype-prism-plus";
import Mermaid from "./Mermaid";

function extractText(node: React.ReactNode): string {
  if (typeof node === "string" || typeof node === "number") return String(node);
  if (Array.isArray(node)) return node.map(extractText).join("");
  if (
    node &&
    typeof node === "object" &&
    "props" in node &&
    (node as { props?: { children?: React.ReactNode } }).props?.children
  ) {
    return extractText((node as { props: { children: React.ReactNode } }).props.children);
  }
  return "";
}

export default function MarkdownRenderer({ content }: { content: string }) {
  return (
    <ReactMarkdown
      remarkPlugins={[remarkGfm]}
      rehypePlugins={[rehypePrismPlus]}
      components={{
        code(props) {
          const { className, children, ...rest } = props;
          const isMermaid = /language-mermaid/.test(className ?? "");
          if (isMermaid) {
            return <Mermaid chart={extractText(children)} />;
          }
          return (
            <code className={className} {...rest}>
              {children}
            </code>
          );
        },
      }}
    >
      {content}
    </ReactMarkdown>
  );
}
