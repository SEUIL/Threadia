# DB schema 변경이력

> **작성자**: Threadia 팀
> **작성일**: 2026-06-27
> **최종 수정**: 2026-07-18
> **버전**: v1.6
> **상태**: 확정
> **태그**: decision, db, schema, changelog

---

## 1. 목적

이 문서는 Threadia DB schema 변경 이력을 기록한다.

Sprint 1 개발 단계에서는 Flyway/Liquibase migration tool을 사용하지 않고, `docs/03-design/ERD.md`와 `db/schema.sql`을 기준으로 DB schema를 관리한다. 따라서 schema 변경 이유와 영향 범위는 이 문서에 남긴다.

---

## 2. 기록 원칙

DB schema가 바뀌면 아래 항목을 기록한다.

| 항목 | 설명 |
|---|---|
| 변경일 | schema 변경을 반영한 날짜 |
| 변경 대상 | table, column, index, foreign key, seed 기준 등 |
| 변경 내용 | 추가, 수정, 삭제 내용 |
| 변경 이유 | 기능 요구사항 또는 설계 변경 이유 |
| 영향 범위 | API, Entity, 화면, 테스트, seed data 영향 |
| 관련 문서 | ERD, API 명세서, 기능 분해표, Sprint 문서 |

---

## 3. 변경 이력

### 2026-07-18 WBS 4.10.4 audit read keyset index 추가

| 항목 | 내용 |
|---|---|
| 변경 대상 | `admin_action_audits.idx_aaa_created (created_at, id)` |
| 변경 내용 | platform audit read의 descending keyset pagination을 위한 created-at/id 복합 index를 clean-rebuild schema에 추가했다. |
| 변경 이유 | offset 없이 안정적인 `(created_at DESC, id DESC)` audit page 조회를 지원하기 위해서다. |
| 영향 범위 | local DB는 clean drop/recreate 후 `db/schema.sql`을 재적용한다. migration, legacy data sanitation, cleanup/purge/archive job은 추가하지 않는다. |
| 관련 문서 | `docs/03-design/ERD.md`, `docs/04-api/API-명세서.md`, `docs/07-decisions/기획문서-변경이력.md` |

### 2026-07-18 WBS 4.10.4 platform admin lifecycle schema 확장

| 항목 | 내용 |
|---|---|
| 변경 대상 | `platform_admins`, `admin_action_audits.action`, `admin_action_audits.target_type` |
| 변경 내용 | `platform_admins`에 `ACTIVE/REVOKED` 상태, revoke/reactivate/updated UTC 시각, status-role index와 상태-시각 CHECK를 추가했다. audit enum에 lifecycle action 4종과 `PLATFORM_ADMIN` target을 추가했다. |
| 변경 이유 | soft revoke로 권한 이력을 보존하면서 ACTIVE-only authorization과 grant/revoke/reactivate/role-change의 immutable audit를 지원하기 위해서다. |
| 영향 범위 | local DB는 clean drop/recreate 후 `db/schema.sql`을 재적용한다. migration tool, migration history, additive production DDL, retention/cleanup job은 추가하지 않는다. |
| 관련 문서 | `docs/03-design/ERD.md`, `docs/04-api/API-명세서.md`, `docs/07-decisions/기획문서-변경이력.md` |

### 2026-07-16 WBS 4.10.3 system settings typed catalog schema 추가

| 항목 | 내용 |
|---|---|
| 변경 대상 | `system_settings`, `admin_action_audits.action`, `admin_action_audits.target_type`, clean-rebuild seed |
| 변경 내용 | surrogate BIGINT `id`, unique `setting_key`, JSON value, `value_schema_version`, optimistic `version`, UTC timestamp와 positive version CHECK를 가진 `system_settings`를 추가했다. policy의 8개 canonical key를 schema version 1 minimal JSON으로 seed하며, audit enum에 `SYSTEM_SETTING_REPLACED`/`SYSTEM_SETTING`을 추가했다. |
| 변경 이유 | WBS `4.10.3`의 code-owned typed operational settings catalog를 arbitrary settings CRUD 없이 whole-document replacement와 immutable before/after audit으로 구현하기 위해서다. |
| 영향 범위 | local DB는 허용된 drop/recreate 후 `db/schema.sql` 재적용이 필요하다. 신규 JPA Entity/locking repository, key별 JSON validator, platform API, audit transaction과 schema/mapping test에 영향을 준다. Flyway/Liquibase, migration history table, 운영 배포 절차는 추가하지 않는다. |
| 관련 문서 | `docs/02-requirements/S15-플랫폼-관리자-1차-범위-정의.md`, `docs/03-design/ERD.md`, `docs/04-api/API-명세서.md`, `docs/07-decisions/DB-schema-관리-정책.md`, `docs/07-decisions/기획문서-변경이력.md` |

### 2026-07-15 WBS 4.10.1 platform support case schema 추가

