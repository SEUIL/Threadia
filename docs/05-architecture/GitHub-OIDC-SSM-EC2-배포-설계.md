> **작성자**: Threadia 팀
> **작성일**: 2026-07-14
> **최종 수정**: 2026-07-17
> **버전**: v0.7
> **상태**: 검토 중
> **태그**: aws, github-actions, oidc, ssm, s3, ec2, parameter-store

# GitHub OIDC, SSM Command, EC2 배포 설계

## 1. 목표 구조

신규 active path는 `main` push 또는 `main` ref의 수동 dispatch에서만 GitHub Actions job이 실행되어 `restricted-nonprod` Environment OIDC subject로 role을 assume하고, versioned private S3 object를 기록한 뒤 고정 SSM Command document를 정확히 한 대의 private EC2에 보낸다. job-level `refs/heads/main` guard가 다른 ref의 수동 dispatch를 차단하고, 고정 concurrency group은 실행 중인 release를 취소하지 않은 채 후속 release를 직렬화한다. EC2는 S3 Gateway Endpoint와 SSM interface endpoint를 사용하며 NAT, public IP, public ingress, SSH, CodeDeploy를 사용하지 않는다. 승인된 별도 mail network gate는 dedicated SES SMTP Interface Endpoint의 private DNS와 EC2↔endpoint TCP/587 security group 규칙만 추가하며, SMTP credential이나 `MAIL_*` runtime delivery는 포함하지 않는다.

```text
main → restricted-nonprod Environment OIDC → GitHub deploy role
  → private versioned SSE-S3 release object → fixed SSM Command
  → DeliveryHost EC2 → root-owned release helper → systemd → private RDS
```

OIDC trust는 `aud=sts.amazonaws.com` 및 `repo:<OWNER>/<REPOSITORY>:environment:restricted-nonprod` 두 exact claim만 허용한다. Environment는 한 달 개인 포트폴리오 용도로 custom branch policy를 `main` 하나만 두고 required reviewer는 의도적으로 두지 않는다. Environment subject에는 branch claim을 추가하지 않으며 branch 제한은 GitHub Environment 설정과 workflow의 job-level main ref guard가 함께 담당한다.

## 2. Artifact와 command 경계

artifact key는 commit SHA, workflow run ID, attempt를 포함한다. workflow는 S3가 반환한 VersionId, archive SHA-256, release ID만 fixed document에 전달하고 모든 AWS CLI release operation에 workflow Region을 명시한다. document는 shell text나 경로, bucket, instance, service name, Region을 입력으로 받지 않으며 stack Region을 `AWS_REGION`과 `AWS_DEFAULT_REGION`으로 helper 직전에 고정 주입한다. fixed command는 workflow polling보다 짧은 540초로 제한한다. helper는 주입된 `AWS_REGION`을 검증하고 S3 artifact read와 SSM Parameter Store read에 명시하며 host-level exclusive lock을 먼저 획득해 동시 activation을 거부한다. `ssm:GetCommandInvocation`은 command invocation status 읽기에 AWS가 `Resource: *`를 요구하므로 GitHub role의 유일한 wildcard read action이며, workflow는 이 polling read에 bounded CLI connect/read timeout을 적용한다.

EC2 S3 read는 release prefix와 Network stack이 export한 exact S3 Gateway Endpoint ID를 모두 요구한다. Network endpoint policy도 account/region-derived artifact bucket의 `releases/*`에 대한 `GetObject`와 `GetObjectVersion`만 별도로 허용한다. 이 endpoint policy는 GitHub-hosted upload에 적용되지 않으며, bucket policy에 `aws:SourceVpce` 기반 Deny를 두지 않는다. S3 prefix list ID는 CloudFormation이 동적으로 안전하게 조회할 수 없으므로 approved regional ID를 validation하는 non-secret parameter로 제공한다. 이를 `0.0.0.0/0` egress로 대체하지 않는다.

## 3. Runtime SecureString 계약

platform owner는 아래 exact Parameter Store `SecureString`을 release 전에 생성하고 rotation 책임을 가진다. template, workflow, manifest, log, repository에는 값과 RDS master secret을 넣지 않는다.

- `/threadia/restricted-nonprod/runtime/THREADIA_DB_URL`
- `/threadia/restricted-nonprod/runtime/THREADIA_DB_USERNAME`
- `/threadia/restricted-nonprod/runtime/THREADIA_DB_PASSWORD`
- `/threadia/restricted-nonprod/runtime/THREADIA_JWT_SECRET`
- `/threadia/restricted-nonprod/runtime/THREADIA_COOKIE_SECURE`
- `/threadia/restricted-nonprod/runtime/THREADIA_CORS_ALLOWED_ORIGIN`

