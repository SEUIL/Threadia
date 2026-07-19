# DB schema 변경이력

> **작성자**: Threadia 팀
> **작성일**: 2026-06-27
> **최종 수정**: 2026-07-18
> **버전**: v1.3
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

### 2026-07-18 Persistent SSM Platform foundation additive exception

| 항목 | 내용 |
|---|---|
| 변경 대상 | `scripts/ssm-delivery/platform-foundation-additive.sql`, `companies.status`, Platform foundation 7개 table, bootstrap lock, 8개 system setting seed |
| 변경 내용 | persistent SSM host의 확인된 schema drift 보정을 위해 MySQL 8 additive/idempotent operator artifact를 추가했다. existing `companies.status`는 변경하지 않고, missing table/seed만 추가한다. |
| 변경 이유 | restricted SSM release의 Hibernate schema validation이 `admin_action_audits` 누락으로 실패했으며, persistent DB에는 destructive Fresh DB initializer를 사용할 수 없기 때문이다. |
| 영향 범위 | 별도 승인된 DB operator의 one-time 실행에만 적용된다. SSM workflow/helper/CloudFormation은 DB migration을 자동 실행하지 않으며, `db/schema.sql`은 ECR Compose Fresh DB path에만 유지한다. |
| 관련 문서 | `scripts/ssm-delivery/platform-foundation-additive.sql`, `docs/00-project/restricted-ssm-platform-foundation-db-migration-runbook.md`, `docs/00-project/restricted-ssm-release-runbook.md`, `docs/07-decisions/DB-schema-관리-정책.md` |

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
