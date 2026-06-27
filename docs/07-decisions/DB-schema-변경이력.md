# DB schema 변경이력

> **작성자**: Threadia 팀
> **작성일**: 2026-06-27
> **최종 수정**: 2026-06-27
> **버전**: v1.0
> **상태**: 확정

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
