-- One-time, operator-run additive DDL for an existing persistent MySQL 8 database.
-- This script is intentionally not called by deployment automation.
CREATE TABLE IF NOT EXISTS attachments (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    company_id         BIGINT       NULL,
    created_by_user_id BIGINT       NOT NULL,
    target_type        ENUM('DOCUMENT','ISSUE','MESSAGE','THREAD_COMMENT','DIRECT_MESSAGE','USER_AVATAR','COMPANY_LOGO','BINARY_DOCUMENT') NOT NULL,
    target_id          BIGINT       NOT NULL,
    object_key         VARCHAR(500) NOT NULL,
    original_filename  VARCHAR(255) NOT NULL,
    content_type       VARCHAR(255) NOT NULL,
    content_length     BIGINT       NOT NULL,
    checksum_sha256    VARCHAR(88)  NOT NULL,
    status             ENUM('UPLOADING','READY','DELETED') NOT NULL DEFAULT 'UPLOADING',
    completed_at       DATETIME     NULL,
    deleted_at         DATETIME     NULL,
    created_at         DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_attachments_object_key (object_key),
    KEY idx_attachments_target_status_created (target_type, target_id, status, created_at),
    KEY idx_attachments_status_deleted (status, deleted_at),
    KEY idx_attachments_company (company_id),
    KEY idx_attachments_created_by (created_by_user_id),
    CONSTRAINT fk_attachments_company FOREIGN KEY (company_id) REFERENCES companies (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_attachments_created_by FOREIGN KEY (created_by_user_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_attachments_lifecycle CHECK (
        (status = 'UPLOADING' AND completed_at IS NULL AND deleted_at IS NULL) OR
        (status = 'READY' AND completed_at IS NOT NULL AND deleted_at IS NULL) OR
        (status = 'DELETED' AND deleted_at IS NOT NULL)
    ),
    CONSTRAINT chk_attachments_length CHECK (content_length > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE users ADD COLUMN IF NOT EXISTS avatar_attachment_id BIGINT NULL;
ALTER TABLE companies ADD COLUMN IF NOT EXISTS logo_attachment_id BIGINT NULL;

-- MySQL 8 does not support ADD CONSTRAINT IF NOT EXISTS. Information-schema guards keep this one-time
-- operator artifact retry-safe without turning deployment automation into a migration runner.
SET @threadia_schema = DATABASE();
SET @has_users_avatar_index = (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = @threadia_schema AND table_name = 'users' AND index_name = 'idx_users_avatar_attachment');
SET @sql = IF(@has_users_avatar_index = 0,
    'ALTER TABLE users ADD KEY idx_users_avatar_attachment (avatar_attachment_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_companies_logo_index = (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = @threadia_schema AND table_name = 'companies' AND index_name = 'idx_companies_logo_attachment');
SET @sql = IF(@has_companies_logo_index = 0,
    'ALTER TABLE companies ADD KEY idx_companies_logo_attachment (logo_attachment_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_users_avatar_fk = (SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE constraint_schema = @threadia_schema AND table_name = 'users' AND constraint_name = 'fk_users_avatar_attachment');
SET @sql = IF(@has_users_avatar_fk = 0,
    'ALTER TABLE users ADD CONSTRAINT fk_users_avatar_attachment FOREIGN KEY (avatar_attachment_id) REFERENCES attachments (id) ON DELETE SET NULL ON UPDATE RESTRICT', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_companies_logo_fk = (SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE constraint_schema = @threadia_schema AND table_name = 'companies' AND constraint_name = 'fk_companies_logo_attachment');
SET @sql = IF(@has_companies_logo_fk = 0,
    'ALTER TABLE companies ADD CONSTRAINT fk_companies_logo_attachment FOREIGN KEY (logo_attachment_id) REFERENCES attachments (id) ON DELETE SET NULL ON UPDATE RESTRICT', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
