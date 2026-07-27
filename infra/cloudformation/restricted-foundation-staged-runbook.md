> **작성자**: Threadia 팀  
> **작성일**: 2026-07-14  
> **최종 수정**: 2026-07-14  
> **버전**: v1.3  
> **상태**: 검토 중  
> **태그**: aws, cloudformation, iam, security, preflight

# Additive restricted foundation staged runbook

## 1. 범위와 legacy 경계

이 runbook의 canonical source는 `restricted-network.yaml`, `restricted-database.yaml`, `restricted-compute.yaml`이다. 기존 `restricted-foundation.yaml` 및 `restricted-foundation-*` artifact는 감사와 rollback 기록을 위해 **legacy/deprecated**로 보존하며 수정·배포 대상이 아니다.

AWS API 호출, IAM policy 연결, Change Set 생성·실행은 이 repository 작업에 포함하지 않는다. `delivery-foundation.yaml`은 세 foundation stage 안정화 전까지 보류한다.

| 순서 | Stack | 소유 resource | prerequisite |
|---|---|---|---|
| 1 | `threadia-restricted-network` | VPC, private compute/DB subnet, EC2 SG, SSM endpoint SG, private SSM interface endpoint | 없음 |
| 2 | `threadia-restricted-database` | RDS SG, DB subnet group, private encrypted Single-AZ MySQL | Network export |
| 3 | `threadia-restricted-compute` | EC2 egress, EC2 role/profile, IMDSv2 EC2 | Network/Database export, IAM Simulator, EC2 DryRun |

Network에는 Internet Gateway, NAT, public route, public subnet이 없다. EC2는 public IP, public ingress, `UserData`, `KeyName` 없이 private SSM endpoint와 private RDS만 통신한다. Database는 `ManageMasterUserPassword: true`, `DeletionPolicy: Snapshot`, `UpdateReplacePolicy: Snapshot`, deletion-protection parameter를 사용하며 secret 또는 endpoint를 output하지 않는다.

## 2. policy source와 local validation

각 service role에는 공통 `restricted-service-guardrails-policy.json`과 해당 stage Allow policy만 연결한다. guardrail은 Deny-only다. `restricted-compute-dry-run-policy.json`은 Compute Change Set 전 승인 window의 simulator/DryRun 검토 source이며, production role attachment 또는 permission 확대의 근거가 아니다.

```bash
python3 infra/cloudformation/validate-restricted-foundation-service-policies.py
python3 infra/cloudformation/validate-restricted-compute-preflight.py
python3 -c "import json,pathlib,yaml; [json.loads(pathlib.Path(p).read_text(encoding='utf-8')) for p in pathlib.Path('infra/cloudformation').glob('restricted-*-parameters.example.json')]; [yaml.compose(pathlib.Path(p).read_text(encoding='utf-8')) for p in ['infra/cloudformation/restricted-network.yaml','infra/cloudformation/restricted-database.yaml','infra/cloudformation/restricted-compute.yaml']]; print('local YAML and JSON parsing passed')"
cfn-lint infra/cloudformation/restricted-network.yaml infra/cloudformation/restricted-database.yaml infra/cloudformation/restricted-compute.yaml
```

모든 policy source는 minified JSON으로 6144 characters 이하이어야 한다. account ID가 포함되지 않은 source의 `<AWS_ACCOUNT_ID>`와 `<APPROVED_AMI_ID>`는 승인 window에서 repository 밖 temporary copy에만 치환한다. source hash와 치환 diff는 account ID/approved AMI ID 외 변경이 없음을 독립 reviewer가 확인한다.

### Root bootstrap role process

root는 승인된 bootstrap window에서만 Change Set을 실행하지 않고 다음 source를 사용해 stage별 role을 준비한 뒤 logout한다. trust policy에는 account root, wildcard principal, OIDC, SAML, 사람 principal 추가를 금지한다.

