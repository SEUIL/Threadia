"use client";

/*
 * DM React Query 훅 모음 — dm/layout.tsx, dm/[roomId]/page.tsx,
 * members/page.tsx(S14 DM 버튼)가 전부 이걸 쓴다(이슈 #217, PR #173 4차
 * 재리뷰 사후 마이그레이션).
 *
 * 이전엔 dm/layout.tsx 하나에 sequenceRef/latestLoadSequenceRef/
 * readGenerationsRef/startDmSequenceRef/pickerRequestSequenceRef/mountedRef/
 * pathnameRef 7개를 손으로 짜서 "이 응답이 아직 유효한가"를 판정했다. React
 * Query로 옮기면서 이 중 대부분이 사라진다 — 이유는 각 훅 주석에 개별로
 * 적어둔다. DmRoomsContext(방 목록을 dm/[roomId]/page.tsx와 공유하려고
 * 만들었던 Context)도 통째로 없앤다 — 같은 queryKey를 쓰는 두 컴포넌트는
 * React Query가 자동으로 같은 캐시를 공유해서 markRead가 캐시를 갱신하면
 * rail도 다시 렌더 없이 그냥 최신값을 본다.
 */
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { directMessageService } from "@/services/direct-message.service";
import type { DirectMessageResponse, DirectMessageRoomResponse } from "@/types/direct-message.types";

export function dmRoomsQueryKey(companyId: string | number) {
  return ["dm-rooms", String(companyId)] as const;
}

export function dmMessagesQueryKey(roomId: string | number) {
  return ["dm-messages", String(roomId)] as const;
}

export function dmCandidatesQueryKey(companyId: string | number) {
  return ["dm-candidates", String(companyId)] as const;
}

/*
 * 방 목록 — dm/layout.tsx(rail)와 dm/[roomId]/page.tsx(room.companyId 검증,
 * blocker 1) 둘 다 이 훅을 쓴다. 같은 queryKey라 실제 네트워크 요청은 한 번만
 * 나간다(React Query dedup) — 이전엔 두 컴포넌트가 각자 fetch했다.
 *
 * "오래된 응답이 최신 응답을 덮는" 문제(4차 재리뷰, 옛 loadRooms 응답이
 * startDm 직후의 최신 응답을 덮던 것)는 React Query의 Query 인스턴스가
 * 내부적으로 "가장 최근에 시작한 fetch만 커밋"하는 로직을 갖고 있어서 별도
 * 코드 없이 해결된다 — latestLoadSequenceRef가 하던 일이다.
 */
export function useDmRoomsQuery(companyId: string | number | undefined) {
  return useQuery({
    queryKey: dmRoomsQueryKey(companyId ?? ""),
    queryFn: async () => {
      const { data } = await directMessageService.listRooms(companyId!);
      return data.data.rooms;
    },
    enabled: companyId !== undefined,
  });
}

/*
 * mark-read — 성공하면 dm-rooms 캐시의 해당 room만 직접 patch한다
 * (setQueryData). readGenerationsRef가 하던 "이 room은 이 시점 이후 이
 * unreadCount로 고정" 역할을, onMutate에서 진행 중이던 dm-rooms 재조회를
 * 취소(cancelQueries)하는 방식으로 대체한다 — mutation이 시작되는 순간
 * "지금 진행 중인 목록 재조회는 곧 낡은 값이 될 것"이므로 미리 취소해서,
 * mark-read 이후 도착해 unreadCount를 되돌리는 옛 응답 자체가 존재하지
 * 않게 만든다(4차 재리뷰 blocker 2와 동일 시나리오를 라이브러리 기능으로 해결).
 */
export function useMarkDmRoomReadMutation(companyId: string | number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ roomId, lastReadDirectMessageId }: { roomId: number; lastReadDirectMessageId: number }) =>
      directMessageService.markRead(roomId, { lastReadDirectMessageId }),
    onMutate: async () => {
      await queryClient.cancelQueries({ queryKey: dmRoomsQueryKey(companyId) });
    },
    onSuccess: (response, variables) => {
      queryClient.setQueryData<DirectMessageRoomResponse[]>(dmRoomsQueryKey(companyId), (rooms) =>
        rooms?.map((room) =>
          room.id === variables.roomId ? { ...room, unreadCount: response.data.data.unreadCount } : room
        )
      );
    },
  });
}

/*
 * 새 DM 시작 후보 목록 — picker가 열려 있을 때만 fetch한다(enabled). candidates
 * 는 서버 원본을 그대로 캐시에 두고, "기존 room 상대 제외"는 호출부(dm/layout.tsx)
 * 에서 useMemo로 dm-rooms 캐시(둘 다 React Query 훅)와 조합해 계산한다 — 그러면
 * 어느 쪽 응답이 나중에 오든 항상 최신 조합이 나온다(blocker 3와 동일한 문제를
 * "필터링을 fetch 시점이 아니라 렌더 시점에 한다"로 해결한 것과 원리가 같다 —
 * candidates state를 손으로 pre-filter해서 저장하지 않는다).
 */
export function useDmCandidatesQuery(companyId: string | number, enabled: boolean) {
  return useQuery({
    queryKey: dmCandidatesQueryKey(companyId),
    queryFn: async () => {
      const { data } = await directMessageService.listCandidates(companyId);
      return data.data.candidates;
    },
    enabled,
  });
}

/*
 * get-or-create — 성공하면 dm-rooms를 무효화해 rail을 최신화한다.
 * "늦게 도착한 응답이 이미 이동한 화면을 덮어쓰는" 문제(blocker 4)는 React
 * Query가 대신 풀어주지 않는다 — router.push는 React Query 캐시가 아니라
 * 화면 전환이라는 별개의 부수효과라서, 호출부가 여전히 "응답이 왔을 때도
 * 아직 같은 화면인지"를 확인해야 한다(각 호출부에서 pathname ref로 처리).
 */
export function useStartDmMutation(companyId: string | number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (otherUserId: number) => directMessageService.getOrCreateRoom(companyId, { otherUserId }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: dmRoomsQueryKey(companyId) });
    },
  });
}

export function useDmMessagesQuery(roomId: string | number, enabled: boolean) {
  return useQuery({
    queryKey: dmMessagesQueryKey(roomId),
    queryFn: async () => {
      const { data } = await directMessageService.listMessages(roomId);
      return data.data.messages;
    },
    enabled,
  });
}

export function useSendDmMessageMutation(roomId: string | number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (content: string) => directMessageService.send(roomId, { content }),
    onSuccess: (response) => {
      queryClient.setQueryData<DirectMessageResponse[]>(dmMessagesQueryKey(roomId), (messages) => [
        ...(messages ?? []),
        response.data.data.message,
      ]);
    },
  });
}
