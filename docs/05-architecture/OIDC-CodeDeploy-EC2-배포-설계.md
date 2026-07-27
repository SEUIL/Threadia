> **작성자**: Threadia 팀  
> **작성일**: 2026-07-13  
> **최종 수정**: 2026-07-14  
> **버전**: v0.2  
> **상태**: 검토 중  
> **태그**: architecture, aws, github-actions, oidc, codedeploy, ec2, s3

# GitHub OIDC, S3 Artifact, CodeDeploy-EC2 배포 설계

## 1. 목적과 권한 경계

이 문서는 Threadia backend를 GitHub Actions OIDC, private S3 artifact bucket, AWS CodeDeploy, EC2로 배포하기 위한 설계 초안이다. 선택된 배포 방향은 OIDC와 CodeDeploy 조합이며, 이 문서는 검토와 승인 게이트의 입력이다. 이 문서만으로 AWS resource, IAM trust 또는 policy, GitHub Environment, workflow, CodeDeploy application 또는 deployment group, EC2 host 설정을 생성·변경하거나 실제 배포할 권한은 생기지 않는다.

현재 restricted foundation은 아직 실행되지 않았고 의도적으로 GitHub OIDC, CodeDeploy resource와 agent, application `UserData`를 포함하지 않는다. 또한 foundation policy guardrail은 CodeDeploy와 runtime secret read를 명시적으로 거부한다. 따라서 delivery 구성은 현재 foundation 위에 즉시 추가할 수 없으며, stack 생성 전에 별도 reviewed foundation revision으로 설계·review·승인되어야 한다.

이 문서는 deploy readiness 또는 production readiness를 주장하지 않는다. endpoint, domain, CORS, OAuth, secret 주입, migration, rollback 책임이 확정되고 각 후속 구현이 검증되기 전까지 배포는 hold 상태다.

## 2. 전제와 목표 구조

backend는 실행 가능한 Spring Boot JAR이며 표준 health check는 `/actuator/health`다. host model은 Docker가 아닌 JAR와 `systemd`를 기본안으로 한다. frontend의 `NEXT_PUBLIC_API_URL`은 build-time public configuration이므로 backend deploy만으로 browser API URL, cookie, CORS 또는 OAuth redirect가 갱신되지 않는다.

```text
승인된 GitHub Environment의 deploy workflow
  └─ GitHub OIDC token
      └─ GitHub OIDC deploy role
          ├─ immutable JAR + manifest + checksum을 private S3 artifact bucket에 기록
          └─ CodeDeploy deployment 생성
              └─ CodeDeploy service role
                  └─ 승인된 EC2 deployment group으로 명령
                      └─ EC2 instance profile + CodeDeploy agent
                          └─ private S3 artifact를 받아 JAR를 교체하고 systemd를 제어
                              └─ Spring Boot JAR → /actuator/health → RDS
```

S3 artifact, CodeDeploy control plane, EC2 runtime, RDS credential은 각기 다른 trust와 권한 경계를 가진다. 사람의 SSH, long-lived AWS access key, EC2 SSH private key를 deployment 경로에 사용하지 않는다. 운영 점검이 필요하면 별도 승인된 SSM 기반 절차를 사용하며, 그것도 deploy 권한과 분리한다.

## 3. 역할 분리와 최소 권한 원칙

세 역할은 반드시 독립된 IAM role 또는 instance profile로 유지한다. 역할을 합치거나 역할 간 `sts:AssumeRole`을 일반 경로로 열지 않는다.

| 주체 | 신뢰 주체와 용도 | 허용 범위 | 금지 범위 |
|---|---|---|---|
| GitHub OIDC deploy role | GitHub Actions OIDC provider가 승인된 repository, Environment, ref 조건에서 assume한다 | artifact bucket의 지정 prefix에 artifact와 manifest 기록, CodeDeploy의 지정 application/deployment group에 deployment 생성·조회, 필요한 최소 읽기 | EC2 직접 접속·명령 실행, application runtime secret read, RDS 접근, broad IAM 관리, 다른 Environment 배포 |
| CodeDeploy service role | `codedeploy.amazonaws.com` service principal만 assume한다 | 지정 deployment group에서 EC2 deployment orchestration에 필요한 CodeDeploy API와 대상 조회 | GitHub OIDC trust, artifact publish, application secret read, 사람·workflow 직접 assume |
| EC2 instance profile | `ec2.amazonaws.com`만 assume한다 | CodeDeploy agent가 지정 artifact object와 checksum/manifest를 읽고 lifecycle script가 필요한 host-local `systemd` 작업을 수행한다 | GitHub OIDC assume, CodeDeploy deployment 생성, RDS master secret read, 장기 access key 사용 |