| 항목 | 내용 |
|---|---|
| 변경 대상 | `platform_admin_bootstrap_locks`, `platform_admins`, `support_cases`, `support_case_public_replies`, `support_case_internal_notes`, `admin_action_audits` |
| 변경 내용 | singleton bootstrap lock row와 platform role을 membership과 분리하고, 문의/신고와 상태, public reply/internal note 분리, immutable mutation audit와 FK/index/CHECK를 `db/schema.sql`에 추가했다. bootstrap은 고정 lock row의 pessimistic lock으로 동시 최초 관리자 생성을 직렬화한다. REPORT target은 type과 nullable `CHAR(36)` UUID로 저장하며 다형성 FK를 추가하지 않았다. |
| 변경 이유 | 승인된 WBS `4.10.1` support case Backend vertical slice와 필수 platform authorization/audit foundation을 영속화하기 위해서다. |
| 영향 범위 | 신규 JPA Entity/Repository, 동시성 안전한 최초 `SUPER_ADMIN` bootstrap, support case service/API, audit transaction, schema validation test에 영향을 준다. 기존 `users`, company/project membership role은 변경하지 않는다. |
| 관련 문서 | `docs/02-requirements/S15-플랫폼-관리자-1차-범위-정의.md`, `docs/03-design/ERD.md`, `docs/04-api/API-명세서.md`, `docs/07-decisions/플랫폼-관리자-인가-모델-결정.md` |

### 2026-07-02 ERD/schema drift 보정

| 항목 | 내용 |
|---|---|
| 변경 대상 | `db/schema.sql`의 `company_members`, `project_members` |
| 변경 내용 | `company_members.joined_at`을 ERD와 동일하게 `DATETIME NULL`로 변경하고, ERD 상세 정의에 없는 `company_members.added_by_user_id`, `project_members.added_by_user_id` 및 관련 index/FK를 제거했다. |
| 변경 이유 | PR #15 review에서 지적된 ERD/schema drift를 해소하기 위해서다. Membership 생성 감사 정보는 `invitations.inviter_user_id`를 기준으로 남기고, membership table은 ERD에 정의된 실제 소속 상태만 보관한다. |
| 영향 범위 | DB DDL과 후속 JPA Entity mapping 기준에 영향을 준다. ERD의 column 정의는 이미 기준과 일치하므로 변경하지 않는다. |
| 관련 문서 | `db/schema.sql`, `docs/03-design/ERD.md`, `docs/07-decisions/DB-schema-관리-정책.md` |

### 2026-07-01 전체 MVP DDL 스켈레톤 최초 생성

| 항목 | 내용 |
|---|---|
| 변경 대상 | `db/schema.sql`, 전체 MVP 27개 table DDL, Auth 4개 table, 주요 FK/unique/index/check constraint |
| 변경 내용 | `docs/03-design/ERD.md` 개발주간 ERD 상세화 v1.2 기준으로 MySQL 8.4용 `db/schema.sql`을 생성했다. `users`, `oauth_accounts`, `password_reset_tokens`, `refresh_tokens`를 포함한 전체 MVP 27개 table을 정의하고, `messages.thread_id`, `issues.issue_key VARCHAR(40)`, 채팅/DM/알림 조회용 복합 index, archive/trash/date/source CHECK를 반영했다. |
| 변경 이유 | Sprint 1 Day 1 이후 backend Entity와 로컬 DB 생성 기준을 ERD 문서가 아니라 실행 가능한 schema 파일로 고정하기 위해서다. 오후 schema 초안과 추가 수정필요사항을 그대로 수용하지 않고 canonical API/ERD와 대조해 충돌 항목을 보정했다. |
| 영향 범위 | Sprint 1 구현·검증 직접 대상은 Auth 4개 table로 유지한다. 후속 Sprint의 company/project/issue/document/chat/notification table은 reference DDL로 제공되며, tenant/project/다형성 정합성은 service policy와 테스트에서 추가 검증한다. |
| 관련 문서 | `db/schema.sql`, `docs/03-design/ERD.md`, `docs/03-design/ERD-상세화-근거표.md`, `docs/07-decisions/기획문서-변경이력.md`, `docs/07-decisions/DB-schema-관리-정책.md` |

---

### 2026-06-27 DB schema 관리 방식 확정

| 항목 | 내용 |
|---|---|
| 변경 대상 | DB schema 관리 정책 |
| 변경 내용 | Sprint별 migration tool 기반 versioned migration 대신 ERD 기반 `db/schema.sql`과 문서 변경이력으로 관리 |
| 변경 이유 | 멘토 피드백에 따라 교육용 MVP 초기 단계에서는 전체 ERD 기반 schema 구축과 문서 기반 버전관리를 우선하기로 결정 |
| 영향 범위 | ERD, DoD, 검증 기준, CI 기준, PR Template, Sprint 운영 문서 |
| 관련 문서 | `docs/07-decisions/DB-schema-관리-정책.md`, `docs/03-design/ERD.md` |

---

## 4. 작성 템플릿

```md
### YYYY-MM-DD 변경 제목

| 항목 | 내용 |
|---|---|
| 변경 대상 |  |
| 변경 내용 |  |
| 변경 이유 |  |
| 영향 범위 |  |
| 관련 문서 |  |
```
