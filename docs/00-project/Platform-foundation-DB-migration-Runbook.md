> **작성자**: Threadia 팀  
> **작성일**: 2026-07-18  
> **최종 수정**: 2026-07-18  
> **버전**: v1.0  
> **상태**: 검토 중  
> **태그**: database, mysql, migration, platform, runbook, approval

# Platform foundation DB migration Runbook

## 1. 목적과 적용 범위

이 Runbook은 `db/migrations/V20260718__platform_foundation.sql`을 기존 MySQL 8 DB에 수동 적용하는 승인 절차다. ECR/Compose 배포 전에 승인된 DB operator만 실행한다. 이 저장소의 배포 workflow와 Compose에는 DB migration hook이 없으므로 image 배포가 migration을 대체하지 않는다.

대상은 다음 Platform foundation batch로 한정한다.

| 구분 | 대상 |
|---|---|
| 기존 table 보강 | `companies.status` |
| Platform 권한 | `platform_admin_bootstrap_locks`, `platform_admins` |
| 운영 설정 | `system_settings`와 누락된 기본 setting key |
| 고객센터 | `support_cases`, `support_case_public_replies`, `support_case_internal_notes` |
| 감사 | `admin_action_audits` |

DM, project chat, notification, thread, deployment workflow, Docker, infra, invitation `ReceivedInvitationService`와 secret 값은 범위 밖이다. `db/schema.sql`은 destructive 초기화 기준이므로 기존 DB에 실행하지 않는다.

## 2. 책임과 승인

| 역할 | 책임 |
|---|---|
| DB operator | 사전 점검, SQL 실행, 검증 query 실행, 결과 기록 |
| Release owner | 대상 release와 migration artifact hash를 확인하고 ECR/Compose 배포 순서를 조정 |
| 최종 승인자 | migration 실행 창, 영향, 중단 기준, 배포 진행을 별도로 승인 |

실제 DB 접속과 SQL 실행은 이 문서 작성 또는 repository 검토 승인과 별개의 승인 대상이다. 승인 기록에 secret, credential, 실제 host, DB명, connection string을 남기지 않는다.

## 3. 사전 점검과 Go/No-Go

다음 항목이 모두 PASS일 때만 실행한다.

1. 선택 release ref와 `V20260718__platform_foundation.sql`의 검토 대상 hash가 승인 기록과 일치한다.
2. MySQL server가 8.x이며 대상 schema에 대한 `CREATE`, `ALTER`, `INDEX`, `REFERENCES`, `INSERT`, `SELECT` 권한이 확인된다.
3. 승인된 backup 또는 복구 가능 restore point와 복구 책임자가 확인된다.
4. 대상 DB에 `users`, `companies` table이 있고 `users.id`, `companies.logo_url`이 존재한다.
5. `companies.status`가 없다면 추가 대상이다. 존재한다면 `ENUM('ACTIVE','INACTIVE')`, `NOT NULL`, default `ACTIVE`와 호환되는지 확인한다. 불일치하면 실행하지 않는다.
6. 이미 존재하는 batch table은 아래 정의와 column, index, FK, named CHECK constraint가 호환되는지 확인한다. 부분 생성 또는 이름 충돌은 자동 보정 대상이 아니므로 실행하지 않는다.
7. 실행 중 application write를 막거나 maintenance window를 확보한다. 이 script의 DDL은 MySQL implicit commit을 발생시키며 DDL statement 단위 atomicity가 transaction rollback을 보장하지 않는다.

사전 점검 query는 operator의 승인된 DB session에서 `information_schema`만 조회한다.

```sql
SELECT table_name
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN ('users', 'companies', 'platform_admin_bootstrap_locks', 'platform_admins', 'system_settings', 'support_cases', 'support_case_public_replies', 'support_case_internal_notes', 'admin_action_audits')
ORDER BY table_name;

SELECT column_name, column_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'companies'
  AND column_name = 'status';

SELECT table_name, index_name, non_unique, column_name, seq_in_index
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name IN ('platform_admins', 'system_settings', 'support_cases', 'support_case_public_replies', 'support_case_internal_notes', 'admin_action_audits')
ORDER BY table_name, index_name, seq_in_index;

SELECT table_name, constraint_name, constraint_type
FROM information_schema.table_constraints
WHERE constraint_schema = DATABASE()
  AND table_name IN ('platform_admin_bootstrap_locks', 'platform_admins', 'system_settings', 'support_cases', 'support_case_public_replies', 'support_case_internal_notes', 'admin_action_audits')
ORDER BY table_name, constraint_name;
```

No-Go 조건은 base table 또는 권한 부족, 기존 객체의 비호환 정의, backup/restore 책임자 부재, 승인 기록 부재, 실행 중 write 차단 불가다.

## 4. 실행 절차

