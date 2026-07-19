> **작성자**: Threadia 팀
> **작성일**: 2026-07-14
> **최종 수정**: 2026-07-19
> **버전**: v1.4
> **상태**: 검토 중
> **태그**: aws, release, ssm, approval, runbook

# Restricted SSM Release Runbook

## 승인 gate

다음 순서의 별도 승인 없이는 GitHub workflow 또는 CloudFormation Change Set을 실행하지 않는다.

1. Network Change Set: compute subnet 전용 route table, S3 Gateway Endpoint, approved S3 managed prefix list HTTPS egress, deterministic artifact `releases/*` GetObject/GetObjectVersion endpoint policy와 restricted EC2에서 dedicated SES SMTP Interface Endpoint로 향하는 TCP/587 ingress/egress만 검토한다. SES endpoint는 private DNS와 ComputeSubnet만 사용하며 NAT, public egress 또는 SMTP credential/runtime delivery는 이 승인 gate에 포함하지 않는다. apply 전에는 UPDATE Change Set을 검토한다. pre-existing resource에 Modify 또는 Remove가 있거나 Replacement가 `True` 또는 `Conditional`이면 hard reject한다. `SesSmtpEndpointSecurityGroup`, `SesSmtpEndpointIngress`, `Ec2SmtpToSesEndpoint`, `SesSmtpEndpoint`의 신규 Add만 승인하며 그 밖의 action은 실행하지 않는다.
2. SSM delivery Change Set: `repo:SEUIL/Threadia:environment:restricted-nonprod` exact OIDC subject, deterministic private bucket, exact host role/instance, fixed Command document와 IAM policy를 검토한다. fixed document는 caller parameter 없이 stack Region을 `AWS_REGION`과 `AWS_DEFAULT_REGION`에 고정 주입하며 helper는 이 Region을 검증해 모든 S3/SSM 호출에 명시한다. 이 operator role은 execute하지 않고 Change Set review까지만 수행한다.
3. Host gate: Java는 archive의 Linux x86_64 runtime으로 제공한다. AMI에는 SSM Agent, AWS CLI, Python 3, tar, curl, `flock`, systemd, `threadia` user와 root-owned service/helper만 준비한다. `bootstrap-host.sh`를 approved local artifact로 한 번 실행하며 package manager 또는 network access를 사용하지 않는다. helper maintenance는 `apply-release`만 갱신한다. service unit revision이 `Environment=MANAGEMENT_HEALTH_MAIL_ENABLED=false`를 포함하는지 확인하고, 누락 시 아래 service-unit maintenance channel의 별도 Change Set 승인으로만 remediation한다.
4. Runtime gate: restricted SSM delivery path는 여섯 legacy base key와 필수 `THREADIA_PROJECT_CHAT_CURSOR_HMAC_SECRET`, `THREADIA_NOTIFICATION_CURSOR_HMAC_SECRET`, `THREADIA_AUDIT_CURSOR_HMAC_SECRET`을 exact-name Parameter Store SecureString으로 전달한다. 세 cursor secret은 모두 nonblank UTF-8 32바이트 이상이고 control character가 없어야 하며, 각 exact SSM ARN과 `ssm:GetParameter(s)` IAM allow 없이 release하면 안 된다. helper는 모든 required base/cursor name이 정확히 반환되는지 검증한 뒤 staged runtime env를 한 번에 materialize한다. bootstrap email ARN은 IAM에 exact-name으로 계속 허용하지만, `THREADIA_PLATFORM_ADMIN_BOOTSTRAP_EMAIL`은 two-phase key다. **Phase 1 hard gate**는 key가 Parameter Store response에서 정확히 absent이고 staged runtime env에도 해당 key가 없으며 `platform_admins` row count가 zero인 경우다. 이 세 조건 중 하나라도 실패하면 helper activation, application start, write freeze 해제를 하지 않는다. Phase 2에서 key가 반환되면 helper는 control character를 거부하고 trim + lowercase한 값에 service와 같은 `^[^\s@]+@[^\s@]+\.[^\s@]+$` shape를 강제한다. blank·malformed·response mismatch는 actual value를 출력하지 않고 activation 전에 실패한다. Phase 2의 email은 이미 존재하는 intended ACTIVE user를 가리켜야 하며, restart는 one-time `SUPER_ADMIN` bootstrap만 수행한다. Phase 2 postflight에서만 intended ACTIVE `SUPER_ADMIN` exactly one row와 다른 platform admin row의 approved evidence를 reconciliation한다. optional MAIL group은 `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `NOTIFICATION_FROM_EMAIL` 다섯 SecureString 전체만 허용한다. 다섯 값이 모두 absent이면 MAIL env를 생략하고, 다섯 값이 모두 non-empty이고 control character가 없을 때만 함께 materialize한다. blank·partial·missing·invalid 조합은 activation 전에 parameter name만 표시하고 실패하며 AWS CLI read error는 absent가 아니라 release failure다. master와 분리된 application DB user가 존재하고, parameter creation/rotation owner가 확인됐는지 검토한다.
   MAIL group을 제공하거나 SMTP 발송을 운영하기 전에는 SES Production access와 SES SMTP PrivateLink가 모두 승인·적용됐는지 확인한다. identity/DKIM 검증만으로는 충분하지 않으며, pending network PrivateLink 변경은 이 release change와 분리한다.
5. GitHub gate: `restricted-nonprod` Environment에 `main`만 custom branch policy로 설정하고 OIDC workflow variables와 workflow revision을 검토한다. release job 자체도 `refs/heads/main`만 허용하며 고정 concurrency group에서 이전 release를 취소하지 않고 직렬 실행한다.
6. Helper maintenance gate: helper artifact workflow가 `apply-release` 한 파일을 `releases/<commit>/<run>-<attempt>/apply-release`에 upload한 뒤 기록한 key, S3 VersionId, SHA-256, commit, run evidence를 승인한다. artifact publish는 host를 mutate하지 않아 별도 concurrency group을 사용한다. 같은 triple을 Change Set의 helper parameter 세 개에 pin하고, Change Set에서 parameterless maintenance document와 exact host만 변경되는지 검토한다.
7. Service-unit maintenance gate: service-unit artifact workflow가 `threadia-backend.service` 한 파일을 `releases/<commit>/<run>-<attempt>/threadia-backend.service`에 upload하기 전에 mail-health sentinel과 `systemd-analyze verify`를 통과했는지 확인한다. 기록된 key, S3 VersionId, SHA-256, commit, run evidence triple을 service-unit parameter 세 개에 pin하고, Change Set에서 parameterless service-unit document, GitHub role의 해당 document ARN, exact host만 변경되는지 검토한다. document revision lifecycle에는 `ssm:UpdateDocumentDefaultVersion`가 필요하므로 live rollout 전 SSM document version capacity와 non-default version 정리를 별도 검토한다.

## Managed policy recovery sequence

PR #152처럼 source managed policy가 롤백된 경우에는 old-policy Change Set을 다시 실행하지 않는다. `restricted-network.yaml`은 IAM policy를 소유하지 않으므로, `restricted-network-service-permissions-policy.json`의 source merge를 먼저 반영하고 그 다음에 새 managed-policy version을 발행해 default version으로 전환한다. 이전 default version은 release window 동안 유지해 비교와 rollback을 가능하게 둔다.

1. `restricted-network-service-permissions-policy.json`에 `ec2:DescribeRouteTables`를 포함한 source merge를 먼저 완성한다.
2. 새 managed-policy version을 생성하고 default로 지정한다. old default version을 덮어쓰지 않는다.
3. restricted network의 service/operator role이 새 default version을 바라보는지 검증한다.
4. 그 다음에만 fresh UPDATE Change Set을 다시 만든다. 실패했던 old-policy Change Set은 재시도하지 않는다.
5. recovery Change Set은 `SesSmtpEndpointSecurityGroup`, `SesSmtpEndpointIngress`, `Ec2SmtpToSesEndpoint`, `SesSmtpEndpoint`의 네 개 Add만 보여야 하며, 그 외 action이 있으면 중단한다.

## Release와 failure 처리

`main` push 또는 `main` ref에서의 수동 `workflow_dispatch`만 release job을 실행한다. 다른 ref의 수동 dispatch는 job-level guard에서 거부한다. release와 helper maintenance는 같은 고정 concurrency group과 host `flock`으로 서로 직렬화하고 실행 중인 작업을 취소하지 않는다. bounded job timeout 안에서 immutable bundle을 upload하고 fixed SSM document의 completion을 확인한다. operator가 shell command, bucket, host, path 또는 Region을 입력해 실행하는 절차는 없다. command가 실패하면 VersionId, release ID, systemd journal, SSM output의 secret 없는 error state를 보존하고 신규 release를 중단한다.

helper는 restart 후 최대 120초 동안 systemd service가 active인지 확인하면서 local `/actuator/health` 성공을 polling한다. Phase 1 postflight evidence에는 bootstrap key가 staged runtime env에서 absent였다는 결과, `platform_admins` row count zero, `ddl-auto=validate` startup 성공, active service와 health success가 모두 포함돼야 한다. Phase 2 postflight에서만 intended ACTIVE `SUPER_ADMIN` exactly one row와 다른 platform admin row의 approved evidence를 reconciliation한다. service가 inactive가 되거나 timeout까지 health가 성공하지 않으면 직전 active symlink와 `/etc/threadia/runtime.env`를 함께 복구하고 이전 service를 restart한다. 첫 release처럼 이전 상태가 없으면 새 symlink와 runtime env를 삭제하고 service를 중지한 채 실패한다. restricted egress에서 mail health가 release readiness를 503으로 만들지 않도록 systemd unit에만 `MANAGEMENT_HEALTH_MAIL_ENABLED=false`를 설정하며 global backend application properties는 변경하지 않는다. single-host restart downtime은 허용되지만 DB migration은 금지다. Parameter rotation은 platform owner가 새 SecureString value를 저장한 후 별도 승인된 release 또는 restart로 적용하며, RDS master credential을 runtime에 재사용하지 않는다.

Helper maintenance는 Change Set으로 이미 pin된 key, VersionId, SHA-256만 사용하는 parameterless document를 exact host에 invoke한다. document는 current helper를 unique sibling backup으로 copy한 뒤 root:root `0750` staged sibling을 target 위로 atomic rename하고 digest와 `bash -n`을 다시 검증한다. ERR, INT, TERM, EXIT는 success 전 backup을 atomic restore하며 target을 먼저 이동하지 않는다. service restart와 Parameter Store 접근은 하지 않는다. 설치 실패는 같은 pinned triple의 재invoke가 아니라 Change Set 검토·수정 또는 승인된 이전 triple로의 rollback 경계로 처리한다.

## One-time persistent DB exception

GitHub workflow, `apply-release`, helper maintenance, CloudFormation, 그리고 일반 release 절차는 DB migration을 자동 실행하지 않으며 이 금지는 유지한다. 확인된 Platform foundation schema drift에 한해 별도 승인된 DB operator가 [`restricted-ssm-platform-foundation-db-migration-runbook.md`](./restricted-ssm-platform-foundation-db-migration-runbook.md)의 one-time additive SQL을 실행할 수 있다. repository/PR 검토와 live DB backup·SQL execution·same-SHA release retry는 각각 별도 gate이며, 이 runbook의 approval이 서로를 대체하지 않는다. persistent SSM host에서 `db/schema.sql`을 실행하는 것은 금지한다. Phase 1은 schema postflight 후 empty `platform_admins`를 유지한 채 retry할 수 있다. migration/release failure 뒤 runtime rollback은 DB authorization을 되돌리지 않으며, exactly one intended ACTIVE `SUPER_ADMIN` 및 다른 platform admin row의 explicit approved evidence reconciliation은 별도 승인된 Phase 2 bootstrap 후에만 수행한다.

Service-unit maintenance는 helper maintenance와 별도 channel이며 `threadia-backend.service`만 다룬다. Change Set으로 pin된 service-unit key, VersionId, SHA-256을 parameterless document가 exact host에서 내려받아 digest, mail-health sentinel, `systemd-analyze verify`를 확인한다. document는 shared nonblocking host lock을 잡고 current unit을 unique backup으로 copy한 뒤 staged root:root `0644` file을 atomic install한다. daemon reload와 restart 후 최대 120초 동안 active 상태와 local `/actuator/health`를 확인한다. ERR, INT, TERM, EXIT 또는 readiness 실패는 backup unit을 atomic restore하고 daemon reload/restart 및 rollback health까지 확인한다. 성공 후에만 staged/download/backup을 정리한다. 이 source revision은 배포된 상태를 주장하지 않으며 live Change Set 승인과 invoke는 별도 절차다.

## Legacy 경계

CodeDeploy source와 이전 deploy skeleton은 삭제하지 않는다. 이 runbook의 SSM path가 승인 대상이며 legacy CodeDeploy source는 superseded reference로만 유지한다.

## 변경 이력

| 날짜 | 내용 |
|---|---|
| 2026-07-19 | Phase 1은 bootstrap parameter absent와 empty `platform_admins`를 요구하고, exactly-one `SUPER_ADMIN` reconciliation은 Phase 2 postflight로 분리했다. |
| 2026-07-19 | Phase 1 no-bootstrap runtime contract와 `ddl-auto=validate` startup·readiness postflight evidence를 추가했다. |
| 2026-07-19 | audit cursor secret을 required exact-name SSM key와 IAM ARN으로 전달하고, bootstrap email을 absent-only Phase 1 omit/valid Phase 2 bootstrap contract로 분리했다. |
| 2026-07-19 | project-chat·notification·audit cursor secret 세 개를 동일한 exact-name SSM/IAM/runtime validation contract로 정렬했다. |
| 2026-07-18 | restricted helper의 bootstrap email pre-activation validation과 rollback 이후 DB authorization reconciliation gate를 추가했다. |
| 2026-07-18 | workflow/helper 자동 migration 금지를 유지하면서, 별도 승인된 one-time persistent DB operator migration과 bootstrap email runtime gate를 명시했다. |
| 2026-07-17 | restricted network managed-policy source 복구 절차를 추가했다. old-policy Change Set은 재시도하지 않고 source merge → 새 managed-policy version/default 전환 → role 검증 → fresh Change Set 순서로 복구해야 하며, recovery Change Set은 네 SES logical resource Add만 보여야 한다고 명시했다. |
| 2026-07-17 | restricted EC2와 SES SMTP PrivateLink 사이의 TCP/587 전용 network gate를 추가했다. UPDATE Change Set에서 pre-existing resource Modify/Remove, Replacement `True`/`Conditional`을 hard reject하고 네 SES logical resource의 신규 Add만 승인하며 SMTP credential 및 runtime delivery는 별도 후속 승인 범위로 분리했다. |
| 2026-07-17 | optional MAIL five-key group의 blank value를 pre-activation failure로 추가했다. |
| 2026-07-17 | optional all-or-none MAIL five-key runtime contract, partial group pre-activation failure, SES Production access·PrivateLink gate와 mail health disabled 유지 기준을 추가했다. |
| 2026-07-16 | 현재 restricted SSM delivery path가 여섯 SecureString만 전달하며 bootstrap email을 주입할 수 없음을 명시했다. `platform_admins`가 비어 있는 첫 배포의 No-Go 경계를 추가했다. |
