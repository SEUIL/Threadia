# 5일차 최종본 6일차 수정본 v2

> **작성자**: Threadia 팀
> **작성일**: 2026-06-17
> **최종 수정**: 2026-06-17
> **버전**: 6일차 수정본 v2
> **상태**: 6일차 수정본
> **수정 근거**: Threadia-기획문서-검수리포트.md

## 1. 목적

이 폴더는 5일차 최종 산출물 원본을 수정하지 않고, 6일차 검수 결과를 반영한 versioned corrected copy를 보관한다.

## 2. 기준 자료

| 구분 | 경로 |
|---|---|
| 원본 폴더 | `/home/ubuntu/dev/Threadia/docs/01-planning/기획/05_기획주간_5일차_자료/최종 산출물` |
| 검수리포트 | `/home/ubuntu/dev/Threadia/docs/01-planning/기획/06_기획주간_6일차_자료/분업 산출물/검토 문서/완료_Threadia-기획문서-검수리포트.md` |
| 추가 검토 문서 | `/home/ubuntu/dev/Threadia/docs/01-planning/기획/06_기획주간_6일차_자료/분업 산출물/검토 문서` |
| 수정본 폴더 | `/home/ubuntu/dev/Threadia/docs/01-planning/기획/06_기획주간_6일차_자료/분업 산출물/5일차 최종본 수정` |

## 3. 수정본 목록

| 수정본 | 주요 반영 내용 | 닫은 이슈 |
|---|---|---|
| `ERD v2.0 정합도-v1 invitation-v1 6일차-수정본-v1.md` | `refresh_tokens` entity, Mermaid 관계, table detail 추가. `invitations` entity, Mermaid 관계, table detail과 7일 만료, lazy expiration, accept 시 company membership 생성 정책 반영. project invitation Sprint 2 분리와 notification optional 기준 반영 | #1, #2, #4, #7 |
| `API 명세서 v2.3 정합도-v1 invitation-v1 6일차-수정본-v1.md` | `invitationStatus`, `invitationScopeType`, company invitation endpoint와 예시 추가. member immediate create API와 project invitation을 Sprint 1.5 user-facing API에서 제외. invitation error code 통합. UTC/email/Auth hardening/seed/notification optional 기준 반영 | #1, #4, #7, #9 |
| `화면정의서_v3 정합도-v1 invitation-v1 6일차-수정본-v1.md` | S14 구성원 추가를 company invitation 생성, 상태 표시, accept 후 company membership 생성 흐름으로 교정. S13 seed 기준, email lookup 정규화, project invitation Sprint 2 분리, notification optional 기준 반영 | #1, #4, #7 |
| `기능-시나리오-API-ERD 추적표 v1.0 정합도-v1 invitation-v1 6일차-수정본-v1.md` | company invitation 기능을 scenario, API, `invitations` ERD에 연결. Auth token trace에 `refresh_tokens` 포함. Auth hardening과 project invitation Sprint 2 분리, notification optional 기준 반영 | #1, #2, #7 |
| `Definition of Done_작업 유형별 완료 조건 v2 정합도-v1 invitation-v1 6일차-수정본-v1.md` | Sprint 1 DB 범위에 `refresh_tokens` 추가. Sprint 1.5 company invitation DoD와 UTC/email/Auth hardening/seed/notification optional 완료 기준 추가 | #3, #6, #7 |
| `Sprint1_Backlog 정합도-v1 invitation-v1 6일차-수정본-v1.md` | Sprint 1 Auth-only 유지. Sprint 1.5 company invitation backlog, owner, timing, completion criteria에 UTC/email/Auth hardening/seed 기준 추가 | #3, #7 |
| `Sprint 1_detail 정합도-v1 invitation-v1 6일차-수정본-v1.md` | Sprint 1 Auth-only 유지. Sprint 1.5 company invitation 상세 계획과 DoD-level 기준에 UTC/email/Auth hardening/seed 기준 추가 | #3, #7 |
| `Sprint1-백엔드-의존성-기술스택-DoD-v1.1 정합도-v1 invitation-v1 6일차-수정본-v1.md` | Sprint 1 인증 DB 4개 table 유지. Sprint 1.5 company invitation Backend DB/API 범위와 DoD, UTC/email/Auth hardening/seed/notification optional 기준 추가 | #3, #7 |
| `기능 분해표 v3.0 정합도-v1 invitation-v1 6일차-수정본-v1.md` | 도메인 기능을 `MVP, 후속 sprint 구현`으로 정리. company invitation 기능을 Sprint 1.5로 추가하고 project invitation Sprint 2 분리, Auth hardening/notification optional 기준 연결 | #5, #7 |
| `Sprint1-프론트엔드-의존성-기술스택-DoD-v1.1 정합도-v1 invitation-v1 6일차-수정본-v1.md` | Frontend Auth-only 응답 처리, `companies: []` 허용, `/auth/me` minimal context, token 저장 권장안, reset 후 재로그인, ACTIVE 사용자 기준 반영 | #7, #8 |
| `검증 방법_test-build-API-화면 확인 기준 v2 정합도-v1 invitation-v1 6일차-수정본-v1.md` | API 예시 경로 보정, email 정규화/OAuth link/refresh revoke/ACTIVE-only 검증, Sprint 1.5 company invitation seed·UTC·notification optional 검증 기준 추가 | #7, #8 |
| `사용자 시나리오 v3.0 정합도-v1 invitation-v1 6일차-수정본-v1.md` | Auth hardening 성공 조건, seed 회사 기준, 즉시 membership 대신 company invitation accept/decline 흐름, project invitation Sprint 2 분리, S11 UI 제외 기준 반영 | #7, #8 |
| `보류 기능 최종표 v1.0 정합도-v1 invitation-v1 6일차-수정본-v1.md` | project invitation, token link, 미가입 초대, signup auto-accept, resend, batch scheduler, notification_events 필수 구현, Push/S11 UI, 별도 invitation preference를 Sprint 1.5 제외 범위로 명시 | #7, #8 |
| `AI 사용 규칙 v1.0 정합도-v1 6일차-수정본-v1.md` | AI가 제안한 Auth/token/OAuth/invitation 변경의 사람 검증 책임과 6일차 v2 공통 기준 추가 | #7, #8 |
| `CI 기준 확정 v1.0 정합도-v1 6일차-수정본-v1.md` | Auth 변경 PR과 invitation/DB 변경 PR의 6일차 v2 검증 추가 기준 반영 | #7, #8 |
| `PR Template v1.0 정합도-v1 6일차-수정본-v1.md` | Auth·Invitation 변경 체크리스트, secret/raw token 노출 방지 체크 추가 | #7, #8 |
| `Sprint1_담당그룹_역할분담 v1.1 정합도-v1 6일차-수정본-v1.md` | Auth hardening, Sprint 1.5 invitation, notification 분리, 문서 정합성 확인 역할 추가 | #7, #8 |