1. DB operator는 승인된 maintenance window에서 application write를 차단했음을 기록한다.
2. operator는 승인된 안전한 DB 접속 경로에서 `db/migrations/V20260718__platform_foundation.sql`을 한 번 실행한다. 이 repository는 접속 명령이나 credential을 제공하지 않는다.
3. client가 첫 오류에서 중단되도록 설정한다. 오류가 나면 이후 statement를 실행하거나 ECR/Compose 배포를 시작하지 않는다.
4. 실행이 성공하면 즉시 5절의 검증 query를 실행한다.
5. 모든 검증이 PASS인 경우에만 최종 승인자가 ECR/Compose 배포 진행을 별도 승인한다.

이 SQL은 새 table에 `CREATE TABLE IF NOT EXISTS`를 사용하고, `companies.status` 추가 전에는 `INFORMATION_SCHEMA.COLUMNS`를 확인한다. 설정 seed는 key가 없을 때만 insert하므로 기존 설정을 변경하지 않는다. 재실행은 객체와 seed를 중복 생성하지 않는 idempotent 동작을 목표로 한다.

## 5. 실행 후 검증

아래 query 결과가 기대값과 일치해야 한다.

```sql
SELECT table_name
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN ('platform_admin_bootstrap_locks', 'platform_admins', 'system_settings', 'support_cases', 'support_case_public_replies', 'support_case_internal_notes', 'admin_action_audits')
ORDER BY table_name;

SELECT column_name, column_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'companies'
  AND column_name = 'status';

SELECT table_name, constraint_name, constraint_type
FROM information_schema.table_constraints
WHERE constraint_schema = DATABASE()
  AND table_name IN ('platform_admin_bootstrap_locks', 'platform_admins', 'system_settings', 'support_cases', 'support_case_public_replies', 'support_case_internal_notes', 'admin_action_audits')
ORDER BY table_name, constraint_name;

SELECT setting_key
FROM system_settings
WHERE setting_key IN ('SUPPORT_CASE_CATEGORIES', 'REPORT_REASON_CATEGORIES', 'ENFORCEMENT_REASON_PRESETS', 'ENFORCEMENT_DURATION_PRESETS', 'REPLY_TEMPLATES', 'ANNOUNCEMENT_BANNER', 'SUPPORT_CENTER_STATUS', 'RESULT_NOTIFICATION_POLICY')
ORDER BY setting_key;

SELECT id
FROM platform_admin_bootstrap_locks
WHERE id = 1;
```

기대 결과는 신규 table 7개, `companies.status` 1개, bootstrap lock `id = 1` 1건, system setting key 8개다. FK, index, CHECK constraint의 이름과 column은 migration SQL 정의 및 `information_schema` 사전 점검 결과와 일치해야 한다. 이후 backend가 `ddl-auto=validate`로 정상 기동하는지는 ECR/Compose 배포 승인 후 별도 release verification으로 확인한다.

## 6. Stop 및 복구 경계

다음 중 하나라도 발생하면 즉시 중단하고 ECR/Compose 배포를 진행하지 않는다.

- preflight의 base table, 권한, 호환성, backup, 승인 조건이 실패했다.
- SQL statement가 오류를 반환했거나 connection이 끊겼다.
- 검증 query에서 table, column, named constraint, seed key가 누락되었거나 예상과 다르다.
- application mapping validation 또는 핵심 Platform 기능 검증이 실패했다.

MySQL 8 DDL은 implicit commit을 수행하며 각 DDL은 atomic하게 성공 또는 실패할 수 있어도 migration 전체를 transaction으로 rollback할 수 없다. 따라서 이 artifact는 `DROP`, data deletion, column 축소, constraint 제거를 포함하지 않는다. 이미 적용된 additive DDL을 즉시 역실행하지 않는다. partial state 또는 호환성 오류는 영향 범위와 실행된 마지막 statement를 기록하고, DB operator와 최종 승인자가 backup/restore 또는 별도 승인된 forward-fix를 판단한다. 이 Runbook은 restore 실행 권한을 부여하거나 복구 명령을 정의하지 않는다.

## 7. 실행 기록

승인 기록에는 release ref, migration hash, 실행 시각, operator, 승인자, preflight와 postflight PASS/FAIL, 중단 여부, 복구 판단만 기록한다. secret, credential, 실제 host, DB명, connection string, query 결과의 민감 데이터는 기록하지 않는다.

## 8. 관련 기준

- [Pre-deploy Release Checklist](./pre-deploy-release-checklist.md)
- [DB schema 관리 정책](../07-decisions/DB-schema-관리-정책.md)
- [DB schema 변경이력](../07-decisions/DB-schema-변경이력.md)
- [S15 플랫폼 관리자 1차 범위 정의](../02-requirements/S15-플랫폼-관리자-1차-범위-정의.md)