RDS `ManageMasterUserPassword`가 만드는 master secret은 application runtime에 제공해서는 안 된다. application DB credential은 master와 분리한 least-privilege DB user를 사용할지, 저장소와 rotation을 어떻게 운영할지 결정이 필수다. 해당 결정이 없으면 runtime secret read 권한, secret injection 방식, application startup을 구현하지 않는다.

## 4. GitHub Environment와 OIDC trust 제약

deploy workflow는 승인된 GitHub Environment `restricted-nonprod`를 반드시 참조한다. 이 Environment는 one-month personal portfolio 예외로 required reviewer를 의도적으로 설정하지 않는다. 대신 GitHub UI에서 정확히 하나의 custom deployment branch policy `main`을 설정하고, `custom_branch_policies=true`, `protected_branches=false`를 유지한다. `main` 외 branch 또는 tag 정책을 추가하지 않는다. GitHub Actions job에는 OIDC token 발급에 필요한 최소 permission인 `id-token: write`와 source checkout에 필요한 `contents: read`만 둔다. GitHub secret에 AWS access key를 저장하지 않는다.

AWS IAM trust policy의 조건은 다음 exact claim 제약을 사용한다. 실제 organization, repository, Environment, ref 값은 승인 후에만 치환하며 이 문서에 값으로 기록하지 않는다.

| claim | 요구 조건 | 이유 |
|---|---|---|
| `token.actions.githubusercontent.com:aud` | `sts.amazonaws.com`과 정확히 일치해야 한다 | AWS STS용으로 발급된 token만 허용한다 |
| `token.actions.githubusercontent.com:sub` | `repo:<OWNER>/<REPOSITORY>:environment:<ENVIRONMENT>`과 정확히 일치해야 한다 | repository 전체 wildcard, organization wildcard, pull request subject, branch-only subject를 배제하고 승인된 Environment로 좁힌다 |

Environment를 사용하는 경우 GitHub OIDC `sub`는 Environment subject를 사용하므로 ref를 `sub`에 함께 붙여 추측하지 않는다. Environment의 custom deployment branch policy는 eligible ref를 `main`으로 제한하는 GitHub 설정이고, OIDC Environment subject는 IAM trust의 exact identity 조건이다. 두 경계를 섞지 않으며 OIDC `sub`에 branch condition을 추가하지 않는다. exact subject를 정하기 전에는 `repo:<OWNER>/<REPOSITORY>:*`, `repo:<OWNER>/*:*`, `StringLike` wildcard 또는 account-wide principal을 trust 조건으로 사용하지 않는다.

## 5. Artifact 계약: private, versioned, encrypted S3

artifact storage는 application data와 분리된 dedicated private S3 bucket으로 둔다. bucket은 versioning과 default encryption을 활성화하고 public access를 전면 차단한다. bucket policy는 TLS 외 접근을 거부하고 GitHub OIDC deploy role의 승인된 write prefix와 EC2 instance profile의 승인된 read prefix만 허용한다. retention과 noncurrent version lifecycle은 승인된 기간을 적용하며, 임의의 delete 권한이나 public URL을 제공하지 않는다.

release artifact는 mutable filename이나 latest pointer가 아니라 immutable identity로 추적한다. 최소 구성은 아래와 같다.

| 항목 | 계약 |
|---|---|
| release identity | immutable Git commit SHA 또는 승인된 immutable tag와 build run identifier를 함께 기록한다 |
| payload | backend JAR와 CodeDeploy revision에 필요한 manifest를 하나의 versioned revision으로 보관한다 |
| integrity | SHA-256 checksum을 manifest에 기록하고 upload 전후 및 host download 후 동일성을 검증한다 |
| metadata | source commit, build workflow run, JAR filename, checksum, 생성 시각만 기록하며 secret은 기록하지 않는다 |
| promotion | CodeDeploy deployment는 manifest가 가리키는 exact S3 object version과 checksum만 사용하며, latest alias 재사용을 금지한다 |

artifact identity 또는 checksum 검증이 실패하면 lifecycle을 중단하고 deployment를 실패로 처리한다. checksum을 다시 계산해 manifest를 덮어쓰거나 다른 artifact로 대체하지 않는다.

## 6. EC2 JAR와 systemd host model

EC2에는 CodeDeploy agent와 별도 systemd service를 운영한다. agent 설치, JAR directory ownership, service account, runtime environment file, network ingress, SSM 운영 경로는 foundation revision에서 명시적으로 review한다. application JAR는 root 소유의 임시 위치에 풀지 않고, 비대화형 전용 OS user가 읽을 수 있는 versioned release directory에 둔다.

