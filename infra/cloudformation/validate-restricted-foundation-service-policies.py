#!/usr/bin/env python3
"""Fail closed validation for additive restricted foundation policy sources.

This program parses local files only. It never invokes AWS, subprocesses, or a network.
"""

import json
from pathlib import Path

BASE = Path(__file__).parent
QUOTA = 6144
POLICIES = {
    "guardrails": "restricted-service-guardrails-policy.json",
    "network": "restricted-network-service-permissions-policy.json",
    "database": "restricted-database-service-permissions-policy.json",
    "compute": "restricted-compute-service-permissions-policy.json",
    "dry-run": "restricted-compute-dry-run-policy.json",
}
STAGES = {
    "network": "Network",
    "database": "Database",
    "compute": "Compute",
}


def compact(document):
    return json.dumps(document, separators=(",", ":"), sort_keys=True)


def statements(document):
    value = document.get("Statement")
    assert isinstance(value, list) and value, "Statement must be a non-empty list"
    return value


def action_set(statement):
    action = statement["Action"]
    return {action} if isinstance(action, str) else set(action)


documents = {
    name: json.loads((BASE / filename).read_text(encoding="utf-8"))
    for name, filename in POLICIES.items()
}
operator_documents = {
    stage: json.loads((BASE / f"restricted-{stage}-operator-policy.json").read_text(encoding="utf-8"))
    for stage in STAGES
}
operator_trust_documents = {
    stage: json.loads((BASE / f"restricted-{stage}-operator-role-trust-policy.json").read_text(encoding="utf-8"))
    for stage in STAGES
}
service_trust_documents = {
    stage: json.loads((BASE / f"restricted-{stage}-service-role-trust-policy.json").read_text(encoding="utf-8"))
    for stage in STAGES
}
bootstrap_assume_documents = {
    stage: json.loads((BASE / f"restricted-{stage}-bootstrap-assume-policy.json").read_text(encoding="utf-8"))
    for stage in STAGES
}
dry_run_role_trust = json.loads((BASE / "restricted-compute-dry-run-role-trust-policy.json").read_text(encoding="utf-8"))
dry_run_bootstrap_assume = json.loads((BASE / "restricted-compute-dry-run-bootstrap-assume-policy.json").read_text(encoding="utf-8"))
for name, document in documents.items():
    assert document.get("Version") == "2012-10-17", name
    assert len(compact(document)) <= QUOTA, (name, len(compact(document)))
for family in (operator_documents, operator_trust_documents, service_trust_documents, bootstrap_assume_documents):
    for stage, document in family.items():
        assert document.get("Version") == "2012-10-17", stage
        assert len(compact(document)) <= QUOTA, (stage, len(compact(document)))
for document in (dry_run_role_trust, dry_run_bootstrap_assume):
    assert document.get("Version") == "2012-10-17"
    assert len(compact(document)) <= QUOTA

assert all(item["Effect"] == "Deny" for item in statements(documents["guardrails"]))
for name in ("network", "database", "compute", "dry-run"):
    assert all(item["Effect"] == "Allow" for item in statements(documents[name])), name

for name in ("network", "database", "compute", "dry-run"):
    for item in statements(documents[name]):
        if any(action.startswith(("ec2:", "iam:")) and action not in {"ec2:DescribeAvailabilityZones", "ec2:DescribeImages", "ec2:DescribeInstances", "ec2:DescribeManagedPrefixLists", "ec2:DescribeNetworkInterfaces", "ec2:DescribeRouteTables", "ec2:DescribeSecurityGroups", "ec2:DescribeSubnets", "ec2:DescribeVolumes", "ec2:DescribeVpcs", "ec2:DescribeVpcEndpoints", "iam:GetRole", "iam:GetInstanceProfile", "iam:ListAttachedRolePolicies", "iam:ListInstanceProfilesForRole"} for action in action_set(item)):
            assert item.get("Resource") != "*", (name, item.get("Sid"))
        assert all(not action.startswith("cloudformation:") for item in statements(documents[name]) for action in action_set(item)), name

