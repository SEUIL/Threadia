> **작성자**: 황스일  
> **작성일**: 2026-07-15  
> **최종 수정**: 2026-07-15  
> **버전**: v0.1  
> **상태**: 작성 중  
> **태그**: threadia, aws, github-actions, cloudformation, ssm, deployment

# Threadia SSM service-unit maintenance 협업 가이드

> 이 문서는 PR #124 merge 후 `threadia-backend.service`의 stale revision을 fixed SSM maintenance channel로 갱신하고, fresh release를 검증하기 위한 사용자·AI 협업 절차다. 실제 AWS/GitHub 변경은 사용자가 Console에서 승인·실행하고, AI는 실행 전 입력값 검토·실행 결과 판정·오류 분석을 담당한다.

## 1. 현재 상태

| 항목 | 상태 | 근거 |
| --- | --- | --- |
| service-unit maintenance source | 완료 | PR #124가 `main`에 merge됨 |
| local source 검증 | 완료 | preflight, JSON/YAML parse, shell syntax, alternate-root `systemd-analyze verify`, Oracle review 통과 |
| helper maintenance | 완료 | `apply-release` 갱신 workflow 성공 |
| fresh release | 실패 이력 있음 | 마지막 확인 release는 `/actuator/health` 503으로 실패 |
| service-unit artifact publish | 미실행 | immutable key, S3 VersionId, SHA-256 triple 없음 |
| CloudFormation Change Set | 미실행 | service-unit document와 pinned triple이 stack에 반영되지 않음 |
| service-unit SSM invoke | 미실행 | EC2의 unit revision은 아직 갱신되지 않음 |
| fresh release 재검증 | 미실행 | unit remediation 성공 후 별도 실행 필요 |

### 현재 blocker

1. repository default branch가 `dev`라서, `main`에만 merge된 `workflow_dispatch` workflow는 Actions UI에 등록되지 않는다.
2. `restricted-nonprod` Environment에 `THREADIA_SSM_SERVICE_UNIT_MAINTENANCE_DOCUMENT_NAME` 변수가 없다. 이 값은 stack update 후 얻는 physical document name이어야 한다.
3. CloudFormation service role의 IAM policy와 SSM document version capacity는 live Console에서 확인해야 한다.

## 2. 가장 빠른 협업 방식

한 번에 여러 Console 작업을 진행하지 않는다. 아래 checkpoint 하나를 끝낼 때마다 사용자가 증적을 보내고, AI가 `진행 가능`, `중단`, `수정 필요` 중 하나로 판정한다.

| 역할 | 담당 작업 |
| --- | --- |
| 사용자 | GitHub PR merge, Environment 변수 변경, AWS IAM/CloudFormation/SSM Console의 승인·실행 |
| AI | 입력값 대조, Change Set 변경 목록 검토, GitHub Actions/SSM 출력 해석, 실패 원인 분석, 다음 단계 안내 |
| 함께 확인 | key·VersionId·SHA-256 triple 일치, exact host/document, rollback/health 결과 |

### 사용자 공유 형식

- GitHub Actions: run URL, conclusion, summary의 `service_unit_key`, `service_unit_version_id`, `service_unit_sha256`, `commit`, `run`만 공유한다.
- CloudFormation: Change Set의 **변경 사항** 목록, service-unit parameter 3개, 실행 전 상태만 공유한다.
- Systems Manager: command ID, status, Standard output/error의 secret 없는 부분만 공유한다.
- Environment variable: 변수 **이름과 값이 document physical name인지 여부만** 공유한다. token, password, SecureString 값, AWS access key는 공유하지 않는다.

## 3. 실행 순서

### Checkpoint 0. `dev`에 manual workflow 등록

### 목표

GitHub Actions에서 다음 workflow를 수동 실행할 수 있게 한다.

- `restricted-ssm-service-unit-artifact`
- `restricted-ssm-service-unit-maintenance`

### 이유

