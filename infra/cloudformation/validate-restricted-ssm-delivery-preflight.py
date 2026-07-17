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
network_service_policy = json.loads(
    (BASE / "restricted-network-service-permissions-policy.json").read_text(encoding="utf-8")
)
expected_network_describe_actions = [
    "ec2:DescribeAvailabilityZones",
    "ec2:DescribeVpcs",
    "ec2:DescribeSubnets",
    "ec2:DescribeRouteTables",
    "ec2:DescribeSecurityGroups",
    "ec2:DescribeVpcEndpoints",
    "ec2:DescribeNetworkInterfaces",
]
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
release_helper = (BASE.parent.parent / "scripts/ssm-delivery/apply-release").read_text(encoding="utf-8")
service_unit = (BASE.parent.parent / "scripts/ssm-delivery/threadia-backend.service").read_text(encoding="utf-8")
bootstrap = (BASE.parent.parent / "scripts/ssm-delivery/bootstrap-host.sh").read_text(encoding="utf-8")
version_pattern = "^[A-Za-z0-9._+=/-]{1,1000}[A-Za-z0-9._+=/-]{0,24}$"
required_runtime_parameters = {
    "/threadia/restricted-nonprod/runtime/THREADIA_DB_URL",
    "/threadia/restricted-nonprod/runtime/THREADIA_DB_USERNAME",
    "/threadia/restricted-nonprod/runtime/THREADIA_DB_PASSWORD",
    "/threadia/restricted-nonprod/runtime/THREADIA_JWT_SECRET",
    "/threadia/restricted-nonprod/runtime/THREADIA_COOKIE_SECURE",
    "/threadia/restricted-nonprod/runtime/THREADIA_CORS_ALLOWED_ORIGIN",
}
optional_mail_runtime_parameters = {
    "/threadia/restricted-nonprod/runtime/MAIL_HOST",
    "/threadia/restricted-nonprod/runtime/MAIL_PORT",
    "/threadia/restricted-nonprod/runtime/MAIL_USERNAME",
    "/threadia/restricted-nonprod/runtime/MAIL_PASSWORD",
    "/threadia/restricted-nonprod/runtime/NOTIFICATION_FROM_EMAIL",
}


