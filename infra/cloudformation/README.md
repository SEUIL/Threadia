# Threadia restricted foundation CloudFormation

> **중요 — legacy**: `restricted-foundation.yaml`과 이름이 `restricted-foundation-*`인 기존 policy/parameter source는 이전 monolithic baseline이며 삭제하지 않는다. 새 Change Set 또는 IAM bootstrap에는 사용하지 말고, `restricted-network.yaml` → `restricted-database.yaml` → `restricted-compute.yaml` 및 [staged runbook](./restricted-foundation-staged-runbook.md)을 canonical 기준으로 사용한다.

> **delivery gate**: foundation 세 stage가 각각 승인된 Change Set과 post-verification을 통과하기 전에는 `delivery-foundation.yaml`을 생성·실행하지 않는다.

> **작성자**: Threadia 팀  
> **작성일**: 2026-07-13  
> **최종 수정**: 2026-07-14  
> **버전**: v1.0  
> **상태**: 검토 중  
> **태그**: aws, cloudformation, ec2, rds, security

## 1. 목적과 상태

`restricted-foundation.yaml`은 `ap-northeast-2`에서 검토할 non-production restricted foundation의 첫 stack template이다. 현재 세션은 template 준비만 수행하며 AWS API 호출, stack 생성, IAM policy 연결 또는 배포를 수행하지 않는다.

이 foundation은 public subnet의 EC2 한 대와 private RDS MySQL만 준비한다. public API ingress와 frontend 연동은 없으며 production deployment 구성이 아니다.

## 2. 포함 범위

- DNS support와 DNS hostname이 활성화된 VPC
- Internet Gateway와 명시적 public/private route table
- 첫 번째 Availability Zone의 public EC2 subnet 한 개
- 서로 다른 두 Availability Zone의 private DB subnet 두 개
- inbound rule이 없고 SSM HTTPS, VPC DNS, RDS MySQL만 명시적으로 허용하는 EC2 Security Group
- EC2 Security Group에서 오는 MySQL `3306/tcp`만 허용하는 RDS Security Group
- `AmazonSSMManagedInstanceCore`만 연결한 EC2 role과 instance profile
- `KeyName`과 application `UserData`가 없고 IMDSv2를 강제하는 EC2
- public access가 없고 암호화된 Single-AZ RDS MySQL
- RDS가 Secrets Manager에서 master password를 생성·관리하는 `ManageMasterUserPassword: true` 구성

Single-AZ RDS도 DB subnet group에는 서로 다른 두 Availability Zone의 subnet이 필요하다. private route table에는 Internet Gateway 또는 NAT 경로를 추가하지 않는다.

모든 tag 지원 resource에는 `Project=Threadia`, `Environment=restricted-nonprod`, `Owner=<parameter>`, `ManagedBy=CloudFormation`을 명시한다. foundation EC2에는 초기 tag `DeliveryHost=true`도 함께 부여하며, delivery stack은 이 EC2 tag를 생성하거나 변경하지 않는다. template은 고정 `RoleName`, DB identifier 또는 resource name을 사용하지 않는다.

## 3. 명시적 제외 범위

다음 항목은 이 restricted foundation에 포함하지 않는다.

- NAT Gateway
- Route 53, `threadia.dev`를 포함한 domain, DNS record, ACM certificate
- Amplify 또는 다른 frontend hosting
- GitHub OIDC, CI workflow, deploy workflow, deploy role
- public API ingress, ALB, API Gateway, inbound EC2 Security Group rule
- application `UserData`, Docker/Nginx/application 배포
- EC2 role의 application secret 또는 RDS managed master secret 접근 권한
- production traffic, frontend/API integration, production deployment

## 4. Parameter 준비

`restricted-foundation-parameters.example.json`을 복사해 승인된 non-secret 값만 입력한다. account ID, password, token, access key, secret ARN 또는 다른 credential을 parameter에 넣지 않는다.