GitHub 공식 문서 기준 `workflow_dispatch`는 workflow 파일이 repository의 default branch에 있을 때 수동 실행 버튼을 제공한다. Threadia의 default branch는 `dev`이며, PR #124는 `main`에 merge됐다.

### 담당

- AI: `dev` 기준으로 workflow 등록만 포함하는 작은 PR을 준비하거나, 사용자가 준비한 PR의 diff를 검토한다.
- 사용자: PR을 review/merge한다. default branch 변경이나 ruleset bypass는 하지 않는다.

### 확인 기준

1. GitHub repository의 **Actions** 탭에서 두 workflow 이름이 목록에 보인다.
2. 각 workflow의 **Run workflow** 버튼을 열면 branch 선택이 가능하다.
3. 실행할 때 branch는 반드시 `main`을 선택한다. workflow job 자체가 `refs/heads/main` 외 ref를 거부한다.

### 중단 조건

- workflow를 보이게 하려고 default branch를 `main`으로 바꾸지 않는다.
- `main` 전체를 `dev`에 merge하여 workflow를 등록하지 않는다. `dev`에 필요한 workflow 파일만 포함한 작은 PR을 사용한다.

### Checkpoint 1. GitHub Environment 사전 점검

### Console 경로

GitHub repository → **Settings** → **Environments** → `restricted-nonprod`

### 사용자가 확인할 값

| 변수 | 기대값 | 조치 |
| --- | --- | --- |
| `AWS_REGION` | `ap-northeast-2` | stack Region과 다르면 중단 |
| `AWS_ROLE_TO_ASSUME` | approved GitHub OIDC role ARN | stack output과 대조 |
| `THREADIA_ARTIFACT_BUCKET` | restricted artifact bucket | stack output과 대조 |
| `THREADIA_DELIVERY_INSTANCE_ID` | approved exact EC2 instance | 기존 승인값 유지 |
| `THREADIA_SSM_SERVICE_UNIT_MAINTENANCE_DOCUMENT_NAME` | 아직 없음 | **이 checkpoint에서는 만들지 않음** |

또한 deployment branch 정책이 `main`만 허용하는지 확인한다.

### 중단 조건

- service-unit document 변수에 예상 이름, 빈 값, 임시 값을 미리 넣지 않는다.
- role ARN, bucket, instance ID가 기존 승인값과 다르면 AI에게 화면 정보를 공유하고 중단한다.

### Checkpoint 2. immutable service-unit artifact 발행

### Console 경로

GitHub repository → **Actions** → `restricted-ssm-service-unit-artifact` → **Run workflow** → branch `main` 선택 → **Run workflow**

### 이 단계에서 일어나는 일

`scripts/ssm-delivery/threadia-backend.service` 한 파일을 versioned S3 object로 upload한다. host나 systemd service는 변경하지 않는다.

### 성공 증적

workflow summary에서 아래 다섯 값을 한 묶음으로 복사해 AI에게 보낸다.

```text
service_unit_key=
service_unit_version_id=
service_unit_sha256=
commit=
run=
```

### AI 판정 기준

- workflow가 `Success`여야 한다.
- key는 `releases/<40-char-main-sha>/<run>-<attempt>/threadia-backend.service` 형식이어야 한다.
- SHA-256은 64자리 소문자 hex여야 한다.
- key, VersionId, SHA-256은 같은 run의 한 묶음이어야 한다.

### 중단 조건

- `systemd-analyze verify`, mail-health sentinel, OIDC, S3 upload 중 하나라도 실패하면 host 변경 단계로 넘어가지 않는다.
- 다른 run에서 나온 key, VersionId, SHA-256을 섞지 않는다.

### Checkpoint 3. CloudFormation과 IAM/SSM version 여유 확인

이 단계는 **읽기·검토만** 한다. 아직 Change Set을 실행하지 않는다.

### IAM Console 확인

AWS Console → **IAM** → CloudFormation service role에 연결된 permissions policy를 연다.

