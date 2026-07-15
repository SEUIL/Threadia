/*
 * UserAvatar — components/profile/UserAvatar.tsx
 *
 * 사용자 아바타 한 곳. avatarUrl이 있으면 이미지를, 없으면 닉네임 첫 글자 원을
 * 그린다. RailShell/HubOverlay가 각자 이니셜 원을 그리던 4곳을 이걸로 모았다
 * (avatarUrl은 /auth/me로 받아오면서도 화면엔 안 쓰이던 죽은 데이터였다 — WBS 4.2.2).
 *
 * 이미지 로드 실패(끊긴 URL, 404)는 흔한 케이스라 이니셜로 자동 폴백한다.
 * avatarUrl은 사용자가 직접 넣는 외부 URL이고 서버가 형식 검증을 하지 않으므로
 * (API-명세서 §13.3 "URL 형식 validation은 하지 않는다") 깨진 이미지가 그대로
 * 노출되지 않게 하려면 이 폴백이 필요하다.
 *
 * next/image 대신 <img>를 쓴다 — 임의 외부 호스트를 허용하려면 next.config의
 * images.remotePatterns를 열어야 하는데, 그건 이 작업 범위 밖이다.
 *
 * 스타일 규칙: style={{}} 금지, @theme 시멘틱 토큰 클래스만 사용.
 */
"use client";

import { useState } from "react";

export function UserAvatar({
  nickname,
  avatarUrl,
  className = "h-8 w-8",
}: {
  nickname: string;
  avatarUrl?: string | null;
  /** 크기 등 배치용 클래스. 호출부가 기존에 쓰던 h-8 w-8 등을 그대로 넘긴다. */
  className?: string;
}) {
  // 실패 사실을 boolean이 아니라 **실패한 URL**로 기억한다. boolean으로 두면 한 번
  // 깨진 뒤에는 사용자가 URL을 고쳐도 계속 이니셜만 나온다(모달 미리보기는 입력 중인
  // URL을 그대로 넘기므로 오타를 지나가는 것만으로도 이 상태가 된다).
  const [brokenUrl, setBrokenUrl] = useState<string | null>(null);
  const initial = nickname.slice(0, 1);
  const showImage = Boolean(avatarUrl) && avatarUrl !== brokenUrl;

  return (
    <span
      className={
        "flex shrink-0 items-center justify-center overflow-hidden rounded-full " +
        "avatar-ring text-xs font-semibold text-chrome-foreground " +
        className
      }
    >
      {showImage ? (
        // eslint-disable-next-line @next/next/no-img-element -- 임의 외부 호스트 허용(위 주석 참고)
        <img
          src={avatarUrl as string}
          alt=""
          className="h-full w-full object-cover"
          onError={() => setBrokenUrl(avatarUrl as string)}
        />
      ) : (
        initial
      )}
    </span>
  );
}
