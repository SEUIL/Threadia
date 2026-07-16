> **작성자**: Threadia 팀
> **작성일**: 2026-07-16
> **최종 수정**: 2026-07-16
> **버전**: v1.2
> **상태**: 검토 중
> **태그**: aws, ecr, docker-compose, ssm, oidc, deployment

# ECR Compose SSM 수동 배포 가이드

## 1. 목적과 범위

이 문서는 Spring Boot backend를 immutable ECR digest로 배포하는 첫 번째 수동 운영 경로를 정의한다. GitHub Actions는 OIDC로 한정된 AWS role을 assume하고 ECR image를 push한 뒤, SSM을 command transport로만 사용해 EC2의 고정 host script를 실행한다.

기존 `.github/workflows/restricted-ssm-release.yml`과 `scripts/ssm-delivery/*`의 JAR/S3/SSM 경로는 legacy source 배포 및 수동 fallback으로 계속 유지한다. legacy workflow는 실제 legacy deploy input 변경에만 자동 실행되며 workflow 파일만 변경한 main merge로는 실행되지 않는다. 이 문서는 AWS resource 생성, GitHub variable 설정, EC2 bootstrap, deploy 실행을 수행하지 않는다.

```text
workflow_dispatch
  → OIDC role → ECR repository@sha256:digest
  → fixed SSM document version → fixed EC2 host script
  → Docker Compose app → external RDS
```

## 2. Repository 구성 계약

| 경로 | 책임 |
|---|---|
| `backend/Dockerfile` | Java 17 Spring Boot image를 build하고 non-root user로 실행한다. secret을 image layer에 넣지 않는다. |
| `compose.production.yml` | 단일 `app` service를 localhost `127.0.0.1:8080`에만 bind하고 외부 RDS 환경변수를 전달한다. root-only runtime env file은 `env_file`로 전달하므로 미설정 optional key를 empty string으로 만들지 않는다. MySQL service는 포함하지 않는다. |
| `scripts/ecr-compose-deploy/deploy-image` | full ECR `repository@sha256:<64-hex>` reference만 수락하고, Compose 전용 lock과 Compose state를 확인한 뒤 health failure 시 직전 current digest로 복구한다. |
| `.github/workflows/ecr-compose-ssm-deploy.yml` | `workflow_dispatch`로만 실행되고 `main` 및 `ecr-compose-nonprod` Environment에서 normal build/publish 또는 explicit rollback validation 후 SSM completion을 확인한다. |

`compose.mysql.yml`은 local MySQL 용도이며 production Compose 경로가 참조하지 않는다.

## 3. EC2 수동 bootstrap 계약

배포 담당자는 EC2, instance profile, SSM Agent, Docker Engine과 Docker Compose plugin을 직접 준비한다. SSM document는 고정된 root-owned script만 호출해야 하며, 임의 shell text, file path, service name을 workflow input으로 받으면 안 된다.

host에는 검토된 repository revision에서 아래 두 파일을 root-owned 경로로 수동 설치한다.

| 원본 | EC2 설치 경로 | 권한 |
|---|---|---|
| `compose.production.yml` | `/opt/threadia-compose/compose.production.yml` | root 소유, 읽기 전용 |
| `scripts/ecr-compose-deploy/deploy-image` | `/usr/local/lib/threadia-compose/deploy-image` | root 소유, 실행 가능 |

runtime 설정은 `/etc/threadia-compose/runtime.env`에만 둔다. 이 파일은 root 소유 `0600`이어야 하며 repository, Docker image, GitHub Actions, SSM parameter와 command output에 넣지 않는다. Compose file과 host script도 root:root 소유이고 group/other write가 없어야 한다. 필수 key는 `THREADIA_DB_URL`, `THREADIA_DB_USERNAME`, `THREADIA_DB_PASSWORD`, `THREADIA_JWT_SECRET`, `THREADIA_COOKIE_SECURE`, `THREADIA_CORS_ALLOWED_ORIGIN`이다. OAuth와 mail key는 해당 기능을 운영할 때만 추가하며, 없으면 container environment에 주입되지 않는다. `MANAGEMENT_HEALTH_MAIL_ENABLED=false`는 Compose가 고정 주입하여 deployment health가 mail 설정에 의존하지 않게 한다.

EC2 instance role은 해당 ECR repository의 pull에 필요한 최소 권한만 갖는다. host script는 instance role로 ECR login token을 얻고 temporary Docker config를 종료 시 삭제하며 long-lived credential을 저장하지 않는다. 현재 production Compose host는 `x86_64`를 요구하며 workflow도 image를 `linux/amd64`로 build한다.