`infra/cloudformation/restricted-ssm-delivery-foundation-service-permissions-policy.json`의 `AllowManageFixedServiceUnitMaintenanceCommandDocument` statement와 대조한다. service-unit document ARN에 대해 최소한 다음 lifecycle action이 허용돼야 한다.

- `ssm:CreateDocument`
- `ssm:DescribeDocument`
- `ssm:GetDocument`
- `ssm:UpdateDocument`
- `ssm:UpdateDocumentDefaultVersion`
- `ssm:DeleteDocument`
- `ssm:AddTagsToResource`
- `ssm:RemoveTagsFromResource`

customer-managed policy version 한도에 도달했다면 Change Set보다 먼저 승인된 절차로 오래된 **non-default** version만 정리한다. 현재 default version과 rollback에 보존하기로 한 version은 삭제하지 않는다.

### Systems Manager Console 확인

AWS Console → **Systems Manager** → **문서(Documents)** → 소유 문서에서 기존 release/helper document와 service-unit document version 상태를 확인한다. service-unit document가 아직 없다면 이는 Change Set 실행 전에는 정상이며, 기존 release/helper document의 version 여유를 확인한다.

- document version 여유가 있는지 확인한다.
- version 정리가 필요하면 default가 아닌 오래된 version만 승인 후 정리한다.
- SSM document는 직접 편집하거나 임의 문서를 실행하지 않는다.

### 중단 조건

- IAM policy에 service-unit document lifecycle 권한이 없으면 Change Set을 만들지 않는다.
- IAM policy 또는 SSM document version 여유가 없으면 cleanup 승인과 결과 확인 전까지 중단한다.

### Checkpoint 4. CloudFormation Change Set 생성·검토·실행

### Console 경로

AWS Console 상단 Region을 `아시아 태평양(서울) ap-northeast-2`로 맞춘다.

**CloudFormation** → **스택** → `threadia-restricted-ssm-delivery` 선택 → **스택 작업** → **변경 세트 생성**

AWS 한국어 Console에서는 화면에 따라 `변경 세트 생성`, `변경 사항`, `변경 세트 실행`으로 보인다. 메뉴명이 약간 달라도 스택의 Change Set 흐름을 사용한다.

### 입력 규칙

1. PR #124 `main`의 검증된 `infra/cloudformation/restricted-ssm-delivery-foundation.yaml` template을 사용한다. 기존 template 사용은 service-unit document가 빠질 수 있으므로 사용하지 않는다.
2. 기존 helper triple과 host 식별 parameter는 승인된 값을 그대로 유지한다.
3. Checkpoint 2의 동일한 artifact triple만 아래 세 parameter에 넣는다.

| ParameterKey | 입력값 |
| --- | --- |
| `ServiceUnitArtifactKey` | `service_unit_key` |
| `ServiceUnitArtifactVersionId` | `service_unit_version_id` |
| `ServiceUnitArtifactSha256` | `service_unit_sha256` |

4. IAM resource 변경 확인이 나오면 CloudFormation이 요구하는 capability acknowledgement를 검토한 뒤에만 선택한다.

### 실행 전 AI 검토 요청

Change Set 상태가 `CREATE_COMPLETE`가 되면, **변경 사항** 탭의 전체 목록과 parameter 표를 AI에게 공유한다. 실행 버튼을 누르기 전에 다음을 확인한다.

- service-unit maintenance document 추가/갱신
- GitHub OIDC deploy role에 해당 document ARN invoke 권한 추가
- exact host와 승인된 artifact triple 유지
- 예상하지 않은 bucket 교체, EC2 host 교체, OIDC subject 확대, wildcard IAM 권한, 다른 artifact triple 변경이 없음

### 실행

AI가 `실행 가능`으로 판정하고 사용자가 별도 인프라 변경 승인을 한 뒤에만 **변경 세트 실행**을 선택한다. 스택 상태가 `UPDATE_COMPLETE`가 될 때까지 기다린다.

### 중단 조건

