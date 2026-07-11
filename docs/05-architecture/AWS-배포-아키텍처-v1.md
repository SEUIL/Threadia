> **작성자**: Threadia 팀
> **작성일**: 2026-07-11
> **최종 수정**: 2026-07-11
> **버전**: v0.1 AWS 배포 준비 proposal
> **상태**: 검토 중
> **태그**: architecture, aws, deploy, ec2, rds, route53, github-actions

# AWS 배포 아키텍처 v1

## 1. 문서 목적

이 문서는 Threadia MVP의 첫 AWS 배포를 준비하기 위한 v1 architecture proposal이다. ADR이나 최종 결정 문서가 아니며, 실제 AWS resource 생성과 배포 실행은 별도 승인 후 진행한다.

기존 후보 비교는 [`배포-호스팅-아키텍처-연구.md`](./배포-호스팅-아키텍처-연구.md)를 참조한다. 이번 문서는 Sprint 2 Day 6, 2026-07-21 CI/Deploy/QA gate에서 사용할 실행 준비 관점의 구조를 정리한다.

## 2. 현재 repo stack

| 영역 | 기준 |
|---|---|
| Backend | Spring Boot 3.5.16, Java 17, `backend/`, MySQL connector, JWT, OAuth2 client, mail |
| Frontend | Next.js 16.2.9, `frontend/`, `NEXT_PUBLIC_API_URL`로 API base URL 주입 |
| Docs site | Next.js 16.2.9, `docs-site/` |
| DB | MySQL 8.4 local compose, production 후보는 RDS MySQL |
| CI | repo-local GitHub Actions CI만 추가하고 deployment workflow는 후속으로 분리 |

## 3. First-pass MVP 권장 경로

Threadia 첫 배포는 운영 복잡도를 낮추면서 AWS 학습 목표를 충족하는 경로를 우선한다.

```text
User
  |
  | HTTPS
  v
Route 53
  |-- app.theadia.com  -> Frontend hosting 후보: AWS Amplify Hosting
  |-- api.theadia.com  -> EC2 Nginx 443/80 -> Docker container: Spring Boot 8080
  |-- docs.theadia.com -> Docs-site hosting 후보: Amplify 또는 후속 결정

Backend EC2 -> RDS MySQL 8.x
GitHub Actions CI -> future deploy with AWS OIDC role
```

| 구성요소 | 1차 권장 | 이유 |
|---|---|---|
| Domain/DNS | Route 53 hosted zone + custom domain | AWS 내부 DNS와 ACM validation을 함께 관리하기 쉽다 |
| Frontend | AWS Amplify Hosting 우선 검토 | Next.js SSR/SSG hosting과 Git 연동이 단순하다 |
| Backend | EC2 + Docker + Nginx | Spring Boot container 운영과 Nginx TLS/proxy 학습 범위가 명확하다 |
| DB | RDS MySQL | local MySQL 8.4와 운영 DB를 분리하고 backup/monitoring 기반을 확보한다 |
| CD | GitHub Actions OIDC 기반 후속 deploy | 장기 access key를 secret으로 저장하지 않는 방향이다 |

## 4. Backend EC2 구성 proposal

| 항목 | 권장 기준 |
|---|---|
| Runtime | Docker container로 Spring Boot jar 실행 |
| Reverse proxy | Nginx가 80/443을 받고 container `8080`으로 proxy한다 |
| TLS | ACM 또는 EC2/Nginx 인증서 방식은 도메인·CloudFront 사용 여부 확정 후 결정한다 |
| Health check | `/actuator/health`를 우선 후보로 둔다 |
| Config | `.env` 또는 process env로 주입하되 secret 값은 repository와 log에 남기지 않는다 |
| Deploy script | `scripts/deploy/deploy-backend-ec2.example.sh`를 skeleton으로 두고 dry-run 기본값을 유지한다 |

Nginx 설정, Dockerfile, systemd unit, compose 파일은 이번 준비 범위에서 추가하지 않는다. 실제 EC2 운영 방식이 확정된 뒤 최소 단위로 추가한다.

## 5. RDS MySQL 구성 proposal

| 항목 | 권장 기준 |
|---|---|
| Engine | MySQL 8.x |
| Network | Backend EC2와 같은 VPC, public exposure 금지 |
| Credential | Secrets Manager 또는 EC2 환경 변수 주입 후보. repository에는 기록하지 않는다 |
| Migration | `db/schema.sql` 기준과 backend test strategy 정합성을 확인한 뒤 적용한다 |
| Backup | automated backup과 snapshot 정책은 production 전 별도 결정한다 |

