> **작성자**: Threadia 팀
> **작성일**: 2026-07-21
> **최종 수정**: 2026-07-22
> **버전**: v1.3
> **상태**: 검토 중
> **태그**: aws, s3, attachment, ecr, compose, runbook, approval

# Public Compose Attachment Storage Runbook

## 1. 목적과 범위

이 문서는 public ECR dual-image Compose backend의 S3 presigned attachment rollout 절차다. 대상은 dedicated attachment bucket, backend EC2 workload role, root-only runtime env, one-time attachment DDL이다. restricted-network S3 Gateway Endpoint policy, immutable release artifact bucket, GitHub release role, GitHub Actions credential, browser 공개 bucket은 이 절차의 대상이 아니다.

attachment bucket은 `threadia-attachments-${accountId}-${region}` 형식의 account/region-derived private bucket만 사용한다. artifact `releases/*` bucket을 attachment 용도로 재사용하거나 GitHub release role에 attachment object 권한을 추가하지 않는다.

## 2. 승인과 역할 분리

1. PR review는 AWS/IAM/deploy 변경으로서 팀 승인 대상이다. release owner와 최종 승인자는 분리한다.
2. CloudFormation operator는 Change Set 생성·조회만 하고 Execute하지 않는다. 승인된 별도 executor가 최종 Change Set과 승인 기록을 대조한 뒤에만 실행한다.
3. DB backup, attachment additive SQL 실행, schema postflight는 승인된 DB operator만 수행한다. 이 승인은 CloudFormation 또는 image release 승인을 대체하지 않는다.
4. `attachment-storage-additive.sql`은 일반 release, SSM document, Compose helper, CI가 호출하면 안 된다.

## 3. Change Set 생성 gate

CloudFormation operator는 아래와 같이 attachment foundation stack용 Change Set만 생성한다. 이 단계는 resource 생성이나 update를 적용하지 않는다. parameter file은 repository example을 복사한 approved local file이어야 하며 actual bucket name, credential, presigned URL, object key를 포함하면 안 된다.

```bash
aws cloudformation create-change-set \
  --region ap-northeast-2 \
  --stack-name REPLACE_WITH_APPROVED_ATTACHMENT_STORAGE_STACK \
  --change-set-name REPLACE_WITH_REVIEW_IDENTIFIER \
  --change-set-type CREATE \
  --template-body file://infra/cloudformation/attachment-storage-foundation.yaml \
  --parameters file://APPROVED_ATTACHMENT_STORAGE_PARAMETERS.json \
  --capabilities CAPABILITY_IAM
```

기존 stack을 의도적으로 update하는 경우에만 `--change-set-type UPDATE`를 사용한다. 기존 artifact bucket 또는 release foundation stack을 attachment stack으로 대체하거나, Change Set 생성 단계에서 `aws cloudformation execute-change-set`을 호출하는 것은 No-Go다.

## 4. Change Set review gate

다음 조건을 모두 만족하는 fresh Change Set만 승인 후보가 된다.

- `AttachmentBucket`, attachment bucket policy, exact backend EC2 role inline policy의 Add 또는 의도된 update만 포함한다.
- bucket name은 account/region-derived attachment 이름이고 artifact bucket 이름 또는 `releases/*` resource가 아니다.
- bucket은 Block Public Access, BucketOwnerEnforced, SSE-S3, versioning, lifecycle, TLS-only deny를 가진다.
- backend role allow는 attachment bucket object ARN의 `s3:GetObject`, `s3:PutObject`, `s3:DeleteObject`만 가진다. `ListBucket`, wildcard bucket/object, static access key, GitHub role attachment access는 거부한다.
- public dual-image Compose host가 restricted-network를 사용한다는 근거가 없으므로 restricted S3 Gateway Endpoint policy는 attachment rollout에서 변경하지 않는다. host VPC endpoint topology가 확정되면 별도 Change Set과 별도 approval로 검토한다.
- bucket policy에 `aws:SourceVpce` only allow/deny를 넣지 않는다. browser presigned PUT/GET는 VPC endpoint를 통과하지 않으며, public access는 Block Public Access와 exact backend principal·presigned signature로 막는다.
- CORS는 승인된 HTTPS frontend origin, `GET`/`PUT`, `content-type`, `x-amz-checksum-sha256` 및 required SigV4 headers만 허용한다. `*` origin 또는 header 허용은 No-Go다.