## 4. Fixed SSM command 계약

사용자가 생성하는 SSM document는 하나의 `ImageRef` parameter만 받아 아래 고정 동작을 수행해야 한다.

1. `/usr/local/lib/threadia-compose/deploy-image`를 root 권한으로 실행한다.
2. `ImageRef`를 script의 유일한 positional argument로 전달한다.
3. image reference, 경로, Compose service, shell command를 추가 input으로 허용하지 않는다.

host script는 full ECR `repository@sha256:<64-hex>` 형식만 허용한다. tag, short digest, registry 없는 repository, 여러 argument는 거부한다. `/var/lib/threadia-compose/current-image`에 active digest만 저장하므로 source control과 분리된다. 실행 중인 Compose `app`이 current-image state 없이 존재하거나, container image가 current-image와 다르면 script는 자동 추측하지 않고 수동 reconciliation을 요구한다.

candidate image를 pull하고 Compose를 재생성한 뒤 `http://127.0.0.1:8080/actuator/health`가 제한 시간 안에 성공해야 한다. 실패하면 직전 current digest를 pull·재기동·health check하고, 복구 결과와 관계없이 candidate 배포는 실패로 종료한다. Compose host는 `/run/lock/threadia-compose-deploy.lock`만 non-blocking으로 획득해 동일 Compose 배포만 직렬화한다. legacy JAR host와는 별도 EC2이므로 release lock을 공유하지 않는다.

SSM document는 parameter interpolation을 shell text에 직접 삽입하지 않는다. `ImageRef`에 `interpolationType: ENV_VAR`와 host script와 같은 `allowedPattern`을 지정하고, SSM Agent가 제공하는 `SSM_ImageRef`를 하나의 double-quoted argument로 전달한다. 다음은 account, domain, secret이 없는 최소 계약 fragment다.

```yaml
schemaVersion: '2.2'
parameters:
  ImageRef:
    type: String
    interpolationType: ENV_VAR
    allowedPattern: '^[0-9]{12}\.dkr\.ecr\.[a-z]{2}(-[a-z0-9]+)+-[0-9]+\.amazonaws\.com/[a-z0-9]+([._/-][a-z0-9]+)*@sha256:[a-f0-9]{64}$'
mainSteps:
  - action: aws:runShellScript
    name: deployImage
    inputs:
      runCommand:
        - 'exec /usr/local/lib/threadia-compose/deploy-image "$SSM_ImageRef"'
```

## 5. JAR fallback과 Compose의 제어된 전환

legacy JAR runtime과 Compose `app`은 서로 다른 EC2에서 실행한다. 각 host는 자신의 `127.0.0.1:8080`을 사용할 수 있으므로 port 충돌, systemd unit 정지, release lock 공유를 전제로 하지 않는다. legacy JAR/S3/SSM source는 유지하며 이 절차로 수정하지 않는다.

JAR fallback에서 Compose로 최초 전환할 때 사용자는 다음 순서를 수동으로 수행한다.

1. Compose public host에서 ECR Compose workflow를 수동 실행한다.
2. Compose host의 local actuator health와 해당 host의 Nginx proxy health를 확인한다. 이 단계에서는 legacy JAR host의 service와 workflow를 변경하지 않는다.
3. DNS 또는 public Nginx upstream을 Compose public host로 전환하고, 실제 public endpoint health와 주요 요청을 확인한다.
4. legacy JAR/S3/SSM release 경로는 deployment fallback으로 유지한다. Compose host에서 새 digest 배포가 실패하면 host script는 직전 Compose digest를 먼저 복구하며, DNS/Nginx traffic은 Compose public host를 계속 가리킨다.

legacy JAR host는 private host이므로 public DNS 또는 public Nginx upstream을 legacy JAR host로 변경하지 않는다. public traffic fallback은 별도로 승인된 public edge route가 설계·구축되기 전까지 범위 밖이다.

## 6. GitHub Actions 수동 운영 절차

workflow는 `refs/heads/main`에서만 job을 실행하고, hard-coded non-secret Environment `ecr-compose-nonprod`를 사용한다. 사용자는 GitHub에서 이 Environment에 deployment protection rule과 main branch policy를 수동 설정해야 한다. workflow에는 아래 GitHub Actions repository variable이 필요하다. 값은 사용자가 AWS와 GitHub에서 수동으로 설정하며 이 repository에는 기록하지 않는다.

