#!/usr/bin/env python3
"""Fail-closed source preflight and non-executing Compute authorization plan generator."""

import json
import re
from pathlib import Path

BASE = Path(__file__).parent
TEMPLATES = ["restricted-network.yaml", "restricted-database.yaml", "restricted-compute.yaml"]
PARAMETERS = [
    "restricted-network-parameters.example.json",
    "restricted-database-parameters.example.json",
    "restricted-compute-parameters.example.json",
]

sources = {name: (BASE / name).read_text(encoding="utf-8") for name in TEMPLATES}
for filename, text in sources.items():
    assert "UserData:" not in text, f"{filename}: UserData is prohibited"
    assert "KeyName:" not in text, f"{filename}: KeyName is prohibited"
    assert "AWS::EC2::NatGateway" not in text, f"{filename}: NAT is prohibited"
    assert "0.0.0.0/0" not in text, f"{filename}: internet route/egress is prohibited"
    assert "PubliclyAccessible: true" not in text, f"{filename}: public RDS is prohibited"

network = sources["restricted-network.yaml"]
database = sources["restricted-database.yaml"]
compute = sources["restricted-compute.yaml"]
assert network.count("VpcEndpointType: Interface") == 3
assert "&Tags" not in network and "*Tags" not in network, "YAML aliases are prohibited"
for endpoint in ("SsmEndpoint", "SsmMessagesEndpoint", "Ec2MessagesEndpoint"):
    match = re.search(rf"^  {endpoint}:\n(?P<body>.*?)(?=^  [A-Za-z][A-Za-z0-9]*:|\Z)", network, re.MULTILINE | re.DOTALL)
    assert match, f"{endpoint}: interface endpoint resource is missing"
    endpoint_block = match.group("body")
    assert "Type: AWS::EC2::VPCEndpoint" in endpoint_block, f"{endpoint}: incorrect resource type"
    for tag_line in (
        "Tags:\n        - Key: Project\n          Value: Threadia",
        "- Key: Environment\n          Value: restricted-nonprod",
        "- Key: Owner\n          Value: !Ref Owner",
        "- Key: ManagedBy\n          Value: CloudFormation",
    ):
        assert tag_line in endpoint_block, f"{endpoint}: required inline tag is missing"
assert "AWS::SSM::Parameter::Value" not in compute
assert "AssociatePublicIpAddress: false" in compute
assert "HttpTokens: required" in compute
assert "DeviceName: /dev/xvda" in compute
assert "Encrypted: true" in compute and "VolumeType: gp3" in compute
assert "VolumeSize: 20" in compute and "DeliveryHost" in compute
assert "Outputs:" in compute and "Ec2InstanceId:" in compute and "Ec2RoleName:" in compute
assert "ManageMasterUserPassword: true" in database
assert "DeletionPolicy: Snapshot" in database and "UpdateReplacePolicy: Snapshot" in database
for forbidden_property in (
    "DBSecurityGroups:", "DBParameterGroupName:", "OptionGroupName:", "KmsKeyId:",
    "MonitoringRoleArn:", "DomainIAMRoleName:",
):
    assert forbidden_property not in database, f"restricted-database.yaml: {forbidden_property} is prohibited"
assert "&Tags" not in database and "*Tags" not in database, "YAML aliases are prohibited"
for resource in ("RdsSecurityGroup", "DbSubnetGroup", "Database"):
    match = re.search(rf"^  {resource}:\n(?P<body>.*?)(?=^  [A-Za-z][A-Za-z0-9]*:|\Z)", database, re.MULTILINE | re.DOTALL)
    assert match, f"{resource}: required Database resource is missing"
    resource_block = match.group("body")
    for tag_line in (
        "Tags:\n        - Key: Project\n          Value: Threadia",
        "- Key: Environment\n          Value: restricted-nonprod",
        "- Key: Owner\n          Value: !Ref Owner",
        "- Key: ManagedBy\n          Value: CloudFormation",
    ):
        assert tag_line in resource_block, f"{resource}: required inline tag is missing"
