> **작성일**: 2026-06-17
> **작성자**: 문서 검수
> **대상**: `Threadia/docs/01-planning/기획/05_기획주간_5일차_자료/최종 산출물/` 13종 + invitation 원안
> **목적**: 최종 산출물 간 정합성, 충돌, 누락 점검

# Threadia 기획 문서 검수 리포트

## 0. 검수 범위

다음 문서를 교차 대조했다.

| 분류 | 문서 |
|---|---|
| 기능 | 기능 분해표 v3.0, 보류 기능 최종표 v1.0 |
| 데이터 | ERD v2.0 |
| API | API 명세서 v2.3 |
| 화면 | 화면정의서 v3 |
| 흐름 | 사용자 시나리오 v3.0 |
| 추적 | 기능-시나리오-API-ERD 추적표 v1.0 |
| 계획 | Sprint 1_detail, Sprint1_Backlog, Sprint1 백엔드 의존성/DoD v1.1 |
| 완료 기준 | Definition of Done v2 |
| invitation | Invitation 테이블 추가안 (원안, 상태: 제안) |

심각도 기준: 🔴 개발을 막는 구멍 / 🟡 합의·보강 필요 / 🟢 문서 위생·메타

---

## 🔴 심각 — 즉시 정리 필요

### 이슈 1. invitation "반영 완료" 표기가 사실과 다름

모든 최종 산출물 하단의 *Phase B invitation 메모*는 **"상태: 공식 결정 반영 완료"**라고 적혀 있으나, 문서 본문은 invitation을 전혀 반영하지 않았다.

| 문서 | 메모 주장 | 실제 본문 상태 |
|---|---|---|
| ERD v2.0 | invitation 반영 완료 | §2 엔티티 목록·§4 테이블 상세에 **`invitations` 테이블 없음** |
| API 명세서 v2.3 | 반영 완료 | §5에 여전히 `POST .../company-members`(즉시 등록), **"미가입 이메일 → 400 VALIDATION_ERROR"** 옛 정책 유지. invitation 엔드포인트·`invitationStatus`/`invitationScopeType` enum **전부 없음** |
| 화면정의서 S14 | 반영 완료 | "**Invitation 테이블 없음. 미가입 이메일은 400 반환**", "구성원 추가 정책 (즉시 등록)" |
| 추적표 §4 | 반영 완료 | "Invitation 테이블 없이 가입 사용자 즉시 등록" |

**결정적 근거**: 원안 `Invitation 테이블 추가안.md` §11·§6.3은 *"현재 API 명세서 원본은 건드리지 않았다. 합의 전까지 변경 내용은 이 문서에만 둔다. 팀 합의·별도 승인 후 ERD/API에 반영한다(두 문서 모두 상태 확정)"*고 명시한다. 즉 실제로는 **합의 대기 중**인데 최종본 메모만 "반영 완료"로 적혀 있어 상태를 오도한다.

**영향**: 본문(즉시 등록)과 메모(invitation 경유)가 정면으로 충돌. 구현자가 어느 정책을 따를지 알 수 없다.

**권고**: 메모 문구를 "결정됨, 본문 미반영(합의·승인 후 반영 예정)"으로 정정하거나, 실제로 ERD/API/화면/추적표 본문에 invitation을 반영한다(승인 필요).

---

### 이슈 2. `refresh_tokens` 테이블이 ERD에 정의되지 않음

모든 문서의 Phase A 메모와 Backlog·백엔드 DoD가 **Sprint 1 필수 4개 테이블**로 `users, oauth_accounts, password_reset_tokens, refresh_tokens`를 명시하지만, ERD v2.0에는:

- §2 엔티티 목록에 `refresh_tokens` **없음**
- §4 테이블 상세 스키마 **없음** (users / oauth_accounts / password_reset_tokens / activity_events / project_dashboard_summaries만 존재)
- mermaid 다이어그램에도 **없음**

JWT refresh 정책(hash 저장·rotation·14일 만료)은 API §3.7/§12에 산문으로만 있고 **컬럼 스키마가 어디에도 없다.**

**영향**: Day 2 "인증 JPA Entity 구현" 담당(여도현+김태준+김민경)이 스키마 없이 `refresh_tokens` 엔티티를 만들어야 한다.

**권고**: ERD §2/§4에 `refresh_tokens` 테이블(예: `id`, `user_id` FK, `token_hash` UNIQUE, `expires_at`, `rotated_at`/`revoked_at`, `created_at`)을 추가한다.

---

## 🟡 중간 — 합의/보강 필요

### 이슈 3. Sprint 1.5(invitation) 실행 계획 부재

invitation 메모는 "Sprint 1.5"라는 단계를 새로 도입하지만, Backlog·Sprint 1_detail·백엔드 DoD 어디에도 **Sprint 1.5의 백로그·일정·담당자·DoD가 없다.** 정책만 있고 실행 문서가 비어 있다.

**권고**: Sprint 1.5 백로그와 담당/일정/DoD를 별도 문서로 작성하거나, 최소한 "Sprint 1.5 실행 계획 미작성" 상태를 명시한다.

---

### 이슈 4. invitation 만료 정책이 원안과 충돌

| 출처 | 내용 |
|---|---|
| 최종본 Phase B 메모 | "기본 만료 기간 **7일**", "만료된 PENDING accept는 `409 INVITATION_EXPIRED`" |
| 원안 §4 / 응답 예시 | `expires_at DATETIME, NULL -- Sprint 1은 nullable 허용`, 예시 `"expiresAt": null` |

기본 7일 설정인지 nullable인지 **상충**. 원안이 공식 결정보다 먼저 작성되어 동기화되지 않았다.

