#!/usr/bin/env python3
"""Fail-closed static preflight for the SSM-only restricted delivery source."""

import json
import re
from pathlib import Path

BASE = Path(__file__).parent
template = (BASE / "restricted-ssm-delivery-foundation.yaml").read_text(encoding="utf-8")
network = (BASE / "restricted-network.yaml").read_text(encoding="utf-8")
service_policy = json.loads(
    (BASE / "restricted-ssm-delivery-foundation-service-permissions-policy.json").read_text(encoding="utf-8")
)
operator_policy = json.loads(
    (BASE / "restricted-ssm-delivery-foundation-operator-policy.json").read_text(encoding="utf-8")
)
parameters = json.loads(
    (BASE / "restricted-ssm-delivery-foundation-parameters.example.json").read_text(encoding="utf-8")
)
trust_sources = {
    name: json.loads((BASE / name).read_text(encoding="utf-8"))
    for name in (
        "restricted-ssm-delivery-operator-role-trust-policy.json",
        "restricted-ssm-delivery-bootstrap-assume-policy.json",
        "restricted-ssm-delivery-service-role-trust-policy.json",
        "restricted-ssm-delivery-github-oidc-role-trust-policy.json",
        "restricted-ssm-delivery-ec2-role-trust-policy.json",
    )
}
workflow = (BASE.parent.parent / ".github/workflows/restricted-ssm-release.yml").read_text(encoding="utf-8")
helper_artifact_workflow = (BASE.parent.parent / ".github/workflows/restricted-ssm-helper-artifact.yml").read_text(encoding="utf-8")
helper_maintenance_workflow = (BASE.parent.parent / ".github/workflows/restricted-ssm-helper-maintenance.yml").read_text(encoding="utf-8")
release_helper = (BASE.parent.parent / "scripts/ssm-delivery/apply-release").read_text(encoding="utf-8")
service_unit = (BASE.parent.parent / "scripts/ssm-delivery/threadia-backend.service").read_text(encoding="utf-8")
bootstrap = (BASE.parent.parent / "scripts/ssm-delivery/bootstrap-host.sh").read_text(encoding="utf-8")
runbook = (BASE.parent.parent / "docs/00-project/restricted-ssm-release-runbook.md").read_text(encoding="utf-8")
architecture = (BASE.parent.parent / "docs/05-architecture/GitHub-OIDC-SSM-EC2-배포-설계.md").read_text(encoding="utf-8")
version_pattern = "^[A-Za-z0-9._+=/-]{1,1000}[A-Za-z0-9._+=/-]{0,24}$"
required_runtime_parameters = {
    "/threadia/restricted-nonprod/runtime/THREADIA_DB_URL",
    "/threadia/restricted-nonprod/runtime/THREADIA_DB_USERNAME",
    "/threadia/restricted-nonprod/runtime/THREADIA_DB_PASSWORD",
    "/threadia/restricted-nonprod/runtime/THREADIA_JWT_SECRET",
    "/threadia/restricted-nonprod/runtime/THREADIA_COOKIE_SECURE",
    "/threadia/restricted-nonprod/runtime/THREADIA_CORS_ALLOWED_ORIGIN",
    "/threadia/restricted-nonprod/runtime/THREADIA_PLATFORM_ADMIN_BOOTSTRAP_EMAIL",
}
optional_mail_runtime_parameters = {
    "/threadia/restricted-nonprod/runtime/MAIL_HOST",
    "/threadia/restricted-nonprod/runtime/MAIL_PORT",
    "/threadia/restricted-nonprod/runtime/MAIL_USERNAME",
    "/threadia/restricted-nonprod/runtime/MAIL_PASSWORD",
    "/threadia/restricted-nonprod/runtime/NOTIFICATION_FROM_EMAIL",
}
expected_parameter_keys = {
    "NetworkStackName",
    "ExistingEc2InstanceId",
    "ExistingEc2RoleName",
    "ArtifactNoncurrentVersionRetentionDays",
    "HelperArtifactKey",
    "HelperArtifactVersionId",
    "HelperArtifactSha256",
    "HelperArtifactKeyV2",
    "HelperArtifactVersionIdV2",
    "HelperArtifactSha256V2",
}