`DbEngineVersion`에는 default가 없다. Change Set 생성 전에 `ap-northeast-2`에서 실제 제공되는 승인된 MySQL engine version을 확인하고 `REPLACE_WITH_APPROVED_MYSQL_VERSION`을 교체한다. regional minor version을 추측하거나 template에 임의로 고정하지 않는다.

CIDR parameter는 RFC 1918 범위와 prefix 형식을 제한하고, CloudFormation `Rules`는 세 subnet CIDR이 서로 다른지 확인한다. CIDR이 VPC 범위 안에 있고 서로 겹치지 않는지는 Change Set 전 별도로 검토한다.

## 5. Local validation

현재 작업에서는 AWS API를 호출하지 않는다. repository artifact는 다음 명령으로만 검사한다.

```bash
python3 -c "import pathlib, yaml; yaml.compose(pathlib.Path('infra/cloudformation/restricted-foundation.yaml').read_text(encoding='utf-8'))"
python3 infra/cloudformation/validate-restricted-foundation-service-policies.py
python3 -c "import json, pathlib, re; files=['infra/cloudformation/restricted-foundation-parameters.example.json','infra/cloudformation/restricted-foundation-operator-policy.json','infra/cloudformation/restricted-foundation-service-guardrails-policy.json','infra/cloudformation/restricted-foundation-service-permissions-network-policy.json','infra/cloudformation/restricted-foundation-service-permissions-runtime-policy.json']; texts=[pathlib.Path(f).read_text(encoding='utf-8') for f in files]; [json.loads(t) for t in texts]; assert all(not re.search(r'(?<![0-9])[0-9]{12}(?![0-9])',t) for t in texts); print('valid JSON without account IDs')"
cfn-lint infra/cloudformation/restricted-foundation.yaml
```

`cfn-lint`가 없으면 dependency를 추가하지 않고 승인 환경에서 다시 실행한다. 다음 Access Analyzer 명령은 AWS API를 호출하므로 현재 실행하지 않으며, 향후 승인 window에서 account ID를 치환한 local copy에 대해서만 실행한다.

```bash
aws accessanalyzer validate-policy \
  --policy-type IDENTITY_POLICY \
  --policy-document file:///tmp/restricted-foundation-operator-policy.json

aws accessanalyzer validate-policy \
  --policy-type IDENTITY_POLICY \
  --policy-document file:///tmp/restricted-foundation-service-guardrails-policy.json

aws accessanalyzer validate-policy \
  --policy-type IDENTITY_POLICY \
  --policy-document file:///tmp/restricted-foundation-service-permissions-network-policy.json

aws accessanalyzer validate-policy \
  --policy-type IDENTITY_POLICY \
  --policy-document file:///tmp/restricted-foundation-service-permissions-runtime-policy.json
```

## 6. 두 role 권한 경계

향후 승인된 identity 흐름은 `ThreadiaBootstrapOperator` IAM user → `ThreadiaRestrictedFoundationOperatorRole` → CloudFormation Change Set → `ThreadiaRestrictedFoundationCloudFormationServiceRole` 순서다. 현재 세션에서는 policy 또는 role을 생성·연결하지 않는다.

