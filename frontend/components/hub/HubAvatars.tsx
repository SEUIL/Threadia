"use client";

/*
 * HubAvatars — components/hub/HubAvatars.tsx
 *
 * 회사 구성원 중 온라인(useHubPresence, "온라인 = 허브 페이지 접속 여부")인
 * 사람만 hubSeats.ts의 실측 좌석에 배치한다. 원래 브랜치(허브 실시간 채팅+3D
 * 아바타, WBS 밖 개인 R&D)는 실시간 채팅까지 포함했는데, 채팅은 DB 테이블
 * (`hub_chat_messages`)이 필요해서 이미 호스팅된 프로덕션에 스키마 마이그레이션을
 * 새로 얹는 비용/리스크가 커 폐기했다(2026-07-22, 사용자 결정) — 이 컴포넌트는
 * DB 의존이 전혀 없는 presence 부분만 다시 만든 것이다.
 *
 * 좌석 배정: "랜덤 자리"를 요청받았지만, 매 렌더/새로고침마다 진짜로 무작위면
 * 온라인 상태가 바뀔 때마다 다른 사람이 자리를 옮겨 다니는 것처럼 보여
 * 오히려 어색하다. 그래서 회사 구성원 전체 목록을 userId로 정렬해 "역할/가입
 * 순서와 무관하지만 항상 같은" 좌석을 한 번만 배정하고, 그중 온라인인 사람만
 * 렌더링한다 — 누가 로그인/로그아웃해도 다른 사람의 좌석은 안 흔들린다.
 * 좌석(8석)보다 인원이 많으면 넘치는 인원은 표시하지 않는다(기존 설계와 동일).
 *
 * 후보 목록은 이슈 #208/PR #213의 `GET .../direct-messages/candidates`를
 * 재사용한다 — 일반 MEMBER 권한으로도 호출 가능하고(ACTIVE company/user만),
 * `companyMemberService.list()`와 달리 COMPANY_ADMIN 전용이 아니다(S14에서
 * 같은 이유로 이미 교체된 전례가 있다). 응답에 role이 없어 본인을 제외한
 * 다른 구성원은 전부 MEMBER 모자로 그린다 — 정확한 역할 구분은 이 화면의
 * 핵심이 아니라서 의도적으로 생략했다.
 */
import { useEffect, useState } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { HubAvatar, type HubAvatarRole } from "@/components/hub/HubAvatar";
import { HUB_SEAT_POSITIONS } from "@/components/hub/hubSeats";
import { useHubPresence } from "@/hooks/useHubPresence";
import { directMessageService } from "@/services/direct-message.service";

// hubSeats.ts의 좌표는 chairDesk.glb 배치용 원점(모델 pivot)이지 의자 방석의
// 실제 중심이 아니다 — 실측(2026-07-21) 결과 방석 중심은 이 좌표에서
// (x:-0.157, z:-0.167)만큼 떨어져 있었다. 아바타는 마커보다 커서 이 오차가
// 그대로 드러나 보정이 필요하다.
const SEAT_CENTER_OFFSET: [number, number] = [-0.157, -0.167];

interface RosterEntry {
  userId: number;
  nickname: string;
  role: HubAvatarRole;
  seatIndex: number;
}

export function HubAvatars() {
  const { user, companyMemberships } = useAuth();
  const primaryMembership = companyMemberships[0] ?? null;
  const onlineUserIds = useHubPresence(primaryMembership?.companyId);

  const [roster, setRoster] = useState<RosterEntry[]>([]);

  useEffect(() => {
    if (!primaryMembership || !user) return;
    let cancelled = false;

    (async () => {
      const self = { userId: user.id, nickname: user.nickname, role: primaryMembership.role as HubAvatarRole };
      try {
        const { data } = await directMessageService.listCandidates(primaryMembership.companyId);
        if (cancelled) return;
        const others = data.data.candidates.map((c) => ({
          userId: c.id,
          nickname: c.nickname,
          role: "MEMBER" as HubAvatarRole,
        }));
        // userId로 정렬해 좌석을 한 번만, 고정적으로 배정한다(위 파일 주석 참고).
        const sorted = [self, ...others].sort((a, b) => a.userId - b.userId);
        setRoster(
          sorted
            .map((entry, seatIndex) => ({ ...entry, seatIndex }))
            .filter((entry) => entry.seatIndex < HUB_SEAT_POSITIONS.length)
        );
      } catch {
        // 후보 조회에 실패해도 본인 자리(seat 0)는 보여준다.
        if (!cancelled) setRoster([{ ...self, seatIndex: 0 }]);
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [primaryMembership, user]);

  if (!primaryMembership || !user) return null;

  return (
    <>
      {roster
        .filter((entry) => onlineUserIds.has(entry.userId))
        .map((entry) => {
          const [seatX, seatY, seatZ] = HUB_SEAT_POSITIONS[entry.seatIndex];
          const position: [number, number, number] = [
            seatX + SEAT_CENTER_OFFSET[0],
            seatY,
            seatZ + SEAT_CENTER_OFFSET[1],
          ];
          return (
            <HubAvatar key={entry.userId} position={position} role={entry.role} userId={entry.userId} nickname={entry.nickname} />
          );
        })}
    </>
  );
}