**권고**: 공식 결정(7일)을 기준으로 원안 §4와 예시의 `expiresAt`을 정정한다.

---

### 이슈 5. Sprint 1 범위 축소의 잔여 불일치

기능 분해표 §5 우선순위 칸은 도메인 기능(회사/프로젝트/이슈/문서/채팅/DM/쓰레드/알림)을 여전히 "MVP / Sprint 1 확정"으로 표기하나, Backlog는 이를 전부 "Sprint 1 제외 → 다음 스프린트"로 이동했다. 상단 Phase A 메모로 "Auth only"라고 덮어쓰긴 했지만, **표 셀 값과 메모가 겉으로 충돌**한다.

Backlog 자신도 *"이 범위 축소는 팀 회의에서 공식 합의하고 6일차 보고에 반영해야 한다 / 합의 없이 진행하면 6일차 보고와 실제 진행이 어긋난다"*고 미해결로 표시.

**권고**: 6일차 회의에서 범위 축소를 공식 합의했는지 확인하고, 합의됐다면 기능 분해표 우선순위 칸을 "Sprint 1 / 후속 sprint"로 명확히 갱신한다.

---

### 이슈 6. DoD 문서의 DB 범위에서 `refresh_tokens` 누락

`Definition of Done v2` §2는 DB 범위를 `users, oauth_accounts, password_reset_tokens` **3개로만** 적어 `refresh_tokens`가 빠졌다. Backlog·백엔드 DoD(4개)와 불일치.

**권고**: DoD §2 DB 행에 `refresh_tokens` 추가.

---

## 🟢 경미 — 문서 위생/메타

### 이슈 7. "확정" 문서를 수정하고도 메타 미갱신

ERD·API 등은 `상태: 확정`, `최종 수정: 2026-06-15`인데 본문에 **2026-06-16자 invitation 메모**가 추가됐다. 문서작성규칙·docs/CLAUDE.md의 *"확정 문서 수정 시 최종 수정일·버전 갱신"* 원칙 위반. 또한 *"상태 확정 문서는 사용자 확인 없이 수정 금지"* 규칙과도 충돌 소지.

**권고**: invitation 메모를 본문에 유지한다면 최종 수정일·버전을 갱신한다.

---

### 이슈 8. "최종 산출물"인데 상태가 미완인 문서 혼재

| 문서 | 상태 |
|---|---|
| Definition of Done v2 | 검토 중 |
| Sprint1 백엔드 의존성/DoD v1.1 | 작성 중 |
| Invitation 추가안 (원안) | 제안 |

최종본 폴더에 있으나 상태값이 최종이 아니다.

**권고**: 상태를 "확정"으로 올리거나, 폴더/문서명으로 초안임을 구분한다.

---

### 이슈 9. invitation 예외코드 미통합

원안의 `ALREADY_PROJECT_MEMBER`, `INVITATION_ALREADY_PENDING`, `INVITATION_EXPIRED`, `INVITATION_CANCELED`가 API 공통 에러코드표에 없다(이슈 1과 동일 뿌리).

**권고**: invitation 본문 반영 시 공통 에러코드표에 함께 추가.

---

## ✅ 정합성이 양호한 부분

- **권한 모델**: `users.role` 금지 → `company_members.role`/`project_members.role` 판정. ERD·API·화면·추적표·Backlog 전반에서 완벽히 일관.
- **삭제 정책**: 이슈/문서 휴지통·복원, 프로젝트 archive, 메시지/쓰레드 댓글 삭제 불가 — 문서 간 모순 없음.
- **기타 불변식**: DM 1:1 unique(`user1_id < user2_id`), 칸반 5컬럼 고정, 알림 GLOBAL scope only, Redis는 cache/push 보조만 — 일관.
- **추적표**: 기능 ID↔시나리오↔API↔ERD 매핑이 체계적이라 교차검증 신뢰도가 높다.

---

## 핵심 진단

개별 문서의 품질과 정합성은 높다. 다만 실제 개발을 막는 두 구멍이 우선 과제다.

1. **이슈 1** — invitation을 "메모만 붙이고 본문 미반영"한 상태에서 '반영 완료'로 표기 → 본문(즉시 등록) vs 메모(invitation 경유) 정면 충돌.
2. **이슈 2** — Sprint 1 필수 테이블인 `refresh_tokens`의 스키마가 ERD에 없음.

이 둘을 먼저 닫고, 나머지(🟡/🟢)는 6일차 회의 합의 결과와 함께 정리하는 순서를 권고한다.

---

## 이슈 요약표

| # | 심각도 | 이슈 | 관련 문서 |
|---|---|---|---|
| 1 | 🔴 | invitation "반영 완료" 표기가 본문과 불일치 | ERD, API, 화면정의서, 추적표 |
| 2 | 🔴 | `refresh_tokens` 테이블 스키마 부재 | ERD v2.0 |
| 3 | 🟡 | Sprint 1.5 실행 계획(백로그/일정/DoD) 부재 | Backlog, Sprint 1_detail |
| 4 | 🟡 | invitation 만료 정책 충돌(7일 vs nullable) | invitation 원안 ↔ Phase B 메모 |
| 5 | 🟡 | Sprint 1 범위 축소의 표/메모 불일치 | 기능 분해표 ↔ Backlog |
| 6 | 🟡 | DoD DB 범위에서 `refresh_tokens` 누락 | Definition of Done v2 |
| 7 | 🟢 | 확정 문서 수정 후 메타(최종수정일/버전) 미갱신 | ERD, API 등 |
| 8 | 🟢 | 최종본 폴더에 미완 상태 문서 혼재 | DoD, 백엔드 DoD, invitation 원안 |
| 9 | 🟢 | invitation 예외코드 공통표 미통합 | API 명세서 v2.3 |