Change Set reviewer는 operator가 만든 exact Change Set ID만 조회한다. status가 `CREATE_PENDING`, `CREATE_IN_PROGRESS`, `FAILED`, `DELETE_COMPLETE`이거나 Changes가 비어 있으면 No-Go다.

```bash
aws cloudformation describe-change-set \
  --region ap-northeast-2 \
  --stack-name REPLACE_WITH_APPROVED_ATTACHMENT_STORAGE_STACK \
  --change-set-name REPLACE_WITH_REVIEW_IDENTIFIER
```

review 결과와 Change Set ID를 승인 기록에 남긴 뒤, operator 권한으로는 더 이상 진행하지 않는다.

## 5. Change Set execute gate

승인된 별도 executor만 review된 Change Set ID와 approval record가 일치할 때 execute한다. executor는 Change Set을 새로 만들거나 parameter를 바꾸지 않으며, 현재 Change Set이 `CREATE_COMPLETE`이고 review된 action summary와 동일하지 않으면 실행하지 않는다.

```bash
aws cloudformation execute-change-set \
  --region ap-northeast-2 \
  --stack-name REPLACE_WITH_APPROVED_ATTACHMENT_STORAGE_STACK \
  --change-set-name REPLACE_WITH_REVIEW_IDENTIFIER
```

이 명령은 이 repository 작업에서 실행하지 않는다. execute 후에는 stack event에서 failed resource와 unexpected replacement가 없는지만 확인하고, 승인된 runtime/DB/release gate가 완료될 때까지 browser traffic이나 image release를 시작하지 않는다.

## 6. Runtime delivery gate

`/etc/threadia-compose/runtime.env`는 root:root `0600`이며 public Compose host의 approved workload role이 AWS default credential chain으로 S3를 호출한다. 다음 non-secret keys는 모두 정확히 하나씩 필요하다.

| 키 | required value contract |
|---|---|
| `THREADIA_STORAGE_S3_REGION` | approved AWS region |
| `THREADIA_STORAGE_S3_BUCKET` | dedicated account/region-derived attachment bucket |
| `THREADIA_STORAGE_S3_PATH_STYLE` | `false` |
| `THREADIA_STORAGE_S3_PRESIGN_TTL` | `PT1M`~`PT10M` ISO-8601 duration; existing default `PT10M`과 local integration `PT2M`을 넘지 않는다 |
| `THREADIA_STORAGE_ORPHAN_CLEANUP_ENABLED` | `true` |
| `THREADIA_STORAGE_ORPHAN_CLEANUP_DELAY` | positive ISO-8601 duration |
| `THREADIA_STORAGE_UPLOAD_CLEANUP_DELAY` | positive ISO-8601 duration |

`THREADIA_STORAGE_S3_ENDPOINT`, `THREADIA_STORAGE_S3_ACCESS_KEY`, `THREADIA_STORAGE_S3_SECRET_KEY`는 AWS runtime env에 존재하면 No-Go다. 이 키는 local MinIO만을 위한 것이다. `deploy-dual-release`는 runtime file permission, dedicated bucket pattern, `PT1M`~`PT10M` presign TTL, static key/endpoint absence를 release 전에 fail-closed 검증한다.

## 7. One-time DB gate