- Bootstrap IAM user에는 operator role ARN 하나에 대한 `sts:AssumeRole`만 change window 동안 별도 허용한다. operator policy 자체에는 `sts:AssumeRole`을 넣지 않는다.
- operator role trust는 exact Bootstrap IAM user principal 하나만 허용하고 OIDC, SAML, service principal, account 전체 또는 root principal을 허용하지 않는다.
- service role trust는 `cloudformation.amazonaws.com` service principal의 `sts:AssumeRole`만 허용한다. 사람, Bootstrap user, operator role, OIDC 또는 CI principal은 이 role을 assume할 수 없다.
- operator policy는 이 source의 고정 stack/change-set에 대해 Change Set 생성·조회·실행·삭제, stack describe, exact service role에 대한 `iam:PassRole`만 허용한다.
- service role에는 `restricted-foundation-service-guardrails-policy.json`, `restricted-foundation-service-permissions-network-policy.json`, `restricted-foundation-service-permissions-runtime-policy.json`으로 생성한 세 customer-managed policy의 default version을 모두 연결한다. guardrails는 모든 explicit Deny, network/runtime의 union은 template의 EC2/VPC/RDS/IAM runtime role·instance profile, public SSM AMI parameter, RDS managed secret 생성/tag, 기본 Secrets Manager KMS key metadata Allow를 보유한다. 세 policy 중 하나라도 누락된 상태는 승인하지 않으며 service role request에는 `aws:CalledVia`가 없을 수 있으므로 해당 condition을 사용하지 않는다.

`<AWS_ACCOUNT_ID>`는 account-specific ARN에만 존재한다. 승인 전에 operator와 세 service managed-policy source의 hash를 기록하고, repository 밖의 권한 제한된 temporary local copy에서 이 token만 일괄 치환한다. 치환 후 JSON diff에는 account ID 외 변경이 없어야 하며 account ID가 포함된 copy와 validation output은 작업 종료 즉시 삭제한다.

root는 향후 승인된 IAM bootstrap에서 두 role과 trust, operator inline policy 하나, 같은 service role에 세 versioned customer-managed policy, Bootstrap user의 temporary assume-only permission만 준비한다. root는 Change Set 또는 stack을 실행하지 않고 logout한다. operator role과 Bootstrap assume permission은 window 종료 즉시 삭제한다. CloudFormation service role과 세 managed policy는 stack에 연결된 뒤 rollback과 이후 update에 계속 사용되므로 stack이 존재하는 동안 함께 유지하며 사람에게 재사용하지 않는다.

### Root bootstrap recovery sequence

실제 root bootstrap은 role을 만든 뒤 aggregate inline-policy quota 초과로 `RestrictedFoundationServiceGuardrailsPolicy` inline policy만 service role에 연결된 상태에서 중단되었다. stack과 AWS resource는 생성하지 않았으며, 다음은 다음 승인 window에서만 root가 실행할 최소 recovery sequence다. 현재 작업에서는 이 명령을 실행하지 않는다.

1. repository 밖 권한 제한 temporary directory에 세 service policy source를 복사하고 `<AWS_ACCOUNT_ID>`만 local account ID로 치환한 뒤, 5절의 local validation과 Access Analyzer validation을 실행한다.
2. 아래 세 `create-policy` 명령으로 각 source에서 customer-managed policy를 만든다. 각 생성 결과의 default version은 `v1`이며, 이후 변경은 새 version 생성·검토·default 전환으로만 수행한다.
3. 반환받은 세 ARN을 같은 `ThreadiaRestrictedFoundationCloudFormationServiceRole`에 모두 attach한다.
4. 세 attachment가 확인된 뒤에만 이미 연결된 partial `RestrictedFoundationServiceGuardrailsPolicy` **inline policy를 삭제**한다. 새 guardrails managed policy를 먼저 attach하므로 explicit Deny의 공백이 생기지 않는다.
5. Change Set이나 stack은 실행하지 않고 logout한다.