for stage, title in STAGES.items():
    stage_name = f"threadia-restricted-{stage}"
    change_set_prefix = f"restricted-{stage}-*"
    service_role = f"ThreadiaRestricted{title}CloudFormationServiceRole"
    operator_role = f"ThreadiaRestricted{title}OperatorRole"
    operator = operator_documents[stage]
    assert all(item["Effect"] == "Allow" for item in statements(operator)), stage
    assert all(
        action in {
            "cloudformation:CreateChangeSet", "cloudformation:DescribeChangeSet", "cloudformation:ExecuteChangeSet",
            "cloudformation:DeleteChangeSet", "cloudformation:DeleteStack", "cloudformation:DescribeStacks", "cloudformation:DescribeStackEvents",
            "cloudformation:DescribeStackResources", "cloudformation:GetTemplate", "iam:PassRole",
        }
        for item in statements(operator) for action in action_set(item)
    ), stage
    operator_text = compact(operator)
    assert stage_name in operator_text and change_set_prefix in operator_text, stage
    assert f"role/{service_role}" in operator_text, stage
    for other_stage in STAGES:
        if other_stage != stage:
            assert f"threadia-restricted-{other_stage}" not in operator_text, stage
    create = next(item for item in statements(operator) if item["Action"] == "cloudformation:CreateChangeSet")
    assert create["Condition"]["StringEquals"]["cloudformation:RoleARN"].endswith(f"role/{service_role}"), stage
    pass_role = next(item for item in statements(operator) if item["Action"] == "iam:PassRole")
    assert pass_role["Resource"].endswith(f"role/{service_role}"), stage
    delete_stack = [item for item in statements(operator) if item["Action"] == "cloudformation:DeleteStack"]
    if stage == "database":
        assert len(delete_stack) == 1
        assert delete_stack[0]["Resource"] == "arn:aws:cloudformation:ap-northeast-2:<AWS_ACCOUNT_ID>:stack/threadia-restricted-database/*"
    else:
        assert not delete_stack, stage
    operator_trust = statements(operator_trust_documents[stage])
    assert len(operator_trust) == 1 and operator_trust[0]["Principal"] == {"AWS": "arn:aws:iam::<AWS_ACCOUNT_ID>:user/ThreadiaBootstrapOperator"}, stage
    service_trust = statements(service_trust_documents[stage])
    assert len(service_trust) == 1 and service_trust[0]["Principal"] == {"Service": "cloudformation.amazonaws.com"}, stage
    bootstrap_assume = statements(bootstrap_assume_documents[stage])
    assert len(bootstrap_assume) == 1 and bootstrap_assume[0]["Resource"].endswith(f"role/{operator_role}"), stage

dry_run_trust_statements = statements(dry_run_role_trust)
assert len(dry_run_trust_statements) == 1
assert dry_run_trust_statements[0]["Principal"] == {"AWS": "arn:aws:iam::<AWS_ACCOUNT_ID>:user/ThreadiaBootstrapOperator"}
assert dry_run_trust_statements[0]["Action"] == "sts:AssumeRole"
dry_run_assume_statements = statements(dry_run_bootstrap_assume)
assert len(dry_run_assume_statements) == 1
assert dry_run_assume_statements[0]["Action"] == "sts:AssumeRole"
assert dry_run_assume_statements[0]["Resource"] == "arn:aws:iam::<AWS_ACCOUNT_ID>:role/ThreadiaRestrictedComputeDryRunRole"

