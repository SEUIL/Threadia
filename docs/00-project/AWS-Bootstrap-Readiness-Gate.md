> **작성자**: Threadia 팀  
> **작성일**: 2026-07-13  
> **최종 수정**: 2026-07-13  
> **버전**: v1.1  
> **상태**: 작성 중  
> **태그**: aws, bootstrap, readiness, security, cost, approval

# AWS Bootstrap Readiness Gate

## 1. 목적

이 문서는 Threadia의 AWS Bootstrap을 시작하기 전에 확인해야 하는 최소 준비 조건과 승인 기준을 정의한다. 이 Gate는 AWS 계정, 결제, IAM, 네트워크, DNS 또는 배포 리소스를 생성·변경하기 위한 일반 실행 문서가 아니다. 단, 승인된 non-root Bootstrap identity 생성과 검증은 아래 기록된 예외 범위로만 수행할 수 있다.

본 세션에서는 AWS 리소스, billing, DNS, deploy mutation을 수행하지 않았다. 승인된 IAM 예외로 non-root Bootstrap identity와 해당 identity의 local credential profile을 생성·검증했다.

## 2. 적용 범위

이 Gate는 다음 준비 상태만 다룬다.

- AWS 계정 보안 및 복구 수단
- 비용 통제와 예산 알림 준비
- 사람 및 배포 주체의 identity 원칙
- credential 전달 뒤 허용되는 read-only 확인
- Bootstrap 전에 팀이 결정하거나 승인해야 하는 Architecture 항목
- Gate 통과, 보류, rollback 기준

## 3. 현재 확인 상태

다음 항목은 이 문서 작성 세션에서 검증되지 않았으며, 값이나 상태를 추정하지 않는다.

| 구분 | 항목 | 상태 |
|---|---|---|
| 계정 보안 | root MFA 활성화 여부 | 사용자 확인 |
| 계정 보안 | root access key 존재 여부 및 비활성화 상태 | 사용자 확인 |
| 계정 복구 | recovery contact 등록 및 최신성 | 사용자 확인 |
| 계정 계획 | AWS 계정 유형·소유·운영 계획 | 결정 필요 |
| 비용 | budget 및 alert 설정 상태 | 사용자 확인 |
| 비용 | 월별 또는 프로젝트별 비용 한도 | 사용자 확인 |
| 위치 | 운영 Region | 결정 필요 |
| 도메인 | domain 선택 및 소유·DNS 위임 계획 | 결정 필요 |
| Architecture | frontend hosting, backend, DB, secret, 네트워크 topology | foundation 범위만 승인 |
| 자격 증명 | 작업용 credential 전달 방식과 유효 범위 | non-root Bootstrap profile 설정·STS 검증 완료 |
| 배포 권한 | 배포 역할 및 CI/CD 역할 설계 | 결정 필요 |

계정 ID, ARN, credential, domain, Region, 비용 한도는 이 문서에 기록하지 않는다.

### 3.1 Local identity verification

- 2026-07-13에 `threadia-bootstrap` profile로 STS caller identity 응답 유효성을 확인했다.
- 확인 결과는 성공 여부만 기록했으며 account identifier, ARN, user identifier, credential 값은 출력·저장하지 않았다.
- root Console session은 non-root identity 생성·검증 후 local CLI에서 logout했다. root profile을 deploy, IaC, CI secret에 사용하지 않는다.

### 3.2 Approved non-root Bootstrap identity exception

- `ThreadiaBootstrapOperator` identity를 생성하고, attached policy와 inline policy가 없는 상태 및 access key가 하나만 존재하는 상태를 확인했다.
- 해당 identity의 local profile은 STS caller identity 응답 유효성만 검증했다. credential 값, account identifier, ARN, user identifier는 출력·저장하지 않았다.
- 이 identity에는 현재 AWS resource, IAM, billing, DNS, deploy 권한이 없다.
- 이 identity의 access key는 2026-07-20 이전에 disable 또는 delete하고, 이후 필요한 권한은 별도 승인된 role 기반 설계로 전환한다.

### 3.3 Approved RDS managed secret exception

- restricted foundation의 RDS master password는 `ManageMasterUserPassword`로 생성되는 Secrets Manager managed secret에만 보관하도록 승인됐다.
- secret value와 ARN은 template parameter, CloudFormation Output, EC2 환경 변수, Git, 문서, 로그에 기록하지 않는다.
- 이 승인에는 EC2 또는 application runtime의 secret read 권한, application configuration 주입, rotation automation이 포함되지 않는다.