for source_name, source in (("template", template), ("network", network)):
    for prohibited in ("AWS::CodeDeploy", "AWS-RunShellScript", "NatGateway", "MapPublicIpOnLaunch: true", "0.0.0.0/0"):
        assert prohibited not in source, f"{source_name}: prohibited {prohibited}"

assert "Type: AWS::EC2::RouteTable" in network
assert "Type: AWS::EC2::SubnetRouteTableAssociation" in network
assert "Type: AWS::EC2::VPCEndpoint" in network and "VpcEndpointType: Gateway" in network
assert "com.amazonaws.${AWS::Region}.s3" in network
assert "S3ManagedPrefixListId:" in network and "DestinationPrefixListId: !Ref S3ManagedPrefixListId" in network
assert "S3GatewayEndpointId:" in network
assert "PolicyDocument:" in network
assert "AllowOnlyVersionedArtifactRead" in network
assert "s3:GetObjectVersion" in network and "s3:ListBucket" not in network.split("S3GatewayEndpoint:", 1)[1]
expected_artifact_bucket = "threadia-restricted-${AWS::AccountId}-${AWS::Region}-artifacts"
assert expected_artifact_bucket in network and expected_artifact_bucket in template
artifact_bucket_statement = next(
    statement
    for statement in service_policy["Statement"]
    if statement["Sid"] == "AllowManageExactArtifactBucket"
)
assert set(artifact_bucket_statement["Action"]) == {
    "s3:DeleteBucket",
    "s3:DeleteBucketPolicy",
    "s3:GetBucketEncryption",
    "s3:GetBucketLifecycleConfiguration",
    "s3:GetBucketOwnershipControls",
    "s3:GetBucketPolicy",
    "s3:GetBucketPublicAccessBlock",
    "s3:GetBucketTagging",
    "s3:GetBucketVersioning",
    "s3:GetEncryptionConfiguration",
    "s3:GetLifecycleConfiguration",
    "s3:PutBucketEncryption",
    "s3:PutBucketLifecycleConfiguration",
    "s3:PutBucketOwnershipControls",
    "s3:PutBucketPolicy",
    "s3:PutBucketPublicAccessBlock",
    "s3:PutBucketTagging",
    "s3:PutBucketVersioning",
    "s3:PutEncryptionConfiguration",
    "s3:PutLifecycleConfiguration",
}

for required in (
    "AWS::IAM::OIDCProvider",
    "AWS::S3::Bucket",
    "BucketOwnerEnforced",
    "BlockPublicPolicy: true",
    "VersioningConfiguration:",
    "SSEAlgorithm: AES256",
    "DenyInsecureTransport",
    "AWS::SSM::Document",
    "DocumentType: Command",
    "TargetType: /AWS::EC2::Instance",
    "aws:runShellScript",
    "ArtifactVersionId",
    "ArtifactSha256",
    "ReleaseId",
    "aws:SourceVpce",
):
    assert required in template, f"template: missing {required}"

