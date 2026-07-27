> **작성자**: Threadia 팀  
> **작성일**: 2026-07-16  
> **최종 수정**: 2026-07-16  
> **버전**: v1.0  
> **상태**: 검토 중  
> **태그**: ecr, docker-compose, ssm, deployment, nextjs

# Public ECR Compose dual-image 계약

## 1. 범위

이 계약은 public `ecr-compose-nonprod` host에서 Next.js SSR frontend와 Spring Boot backend를 immutable ECR digest pair로 실행한다. `restricted-*` S3/JAR/SSM delivery stack, IAM role, workflow, host는 사용하거나 수정하지 않는다.

`ecr-compose-ssm-deploy.yml`, `compose.production.yml`, `deploy-image`는 backend-only public host fallback으로 유지한다. dual host는 `compose.dual-image.production.yml`과 `deploy-dual-release`만 사용한다.

## 2. Repository와 host 계약

| Repository source | Public host path |
|---|---|
| `compose.dual-image.production.yml` | `/opt/threadia-compose/compose.dual-image.production.yml` |
| `scripts/ecr-compose-deploy/deploy-dual-release` | `/usr/local/lib/threadia-compose/deploy-dual-release` |
| `scripts/ecr-compose-deploy/dual-image-ssm-document.yaml` | public SSM Command document content |

모든 host file은 `root:root`, group/other non-writable여야 한다. runtime env는 `/etc/threadia-compose/runtime.env`, mode `0600`이며 기존 backend runtime key를 사용한다. frontend는 `127.0.0.1:3000`, backend는 `127.0.0.1:8080`만 bind한다.

`deploy-dual-release`는 `threadia-compose` project의 backend-only `app` container 또는 `/var/lib/threadia-compose/current-image`가 있으면 실패한다. 자동 migration·legacy stop·restricted host 접근은 하지 않는다. approved migration은 기존 public backend-only app을 별도로 중지·제거하고 state를 archive한 뒤 시작해야 한다.

## 3. Fixed SSM document

`dual-image-ssm-document.yaml`은 `ReleaseId`, `FrontendImageRef`, `BackendImageRef`만 받는다. 모든 parameter는 strict regex와 `interpolationType: ENV_VAR`를 사용하며 host helper를 고정 경로로 exec한다. workflow는 document name/version을 Environment variable에서 받고, 명령·path·service name을 input으로 받지 않는다.

## 4. Required AWS and GitHub configuration

AWS operator가 public path에 별도로 준비해야 하는 resource는 다음이다.

1. `threadia-frontend` ECR repository와 existing `threadia-backend` repository.
2. public host instance role의 두 repository pull (`ecr:GetAuthorizationToken`, image layer read) 권한.
3. GitHub `ecr-compose-nonprod` OIDC role의 두 repository push/describe, exact public dual SSM document의 `ssm:SendCommand`, exact instance resource, `ssm:GetCommandInvocation` 권한.
4. `dual-image-ssm-document.yaml` content로 생성한 public SSM Command document와 approved version.
5. public EC2 host의 Docker/Compose/SSM Agent, root-owned files, runtime env, Nginx/TLS reverse proxy.

Environment `ecr-compose-nonprod`에는 secret이 아닌 아래 variables가 필요하다.

| Variable | Purpose |
|---|---|
| `AWS_REGION` | ECR/SSM region |
| `AWS_ROLE_TO_ASSUME` | public ECR Compose OIDC role ARN |
| `THREADIA_ECR_REGISTRY` | registry endpoint |
| `THREADIA_ECR_FRONTEND_REPOSITORY` | frontend repository name |
| `THREADIA_ECR_BACKEND_REPOSITORY` | backend repository name |
| `THREADIA_ECR_COMPOSE_INSTANCE_ID` | exact public host instance ID |
| `THREADIA_ECR_COMPOSE_DUAL_IMAGE_SSM_DOCUMENT_NAME` | fixed public dual document name |
| `THREADIA_ECR_COMPOSE_DUAL_IMAGE_SSM_DOCUMENT_VERSION` | approved positive integer version |

`NEXT_PUBLIC_API_URL` is fixed to `https://api.threadia.click` during frontend image build. It is public browser configuration, not a secret.