## 4. 보안 Readiness

Bootstrap 승인 전 다음 조건을 책임자가 확인해야 한다.

- root 계정의 MFA 활성화 상태를 확인한다.
- root access key가 존재하지 않거나 비활성화되었는지 확인한다.
- 계정 recovery contact가 등록되어 있고 최신 상태인지 확인한다.
- 일상 작업은 root 계정이 아닌 최소 권한의 사람 또는 역할 기반 identity로 수행하는 원칙을 승인한다.
- credential은 안전한 전달 경로로만 제공하며, 문서·채팅·소스 코드·로그에 포함하지 않는다.
- 배포 권한은 개발자 개인 credential과 분리하고, 대상 환경과 작업 범위를 최소 권한으로 제한하는 역할 기반 모델을 결정한다.

위 항목은 사용자 확인 기준으로 기록했으며, 설정값이나 개인 연락처는 이 문서에 기록하지 않는다.

## 5. 비용 Readiness

비용 통제는 리소스 생성보다 먼저 승인되어야 한다.

- 계정 및 비용 책임자를 지정한다.
- 예산과 비용 alert의 설정 여부를 확인한다.
- 비용 alert의 수신자와 대응 책임자를 결정한다.
- Bootstrap 및 초기 운영에 적용할 비용 한도를 팀 승인으로 결정한다.
- 비용 한도 초과 또는 예측 초과 시 리소스 생성·확장을 중지하고 검토하는 절차를 합의한다.

예산, alert, 비용 한도는 사용자 확인 기준으로 기록했으며, 실제 값과 수신자 정보는 이 문서에 기록하지 않는다.

## 6. Identity 및 credential 원칙

AWS credential이 전달되기 전에는 AWS CLI 또는 SDK를 사용한 확인을 수행하지 않는다. credential이 전달된 뒤 이 Gate에서 허용하는 기본 확인은 아래 명령 하나로 제한한다.

```bash
aws sts get-caller-identity
```

이 명령은 caller identity 확인 목적의 read-only 검증으로만 사용한다. 실행 결과의 JSON 전체, account identifier, user identifier, ARN 등 식별자는 출력·저장·문서화·공유하지 않는다. 검증 기록에는 성공 또는 실패 여부와 실행 시각만 남긴다.

다음 행위는 3.2의 승인된 identity 생성 예외 외에는 이 Gate 범위에서 금지한다.

- access key, secret key, session token 또는 profile 내용을 출력·기록·공유하는 행위
- IAM user, role, policy, access key, permission boundary를 생성·변경·삭제하는 행위
- 배포 credential 또는 GitHub OIDC trust relationship을 생성·변경하는 행위
- 계정, organization, billing, support plan 설정을 생성·변경하는 행위

## 7. Open Architecture Decisions

다음 Architecture는 향후 검토 대상일 뿐, 이 문서로 승인되거나 확정되지 않는다.

| 결정 항목 | 현재 상태 | Bootstrap 전 필요한 승인 |
|---|---|---|
| Route 53 및 custom domain | 미결정 | domain 선택, 소유권, DNS 운영 책임 |
| ACM 및 HTTPS 종료 위치 | 미결정 | 인증서 발급·갱신 책임과 TLS termination 위치 |
| frontend hosting | 미결정 | hosting 방식과 배포·rollback 책임 |
| backend topology | 미결정 | EC2 등 실행 환경, 네트워크 경계, 운영 책임 |
| RDS 및 데이터 저장소 | 미결정 | DB 엔진·접근 경로·backup·복구 기준 |
| secrets 관리 | 미결정 | secret 저장 위치, 접근 주체, rotation 책임 |
| GitHub OIDC 기반 배포 | 미결정 | trust 정책, 대상 환경, 최소 권한 배포 역할 |
| 운영 Region | 미결정 | 선택 근거, 데이터 위치, 비용·지연시간 영향 |

Route 53, ACM, frontend hosting, EC2, RDS, secrets, GitHub OIDC는 미래 제안이며, 이 Gate에서 리소스 생성이나 구성 변경을 허용하지 않는다.

## 8. Bootstrap identity lifecycle

Bootstrap identity를 생성하는 경우에도 다음 경계를 사전에 승인해야 한다.