network = statements(documents["network"])
assert any(item["Action"] == "ec2:ModifyVpcAttribute" for item in network)
network_by_sid = {item["Sid"]: item for item in network if "Sid" in item}
assert "AllowTaggedNetworkCreation" not in network_by_sid
request_tag_keys = {"aws:RequestTag/Project", "aws:RequestTag/Environment", "aws:RequestTag/ManagedBy"}
resource_tag_keys = {"ec2:ResourceTag/Project", "ec2:ResourceTag/Environment", "ec2:ResourceTag/ManagedBy"}
read_only = [item for item in network if item.get("Action") == ["ec2:DescribeAvailabilityZones", "ec2:DescribeVpcs", "ec2:DescribeSubnets", "ec2:DescribeSecurityGroups", "ec2:DescribeVpcEndpoints", "ec2:DescribeNetworkInterfaces"] and item.get("Resource") == "*"]
assert len(read_only) == 1
assert read_only[0]["Action"] == ["ec2:DescribeAvailabilityZones", "ec2:DescribeVpcs", "ec2:DescribeSubnets", "ec2:DescribeSecurityGroups", "ec2:DescribeVpcEndpoints", "ec2:DescribeNetworkInterfaces"]
assert read_only[0]["Resource"] == "*"
for action, reference_sid, output_sid, reference_resource, output_resource in (
    ("ec2:CreateSubnet", "AllowCreateSubnetInTaggedVpc", "AllowCreateTaggedSubnet", "arn:aws:ec2:ap-northeast-2:<AWS_ACCOUNT_ID>:vpc/*", "arn:aws:ec2:ap-northeast-2:<AWS_ACCOUNT_ID>:subnet/*"),
    ("ec2:CreateSecurityGroup", "AllowCreateSecurityGroupInTaggedVpc", "AllowCreateTaggedSecurityGroup", "arn:aws:ec2:ap-northeast-2:<AWS_ACCOUNT_ID>:vpc/*", "arn:aws:ec2:ap-northeast-2:<AWS_ACCOUNT_ID>:security-group/*"),
):
    reference = network_by_sid[reference_sid]
    output = network_by_sid[output_sid]
    assert reference["Action"] == action and reference["Resource"] == reference_resource
    assert output["Action"] == action and output["Resource"] == output_resource
    reference_keys = set(reference["Condition"]["StringEquals"])
    output_keys = set(output["Condition"]["StringEquals"])
    assert resource_tag_keys <= reference_keys and not request_tag_keys & reference_keys, reference_sid
    assert request_tag_keys <= output_keys and not resource_tag_keys & output_keys, output_sid
vpc_create = network_by_sid["AllowCreateVpcWithRequiredTags"]
assert vpc_create["Action"] == "ec2:CreateVpc" and vpc_create["Resource"] == "arn:aws:ec2:ap-northeast-2:<AWS_ACCOUNT_ID>:vpc/*"
assert request_tag_keys <= set(vpc_create["Condition"]["StringEquals"])
endpoint_references = network_by_sid["AllowCreateSsmEndpointWithTaggedReferences"]
assert endpoint_references["Action"] == "ec2:CreateVpcEndpoint"
assert set(endpoint_references["Resource"]) == {
    "arn:aws:ec2:ap-northeast-2:<AWS_ACCOUNT_ID>:vpc/*",
    "arn:aws:ec2:ap-northeast-2:<AWS_ACCOUNT_ID>:subnet/*",
    "arn:aws:ec2:ap-northeast-2:<AWS_ACCOUNT_ID>:security-group/*",
    "arn:aws:ec2:ap-northeast-2:<AWS_ACCOUNT_ID>:route-table/*",
}
endpoint_reference_keys = set(endpoint_references["Condition"]["StringEquals"])
assert resource_tag_keys <= endpoint_reference_keys and not request_tag_keys & endpoint_reference_keys
endpoint_output = network_by_sid["AllowCreateTaggedApprovedEndpoints"]
assert endpoint_output["Action"] == "ec2:CreateVpcEndpoint"
assert endpoint_output["Resource"] == "arn:aws:ec2:ap-northeast-2:<AWS_ACCOUNT_ID>:vpc-endpoint/*"
endpoint_output_values = endpoint_output["Condition"]["StringEquals"]
assert request_tag_keys <= set(endpoint_output_values) and not resource_tag_keys & set(endpoint_output_values)
assert endpoint_output_values["ec2:VpceServiceOwner"] == "amazon"
assert set(endpoint_output_values["ec2:VpceServiceName"]) == {
        "com.amazonaws.ap-northeast-2.ssm",
        "com.amazonaws.ap-northeast-2.ssmmessages",
        "com.amazonaws.ap-northeast-2.ec2messages",
        "com.amazonaws.ap-northeast-2.s3",
    }
