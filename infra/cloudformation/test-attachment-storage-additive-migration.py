#!/usr/bin/env python3
"""Execute the attachment additive migration against fresh and partially applied MySQL baselines."""

import os
import subprocess
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
MIGRATION = ROOT / "scripts/ssm-delivery/attachment-storage-additive.sql"
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


def legacy_baseline() -> None:
    mysql(SCHEMA.read_text(encoding="utf-8"))
    mysql(
        """
        ALTER TABLE users DROP FOREIGN KEY fk_users_avatar_attachment;
        ALTER TABLE companies DROP FOREIGN KEY fk_companies_logo_attachment;
        ALTER TABLE users DROP COLUMN avatar_attachment_id;
        ALTER TABLE companies DROP COLUMN logo_attachment_id;
        DROP TABLE attachments;
        """
    )


def assert_attachment_contract() -> None:
    table_count = mysql(
        """
        SELECT COUNT(*)
        FROM information_schema.tables
        WHERE table_schema = DATABASE() AND table_name = 'attachments';
        """
    ).strip()
    assert table_count == "1", table_count

    expected_columns = {
        ("attachments", "object_key", "varchar(500)", "NO"),
        ("users", "avatar_attachment_id", "bigint", "YES"),
        ("companies", "logo_attachment_id", "bigint", "YES"),
    }
    actual_columns = {
        tuple(row.split("\t"))
        for row in mysql(
            """
            SELECT table_name, column_name, column_type, is_nullable
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND (table_name = 'attachments' AND column_name = 'object_key'
                   OR table_name = 'users' AND column_name = 'avatar_attachment_id'
                   OR table_name = 'companies' AND column_name = 'logo_attachment_id');
            """
        ).strip().splitlines()
    }
    assert expected_columns <= actual_columns, actual_columns

    expected_indexes = {
        ("attachments", "uq_attachments_object_key"),
        ("users", "idx_users_avatar_attachment"),
        ("companies", "idx_companies_logo_attachment"),
    }
    actual_indexes = {
        tuple(row.split("\t"))
        for row in mysql(
            """
            SELECT table_name, index_name
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND index_name IN (
                  'uq_attachments_object_key', 'idx_users_avatar_attachment',
                  'idx_companies_logo_attachment'
              );
            """
        ).strip().splitlines()
    }
    assert expected_indexes <= actual_indexes, actual_indexes

    expected_constraints = {
        "fk_attachments_company",
        "fk_attachments_created_by",
        "fk_users_avatar_attachment",
        "fk_companies_logo_attachment",
        "chk_attachments_lifecycle",
        "chk_attachments_length",
    }
    actual_constraints = set(
        mysql(
            """
            SELECT constraint_name
            FROM information_schema.table_constraints
            WHERE constraint_schema = DATABASE()
              AND constraint_name IN (
                  'fk_attachments_company', 'fk_attachments_created_by',
                  'fk_users_avatar_attachment', 'fk_companies_logo_attachment',
                  'chk_attachments_lifecycle', 'chk_attachments_length'
              );
            """
        ).strip().splitlines()
    )
    assert expected_constraints <= actual_constraints, actual_constraints


def run_and_verify() -> None:
    mysql(MIGRATION.read_text(encoding="utf-8"))
    mysql(MIGRATION.read_text(encoding="utf-8"))
    assert_attachment_contract()


legacy_baseline()
run_and_verify()

legacy_baseline()
partial_migration = MIGRATION.read_text(encoding="utf-8").split(
    "-- MySQL 8 does not support ADD COLUMN IF NOT EXISTS", 1
)[0]
mysql(partial_migration)
run_and_verify()

print("Attachment additive migration fresh, partial, and retry fixtures passed.")