| Stage | Bootstrap user → operator trust/source | Operator policy | CloudFormation service trust/source | Service role attachment |
|---|---|---|---|---|
| Network | `restricted-network-bootstrap-assume-policy.json` / `restricted-network-operator-role-trust-policy.json` | `restricted-network-operator-policy.json` | `restricted-network-service-role-trust-policy.json` | shared `restricted-service-guardrails-policy.json` + `restricted-network-service-permissions-policy.json` |
| Database | `restricted-database-bootstrap-assume-policy.json` / `restricted-database-operator-role-trust-policy.json` | `restricted-database-operator-policy.json` | `restricted-database-service-role-trust-policy.json` | shared guardrail + `restricted-database-service-permissions-policy.json` |
| Compute | `restricted-compute-bootstrap-assume-policy.json` / `restricted-compute-operator-role-trust-policy.json` | `restricted-compute-operator-policy.json` | `restricted-compute-service-role-trust-policy.json` | shared guardrail + `restricted-compute-service-permissions-policy.json` |

각 bootstrap assume source는 `ThreadiaBootstrapOperator` user가 exact stage operator role 하나만 assume하도록 제한한다. 각 operator source는 exact `threadia-restricted-{stage}` stack, `restricted-{stage}-*` Change Set, matching `ThreadiaRestricted{Stage}CloudFormationServiceRole`의 CloudFormation `iam:PassRole`만 allow한다. service role은 `cloudformation.amazonaws.com`만 assume하고 shared Deny-only guardrail과 해당 stage Allow policy만 attach한다. stage Allow policy에는 CloudFormation control-plane action을 넣지 않는다.

stable CREATE/rollback 뒤에는 해당 stage operator role과 Bootstrap user의 temporary assume permission을 detach/delete하고 session을 revoke한다. stack이 존재하는 동안 matching service role과 두 attached managed policy는 CloudFormation rollback/update 전용으로 유지하며 사람 또는 delivery role에 재사용하지 않는다.

### Compute DryRun temporary role

Compute preflight의 EC2 DryRun은 `ThreadiaRestrictedComputeDryRunRole`만 사용한다. root는 승인된 DryRun window에서 `restricted-compute-dry-run-role-trust-policy.json`으로 role trust를 만들고, `restricted-compute-dry-run-bootstrap-assume-policy.json`을 `ThreadiaBootstrapOperator` user에 temporary로 연결한다. trust는 exact Bootstrap user만 허용하며 root, wildcard principal, OIDC, SAML 또는 service principal을 허용하지 않는다.

role에는 repository 밖 temporary local copy의 `restricted-compute-dry-run-policy.json` 하나만 attach한다. 이 policy는 IAM이 DryRun flag를 구분하지 못해 actual `ec2:RunInstances` mutation도 가능하므로 instance profile, `iam:PassRole`, 다른 managed/inline policy를 추가하지 않는다. approved DryRun evidence를 기록한 직후 policy를 detach하고 Bootstrap assume permission·role을 삭제하며 session과 account-specific local copy를 revoke/delete한다. Compute Change Set execution과 persistent CloudFormation service role에는 이 temporary role을 사용하지 않는다.

## 3. Compute preflight approval gate

Compute Change Set 생성 전에 `validate-restricted-compute-preflight.py`가 성공해야 한다. 이 script는 AWS를 호출하지 않고 RunInstances resource type별 positive/negative matrix와 command를 생성한다.

| 대상 | positive | negative |
|---|---|---|
| IAM Simulator image | approved AMI | unapproved AMI |
| IAM Simulator subnet/SG | required Network tag | missing tag |
| IAM Simulator instance | IMDSv2, no key pair, no public IP | IMDSv1, key pair, public IP |
| IAM Simulator volume | encrypted `gp3` root volume | unencrypted or non-`gp3` volume |
| IAM Simulator ENI | no public IP | public IP |
| IAM Simulator PassRole | generated Compute EC2 role to `ec2.amazonaws.com` | other role or service |
| EC2 DryRun | approved AMI, explicit ENI, encrypted `gp3` `/dev/xvda`, no public IP | identical request with public IP |

IAM Simulator command는 실제 `COMPUTE_SERVICE_ROLE_ARN`을 policy source ARN으로 사용해 image, subnet, SG, instance, volume, ENI 전체 multi-resource authorization을 검증한다. `iam:PassRole`도 같은 service role에 대해 별도 검증한다.