assert "token.actions.githubusercontent.com:aud: sts.amazonaws.com" in template
assert "token.actions.githubusercontent.com:sub: repo:SEUIL/Threadia:environment:restricted-nonprod" in template
assert "StringLike:" not in template.split("GitHubDeployRole:", 1)[1].split("ExistingEc2DeliveryPolicy:", 1)[0]
github_role = template.split("GitHubDeployRole:", 1)[1].split("ExistingEc2DeliveryPolicy:", 1)[0]
assert "${ReleaseCommandDocument}" in github_role
assert "!Sub arn:${AWS::Partition}:ssm:${AWS::Region}:${AWS::AccountId}:document/threadia-restricted-ssm-delivery-helper-maintenance-v2" in github_role
assert "${HelperMaintenanceCommandDocumentV2}" not in github_role
assert "${HelperMaintenanceCommandDocument}" not in github_role
assert "${ExistingEc2InstanceId}" in github_role
assert "ssm:GetParametersByPath" not in template
assert "secretsmanager:" not in template.lower()
assert "ManageMasterUserPassword" not in template
runtime_parameter_arns = set(re.findall(r"parameter(/threadia/restricted-nonprod/runtime/[A-Z_]+)", template))
assert runtime_parameter_arns == required_runtime_parameters | optional_mail_runtime_parameters
assert "Resource: '*'" in template and "Action: ssm:GetCommandInvocation" in template
assert "AWS-RunShellScript" not in template
assert f"allowedPattern: '{version_pattern}'" in template
assert "HelperArtifactKey:" in template
assert "HelperArtifactVersionId:" in template
assert "HelperArtifactSha256:" in template
assert "HelperArtifactKeyV2:" in template
assert "HelperArtifactVersionIdV2:" in template
assert "HelperArtifactSha256V2:" in template
template_parameters = template.split("Parameters:\n", 1)[1].split("\nResources:", 1)[0]
assert set(re.findall(r"^  ([A-Za-z0-9]+):\n    Type:", template_parameters, re.MULTILINE)) == expected_parameter_keys


def parameter_definition(name):
    return re.search(
        rf"^  {re.escape(name)}:\n(?P<definition>(?:    .+(?:\n|$))*)",
        template_parameters,
        re.MULTILINE,
    ).group("definition")


for legacy_parameter, v2_parameter in (
    ("HelperArtifactKey", "HelperArtifactKeyV2"),
    ("HelperArtifactVersionId", "HelperArtifactVersionIdV2"),
    ("HelperArtifactSha256", "HelperArtifactSha256V2"),
):
    legacy_definition = parameter_definition(legacy_parameter)
    v2_definition = parameter_definition(v2_parameter)
    assert re.search(r"^    Type: .+$", legacy_definition, re.MULTILINE).group() == re.search(
        r"^    Type: .+$", v2_definition, re.MULTILINE
    ).group()
    assert re.search(r"^    AllowedPattern: .+$", legacy_definition, re.MULTILINE).group() == re.search(
        r"^    AllowedPattern: .+$", v2_definition, re.MULTILINE
    ).group()