배포 lifecycle은 신규 release를 완전히 준비하고 checksum을 검증한 뒤에만 active release를 전환한다. `systemctl`로 Spring Boot service를 restart하고, service start 실패 또는 health check 실패 시 이전에 검증된 release로 자동 복귀할 수 있는 host-local 전환 경계를 둔다. 이를 위해 이전 release와 그 manifest/checksum은 S3 retention 및 host cleanup 정책에서 보존 기간을 맞춰야 한다.

runtime environment는 process environment를 표준 전달 경로로 사용한다. `.env`, GitHub Actions log, JAR, manifest, S3 object metadata에 secret 값을 넣지 않는다. `NEXT_PUBLIC_API_URL`은 frontend bundle에 포함되는 값이므로 backend host 환경 변수로 해결할 수 없다.

## 7. CodeDeploy lifecycle과 검증

후속 구현에서 `appspec`은 이 설계를 따르는 별도 산출물로 review한다. 이 문서는 appspec 파일을 생성하지 않는다. EC2/On-Premises lifecycle은 아래의 의도를 충족해야 한다.

| lifecycle 단계 | 설계 책임 |
|---|---|
| `ApplicationStop` | 기존 systemd service를 정상 중지한다. 최초 배포처럼 service가 없을 수 있는 경우를 명시적으로 처리한다 |
| `BeforeInstall` | versioned release directory와 권한을 준비하고 불완전한 이전 staging 파일을 정리한다 |
| `Install` | immutable revision의 JAR와 manifest를 배치하되 active release를 즉시 교체하지 않는다 |
| `AfterInstall` | JAR SHA-256을 manifest와 대조하고 runtime config 존재 여부만 검증한다. secret 값은 출력하지 않는다 |
| `ApplicationStart` | checksum 검증된 release를 active release로 전환하고 systemd service를 시작한다 |
| `ValidateService` | `/actuator/health`에 local request를 수행하여 HTTP 성공과 application health를 확인한다. 실패 시 lifecycle을 실패로 반환한다 |

CodeDeploy deployment configuration은 traffic model과 availability 요구를 결정한 뒤 선택한다. 단일 EC2이면 in-place deployment 중 service interruption이 발생할 수 있으므로 downtime 허용 여부와 deployment config를 별도 승인한다. automatic rollback은 deployment failure, CloudWatch alarm 또는 승인된 stop 조건에 대해 활성화하고, rollback target은 직전 성공 revision으로 고정한다.

automatic rollback은 application artifact와 service version만 되돌릴 수 있으며 DB schema 또는 data migration을 자동으로 되돌리지 못한다. migration이 포함된 release에 artifact rollback만 적용하면 이전 JAR가 새 schema와 호환되지 않을 수 있다. 이 제한은 deployment failure를 성공으로 해석하지 않는 필수 경계다.

## 8. DB migration 정책 경계

DB migration은 CodeDeploy hook에 암묵적으로 포함하지 않는다. migration owner, 실행 주체, 실행 시점, idempotency, backup 또는 restore 책임, forward-fix와 rollback 가능성은 별도 승인한다. destructive 또는 비가역 migration은 application artifact rollout과 독립된 change gate로 검토한다.

기본 정책은 backward-compatible expand/contract migration을 우선하고, application rollout 전후 호환 기간을 둔다. migration 실패 시 CodeDeploy automatic rollback이 DB를 복원한다는 전제를 두지 않는다. RDS master credential을 migration이나 application runtime에 재사용하지 않는다.

## 9. 구현 전 승인 및 결정 목록

아래 항목은 AWS 또는 GitHub mutation 전에 모두 명시적으로 승인·기록해야 한다. 값, account ID, ARN, secret은 이 문서나 repository에 기록하지 않는다.

| 승인 또는 결정 | 필요한 내용 |
|---|---|
| GitHub Environment와 ref | `restricted-nonprod`, required reviewer 의도적 미설정, 정확히 하나의 custom deployment branch policy `main`, `custom_branch_policies=true`, `protected_branches=false`, 만료일 `2026-08-14`, deploy trigger owner: 개인 포트폴리오 owner, 재승인 owner: GitHub repository owner |
| AWS account와 Region | target account, Region, 비용·운영 책임, restricted foundation과의 연결 경계 |
| exact OIDC subject | `repo:<OWNER>/<REPOSITORY>:environment:<ENVIRONMENT>`의 실제 exact 값과 `aud=sts.amazonaws.com` trust 검토 |
| CodeDeploy deployment config | in-place 또는 다른 승인 모델, instance 수, downtime 허용 여부, automatic rollback event와 alarm |
| S3 retention | version 보존 기간, noncurrent version lifecycle, host의 이전 release 보존 수, 삭제 책임 |
| secret store와 application DB user | secret store, injection 주체, runtime read policy, master와 분리된 application DB user 및 rotation 책임 |
| migration owner와 rollback | migration 실행 책임자, compatibility 전략, backup/restore 또는 forward-fix 기준, failure 대응 |
| endpoint, domain, CORS, OAuth | canonical endpoint/domain, TLS termination, frontend build-time API URL, CORS origin, cookie policy, OAuth redirect URI |