`restricted-compute-dry-run-policy.json`은 `ThreadiaRestrictedComputeDryRunRole`에만 승인 window 동안 연결한다. `<AWS_ACCOUNT_ID>`, `<APPROVED_AMI_ID>`, `<APPROVED_SUBNET_ID>`, `<APPROVED_SECURITY_GROUP_ID>`는 repository 밖 temporary local copy에서 approved value로만 치환한다. 이 policy는 IAM이 DryRun flag를 구분하지 못하므로 실제 `ec2:RunInstances` mutation도 가능한 고위험 권한이다. 따라서 role에는 exact approved AMI/subnet/SG ARN 외 권한, instance profile, `iam:PassRole`를 주지 않고, generated DryRun command에는 `--iam-instance-profile`이 없다. 결과 기록 뒤에는 policy를 detach하고 temporary role과 local account-specific copy를 즉시 삭제·revoke한다.

승인된 operator는 generated simulator/DryRun command의 placeholder를 approved non-secret runtime value로만 바꾼다. Simulator의 모든 positive verdict가 `allowed`이고 모든 negative verdict가 `explicitDeny` 또는 `implicitDeny`여야 한다. DryRun의 positive request는 `DryRunOperation`이어야 하며 negative request는 `UnauthorizedOperation`이어야 한다. 이 결과가 없거나 반대이면 Compute Change Set을 만들지 않는다. 이 repository에서 AWS Simulator 또는 DryRun이 통과했다고 주장하지 않는다.

## 4. staged Change Set approval

각 stage에서 다음을 반복하고, 다음 stage로 넘어가기 전에 stable `CREATE_COMPLETE` 또는 승인된 rollback 상태를 독립 reviewer가 확인한다.

1. canonical template, parameter example, stage policy, shared guardrail의 SHA-256과 local validation 결과를 기록한다.
2. approved operator가 exact stage-specific CloudFormation service role을 `--role-arn`으로 지정한 **CREATE Change Set만** 생성한다. `NetworkStackName`, `DatabaseStackName`, `Owner`, approved AMI ID와 DB engine version은 repository에 저장하지 않는다.
3. reviewer가 Original template byte hash, resource ownership, imports/exports, policy attachment, tag, region `ap-northeast-2`을 확인한다. Compute는 3절 preflight evidence도 확인한다.
4. reviewer가 Change Set의 public ingress, NAT, public IP, `UserData`, `KeyName`, secret output/read, snapshot restore/delete/import, delivery resource가 없음을 확인한 뒤 별도 실행 승인을 한다.
5. execute 뒤 Network는 endpoint private DNS/SG boundary와 endpoint tag, Database는 private encrypted Single-AZ/managed password/snapshot lifecycle, Compute는 ENI public-IP false/encrypted root `gp3` volume/IMDSv2/SSM role/`DeliveryHost=true`만 post-verification한다. secret, secret ARN, public IP는 output·log·evidence에 기록하지 않는다.

권한 부족, unsupported condition, simulator mismatch, DryRun mismatch, unexpected resource 또는 region drift는 fail-closed blocker다. policy를 현장에서 넓히지 말고 source 수정·independent review·새 approval window로 돌아간다. rollback에는 같은 stage service role을 유지하며 RDS snapshot restore/delete/import 권한을 추가하지 않는다.

### Database hardening UPDATE gate

Database 최초 CREATE가 안정화된 뒤에는 별도 승인된 **UPDATE Change Set**만으로 `DbDeletionProtection=true`를 적용한다. 같은 hardening review에서 `DeletionPolicy`와 `UpdateReplacePolicy`를 initial `Snapshot` lifecycle에서 `Retain` lifecycle로 바꾸는 template revision을 별도 검토한다. CREATE Change Set과 hardening UPDATE를 섞지 않으며, replacement·다른 resource 변경·snapshot restore/delete/import 권한이 나타나면 blocker다. UPDATE Change Set source diff와 parameter value를 독립 reviewer가 승인하기 전에는 실행하지 않는다.

## 5. delivery release gate

Network, Database, Compute의 모든 Change Set 및 post-verification이 stable하다는 별도 승인 전에는 `delivery-foundation.yaml`, OIDC, CodeDeploy, artifact bucket, runtime secret read를 생성하거나 실행하지 않는다. delivery S3/CodeDeploy private endpoint와 그 template/policy 연결은 별도 명시 승인 범위가 필요하므로 이 task에서 구현하지 않는다.
