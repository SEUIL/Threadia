> **작성자**: Threadia 팀
> **작성일**: 2026-07-18
> **최종 수정**: 2026-07-18
> **버전**: v1.0
> **상태**: 검토 중
> **태그**: ssm, database, mysql, platform, runbook

# Restricted SSM Platform Foundation DB Migration Runbook

## 1. 목적과 경계

이 문서는 persistent SSM host의 확인된 Platform foundation schema drift를 한 번만 보정하는 별도 DB operator 절차다. 대상 artifact는 `scripts/ssm-delivery/platform-foundation-additive.sql`이며, MySQL 8에서 additive DDL과 missing seed만 수행한다.

`db/schema.sql`은 Fresh DB initializer다. ECR Compose가 관리하는 기존 Fresh DB 경로에서만 destructive recreate와 함께 사용할 수 있으며, persistent SSM host에서는 어떤 경우에도 실행하지 않는다. 이 runbook은 Flyway, Liquibase, Hibernate 자동 DDL 또는 일반 migration framework를 도입하지 않는다.

## 2. 역할과 승인 경계

| 작업 | 책임과 승인 |
|---|---|
| repository/PR 검토·merge | repository reviewer와 merge 권한자; SQL checksum과 runbook revision을 승인한다. |
| live DB backup·restore identifier 기록 | DB operator; backup 성공과 restore 가능한 identifier를 release evidence에 기록한다. |
| SQL 실행 | 별도 승인된 DB operator만 수행한다. SSM helper, GitHub workflow, CloudFormation은 실행 주체가 아니다. |
| release retry | release operator; 아래 same-SHA rerun gate를 만족한 경우에만 수행한다. |

repository PR 승인이나 merge는 live DB backup, SQL 실행, release retry의 승인이 아니다. 각 live 단계는 별도 승인과 evidence가 필요하다.

## 3. 실행 전 hard gate

아래 항목 하나라도 실패하면 SQL을 실행하지 않는다.

1. 승인된 repository revision에서 `platform-foundation-additive.sql`의 SHA-256을 기록하고, operator가 실행할 local artifact checksum과 일치시킨다.
2. DB operator는 backup 완료 시각과 restore identifier를 기록하고, restore 절차와 접근 권한을 확인한다. backup 또는 restore identifier를 만들거나 실행하는 행위는 이 repository 절차의 범위 밖이다.
3. 대상 DB가 MySQL 8인지 확인한다. `SELECT VERSION();` 결과가 MySQL 8 계열이 아니면 중단한다.
4. `DATABASE()`가 승인된 persistent application DB를 가리키는지 확인한다. DB URL, host, credential, identifier 또는 결과 값은 repository/PR evidence에 기록하지 않는다.
5. base table `companies`, `users`가 존재하고, SQL이 참조하는 base column과 type이 current canonical schema의 `companies.logo_url`, `users.id`와 정확히 호환되는지 `information_schema`로 확인한다. `companies.status`가 이미 있으면 `ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE'`와 호환되는지 확인한다. 하나라도 불일치하면 NO-GO다.
6. 다음 Platform object가 이미 존재하면 `SHOW CREATE TABLE`과 `information_schema`로 column, type, nullable/default, PK, unique/index, foreign key, check constraint, charset/collation을 artifact와 정확히 비교한다: `platform_admin_bootstrap_locks`, `platform_admins`, `system_settings`, `support_cases`, `support_case_public_replies`, `support_case_internal_notes`, `admin_action_audits`. 비교 불일치 또는 비교 불가 결과는 `CREATE TABLE IF NOT EXISTS`에 맡기지 않고 strict NO-GO로 처리한다.
7. 기존 `system_settings` seed key가 있으면 JSON 값과 schema/version이 승인된 artifact와 호환되는지 확인한다. 기존 singleton lock row가 있으면 `id = 1`만 존재하는지 확인한다. 불일치는 NO-GO다.
8. DB write freeze를 선언하고 application write traffic, batch, admin 작업을 중지했음을 확인한다. MySQL DDL은 implicit commit이므로 transaction rollback을 복구 수단으로 가정하지 않는다.
9. approved runtime Parameter Store contract에 `THREADIA_PLATFORM_ADMIN_BOOTSTRAP_EMAIL`이 포함되고, valid ACTIVE user를 가리키는 값이 별도 secret-safe channel로 준비됐음을 확인한다. 값 자체를 출력·문서화·검증 로그에 기록하지 않는다.