def optional_mail_group_action(returned: set[str], invalid: set[str]) -> str:
    if not returned and invalid == optional_mail_runtime_parameters:
        return "omit"
    if returned == optional_mail_runtime_parameters and not invalid:
        return "materialize"
    return "reject"

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
assert network_service_policy["Statement"][0]["Action"] == expected_network_describe_actions
ses_smtp_endpoint = network.split("SesSmtpEndpoint:", 1)[1].split("S3GatewayEndpoint:", 1)[0]
assert "PrivateDnsEnabled: true" in ses_smtp_endpoint
assert "ServiceName: !Sub com.amazonaws.${AWS::Region}.email-smtp" in ses_smtp_endpoint
assert "SubnetIds: [!Ref ComputeSubnet]" in ses_smtp_endpoint
assert "SecurityGroupIds: [!GetAtt SesSmtpEndpointSecurityGroup.GroupId]" in ses_smtp_endpoint
assert "VpcEndpointType: Interface" in ses_smtp_endpoint
ses_smtp_endpoint_group = network.split("SesSmtpEndpointSecurityGroup:", 1)[1].split("SesSmtpEndpointIngress:", 1)[0]
assert "CidrIp: 127.0.0.1/32" in ses_smtp_endpoint_group
assert "IpProtocol: '-1'" in ses_smtp_endpoint_group
ses_smtp_ingress = network.split("SesSmtpEndpointIngress:", 1)[1].split("Ec2SmtpToSesEndpoint:", 1)[0]
assert "GroupId: !GetAtt SesSmtpEndpointSecurityGroup.GroupId" in ses_smtp_ingress
assert "FromPort: 587" in ses_smtp_ingress and "ToPort: 587" in ses_smtp_ingress
assert "SourceSecurityGroupId: !GetAtt Ec2SecurityGroup.GroupId" in ses_smtp_ingress
ses_smtp_egress = network.split("Ec2SmtpToSesEndpoint:", 1)[1].split("Ec2HttpsToS3GatewayEndpoint:", 1)[0]
assert "GroupId: !GetAtt Ec2SecurityGroup.GroupId" in ses_smtp_egress
assert "FromPort: 587" in ses_smtp_egress and "ToPort: 587" in ses_smtp_egress
assert "DestinationSecurityGroupId: !GetAtt SesSmtpEndpointSecurityGroup.GroupId" in ses_smtp_egress
approved_endpoint_services = next(
    statement
    for statement in network_service_policy["Statement"]
    if statement.get("Sid") == "AllowCreateTaggedApprovedEndpoints"
)["Condition"]["StringEquals"]["ec2:VpceServiceName"]
assert approved_endpoint_services == [
    "com.amazonaws.ap-northeast-2.ssm",
    "com.amazonaws.ap-northeast-2.ssmmessages",
    "com.amazonaws.ap-northeast-2.ec2messages",
    "com.amazonaws.ap-northeast-2.s3",
    "com.amazonaws.ap-northeast-2.email-smtp",
]
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
assert "ssm:GetParametersByPath" not in template
assert "secretsmanager:" not in template.lower()
assert "ManageMasterUserPassword" not in template
runtime_parameter_arns = set(re.findall(r"parameter(/threadia/restricted-nonprod/runtime/[A-Z_]+)", template))
assert runtime_parameter_arns == required_runtime_parameters | optional_mail_runtime_parameters
assert "Resource: '*'" in template and "Action: ssm:GetCommandInvocation" in template
assert "AWS-RunShellScript" not in template
assert f"allowedPattern: '{version_pattern}'" in template
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
fixed_command_document = next(
    statement
    for statement in service_policy["Statement"]
    if statement["Sid"] == "AllowManageFixedCommandDocument"
)
assert fixed_command_document["Action"] == [
    "ssm:CreateDocument",
    "ssm:DescribeDocument",
    "ssm:GetDocument",
    "ssm:UpdateDocument",
    "ssm:DeleteDocument",
    "ssm:AddTagsToResource",
    "ssm:RemoveTagsFromResource",
]
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
assert isinstance(parameters, list) and len(parameters) == 4
assert all(not re.search(r"(?<![0-9])[0-9]{12}(?![0-9])", json.dumps(item)) for item in parameters)
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
assert "cp -a \"$JAVA_HOME\" \"$stage/runtime\"" in workflow
assert "tar -C \"$stage\" -czf \"$archive\" app.jar app.jar.sha256 manifest.json runtime" in workflow
release_job = workflow.split("  release:", 1)[1]
assert "    if: github.ref == 'refs/heads/main'" in release_job
assert "    concurrency:\n      group: restricted-ssm-release\n      cancel-in-progress: false" in release_job
assert "    timeout-minutes: 30" in release_job
assert 'aws s3api put-object --region "$AWS_REGION"' in release_job
assert 'aws ssm send-command --region "$AWS_REGION"' in release_job
assert 'aws ssm get-command-invocation --region "$AWS_REGION"' in release_job
assert "--cli-connect-timeout 5 --cli-read-timeout 10" in release_job
assert "java " not in release_helper.split("for command", 1)[1].split("umask", 1)[0]
assert "runtime/bin/java" in release_helper and "ExecStart=/opt/threadia/current/runtime/bin/java" in service_unit
assert "yum " not in bootstrap and "apt " not in bootstrap and "http" not in bootstrap
document_parameters = template.split("parameters:", 1)[1].split("mainSteps:", 1)[0]
assert all(name in document_parameters for name in ("ArtifactKey:", "ArtifactVersionId:", "ArtifactSha256:", "ReleaseId:"))
assert "Command:" not in document_parameters and "runCommand:" not in document_parameters
assert "AWS_REGION:" not in document_parameters and "AWS_DEFAULT_REGION:" not in document_parameters
assert "AWS_REGION='${AWS::Region}' AWS_DEFAULT_REGION='${AWS::Region}'\n                  /usr/local/lib/threadia-release/apply-release" in template
assert "timeoutSeconds: '540'" in template
assert '[[ -n "${AWS_REGION:-}" ]] || fail "AWS_REGION is required"' in release_helper
assert '[[ "$AWS_REGION" =~ ^[a-z]{2}(-[a-z0-9]+)+-[0-9]+$ ]] || fail "invalid AWS_REGION"' in release_helper
assert 'aws s3api get-object --region "$AWS_REGION"' in release_helper
assert 'aws ssm get-parameters --region "$AWS_REGION"' in release_helper
assert release_helper.count('aws ssm get-parameters --region "$AWS_REGION" --with-decryption --names') == 2
assert "ssm:GetParametersByPath" not in release_helper
assert all(parameter in release_helper for parameter in required_runtime_parameters | optional_mail_runtime_parameters)
required_parameter_fetch = release_helper.split('parameters_json="$stage/runtime-parameters.json"', 1)[1].split("python3 -", 1)[0]
mail_parameter_fetch = release_helper.split('mail_parameters_json="$stage/mail-runtime-parameters.json"', 1)[1].split("python3 -", 1)[0]
assert all(parameter in required_parameter_fetch for parameter in required_runtime_parameters)
assert not any(parameter in required_parameter_fetch for parameter in optional_mail_runtime_parameters)
assert all(parameter in mail_parameter_fetch for parameter in optional_mail_runtime_parameters)
assert not any(parameter in mail_parameter_fetch for parameter in required_runtime_parameters)
assert optional_mail_group_action(set(), optional_mail_runtime_parameters) == "omit"
assert optional_mail_group_action(optional_mail_runtime_parameters, set()) == "materialize"
assert optional_mail_group_action({next(iter(optional_mail_runtime_parameters))}, set()) == "reject"
assert optional_mail_group_action(set(), {next(iter(optional_mail_runtime_parameters))}) == "reject"
assert 'if not returned and invalid == set(optional):' in release_helper
assert 'if returned != set(optional) or invalid:' in release_helper
assert 'affected = sorted(invalid | (set(optional) - returned))' in release_helper
assert 'with open(sys.argv[2], "a", encoding="utf-8") as target:' in release_helper
assert 'raise SystemExit("optional runtime parameter group is incomplete or invalid: " + ", ".join(affected))' in release_helper
assert 'if value == "":' in release_helper
assert 'raise SystemExit("optional runtime parameter is blank: " + parameter_name)' in release_helper
assert 'escaped_value = value.replace(chr(92), chr(92) * 2).replace(chr(34), chr(92) + chr(34))' in release_helper
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
assert "readonly RELEASE_LOCK_FILE=/run/lock/threadia-release.lock" in release_helper
assert 'exec 9>"$RELEASE_LOCK_FILE"' in release_helper
assert 'flock -n 9 || fail "another release is already active"' in release_helper
assert "flock" in bootstrap
assert "Environment=MANAGEMENT_HEALTH_MAIL_ENABLED=false" in service_unit
assert (BASE.parent.parent / "scripts/ssm-delivery/apply-release").stat().st_mode & 0o111
assert (BASE.parent.parent / "scripts/ssm-delivery/bootstrap-host.sh").stat().st_mode & 0o111
assert not (BASE.parent.parent / "scripts/ssm-delivery/threadia-backend.service").stat().st_mode & 0o111

print("Restricted SSM delivery source preflight passed; no AWS or GitHub command was executed.")
