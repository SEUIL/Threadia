> **작성자**: 김민정
> **작성일**: 2026-07-02
> **최종 수정**: 2026-07-02
> **버전**: v1.1
> **상태**: 검토 중
> **태그**: auth, api, review, wbs-1.3.1

# 이메일 Auth API(WBS 1.3.1) 확정 사항 — 팀장 컨펌 및 팀 공유

> 작성 배경: 민경님이 작성한 이메일 Auth API 구현 문서를 `docs/04-api/API-명세서.md` 13.2절 기준으로 리뷰 → 수정본 반영 확인 → 쿠키 `Path` 값을 `docs/05-architecture/프론트엔드-구조.md` 기준으로 통일 → 최종 확정본 작성까지 진행한 결과를 정리한 문서다.

---

## 1. 팀장 컨펌 요청

아래 1가지만 확인/승인 부탁드립니다. 나머지는 이미 확정된 문서(API 명세서, 프론트엔드-구조) 기준을 그대로 따른 것이라 별도 결정 사항은 없습니다.

| 확인 항목 | 내용 | 근거 |
|---|---|---|
| refresh token 쿠키 `Path` 값 | 민경님 초안은 `/api/v1/auth`로 좁혀서 구현했으나, `Path=/`(전체 경로)로 통일 | `docs/05-architecture/프론트엔드-구조.md` 5.4절이 이미 팀 간 참조되는 확정 계약이라, 기존 값에 맞추는 쪽이 변경 범위가 작고 후속 API(OAuth, token refresh 등)와 충돌 위험이 없음 |

승인해주시면 `docs/05-architecture/프론트엔드-구조.md`와 이메일 Auth API 구현 확정본 양쪽 값이 일치한 상태로 그대로 진행합니다.

---

## 2. 확정된 API 계약 요약

`docs/04-api/API-명세서.md` 13.2절 기준, 4개 API 전부 아래 규칙을 따릅니다.

| 규칙 | 내용 |
|---|---|
| 응답 형식 | 모든 응답 `{ "data": {...}, "meta": { "requestId": "..." } }` envelope |
| signup | 토큰 발급 없이 `data.user`만 반환 (자동 로그인 아님) |
| login | `data.accessToken` + `data.user`(4필드) + `data.companies: []` |
| user 객체 | `id`, `email`, `nickname`, `avatarUrl` **4개 필드만** (status/createdAt/updatedAt 미노출) |
| access token 만료 | 30분(1800초) |
| refresh token 전달 | JSON body 미노출, `Set-Cookie`로만 전달 |
| refresh token cookie | `HttpOnly`, `SameSite=Lax`, `Path=/`, `Secure`(운영만), `Max-Age=1209600`(14일) |
| logout 응답 | `200 OK` + `{ data: { success: true } }` |
| email 정규화 | 모든 진입점에서 `trim + lowercase` |
| 인증 통과 조건 | `users.status = ACTIVE`만 로그인/토큰갱신/`me` 허용 |

전체 요청/응답 예시, curl 검증법은 민경님 구현 확정본에 정리돼 있다.

---

## 3. 팀원 공유 사항 — 후속 Auth 작업 시 반드시 이 계약을 따라야 함

1.3.1에서 확정한 응답 계약은 앞으로 구현될 다른 Auth API에도 그대로 이어집니다. 각 담당자가 아래 내용을 참고해주세요.

| 후속 작업 | 담당(예정) | 지켜야 할 점 |
|---|---|---|
| 소셜 로그인(OAuth callback, 1.3.2) | 여도현 | OAuth callback은 이메일 로그인과 동일한 Threadia access/refresh token pair를 발급한다. 단, callback endpoint의 직접 HTTP 응답 shape는 login JSON 응답과 동일하다고 확정하지 않으며, browser redirect 후 accessToken 전달 방식은 별도 결정한다 (`docs/05-architecture/프론트엔드-구조.md` 5.6절 — "OAuth callback 후 accessToken 전달 방식은 백엔드와 추가 협의가 필요하다") |
| 비밀번호 재설정(3.3) | 여도현/김태준/김민경 | confirm 성공 시 해당 사용자의 **모든 기존 refresh_tokens를 revoke**해야 함 (logout은 쿠키 1개만 revoke하면 되지만, 이건 "사용자 기준 전체 revoke" 별도 기능 필요) |
| Token refresh(3.7) | 여도현/김태준/김민경 | 같은 쿠키(`Path=/`, `HttpOnly`, `SameSite=Lax`)를 읽어서 rotate 처리. `Path=/`로 확정해둔 덕분에 refresh 엔드포인트가 어디에 있든 쿠키 전송 문제 없음 |
| `PATCH /users/me/profile` | - | 이미 같은 `user` 4필드 구조로 응답하게 스펙에 있어서 별도 조정 불필요 |

## 4. 구현 시 추가로 반영해야 할 것 (문서 요약엔 없지만 스펙엔 있는 항목)

- `password`: 8자 이상, 영문+숫자 조합 (validation)
- `nickname`: 2~100자 (validation)
- 에러 응답은 공통 형식 `{ "error": { "code", "message", "details" }, "meta": {...} }` 사용 (API-명세서.md 13.1절)
- CORS: 개발 환경 `Access-Control-Allow-Origin=http://localhost:3000`, `Access-Control-Allow-Credentials=true`

---

## 참고 문서

- 민경님 구현 확정본 (카카오톡 공유, 저장소 미포함) — 전체 요청/응답 예시, curl 검증, 완료 조건 체크리스트
- `docs/04-api/API-명세서.md` 13.2절 — canonical 응답 계약
- `docs/00-project/sprint-운영/sprint-1-백엔드-dod.md` — JWT 정책, ACTIVE 사용자 기준
- `docs/05-architecture/프론트엔드-구조.md` 5.4절 — CORS/Cookie 확정값