### 3.1 Preflight read-only query templates

아래 query는 DB operator가 approved persistent application DB connection에서만 복사해 실행하는 read-only template이다. DB name, host, credential을 넣지 않으며 `DATABASE()`가 승인된 대상인지 먼저 gate 4에서 확인한다. 결과를 `scripts/ssm-delivery/platform-foundation-additive.sql`의 exact column/type/default/constraint/index/charset/collation/seed 계약과 비교한다. existing Platform object의 결과가 하나라도 artifact와 다르거나 비교할 수 없으면 strict NO-GO다.

```sql
-- Base users/companies table and required base columns.
SELECT
    t.TABLE_NAME, t.ENGINE, t.TABLE_COLLATION,
    c.COLUMN_NAME, c.ORDINAL_POSITION, c.COLUMN_TYPE, c.IS_NULLABLE,
    c.COLUMN_DEFAULT, c.EXTRA, c.CHARACTER_SET_NAME, c.COLLATION_NAME
FROM information_schema.TABLES AS t
LEFT JOIN information_schema.COLUMNS AS c
  ON c.TABLE_SCHEMA = t.TABLE_SCHEMA AND c.TABLE_NAME = t.TABLE_NAME
WHERE t.TABLE_SCHEMA = DATABASE()
  AND t.TABLE_NAME IN ('users', 'companies')
  AND (c.COLUMN_NAME IS NULL OR c.COLUMN_NAME IN ('id', 'logo_url', 'status'))
ORDER BY t.TABLE_NAME, c.ORDINAL_POSITION;

-- companies.status must be absent or exactly the approved artifact definition.
SELECT
    COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT, EXTRA,
    CHARACTER_SET_NAME, COLLATION_NAME
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'companies'
  AND COLUMN_NAME = 'status';
```

```sql
-- All seven Platform tables: absence is permitted before execution; existence requires exact compatibility.
SELECT TABLE_NAME, ENGINE, TABLE_COLLATION
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN (
      'platform_admin_bootstrap_locks', 'platform_admins', 'system_settings',
      'support_cases', 'support_case_public_replies',
      'support_case_internal_notes', 'admin_action_audits'
  )
ORDER BY TABLE_NAME;

-- Full column metadata for every pre-existing Platform table.
SELECT
    TABLE_NAME, COLUMN_NAME, ORDINAL_POSITION, COLUMN_TYPE, IS_NULLABLE,
    COLUMN_DEFAULT, EXTRA, CHARACTER_SET_NAME, COLLATION_NAME
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN (
      'platform_admin_bootstrap_locks', 'platform_admins', 'system_settings',
      'support_cases', 'support_case_public_replies',
      'support_case_internal_notes', 'admin_action_audits'
  )
ORDER BY TABLE_NAME, ORDINAL_POSITION;
```

