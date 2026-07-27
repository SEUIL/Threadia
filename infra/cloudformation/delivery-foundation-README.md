> **작성자**: Threadia 팀  
> **작성일**: 2026-07-13  
> **최종 수정**: 2026-07-14  
> **버전**: v0.4  
> **상태**: 검토 중  
> **태그**: aws, cloudformation, iam, oidc, codedeploy, s3

# Threadia delivery foundation CloudFormation

> **보류 상태**: 이 delivery artifact는 legacy monolithic foundation 기준의 reviewed source로 유지한다. additive `Network` → `Database` → `Compute` foundation의 각 stage가 안정화되고 별도 승인된 이후에만 delivery Change Set 검토를 재개한다. delivery S3/CodeDeploy private endpoint와 해당 template/policy 변경은 별도 명시 승인 범위이며 이 foundation task에 포함하지 않는다. 이 문서는 해당 안정화 전 deployment 승인을 의미하지 않는다.

## 1. 목적과 적용 전제

`delivery-foundation.yaml`은 이미 승인·생성된 `restricted-foundation` EC2 한 대에 delivery capability를 별도 stack으로 추가하는 reviewed artifact다. 이 repository revision은 AWS API 호출, Change Set 생성·실행, IAM policy 연결, GitHub Environment 변경, resource 생성 또는 deployment를 수행하지 않는다.

이 template은 GitHub OIDC provider와 deploy role, private·versioned·SSE-S3 artifact bucket, CodeDeploy application/deployment group 및 CodeDeploy-only service role을 생성한다. 기존 EC2 runtime role에는 `releases/*` prefix의 `s3:GetObject`와 `s3:GetObjectVersion`만 inline policy로 추가한다. `AWS-ConfigureAWSPackage` SSM Association은 지정 instance에 `AWSCodeDeployAgent` 설치을 요청하고, foundation이 초기 EC2 tag로 부여한 `DeliveryHost=true`로 deployment group 대상을 고정한다. delivery stack은 이 EC2 tag를 생성하거나 변경하지 않는다.

## 2. 역할 분리와 신뢰 경계

| 역할 또는 profile | trust와 허용 범위 | 명시적 제외 |
|---|---|---|
| GitHub OIDC deploy role | exact `repo:<approved repository>:environment:restricted-nonprod` subject 및 `aud=sts.amazonaws.com`; artifact `releases/*` read/write와 지정 CodeDeploy application/deployment group deployment 생성 | EC2/SSM 직접 제어, IAM 관리, RDS, secret read |
| CodeDeploy service role | `codedeploy.amazonaws.com`만 assume; AWS 관리형 `AWSCodeDeployRole` service role policy | GitHub OIDC assume, artifact publish, runtime secret read |
| 기존 EC2 instance profile | `ec2.amazonaws.com` trust는 foundation에 유지; artifact `releases/*` object/version read만 추가 | deployment 생성, GitHub OIDC assume, RDS master secret 또는 application secret read |

GitHub Environment `restricted-nonprod`에는 GitHub UI에서 정확히 하나의 custom deployment branch policy로 `main`만 허용해야 한다. 이 one-month personal portfolio 예외에서는 required reviewer를 의도적으로 설정하지 않는다. Environment 설정은 `custom_branch_policies=true`, `protected_branches=false`여야 하며, `main` 외 branch 또는 tag 정책을 추가하지 않는다. 이 branch 정책은 Environment 접근을 제한하는 설정이며, Environment subject를 사용하는 OIDC trust에 branch condition 또는 wildcard subject를 추가하지 않는다. `GitHubRepository`는 Change Set 전에 exact owner/repository 값으로만 공급한다.

## 3. artifact와 host 계약

