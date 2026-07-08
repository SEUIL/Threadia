> **작성자**: 김민정
> **작성일**: 2026-07-07
> **최종 수정**: 2026-07-08
> **버전**: v1.2
> **상태**: 검토 중
> **태그**: sprint-1.5, wbs-2.6, invitation, dod, qa

# Sprint 1.5 WBS 2.6 QA Gate 결과

이 문서는 WBS 2.6("Sprint 1.5 문서와 DoD 동기화, QA gate")의 완료 조건 확인 결과다. WBS 2.6의 완료 조건은 "API, ERD, 화면, 추적표, DoD가 아래 8개 기준(§2)과 충돌하지 않음"이며, 선행 조건은 Sprint 1.5 invitation 작업(WBS 2.1~2.5) 전체 완료다.

**기준 문서**: `docs/00-project/sprint-운영/sprint-1-backlog.md`(작업 묶음 1-B, Phase B/Phase C 결정), `docs/00-project/Definition-of-Done.md`, `docs/00-project/검증-방법.md`

---

## 결론

**8개 완료 조건(§2) 전부 충돌 없음, 제외 범위(§3) 6개 전부 준수, 화면 표시(§4) 4개 항목 전부 정상 확인.** Sprint 1.5 invitation 기능은 QA gate 기준을 통과한 것으로 판단한다. Reviewer 확인 후 merge하면 WBS 2.6을 완료 처리할 수 있다.

Sprint 1.5 범위와 무관한 후속 항목 2건은 §6에 별도로 남겨뒀다(이 문서의 완료 판정을 막지 않음).

---

## 1. 선행 조건(WBS 2.1~2.5) 완료 현황

각 PR의 review 기록을 GitHub API로 직접 확인했다. review 유무와 review에서 실제로 어떤 문제가 나왔고 어떻게 처리됐는지까지 정리한다.

### 2.1 — `invitations` DB/entity와 seed 검증 기반 구현 (Owner 여도현, PR #42)

- **review 기록 없음.** 부팀장(cjfwls39)이 merge했고, PR-Template.md 기준 bypass 사유가 PR 본문/comment에 남아있지 않다.
- PR 본문에 저자가 스스로 "`JwtTokenServiceImplTest`의 tampered token 테스트가 flaky하게 실패할 수 있으며, 이번 WBS 2.1 변경과 무관한 기존 이슈"라고 기록해뒀다. 이는 이번 문서 §6에 정리한 김태준 발견 건과 동일한 이슈이며, 최소 2026-07-07부터 이미 알려져 있었다.
- 산출물: `Invitation`/`InvitationScopeType`/`InvitationStatus` 도메인, `InvitationRepository`, `InvitationSeedValidationTests`, `InvitationEntityMappingTests`.

### 2.2 — 등록 사용자 exact email lookup API 구현 (Owner 김민경, PR #26)

- **review 기록 있음** (COMMENTED → DISMISSED → APPROVED).
- 1차 리뷰에서 **`GET /users/lookup`이 `SecurityConfig`상 `permitAll`로 열려 있어 인증 없이 호출 가능한 email enumeration 위험**을 지적받음.
- 후속 커밋에서 Bearer token + `ACTIVE` 사용자 + `ACTIVE COMPANY_ADMIN` 권한 검증을 추가했고, 리뷰어가 anonymous 401 / non-admin 403 / admin 200 / 미가입 404 흐름을 직접 재검증한 뒤 approve.
- 현재 `UserLookupService.validateRequester`가 COMPANY_ADMIN 권한 검사를 포함하고 있는 것과 일치한다(본 문서 §2 기준 4, 7과 연결).

### 2.3 — 회사 invitation 생성/목록 API 구현 (Owner 김태준, PR #47)