```sql
-- Primary, unique, and secondary indexes; compare names, order, uniqueness, and indexed columns exactly.
SELECT TABLE_NAME, INDEX_NAME, NON_UNIQUE, SEQ_IN_INDEX, COLUMN_NAME,
       COLLATION, SUB_PART, INDEX_TYPE
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN (
      'platform_admin_bootstrap_locks', 'platform_admins', 'system_settings',
      'support_cases', 'support_case_public_replies',
      'support_case_internal_notes', 'admin_action_audits'
  )
ORDER BY TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX;

-- All constraints, including PRIMARY KEY and UNIQUE constraints.
SELECT TABLE_NAME, CONSTRAINT_NAME, CONSTRAINT_TYPE
FROM information_schema.TABLE_CONSTRAINTS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN (
      'platform_admin_bootstrap_locks', 'platform_admins', 'system_settings',
      'support_cases', 'support_case_public_replies',
      'support_case_internal_notes', 'admin_action_audits'
  )
ORDER BY TABLE_NAME, CONSTRAINT_TYPE, CONSTRAINT_NAME;

-- Foreign key column mapping and referenced target must match the artifact exactly.
SELECT TABLE_NAME, CONSTRAINT_NAME, COLUMN_NAME, ORDINAL_POSITION,
       REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN (
      'platform_admin_bootstrap_locks', 'platform_admins', 'system_settings',
      'support_cases', 'support_case_public_replies',
      'support_case_internal_notes', 'admin_action_audits'
  )
  AND REFERENCED_TABLE_NAME IS NOT NULL
ORDER BY TABLE_NAME, CONSTRAINT_NAME, ORDINAL_POSITION;

-- CHECK constraint names and clauses must match the artifact exactly.
SELECT tc.TABLE_NAME, tc.CONSTRAINT_NAME, cc.CHECK_CLAUSE
FROM information_schema.TABLE_CONSTRAINTS AS tc
JOIN information_schema.CHECK_CONSTRAINTS AS cc
  ON cc.CONSTRAINT_SCHEMA = tc.CONSTRAINT_SCHEMA
 AND cc.CONSTRAINT_NAME = tc.CONSTRAINT_NAME
WHERE tc.TABLE_SCHEMA = DATABASE()
  AND tc.TABLE_NAME IN (
      'platform_admin_bootstrap_locks', 'platform_admins', 'system_settings',
      'support_cases', 'support_case_public_replies',
      'support_case_internal_notes', 'admin_action_audits'
  )
  AND tc.CONSTRAINT_TYPE = 'CHECK'
ORDER BY tc.TABLE_NAME, tc.CONSTRAINT_NAME;
```

The preceding seven-table `information_schema.TABLES` existence query is a mandatory guard for the following direct-table queries. Absence of either table is permitted before this one-time migration, so do not issue a direct query against an absent table; record the absence as compatible and let the approved additive artifact create it.

```sql
-- Run only when the preceding information_schema.TABLES query confirms `system_settings` exists; absence is allowed before migration.
-- Compare existing seed keys and versions with the artifact before execution.
SELECT setting_key, value_json, value_schema_version, version
FROM system_settings
WHERE setting_key IN (
    'SUPPORT_CASE_CATEGORIES', 'REPORT_REASON_CATEGORIES',
    'ENFORCEMENT_REASON_PRESETS', 'ENFORCEMENT_DURATION_PRESETS',
    'REPLY_TEMPLATES', 'ANNOUNCEMENT_BANNER', 'SUPPORT_CENTER_STATUS',
    'RESULT_NOTIFICATION_POLICY'
)
ORDER BY setting_key;

-- Run only when the preceding information_schema.TABLES query confirms `platform_admin_bootstrap_locks` exists; absence is allowed before migration.
-- Existing lock state must contain only id = 1 when the table exists.
SELECT id, COUNT(*) AS row_count
FROM platform_admin_bootstrap_locks
GROUP BY id
ORDER BY id;
```

For each pre-existing Platform table, run these literal `SHOW CREATE TABLE` statements and the six structured templates below. `SHOW CREATE TABLE` is the final comparison source for emitted DDL, including engine, charset, collation, indexes, FK actions, and CHECK syntax; it does not replace the structured `information_schema` comparison above.

```sql
SHOW CREATE TABLE `platform_admin_bootstrap_locks`;
SHOW CREATE TABLE `platform_admins`;
SHOW CREATE TABLE `system_settings`;
SHOW CREATE TABLE `support_cases`;
SHOW CREATE TABLE `support_case_public_replies`;
SHOW CREATE TABLE `support_case_internal_notes`;
SHOW CREATE TABLE `admin_action_audits`;
```

