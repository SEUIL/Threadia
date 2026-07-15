# AI 사용 규칙 v1.0

> **작성자**: Threadia 팀
> **작성일**: 2026-06-15
> **최종 수정**: 2026-06-17
> **버전**: 6일차 수정본 v2
> **상태**: 6일차 수정본
> **수정 근거**: Threadia-기획문서-검수리포트.md
> **기준 문서**: AI_사용_규칙_초안, 협업_하네스_최소_규칙_초안, 4일차 회의 결정

---

> **Phase A 정합도 메모**: 이 파일은 공식 결정 반영 조정본이다. Sprint 1 구현 기준은 Auth only, Spring Boot/MySQL/Frontend skeleton, CI/PR baseline, Auth flows이며, Sprint 1 DB migration은 `users`, `oauth_accounts`, `password_reset_tokens`, `refresh_tokens`만 포함한다. 회사, 프로젝트, 이슈, 문서, 채팅, DM, 쓰레드, 알림, 프로필 수정, invitation 구현은 Sprint 1 범위에서 제외하고, 전체 MVP API/ERD는 후속 구현 sprint의 reference contract로 유지한다.


## 1. 최종 규칙

| 규칙 | 최종 문장 |
|---|---|
| AI 사용 공개 | PR에서 AI를 사용했다면 사용 여부와 범위를 적는다. |
| secret 입력 금지 | `.env`, API key, token, password, 개인정보, 비공개 계정 정보는 AI 도구에 입력하지 않는다. |
| 사람 책임 | AI가 만든 코드, 문서, 설정은 작성자가 직접 이해하고 검토한 뒤 제출한다. |
| 검증 책임 | AI가 만들었더라도 test, build, 화면 확인, API 확인 같은 검증 책임은 PR 작성자에게 있다. |
| GitHub metadata | AI 도구나 agent 이름을 GitHub collaborator, commit author, co-author, reviewer, assignee로 남기지 않는다. |
| 승인 필요 작업 | DB, 배포, 인증/인가, 보안 변경은 AI 제안만으로 진행하지 않고 팀 승인 기준을 따른다. |
| 인증/초대 정책 검증 | AI가 제안한 Auth, token, OAuth, invitation 변경은 6일차 수정본 v2 공통 기준과 충돌하지 않는지 사람이 확인한다. |
| 구조 변경 | 대규모 구조 변경은 선조치 후보고가 가능하더라도 테스트와 변경 이유 설명을 반드시 남긴다. |

---

## 2. PR 기재 기준

```md
## AI 사용 여부

- 사용 여부: 예
- 사용 범위: 문서 초안 정리, test case 후보 정리, 코드 초안 작성 등
- 사람 검토: 작성자가 코드와 문서를 직접 이해하고 확인함
```

AI 사용을 숨기지 않는다. 단, GitHub 계정 표시나 commit metadata에 AI 도구를 남기지 않고 PR 본문에만 사용 범위를 적는다.

---

## 3. 금지 입력

- `.env` 전체 내용.
- API key, token, password, DB 접속 정보.
- 팀원 개인정보, 비공개 계정 정보.
- 외부 서비스 계정 정보.
- 공개하면 안 되는 수업/기관 내부 자료.

---

## 4. 승인 필요 작업

| 작업 | 기준 |
|---|---|
| DB 변경 | ERD/migration 영향 설명 후 팀 승인 |
| 배포/인프라 변경 | AWS, GitHub Actions, domain, secret 변경 전 팀 승인 |
| 인증/인가 변경 | JWT, OAuth, 권한 정책 변경 전 팀 승인 |
| 보안 변경 | 민감정보, 암호화, 권한 우회 가능성 검토 후 승인 |
| 대규모 구조 변경 | 변경 이유, 영향 범위, 테스트 결과를 PR에 명시 |

---

## 5. 팀 책임 원칙

AI는 작성 보조 도구다. 최종 책임은 PR 작성자와 리뷰어에게 있다. AI가 만든 코드나 문서를 이해하지 못한 상태로 제출하지 않는다.

---

## 6. 6일차 수정본 v2 공통 기준

1. Sprint 1.5 invitation 검증은 local/dev/demo seed 회사 1개와 `COMPANY_ADMIN` 1명을 사용한다. `POST /api/v1/companies`는 Sprint 1.5 구현 범위에 포함하지 않는다.
2. `activity_events`와 `notification_events`는 통합하지 않고 각각 활동 원장과 알림 사건 원본으로 별도 운영한다.
3. 서버 생성 시각과 만료 시각은 UTC 기준으로 생성, 저장, 비교하고 API 응답은 ISO-8601 UTC `Z` 형식으로 반환한다.
4. 로그인 이메일은 signup, login, password reset, OAuth 계정 연결, invitation lookup에서 모두 `trim + lowercase`로 정규화한다.
5. OAuth 계정은 이메일만 같다는 이유로 blind auto-link하지 않는다.
6. `password-reset/confirm` 성공 시 해당 사용자의 모든 기존 `refresh_tokens`를 revoke한다.
7. login, OAuth callback, token refresh, `/auth/me`는 `users.status = ACTIVE`인 사용자만 성공 처리한다.
