> **작성자**: Threadia 팀  
> **작성일**: 2026-07-14  
> **최종 수정**: 2026-07-15
> **버전**: v0.6
> **상태**: 검토 중  
> **태그**: aws, release, ssm, approval, runbook

# Restricted SSM Release Runbook

## 승인 gate

다음 순서의 별도 승인 없이는 GitHub workflow 또는 CloudFormation Change Set을 실행하지 않는다.

1. Network Change Set: compute subnet 전용 route table, S3 Gateway Endpoint, approved S3 managed prefix list HTTPS egress와 deterministic artifact `releases/*` GetObject/GetObjectVersion endpoint policy를 검토한다.
2. SSM delivery Change Set: `repo:SEUIL/Threadia:environment:restricted-nonprod` exact OIDC subject, deterministic private bucket, exact host role/instance, fixed Command document와 IAM policy를 검토한다. fixed document는 caller parameter 없이 stack Region을 `AWS_REGION`과 `AWS_DEFAULT_REGION`에 고정 주입하며 helper는 이 Region을 검증해 모든 S3/SSM 호출에 명시한다. 이 operator role은 execute하지 않고 Change Set review까지만 수행한다.
3. Host gate: Java는 archive의 Linux x86_64 runtime으로 제공한다. AMI에는 SSM Agent, AWS CLI, Python 3, tar, curl, `flock`, systemd, `threadia` user와 root-owned service/helper만 준비한다. `bootstrap-host.sh`를 approved local artifact로 한 번 실행하며 package manager 또는 network access를 사용하지 않는다. helper maintenance는 `apply-release`만 갱신한다. service unit revision이 `Environment=MANAGEMENT_HEALTH_MAIL_ENABLED=false`를 포함하는지 확인하고, 누락 시 아래 service-unit maintenance channel의 별도 Change Set 승인으로만 remediation한다.
4. Runtime gate: six Parameter Store SecureString과 master와 분리된 application DB user가 존재하고, parameter creation/rotation owner가 확인됐는지 검토한다.
5. GitHub gate: `restricted-nonprod` Environment에 `main`만 custom branch policy로 설정하고 OIDC workflow variables와 workflow revision을 검토한다. release job 자체도 `refs/heads/main`만 허용하며 고정 concurrency group에서 이전 release를 취소하지 않고 직렬 실행한다.
6. Helper maintenance gate: helper artifact workflow가 `apply-release` 한 파일을 `releases/<commit>/<run>-<attempt>/apply-release`에 upload한 뒤 기록한 key, S3 VersionId, SHA-256, commit, run evidence를 승인한다. artifact publish는 host를 mutate하지 않아 별도 concurrency group을 사용한다. 같은 triple을 Change Set의 helper parameter 세 개에 pin하고, Change Set에서 parameterless maintenance document와 exact host만 변경되는지 검토한다.
7. Service-unit maintenance gate: service-unit artifact workflow가 `threadia-backend.service` 한 파일을 `releases/<commit>/<run>-<attempt>/threadia-backend.service`에 upload하기 전에 mail-health sentinel과 `systemd-analyze verify`를 통과했는지 확인한다. 기록된 key, S3 VersionId, SHA-256, commit, run evidence triple을 service-unit parameter 세 개에 pin하고, Change Set에서 parameterless service-unit document, GitHub role의 해당 document ARN, exact host만 변경되는지 검토한다. document revision lifecycle에는 `ssm:UpdateDocumentDefaultVersion`가 필요하므로 live rollout 전 SSM document version capacity와 non-default version 정리를 별도 검토한다.

## Release와 failure 처리

`main` push 또는 `main` ref에서의 수동 `workflow_dispatch`만 release job을 실행한다. 다른 ref의 수동 dispatch는 job-level guard에서 거부한다. release와 helper maintenance는 같은 고정 concurrency group과 host `flock`으로 서로 직렬화하고 실행 중인 작업을 취소하지 않는다. bounded job timeout 안에서 immutable bundle을 upload하고 fixed SSM document의 completion을 확인한다. operator가 shell command, bucket, host, path 또는 Region을 입력해 실행하는 절차는 없다. command가 실패하면 VersionId, release ID, systemd journal, SSM output의 secret 없는 error state를 보존하고 신규 release를 중단한다.

helper는 restart 후 최대 120초 동안 systemd service가 active인지 확인하면서 local `/actuator/health` 성공을 polling한다. service가 inactive가 되거나 timeout까지 health가 성공하지 않으면 직전 active symlink와 `/etc/threadia/runtime.env`를 함께 복구하고 이전 service를 restart한다. 첫 release처럼 이전 상태가 없으면 새 symlink와 runtime env를 삭제하고 service를 중지한 채 실패한다. restricted egress에서 mail health가 release readiness를 503으로 만들지 않도록 systemd unit에만 `MANAGEMENT_HEALTH_MAIL_ENABLED=false`를 설정하며 global backend application properties는 변경하지 않는다. single-host restart downtime은 허용되지만 DB migration은 금지다. Parameter rotation은 platform owner가 새 SecureString value를 저장한 후 별도 승인된 release 또는 restart로 적용하며, RDS master credential을 runtime에 재사용하지 않는다.

Helper maintenance는 Change Set으로 이미 pin된 key, VersionId, SHA-256만 사용하는 parameterless document를 exact host에 invoke한다. document는 current helper를 unique sibling backup으로 copy한 뒤 root:root `0750` staged sibling을 target 위로 atomic rename하고 digest와 `bash -n`을 다시 검증한다. ERR, INT, TERM, EXIT는 success 전 backup을 atomic restore하며 target을 먼저 이동하지 않는다. service restart와 Parameter Store 접근은 하지 않는다. 설치 실패는 같은 pinned triple의 재invoke가 아니라 Change Set 검토·수정 또는 승인된 이전 triple로의 rollback 경계로 처리한다.

Service-unit maintenance는 helper maintenance와 별도 channel이며 `threadia-backend.service`만 다룬다. Change Set으로 pin된 service-unit key, VersionId, SHA-256을 parameterless document가 exact host에서 내려받아 digest, mail-health sentinel, `systemd-analyze verify`를 확인한다. document는 shared nonblocking host lock을 잡고 current unit을 unique backup으로 copy한 뒤 staged root:root `0644` file을 atomic install한다. daemon reload와 restart 후 최대 120초 동안 active 상태와 local `/actuator/health`를 확인한다. ERR, INT, TERM, EXIT 또는 readiness 실패는 backup unit을 atomic restore하고 daemon reload/restart 및 rollback health까지 확인한다. 성공 후에만 staged/download/backup을 정리한다. 이 source revision은 배포된 상태를 주장하지 않으며 live Change Set 승인과 invoke는 별도 절차다.

## Legacy 경계

CodeDeploy source와 이전 deploy skeleton은 삭제하지 않는다. 이 runbook의 SSM path가 승인 대상이며 legacy CodeDeploy source는 superseded reference로만 유지한다.
