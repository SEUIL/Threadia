"use client";

/*
 * DmRoomsContext — dm/layout.tsx의 방 목록(아이콘 레일 unread 배지)을
 * dm/[roomId]/page.tsx와 공유한다.
 *
 * 리뷰 반영(PR #173, pktewon): 방에 입장해 markRead가 성공해도 레일의
 * unreadCount는 layout이 소유한 별개 state라 갱신되지 않았다. 매번 방
 * 목록을 다시 불러오는 대신, markRoomRead()로 해당 방의 unreadCount만
 * 로컬에서 0으로 내려 즉시 반영한다(서버 재조회 없음 — 이미 markRead API
 * 호출이 성공했다는 걸 전제로 한다).
 */
import { createContext, useContext } from "react";
import type { DirectMessageRoomResponse } from "@/types/direct-message.types";

interface DmRoomsContextValue {
  rooms: DirectMessageRoomResponse[];
  markRoomRead: (roomId: number) => void;
}

const DmRoomsContext = createContext<DmRoomsContextValue | null>(null);

export function DmRoomsProvider({
  value,
  children,
}: {
  value: DmRoomsContextValue;
  children: React.ReactNode;
}) {
  return <DmRoomsContext.Provider value={value}>{children}</DmRoomsContext.Provider>;
}

export function useDmRooms() {
  const ctx = useContext(DmRoomsContext);
  if (!ctx) {
    throw new Error("useDmRooms는 DmRoomsProvider 안에서만 사용할 수 있습니다.");
  }
  return ctx;
}