create_route_tables = [item for item in network if item["Action"] == "ec2:CreateRouteTable"]
assert len(create_route_tables) == 2
route_table_in_vpc = [item for item in create_route_tables if item["Resource"] == "arn:aws:ec2:ap-northeast-2:<AWS_ACCOUNT_ID>:vpc/*"]
assert len(route_table_in_vpc) == 1
route_table_in_vpc = route_table_in_vpc[0]
assert route_table_in_vpc["Action"] == "ec2:CreateRouteTable"
route_table_in_vpc_keys = set(route_table_in_vpc["Condition"]["StringEquals"])
assert resource_tag_keys <= route_table_in_vpc_keys and not request_tag_keys & route_table_in_vpc_keys
route_table = [item for item in create_route_tables if item["Resource"] == "arn:aws:ec2:ap-northeast-2:<AWS_ACCOUNT_ID>:route-table/*"]
assert len(route_table) == 1
route_table = route_table[0]
assert route_table["Action"] == "ec2:CreateRouteTable"
route_table_keys = set(route_table["Condition"]["StringEquals"])
assert request_tag_keys <= route_table_keys and not resource_tag_keys & route_table_keys
assert network_by_sid["AllowTaggedRouteTableAssociation"]["Action"] == ["ec2:AssociateRouteTable", "ec2:DisassociateRouteTable"]
database = statements(documents["database"])
assert any(item["Sid"] == "AllowTagRdsSecurityGroupOnCreate" for item in database)
assert any(item["Action"] == "rds:CreateDBSubnetGroup" for item in database)
assert any(item["Action"] == "rds:CreateDBInstance" for item in database)
database_by_sid = {item["Sid"]: item for item in database}
database_arn = "arn:aws:rds:ap-northeast-2:<AWS_ACCOUNT_ID>:db:threadia-restricted-database-*"
subnet_group_arn = "arn:aws:rds:ap-northeast-2:<AWS_ACCOUNT_ID>:subgrp:threadia-restricted-database-*"
default_group_arns = {
    "arn:aws:rds:ap-northeast-2:<AWS_ACCOUNT_ID>:pg:default.mysql*",
    "arn:aws:rds:ap-northeast-2:<AWS_ACCOUNT_ID>:og:default:mysql-*",
}
create_db_instance_resources = {
    resource
    for item in database
    if item["Action"] == "rds:CreateDBInstance"
    for resource in (item["Resource"] if isinstance(item["Resource"], list) else [item["Resource"]])
}
assert create_db_instance_resources == {database_arn, subnet_group_arn}.union(default_group_arns)
subnet_group_reference = database_by_sid["AllowCreateManagedMysqlWithTaggedSubnetGroup"]
assert subnet_group_reference["Action"] == "rds:CreateDBInstance" and subnet_group_reference["Resource"] == subnet_group_arn
assert "Condition" not in subnet_group_reference
subnet_group_create = database_by_sid["AllowCreateTaggedDbSubnetGroup"]
assert "aws:RequestTag" in json.dumps(subnet_group_create["Condition"])
subnet_group_lifecycle = database_by_sid["AllowTaggedDbSubnetGroupLifecycle"]
assert action_set(subnet_group_lifecycle) == {"rds:ModifyDBSubnetGroup", "rds:DeleteDBSubnetGroup"}
assert subnet_group_lifecycle["Resource"] == subnet_group_arn and "Condition" not in subnet_group_lifecycle
default_groups = database_by_sid["AllowCreateManagedMysqlWithDefaultGroups"]
assert default_groups["Action"] == "rds:CreateDBInstance" and set(default_groups["Resource"]) == default_group_arns
assert "Condition" not in default_groups
database_output = database_by_sid["AllowCreateTaggedManagedMysql"]
assert database_output["Action"] == "rds:CreateDBInstance" and database_output["Resource"] == database_arn
database_output_condition = json.dumps(database_output["Condition"])
assert "aws:RequestTag" in database_output_condition and "rds:DatabaseEngine" in database_output_condition
snapshot_db_arn = "arn:aws:rds:ap-northeast-2:<AWS_ACCOUNT_ID>:db:threadia-restricted-database-*"
snapshot_arn = "arn:aws:rds:ap-northeast-2:<AWS_ACCOUNT_ID>:snapshot:threadia-restricted-database-*"
snapshot_create = next(item for item in database if item["Action"] == "rds:CreateDBSnapshot")
assert set(snapshot_create["Resource"]) == {snapshot_db_arn, snapshot_arn}
snapshot_tag = next(item for item in database if item["Sid"] == "AllowTagGeneratedSnapshotsOnly")
assert snapshot_tag["Action"] == "rds:AddTagsToResource" and snapshot_tag["Resource"] == snapshot_arn
for item in database:
    actions = action_set(item)
    if actions & {"rds:ModifyDBSubnetGroup", "rds:DeleteDBSubnetGroup", "rds:ModifyDBInstance", "rds:DeleteDBInstance"}:
        condition = json.dumps(item.get("Condition", {}))
        assert "aws:RequestTag" not in condition and "rds:DatabaseEngine" not in condition, item["Sid"]

