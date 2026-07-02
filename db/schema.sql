DROP TABLE IF EXISTS project_dashboard_summaries;
DROP TABLE IF EXISTS notification_recipients;
DROP TABLE IF EXISTS notification_preferences;
DROP TABLE IF EXISTS notification_events;
DROP TABLE IF EXISTS activity_events;
DROP TABLE IF EXISTS thread_comments;
DROP TABLE IF EXISTS project_chat_member_states;
DROP TABLE IF EXISTS document_issue_links;
DROP TABLE IF EXISTS issue_status_history;
DROP TABLE IF EXISTS issues;
DROP TABLE IF EXISTS messages;
DROP TABLE IF EXISTS threads;
DROP TABLE IF EXISTS dm_room_user_states;
DROP TABLE IF EXISTS direct_messages;
DROP TABLE IF EXISTS direct_message_rooms;
DROP TABLE IF EXISTS documents;
DROP TABLE IF EXISTS invitations;
DROP TABLE IF EXISTS sprints;
DROP TABLE IF EXISTS project_members;
DROP TABLE IF EXISTS projects;
DROP TABLE IF EXISTS company_members;
DROP TABLE IF EXISTS departments;
DROP TABLE IF EXISTS companies;
DROP TABLE IF EXISTS refresh_tokens;
DROP TABLE IF EXISTS password_reset_tokens;
DROP TABLE IF EXISTS oauth_accounts;
DROP TABLE IF EXISTS users;