```bash
SERVICE_ROLE_NAME=ThreadiaRestrictedFoundationCloudFormationServiceRole
POLICY_DIR=/approved/private/path/with-account-id-substituted-policies

GUARDRAILS_POLICY_ARN="$(aws iam create-policy \
  --policy-name ThreadiaRestrictedFoundationServiceGuardrails \
  --policy-document file://"$POLICY_DIR"/restricted-foundation-service-guardrails-policy.json \
  --query 'Policy.Arn' --output text)"

NETWORK_POLICY_ARN="$(aws iam create-policy \
  --policy-name ThreadiaRestrictedFoundationServicePermissionsNetwork \
  --policy-document file://"$POLICY_DIR"/restricted-foundation-service-permissions-network-policy.json \
  --query 'Policy.Arn' --output text)"

RUNTIME_POLICY_ARN="$(aws iam create-policy \
  --policy-name ThreadiaRestrictedFoundationServicePermissionsRuntime \
  --policy-document file://"$POLICY_DIR"/restricted-foundation-service-permissions-runtime-policy.json \
  --query 'Policy.Arn' --output text)"

aws iam attach-role-policy --role-name "$SERVICE_ROLE_NAME" --policy-arn "$GUARDRAILS_POLICY_ARN"
aws iam attach-role-policy --role-name "$SERVICE_ROLE_NAME" --policy-arn "$NETWORK_POLICY_ARN"
aws iam attach-role-policy --role-name "$SERVICE_ROLE_NAME" --policy-arn "$RUNTIME_POLICY_ARN"
aws iam list-attached-role-policies --role-name "$SERVICE_ROLE_NAME"

aws iam delete-role-policy \
  --role-name "$SERVICE_ROLE_NAME" \
  --policy-name RestrictedFoundationServiceGuardrailsPolicy
```

이 sequence는 `RestrictedFoundationServiceGuardrailsPolicy` inline policy만 제거한다. operator policy, template, delivery policy와 delivery IaC는 변경하지 않는다. 세 managed policy 중 하나라도 create 또는 attach에 실패하면 partial attachment를 제거하고 승인 기록·source review로 돌아가며, 현장에서 permissions를 넓히거나 inline policy를 다시 추가하지 않는다.

## 7. exact review와 change window

작성자와 독립 reviewer는 같은 revision의 template, parameter, operator policy, service guardrails managed-policy source, network managed-policy source, runtime managed-policy source 전체를 검토하고 SHA-256, UTC 시작·종료, operator, rollback 책임자를 승인 기록에 남긴다. account ID, ARN, credential, secret value는 기록하지 않는다.

1. operator `Allow`가 CloudFormation Change Set/stack describe와 exact service-role `iam:PassRole`뿐인지 확인한다.
2. `cloudformation:CreateChangeSet`은 `cloudformation:RoleARN`과 `--role-arn` 모두 exact service role을 가리켜야 한다.
3. service guardrails에는 Deny statement만, 두 service permissions source에는 Allow statement만 있어야 한다. 세 source 모두 CloudFormation control-plane action과 `aws:CalledVia`가 없어야 하며, local validator의 permission fingerprint assertion이 원본 28 statement와 정확히 일치해야 한다. 세 managed policy의 default version이 같은 service role에 모두 연결되는지 확인한다.
4. `RunInstances`는 public AMI, 기존 tagged subnet/Security Group, 새 tagged instance, implicit volume/network-interface statement로 분리되어야 한다. instance에는 승인 tag와 `t3.micro`/`t3.small`, IMDSv2 required, public IP, key pair 부재 조건을 모두 요구한다. template이 tag propagation을 설정하지 않는 implicit volume/ENI에는 request tag를 요구하지 않고 같은 instance type, IMDSv2, public IP, key pair 부재 request 조건으로 생성을 제한한다.
5. `ec2:CreateTags`는 VPC, Internet Gateway, subnet, route table, Security Group, instance, volume, network interface ARN과 `ec2:CreateAction`의 승인 create action에만 적용되어야 한다.
6. runtime role ARN은 `threadia-restricted-foundation-Ec2Role-*`, instance profile ARN은 `threadia-restricted-foundation-Ec2InstanceProfile-*`, managed policy는 `AmazonSSMManagedInstanceCore`, runtime `iam:PassRole` service는 `ec2.amazonaws.com` 하나여야 한다.
7. explicit Deny가 서울 외 regional action, Route 53/domain/ACM/Amplify/OIDC/CI/deploy/runtime secret read, IAM policy·trust·credential escalation, unapproved attachment와 arbitrary `iam:PassRole`을 차단해야 한다.
8. Change Set은 template logical resource, parameter, tag, EC2 trust `ec2.amazonaws.com`, managed policy, RDS private/encrypted/Single-AZ/managed-password 설정을 source와 대조한다. `KeyName`, `UserData`, public ingress, domain/OIDC/CI/deploy resource가 있으면 이 review-only source에서는 실행하지 않으며, execution은 별도의 승인된 policy revision/window에서만 허용한다.