- **formal review 기록 없음**(review API상 0건). 부팀장(cjfwls39)이 merge했고, 마찬가지로 bypass 사유 기록은 없다.
- 다만 **PR 댓글로 실질적인 코드 리뷰가 남아있다**: `CompanyInvitationService.assertInviteeCanBeInvited`가 상태 필터 없는 `existsByCompany_IdAndUser_Id`를 써서, 향후 탈퇴(`LEFT`) 기능이 생기면 한 번 탈퇴한 사람은 영구히 재초대가 불가능해지는 잠재 버그를 지적받음.
- 이 지적은 반영되어, 현재 코드는 `existsByCompany_IdAndUser_IdAndStatus(..., ACTIVE)`로 상태 필터가 걸려 있다(`CompanyInvitationService.java:268-271`). 이번 QA gate에서 Apidog으로 직접 재초대(member1 CANCELED → 재초대 PENDING)를 실행해 실제로 정상 동작하는 것까지 확인했다(§5 검증 방법 참고).

### 2.4 — 내 invitation 목록과 accept/decline/cancel 구현 (Owner 여도현, PR #48)

- **review 기록 있음** (COMMENTED → APPROVED).
- 리뷰에서 두 가지를 지적받음: (1) 만료된 `PENDING` 초대가 lazy expiration 전이라 재초대를 막을 수 있음, (2) accept 시 `LEFT`와 `SUSPENDED`가 구분 없이 둘 다 재활성화될 수 있음.
- 둘 다 후속 커밋에서 반영되어 재검토 후 approve. 현재 정책(`SUSPENDED`는 `COMPANY_MEMBER_SUSPENDED`로 차단, `LEFT`만 재활성화)과 정확히 일치하며, 본 문서 작성 과정에서 이 두 케이스를 3.1.5 검증 문서의 케이스 19-A/19-B로 별도 추가해뒀다.

### 2.5 — S14 구성원·권한 관리 화면 invitation 연결 (Owner 정철진, PR #55)

- **review 기록 있음** (Reviewer: 김민정, 본 QA gate 작성자).
- 리뷰 중 확정 버그 1건(취소 실패 시 에러 미표시)을 발견 → 수정 커밋에서 새로운 회귀(에러 표시 시 목록 전체 소실)를 재발견 → 로컬 backend + Apidog + 브라우저로 재현 절차를 만들어 재검증 → 최종 수정 확인 후 approve.
- 상세 이력은 PR #55 코멘트에 남아있다.

