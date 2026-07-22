/*
 * Root Layout — app/layout.tsx
 *
 * 역할:
 *   모든 페이지의 최상위 래퍼.
 *   폰트 CSS 변수 주입 + globals.css 로드만 담당한다.
 *
 * 의존성 규칙:
 *   - 이 파일 아래에 (auth)/layout.tsx, (app)/layout.tsx 가 중첩된다.
 *   - 폰트 변수(--font-geist-sans, --font-geist-mono)는 여기서 주입되어
 *     globals.css의 --font-sans / --font-mono 가 참조한다.
 *   - globals.css 는 반드시 여기서 한 번만 import 한다. 하위 layout에서 재import 금지.
 *
 * 변경 시 주의:
 *   - <html lang> 은 한국어 서비스이므로 "ko" 로 고정.
 *   - className 의 폰트 변수는 globals.css 의 --font-sans 와 쌍을 이룬다.
 *     폰트를 교체하려면 globals.css 의 --font-sans 와 함께 수정할 것.
 */
import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import "./globals.css";
import { AuthProvider } from "@/contexts/AuthContext";
import { QueryProvider } from "@/contexts/QueryProvider";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "Threadia",
  description: "팀 협업 플랫폼 Threadia",
};

export default function RootLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return (
    <html
      lang="ko"
      className={`${geistSans.variable} ${geistMono.variable} antialiased`}
    >
      {/*
       * body: globals.css 의 --bg-page, --text-primary, --font-sans 가 적용됨.
       * flex flex-col: (app) 레이아웃의 헤더+콘텐츠+푸터 구조를 지원.
       * html에 h-full 을 두지 않는다 — (auth) layout 의 min-h-screen 이
       * 뷰포트 기준으로 높이를 잡기 때문에 height 체인 불필요.
       */}
      {/* AuthProvider: 앱 로드 시 refresh 쿠키로 세션을 복원하고 전역 인증 상태를 제공한다. */}
      <body className="flex flex-col">
        <QueryProvider>
          <AuthProvider>{children}</AuthProvider>
        </QueryProvider>
      </body>
    </html>
  );
}