Local `compose.mysql.yml`은 개발 검증용이며 운영 DB와 연결하지 않는다.

## 6. Frontend와 docs-site hosting proposal

Frontend는 `app.theadia.com`, backend API는 `api.theadia.com`을 우선 후보로 둔다. docs-site는 필요 시 `docs.theadia.com`을 후보로 둔다.

| 대상 | 1차 후보 | 보류 지점 |
|---|---|---|
| Frontend | AWS Amplify Hosting | Next.js 16 지원 세부 설정과 build image Node 22 확인 필요 |
| Docs site | Amplify 또는 후속 docs hosting | 팀 공유 방식이 GitHub Pages, Amplify, 정적 artifact 중 어디인지 미확정이다 |
| Backend API | EC2 + Nginx | TLS termination과 certificate 위치를 확정해야 한다 |

## 7. CORS, OAuth, cookie 영향

도메인이 분리되면 frontend와 backend의 origin은 다음처럼 분리된다.

| 항목 | 후보 값 |
|---|---|
| Frontend origin | `https://app.theadia.com` |
| Backend origin | `https://api.theadia.com` |
| CORS allowed origin | `https://app.theadia.com` |
| OAuth redirect URI | `https://api.theadia.com/api/v1/auth/oauth2/callback/google` 후보 |
| Frontend API URL | `https://api.theadia.com` |

사용자 제시 domain candidate는 `theadia.com`이지만 기존 문서에는 `threadia.com` 예시가 남아 있다. DNS, OAuth redirect URI, CORS, cookie, ACM certificate를 변경하기 전 최종 spelling을 확인해야 한다.

Refresh token cookie는 HTTPS 운영 환경에서 `THREADIA_COOKIE_SECURE=true`가 필수다. cross-subdomain cookie 정책은 실제 auth flow 검증 후 조정한다.

## 8. GitHub Actions OIDC 후속 설계

이번 pass에서는 deployment workflow를 만들지 않는다. 후속 deploy job은 다음 원칙을 따른다.

| 항목 | 기준 |
|---|---|
| Permission | `id-token: write`, `contents: read` |
| Credential action | `aws-actions/configure-aws-credentials` |
| Secret/variable | `AWS_REGION`, `AWS_ROLE_TO_ASSUME` |
| Approval gate | GitHub Environment protection rule 사용 |
| Production secrets | Environment secrets 우선 |
| Non-prod shared values | repository secrets 또는 variables 후보 |

장기 AWS access key를 repository secret에 저장하는 방식은 기본안으로 삼지 않는다.

## 9. 대안과 제외 범위

| 대안 | 위치 | 이번 판단 |
|---|---|---|
| AWS App Runner | Backend container managed hosting | 단순하지만 EC2/Nginx 학습과 운영 제어 범위가 줄어 대안으로 둔다 |
| ECS/Fargate | Backend container orchestration | MVP 1차 배포에는 운영 복잡도가 높아 대안으로 둔다 |
| 단일 EC2 통합 배포 | Frontend/Backend/Nginx를 한 EC2에 배치 | CORS 단순화 장점은 있으나 Next.js 운영 방식 확정 전 대안으로 둔다 |
| Vercel | Frontend hosting | DX는 좋지만 AWS 중심 운영 목표에서는 1차 기본안으로 두지 않는다 |

## 10. 승인 필요 작업

- Route 53 domain registration 또는 hosted zone 생성이다.
- NS delegation과 DNS record 변경이다.
- ACM certificate 발급과 DNS validation이다.
- EC2, RDS, security group, IAM role 생성이다.
- GitHub Secrets/Environments/branch protection/required check 설정이다.
- deployment workflow 추가와 실제 배포 실행이다.

## 11. 참조 문서

| 문서 | 목적 |
|---|---|
| [`AWS-운영-환경변수-목록.md`](./AWS-운영-환경변수-목록.md) | 운영 환경 변수와 secret 분류 |
| [`Route53-커스텀-도메인-가이드.md`](./Route53-커스텀-도메인-가이드.md) | domain, hosted zone, ACM, HTTPS 준비 기준 |
| [`../00-project/sprint-운영/sprint-2-CI-Deploy-준비계획.md`](../00-project/sprint-운영/sprint-2-CI-Deploy-준비계획.md) | Sprint 2 Day 6 운영 계획 |
| [`배포-호스팅-아키텍처-연구.md`](./배포-호스팅-아키텍처-연구.md) | 기존 hosting 후보 비교 |
