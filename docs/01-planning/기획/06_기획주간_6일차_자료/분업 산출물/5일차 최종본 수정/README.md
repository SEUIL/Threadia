# 5일차 최종본 6일차 수정본 v1

> **작성자**: Threadia 팀
> **작성일**: 2026-06-17
> **최종 수정**: 2026-06-17
> **버전**: 6일차 수정본 v1
> **상태**: 6일차 수정본
> **수정 근거**: Threadia-기획문서-검수리포트.md

## 1. 목적

이 폴더는 5일차 최종 산출물 원본을 수정하지 않고, 6일차 검수 결과를 반영한 versioned corrected copy를 보관한다.

## 2. 기준 자료

| 구분 | 경로 |
|---|---|
| 원본 폴더 | `/home/ubuntu/dev/Threadia/docs/01-planning/기획/05_기획주간_5일차_자료/최종 산출물` |
| 검수리포트 | `/home/ubuntu/dev/Threadia/docs/01-planning/기획/06_기획주간_6일차_자료/분업 산출물/Threadia-기획문서-검수리포트.md` |
| 수정본 폴더 | `/home/ubuntu/dev/Threadia/docs/01-planning/기획/06_기획주간_6일차_자료/분업 산출물/5일차 최종본 수정` |

## 3. 수정본 목록

| 수정본 | 주요 반영 내용 | 닫은 이슈 |
|---|---|---|
| `ERD v2.0 정합도-v1 invitation-v1 6일차-수정본-v1.md` | `refresh_tokens` entity, Mermaid 관계, table detail 추가. `invitations` entity, Mermaid 관계, table detail과 7일 만료, lazy expiration, accept 시 membership 생성 정책 반영 | #1, #2, #4, #7 |
| `API 명세서 v2.3 정합도-v1 invitation-v1 6일차-수정본-v1.md` | `invitationStatus`, `invitationScopeType`, invitation endpoint와 예시 추가. member immediate create API를 Sprint 1.5 user-facing API에서 제외. invitation error code 통합 | #1, #4, #7, #9 |
| `화면정의서_v3 정합도-v1 invitation-v1 6일차-수정본-v1.md` | S14 구성원 추가를 invitation 생성, 상태 표시, accept 후 membership 생성 흐름으로 교정 | #1, #4, #7 |
| `기능-시나리오-API-ERD 추적표 v1.0 정합도-v1 invitation-v1 6일차-수정본-v1.md` | invitation 기능을 scenario, API, `invitations` ERD에 연결. Auth token trace에 `refresh_tokens` 포함 | #1, #2, #7 |
| `Definition of Done_작업 유형별 완료 조건 v2 정합도-v1 invitation-v1 6일차-수정본-v1.md` | Sprint 1 DB 범위에 `refresh_tokens` 추가. Sprint 1.5 invitation DoD 추가 | #3, #6, #7 |
| `Sprint1_Backlog 정합도-v1 invitation-v1 6일차-수정본-v1.md` | Sprint 1 Auth-only 유지. Sprint 1.5 invitation backlog, owner, timing, completion criteria 추가 | #3, #7 |
| `Sprint 1_detail 정합도-v1 invitation-v1 6일차-수정본-v1.md` | Sprint 1 Auth-only 유지. Sprint 1.5 invitation 상세 계획과 DoD-level 기준 추가 | #3, #7 |
| `Sprint1-백엔드-의존성-기술스택-DoD-v1.1 정합도-v1 invitation-v1 6일차-수정본-v1.md` | Sprint 1 인증 DB 4개 table 유지. Sprint 1.5 invitation Backend DB/API 범위와 DoD 추가 | #3, #7 |
| `기능 분해표 v3.0 정합도-v1 invitation-v1 6일차-수정본-v1.md` | 도메인 기능을 `MVP, 후속 sprint 구현`으로 정리. invitation 기능을 Sprint 1.5로 추가 | #5, #7 |

## 4. 닫은 검수리포트 이슈

| 이슈 | 처리 상태 |
|---|---|
| #1 invitation 메모와 본문 불일치 | ERD, API, 화면정의서, 추적표 본문에 invitation 흐름을 직접 반영했다 |
| #2 `refresh_tokens` ERD 누락 | ERD entity 목록, Mermaid, table detail에 반영하고 추적표 Auth trace에도 추가했다 |
| #3 Sprint 1.5 실행 계획 부재 | Backlog, Sprint 1_detail, Backend DoD, DoD 문서에 Sprint 1.5 계획과 완료 기준을 추가했다 |
| #4 invitation 만료 정책 충돌 | API 예시와 ERD, 화면, DoD에서 기본 7일 `expiresAt`과 lazy expiration 기준으로 통일했다 |
| #5 Sprint 1 범위 표현 불일치 | 기능 분해표 도메인 row를 `MVP, 후속 sprint 구현` 중심으로 교정하고 invitation은 Sprint 1.5로 분리했다 |
| #6 DoD DB 범위 `refresh_tokens` 누락 | Definition of Done Sprint 1 DB 범위에 `refresh_tokens`를 추가했다 |
| #7 metadata hygiene | 9개 수정본 metadata를 `최종 수정: 2026-06-17`, `버전: 6일차 수정본 v1`, `상태: 6일차 수정본`, `수정 근거`로 갱신했다 |
| #8 최종본 폴더의 미완 상태 문서 혼재 | 6일차 수정본 묶음의 대상 문서를 모두 `상태: 6일차 수정본`으로 통일하고, 원본 5일차 산출물과 구분되는 corrected copy로 보관한다 |
| #9 invitation error code 미통합 | API 공통 error code에 `ALREADY_COMPANY_MEMBER`, `ALREADY_PROJECT_MEMBER`, `INVITATION_ALREADY_PENDING`, `INVITATION_EXPIRED`, `INVITATION_CANCELED`를 추가했다 |

## 5. 남은 caveat

- 이 수정본은 기획 문서 정합성 보정본이다. 실제 schema migration, Backend 구현, Frontend 구현은 포함하지 않는다.
- Sprint 1.5는 계획과 DoD를 추가했지만, 실제 일정 확정과 issue 생성은 별도 팀 운영 작업이다.
- Actual email sending, token link invitation, unregistered-user invitation, signup auto-accept, resend, 필수 batch scheduler는 공식 결정에 따라 Sprint 1.5 범위에서 제외했다.