| Variable | 용도 |
|---|---|
| `AWS_REGION` | ECR과 SSM API region |
| `AWS_ROLE_TO_ASSUME` | OIDC가 assume할 deploy role |
| `THREADIA_ECR_REGISTRY` | ECR registry endpoint |
| `THREADIA_ECR_REPOSITORY` | backend image repository 이름 |
| `THREADIA_ECR_COMPOSE_INSTANCE_ID` | 고정 EC2 instance 식별자 |
| `THREADIA_ECR_COMPOSE_SSM_DOCUMENT_NAME` | 고정 deploy SSM document 이름 |
| `THREADIA_ECR_COMPOSE_SSM_DOCUMENT_VERSION` | 고정 deploy SSM document version. positive integer만 허용하며, 다음에 생성될 AWS document version은 `2`이다. |

일반 배포는 Actions 화면에서 `ecr-compose-ssm-deploy` workflow를 수동 실행하고 `rollback_digest`를 비운다. workflow는 checkout, backend test, `linux/amd64` image build, immutable tag push를 수행하고 ECR이 반환한 digest를 SSM document에 전달한다.

rollback은 Actions 화면에서 검증된 이전 `sha256:<64-hex>` digest를 `rollback_digest`에 입력해 수동 실행한다. rollback job은 digest format과 ECR object 존재만 확인한 뒤 SSM을 호출하며 checkout, Java setup, test, build, push를 수행하지 않는다. rollback은 tag를 재사용하지 않으며 같은 host script의 validation, health check, state persistence를 사용한다.

legacy main-push JAR workflow와 Compose workflow는 별도 EC2 host를 대상으로 독립적으로 실행된다. public traffic의 활성 runtime은 workflow concurrency가 아니라 Compose public host의 DNS 또는 Nginx upstream으로 결정한다. private legacy JAR host를 public DNS 또는 public Nginx upstream 대상으로 사용하지 않으며, public traffic fallback은 별도로 승인된 public edge route가 설계·구축되기 전까지 범위 밖이다. legacy JAR/S3/SSM release 경로는 deployment fallback으로 유지한다.

## 7. 보안과 운영 경계

- workflow는 `workflow_dispatch`만 사용한다. push, pull request, schedule trigger를 추가하지 않는다.
- OIDC role은 ECR push/describe와 해당 SSM document의 command invoke 및 status read에 필요한 최소 권한만 갖는다.
- deploy workflow는 `THREADIA_ECR_COMPOSE_SSM_DOCUMENT_VERSION`를 non-secret GitHub Environment variable로 읽고 positive integer인지 검증한 뒤, `aws ssm send-command --document-version`으로 고정 document version만 호출한다.
- secret은 GitHub Actions secret/variable, workflow env, Docker build argument, image layer, tracked env file, SSM command parameter와 log에 넣지 않는다.
- SSM은 SSH 대체 command transport일 뿐 artifact contract가 아니다. 배포 및 rollback identity는 ECR digest다.
- DB schema migration과 application S3 integration은 이 경로의 범위가 아니다.
- Docker daemon access 또는 `docker` group membership은 root-equivalent privilege다. runtime env와 deployment artifact에 접근할 수 있는 사용자를 최소화한다.

## 8. 운영 전 확인

- [ ] 사용자와 platform 담당자가 AWS resource와 IAM 최소 권한을 수동으로 생성·검토했다.
- [ ] 사용자가 EC2와 host script, Compose file, root-only runtime env를 수동 bootstrap했다.
- [ ] ECR pull과 external RDS network reachability를 host에서 별도 확인했다.
- [ ] SSM document가 고정 script와 하나의 `ImageRef` parameter만 사용한다.
- [ ] JAR fallback과 Compose `app`이 별도 EC2 host에서 정상인지 확인하고, public DNS 또는 Nginx upstream이 Compose public host만 가리키는지 확인했다.
- [ ] reverse proxy와 TLS는 `Route53-EIP-Nginx-Certbot-운영-가이드.md` 절차로 별도 준비했다.
- [ ] 기존 JAR/S3/SSM release 경로가 fallback으로 남아 있음을 확인했다.
- [ ] RDS는 private이고 public access가 disabled이며, RDS inbound는 EC2 security group에서만 허용한다.
- [ ] EC2 inbound는 80/443만 허용하고 public 8080과 broad SSH는 허용하지 않는다.
- [ ] `ecr-compose-nonprod` GitHub Environment protection과 main branch policy를 사용자가 설정했다.