EC2 lifecycle mutation은 action별 VPC, Internet Gateway, subnet, route table, Security Group, instance ARN statement로 분리하고 모든 statement에서 기존 foundation tag를 요구한다. route/association과 gateway attachment 같은 multi-resource authorization도 지원 ARN과 tag로 제한한다. 향후 account ID 치환본에 대한 Access Analyzer 결과에서 unsupported resource/condition 또는 missing required resource finding이 하나라도 있으면 blocker로 처리하고, IAM Service Authorization Reference와 Change Set의 실제 대상 ARN을 사람이 다시 대조한다. 이를 해결하기 위해 `Resource: "*"`로 넓히지 않는다.

operator role session은 1시간 이하이고 policy와 Bootstrap assume permission은 승인 UTC window 안에서만 유효하다. 권한 부족을 현장에서 확대하지 않고 source 수정, 독립 review, 새 window로 돌아간다.

## 8. 첫 stack Change Set-only 절차

아래 명령은 향후 승인된 operator role session에서만 사용한다. `APPROVED_OWNER`, `APPROVED_DB_ENGINE_VERSION`, `APPROVED_DB_MASTER_USERNAME`, `CLOUDFORMATION_SERVICE_ROLE_ARN`은 모두 승인된 non-secret local value이며 repository에 저장하지 않는다.

```bash
aws cloudformation create-change-set \
  --region ap-northeast-2 \
  --stack-name threadia-restricted-foundation \
  --change-set-name restricted-foundation-create-YYYYMMDD \
  --change-set-type CREATE \
  --template-body file://infra/cloudformation/restricted-foundation.yaml \
  --parameters \
    ParameterKey=Owner,ParameterValue="$APPROVED_OWNER" \
    ParameterKey=DbEngineVersion,ParameterValue="$APPROVED_DB_ENGINE_VERSION" \
    ParameterKey=DbMasterUsername,ParameterValue="$APPROVED_DB_MASTER_USERNAME" \
  --capabilities CAPABILITY_IAM \
  --role-arn "$CLOUDFORMATION_SERVICE_ROLE_ARN" \
  --tags \
    Key=Project,Value=Threadia \
    Key=Environment,Value=restricted-nonprod \
    Key=Owner,Value="$APPROVED_OWNER" \
    Key=ManagedBy,Value=CloudFormation \
  --on-stack-failure ROLLBACK

aws cloudformation describe-change-set \
  --region ap-northeast-2 \
  --stack-name threadia-restricted-foundation \
  --change-set-name restricted-foundation-create-YYYYMMDD

# 제출된 Original template을 JSON envelope로 추출하고 승인 source와 exact hash를 비교한다.
# `--output text`는 presentation format이므로 byte identity 검증에 사용하지 않는다.
aws cloudformation get-template \
  --region ap-northeast-2 \
  --stack-name threadia-restricted-foundation \
  --change-set-name restricted-foundation-create-YYYYMMDD \
  --template-stage Original \
  --output json > /tmp/restricted-foundation-get-template.json

python3 -c "import json,pathlib; r=json.loads(pathlib.Path('/tmp/restricted-foundation-get-template.json').read_text(encoding='utf-8')); pathlib.Path('/tmp/restricted-foundation-submitted-template.yaml').write_text(r['TemplateBody'],encoding='utf-8')"
sha256sum infra/cloudformation/restricted-foundation.yaml /tmp/restricted-foundation-submitted-template.yaml
cmp --silent infra/cloudformation/restricted-foundation.yaml /tmp/restricted-foundation-submitted-template.yaml

# 사람의 Change Set 검토와 별도 승인이 끝난 뒤에만 실행한다.
aws cloudformation execute-change-set \
  --region ap-northeast-2 \
  --stack-name threadia-restricted-foundation \
  --change-set-name restricted-foundation-create-YYYYMMDD
```