for filename in PARAMETERS:
    entries = json.loads((BASE / filename).read_text(encoding="utf-8"))
    assert isinstance(entries, list) and entries, filename
    assert not re.search(r"(?<![0-9])[0-9]{12}(?![0-9])", json.dumps(entries)), filename

TAG_CONTEXT = (
    "ContextKeyName=aws:RequestTag/Project,ContextKeyValues=Threadia,ContextKeyType=string "
    "ContextKeyName=aws:RequestTag/Environment,ContextKeyValues=restricted-nonprod,ContextKeyType=string "
    "ContextKeyName=aws:RequestTag/ManagedBy,ContextKeyValues=CloudFormation,ContextKeyType=string "
    "ContextKeyName=aws:RequestTag/Owner,ContextKeyValues=$APPROVED_OWNER,ContextKeyType=string"
)
REQUESTED_REGION_CONTEXT = "ContextKeyName=aws:RequestedRegion,ContextKeyValues=ap-northeast-2,ContextKeyType=string"


def with_requested_region(context):
    return " ".join(part for part in (REQUESTED_REGION_CONTEXT, context) if part)


assert REQUESTED_REGION_CONTEXT == "ContextKeyName=aws:RequestedRegion,ContextKeyValues=ap-northeast-2,ContextKeyType=string"
RESOURCE_TAG_CONTEXT = (
    "ContextKeyName=ec2:ResourceTag/Project,ContextKeyValues=Threadia,ContextKeyType=string "
    "ContextKeyName=ec2:ResourceTag/Environment,ContextKeyValues=restricted-nonprod,ContextKeyType=string "
    "ContextKeyName=ec2:ResourceTag/ManagedBy,ContextKeyValues=CloudFormation,ContextKeyType=string "
    "ContextKeyName=ec2:ResourceTag/Owner,ContextKeyValues=$APPROVED_OWNER,ContextKeyType=string"
)
INSTANCE_POSITIVE_CONTEXT = (
    f"{TAG_CONTEXT} ContextKeyName=ec2:InstanceType,ContextKeyValues=t3.micro,ContextKeyType=string "
    "ContextKeyName=ec2:MetadataHttpTokens,ContextKeyValues=required,ContextKeyType=string"
)
INSTANCE_NEGATIVE_CONTEXT = INSTANCE_POSITIVE_CONTEXT.replace("required", "optional")
assert "ec2:AssociatePublicIpAddress" not in INSTANCE_POSITIVE_CONTEXT
assert "ec2:KeyPairName" not in INSTANCE_POSITIVE_CONTEXT
VOLUME_POSITIVE_CONTEXT = "ContextKeyName=ec2:Encrypted,ContextKeyValues=true,ContextKeyType=boolean ContextKeyName=ec2:VolumeType,ContextKeyValues=gp3,ContextKeyType=string"
VOLUME_NEGATIVE_CONTEXT = VOLUME_POSITIVE_CONTEXT.replace("true", "false").replace("gp3", "gp2")
ENI_POSITIVE_CONTEXT = "ContextKeyName=ec2:AssociatePublicIpAddress,ContextKeyValues=false,ContextKeyType=boolean"
ENI_NEGATIVE_CONTEXT = ENI_POSITIVE_CONTEXT.replace("false", "true")
MULTI_RESOURCE_POSITIVE_CONTEXT = with_requested_region(f"{RESOURCE_TAG_CONTEXT} {INSTANCE_POSITIVE_CONTEXT} {VOLUME_POSITIVE_CONTEXT} {ENI_POSITIVE_CONTEXT}")
MULTI_RESOURCE_NEGATIVE_CONTEXT = MULTI_RESOURCE_POSITIVE_CONTEXT.replace("required", "optional").replace("false", "true").replace("gp3", "gp2")
assert MULTI_RESOURCE_POSITIVE_CONTEXT.startswith(REQUESTED_REGION_CONTEXT)
assert MULTI_RESOURCE_NEGATIVE_CONTEXT.startswith(REQUESTED_REGION_CONTEXT)
MULTI_RESOURCE_ARNS = "\"$APPROVED_AMI_ARN\" \"$COMPUTE_SUBNET_ARN\" \"$EC2_SECURITY_GROUP_ARN\" \"$COMPUTE_INSTANCE_ARN\" \"$COMPUTE_VOLUME_ARN\" \"$COMPUTE_ENI_ARN\""
KEY_PAIR_NEGATIVE_ARN = "arn:aws:ec2:ap-northeast-2:<AWS_ACCOUNT_ID>:key-pair/$KEY_PAIR_ID"
RESOURCE_CASES = [
    ("image", "$APPROVED_AMI_ARN", "$UNAPPROVED_AMI_ARN", "approved AMI", "unapproved AMI", "", ""),
    ("subnet", "$COMPUTE_SUBNET_ARN", "$UNTAGGED_SUBNET_ARN", "required network tags", "missing network tags", RESOURCE_TAG_CONTEXT, ""),
    ("security-group", "$EC2_SECURITY_GROUP_ARN", "$UNTAGGED_SECURITY_GROUP_ARN", "required network tags", "missing network tags", RESOURCE_TAG_CONTEXT, ""),
    ("instance", "$COMPUTE_INSTANCE_ARN", "$COMPUTE_INSTANCE_ARN", "required tags and IMDSv2", "IMDSv1", INSTANCE_POSITIVE_CONTEXT, INSTANCE_NEGATIVE_CONTEXT),
    ("volume", "$COMPUTE_VOLUME_ARN", "$COMPUTE_VOLUME_ARN", "encrypted gp3 root volume", "unencrypted or non-gp3 root volume", VOLUME_POSITIVE_CONTEXT, VOLUME_NEGATIVE_CONTEXT),
    ("network-interface", "$COMPUTE_ENI_ARN", "$COMPUTE_ENI_ARN", "no public IP", "public IP", ENI_POSITIVE_CONTEXT, ENI_NEGATIVE_CONTEXT),
]
assert all(with_requested_region(context).startswith(REQUESTED_REGION_CONTEXT) for case in RESOURCE_CASES for context in case[5:])
assert with_requested_region("ContextKeyName=iam:PassedToService,ContextKeyValues=ec2.amazonaws.com,ContextKeyType=string").startswith(REQUESTED_REGION_CONTEXT)

