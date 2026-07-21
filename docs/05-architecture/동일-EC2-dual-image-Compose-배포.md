> **작성자**: Threadia 팀  
> **작성일**: 2026-07-16  
> **최종 수정**: 2026-07-21<br>
> **버전**: v1.3<br>
> **상태**: 검토 중  
> **태그**: docker, compose, ecr, deployment, rollback, nextjs

# 동일 EC2 dual-image Compose 배포

## 1. 범위

동일 EC2에서 Next.js SSR frontend와 Spring Boot backend를 독립적인 immutable ECR image로 실행한다. public reverse proxy/TLS는 이 문서의 범위 밖이며, proxy는 `threadia.click`을 frontend의 `127.0.0.1:3000`으로, `api.threadia.click`을 backend의 `127.0.0.1:8080`으로 전달해야 한다. Docker Compose는 public port를 열지 않는다.

## 2. Repository 계약

| 경로 | 책임 |
|---|---|
| `frontend/Dockerfile` | Node 22 multi-stage build로 Next standalone SSR server를 non-root user로 실행한다. `NEXT_PUBLIC_API_URL`은 build argument이므로 `https://api.threadia.click`으로 build할 때만 browser bundle에 반영된다. |
| `frontend/next.config.ts` | `output: "standalone"`을 선언해 runtime image가 source와 development dependency 없이 실행되게 한다. |
| `compose.dual-image.production.yml` | `frontend`과 `backend`를 분리하고 각각 localhost `3000`, `8080`에 bind한다. `name: threadia-compose-dual`을 사용하고 backend secret은 root-only runtime env file로만 주입한다. |
| `scripts/ecr-compose-deploy/deploy-dual-release` | 두 ECR digest를 가진 release manifest를 검증하고, 두 service health가 모두 성공할 때만 active state를 원자적으로 교체한다. legacy backend-only runtime은 자동으로 교체하지 않는다. |
| `scripts/ecr-compose-deploy/deploy-release-manifest` | fixed SSM document가 전달한 versioned S3 manifest reference 하나를 고정 bucket에서 내려받아 `deploy-dual-release`에 전달한다. |
| `scripts/ecr-compose-deploy/release-manifest.example.env` | tracked example이며 실제 digest나 credential을 담지 않는다. |
| `scripts/ecr-compose-deploy/release-manifest-transport.example.env` | host의 root-only artifact bucket 설정 형식을 보여 주며 실제 bucket 이름을 담지 않는다. |

기존 `compose.production.yml` 및 `deploy-image`는 backend 단일-image host 계약으로 유지한다. `deploy-dual-release`는 root-owned legacy `current-image` state 또는 실행 중인 legacy `app`을 발견하면 실패한다. live backend-only route의 container, Nginx upstream, DNS는 이 helper나 workflow가 자동으로 바꾸지 않는다.

## 3. Immutable release manifest

host의 release manifest는 root:root 소유이며 group/other write가 없어야 한다. `deploy-dual-release`는 shell source를 사용하지 않고 아래 세 key만 엄격하게 읽으므로, manifest에 command 또는 runtime secret을 넣을 수 없다.

```properties
THREADIA_RELEASE_ID=2026.07.16-001
THREADIA_FRONTEND_IMAGE=123456789012.dkr.ecr.ap-northeast-2.amazonaws.com/threadia-frontend@sha256:<64-lowercase-hex>
THREADIA_BACKEND_IMAGE=123456789012.dkr.ecr.ap-northeast-2.amazonaws.com/threadia-backend@sha256:<64-lowercase-hex>
```

digest는 tag가 아닌 full `repository@sha256:<64-hex>` reference여야 한다. frontend와 backend image는 서로 다른 repository를 사용할 수 있다. frontend image를 build할 때에는 다음처럼 API public URL을 build argument로 고정한다.

```bash
docker build --platform linux/amd64 \
  --build-arg NEXT_PUBLIC_API_URL=https://api.threadia.click \
  --tag "$FRONTEND_REPOSITORY:$IMAGE_TAG" frontend
```

`NEXT_PUBLIC_*` 값은 browser bundle에 포함되므로 secret을 전달하면 안 된다.

## 4. Fixed SSM manifest contract

수동 `ecr-compose-ssm-deploy` workflow는 frontend와 backend image를 각각 `linux/amd64` immutable digest로 push한 뒤 release manifest를 versioned artifact bucket에 저장한다. SSM에는 아래 형식의 `ReleaseManifestRef` 하나만 전달한다.

```text
releases/<40-hex-commit>/<run>-<attempt>/dual-image-release-<40-hex-commit>-<run>-<attempt>.env@<S3-VersionId>
```

`DualImageReleaseCommandDocument`는 `ReleaseManifestRef`에 `interpolationType: ENV_VAR`와 이 형식의 allowlist pattern을 적용한다. document는 고정 artifact bucket과 root-owned `/usr/local/lib/threadia-compose/deploy-release-manifest`만 호출한다. bucket, shell text, Compose file, image reference, host path는 input으로 받지 않는다. wrapper는 S3 `VersionId`를 반드시 사용하고 root-only temporary file로 내려받은 뒤 기존 manifest parser를 호출한다.

GitHub Environment에는 `THREADIA_ECR_FRONTEND_REPOSITORY`, `THREADIA_ECR_BACKEND_REPOSITORY`, `THREADIA_ARTIFACT_BUCKET`, `THREADIA_ECR_COMPOSE_DUAL_IMAGE_SSM_DOCUMENT_NAME`, `THREADIA_ECR_COMPOSE_DUAL_IMAGE_SSM_DOCUMENT_VERSION`가 필요하다. workflow의 artifact bucket manifest upload/new document invocation 권한과 host wrapper 설치은 승인된 AWS 운영 작업이다.