```sql
SELECT TABLE_NAME, ENGINE, TABLE_COLLATION
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = '<platform_table>';

SELECT COLUMN_NAME, ORDINAL_POSITION, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT,
       EXTRA, CHARACTER_SET_NAME, COLLATION_NAME
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = '<platform_table>'
ORDER BY ORDINAL_POSITION;

SELECT INDEX_NAME, NON_UNIQUE, SEQ_IN_INDEX, COLUMN_NAME, COLLATION, SUB_PART, INDEX_TYPE
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = '<platform_table>'
ORDER BY INDEX_NAME, SEQ_IN_INDEX;

SELECT CONSTRAINT_NAME, CONSTRAINT_TYPE
FROM information_schema.TABLE_CONSTRAINTS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = '<platform_table>'
ORDER BY CONSTRAINT_TYPE, CONSTRAINT_NAME;

SELECT CONSTRAINT_NAME, COLUMN_NAME, ORDINAL_POSITION,
       REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = '<platform_table>'
  AND REFERENCED_TABLE_NAME IS NOT NULL
ORDER BY CONSTRAINT_NAME, ORDINAL_POSITION;

SELECT tc.CONSTRAINT_NAME, cc.CHECK_CLAUSE
FROM information_schema.TABLE_CONSTRAINTS AS tc
JOIN information_schema.CHECK_CONSTRAINTS AS cc
  ON cc.CONSTRAINT_SCHEMA = tc.CONSTRAINT_SCHEMA
 AND cc.CONSTRAINT_NAME = tc.CONSTRAINT_NAME
WHERE tc.TABLE_SCHEMA = DATABASE() AND tc.TABLE_NAME = '<platform_table>'
  AND tc.CONSTRAINT_TYPE = 'CHECK'
ORDER BY tc.CONSTRAINT_NAME;
```

## 4. 실행 절차

1. operator는 write freeze, backup/restore identifier, artifact SHA-256, preflight 결과를 secret 없는 evidence에 기록한다.
2. SQL client는 first-error stop 모드로 실행한다. client가 error 후 계속 실행하는 옵션을 사용하지 않는다.
3. 승인된 artifact 한 개만 실행한다. `db/schema.sql`, 임의 DDL, seed 수정, DROP/DELETE, migration helper를 함께 실행하지 않는다.
4. 첫 SQL error, connection loss, timeout, lock wait, disk/capacity 경고, write freeze 위반, preflight와 다른 object 발견 시 즉시 중단한다. 실패한 partial DDL을 재실행하거나 수동 보정하지 않는다. backup restore 여부는 DB operator가 별도 승인으로 판단한다.

## 5. 실행 후 postflight

write freeze를 해제하거나 application을 재시작하기 전에 다음을 확인한다.

1. `companies.status`의 type, default, nullability가 artifact와 일치한다.
2. 일곱 Platform table이 존재한다: `platform_admin_bootstrap_locks`, `platform_admins`, `system_settings`, `support_cases`, `support_case_public_replies`, `support_case_internal_notes`, `admin_action_audits`.
3. 각 table의 column, PK, unique key, secondary index, foreign key, check constraint, engine, charset/collation이 artifact와 일치한다.
4. singleton lock은 `id = 1` exactly one row이며, 추가 lock row가 없다.
5. eight system setting key가 각각 exactly one row로 존재한다: `SUPPORT_CASE_CATEGORIES`, `REPORT_REASON_CATEGORIES`, `ENFORCEMENT_REASON_PRESETS`, `ENFORCEMENT_DURATION_PRESETS`, `REPLY_TEMPLATES`, `ANNOUNCEMENT_BANNER`, `SUPPORT_CENTER_STATUS`, `RESULT_NOTIFICATION_POLICY`. 각 value JSON과 version fields는 artifact와 호환되어야 한다.
6. pre-existing Platform object가 없었던 경우에도 postflight에서 모든 constraint/index 이름과 참조 대상이 확인돼야 한다. 결과가 하나라도 불일치하면 write freeze를 유지하고 release를 진행하지 않는다.