**요약**: 5개 PR 중 2개(#42, #47)는 GitHub review 기록 없이 부팀장 bypass로 merge됐다. 다만 #47은 실질적인 review 코멘트가 있었고 반영도 됐다. #42는 review 자체가 없지만 문제가 될 만한 부분은 이번 QA gate에서 별도로 확인했다(§2, §3). 나머지 3개(#26, #48, #55)는 실제 review에서 문제를 찾아 수정까지 반영된 정상적인 리뷰 사이클을 거쳤다.

---

## 2. 완료 조건 8개 확인 결과

| # | 기준 | 결과 | 근거 |
|---|---|---|---|
| 1 | seed bootstrap: local/dev/demo 회사 1개 + `COMPANY_ADMIN` 1명 준비 가능. `POST /api/v1/companies`는 Sprint 1.5 기능 구현 범위 아님 | 충돌 없음 | 저장소에 committed seed script는 없지만, QA 목적 전용 절차를 확립했다: signup 2~3계정 → login(Apidog 후처리기로 `accessToken` 자동 저장) → `POST /api/v1/companies`(MVP reference 경로, QA 시드 생성 용도로만 사용하고 운영 데이터 아님) → lookup → invitation 생성. Apidog 폴더/명명 규칙까지 정리함(`Company-RBAC` 폴더 등) |
| 2 | notification 분리: `activity_events`/`notification_events` 별도 운영, invitation 관련 event는 QA gate 필수 아님 | 충돌 없음 | notification 코드/테이블이 아직 구현되지 않아 invitation 코드가 건드릴 대상 자체가 없음 (`backend/.../invitation/`에 notification 참조 0건) |
| 3 | UTC 시각 처리: `expiresAt` 등 서버 생성/저장/비교는 UTC, API 응답은 ISO-8601 `Z` | 충돌 없음 | `CompanyInvitationService`가 `Clock.systemUTC()`로 생성, `formatUtc()`가 `DateTimeFormatter.ISO_OFFSET_DATE_TIME`으로 UTC를 `Z`로 직렬화한다(`CompanyInvitationService.java:305-306`). Apidog 실측 결과 `POST /api/v1/companies/{companyId}/invitations` 응답의 `expiresAt`이 `"2026-07-15T05:05:15.6966851Z"`로 반환됨을 확인했다. ISO-8601 형식이며 UTC `Z` suffix를 포함한다. |
| 4 | email 정규화: `trim + lowercase`, signup/login/password-reset/OAuth/invitation lookup 전체 동일 기준 | 충돌 없음 | `UserLookupService.lookupByEmail`이 Auth와 동일한 `EmailNormalizer.normalize()`를 그대로 재사용한다(`UserLookupService.java:32`). Apidog 실측 결과 `GET /api/v1/users/lookup?email=...`에 `  Qa-MeMbEr@TeSt.CoM  `처럼 앞뒤 공백과 대소문자를 섞은 email을 전달해도 `200 OK`로 조회됐고, 응답의 `data.user.email`이 정규화된 lowercase email로 반환됨을 확인했다. 예: 요청 `  Qa-MeMbEr@TeSt.CoM  ` → 응답 `data.user.email: "qa-member@test.com"`. |
| 5 | OAuth 계정 연결: blind auto-link 금지 기준 유지 | 충돌 없음 | invitation 패키지에 OAuth/`oauth_accounts` 참조 0건, 기존 OAuth 로직 미변경 |
| 6 | refresh token revoke 기준: invitation 기능이 기존 revoke 정책과 충돌하지 않음 | 충돌 없음 | invitation 패키지에 `RefreshToken`/`refresh_token` 참조 0건, accept/decline/cancel이 refresh token을 건드리지 않음 |
| 7 | ACTIVE 사용자 기준: invitation 조회·accept도 `ACTIVE` 사용자만 허용 | 충돌 없음 | `activeRequester`/`activeInvitee`가 `UserStatus.ACTIVE` 필터를 강제(`CompanyInvitationService.java:192-196, 261-264`), lookup도 동일 필터 적용. Apidog 실측: DB에서 `qa-member@test.com` 계정의 `users.status`를 `SUSPENDED`로 직접 변경한 뒤, 대소문자·공백을 섞은 ` Qa-member@test.com`으로 lookup 요청 → `404 NOT_FOUND` 확인. 정규화(#4)가 먼저 적용된 뒤 ACTIVE 필터에서 거부되는 순서까지 실측으로 함께 확인됨 |
| 8 | ERD/추적표 정합성: `invitations` table과 `users`/`companies`/`company_members` 관계 | 충돌 없음 | `db/schema.sql`의 `invitations` table(FK 4개, `chk_inv_scope`/`chk_inv_self`/`chk_inv_role`/`chk_inv_expiry` CHECK 포함)이 ERD 설계와 일치 |

---

## 3. Sprint 1.5 제외 범위 준수 확인

| 제외 항목 | 결과 | 근거 |
|---|---|---|
| project invitation, project membership creation | 준수 | backend invitation 패키지에 `project_members`/`ProjectMember` 참조 0건 |
| 실제 invitee 이메일 발송 | 준수 | `MailSender`/`SimpleMailMessage`/smtp 참조 0건 |
| invitation token link | 준수 | `POST /invitations/{invitationId}/accept`가 인증된 세션 + DB PK 기준, 비로그인 token link 아님 |
| 미가입 사용자 초대, signup auto-accept | 준수 | `UserLookupService`가 미등록 이메일에 `UserNotFoundException`만 던지고 자동 생성/가입 로직 없음 |
| resend | 준수 | `resend` 관련 endpoint/메서드 0건 |
| 필수 batch scheduler | 준수 | `@Scheduled`/`@EnableScheduling` 전체 backend에서 0건 |

---

## 4. 화면 표시 검증 (S14, PR #55 리뷰 기준)

| 확인 항목 | 결과 | 근거 |
|---|---|---|
| Invitation 상태 5종(PENDING/ACCEPTED/DECLINED/CANCELED/EXPIRED) 표시 | 충돌 없음 | `frontend/lib/invitation.ts`의 `INVITATION_STATUS_LABEL`이 화면정의서 §S14 상태 표와 1:1 일치, 브라우저로 "대기 중"/"취소됨" 실제 렌더 확인 |
| 취소 실패 시 에러 문구 노출 | 충돌 없음 | 최초 구현에서 에러 표시 시 초대 목록 전체가 사라지는 회귀를 브라우저로 재현해 리포트, 후속 커밋에서 에러 상태를 목록 상태와 분리해 수정. "이미 취소된 초대입니다." 문구가 목록이 유지된 채 정상 노출되는 것까지 재검증 |
| accept 후 재로그인 안내 | 충돌 없음 | Sprint 1 백엔드가 로그인 응답의 `companies`를 항상 빈 배열로 반환하는 제약 때문에, accept 직후에도 새 회사가 세션에 즉시 반영되지 않는다. 리뷰 요청으로 화면에 안내 배너를 추가해 확인 |
| nav 진입점("받은 초대" 배지) 갱신 | 충돌 없음 | 라우트 이동에 따른 컴포넌트 unmount/remount 시점에 정상 갱신되는 것을 브라우저로 확인 |

---

## 5. 검증 방법

- backend 8개 기준(§2)과 제외 범위(§3)는 기본적으로 소스 코드 정적 분석과 각 PR의 review 기록 확인(§1)으로 진행했다. 그중 #3(UTC), #4(email 정규화), #7(ACTIVE 기준)은 Apidog + DB 직접 조작으로 실측까지 완료했다(§2 근거란 참고).
- 화면 표시 검증(§4)은 로컬 backend(Docker MySQL) + Apidog으로 실제 초대 데이터를 생성하고, 브라우저로 정상/실패 케이스를 재현해 확인했다. 계정 3개(admin/member1/member2), 초대 3건을 만들어 PENDING/CANCELED 상태를 모두 재현했다.
- 이 과정에서 **member1(한 번 취소된 상대)을 다시 초대하는 흐름도 실제로 성공하는 것을 확인**했다 — §1의 PR #47 review에서 지적된 "상태 필터 누락" 수정이 실제로 반영돼 있다는 실증 근거다.
- 관련 PR: #42, #26, #47, #48, #55.

---

## 6. Sprint 1.5 범위 밖 후속 항목 (이 문서의 완료 판정과 무관)

- **seed bootstrap**: §2-1에서 확립한 Apidog 기반 QA 데이터 생성 절차는 본인이 사용한 방식이다. 원하는 팀원이 있으면 추후 Apidog 프로젝트에 초대해 공유할 예정이다.
- **JWT 변조 테스트 flaky 이슈**: PR #42(2026-07-07, WBS 2.1) 본문에 저자가 이미 "기존 이슈, 이번 변경과 무관"이라고 기록해뒀고, 김태준이 WBS 3.3.1 작업 중(2026-07-08) 독립적으로 재발견했다. `JwtTokenServiceImplTest`의 변조 토큰 테스트가 토큰 마지막 1글자만 바꾸는 방식인데, Base64URL 인코딩 특성상 마지막 문자를 바꿔도 decode된 signature bytes가 동일한 경우가 있어 간헐적으로 실패한다. 실제 서명 검증 로직(JJWT 기반 HMAC 검증) 자체는 정상이며, invitation 기능이나 이 QA gate 판정에는 영향이 없다. 두 차례 독립적으로 발견된 만큼 우선순위를 올려 Auth/JWT 테스트 인프라 개선 항목으로 별도 트래킹이 필요하다.