CREATE TABLE users (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NULL,
    nickname      VARCHAR(100) NOT NULL,
    avatar_url    VARCHAR(500) NULL,
    status        ENUM('ACTIVE','SUSPENDED','DELETED') NOT NULL DEFAULT 'ACTIVE',
    created_at    DATETIME     NOT NULL,
    updated_at    DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE oauth_accounts (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    user_id          BIGINT       NOT NULL,
    provider         ENUM('GOOGLE','KAKAO','NAVER') NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    provider_email   VARCHAR(255) NULL,
    created_at       DATETIME     NOT NULL,
    updated_at       DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_oauth_provider_user (provider, provider_user_id),
    UNIQUE KEY uq_oauth_user_provider (user_id, provider),
    CONSTRAINT fk_oauth_user FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE password_reset_tokens (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    user_id    BIGINT       NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at DATETIME     NOT NULL,
    used_at    DATETIME     NULL,
    created_at DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_prt_token_hash (token_hash),
    KEY idx_prt_user (user_id),
    CONSTRAINT fk_prt_user FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE refresh_tokens (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    user_id    BIGINT       NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at DATETIME     NOT NULL,
    rotated_at DATETIME     NULL,
    revoked_at DATETIME     NULL,
    created_at DATETIME     NOT NULL,
    updated_at DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_rt_token_hash (token_hash),
    KEY idx_rt_user (user_id),
    CONSTRAINT fk_rt_user FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE companies (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    name                VARCHAR(150) NOT NULL,
    representative_name VARCHAR(100) NULL,
    description         TEXT         NULL,
    industry_category   VARCHAR(100) NULL,
    logo_url            VARCHAR(500) NULL,
    status              VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE',
    created_by_user_id  BIGINT       NOT NULL,
    created_at          DATETIME     NOT NULL,
    updated_at          DATETIME     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_companies_created_by (created_by_user_id),
    CONSTRAINT fk_companies_created_by FOREIGN KEY (created_by_user_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE departments (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    company_id BIGINT       NOT NULL,
    name       VARCHAR(100) NOT NULL,
    created_at DATETIME     NOT NULL,
    updated_at DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_dept_company_name (company_id, name),
    CONSTRAINT fk_dept_company FOREIGN KEY (company_id) REFERENCES companies (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE company_members (
    id               BIGINT   NOT NULL AUTO_INCREMENT,
    company_id       BIGINT   NOT NULL,
    user_id          BIGINT   NOT NULL,
    department_id    BIGINT   NULL,
    role             ENUM('COMPANY_ADMIN','MEMBER')    NOT NULL,
    status           ENUM('ACTIVE','SUSPENDED','LEFT') NOT NULL DEFAULT 'ACTIVE',
    joined_at        DATETIME NULL,
    created_at       DATETIME NOT NULL,
    updated_at       DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_cm_company_user (company_id, user_id),
    KEY idx_cm_user (user_id),
    KEY idx_cm_department (department_id),
    CONSTRAINT fk_cm_company FOREIGN KEY (company_id) REFERENCES companies (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_cm_user FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_cm_department FOREIGN KEY (department_id) REFERENCES departments (id)
        ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE projects (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    company_id          BIGINT       NOT NULL,
    name                VARCHAR(150) NOT NULL,
    project_key         VARCHAR(20)  NOT NULL,
    description         TEXT         NULL,
    status              ENUM('PLANNING','ACTIVE','PAUSED','COMPLETED','ARCHIVED') NOT NULL DEFAULT 'PLANNING',
    start_date          DATE         NULL,
    end_date            DATE         NULL,
    created_by_user_id  BIGINT       NOT NULL,
    archived_at         DATETIME     NULL,
    archived_by_user_id BIGINT       NULL,
    archive_reason      TEXT         NULL,
    created_at          DATETIME     NOT NULL,
    updated_at          DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_projects_company_key (company_id, project_key),
    KEY idx_projects_created_by (created_by_user_id),
    KEY idx_projects_archived_by (archived_by_user_id),
    CONSTRAINT fk_projects_company FOREIGN KEY (company_id) REFERENCES companies (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_projects_created_by FOREIGN KEY (created_by_user_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_projects_archived_by FOREIGN KEY (archived_by_user_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_projects_date_range CHECK (
        end_date IS NULL OR start_date IS NULL OR start_date <= end_date
    ),
    CONSTRAINT chk_projects_archive_state CHECK (
        (status = 'ARCHIVED' AND archived_at IS NOT NULL AND archived_by_user_id IS NOT NULL) OR
        (status <> 'ARCHIVED' AND archived_at IS NULL AND archived_by_user_id IS NULL AND archive_reason IS NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE project_members (
    id               BIGINT   NOT NULL AUTO_INCREMENT,
    project_id       BIGINT   NOT NULL,
    user_id          BIGINT   NOT NULL,
    role             ENUM('PROJECT_ADMIN','MEMBER') NOT NULL,
    status           ENUM('ACTIVE','REMOVED')       NOT NULL DEFAULT 'ACTIVE',
    joined_at        DATETIME NULL,
    created_at       DATETIME NOT NULL,
    updated_at       DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_pm_project_user (project_id, user_id),
    KEY idx_pm_user (user_id),
    CONSTRAINT fk_pm_project FOREIGN KEY (project_id) REFERENCES projects (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_pm_user FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE sprints (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    project_id   BIGINT       NOT NULL,
    name         VARCHAR(150) NOT NULL,
    goal         TEXT         NULL,
    status       ENUM('PLANNED','ACTIVE','COMPLETED') NOT NULL DEFAULT 'PLANNED',
    start_date   DATE         NULL,
    end_date     DATE         NULL,
    started_at   DATETIME     NULL,
    completed_at DATETIME     NULL,
    summary      TEXT         NULL,
    created_at   DATETIME     NOT NULL,
    updated_at   DATETIME     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_sprints_project_status (project_id, status),
    CONSTRAINT fk_sprints_project FOREIGN KEY (project_id) REFERENCES projects (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_sprints_date_range CHECK (
        end_date IS NULL OR start_date IS NULL OR start_date <= end_date
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE invitations (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    company_id      BIGINT      NOT NULL,
    project_id      BIGINT      NULL,
    inviter_user_id BIGINT      NOT NULL,
    invitee_user_id BIGINT      NOT NULL,
    scope_type      ENUM('COMPANY','PROJECT') NOT NULL,
    role            VARCHAR(50) NOT NULL,
    status          ENUM('PENDING','ACCEPTED','DECLINED','CANCELED','EXPIRED') NOT NULL DEFAULT 'PENDING',
    expires_at      DATETIME    NOT NULL,
    accepted_at     DATETIME    NULL,
    declined_at     DATETIME    NULL,
    canceled_at     DATETIME    NULL,
    created_at      DATETIME    NOT NULL,
    updated_at      DATETIME    NOT NULL,
    PRIMARY KEY (id),
    KEY idx_inv_company (company_id),
    KEY idx_inv_project (project_id),
    KEY idx_inv_inviter (inviter_user_id),
    KEY idx_inv_invitee (invitee_user_id),
    CONSTRAINT fk_inv_company FOREIGN KEY (company_id) REFERENCES companies (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_inv_project FOREIGN KEY (project_id) REFERENCES projects (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_inv_inviter FOREIGN KEY (inviter_user_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_inv_invitee FOREIGN KEY (invitee_user_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_inv_scope CHECK (
        (scope_type = 'COMPANY' AND project_id IS NULL) OR
        (scope_type = 'PROJECT' AND project_id IS NOT NULL)
    ),
    CONSTRAINT chk_inv_self CHECK (inviter_user_id <> invitee_user_id),
    CONSTRAINT chk_inv_role CHECK (
        (scope_type = 'COMPANY' AND role IN ('COMPANY_ADMIN','MEMBER')) OR
        (scope_type = 'PROJECT' AND role IN ('PROJECT_ADMIN','MEMBER'))
    ),
    CONSTRAINT chk_inv_expiry CHECK (expires_at > created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE issues (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    project_id         BIGINT       NOT NULL,
    sprint_id          BIGINT       NULL,
    issue_key          VARCHAR(40)  NOT NULL,
    reporter_user_id   BIGINT       NOT NULL,
    assignee_user_id   BIGINT       NULL,
    source_type        ENUM('MANUAL','MESSAGE') NOT NULL DEFAULT 'MANUAL',
    source_message_id  BIGINT       NULL,
    title              VARCHAR(200) NOT NULL,
    description        TEXT         NULL,
    status             ENUM('BACKLOG','TODO','IN_PROGRESS','IN_REVIEW','DONE') NOT NULL DEFAULT 'BACKLOG',
    priority           ENUM('LOW','MEDIUM','HIGH','URGENT') NOT NULL DEFAULT 'MEDIUM',
    due_date           DATE         NULL,
    trashed_at         DATETIME     NULL,
    trashed_by_user_id BIGINT       NULL,
    restored_at        DATETIME     NULL,
    created_at         DATETIME     NOT NULL,
    updated_at         DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_issues_project_key (project_id, issue_key),
    KEY idx_issues_project_status (project_id, status),
    KEY idx_issues_project_sprint (project_id, sprint_id),
    KEY idx_issues_assignee_status (assignee_user_id, status),
    KEY idx_issues_reporter (reporter_user_id),
    KEY idx_issues_source_message (source_message_id),
    KEY idx_issues_trashed_by (trashed_by_user_id),
    CONSTRAINT fk_issues_project FOREIGN KEY (project_id) REFERENCES projects (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_issues_sprint FOREIGN KEY (sprint_id) REFERENCES sprints (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_issues_reporter FOREIGN KEY (reporter_user_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_issues_assignee FOREIGN KEY (assignee_user_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_issues_trashed_by FOREIGN KEY (trashed_by_user_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_issues_source CHECK (
        (source_type = 'MANUAL' AND source_message_id IS NULL) OR
        (source_type = 'MESSAGE' AND source_message_id IS NOT NULL)
    ),
    CONSTRAINT chk_issues_trash_pair CHECK (
        (trashed_at IS NULL AND trashed_by_user_id IS NULL) OR
        (trashed_at IS NOT NULL AND trashed_by_user_id IS NOT NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE issue_status_history (
    id                 BIGINT   NOT NULL AUTO_INCREMENT,
    issue_id           BIGINT   NOT NULL,
    changed_by_user_id BIGINT   NOT NULL,
    from_status        ENUM('BACKLOG','TODO','IN_PROGRESS','IN_REVIEW','DONE') NULL,
    to_status          ENUM('BACKLOG','TODO','IN_PROGRESS','IN_REVIEW','DONE') NOT NULL,
    changed_at         DATETIME NOT NULL,
    PRIMARY KEY (id),
    KEY idx_ish_issue_changed (issue_id, changed_at),
    KEY idx_ish_changed_by (changed_by_user_id),
    CONSTRAINT fk_ish_issue FOREIGN KEY (issue_id) REFERENCES issues (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_ish_changed_by FOREIGN KEY (changed_by_user_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE documents (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    project_id         BIGINT       NOT NULL,
    author_user_id     BIGINT       NOT NULL,
    title              VARCHAR(200) NOT NULL,
    document_type      ENUM('DOC','MEETING_NOTE') NOT NULL DEFAULT 'DOC',
    content            LONGTEXT     NOT NULL,
    trashed_at         DATETIME     NULL,
    trashed_by_user_id BIGINT       NULL,
    restored_at        DATETIME     NULL,
    created_at         DATETIME     NOT NULL,
    updated_at         DATETIME     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_documents_project_type (project_id, document_type),
    KEY idx_documents_author (author_user_id),
    KEY idx_documents_trashed_by (trashed_by_user_id),
    CONSTRAINT fk_documents_project FOREIGN KEY (project_id) REFERENCES projects (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_documents_author FOREIGN KEY (author_user_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_documents_trashed_by FOREIGN KEY (trashed_by_user_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_documents_trash_pair CHECK (
        (trashed_at IS NULL AND trashed_by_user_id IS NULL) OR
        (trashed_at IS NOT NULL AND trashed_by_user_id IS NOT NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE document_issue_links (
    id                 BIGINT   NOT NULL AUTO_INCREMENT,
    project_id         BIGINT   NOT NULL,
    document_id        BIGINT   NOT NULL,
    issue_id           BIGINT   NOT NULL,
    created_by_user_id BIGINT   NOT NULL,
    link_type          ENUM('RELATED') NOT NULL DEFAULT 'RELATED',
    created_at         DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_dil_document_issue (document_id, issue_id),
    KEY idx_dil_project (project_id),
    KEY idx_dil_issue (issue_id),
    KEY idx_dil_created_by (created_by_user_id),
    CONSTRAINT fk_dil_project FOREIGN KEY (project_id) REFERENCES projects (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_dil_document FOREIGN KEY (document_id) REFERENCES documents (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_dil_issue FOREIGN KEY (issue_id) REFERENCES issues (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_dil_created_by FOREIGN KEY (created_by_user_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE threads (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    project_id         BIGINT       NOT NULL,
    target_type        ENUM('ISSUE','DOCUMENT','MESSAGE') NOT NULL,
    target_id          BIGINT       NOT NULL,
    topic              VARCHAR(200) NULL,
    created_by_user_id BIGINT       NOT NULL,
    created_at         DATETIME     NOT NULL,
    updated_at         DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_threads_target (target_type, target_id),
    KEY idx_threads_project (project_id),
    KEY idx_threads_created_by (created_by_user_id),
    CONSTRAINT fk_threads_project FOREIGN KEY (project_id) REFERENCES projects (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_threads_created_by FOREIGN KEY (created_by_user_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE messages (
    id             BIGINT   NOT NULL AUTO_INCREMENT,
    project_id     BIGINT   NOT NULL,
    author_user_id BIGINT   NOT NULL,
    thread_id      BIGINT   NULL,
    content        TEXT     NOT NULL,
    created_at     DATETIME NOT NULL,
    PRIMARY KEY (id),
    KEY idx_messages_project_created (project_id, created_at, id),
    KEY idx_messages_author (author_user_id),
    KEY idx_messages_thread (thread_id),
    CONSTRAINT fk_messages_project FOREIGN KEY (project_id) REFERENCES projects (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_messages_author FOREIGN KEY (author_user_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_messages_thread FOREIGN KEY (thread_id) REFERENCES threads (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE issues
    ADD CONSTRAINT fk_issues_source_message FOREIGN KEY (source_message_id) REFERENCES messages (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT;

CREATE TABLE project_chat_member_states (
    project_id           BIGINT   NOT NULL,
    user_id              BIGINT   NOT NULL,
    last_read_message_id BIGINT   NULL,
    last_read_at         DATETIME NULL,
    updated_at           DATETIME NOT NULL,
    PRIMARY KEY (project_id, user_id),
    KEY idx_pcms_user (user_id),
    KEY idx_pcms_last_read (last_read_message_id),
    CONSTRAINT fk_pcms_project FOREIGN KEY (project_id) REFERENCES projects (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_pcms_user FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_pcms_last_read FOREIGN KEY (last_read_message_id) REFERENCES messages (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE direct_message_rooms (
    id         BIGINT   NOT NULL AUTO_INCREMENT,
    company_id BIGINT   NOT NULL,
    user1_id   BIGINT   NOT NULL,
    user2_id   BIGINT   NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_dmr_company_users (company_id, user1_id, user2_id),
    KEY idx_dmr_user1 (user1_id),
    KEY idx_dmr_user2 (user2_id),
    CONSTRAINT fk_dmr_company FOREIGN KEY (company_id) REFERENCES companies (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_dmr_user1 FOREIGN KEY (user1_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_dmr_user2 FOREIGN KEY (user2_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_dmr_user_order CHECK (user1_id < user2_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE direct_messages (
    id             BIGINT   NOT NULL AUTO_INCREMENT,
    room_id        BIGINT   NOT NULL,
    sender_user_id BIGINT   NOT NULL,
    content        TEXT     NOT NULL,
    created_at     DATETIME NOT NULL,
    PRIMARY KEY (id),
    KEY idx_dm_room_created (room_id, created_at, id),
    KEY idx_dm_sender (sender_user_id),
    CONSTRAINT fk_dm_room FOREIGN KEY (room_id) REFERENCES direct_message_rooms (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_dm_sender FOREIGN KEY (sender_user_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE dm_room_user_states (
    room_id                     BIGINT   NOT NULL,
    user_id                     BIGINT   NOT NULL,
    last_read_direct_message_id BIGINT   NULL,
    last_read_at                DATETIME NULL,
    updated_at                  DATETIME NOT NULL,
    PRIMARY KEY (room_id, user_id),
    KEY idx_dmrus_user (user_id),
    KEY idx_dmrus_last_read (last_read_direct_message_id),
    CONSTRAINT fk_dmrus_room FOREIGN KEY (room_id) REFERENCES direct_message_rooms (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_dmrus_user FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_dmrus_last_read FOREIGN KEY (last_read_direct_message_id) REFERENCES direct_messages (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE thread_comments (
    id             BIGINT   NOT NULL AUTO_INCREMENT,
    thread_id      BIGINT   NOT NULL,
    author_user_id BIGINT   NOT NULL,
    content        TEXT     NOT NULL,
    created_at     DATETIME NOT NULL,
    updated_at     DATETIME NOT NULL,
    PRIMARY KEY (id),
    KEY idx_tc_thread_id (thread_id, id),
    KEY idx_tc_author (author_user_id),
    CONSTRAINT fk_tc_thread FOREIGN KEY (thread_id) REFERENCES threads (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_tc_author FOREIGN KEY (author_user_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE activity_events (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    company_id    BIGINT      NOT NULL,
    project_id    BIGINT      NULL,
    actor_user_id BIGINT      NULL,
    event_type    VARCHAR(50) NOT NULL,
    target_type   VARCHAR(50) NOT NULL,
    target_id     BIGINT      NOT NULL,
    metadata_json JSON        NULL,
    created_at    DATETIME    NOT NULL,
    PRIMARY KEY (id),
    KEY idx_ae_company_project (company_id, project_id),
    KEY idx_ae_actor (actor_user_id),
    KEY idx_ae_target (target_type, target_id),
    CONSTRAINT fk_ae_company FOREIGN KEY (company_id) REFERENCES companies (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_ae_project FOREIGN KEY (project_id) REFERENCES projects (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_ae_actor FOREIGN KEY (actor_user_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE notification_events (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    activity_event_id BIGINT       NULL,
    company_id        BIGINT       NULL,
    project_id        BIGINT       NULL,
    actor_user_id     BIGINT       NULL,
    type              VARCHAR(50)  NOT NULL,
    target_type       VARCHAR(50)  NOT NULL,
    target_id         BIGINT       NOT NULL,
    title             VARCHAR(200) NOT NULL,
    body              TEXT         NULL,
    metadata_json     JSON         NULL,
    created_at        DATETIME     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_ne_activity (activity_event_id),
    KEY idx_ne_company_project_type (company_id, project_id, type),
    KEY idx_ne_actor (actor_user_id),
    KEY idx_ne_target (target_type, target_id),
    CONSTRAINT fk_ne_activity FOREIGN KEY (activity_event_id) REFERENCES activity_events (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_ne_company FOREIGN KEY (company_id) REFERENCES companies (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_ne_project FOREIGN KEY (project_id) REFERENCES projects (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_ne_actor FOREIGN KEY (actor_user_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE notification_recipients (
    id                    BIGINT   NOT NULL AUTO_INCREMENT,
    notification_event_id BIGINT   NOT NULL,
    recipient_user_id     BIGINT   NOT NULL,
    read_at               DATETIME NULL,
    created_at            DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_nr_event_recipient (notification_event_id, recipient_user_id),
    KEY idx_nr_recipient_read_created (recipient_user_id, read_at, created_at, id),
    CONSTRAINT fk_nr_event FOREIGN KEY (notification_event_id) REFERENCES notification_events (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_nr_recipient FOREIGN KEY (recipient_user_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE notification_preferences (
    id                BIGINT      NOT NULL AUTO_INCREMENT,
    user_id           BIGINT      NOT NULL,
    scope_type        ENUM('GLOBAL') NOT NULL DEFAULT 'GLOBAL',
    notification_type VARCHAR(50) NOT NULL,
    enabled           BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at        DATETIME    NOT NULL,
    updated_at        DATETIME    NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_np_user_scope_type (user_id, scope_type, notification_type),
    CONSTRAINT fk_np_user FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE project_dashboard_summaries (
    project_id              BIGINT       NOT NULL,
    issue_total_count       INT          NOT NULL DEFAULT 0,
    issue_done_count        INT          NOT NULL DEFAULT 0,
    issue_in_progress_count INT          NOT NULL DEFAULT 0,
    document_count          INT          NOT NULL DEFAULT 0,
    activity_count_today    INT          NOT NULL DEFAULT 0,
    sprint_id               BIGINT       NULL,
    sprint_progress_rate    DECIMAL(5,2) NULL,
    refreshed_at            DATETIME     NOT NULL,
    PRIMARY KEY (project_id),
    KEY idx_pds_sprint (sprint_id),
    CONSTRAINT fk_pds_project FOREIGN KEY (project_id) REFERENCES projects (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_pds_sprint FOREIGN KEY (sprint_id) REFERENCES sprints (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
