# PR Template v1.0

> **작성자**: Threadia 팀
> **작성일**: 2026-06-15
> **최종 수정**: 2026-06-27
> **버전**: canonical 문서 변경이력 반영 v1
> **상태**: 확정
> **태그**: pr, github, branch, ci
> **수정 근거**: Threadia-기획문서-검수리포트.md, DB-schema-관리-정책.md
> **기준 문서**: PR_Template_체크항목_초안, 4일차 회의 결정, CONTRIBUTING.md

---

> **Phase A 정합도 메모**: 이 파일은 공식 결정 반영 조정본이다. Sprint 1 구현 기준은 Auth only, Spring Boot/MySQL/Frontend skeleton, CI/PR baseline, Auth flows다. DB schema는 `docs/03-design/ERD.md`와 `db/schema.sql`을 기준으로 관리하고, Spring Boot/JPA는 schema 생성이 아니라 mapping 검증에 사용한다. 회사, 프로젝트, 이슈, 문서, 채팅, DM, 쓰레드, 알림, 프로필 수정, invitation 구현은 Sprint 1 범위에서 제외하고, 전체 MVP API/ERD는 후속 구현 sprint의 reference contract로 유지한다.


## 1. 결정 사항

| 항목 | 결정 |
|---|---|
| PR Template 구성 | 작업 내용, 변경 유형, 검증, 보안/승인 체크 포함 |
| GitHub metadata | collaborator, author, co-author, reviewer, assignee에는 실제 팀원만 표시 |
| 검증 명령 | Backend는 `./gradlew test` 우선, 안정화 후 `./gradlew build` 확대 |
| 승인 필요 작업 | DB, 배포, 인증/인가, 보안 변경은 팀 승인 필요 |
| 구조 변경 | 선조치 후보고 가능하되 test/build 또는 직접 검증 필수 |
| canonical 문서 변경 | `docs/01-planning/` 원본 archive에서 승격된 canonical 확정 문서의 의미를 수정하면 `기획문서-변경이력.md` 링크를 PR에 포함 |
| Ruleset bypass | admin은 긴급 상황에만 사용, 부팀장 `cjfwls39`는 `dev` PR review 조건만 bypass 가능 |

---

## 2. PR Template 본문

```md
## 작업 내용

-

## 변경 유형

- [ ] 기능 추가
- [ ] 버그 수정
- [ ] 문서 수정
- [ ] 설정 변경
- [ ] 리팩터링
- [ ] 테스트 추가/수정

## 관련 문서/이슈

-

## 검증

- [ ] 관련 기능을 직접 실행했다.
- [ ] Backend 변경이면 `./gradlew test` 또는 `./gradlew build`를 실행했다.
- [ ] Frontend 변경이면 브라우저에서 화면 또는 route를 확인했다.
- [ ] API 변경이면 요청/응답 예시를 확인했다.
- [ ] DB 변경이면 ERD/API 문서와 `db/schema.sql` 영향을 확인했다.
- [ ] Auth 변경이면 email 정규화, OAuth link, refresh token revoke, ACTIVE 사용자 기준을 확인했다.
- [ ] Invitation 변경이면 seed 회사, accept 시 membership 생성, notification 분리, Sprint 1.5 제외 범위를 확인했다.
- [ ] canonical 확정 문서의 의미를 수정했다면 `docs/07-decisions/기획문서-변경이력.md` 변경 묶음 링크를 관련 문서/이슈에 추가했다.
- [ ] 실패한 test/build가 있으면 아래에 이유와 후속 처리를 적었다.

## 보안/승인 체크

- [ ] `.env`, API key, token, password, 개인정보가 포함되지 않았다.
- [ ] DB 변경이면 팀 승인 기준을 확인했다.
- [ ] 배포/인프라 변경이면 팀 승인 기준을 확인했다.
- [ ] 인증/인가/보안 변경이면 팀 승인 기준을 확인했다.
- [ ] GitHub Actions, ruleset, repository 권한 변경이면 팀 승인 기준을 확인했다.
- [ ] secret, OAuth client secret, SMTP credential, raw token이 PR 본문/로그/스크린샷에 노출되지 않았다.
- [ ] 대규모 구조 변경이면 변경 이유와 검증 결과를 설명했다.

## 추가 메모

-
```

---

## 3. 적용 위치

실제 repository 파일 생성 시 위치는 다음과 같다.

```text
.github/PULL_REQUEST_TEMPLATE.md
```

현재 실제 repository에는 `.github/PULL_REQUEST_TEMPLATE.md`가 생성되어 있다. 이 문서의 PR Template 본문과 실제 `.github/PULL_REQUEST_TEMPLATE.md`는 같은 체크 항목을 유지한다.

---

## 4. Branch ruleset 반영 기준

개발단계부터 `main`과 `dev`에는 GitHub repository ruleset을 적용한다.

| Branch | Ruleset 기준 |
|---|---|
| `main` | PR required, approval 1명, conversation resolution required, stale approval dismiss, last push approval, deletion 금지, non-fast-forward 금지 |
| `dev` | PR required, approval 1명, conversation resolution required, stale approval dismiss, last push approval, deletion 금지, non-fast-forward 금지 |
| `feature/*` | 별도 ruleset 없음. 담당자가 자유롭게 push 가능 |

`required status checks`는 GitHub Actions workflow가 생성된 뒤 활성화한다.

### 4.1 Ruleset bypass 운영 기준

Ruleset bypass는 팀 운영상 필요한 최소 예외로만 사용한다.

| 대상 | Bypass 범위 | 운영 기준 |
|---|---|---|
| Repository admin | `main`, `dev` ruleset bypass 가능 | 긴급 복구, repository 설정 정정, ruleset 오류 수정처럼 즉시 조치가 필요한 경우에만 사용한다. 일반 기능 개발은 PR 기준을 따른다. |
| 부팀장 `cjfwls39` | `dev` PR의 review 조건 bypass 가능 | PR은 반드시 생성한다. 팀장 부재, 마감 직전 문서/설정 정정처럼 review 대기가 blocker일 때만 사용하고, PR 본문 또는 comment에 사유를 남긴다. |
| 일반 write 권한 팀원 | bypass 없음 | `feature/*`, `fix/*`, `docs/*`에서 `dev`로 PR을 생성하고 다른 팀원 approval 1개 이상을 받는다. |

부팀장 bypass는 `pull_request` mode로 운영한다. 따라서 `dev` 직접 push, branch 삭제, non-fast-forward push를 허용하는 예외가 아니다.

---

## 5. 6일차 수정본 v2 공통 기준

1. Sprint 1.5 invitation 검증은 local/dev/demo seed 회사 1개와 `COMPANY_ADMIN` 1명을 사용한다. `POST /api/v1/companies`는 Sprint 1.5 구현 범위에 포함하지 않는다.
2. `activity_events`와 `notification_events`는 통합하지 않고 각각 활동 원장과 알림 사건 원본으로 별도 운영한다.
3. 서버 생성 시각과 만료 시각은 UTC 기준으로 생성, 저장, 비교하고 API 응답은 ISO-8601 UTC `Z` 형식으로 반환한다.
4. 로그인 이메일은 signup, login, password reset, OAuth 계정 연결, invitation lookup에서 모두 `trim + lowercase`로 정규화한다.
5. OAuth 계정은 이메일만 같다는 이유로 blind auto-link하지 않는다.
6. `password-reset/confirm` 성공 시 해당 사용자의 모든 기존 `refresh_tokens`를 revoke한다.
7. login, OAuth callback, token refresh, `/auth/me`는 `users.status = ACTIVE`인 사용자만 성공 처리한다.
