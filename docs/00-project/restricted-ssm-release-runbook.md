> **작성자**: Threadia 팀
> **작성일**: 2026-07-14
> **최종 수정**: 2026-07-14
> **버전**: v0.2
> **상태**: 검토 중
> **태그**: aws, release, ssm, approval, runbook

# Restricted SSM Release Runbook

## 승인 gate

다음 순서의 별도 승인 없이는 GitHub workflow 또는 CloudFormation Change Set을 실행하지 않는다.

1. Network Change Set: compute subnet 전용 route table, S3 Gateway Endpoint, approved S3 managed prefix list HTTPS egress와 deterministic artifact `releases/*` GetObject/GetObjectVersion endpoint policy를 검토한다.
2. SSM delivery Change Set: `repo:SEUIL/Threadia:environment:restricted-nonprod` exact OIDC subject, deterministic private bucket, exact host role/instance, fixed Command document와 IAM policy를 검토한다. 이 operator role은 execute하지 않고 Change Set review까지만 수행한다.
3. Host gate: Java는 archive의 Linux x86_64 runtime으로 제공한다. AMI에는 SSM Agent, AWS CLI, Python 3, tar, curl, systemd, `threadia` user와 root-owned service/helper만 준비한다. `bootstrap-host.sh`를 approved local artifact로 한 번 실행하며 package manager 또는 network access를 사용하지 않는다.
4. Runtime gate: six Parameter Store SecureString과 master와 분리된 application DB user가 존재하고, parameter creation/rotation owner가 확인됐는지 검토한다.
5. GitHub gate: `restricted-nonprod` Environment에 `main`만 custom branch policy로 설정하고 OIDC workflow variables와 workflow revision을 검토한다.

## Release와 failure 처리

`main` push만 release workflow를 trigger한다. workflow는 immutable bundle을 upload하고 fixed SSM document의 completion을 확인한다. operator가 shell command, bucket, host, path를 입력해 실행하는 절차는 없다. command가 실패하면 VersionId, release ID, systemd journal, SSM output의 secret 없는 error state를 보존하고 신규 release를 중단한다.

helper는 health failure에 직전 active symlink로 local rollback한다. 첫 release처럼 이전 release가 없을 때는 service를 중지한 채 실패한다. single-host restart downtime은 허용되지만 DB migration은 금지다. Parameter rotation은 platform owner가 새 SecureString value를 저장한 후 별도 승인된 release 또는 restart로 적용하며, RDS master credential을 runtime에 재사용하지 않는다.

## Legacy 경계

CodeDeploy source와 이전 deploy skeleton은 삭제하지 않는다. 이 runbook의 SSM path가 승인 대상이며 legacy CodeDeploy source는 superseded reference로만 유지한다.
