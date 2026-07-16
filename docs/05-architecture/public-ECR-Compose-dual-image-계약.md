> **작성자**: Threadia 팀\
> **작성일**: 2026-07-16\
> **최종 수정**: 2026-07-16\
> **버전**: v1.1\
> **상태**: 검토 중
> **태그**: ecr, ssm, docker-compose, nextjs

# Public ECR Compose dual-image 계약

이 경로는 `ecr-compose-nonprod` public host 전용이며 `restricted-*` delivery를 사용하거나 변경하지 않는다. workflow는 paired frontend/backend digest와 fixed SSM document만 사용한다.

필수 public AWS resource는 frontend/backend ECR repositories, exact public host instance, `dual-image-ssm-document.yaml` 기반 Command document와 approved version, 두 repository pull 권한의 host role, 두 repository push/describe와 exact SSM invoke/status-read 권한의 public GitHub OIDC role이다.

필수 Environment variables는 `AWS_REGION`, `AWS_ROLE_TO_ASSUME`, `THREADIA_ECR_REGISTRY`, `THREADIA_ECR_FRONTEND_REPOSITORY`, `THREADIA_ECR_BACKEND_REPOSITORY`, `THREADIA_ECR_COMPOSE_INSTANCE_ID`, `THREADIA_ECR_COMPOSE_DUAL_IMAGE_SSM_DOCUMENT_NAME`, `THREADIA_ECR_COMPOSE_DUAL_IMAGE_SSM_DOCUMENT_VERSION`이다.

SSM document는 `FrontendImageRef`에 `940507692158.dkr.ecr.ap-northeast-2.amazonaws.com/threadia-frontend@sha256:<64-hex>`만, `BackendImageRef`에 `940507692158.dkr.ecr.ap-northeast-2.amazonaws.com/threadia-backend@sha256:<64-hex>`만 수락한다. 두 parameter는 `ENV_VAR` interpolation을 유지하며 mutable tag와 서로 바뀐 repository를 거부한다. SSM completion 조회는 이미지 publish와 command 생성 output을 기록하는 `release` step의 `command_id`를 사용한다.

Host에는 root-owned `compose.dual-image.production.yml`, `deploy-dual-release`, mode `0600` runtime env가 필요하다. helper는 기존 public backend-only `app` 또는 `current-image` state를 자동으로 중단하지 않고 실패한다. approved manual migration 후에만 dual host를 시작한다.
