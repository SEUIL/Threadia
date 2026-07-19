#!/usr/bin/env python3
"""Execute the additive Platform foundation migration against a preserved-style MySQL baseline."""

import os
import subprocess
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
MIGRATION = ROOT / "scripts/ssm-delivery/platform-foundation-additive.sql"
SCHEMA = ROOT / "db/schema.sql"
MYSQL = [
    "mysql",
    "--batch",
    "--skip-column-names",
    "--host=127.0.0.1",
    "--port=" + os.environ.get("THREADIA_DB_PORT", "3306"),
    "--user=" + os.environ["THREADIA_DB_USERNAME"],
    "--database=threadia_ci",
]


def mysql(sql: str) -> str:
    result = subprocess.run(
        MYSQL,
        input=sql,
        text=True,
        capture_output=True,
        check=True,
        env={**os.environ, "MYSQL_PWD": os.environ["THREADIA_DB_PASSWORD"]},
    )
    return result.stdout


# This is an ephemeral CI fixture only. db/schema.sql is never used as a persistent migration.
mysql(SCHEMA.read_text(encoding="utf-8"))
mysql(
    """
    DROP TABLE admin_action_audits;
    DROP TABLE system_settings;
    DROP TABLE support_case_internal_notes;
    DROP TABLE support_case_public_replies;
    DROP TABLE support_cases;
    DROP TABLE platform_admins;
    DROP TABLE platform_admin_bootstrap_locks;
    """
)

for _ in range(2):
    mysql(MIGRATION.read_text(encoding="utf-8"))

table_count = mysql(
    """
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name IN (
          'platform_admin_bootstrap_locks', 'platform_admins', 'system_settings',
          'support_cases', 'support_case_public_replies',
          'support_case_internal_notes', 'admin_action_audits'
      );
    """
).strip()
assert table_count == "7", table_count

platform_admin_count = mysql("SELECT COUNT(*) FROM platform_admins;").strip()
assert platform_admin_count == "0", platform_admin_count

expected_columns = {
    ("companies", "status", "varchar"),
    ("platform_admins", "status", "enum"),
    ("platform_admins", "revoked_at", "datetime"),
    ("platform_admins", "reactivated_at", "datetime"),
    ("platform_admins", "updated_at", "datetime"),
    ("admin_action_audits", "action", "enum"),
    ("admin_action_audits", "target_type", "enum"),
}
actual_columns = {
    tuple(row.split("\t"))
    for row in mysql(
        """
        SELECT table_name, column_name, data_type
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND (table_name = 'companies' AND column_name = 'status'
               OR table_name = 'platform_admins'
                  AND column_name IN ('status', 'revoked_at', 'reactivated_at', 'updated_at')
               OR table_name = 'admin_action_audits'
                  AND column_name IN ('action', 'target_type'));
        """
    ).strip().splitlines()
}
assert expected_columns <= actual_columns, actual_columns

expected_indexes = {
    ("platform_admins", "idx_platform_admins_status_role"),
    ("admin_action_audits", "idx_aaa_created"),
}
actual_indexes = {
    tuple(row.split("\t"))
    for row in mysql(
        """
        SELECT table_name, index_name
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND (table_name = 'platform_admins' AND index_name = 'idx_platform_admins_status_role'
               OR table_name = 'admin_action_audits' AND index_name = 'idx_aaa_created');
        """
    ).strip().splitlines()
}
assert expected_indexes <= actual_indexes, actual_indexes

constraints = set(
    mysql(
        """
        SELECT constraint_name
        FROM information_schema.table_constraints
        WHERE constraint_schema = DATABASE()
          AND constraint_name IN ('chk_platform_admins_revocation', 'chk_aaa_authority');
        """
    ).strip().splitlines()
)
assert constraints == {"chk_platform_admins_revocation", "chk_aaa_authority"}, constraints

enum_types = mysql(
    """
    SELECT column_name, column_type
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'admin_action_audits'
      AND column_name IN ('action', 'target_type')
    ORDER BY column_name;
    """
)
for value in (
    "PLATFORM_ADMIN_GRANTED",
    "PLATFORM_ADMIN_REVOKED",
    "PLATFORM_ADMIN_REACTIVATED",
    "PLATFORM_ADMIN_ROLE_CHANGED",
    "PLATFORM_ADMIN",
):
    assert value in enum_types, enum_types

print("Platform foundation additive migration and Phase 1 empty-admin fixture passed.")
