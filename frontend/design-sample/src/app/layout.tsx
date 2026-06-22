import type { Metadata } from "next";
import "./globals.css";
import "@/styles/palettes.css";
import "@/styles/korean-fonts.css";
import { fontVars } from "./fonts";

export const metadata: Metadata = {
  title: "Front Design Sample",
  description: "프론트 디자인 샘플 — 색 조합 쇼케이스",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ko" className={`${fontVars} h-full antialiased`}>
      <body className="min-h-full flex flex-col">{children}</body>
    </html>
  );
}