assert "HelperMaintenanceCommandDocument:" in template
assert template.count("HelperMaintenanceCommandDocumentV2:") == 1
helper_document_v2 = template.split("HelperMaintenanceCommandDocumentV2:", 1)[1].split("HelperMaintenanceCommandDocument:", 1)[0]
assert "Name: threadia-restricted-ssm-delivery-helper-maintenance-v2" in helper_document_v2
assert "UpdateMethod: NewVersion" in helper_document_v2
assert "--version-id '${HelperArtifactVersionIdV2}'" in helper_document_v2
assert "--key '${HelperArtifactKeyV2}'" in helper_document_v2
assert helper_document_v2.count("'${HelperArtifactKeyV2}'") == 1
assert helper_document_v2.count("'${HelperArtifactVersionIdV2}'") == 1
assert helper_document_v2.count("'${HelperArtifactSha256V2}'") == 3
assert "'${HelperArtifactKey}'" not in helper_document_v2
assert "'${HelperArtifactVersionId}'" not in helper_document_v2
assert "'${HelperArtifactSha256}'" not in helper_document_v2
helper_document = template.split("HelperMaintenanceCommandDocument:", 1)[1].split("Outputs:", 1)[0]
assert "parameters:" not in helper_document
assert "{{ " not in helper_document
assert "aws:runShellScript" in helper_document
assert "aws s3api get-object" in helper_document
legacy_helper_get_object = (
    "aws s3api get-object --region '${AWS::Region}' --bucket '${ArtifactBucket}' "
    "--key '${HelperArtifactKey}' --version-id '${HelperArtifactVersionId}' \"$DOWNLOAD\" >/dev/null"
)
assert helper_document.count(legacy_helper_get_object) == 1
assert helper_document.count("'${HelperArtifactKey}'") == 1
assert helper_document.count("'${HelperArtifactVersionId}'") == 1
assert helper_document.count("'${HelperArtifactSha256}'") == 3
assert "'${HelperArtifactKeyV2}'" not in helper_document
assert "'${HelperArtifactVersionIdV2}'" not in helper_document
assert "'${HelperArtifactSha256V2}'" not in helper_document
assert "bash -n \"$DOWNLOAD\"" in helper_document
assert "bash -n \"$STAGED\"" in helper_document
assert "bash -n \"$TARGET\"" in helper_document
assert "install -o root -g root -m 0750" in helper_document
assert 'readonly HOST_LOCK=/run/threadia-release-maintenance.lock' in helper_document
assert "flock cp" in helper_document
assert 'exec 9>"$HOST_LOCK"' in helper_document
assert 'flock -n 9 || {' in helper_document
assert helper_document.index('exec 9>"$HOST_LOCK"') < helper_document.index('readonly DOWNLOAD=')
assert 'readonly BACKUP="$(mktemp /usr/local/lib/threadia-release/.apply-release.backup.XXXXXX)"' in helper_document
assert 'cp -p "$TARGET" "$BACKUP"' in helper_document
assert "backup_ready=false" in helper_document
assert 'backup_ready=true\n                    mv -Tf "$STAGED" "$TARGET"' in helper_document
assert '"$backup_ready" == true && -f "$BACKUP"' in helper_document
assert 'mv -Tf "$STAGED" "$TARGET"' in helper_document
assert 'mv -Tf "$BACKUP" "$TARGET"' in helper_document
assert 'mv -Tf "$TARGET" "$BACKUP"' not in helper_document
assert "trap 'restore \"$?\"' ERR EXIT" in helper_document
assert "trap 'restore 130' INT" in helper_document
assert "trap 'restore 143' TERM" in helper_document
assert "success=true" in helper_document and "trap - ERR INT TERM EXIT" in helper_document
for prohibited in ("systemctl", "get-parameter", "get_parameter", "secretsmanager", "restart "):
    assert prohibited not in helper_document, f"helper document: prohibited {prohibited}"
assert f'[[ "$version_id" =~ {version_pattern} ]] || fail "invalid version ID"' in release_helper

compiled_version_pattern = re.compile(version_pattern)
assert not compiled_version_pattern.fullmatch("")
assert compiled_version_pattern.fullmatch("a")
assert compiled_version_pattern.fullmatch("a" * 1000)
assert compiled_version_pattern.fullmatch("a" * 1001)
assert compiled_version_pattern.fullmatch("a" * 1024)
assert not compiled_version_pattern.fullmatch("a" * 1025)
assert not compiled_version_pattern.fullmatch("a" * 1023 + "!")

service_text = json.dumps(service_policy)
assert "codedeploy:*" in service_text
assert "ssm:GetParametersByPath" in service_text
assert "ssm:SendCommand" in service_text
assert "secretsmanager:*" in service_text
assert "AllowManageFixedCommandDocument" in service_text
expected_document_lifecycle_actions = {
    "ssm:CreateDocument",
    "ssm:DescribeDocument",
    "ssm:GetDocument",
    "ssm:UpdateDocument",
    "ssm:UpdateDocumentDefaultVersion",
    "ssm:DeleteDocument",
    "ssm:AddTagsToResource",
    "ssm:RemoveTagsFromResource",
}


def require_exact_document_lifecycle_statement(sid, resource):
    statements = [statement for statement in service_policy["Statement"] if statement.get("Sid") == sid]
    assert len(statements) == 1, f"service policy: expected exactly one {sid} statement"
    statement = statements[0]
    assert statement["Effect"] == "Allow"
    actions = statement["Action"]
    assert isinstance(actions, list) and len(actions) == len(expected_document_lifecycle_actions)
    assert set(actions) == expected_document_lifecycle_actions
    assert statement["Resource"] == resource


