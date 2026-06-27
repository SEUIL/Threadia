import type { NextConfig } from "next";
import path from "node:path";

const nextConfig: NextConfig = {
  // docs-site/는 모노레포 형태로 Threadia/ 안에 있고, lib/docs.ts가 상위 docs/
  // 폴더를 런타임 경로(process.cwd() + "..")로 읽는다. Vercel의 파일 트레이싱이
  // 프로젝트 루트(docs-site/) 밖의 파일은 자동으로 못 찾기 때문에, 트레이싱
  // 루트를 한 단계 위(Threadia/)로 올리고 /api/search에 docs/ 전체를 명시적으로
  // 포함시킨다. 그렇지 않으면 로컬 빌드는 통과하지만 Vercel 배포 후
  // 검색 API가 "파일 없음"으로 실패할 수 있다.
  outputFileTracingRoot: path.join(__dirname, ".."),
  outputFileTracingIncludes: {
    // lib/docs.ts가 실제로 읽는 폴더를 명시한다.
    "/api/search": [
      "../docs/00-project/**/*.md",
      "../docs/02-requirements/**/*.md",
      "../docs/03-design/**/*.md",
      "../docs/04-api/**/*.md",
      "../docs/05-architecture/**/*.md",
      "../docs/06-meetings/**/*.md",
      "../docs/07-decisions/**/*.md",
    ],
  },
  outputFileTracingExcludes: {
    // outputFileTracingRoot를 한 단계 올리면 자동 추적이 01-planning(raw
    // archive, 이미지·PDF 포함)과 08-members(개인 폴더) 전체를 끌고 들어온다.
    // lib/docs.ts는 이 두 폴더를 의도적으로 제외하므로 트레이싱에서도 명시적으로 뺀다.
    "/api/search": ["../docs/01-planning/**", "../docs/08-members/**"],
  },
};

export default nextConfig;
