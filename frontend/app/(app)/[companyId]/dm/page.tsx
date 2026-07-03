"use client";

/*
 * S09 — 채팅·DM 목록 (진입 화면)
 *
 * `dm/[roomId]`는 이미 있었지만 목록으로 들어오는 진입점(`/[companyId]/dm`)이
 * 없어서 상단바·좌측메뉴의 "채팅·DM" 링크가 404였다. 최소한의 방 목록만 두고
 * 방을 고르면 `dm/[roomId]`로 이동한다.
 *
 * ⚠️ MOCK: DM 방 목록 API(`GET /companies/{companyId}/direct-messages/rooms`)가
 * 없어서 S14(구성원 관리)와 동일한 목업 구성원 목록으로 방을 흉내낸다.
 */
import Link from "next/link";
import { useParams } from "next/navigation";

const MOCK_ROOMS = [
  { roomId: "u1", name: "정철진" },
  { roomId: "u2", name: "김민정" },
  { roomId: "u3", name: "여도현" },
];

export default function DmListPage() {
  const params = useParams<{ companyId: string }>();
  const companyId = params.companyId;

  return (
    <div className="mx-auto max-w-2xl px-6 py-10">
      <h1 className="text-xl font-bold text-foreground">채팅·DM</h1>
      <p className="mt-1 text-sm text-muted">최근 대화 목록입니다.</p>

      <ul className="mt-6 flex flex-col gap-2">
        {MOCK_ROOMS.map((room) => (
          <li key={room.roomId}>
            <Link
              href={`/${companyId}/dm/${room.roomId}`}
              className="flex items-center gap-3 rounded-lg border border-border bg-surface px-4 py-3 shadow-sm transition-colors duration-fast hover:bg-primary-subtle"
            >
              <span className="flex h-9 w-9 items-center justify-center rounded-full bg-primary text-sm font-semibold text-on-primary">
                {room.name.slice(0, 1)}
              </span>
              <span className="text-sm font-medium text-foreground">{room.name}</span>
            </Link>
          </li>
        ))}
      </ul>
    </div>
  );
}