- Change Set이 `FAILED`이거나 예상 밖 resource/IAM/OIDC/host 변경이 있으면 실행하지 않는다.
- operator role에는 Change Set 실행 권한이 없을 수 있다. 권한 오류가 나면 권한 있는 승인자가 실행해야 하며, 권한 우회는 하지 않는다.

### Checkpoint 5. SSM default version 확인 후 Environment 변수 등록

### 1) stack output 확인

CloudFormation → 해당 스택 → **출력(Outputs)** 에서 `ServiceUnitMaintenanceCommandDocumentName` 값을 확인한다.

### 2) SSM document 확인

Systems Manager → **문서(Documents)** → 위 physical document name 선택 → Default version과 document content를 확인한다.

Default version이 Checkpoint 2의 승인 artifact key, VersionId, SHA-256을 사용하는 revision인지 확인한다.

### 3) GitHub Environment 변수 등록

GitHub repository → **Settings** → **Environments** → `restricted-nonprod` → **Environment variables**에서 다음 변수를 추가하거나 갱신한다.

```text
THREADIA_SSM_SERVICE_UNIT_MAINTENANCE_DOCUMENT_NAME=<CloudFormation Output의 정확한 값>
```

값은 예상 이름이 아니라 **CloudFormation Output의 physical name을 그대로** 사용한다.

### Checkpoint 6. fixed service-unit maintenance 실행

### Console 경로

GitHub repository → **Actions** → `restricted-ssm-service-unit-maintenance` → **Run workflow** → branch `main` 선택 → **Run workflow**

### 기대 결과

- workflow가 configured document와 exact instance만 parameter 없이 호출한다.
- SSM command terminal status가 `Success`다.
- host는 pinned unit을 digest/sentinel/systemd 검증 후 atomic install한다.
- daemon reload와 restart 후 최대 120초 동안 local `/actuator/health`를 검증한다.

### 실패 시

workflow summary의 command ID와 Standard output/error 일부를 AI에게 공유한다. 같은 invoke를 임의로 반복 실행하지 않는다. 문서는 실패 시 backup restore와 rollback health를 수행하도록 설계되어 있으므로, 증적을 보존하고 Change Set 또는 승인된 이전 triple을 검토한다.

### Checkpoint 7. fresh release 재실행과 완료 판정

service-unit maintenance 성공은 unit 교체 성공만 뜻한다. backend release readiness까지 별도로 확인해야 한다.

GitHub repository → **Actions** → `restricted-ssm-release` → **Run workflow** → branch `main` 선택 → **Run workflow**

### 완료 기준

| 항목 | 성공 기준 |
| --- | --- |
| service-unit maintenance | GitHub workflow 및 SSM terminal status `Success` |
| fresh release | GitHub workflow `Success` |
| backend readiness | systemd active 및 local `/actuator/health` 성공 |
| rollback | 실패했을 경우 SSM output에서 backup restore와 rollback health 결과 확인 |
| 운영 증적 | run URL, command ID, artifact triple, Change Set 이름/상태 기록 |

## 4. 금지 사항

- `AWS-RunShellScript`나 임의 SSM document를 직접 실행하지 않는다.
- SSH, Session Manager shell, EC2에서 직접 파일 수정으로 우회하지 않는다.
- S3 object version을 지정하지 않는 artifact, 최신값 추정, artifact triple 혼용을 사용하지 않는다.
- SecureString 값, password, token, access key, RDS master credential을 화면 공유·PR·로그에 넣지 않는다.
- maintenance 실패 후 원인 확인 없이 재실행하지 않는다.

## 5. 참고 자료

- [AWS CloudFormation 스택에 대한 변경 세트 생성](https://docs.aws.amazon.com/ko_kr/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-changesets-create.html)
- [GitHub Actions 수동으로 워크플로 실행](https://docs.github.com/ko/actions/how-tos/manage-workflow-runs/manually-run-a-workflow)
- [Restricted SSM Release Runbook](../../00-project/restricted-ssm-release-runbook.md)
- [GitHub OIDC SSM EC2 배포 설계](../../05-architecture/GitHub-OIDC-SSM-EC2-배포-설계.md)
