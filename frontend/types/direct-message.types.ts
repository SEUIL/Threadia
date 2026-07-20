/*
 * 1:1 DM 응답 타입 — backend record와 1:1 매핑.
 * 계약 기준: docs/04-api/API-명세서.md §8, §8.1 (WBS 4.7.3.1, Backend 완료).
 *
 * 이 파일은 RailShell/HubOverlay의 unread 배지 하드코딩(WBS 4.7.3.2 이슈 #174,
 * MOCK_DM_UNREAD_COUNT)을 고치기 위해 unreadCount 계약만 우선 추가했다. 방
 * 목록/메시지 등 나머지 DM 타입은 PR #173(WBS 4.7.3.2 Frontend, 별도 브랜치)이
 * 추가하는 중이라 이 PR과 add/add 충돌이 날 수 있다 — 그때는 두 PR의 필드를
 * 합집합으로 유지한다(반복되는 충돌 해소 패턴, memory 참고).
 */
export interface DirectMessageUnreadCountData {
  unreadCount: number;
}