## 4. 닫은 검수리포트 이슈

| 이슈 | 처리 상태 |
|---|---|
| #1 invitation 메모와 본문 불일치 | ERD, API, 화면정의서, 추적표 본문에 invitation 흐름을 직접 반영했다 |
| #2 `refresh_tokens` ERD 누락 | ERD entity 목록, Mermaid, table detail에 반영하고 추적표 Auth trace에도 추가했다 |
| #3 Sprint 1.5 실행 계획 부재 | Backlog, Sprint 1_detail, Backend DoD, DoD 문서에 Sprint 1.5 계획과 완료 기준을 추가했다 |
| #4 invitation 만료 정책 충돌 | API 예시와 ERD, 화면, DoD에서 기본 7일 `expiresAt`과 lazy expiration 기준으로 통일했다 |
| #5 Sprint 1 범위 표현 불일치 | 기능 분해표 도메인 row를 `MVP, 후속 sprint 구현` 중심으로 교정하고 company invitation은 Sprint 1.5, project invitation은 Sprint 2로 분리했다 |
| #6 DoD DB 범위 `refresh_tokens` 누락 | Definition of Done Sprint 1 DB 범위에 `refresh_tokens`를 추가했다 |
| #7 metadata hygiene | 17개 수정본 metadata를 `최종 수정: 2026-06-17`, `버전: 6일차 수정본 v2`, `상태: 6일차 수정본`, `수정 근거`로 갱신했다 |
| #8 최종본 폴더의 미완 상태 문서 혼재 | 17개 6일차 수정본 대상 문서를 모두 `상태: 6일차 수정본`으로 통일하고, 원본 5일차 산출물과 구분되는 corrected copy로 보관한다 |
| #9 invitation error code 미통합 | API 공통 error code에 `ALREADY_COMPANY_MEMBER`, `ALREADY_PROJECT_MEMBER`, `INVITATION_ALREADY_PENDING`, `INVITATION_EXPIRED`, `INVITATION_CANCELED`를 추가했다 |

## 5. 5일차 최종 산출물 대비 보정

| 구분 | 개수 | 설명 |
|---|---:|---|
| 5일차 최종 산출물 본문 | 17 | README와 `temp/`를 제외한 Markdown 본문 |
| 6일차 수정본 본문 | 17 | 기존 9개 + 이번에 들여온 8개 |
| README | 1 | 이 폴더의 구성과 기준 설명 |

## 6. 남은 caveat

- 이 수정본은 기획 문서 정합성 보정본이다. 실제 schema migration, Backend 구현, Frontend 구현은 포함하지 않는다.
- Sprint 1.5는 계획과 DoD를 추가했지만, 실제 일정 확정과 issue 생성은 별도 팀 운영 작업이다.
- Project invitation, actual email sending, token link invitation, unregistered-user invitation, signup auto-accept, resend, 필수 batch scheduler는 공식 결정에 따라 Sprint 1.5 범위에서 제외했다.
- Sprint 1.5는 local/dev/demo seed 회사 1개와 `COMPANY_ADMIN` 1명으로 검증하며, `POST /api/v1/companies`는 Sprint 1.5 구현 범위가 아니다.
- `notification_events`는 필요 시 내부 event 원장 기록을 남길 수 있지만 Sprint 1.5 완료 조건에는 포함하지 않는다.
- UTC 시간 저장, email `trim + lowercase`, OAuth blind auto-link 금지, password reset 후 refresh token revoke, non-ACTIVE 인증 차단은 6일차 수정본 v2 공통 기준이다.