print("Source preflight passed; no AWS command was executed.")
print("\nDryRun principal model:")
print("IAM Simulator uses the actual Compute CloudFormation service role for every RunInstances resource type.")
print("Attach restricted-compute-dry-run-policy.json only to a separate temporary preflight identity.")
print("That mutation-capable identity has exact approved AMI/subnet/security-group resource scope, no instance profile, and no iam:PassRole.")
print("The temporary identity is used only for EC2 DryRun and is revoked after the approved window.")
print("\nRunInstances IAM Simulator matrix (run only in an approved AWS window):")
print("aggregate | positive | approved AMI, tagged subnet/SG, private IMDSv2 instance, encrypted gp3 volume, private ENI | allowed")
print("aggregate | negative | IMDS/public-IP/volume contract violation across all resource types | implicitDeny or explicitDeny")
for resource_type, positive_arn, negative_arn, positive, negative, _, _ in RESOURCE_CASES:
    print(f"{resource_type} | positive | {positive} | allowed")
    print(f"{resource_type} | negative | {negative} | implicitDeny or explicitDeny")
print("PassRole | positive | generated Compute EC2 role to ec2.amazonaws.com | allowed")
print("PassRole | negative | any non-Compute role or non-EC2 service | explicitDeny or implicitDeny")
print("key-pair | negative | key-pair/$KEY_PAIR_ID has no RunInstances Allow | implicitDeny or explicitDeny")
print("\nGenerated commands are instructions only; this program does not execute them:")
print(
    "aws iam simulate-principal-policy --policy-source-arn \"$COMPUTE_SERVICE_ROLE_ARN\" "
    f"--action-names ec2:RunInstances --resource-arns {MULTI_RESOURCE_ARNS} --context-entries {MULTI_RESOURCE_POSITIVE_CONTEXT} "
    "# aggregate RunInstances positive: all six resource types"
)
print(
    "aws iam simulate-principal-policy --policy-source-arn \"$COMPUTE_SERVICE_ROLE_ARN\" "
    f"--action-names ec2:RunInstances --resource-arns {MULTI_RESOURCE_ARNS} --context-entries {MULTI_RESOURCE_NEGATIVE_CONTEXT} "
    "# aggregate RunInstances negative: IMDS/public-IP/volume contract violation"
)
for resource_type, positive_arn, negative_arn, _, _, positive_context, negative_context in RESOURCE_CASES:
    prefix = "aws iam simulate-principal-policy --policy-source-arn \"$COMPUTE_SERVICE_ROLE_ARN\" --action-names ec2:RunInstances"
    positive_suffix = f" --context-entries {with_requested_region(positive_context)}"
    negative_suffix = f" --context-entries {with_requested_region(negative_context)}"
    print(f"{prefix} --resource-arns \"{positive_arn}\"{positive_suffix} # {resource_type} positive")
    print(f"{prefix} --resource-arns \"{negative_arn}\"{negative_suffix} # {resource_type} negative")