require_exact_document_lifecycle_statement(
    "AllowManageFixedCommandDocument",
    "arn:aws:ssm:ap-northeast-2:<AWS_ACCOUNT_ID>:document/threadia-restricted-ssm-delivery-ReleaseCommandDocument-*",
)
require_exact_document_lifecycle_statement(
    "AllowManageFixedHelperMaintenanceCommandDocument",
    "arn:aws:ssm:ap-northeast-2:<AWS_ACCOUNT_ID>:document/threadia-restricted-ssm-delivery-HelperMaintenanceCommandDocument-*",
)
require_exact_document_lifecycle_statement(
    "ManageThreadiaRestrictedSsmDeliveryHelperMaintenanceV2",
    "arn:aws:ssm:ap-northeast-2:<AWS_ACCOUNT_ID>:document/threadia-restricted-ssm-delivery-helper-maintenance-v2",
)
assert "restricted-ssm-delivery-GitHubDeployRole-*" in service_text
operator_text = json.dumps(operator_policy)
assert "threadia-restricted-ssm-delivery" in operator_text
assert "ThreadiaRestrictedSsmDeliveryCloudFormationServiceRole" in operator_text
assert "cloudformation:ExecuteChangeSet" not in operator_text
assert "sts:AssumeRole" not in operator_text
operator_actions = {
    action
    for statement in operator_policy["Statement"]
    for action in (statement["Action"] if isinstance(statement["Action"], list) else [statement["Action"]])
}
assert operator_actions == {
    "cloudformation:CreateChangeSet",
    "cloudformation:DescribeChangeSet",
    "cloudformation:DeleteChangeSet",
    "cloudformation:DescribeStacks",
    "cloudformation:DescribeStackEvents",
    "cloudformation:DescribeStackResources",
    "cloudformation:GetTemplate",
    "iam:PassRole",
}
pass_role = next(statement for statement in operator_policy["Statement"] if statement["Action"] == "iam:PassRole")
assert pass_role["Resource"] == "arn:aws:iam::<AWS_ACCOUNT_ID>:role/ThreadiaRestrictedSsmDeliveryCloudFormationServiceRole"
assert pass_role["Condition"] == {"StringEquals": {"iam:PassedToService": "cloudformation.amazonaws.com"}}
assert isinstance(parameters, list) and len(parameters) == 10
assert all(not re.search(r"(?<![0-9])[0-9]{12}(?![0-9])", json.dumps(item)) for item in parameters)
assert {item["ParameterKey"] for item in parameters} == expected_parameter_keys
assert trust_sources["restricted-ssm-delivery-operator-role-trust-policy.json"]["Statement"][0]["Principal"] == {"AWS": "arn:aws:iam::<AWS_ACCOUNT_ID>:user/ThreadiaBootstrapOperator"}
assert trust_sources["restricted-ssm-delivery-bootstrap-assume-policy.json"]["Statement"][0]["Resource"] == "arn:aws:iam::<AWS_ACCOUNT_ID>:role/ThreadiaRestrictedSsmDeliveryOperatorRole"
assert trust_sources["restricted-ssm-delivery-service-role-trust-policy.json"]["Statement"][0]["Principal"] == {"Service": "cloudformation.amazonaws.com"}
oidc_statement = trust_sources["restricted-ssm-delivery-github-oidc-role-trust-policy.json"]["Statement"][0]
assert oidc_statement["Condition"]["StringEquals"] == {
    "token.actions.githubusercontent.com:aud": "sts.amazonaws.com",
    "token.actions.githubusercontent.com:sub": "repo:SEUIL/Threadia:environment:restricted-nonprod",
}
assert trust_sources["restricted-ssm-delivery-ec2-role-trust-policy.json"]["Statement"][0]["Principal"] == {"Service": "ec2.amazonaws.com"}
for action, sha in (
    ("actions/checkout", "34e114876b0b11c390a56381ad16ebd13914f8d5"),
    ("actions/setup-java", "c1e323688fd81a25caa38c78aa6df2d33d3e20d9"),
    ("aws-actions/configure-aws-credentials", "7474bc4690e29a8392af63c5b98e7449536d5c3a"),
):
    assert f"uses: {action}@{sha}" in workflow