- artifact bucket은 public access block, bucket-owner-enforced ownership, versioning, SSE-S3 encryption과 TLS-only bucket policy를 사용한다.
- publish/read prefix는 `releases/*` 하나다. mutable `latest` object, public URL, delete 권한을 제공하지 않는다.
- noncurrent version retention 기본값은 30일이며 승인된 값만 parameter로 변경한다. bucket은 data 보존을 위해 `Retain` 정책을 사용한다.
- GitHub workflow는 immutable commit SHA와 build run identifier를 key에 포함하고, CodeDeploy revision에 exact object version 및 manifest checksum을 사용해야 한다. workflow와 AppSpec/lifecycle hook은 별도 review 대상이며 이 artifact에 포함하지 않는다.
- SSM Association 성공만으로 application readiness를 뜻하지 않는다. CodeDeploy agent 상태, host-local systemd service, immutable artifact checksum, `/actuator/health`, rollback은 deployment release gate에서 별도 확인한다.

## 4. DB credential 경계와 one-time bootstrap

RDS `ManageMasterUserPassword`의 master credential은 application 또는 CodeDeploy runtime에 제공하지 않는다. 이 template은 Secrets Manager read 권한, secret ARN parameter, secret injection, DB migration 또는 application startup configuration을 만들지 않는다.

application DB credential은 승인된 operator가 RDS private endpoint에 대해 **one-time manual bootstrap**으로 master와 분리된 최소 권한 DB user를 생성한 뒤 별도 승인된 secret store·rotation·runtime injection 설계로 넘겨야 한다. DB user name, password, secret ARN, SQL, account ID 및 credential 값은 repository, CloudFormation parameter, S3 artifact, workflow log 또는 Change Set evidence에 기록하지 않는다. 이 bootstrap owner와 secret lifecycle이 확정되기 전에는 runtime secret read permission이나 deployment를 진행하지 않는다.

## 5. parameter와 local validation

`delivery-foundation-parameters.example.json`을 복사하여 승인된 non-secret 값만 입력한다. placeholder, account ID, ARN, password, token, access key 또는 secret ARN은 repository에 저장하지 않는다.

AWS API를 호출하지 않는 local validation만 실행한다.

```bash
python3 -c "import json,pathlib,yaml; json.load(open('infra/cloudformation/delivery-foundation-parameters.example.json', encoding='utf-8')); yaml.compose(pathlib.Path('infra/cloudformation/delivery-foundation.yaml').read_text(encoding='utf-8')); print('YAML and JSON parse successfully')"
cfn-lint infra/cloudformation/delivery-foundation.yaml
```

`cfn-lint`가 설치되지 않은 환경에서는 dependency를 추가하지 않는다. 승인된 validation 환경에서 실행해 결과를 기록한다. Access Analyzer validation, CloudFormation Change Set, GitHub Environment configuration, IAM attachment, SSM Association execution 및 CodeDeploy deployment는 모두 AWS 또는 GitHub mutation이므로 별도 승인 window에서만 수행한다.

## 6. delivery IAM bootstrap과 Change Set 역할

delivery revision은 기존 `restricted-foundation-*` operator/service policy를 수정하거나 재사용하지 않는다. root는 승인된 window에서 다음 delivery 전용 role과 inline policy를 별도로 생성·연결한다.

| 대상 | 신뢰 또는 연결 | 허용 경계 |
|---|---|---|
| `ThreadiaDeliveryFoundationOperatorRole` | exact Bootstrap identity만 `sts:AssumeRole`; `delivery-foundation-operator-policy.json` 연결 | 현재 reviewed delivery operator policy는 고정 delivery stack/change set의 생성·조회·삭제와 exact delivery service role의 CloudFormation `iam:PassRole`만 허용하며, 실행은 별도의 승인된 policy revision/window에서만 가능 |
| `ThreadiaDeliveryFoundationCloudFormationServiceRole` | `cloudformation.amazonaws.com`만 `sts:AssumeRole`; `delivery-foundation-service-permissions-policy.json` 연결 | delivery template의 OIDC provider, artifact bucket/policy, 두 delivery role, existing EC2 artifact inline policy, SSM association, CodeDeploy application/group만 허용 |