`GetTemplate`은 exact restricted stack ARN에 대해서만 허용한다. 두 SHA-256이 같고 `cmp`가 exit code 0일 때만 실행 승인을 진행한다. 불일치는 whitespace를 포함해 blocker이며 retrieved template을 정규화하거나 승인 source를 현장에서 수정하지 않는다. 검토 후 `/tmp/restricted-foundation-get-template.json`과 `/tmp/restricted-foundation-submitted-template.yaml`을 삭제한다.

기본값이 아닌 CIDR, instance type, DB class 또는 storage를 승인한 경우 해당 `ParameterKey=...,ParameterValue=...`를 같은 explicit sequence에 추가한다. `--parameters file://...`는 AWS CLI syntax가 아니므로 사용하지 않는다. EC2 role 때문에 `CAPABILITY_IAM`이 필요하지만 named IAM resource가 없어 `CAPABILITY_NAMED_IAM`은 사용하지 않는다. `ExecuteChangeSet`은 생성 시 연결한 service role을 자동으로 사용한다.

## 9. rollback과 권한 회수

실행 전 문제는 Change Set 삭제로 종료한다. 실행 후 create failure는 `--on-stack-failure ROLLBACK`과 service role로 자동 rollback한다. operator는 stack event를 describe만 하며 direct rollback/delete 권한을 갖지 않는다. `UPDATE_ROLLBACK_FAILED` 또는 자동 rollback 실패 시 policy를 현장에서 넓히지 않고 별도 break-glass 설계와 승인을 받는다.

성공 또는 안정된 rollback 뒤 operator session, Bootstrap assume-only permission, operator inline policy와 role을 제거한다. service role과 세 customer-managed policy는 stack이 존재하는 동안 CloudFormation 전용으로 함께 유지한다. credential 노출, 예상 밖 resource/action, tag·trust drift, 서울 외 Region 또는 secret read 시도는 즉시 중단하고 관련 session을 revoke한다.

## 10. 첫 stack 보호와 post-verification hardening

첫 stack은 rollback을 막지 않도록 `DeletionProtection: false`를 사용한다. 삭제 또는 교체 시 데이터 손실을 줄이기 위해 첫 template부터 RDS에 `DeletionPolicy: Snapshot`과 `UpdateReplacePolicy: Snapshot`을 적용한다.

foundation 생성과 SSM 관리 상태, subnet/route, Security Group, RDS private/암호화 상태를 검증한 뒤에는 별도 template revision과 별도 `UPDATE` Change Set으로 다음 세 항목만 hardening한다.

1. RDS `DeletionProtection`을 `true`로 변경한다.
2. RDS `DeletionPolicy`를 `Retain`으로 변경한다.
3. RDS `UpdateReplacePolicy`를 `Retain`으로 변경한다.

hardening은 첫 stack Change Set에 섞지 않는다. template diff와 `UPDATE` Change Set을 함께 review하고, replacement나 다른 resource 변경이 없음을 확인한 뒤 별도 승인을 받아 같은 승인 window 안에서 실행한다. 같은 window 안에 완료할 수 없으면 9절에 따라 operator role과 temporary Bootstrap permission을 먼저 제거한다. `DeletionPolicy`와 `UpdateReplacePolicy`는 Change Set summary에 충분히 드러나지 않을 수 있으므로 source diff review를 생략하지 않는다.

이 template에는 Outputs가 없다. password, managed secret ARN, public IP, domain URL 또는 credential을 stack output이나 검증 기록에 추가하지 않는다.