assert "cp -aL \"$JAVA_HOME\" \"$stage/runtime\"" in workflow
assert "test ! -L \"$stage/runtime\"" in workflow
assert "tar -C \"$stage\" -czf \"$archive\" app.jar app.jar.sha256 manifest.json runtime" in workflow
release_job = workflow.split("  release:", 1)[1]
assert "    if: github.ref == 'refs/heads/main'" in release_job
assert "    concurrency:\n      group: restricted-ssm-host-change\n      cancel-in-progress: false" in release_job
assert "    timeout-minutes: 30" in release_job
assert 'aws s3api put-object --region "$AWS_REGION"' in release_job
assert 'aws ssm send-command --region "$AWS_REGION"' in release_job
assert 'aws ssm get-command-invocation --region "$AWS_REGION"' in release_job
assert "--cli-connect-timeout 5 --cli-read-timeout 10" in release_job
for source in (helper_artifact_workflow, helper_maintenance_workflow):
    assert "on:\n  workflow_dispatch:" in source
    assert "push:" not in source
    assert "if: github.ref == 'refs/heads/main'" in source
    assert "environment: restricted-nonprod" in source
    assert "actions/checkout@34e114876b0b11c390a56381ad16ebd13914f8d5" in source
    assert "aws-actions/configure-aws-credentials@7474bc4690e29a8392af63c5b98e7449536d5c3a" in source

assert 'helper=scripts/ssm-delivery/apply-release' in helper_artifact_workflow
assert 'key="releases/${GITHUB_SHA}/${GITHUB_RUN_ID}-${GITHUB_RUN_ATTEMPT}/apply-release"' in helper_artifact_workflow
assert "sha256sum \"$helper\"" in helper_artifact_workflow
assert "aws s3api put-object" in helper_artifact_workflow
assert "--body scripts/ssm-delivery/apply-release" in helper_artifact_workflow
assert "--query VersionId --output text" in helper_artifact_workflow
assert '[[ -n "$version_id" && "$version_id" != None && "$version_id" != null && "$version_id" =~ ^[A-Za-z0-9._+=/-]{1,1000}[A-Za-z0-9._+=/-]{0,24}$ ]]' in helper_artifact_workflow
assert "aws ssm" not in helper_artifact_workflow
assert "helper_key=" in helper_artifact_workflow
assert "helper_version_id=" in helper_artifact_workflow
assert "helper_sha256=" in helper_artifact_workflow
assert "commit=" in helper_artifact_workflow and "run=" in helper_artifact_workflow

