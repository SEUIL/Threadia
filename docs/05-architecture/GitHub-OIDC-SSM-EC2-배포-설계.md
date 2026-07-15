> **작성자**: Threadia 팀
> **작성일**: 2026-07-14
> **최종 수정**: 2026-07-14
> **버전**: v0.2
> **상태**: 검토 중
> **태그**: aws, github-actions, oidc, ssm, s3, ec2, parameter-store

# GitHub OIDC, SSM Command, EC2 배포 설계

## 1. 목표 구조

신규 active path는 `main`의 GitHub Actions job이 `restricted-nonprod` Environment OIDC subject로 role을 assume하고, versioned private S3 object를 기록한 뒤 고정 SSM Command document를 정확히 한 대의 private EC2에 보낸다. EC2는 S3 Gateway Endpoint와 SSM interface endpoint만 사용하며 NAT, public IP, public ingress, SSH, CodeDeploy를 사용하지 않는다.

```text
main → restricted-nonprod Environment OIDC → GitHub deploy role
  → private versioned SSE-S3 release object → fixed SSM Command
  → DeliveryHost EC2 → root-owned release helper → systemd → private RDS
```

OIDC trust는 `aud=sts.amazonaws.com` 및 `repo:<OWNER>/<REPOSITORY>:environment:restricted-nonprod` 두 exact claim만 허용한다. Environment는 한 달 개인 포트폴리오 용도로 custom branch policy를 `main` 하나만 두고 required reviewer는 의도적으로 두지 않는다. Environment subject에는 branch claim을 추가하지 않으며 branch 제한은 GitHub Environment 설정의 책임이다.

## 2. Artifact와 command 경계

artifact key는 commit SHA, workflow run ID, attempt를 포함한다. workflow는 S3가 반환한 VersionId, archive SHA-256, release ID만 fixed document에 전달한다. document는 shell text나 경로, bucket, instance, service name을 입력으로 받지 않는다. `ssm:GetCommandInvocation`은 command invocation status 읽기에 AWS가 `Resource: *`를 요구하므로 GitHub role의 유일한 wildcard read action이다.

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

## 4. Host release 계약

workflow는 pinned action SHA를 사용하고 setup-java가 제공한 Linux x86_64 runtime을 executable bit 보존 방식으로 release archive에 포함한다. AMI에 Java를 설치하지 않는다. host는 SSM Agent, AWS CLI, Python 3, tar, curl, systemd, `threadia` user, `/usr/local/lib/threadia-release/apply-release`, `threadia-backend.service`만 사전에 갖춰야 한다. `bootstrap-host.sh`는 이 reviewed local artifact만 root-owned path에 설치하며 package manager 또는 network access를 사용하지 않는다. helper는 bundled runtime이 Linux x86_64 host에서 실행되는지, immutable archive와 JAR checksum이 맞는지 확인하고 새 release directory를 만든 뒤 active symlink를 atomic하게 전환한다. systemd는 activated release의 absolute runtime path만 실행한다. restart 또는 local `/actuator/health` 실패 시 직전 local release로 복구한다.

한 대의 EC2 restart에 따른 짧은 downtime은 이 개인 포트폴리오 환경에서 허용한다. release는 DB migration을 실행하지 않으며 application artifact만 rollback한다.

## 5. Legacy와 승인 경계

기존 `delivery-foundation*` CodeDeploy source는 기록 보존을 위해 유지하지만 신규 active path에서 사용하지 않는다. Network Change Set은 endpoint route/egress/policy만, SSM delivery Change Set은 deterministic artifact bucket, exact OIDC subject, fixed document와 runtime policy만 검토한다. delivery operator는 Change Set create/describe/delete와 exact CloudFormation role pass만 허용하며 Execute 권한이 없다. 두 승인 window는 분리한다. 이 repository revision은 AWS/GitHub resource, role, parameter, endpoint, stack, DB user를 생성하거나 변경하지 않는다.