## 5. Host 설치와 배포

root가 검토한 revision의 파일을 아래 위치에 설치한다.

| Repository source | EC2 path | 권한 |
|---|---|---|
| `compose.dual-image.production.yml` | `/opt/threadia-compose/compose.dual-image.production.yml` | root:root, group/other non-writable |
| `scripts/ecr-compose-deploy/deploy-dual-release` | `/usr/local/lib/threadia-compose/deploy-dual-release` | root:root, executable, group/other non-writable |
| `scripts/ecr-compose-deploy/deploy-release-manifest` | `/usr/local/lib/threadia-compose/deploy-release-manifest` | root:root, executable, group/other non-writable |
| `scripts/ecr-compose-deploy/release-manifest-transport.example.env`의 승인된 값 | `/etc/threadia-compose/release-manifest-transport.env` | root:root, `0600` |
| runtime env | `/etc/threadia-compose/runtime.env` | root:root, `0600` |
| `scripts/ecr-compose-deploy/attachment-storage-runtime.example.env`의 승인된 non-secret 값 | `/etc/threadia-compose/runtime.env` | root:root, `0600` |

runtime env에는 backend의 `THREADIA_DB_URL`, `THREADIA_DB_USERNAME`, `THREADIA_DB_PASSWORD`, `THREADIA_JWT_SECRET`, `THREADIA_COOKIE_SECURE`, `THREADIA_CORS_ALLOWED_ORIGIN`과 approved attachment storage non-secret keys를 둔다. attachment storage는 dedicated private bucket과 host workload role의 AWS default credential chain을 사용한다. static access key, secret key, endpoint override는 local MinIO 전용이며 public AWS runtime env에 두지 않는다. OAuth redirect와 password-reset URL도 production domain을 사용한다. 실제 secret, digest manifest, runtime env는 repository에 commit하지 않는다.

SSM transport wrapper는 `ReleaseManifestRef` 하나를 인수로 받고, local root-only transport config의 고정 artifact bucket에서 해당 S3 object version을 private temporary file로 내려받아 `deploy-dual-release`에 전달한다. manifest key, version ID, bucket 설정은 shell source로 해석하지 않으며, wrapper는 `releases/<commit>/<run>-<attempt>/dual-image-release-<release-id>.env@<version-id>` 이외의 입력을 거부한다.

`deploy-dual-release` 자체는 root에서 `ReleaseId FrontendImageRef BackendImageRef` 순서의 세 인수만 받는다.

```bash
/usr/local/lib/threadia-compose/deploy-dual-release \
  2026.07.16-001 \
  123456789012.dkr.ecr.ap-northeast-2.amazonaws.com/threadia-frontend@sha256:<64-lowercase-hex> \
  123456789012.dkr.ecr.ap-northeast-2.amazonaws.com/threadia-backend@sha256:<64-lowercase-hex>
```

script는 `x86_64` host, Docker Compose plugin, AWS CLI와 root-owned file permission을 확인한다. registry별로 temporary Docker config에서 ECR login한 후 two-image pull과 force recreate를 수행한다. frontend `/`와 backend `/actuator/health`가 모두 제한 시간 안에 성공해야 한다.

## 6. Atomic state 및 rollback

active identity는 `/var/lib/threadia-compose-dual/current-release.env`에 `THREADIA_RELEASE_ID`, `THREADIA_FRONTEND_IMAGE`, `THREADIA_BACKEND_IMAGE`를 하나의 manifest로 기록한다. 이 파일은 두 container가 candidate digest와 일치하고 두 health check가 통과한 후에만 `mktemp`와 `mv`로 교체된다.

candidate가 실패하면 script는 current release manifest의 frontend/backend pair를 함께 pull·recreate·health check한다. 따라서 active state는 candidate의 한쪽 service만 가리키지 않으며, rollback도 이전 pair 단위다. 이전 state가 없는 최초 배포가 실패하면 candidate container를 중지·제거하고 실패로 종료한다. 현재 running service와 state manifest가 불일치하면 자동 추측하지 않고 수동 reconciliation을 요구한다.

## 7. One-time migration and route safety

최초 전환은 승인된 운영자가 수행한다. 별도 dual-image host에서 frontend/backend health를 확인한 뒤 public reverse proxy upstream 또는 DNS를 명시적으로 전환하고 public smoke test를 실행한다. 문제 발생 시 edge route를 기존 backend-only host로 되돌린다.

같은 host를 재사용해야 하면 운영자가 legacy `app`과 `current-image` state를 의도적으로 중지·정리한 뒤 dual-image host files를 설치한다. 이 helper는 그 정리나 route 전환을 수행하지 않는다.

## 8. 운영 경계

- 이 repository 변경은 AWS resource, DNS, Nginx, ACM, GitHub Environment/variable 또는 workflow 실행을 변경하지 않는다.
- Nginx server block은 `threadia.click`과 `api.threadia.click`을 각각 위 localhost port로 proxy하고 TLS를 별도로 종료해야 한다.
- Docker daemon access는 root-equivalent privilege다. release manifest와 runtime env의 read/write 권한을 최소화한다.
- backend DB migration과 public edge configuration은 이 release helper의 범위 밖이다.
- attachment bucket/IAM/CORS/one-time additive SQL과 browser presigned transfer smoke는 [Public Compose Attachment Storage Runbook](../00-project/attachment-storage-public-compose-runbook.md)의 별도 approval gate를 따른다.

## 변경 이력

| 날짜 | 내용 |
|---|---|
| 2026-07-21 | dedicated private attachment bucket의 public Compose runtime contract와 별도 operator runbook 경계를 추가했다. |
