import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Threadia Docs",
  description: "Threadia docs/ 렌더링 사이트",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ko">
      <body>{children}</body>
    </html>
  );
}
