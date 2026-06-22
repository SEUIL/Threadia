// 쇼케이스용 무료 폰트 (모두 Google Fonts)
// - next/font 로 로드하면 각 폰트가 CSS 변수로 노출됩니다.
// - layout 에서 fontVars 클래스를 html 에 걸어 전체에서 사용할 수 있게 합니다.
// - 화면에서 폰트를 전환할 때는 이 변수(var(--font-xxx))를 참조합니다.

import {
  Geist,
  Geist_Mono,
  Inter,
  Poppins,
  Plus_Jakarta_Sans,
  DM_Sans,
  Montserrat,
  Roboto,
  Open_Sans,
  Playfair_Display,
  Fraunces,
  Merriweather,
} from "next/font/google";

// 기본 폰트 (globals.css 에서 사용)
export const geistSans = Geist({ subsets: ["latin"], variable: "--font-geist-sans" });
export const geistMono = Geist_Mono({ subsets: ["latin"], variable: "--font-geist-mono" });

// 산세리프 (본문 · UI)
const inter = Inter({ subsets: ["latin"], variable: "--font-inter" });
const poppins = Poppins({
  subsets: ["latin"],
  weight: ["400", "500", "600", "700", "800"],
  variable: "--font-poppins",
});
const jakarta = Plus_Jakarta_Sans({ subsets: ["latin"], variable: "--font-jakarta" });
const dmSans = DM_Sans({ subsets: ["latin"], variable: "--font-dm-sans" });
const montserrat = Montserrat({ subsets: ["latin"], variable: "--font-montserrat" });
const roboto = Roboto({
  subsets: ["latin"],
  weight: ["400", "500", "700", "900"],
  variable: "--font-roboto",
});
const openSans = Open_Sans({ subsets: ["latin"], variable: "--font-open-sans" });

// 세리프 (제목 · 디스플레이)
const playfair = Playfair_Display({ subsets: ["latin"], variable: "--font-playfair" });
const fraunces = Fraunces({ subsets: ["latin"], variable: "--font-fraunces" });
const merriweather = Merriweather({
  subsets: ["latin"],
  weight: ["300", "400", "700", "900"],
  variable: "--font-merriweather",
});

// html 에 한 번에 걸어줄 클래스 문자열
export const fontVars = [
  geistSans,
  geistMono,
  inter,
  poppins,
  jakarta,
  dmSans,
  montserrat,
  roboto,
  openSans,
  playfair,
  fraunces,
  merriweather,
]
  .map((f) => f.variable)
  .join(" ");

// 화면에서 고를 수 있는 폰트 목록 (id / 표시이름 / CSS 변수 / 종류)
export type FontType = "sans" | "serif";
export type FontOption = {
  id: string;
  label: string;
  cssVar: string;
  type: FontType;
};

export const fontOptions: FontOption[] = [
  { id: "geist", label: "Geist", cssVar: "--font-geist-sans", type: "sans" },
  { id: "inter", label: "Inter", cssVar: "--font-inter", type: "sans" },
  { id: "jakarta", label: "Plus Jakarta Sans", cssVar: "--font-jakarta", type: "sans" },
  { id: "dm-sans", label: "DM Sans", cssVar: "--font-dm-sans", type: "sans" },
  { id: "poppins", label: "Poppins", cssVar: "--font-poppins", type: "sans" },
  { id: "montserrat", label: "Montserrat", cssVar: "--font-montserrat", type: "sans" },
  { id: "roboto", label: "Roboto", cssVar: "--font-roboto", type: "sans" },
  { id: "open-sans", label: "Open Sans", cssVar: "--font-open-sans", type: "sans" },
  { id: "playfair", label: "Playfair Display", cssVar: "--font-playfair", type: "serif" },
  { id: "fraunces", label: "Fraunces", cssVar: "--font-fraunces", type: "serif" },
  { id: "merriweather", label: "Merriweather", cssVar: "--font-merriweather", type: "serif" },
];