### 5.1 Postflight read-only query templates

실행 후에도 아래 query만 사용한다. seven Platform tables 모두가 존재해야 하며, query 결과와 `SHOW CREATE TABLE` 결과를 실행한 approved artifact의 exact compatibility 기준으로 operator가 판정한다. missing table, duplicate/missing seed, extra lock row, 또는 artifact와 다른 metadata는 release NO-GO다.

```sql
-- All seven Platform tables must now exist exactly once in the approved schema.
SELECT TABLE_NAME, ENGINE, TABLE_COLLATION
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN (
      'platform_admin_bootstrap_locks', 'platform_admins', 'system_settings',
      'support_cases', 'support_case_public_replies',
      'support_case_internal_notes', 'admin_action_audits'
  )
ORDER BY TABLE_NAME;

-- companies.status must now exactly be ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE'.
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT, EXTRA,
       CHARACTER_SET_NAME, COLLATION_NAME
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'companies'
  AND COLUMN_NAME = 'status';

-- Every expected setting key must appear exactly once; zero or count > 1 is NO-GO.
SELECT setting_key, COUNT(*) AS row_count,
       MIN(value_schema_version) AS value_schema_version,
       MIN(version) AS version
FROM system_settings
WHERE setting_key IN (
    'SUPPORT_CASE_CATEGORIES', 'REPORT_REASON_CATEGORIES',
    'ENFORCEMENT_REASON_PRESETS', 'ENFORCEMENT_DURATION_PRESETS',
    'REPLY_TEMPLATES', 'ANNOUNCEMENT_BANNER', 'SUPPORT_CENTER_STATUS',
    'RESULT_NOTIFICATION_POLICY'
)
GROUP BY setting_key
ORDER BY setting_key;

-- Postflight lock invariant: exactly one row, and it is id = 1.
SELECT COUNT(*) AS total_rows,
       COALESCE(SUM(id = 1), 0) AS singleton_rows,
       COALESCE(SUM(id <> 1), 0) AS non_singleton_rows
FROM platform_admin_bootstrap_locks;
```

Run the seven literal `SHOW CREATE TABLE` statements and the six structured `<platform_table>` templates in section 3.1 again after execution. This postflight repetition verifies columns, indexes, PK/unique constraints, FKs, CHECK constraints, engine, charset, and collation rather than relying only on table existence.

## 6. release rerun gate와 중단 조건

schema postflight와 bootstrap email runtime contract가 모두 통과한 뒤에만, migration 뒤 동일한 source SHA를 사용한 release retry를 한 번 실행할 수 있다. GitHub는 retry마다 새 run/attempt와 새 release identity를 만들므로, 이 retry는 prior artifact의 byte-identical reuse가 아니다. 허용 기준은 source SHA가 같고 source/helper/Parameter Store/IAM/CloudFormation contract 변경이 없다는 점이며, 새 run/attempt/release identity 자체는 same-source retry를 무효화하지 않는다. source 또는 contract 변경이 있으면 별도 review·approval·release 절차가 필요하다.

다음 조건에서는 release rerun을 중단한다: SQL execution error 또는 incomplete postflight, bootstrap email key 누락/invalid, platform admin bootstrap이 ACTIVE user를 찾지 못함, release rollback 재발, schema validation error, 또는 evidence에 backup/restore identifier·checksum·preflight/postflight 결과가 누락됨. 원인 분석과 별도 승인 전에는 추가 retry를 하지 않는다.

## 7. 변경 이력

| 날짜 | 내용 |
|---|---|
| 2026-07-18 | persistent SSM host의 Platform foundation schema drift를 위한 one-time additive DB operator 절차를 추가했다. |