- root identity는 Bootstrap identity 생성과 검증에 필요한 최소 작업 외에는 사용하지 않는다.
- Bootstrap identity는 단일 계정과 명시된 change window에만 사용하고, 짧은 session 또는 승인된 만료 기준을 적용한다.
- Bootstrap identity는 CI/CD deploy role, 애플리케이션 runtime role, 운영자 상시 administrator identity와 분리한다.
- Bootstrap identity를 PR-triggered workflow나 일반 deploy workflow에 연결하지 않는다.
- 권한 범위를 넓힐 수 있는 policy 생성·연결·version 변경, trust policy 변경, access key 생성, OIDC provider 변경, role chaining, `iam:PassRole`는 별도 승인 없이는 허용하지 않는다.
- IAM control-plane action 중 ARN 기준 resource scope가 불가능한 항목은 permissions boundary와 condition을 함께 적용하는 설계 검토가 필요하다.
- Bootstrap identity의 credential 전달 방식, session duration, revoke 절차, 종료 책임자를 기록하고 승인한다.

현재 non-root Bootstrap identity는 3.2의 승인된 예외로 생성됐으며, 이 문서는 추가 IAM identity·policy·role·OIDC 생성 권한을 부여하지 않는다.

## 9. 제외 범위

다음은 이 문서의 범위 밖이며 별도 설계와 승인이 필요하다.

- AWS 계정 생성 또는 ownership 이전
- IAM, billing, DNS, network, security group, logging, monitoring 구성
- Route 53 hosted zone, record, domain delegation 변경
- ACM certificate 요청 또는 검증
- frontend hosting, EC2, RDS, S3, secret 저장소 등 리소스 생성·변경
- CI/CD workflow, GitHub OIDC provider, 배포 역할 생성·변경
- 실제 배포, 데이터 migration, production traffic 전환

## 10. 승인 Gate

Bootstrap을 진행하려면 다음 승인이 모두 기록되어야 한다.

| 승인 대상 | 승인 기준 |
|---|---|
| 계정 보안 책임자 | root MFA, root key 상태, recovery contact 확인 완료 |
| 비용 책임자 | budget·alert 확인 및 비용 한도 승인 |
| Architecture 책임자 | Region, domain, topology, secrets, 배포 역할 방향 승인 |
| 배포 책임자 | credential 전달 방식과 최소 권한 배포 역할 승인 |
| 팀 | 실제 AWS mutation을 수행할 변경 단위와 rollback 계획 승인 |

승인되지 않은 항목이 하나라도 있으면 Gate는 통과하지 않는다. 승인 기록은 민감정보와 식별자를 포함하지 않는 위치에 보관한다.

## 11. Acceptance Criteria

다음 조건을 모두 충족할 때 AWS Bootstrap Readiness Gate를 통과로 판단한다.

- root MFA, root key 상태, recovery contact가 책임자 확인을 받았다.
- 계정 계획, 비용 책임자, budget·alert, 비용 한도가 승인되었다.
- Region, domain, topology, secrets, credential 전달 방식, 배포 역할의 결정 또는 보류 사유가 기록되고 승인되었다.
- Bootstrap identity를 생성하는 경우 목적, lifecycle, 권한 확장 차단, CI/runtime 분리, revoke 책임이 승인되었다.
- credential 전달 뒤 필요 시 `aws sts get-caller-identity`만 실행했으며, identity JSON이나 식별자를 출력·저장하지 않았다.
- 3.2에 기록된 non-root Bootstrap identity 및 local profile 생성 외 AWS mutation은 수행되지 않았다.
- 미결정 Architecture가 배포 확정으로 오인되지 않도록 미래 제안 상태로 유지되었다.

## 12. Rollback 및 중단 기준

이 Gate 단계에서는 AWS mutation을 수행하지 않았으므로 AWS 리소스 rollback 대상이 없다. 다만 다음 상황에서는 Bootstrap 진행을 즉시 중단하고 승인 상태로 되돌린다.

- root MFA, root key 상태 또는 recovery contact를 확인할 수 없는 경우
- 비용 책임자, budget·alert, 비용 한도를 승인하지 못한 경우
- credential이 안전한 경로 외에서 노출되었거나 노출 가능성이 있는 경우
- Region, domain, topology, secrets 또는 배포 역할의 책임과 승인 범위가 불명확한 경우
- 허용 범위를 넘어서는 AWS CLI·SDK 호출 또는 AWS mutation이 발생하려는 경우

중단 시에는 추가 AWS 작업을 실행하지 않고, 노출 가능 credential은 해당 책임자의 보안 절차에 따라 폐기·교체 여부를 판단한다. 재개는 누락된 확인과 승인을 완료한 뒤 별도 승인으로만 가능하다.