위 결정 중 하나라도 미확정이면 해당 영역의 IAM, workflow, CodeDeploy, EC2, S3 또는 secret 변경을 시작하지 않는다. 특히 현재 foundation의 explicit deny를 현장에서 완화하거나 policy를 넓혀 delivery를 우회하지 않는다.

required reviewer 부재 예외는 `2026-08-14`에 만료된다. 만료 전 연장하거나 만료 후 재사용하려면 GitHub repository owner가 기간, reviewer 설정, branch policy와 deploy trigger owner를 다시 명시 승인해야 한다. 재승인 전에는 Environment 설정을 유지·확장하거나 배포를 trigger하지 않는다.

## 10. 단계별 후속 설계·구현 Gate

| 단계 | 산출물과 검토 기준 | 다음 단계 진입 조건 |
|---|---|---|
| 0. hold 유지 | 본 문서 검토, 9절 결정·승인 기록 | 모든 승인 항목의 책임자와 값 외 결정이 확정된다 |
| 1. delivery foundation revision | OIDC provider, 세 역할 분리, artifact bucket, CodeDeploy/agent/host 준비, runtime secret 경계의 IaC 설계 | current restricted foundation과 분리된 revision의 independent review와 Change Set 검토가 완료된다 |
| 2. GitHub delivery 설계 | Environment protection, exact OIDC trust, immutable artifact/manifest/checksum, deployment trigger와 evidence | GitHub settings와 workflow 변경에 대한 별도 review가 완료된다 |
| 3. host와 lifecycle 구현 | JAR/systemd, lifecycle hook, health validation, rollback behavior, observability와 access 경계 | staging-like 환경에서 checksum, `/actuator/health`, failure rollback을 검증한다 |
| 4. migration과 endpoint integration | migration policy 실행 검증, frontend build-time URL, CORS/cookie/OAuth endpoint alignment | application, DB, frontend owner가 compatibility와 rollback limitation을 승인한다 |
| 5. release gate | exact revision, deployment config, alarms, artifact retention, rollback owner와 evidence review | 별도 release 승인 후에만 실제 deployment를 검토한다 |

## 11. 관련 문서과 공식 참고 자료

### 관련 Threadia 문서

- [`AWS-배포-아키텍처-v1.md`](./AWS-배포-아키텍처-v1.md): AWS deployment proposal과 GitHub OIDC 후속 방향이다.
- [`배포-환경-런타임-계약.md`](./배포-환경-런타임-계약.md): process environment, frontend build-time configuration, runtime secret 경계다.
- [`../../infra/cloudformation/README.md`](../../infra/cloudformation/README.md): 현재 restricted foundation의 명시적 제외 범위와 guardrail이다.
- [`../00-project/AWS-Bootstrap-Readiness-Gate.md`](../00-project/AWS-Bootstrap-Readiness-Gate.md): AWS mutation 전 readiness와 승인 경계다.

### 공식 참고 자료

- [GitHub Docs — Configuring OpenID Connect in Amazon Web Services](https://docs.github.com/en/actions/how-tos/secure-your-work/security-harden-deployments/oidc-in-aws)
- [GitHub Docs — OpenID Connect reference](https://docs.github.com/en/actions/reference/openid-connect-reference)
- [AWS IAM User Guide — Configuring a role for GitHub OIDC identity provider](https://docs.aws.amazon.com/IAM/latest/UserGuide/id_roles_providers_create_oidc_reps.html)
- [AWS CodeDeploy User Guide — IAM service role for CodeDeploy](https://docs.aws.amazon.com/codedeploy/latest/userguide/getting-started-create-service-role.html)
- [AWS CodeDeploy User Guide — AppSpec file reference for EC2/On-Premises deployments](https://docs.aws.amazon.com/codedeploy/latest/userguide/reference-appspec-file.html)
- [AWS CodeDeploy User Guide — Roll back deployments](https://docs.aws.amazon.com/codedeploy/latest/userguide/deployments-rollback-and-redeploy.html)
- [AWS S3 User Guide — Using versioning in S3 buckets](https://docs.aws.amazon.com/AmazonS3/latest/userguide/Versioning.html)
- [AWS S3 User Guide — Protecting data with server-side encryption](https://docs.aws.amazon.com/AmazonS3/latest/userguide/serv-side-encryption.html)
