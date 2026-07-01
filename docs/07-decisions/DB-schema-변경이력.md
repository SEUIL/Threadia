# DB schema 변경이력

> **작성자**: Threadia 팀
> **작성일**: 2026-06-27
> **최종 수정**: 2026-07-01
> **버전**: v1.1
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