bootstrap identity에는 operator role ARN 하나에 대한 temporary `sts:AssumeRole`만 별도 허용한다. delivery operator/service role, GitHub OIDC deploy role, CodeDeploy service role, EC2 instance profile은 서로 assume할 수 없다. root와 Bootstrap identity는 Change Set을 생성·실행하지 않는다; current reviewed delivery operator policy는 create/review/delete only이며 execution은 별도의 승인된 policy revision/window가 필요하다.

두 policy의 `<AWS_ACCOUNT_ID>`는 승인 전 repository 밖 권한 제한 temporary copy에서만 치환한다. source hash를 기록하고 치환 diff가 account ID 외 변경이 없음을 검토한다. account-specific copy, validation output 및 temporary Bootstrap assume permission은 window 종료 시 삭제한다.

delivery stack name은 `threadia-delivery-foundation`, Change Set name은 `delivery-foundation-*`로 고정한다. 이 naming은 service policy가 generated delivery role, bucket, CodeDeploy application/group 및 existing EC2 inline policy `threadia-delivery-foundation-artifact-read`를 제한하는 경계다. 다른 stack name, role, policy name 또는 resource ARN이 Change Set에 나타나면 실행하지 않는다.

## 7. Change Set preflight

1. `restricted-foundation` stack이 승인된 EC2 instance와 EC2 role/profile을 이미 생성했는지 확인한다.
2. `ExistingEc2InstanceId`와 `ExistingEc2RoleName`이 같은 approved foundation host를 가리키고 foundation이 초기 부여한 `Project=Threadia`, `Environment=restricted-nonprod`, `DeliveryHost=true` tag를 유지하는지 확인한다. delivery stack은 이 EC2 tag를 생성하거나 변경하지 않는다.
3. GitHub Environment 이름이 정확히 `restricted-nonprod`이고, one-month personal portfolio 예외로 required reviewer가 의도적으로 없는지 확인한다. `custom_branch_policies=true`, `protected_branches=false`와 정확히 하나의 custom deployment branch policy `main`이 적용되어야 한다. OIDC job은 계속해서 `id-token: write`와 `contents: read` 최소 permission만 가져야 한다.
4. GitHub OIDC trust가 exact repository Environment subject와 `aud=sts.amazonaws.com`만 허용하고 wildcard, account principal, long-lived access key가 없는지 확인한다.
5. artifact bucket의 versioning, encryption, public block, TLS Deny와 `releases/*` prefix-only policy를 확인한다.
6. `AWSCodeDeployRole`가 CodeDeploy service role에만 연결되고 GitHub deploy role이나 EC2 runtime role에 연결되지 않는지 확인한다.
7. RDS master secret read, runtime secret injection, DB migration, SSH, public ingress, domain 또는 frontend hosting 리소스가 Change Set에 없는지 확인한다.
8. application DB user bootstrap owner, secret store, rotation, runtime injection owner, migration owner와 rollback boundary가 별도 승인되었는지 확인한다.
9. delivery operator `Allow`가 delivery Change Set/stack describe 및 exact CloudFormation service role `iam:PassRole`뿐인지 확인한다. service policy는 `ssm:SendCommand`, interactive session, secret read, arbitrary role-policy attachment 또는 arbitrary `iam:PassRole`을 허용하지 않아야 한다.
10. existing EC2 role inline policy가 legacy `threadia-restricted-foundation-Ec2Role-*`와 additive `threadia-restricted-compute-Ec2Role-*`로만 제한되고, exact `threadia-delivery-foundation-artifact-read` policy name으로만 연결되며 policy document에 secret read, broad S3, `sts:AssumeRole`, `iam:PassRole`가 없는지 확인한다.
11. account-ID 치환본에 `aws accessanalyzer validate-policy`를 실행하고 unsupported resource/condition 또는 missing required resource finding이 하나라도 있으면 blocker로 처리한다. `cfn-lint`와 Original template byte hash 비교, Change Set 대상 ARN·condition의 독립 review가 완료되기 전에는 execute하지 않는다.
