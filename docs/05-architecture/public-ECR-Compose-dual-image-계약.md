> **작성자**: Threadia 팀\
> **작성일**: 2026-07-16\
> **최종 수정**: 2026-07-17\
> **버전**: v1.3\
> **상태**: 검토 중
> **태그**: ecr, ssm, docker-compose, nextjs

# Public ECR Compose dual-image 계약

이 경로는 public Compose host의 dual-image release 전용이다. public workflow는 고정 SSM command로 dual-image release를 실행하지만, restricted SSM runtime delivery와는 별도 경로이며 public release는 restricted runtime 값을 전달하거나 변경하지 않는다.

workflow는 같은 revision에서 생성한 frontend와 backend 이미지를 하나의 release 단위로 publish하고, Compose helper는 두 image reference를 함께 받아 같은 revision의 frontend와 backend를 배포한다. 한 서비스만 다른 revision으로 배포하는 경로는 이 계약의 범위가 아니다.

host는 Compose definition, dual-image release helper, 그리고 root 소유의 `runtime.env`를 운영한다. public image release는 host에 이미 materialize된 `runtime.env`를 소비하며, runtime secret 또는 credential의 최신 값을 직접 조회하거나 파일에 기록하지 않는다.

public host의 runtime credential과 config materialization은 승인된 운영 담당자의 책임이다. approved secret store의 값만 변경하고 host `runtime.env`를 갱신하지 않으면 Compose runtime은 바뀌지 않는다. materialization 뒤에는 container recreate 또는 동일한 효과의 release를 수행해 변경된 runtime environment를 적용한다. restricted SSM runtime delivery는 restricted host에 별도로 적용되는 경로이며 public host의 runtime 값을 자동 갱신하지 않는다.

[`ecr-compose-dual-image-ssm-deploy.yml`](../../.github/workflows/ecr-compose-dual-image-ssm-deploy.yml), [`dual-image-ssm-document.yaml`](../../scripts/ecr-compose-deploy/dual-image-ssm-document.yaml), [`compose.dual-image.production.yml`](../../compose.dual-image.production.yml), [`deploy-dual-release`](../../scripts/ecr-compose-deploy/deploy-dual-release)는 public dual-image 배포 근거다. [`restricted-ssm-release.yml`](../../.github/workflows/restricted-ssm-release.yml), [`apply-release`](../../scripts/ssm-delivery/apply-release), [`threadia-backend.service`](../../scripts/ssm-delivery/threadia-backend.service)는 restricted runtime materialization과 restart 요구사항의 근거다. helper는 기존 단일-image 상태를 자동 전환하지 않고 실패하므로, dual-image 전환은 승인된 migration 뒤에만 수행한다.

## 변경 이력

| 버전 | 날짜 | 변경 내용 |
|---|---|---|
| v1.3 | 2026-07-17 | 실제 workflow, Compose definition, host helper를 코드 근거로 연결하고 public host runtime materialization과 restricted runtime delivery를 구분했다. |
| v1.2 | 2026-07-17 | public dual-image release와 restricted SSM runtime delivery를 분리하고, `runtime.env` materialization 및 container recreate 조건을 명시했다. |
| v1.1 | 2026-07-16 | public ECR Compose dual-image 계약을 정리했다. |
