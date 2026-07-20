> **작성자**: Threadia 팀
> **작성일**: 2026-07-16
> **최종 수정**: 2026-07-16
> **버전**: v1.0
> **상태**: 검토 중
> **태그**: deployment, ecr, ssm, migration, rollback, nextjs

# dual-image SSM transport migration

## 1. 목적

기존 backend 단일-image Compose 배포를 Next.js SSR frontend와 Spring Boot backend의 dual-image release로 전환하는 절차다. 이 문서는 AWS, GitHub Environment, DNS, Nginx, secret 값을 생성하거나 변경하지 않는다. 실제 전환은 승인된 운영자가 별도 변경 창에서 수행한다.

## 2. 전환 후 계약

GitHub Actions는 frontend와 backend를 각각 `linux/amd64` ECR digest로 publish하고, 세 key만 포함한 versioned S3 manifest를 만든다.

```properties
THREADIA_RELEASE_ID=<commit>-<run>-<attempt>
THREADIA_FRONTEND_IMAGE=<frontend-repository>@sha256:<digest>
THREADIA_BACKEND_IMAGE=<backend-repository>@sha256:<digest>
```

workflow는 `releases/<commit>/<run>-<attempt>/dual-image-release-<release-id>.env@<S3-VersionId>` 형식의 `ReleaseManifestRef`만 fixed SSM document에 전달한다. document는 `interpolationType: ENV_VAR`를 사용해 root-owned `/usr/local/lib/threadia-compose/deploy-release-manifest "$SSM_ReleaseManifestRef"`만 호출한다. image reference, bucket, Compose path, shell text를 SSM input으로 받지 않는다.

host wrapper는 `/etc/threadia-compose/release-manifest-transport.env`의 root-only `THREADIA_ARTIFACT_BUCKET`을 읽고, exact S3 `VersionId`로 manifest를 private temporary file에 내려받는다. 이후 기존 `deploy-release`가 image digest, permission, frontend `/`, backend `/actuator/health`, running container image를 검증하고 pair 단위 rollback을 수행한다.

## 3. 사전 조건

- S3 artifact bucket은 versioning과 server-side encryption을 사용하며, GitHub deploy role에는 manifest upload/read에 필요한 최소 권한만 있다.
- Compose EC2 instance role에는 해당 artifact bucket의 release manifest read와 두 ECR repository pull에 필요한 최소 권한만 있다.
- host에는 `compose.dual-image.production.yml`, `deploy-release`, `deploy-release-manifest`, root-only runtime env와 transport config가 설치되어 있다.
- GitHub Environment variable은 `THREADIA_ECR_FRONTEND_REPOSITORY`, `THREADIA_ECR_BACKEND_REPOSITORY`, `THREADIA_ARTIFACT_BUCKET`, `THREADIA_ECR_COMPOSE_DUAL_IMAGE_SSM_DOCUMENT_NAME`, `THREADIA_ECR_COMPOSE_DUAL_IMAGE_SSM_DOCUMENT_VERSION`을 제공한다.
- frontend production image는 public API URL을 build argument로 받는다. `NEXT_PUBLIC_*` 값은 browser bundle에 포함되므로 secret을 사용하지 않는다.

## 4. one-time migration 순서

1. 승인된 revision의 host files를 설치하고 ownership·mode를 확인한다. runtime env와 transport config는 `root:root`, `0600`이어야 한다.
2. 동일 EC2 재사용 시 legacy `app` container와 `/var/lib/threadia-compose/current-image`를 운영 절차에 따라 의도적으로 중지·정리한다. `deploy-release`는 이를 자동 수행하지 않는다.
3. dual-image workflow를 수동 실행해 candidate pair의 SSM completion을 확인한다. host에서는 frontend와 backend local health 및 active `current-release.env`를 확인한다.
4. public reverse proxy upstream 또는 DNS를 승인된 dual-image host로 명시적으로 전환한 뒤 public SSR page와 API 주요 흐름을 smoke test한다.
5. 전환 근거에는 manifest key/version, 두 image digest, health 결과와 route 전환 결과만 기록한다. secret, runtime env 값, credential, private endpoint는 기록하지 않는다.

## 5. 실패 및 rollback

candidate pair의 pull, frontend validation, Compose health 또는 running image 검증 중 하나라도 실패하면 `deploy-release`는 이전 `current-release.env`의 frontend/backend pair를 함께 복원한다. workflow는 실패로 종료되므로 candidate manifest가 active state가 되지 않는다.

첫 dual-image release에 이전 pair state가 없으면 candidate containers를 중지·제거하고 실패로 종료한다. public route를 이미 전환한 뒤 dual-image host 자체를 복구할 수 없으면, 승인된 edge 절차로 이전 stable public target을 복구한다. legacy helper, container, state file을 script가 추측하거나 자동으로 수정하지 않는다.

## 6. 검증 기준

- `bash -n scripts/ecr-compose-deploy/deploy-release-manifest scripts/ecr-compose-deploy/deploy-release`
- `docker compose -f compose.dual-image.production.yml config`를 runtime과 release manifest가 있는 승인된 host에서 실행한다.
- `frontend`에서 `npm ci`, `npm run lint`, `npm run build`를 실행한다.
- `backend`에서 `./gradlew test`를 실행한다.
- SSM command가 `Success`여도 public route 전환 전에는 host의 두 local health와 container image digest를 별도로 확인한다.

## 7. 관련 문서

- [동일 EC2 dual-image Compose 배포](./동일-EC2-dual-image-Compose-배포.md)
- [ECR Compose SSM 수동 배포 가이드](./ECR-Compose-SSM-수동-배포-가이드.md)
- [배포 환경 및 런타임 계약](./배포-환경-런타임-계약.md)
