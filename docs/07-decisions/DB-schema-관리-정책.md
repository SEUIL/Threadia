# DB schema 관리 정책

> **작성자**: Threadia 팀  
> **작성일**: 2026-06-27  
> **최종 수정**: 2026-06-27  
> **버전**: v1.0  
> **상태**: 확정

---

## 1. 결정

Threadia Sprint 1 개발 단계에서는 Flyway, Liquibase 같은 DB migration tool을 사용하지 않는다.

DB schema는 `docs/03-design/ERD.md`를 설계 기준으로 삼고, 전체 MVP 기준 SQL DDL 파일인 `db/schema.sql`을 DB 생성 기준으로 관리한다. schema 변경 이력은 `docs/07-decisions/DB-schema-변경이력.md`에 기록한다.

---

## 2. 배경

Threadia는 교육용 MVP 프로젝트이며, 초기 개발 단계에서는 운영 데이터 보존보다 전체 ERD 이해, 관계 설계, API 구현 속도가 더 중요하다.

멘토 피드백에 따라 Sprint별 versioned migration을 누적하기보다, 초기에는 전체 ERD 기반 schema를 SQL DDL로 구축하고 변경 사항을 문서로 관리한다.

---

## 3. 적용 기준

| 항목 | 기준 |
|---|---|
| 설계 기준 | `docs/03-design/ERD.md` |
| DB 생성 기준 | `db/schema.sql` |
| 변경 이력 | `docs/07-decisions/DB-schema-변경이력.md` |
| Spring Boot 설정 방향 | JPA는 schema 생성보다 mapping 검증에 사용 |
| 권장 JPA DDL 설정 | `spring.jpa.hibernate.ddl-auto=validate` |
| 비권장 방식 | `spring.jpa.hibernate.ddl-auto=update`로 DB 구조 자동 변경 |
| Sprint 1 migration tool | 사용하지 않음 |

---

## 4. 작업 흐름

1. ERD 변경이 필요하면 `docs/03-design/ERD.md`를 먼저 갱신한다.
2. 변경된 ERD 기준으로 `db/schema.sql`의 최종 DDL을 갱신한다.
3. Spring Boot Entity와 DB schema가 일치하도록 mapping을 작성한다.
4. local DB는 필요 시 drop 후 `db/schema.sql`로 재생성한다.
5. 변경 이유, 변경 대상, 영향 범위를 `docs/07-decisions/DB-schema-변경이력.md`에 기록한다.
6. PR에서는 ERD, API, Entity, `db/schema.sql` 정합성을 확인한다.

---

## 5. 제외 범위

- Sprint 1에서는 Flyway/Liquibase versioned migration을 구성하지 않는다.
- Sprint 1에서는 migration directory 또는 migration history table을 운영 기준으로 두지 않는다.
- Hibernate `ddl-auto=update`에 의존해 DB 구조를 자동 변경하지 않는다.
- 운영 데이터가 생긴 뒤의 무중단 migration 전략은 이 문서의 범위가 아니다.

---

## 6. 향후 재검토 조건

다음 조건 중 하나가 발생하면 Flyway 또는 Liquibase 도입을 다시 검토한다.

- 공유 개발 DB 또는 운영 DB에 보존해야 할 데이터가 생긴다.
- 이미 배포된 DB를 drop/recreate 방식으로 초기화할 수 없어진다.
- schema 변경 이력을 SQL 파일 단위로 자동 추적해야 한다.
- rollback, audit, 배포 자동화 요구가 생긴다.