assert "THREADIA_SSM_HELPER_MAINTENANCE_DOCUMENT_NAME" in helper_maintenance_workflow
assert "THREADIA_DELIVERY_INSTANCE_ID" in helper_maintenance_workflow
assert "    concurrency:\n      group: restricted-ssm-host-change\n      cancel-in-progress: false" in helper_maintenance_workflow
maintenance_command = helper_maintenance_workflow.split("aws ssm send-command", 1)[1].split("--query", 1)[0]
assert "--document-name \"$THREADIA_SSM_HELPER_MAINTENANCE_DOCUMENT_NAME\"" in maintenance_command
assert "--instance-ids \"$THREADIA_DELIVERY_INSTANCE_ID\"" in maintenance_command
assert "--parameters" not in maintenance_command
assert "aws ssm get-command-invocation" in helper_maintenance_workflow
assert "--cli-connect-timeout 5 --cli-read-timeout 10" in helper_maintenance_workflow
assert "for attempt in $(seq 1 60); do" in helper_maintenance_workflow
assert '"$GITHUB_STEP_SUMMARY"' in helper_maintenance_workflow
assert "document_name=%s" in helper_maintenance_workflow
assert "instance_id=%s" in helper_maintenance_workflow
assert "terminal_status=%s" in helper_maintenance_workflow
assert "StandardErrorContent:StandardErrorContent" in helper_maintenance_workflow
assert "StandardOutputContent:StandardOutputContent" in helper_maintenance_workflow
assert "cut -c1-4000" in helper_maintenance_workflow
assert "java " not in release_helper.split("for command", 1)[1].split("umask", 1)[0]
assert "runtime/bin/java" in release_helper and "ExecStart=/opt/threadia/current/runtime/bin/java" in service_unit
assert "yum " not in bootstrap and "apt " not in bootstrap and "http" not in bootstrap
document_parameters = template.split("parameters:", 1)[1].split("mainSteps:", 1)[0]
assert all(name in document_parameters for name in ("ArtifactKey:", "ArtifactVersionId:", "ArtifactSha256:", "ReleaseId:"))
assert "Command:" not in document_parameters and "runCommand:" not in document_parameters
assert "AWS_REGION:" not in document_parameters and "AWS_DEFAULT_REGION:" not in document_parameters
assert "AWS_REGION='${AWS::Region}' AWS_DEFAULT_REGION='${AWS::Region}'\n                  /usr/local/lib/threadia-release/apply-release" in template
assert '[[ -n "${AWS_REGION:-}" ]] || fail "AWS_REGION is required"' in release_helper
assert '[[ "$AWS_REGION" =~ ^[a-z]{2}(-[a-z0-9]+)+-[0-9]+$ ]] || fail "invalid AWS_REGION"' in release_helper
assert 'aws s3api get-object --region "$AWS_REGION"' in release_helper
assert 'aws ssm get-parameters --region "$AWS_REGION"' in release_helper
assert release_helper.count('aws ssm get-parameters --region "$AWS_REGION" --with-decryption --names') == 2
assert all(parameter in release_helper for parameter in required_runtime_parameters | optional_mail_runtime_parameters)
assert 'if not returned and invalid == set(optional):' in release_helper
assert 'if returned != set(optional) or invalid:' in release_helper
assert 'raise SystemExit("optional runtime parameter is blank: " + parameter_name)' in release_helper
assert 'raise SystemExit("optional runtime parameter contains an unsupported control character: " + parameter_name)' in release_helper
assert 'previous_env="$stage/runtime.env.previous"' in release_helper
assert 'cp -a "$RUNTIME_ENV" "$previous_env"' in release_helper
assert 'install -o root -g threadia -m 0640 "$previous_env" "$RUNTIME_ENV"' in release_helper
assert 'rm -f "$RUNTIME_ENV"' in release_helper
assert 'ln -sfn "$previous_target" "$ACTIVE_LINK.rollback"' in release_helper
assert 'rm -f "$ACTIVE_LINK"' in release_helper
assert "readonly READINESS_TIMEOUT_SECONDS=120" in release_helper
assert "readiness_deadline=$((SECONDS + READINESS_TIMEOUT_SECONDS))" in release_helper
assert 'systemctl is-active --quiet "$SERVICE_NAME"' in release_helper
assert "http://127.0.0.1:8080/actuator/health" in release_helper
assert "Environment=MANAGEMENT_HEALTH_MAIL_ENABLED=false" in service_unit
assert 'readonly HOST_LOCK=/run/threadia-release-maintenance.lock' in release_helper
assert "curl install uname flock" in release_helper
assert 'exec 9>"$HOST_LOCK"' in release_helper
assert 'flock -n 9 || fail "another release or helper maintenance operation is active"' in release_helper
assert "Environment=MANAGEMENT_HEALTH_MAIL_ENABLED=false" in runbook
assert "Service-unit maintenance는 helper maintenance와 별도 channel" in runbook
assert "Helper maintenance는 `apply-release`만 갱신한다." in architecture
assert "Service-unit maintenance는 release helper maintenance와 분리된 manual-only channel" in architecture
assert (BASE.parent.parent / "scripts/ssm-delivery/apply-release").stat().st_mode & 0o111
assert (BASE.parent.parent / "scripts/ssm-delivery/bootstrap-host.sh").stat().st_mode & 0o111
assert not (BASE.parent.parent / "scripts/ssm-delivery/threadia-backend.service").stat().st_mode & 0o111

print("Restricted SSM delivery source preflight passed; no AWS or GitHub command was executed.")