print(
    "aws iam simulate-principal-policy --policy-source-arn \"$COMPUTE_SERVICE_ROLE_ARN\" "
    f"--action-names ec2:RunInstances --resource-arns \"{KEY_PAIR_NEGATIVE_ARN}\" "
    f"--context-entries {with_requested_region(INSTANCE_POSITIVE_CONTEXT)} # key-pair negative without an Allow resource"
)
print(
    "aws iam simulate-principal-policy --policy-source-arn \"$COMPUTE_SERVICE_ROLE_ARN\" "
    "--action-names iam:PassRole --resource-arns \"$COMPUTE_ROLE_ARN\" "
    f"--context-entries {with_requested_region('ContextKeyName=iam:PassedToService,ContextKeyValues=ec2.amazonaws.com,ContextKeyType=string')} # positive"
)
print(
    "aws iam simulate-principal-policy --policy-source-arn \"$COMPUTE_SERVICE_ROLE_ARN\" "
    "--action-names iam:PassRole --resource-arns \"$NON_COMPUTE_ROLE_ARN\" "
    f"--context-entries {with_requested_region('ContextKeyName=iam:PassedToService,ContextKeyValues=lambda.amazonaws.com,ContextKeyType=string')} # negative"
)
ROOT_MAPPING = "[{\"DeviceName\":\"/dev/xvda\",\"Ebs\":{\"DeleteOnTermination\":true,\"Encrypted\":true,\"VolumeSize\":20,\"VolumeType\":\"gp3\"}}]"
print(
    "aws ec2 run-instances --region ap-northeast-2 --dry-run --image-id \"$APPROVED_AMI_ID\" "
    "--instance-type t3.micro --metadata-options HttpTokens=required,HttpEndpoint=enabled,HttpPutResponseHopLimit=1 "
    "--network-interfaces DeviceIndex=0,SubnetId=\"$COMPUTE_SUBNET_ID\",Groups=\"$EC2_SECURITY_GROUP_ID\",AssociatePublicIpAddress=false "
    f"--block-device-mappings '{ROOT_MAPPING}' # positive; intentionally no --iam-instance-profile"
)
print(
    "aws ec2 run-instances --region ap-northeast-2 --dry-run --image-id \"$APPROVED_AMI_ID\" "
    "--instance-type t3.micro --metadata-options HttpTokens=required,HttpEndpoint=enabled "
    "--network-interfaces DeviceIndex=0,SubnetId=\"$COMPUTE_SUBNET_ID\",Groups=\"$EC2_SECURITY_GROUP_ID\",AssociatePublicIpAddress=true "
    f"--block-device-mappings '{ROOT_MAPPING}' # negative public-IP case; intentionally no --iam-instance-profile"
)
