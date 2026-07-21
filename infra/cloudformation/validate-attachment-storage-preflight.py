#!/usr/bin/env python3
"""Fail-closed static preflight for the public Compose attachment storage source."""

from pathlib import Path


BASE = Path(__file__).parent
ROOT = BASE.parent.parent
template = (BASE / "attachment-storage-foundation.yaml").read_text(encoding="utf-8")
compose = (ROOT / "compose.dual-image.production.yml").read_text(encoding="utf-8")
helper = (ROOT / "scripts/ecr-compose-deploy/deploy-dual-release").read_text(encoding="utf-8")
runbook = (ROOT / "docs/00-project/attachment-storage-public-compose-runbook.md").read_text(encoding="utf-8")
runtime_example = (ROOT / "scripts/ecr-compose-deploy/attachment-storage-runtime.example.env").read_text(encoding="utf-8")
storage_config = (ROOT / "backend/src/main/java/com/threadia/backend/storage/S3StorageConfig.java").read_text(encoding="utf-8")
storage_sql = (ROOT / "scripts/ssm-delivery/attachment-storage-additive.sql").read_text(encoding="utf-8")
restricted_network = (BASE / "restricted-network.yaml").read_text(encoding="utf-8")

bucket_name = "threadia-attachments-${AWS::AccountId}-${AWS::Region}"
runtime_keys = (
    "THREADIA_STORAGE_S3_REGION",
    "THREADIA_STORAGE_S3_BUCKET",
    "THREADIA_STORAGE_S3_PATH_STYLE",
    "THREADIA_STORAGE_S3_PRESIGN_TTL",
    "THREADIA_STORAGE_ORPHAN_CLEANUP_ENABLED",
    "THREADIA_STORAGE_ORPHAN_CLEANUP_DELAY",
    "THREADIA_STORAGE_UPLOAD_CLEANUP_DELAY",
)

for required in (
    "Type: AWS::S3::Bucket",
    "BucketName: !Sub " + bucket_name,
    "BucketOwnerEnforced",
    "BlockPublicPolicy: true",
    "SSEAlgorithm: AES256",
    "VersioningConfiguration:",
    "ExpireNoncurrentAttachmentVersions",
    "RemoveExpiredDeleteMarkers",
    "ExpiredObjectDeleteMarker: true",
    "AbortIncompleteMultipartUploads",
    "CorsConfiguration:",
    "AllowedMethods: [GET, PUT]",
    "AllowedOrigins: [!Ref AllowedCorsOrigin]",
    "content-type",
    "x-amz-checksum-sha256",
    "DenyInsecureTransport",
    "AccessOnlyDedicatedAttachmentObjects",
    "Action: [s3:GetObject, s3:PutObject, s3:DeleteObject]",
):
    assert required in template, f"attachment template: missing {required}"

assert "ArtifactBucket" not in template
assert "releases/*" not in template
assert "GitHubDeployRole" not in template
assert "aws:SourceVpce" not in template
assert "AllowedHeaders: ['*']" not in template
assert "AllowedOrigins: ['*']" not in template
for key in runtime_keys:
    assert key + ": ${" + key + ":?" in compose
    assert key in helper

for forbidden in (
    "THREADIA_STORAGE_S3_ENDPOINT",
    "THREADIA_STORAGE_S3_ACCESS_KEY",
    "THREADIA_STORAGE_S3_SECRET_KEY",
):
    assert "forbidden_runtime_key " + forbidden in helper

assert "attachment storage must use the dedicated account-and-region-derived bucket" in helper
assert '[[ "$ttl" =~ ^PT([1-9]|10)M$ ]]' in helper
assert "attachment presign TTL must be between PT1M and PT10M" in helper
assert '[[ "$region" == ap-northeast-2 ]]' in helper
assert 'THREADIA_STORAGE_S3_REGION=ap-northeast-2' in runtime_example
assert 'THREADIA_STORAGE_S3_PATH_STYLE=false' in runtime_example
assert "PRODUCTION_REGION = \"ap-northeast-2\"" in storage_config
assert "PRODUCTION_BUCKET_PATTERN" in storage_config
assert "MAX_PRODUCTION_PRESIGN_TTL = Duration.ofMinutes(10)" in storage_config
assert "presign TTL은 10분을 초과할 수 없습니다." in storage_config
assert "DefaultCredentialsProvider.builder().build()" in storage_config
assert "CREATE TABLE IF NOT EXISTS attachments" in storage_sql
assert "fk_users_avatar_attachment" in storage_sql
assert "fk_companies_logo_attachment" in storage_sql
assert "CloudFormation operator는 Change Set 생성·조회만 하고 Execute하지 않는다" in runbook
assert "attachment-storage-additive.sql" in runbook
assert "aws:SourceVpce" in runbook
assert "PT1M`~`PT10M" in runbook
assert "threadia-attachments-${AWS::AccountId}-${AWS::Region}" not in restricted_network

print("Public Compose attachment storage preflight passed; no AWS, SSM, DB, or GitHub command was executed.")
