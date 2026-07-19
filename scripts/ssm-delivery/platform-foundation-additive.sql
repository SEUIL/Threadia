-- Approved one-time manual MySQL 8 migration for the Platform foundation batch.
-- Run only through docs/00-project/restricted-ssm-platform-foundation-db-migration-runbook.md.
-- This script intentionally contains additive DDL only. MySQL DDL implicitly commits.
-- Bound metadata/row-lock waits to 30 seconds so a drained maintenance window fails
-- safely instead of leaving an operator blocked indefinitely.
SET NAMES utf8mb4;
SET SESSION lock_wait_timeout = 30;
SET SESSION innodb_lock_wait_timeout = 30;

-- Existing databases can predate company lifecycle status. Leave an existing status
-- column unchanged; the Runbook requires a compatibility check before execution.
SET @platform_foundation_sql := (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE `companies` ADD COLUMN `status` ENUM(''ACTIVE'',''INACTIVE'') NOT NULL DEFAULT ''ACTIVE'' AFTER `logo_url`',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'companies'
      AND column_name = 'status'
);
PREPARE platform_foundation_statement FROM @platform_foundation_sql;
EXECUTE platform_foundation_statement;
DEALLOCATE PREPARE platform_foundation_statement;

CREATE TABLE IF NOT EXISTS platform_admin_bootstrap_locks (
    id INT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT chk_platform_admin_bootstrap_lock_singleton CHECK (id = 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO platform_admin_bootstrap_locks (id)
SELECT 1
WHERE NOT EXISTS (
    SELECT 1 FROM platform_admin_bootstrap_locks WHERE id = 1
);

CREATE TABLE IF NOT EXISTS platform_admins (
    id         BIGINT   NOT NULL AUTO_INCREMENT,
    user_id    BIGINT   NOT NULL,
    role       ENUM('PLATFORM_ADMIN','SUPER_ADMIN') NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_platform_admins_user (user_id),
    CONSTRAINT fk_platform_admins_user FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS system_settings (
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    setting_key          VARCHAR(100) NOT NULL,
    value_json           JSON         NOT NULL,
    value_schema_version INT          NOT NULL DEFAULT 1,
    version              BIGINT       NOT NULL DEFAULT 1,
    created_at           DATETIME     NOT NULL,
    updated_at           DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_system_settings_key (setting_key),
    CONSTRAINT chk_system_settings_schema_version CHECK (value_schema_version > 0),
    CONSTRAINT chk_system_settings_version CHECK (version > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO system_settings (setting_key, value_json, value_schema_version, version, created_at, updated_at)
SELECT 'SUPPORT_CASE_CATEGORIES', JSON_OBJECT('categories', JSON_ARRAY(JSON_OBJECT('code', 'ACCOUNT', 'label', '계정'), JSON_OBJECT('code', 'COMPANY', 'label', '회사'), JSON_OBJECT('code', 'PROJECT', 'label', '프로젝트'), JSON_OBJECT('code', 'BUG', 'label', '버그'), JSON_OBJECT('code', 'OTHER', 'label', '기타'))), 1, 1, UTC_TIMESTAMP(), UTC_TIMESTAMP()
WHERE NOT EXISTS (SELECT 1 FROM system_settings WHERE setting_key = 'SUPPORT_CASE_CATEGORIES');

INSERT INTO system_settings (setting_key, value_json, value_schema_version, version, created_at, updated_at)
SELECT 'REPORT_REASON_CATEGORIES', JSON_OBJECT('categories', JSON_ARRAY(JSON_OBJECT('code', 'SPAM', 'label', '스팸'), JSON_OBJECT('code', 'IMPERSONATION', 'label', '사칭'), JSON_OBJECT('code', 'MALICIOUS_CONTENT', 'label', '악성 콘텐츠'), JSON_OBJECT('code', 'PERMISSION_ABUSE', 'label', '권한 오남용'))), 1, 1, UTC_TIMESTAMP(), UTC_TIMESTAMP()
WHERE NOT EXISTS (SELECT 1 FROM system_settings WHERE setting_key = 'REPORT_REASON_CATEGORIES');

INSERT INTO system_settings (setting_key, value_json, value_schema_version, version, created_at, updated_at)
SELECT 'ENFORCEMENT_REASON_PRESETS', JSON_OBJECT('presets', JSON_ARRAY(JSON_OBJECT('code', 'POLICY_VIOLATION', 'label', '운영 정책 위반'))), 1, 1, UTC_TIMESTAMP(), UTC_TIMESTAMP()
WHERE NOT EXISTS (SELECT 1 FROM system_settings WHERE setting_key = 'ENFORCEMENT_REASON_PRESETS');

INSERT INTO system_settings (setting_key, value_json, value_schema_version, version, created_at, updated_at)
SELECT 'ENFORCEMENT_DURATION_PRESETS', JSON_OBJECT('presets', JSON_ARRAY(JSON_OBJECT('code', 'ONE_DAY', 'durationDays', 1), JSON_OBJECT('code', 'SEVEN_DAYS', 'durationDays', 7), JSON_OBJECT('code', 'THIRTY_DAYS', 'durationDays', 30), JSON_OBJECT('code', 'INDEFINITE', 'durationDays', NULL))), 1, 1, UTC_TIMESTAMP(), UTC_TIMESTAMP()
WHERE NOT EXISTS (SELECT 1 FROM system_settings WHERE setting_key = 'ENFORCEMENT_DURATION_PRESETS');

INSERT INTO system_settings (setting_key, value_json, value_schema_version, version, created_at, updated_at)
SELECT 'REPLY_TEMPLATES', JSON_OBJECT('templates', JSON_ARRAY(JSON_OBJECT('code', 'DEFAULT', 'title', '기본 답변', 'body', '문의 내용을 확인하겠습니다.'))), 1, 1, UTC_TIMESTAMP(), UTC_TIMESTAMP()
WHERE NOT EXISTS (SELECT 1 FROM system_settings WHERE setting_key = 'REPLY_TEMPLATES');

INSERT INTO system_settings (setting_key, value_json, value_schema_version, version, created_at, updated_at)
SELECT 'ANNOUNCEMENT_BANNER', JSON_OBJECT('enabled', FALSE, 'message', NULL), 1, 1, UTC_TIMESTAMP(), UTC_TIMESTAMP()
WHERE NOT EXISTS (SELECT 1 FROM system_settings WHERE setting_key = 'ANNOUNCEMENT_BANNER');

INSERT INTO system_settings (setting_key, value_json, value_schema_version, version, created_at, updated_at)
SELECT 'SUPPORT_CENTER_STATUS', JSON_OBJECT('status', 'OPEN', 'message', '문의 접수가 가능합니다.'), 1, 1, UTC_TIMESTAMP(), UTC_TIMESTAMP()
WHERE NOT EXISTS (SELECT 1 FROM system_settings WHERE setting_key = 'SUPPORT_CENTER_STATUS');

INSERT INTO system_settings (setting_key, value_json, value_schema_version, version, created_at, updated_at)
SELECT 'RESULT_NOTIFICATION_POLICY', JSON_OBJECT('notifyOnPublicReply', TRUE, 'notifyOnCaseStatusChange', TRUE), 1, 1, UTC_TIMESTAMP(), UTC_TIMESTAMP()
WHERE NOT EXISTS (SELECT 1 FROM system_settings WHERE setting_key = 'RESULT_NOTIFICATION_POLICY');

CREATE TABLE IF NOT EXISTS support_cases (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    reporter_user_id BIGINT       NOT NULL,
    case_type        ENUM('INQUIRY','REPORT') NOT NULL,
    status           ENUM('OPEN','IN_PROGRESS','RESOLVED','REJECTED','CLOSED') NOT NULL DEFAULT 'OPEN',
    target_type      ENUM('USER','COMPANY','PROJECT','CONTENT') NULL,
    target_id        CHAR(36)     NULL,
    title            VARCHAR(200) NOT NULL,
    body             TEXT         NOT NULL,
    created_at       DATETIME     NOT NULL,
    updated_at       DATETIME     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_support_cases_reporter_created (reporter_user_id, created_at, id),
    KEY idx_support_cases_status_created (status, created_at, id),
    CONSTRAINT fk_support_cases_reporter FOREIGN KEY (reporter_user_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_support_cases_target CHECK (
        (case_type = 'INQUIRY' AND target_type IS NULL AND target_id IS NULL) OR
        (case_type = 'REPORT' AND target_type IS NOT NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS support_case_public_replies (
    id                       BIGINT   NOT NULL AUTO_INCREMENT,
    support_case_id          BIGINT   NOT NULL,
    author_platform_admin_id BIGINT   NOT NULL,
    body                     TEXT     NOT NULL,
    created_at               DATETIME NOT NULL,
    PRIMARY KEY (id),
    KEY idx_scpr_case_created (support_case_id, created_at, id),
    KEY idx_scpr_author (author_platform_admin_id),
    CONSTRAINT fk_scpr_case FOREIGN KEY (support_case_id) REFERENCES support_cases (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_scpr_author FOREIGN KEY (author_platform_admin_id) REFERENCES platform_admins (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS support_case_internal_notes (
    id                       BIGINT   NOT NULL AUTO_INCREMENT,
    support_case_id          BIGINT   NOT NULL,
    author_platform_admin_id BIGINT   NOT NULL,
    body                     TEXT     NOT NULL,
    created_at               DATETIME NOT NULL,
    PRIMARY KEY (id),
    KEY idx_scin_case_created (support_case_id, created_at, id),
    KEY idx_scin_author (author_platform_admin_id),
    CONSTRAINT fk_scin_case FOREIGN KEY (support_case_id) REFERENCES support_cases (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_scin_author FOREIGN KEY (author_platform_admin_id) REFERENCES platform_admins (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS admin_action_audits (
    id                      BIGINT       NOT NULL AUTO_INCREMENT,
    support_case_id         BIGINT       NULL,
    actor_user_id           BIGINT       NOT NULL,
    actor_platform_admin_id BIGINT       NULL,
    authority_source        ENUM('USER','PLATFORM_ADMIN') NOT NULL,
    authority_role          ENUM('PLATFORM_ADMIN','SUPER_ADMIN') NULL,
    action                  ENUM('SUPPORT_CASE_CREATED','PUBLIC_REPLY_CREATED','INTERNAL_NOTE_CREATED','STATUS_CHANGED','COMPANY_STATUS_CHANGED','SYSTEM_SETTING_REPLACED') NOT NULL,
    target_type             ENUM('SUPPORT_CASE','PUBLIC_REPLY','INTERNAL_NOTE','COMPANY','SYSTEM_SETTING') NOT NULL,
    target_id               BIGINT       NOT NULL,
    reason                  VARCHAR(500) NOT NULL,
    before_state            LONGTEXT     NULL,
    after_state             LONGTEXT     NOT NULL,
    created_at              DATETIME     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_aaa_case_created (support_case_id, created_at, id),
    KEY idx_aaa_actor_created (actor_user_id, created_at, id),
    KEY idx_aaa_platform_admin (actor_platform_admin_id),
    CONSTRAINT fk_aaa_case FOREIGN KEY (support_case_id) REFERENCES support_cases (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_aaa_actor FOREIGN KEY (actor_user_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_aaa_platform_admin FOREIGN KEY (actor_platform_admin_id) REFERENCES platform_admins (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_aaa_authority CHECK (
        (authority_source = 'USER' AND actor_platform_admin_id IS NULL AND authority_role IS NULL) OR
        (authority_source = 'PLATFORM_ADMIN' AND actor_platform_admin_id IS NOT NULL AND authority_role IS NOT NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