1. application release 전에 backup identity, restoration owner, maintenance window를 기록한다.
2. DB operator가 reviewed `scripts/ssm-delivery/attachment-storage-additive.sql`만 persistent MySQL 8 target에 실행한다. `db/schema.sql` 실행과 automated migration은 금지한다.
3. `attachments` table, object-key unique index, lifecycle checks, users avatar/company logo nullable references와 foreign keys를 name-only schema query로 확인한다. attachment metadata나 user content는 evidence에 기록하지 않는다.
4. SQL failure 또는 partial state는 automatic rollback하지 않는다. operator가 backup/approved remediation boundary에서 판단하며 image release를 중단한다.

## 8. Release 및 smoke gate

Before any production Change Set execution or host release, a real staging browser presigned upload/download/CORS/authorization smoke PASS evidence record must exist; absent staging evidence is No-Go.

1. approved runtime env와 backend EC2 role access가 준비된 뒤 immutable dual-image release를 수행한다. release helper는 DB SQL, bucket creation, IAM change를 수행하지 않는다.
2. normal health success 뒤 authorized actor로 presigned upload를 요청한다. browser PUT은 signed `Content-Type`와 `x-amz-checksum-sha256`를 보낸다.
3. checksum HEAD verification과 complete 성공 뒤 authorized download만 확인한다.
4. cross-company/unauthorized attachment API access, expired URL, checksum mismatch, browser CORS preflight, public unauthenticated object URL denial을 모두 확인한다.
5. stale `UPLOADING` 및 logically deleted metadata cleanup이 configured delay 뒤 object delete를 수행하는지 non-content evidence로 확인한다.

실패하면 신규 release와 write를 중단한다. application release rollback은 image pair와 runtime env만 되돌리며 bucket object 또는 DB metadata를 자동 복구하지 않는다.

## 9. Rollback 및 No-Go

1. Change Set execute 전 review mismatch, unexpected Modify/Remove/Replacement, artifact bucket reference, wildcard attachment access, CORS wildcard, static credential/runtime endpoint 또는 SourceVpce-only attachment restriction을 발견하면 Change Set을 execute하지 않고 폐기한다.
2. Change Set execute가 실패하면 failed event를 name-only로 기록하고 runtime env 변경, DB SQL, image release를 모두 중단한다. 기존 stack과 artifact delivery는 변경하거나 복구하려 시도하지 않는다.
3. execute 후 S3/IAM policy를 되돌려야 하면 별도 reviewed rollback Change Set으로 attachment role policy만 대상으로 한다. `DeletionPolicy: Retain` attachment bucket은 자동 삭제하지 않으며, bucket/object 삭제는 별도 data-retention 승인 없이는 No-Go다.
4. `attachment-storage-additive.sql` 실행 후에는 automatic SQL rollback을 하지 않는다. attachment metadata 또는 object를 삭제하지 말고 DB operator가 backup과 approved remediation plan으로만 판단한다.

## 10. 기록할 evidence

Change Set ID와 action summary, reviewer/executor/DB operator, release ID, bucket policy/IAM/CORS name-only review 결과, runtime key 존재 여부, schema postflight, smoke PASS/FAIL, Go/No-Go와 rollback 판단을 기록한다. account ID, actual bucket value, object key, presigned URL, credential, checksum, user content는 기록하지 않는다.

2026-07-22 public dual-image in-place presentation release의 실행 증빙과 열려 있는 attachment transfer smoke gate는 [Public dual-image release 검증기록](./public-dual-image-release-검증기록.md)에서 확인한다.

## 변경 이력

| 날짜 | 내용 |
|---|---|
| 2026-07-22 | public dual-image in-place presentation release의 검증기록 링크를 추가했다. authenticated upload/presign smoke는 test credential 부재로 미수행되어 attachment transfer smoke gate가 열려 있음을 해당 기록에서 명시했다. |
| 2026-07-21 | public Compose topology가 restricted-network와 분리됨을 명시하고, restricted endpoint/policy 변경을 attachment rollout 범위에서 제외했다. versioned object의 noncurrent version 만료와 expired delete-marker cleanup, 기존 `PT10M` default·local `PT2M`에 근거한 `PT1M`~`PT10M` presign TTL gate를 적용했다. |