compute = statements(documents["compute"])
run = [item for item in compute if item["Action"] == "ec2:RunInstances"]
resources = {item["Resource"] for item in run}
expected = {
    "arn:aws:ec2:ap-northeast-2::image/<APPROVED_AMI_ID>",
    "arn:aws:ec2:ap-northeast-2:<AWS_ACCOUNT_ID>:subnet/*",
    "arn:aws:ec2:ap-northeast-2:<AWS_ACCOUNT_ID>:security-group/*",
    "arn:aws:ec2:ap-northeast-2:<AWS_ACCOUNT_ID>:instance/*",
    "arn:aws:ec2:ap-northeast-2:<AWS_ACCOUNT_ID>:volume/*",
    "arn:aws:ec2:ap-northeast-2:<AWS_ACCOUNT_ID>:network-interface/*",
}
assert resources == expected, resources
for item in run:
    resource = item["Resource"]
    condition = item.get("Condition", {})
    has_public_ip_condition = "ec2:AssociatePublicIpAddress" in json.dumps(condition)
    if resource == "arn:aws:ec2:ap-northeast-2:<AWS_ACCOUNT_ID>:network-interface/*":
        assert has_public_ip_condition and '"false"' in json.dumps(condition), item["Sid"]
    else:
        assert not has_public_ip_condition, item["Sid"]
    if resource == "arn:aws:ec2:ap-northeast-2:<AWS_ACCOUNT_ID>:volume/*":
        assert '"ec2:Encrypted"' in json.dumps(condition) and '"gp3"' in json.dumps(condition), item["Sid"]
    if resource == "arn:aws:ec2:ap-northeast-2:<AWS_ACCOUNT_ID>:instance/*":
        instance_condition = json.dumps(condition)
        assert "ec2:InstanceType" in instance_condition and "ec2:MetadataHttpTokens" in instance_condition, item["Sid"]
        assert all(key in instance_condition for key in ("aws:RequestTag/Project", "aws:RequestTag/Environment", "aws:RequestTag/ManagedBy", "aws:RequestTag/Owner")), item["Sid"]
        assert "ec2:KeyPairName" not in instance_condition and "ec2:AssociatePublicIpAddress" not in instance_condition, item["Sid"]
assert not any(":key-pair/" in resource for resource in resources)

dry_run_resources = {item["Resource"] for item in statements(documents["dry-run"]) if item["Action"] == "ec2:RunInstances"}
assert "arn:aws:ec2:ap-northeast-2:<AWS_ACCOUNT_ID>:subnet/<APPROVED_SUBNET_ID>" in dry_run_resources
assert "arn:aws:ec2:ap-northeast-2:<AWS_ACCOUNT_ID>:security-group/<APPROVED_SECURITY_GROUP_ID>" in dry_run_resources
assert not any(resource.endswith(":subnet/*") or resource.endswith(":security-group/*") for resource in dry_run_resources)
dry_run_by_resource = {item["Resource"]: item for item in statements(documents["dry-run"]) if item["Action"] == "ec2:RunInstances"}
dry_instance_condition = json.dumps(dry_run_by_resource["arn:aws:ec2:ap-northeast-2:<AWS_ACCOUNT_ID>:instance/*"]["Condition"])
assert "ec2:InstanceType" in dry_instance_condition and "ec2:MetadataHttpTokens" in dry_instance_condition
assert "ec2:KeyPairName" not in dry_instance_condition and "ec2:AssociatePublicIpAddress" not in dry_instance_condition
dry_eni_condition = json.dumps(dry_run_by_resource["arn:aws:ec2:ap-northeast-2:<AWS_ACCOUNT_ID>:network-interface/*"]["Condition"])
assert "ec2:AssociatePublicIpAddress" in dry_eni_condition and '"false"' in dry_eni_condition
assert not any(":key-pair/" in resource for resource in dry_run_resources)

print("additive managed-policy documents valid:", {name: len(compact(doc)) for name, doc in documents.items()})