EC2 instance role은 위 ARN의 `ssm:GetParameter(s)`만 받을 수 있으며 `GetParametersByPath`, Secrets Manager, RDS master credential read를 받지 않는다. DB 연결에는 RDS master와 분리된 최소 권한 application DB user가 사전에 존재해야 한다. DB migration은 이 release path에 포함하지 않는다.

이 six-key restriction에는 `THREADIA_PLATFORM_ADMIN_BOOTSTRAP_EMAIL`이 포함되지 않는다. 따라서 현재 active path는 bootstrap email을 주입할 수 없다. `platform_admins`가 비어 있는 첫 배포는 이미 platform admin이 존재하거나, matching ACTIVE user용 해당 키를 제공하는 별도 승인된 runtime delivery contract, IAM, helper 변경이 있기 전까지 No-Go다.

## 4. Host release 계약

workflow는 pinned action SHA를 사용하고 setup-java가 제공한 Linux x86_64 runtime을 executable bit 보존 방식으로 release archive에 포함한다. AMI에 Java를 설치하지 않는다. host는 SSM Agent, AWS CLI, Python 3, tar, curl, systemd, `flock`, `threadia` user, `/usr/local/lib/threadia-release/apply-release`, `threadia-backend.service`만 사전에 갖춰야 한다. `bootstrap-host.sh`는 이 reviewed local artifact만 root-owned path에 설치하며 package manager 또는 network access를 사용하지 않는다. helper는 host-level exclusive lock을 획득한 뒤 bundled runtime이 Linux x86_64 host에서 실행되는지, immutable archive와 JAR checksum이 맞는지 확인하고 새 release directory를 만든 뒤 runtime env와 active symlink를 atomic하게 교체한다. systemd는 activated release의 absolute runtime path만 실행하고 restricted runtime에서만 mail health를 `MANAGEMENT_HEALTH_MAIL_ENABLED=false`로 비활성화한다. global backend application properties는 변경하지 않는다. restart 후 helper는 약 120초 동안 service active 상태와 local `/actuator/health`를 polling한다. service가 inactive가 되거나 readiness timeout이 발생하면 직전 runtime env와 active symlink를 함께 복구하고 이전 service를 restart하며, 이전 상태가 없으면 새 env와 symlink를 삭제하고 service를 중지한다.

한 대의 EC2 restart에 따른 짧은 downtime은 이 개인 포트폴리오 환경에서 허용한다. release는 DB migration을 실행하지 않으며 application artifact와 해당 release가 교체한 runtime env만 rollback한다.

## 5. Legacy와 승인 경계

기존 `delivery-foundation*` CodeDeploy source는 기록 보존을 위해 유지하지만 신규 active path에서 사용하지 않는다. Network Change Set은 endpoint route/egress/policy만, SSM delivery Change Set은 deterministic artifact bucket, exact OIDC subject, fixed document와 runtime policy만 검토한다. SES SMTP endpoint를 추가할 때는 UPDATE Change Set에서 pre-existing resource의 Modify/Remove 또는 Replacement `True`/`Conditional`을 hard reject한다. 승인 가능한 action은 `SesSmtpEndpointSecurityGroup`, `SesSmtpEndpointIngress`, `Ec2SmtpToSesEndpoint`, `SesSmtpEndpoint`의 신규 Add뿐이며 그 밖의 action은 실행하지 않는다. delivery operator는 Change Set create/describe/delete와 exact CloudFormation role pass만 허용하며 Execute 권한이 없다. 두 승인 window는 분리한다. 이 repository revision은 AWS/GitHub resource, role, parameter, endpoint, stack, DB user를 생성하거나 변경하지 않는다.

## 변경 이력

| 날짜 | 내용 |
|---|---|
| 2026-07-17 | SES SMTP PrivateLink의 private DNS 및 TCP/587 전용 network gate를 승인된 endpoint 집합에 추가했다. UPDATE Change Set에서 pre-existing resource Modify/Remove, Replacement `True`/`Conditional`을 hard reject하고 네 SES logical resource의 신규 Add만 승인하며 SMTP credential과 `MAIL_*` runtime delivery는 후속 gate로 유지했다. |
| 2026-07-16 | active restricted SSM path의 six-key 제한, bootstrap email 미전달, `platform_admins`가 비어 있는 첫 배포의 No-Go 경계를 명시했다. |
