> **작성자**: Threadia 팀
> **작성일**: 2026-07-11
> **최종 수정**: 2026-07-11
> **버전**: v0.1 Sprint 2 CI/Deploy 준비 초안
> **상태**: 작성 중
> **태그**: sprint-2, ci, deploy, qa, aws, operations

# Sprint 2 CI/Deploy 준비계획

## 1. 문서 목적

이 문서는 Sprint 2 Day 6, 2026-07-21의 `3.6 CI/Deploy/QA` gate를 준비하기 위한 repo-local CI/CD, AWS 배포 준비, 환경 변수 inventory, Route 53 도메인 검토, 안전한 배포 script skeleton 범위를 정리한다.

이 문서는 배포 확정 결정이나 인프라 생성 지시가 아니다. 확정 기준은 기존 canonical 문서인 [`docs/00-project/CI-기준.md`](../CI-기준.md), [`docs/00-project/검증-방법.md`](../검증-방법.md), [`docs/00-project/Definition-of-Done.md`](../Definition-of-Done.md)를 따른다.

## 2. 현재 repo 기준

| 영역 | 현재 기준 | 이번 준비 범위 |
|---|---|---|
| Backend | Spring Boot 3.5.16, Java 17, `backend/`, Gradle wrapper | GitHub Actions에서 `./gradlew test`를 실행한다 |
| Frontend | Next.js 16.2.9, Node 22 대상, `frontend/` | `npm ci`, `npm run lint`, `npm run build`를 실행한다 |
| Docs site | Next.js 16.2.9, Node 22 대상, `docs-site/` | `npm ci`, `npm run lint`, `npm run build`를 실행한다 |
| DB | MySQL 8.4 local compose, `compose.mysql.yml` | CI에서는 실제 DB password를 주입하지 않고 안전한 dummy/test 환경만 사용한다 |
| Workflow | CI workflow 3종과 수동 readiness validation workflow | CI workflow 3종과 repository test/build 전용 수동 readiness validation workflow를 운영하고 actual deployment workflow는 만들지 않는다 |

## 3. Sprint 2 Day 6 목표

| 날짜 | WBS | 목표 산출물 | 완료 기준 |
|---|---|---|---|
| 2026-07-21 | 3.6 CI/Deploy/QA | Backend/Frontend/docs-site CI baseline, AWS 배포 준비 문서, 환경 변수 inventory, Route 53 domain guide, safe deployment script skeleton | PR 또는 push에서 CI check가 실행 가능한 YAML이 있고, 실제 배포·인프라 변경은 승인 필요 follow-up으로 남아 있다 |

## 4. 이번 작업에서 추가하는 repo-local 산출물

| 파일 | 목적 | 운영 기준 |
|---|---|---|
| `.github/workflows/backend-ci.yml` | backend 변경 PR/push의 최소 test 자동화 | `backend`, `db`, workflow 변경에만 반응한다 |
| `.github/workflows/frontend-ci.yml` | frontend lint/build 자동화 | `frontend`와 해당 workflow 변경에만 반응한다 |
| `.github/workflows/docs-site-ci.yml` | docs-site와 canonical docs 변경 확인 | `docs-site`, `docs/00-project`, `docs/02-requirements`, `docs/03-design`, `docs/04-api`, `docs/05-architecture`, 해당 workflow 변경에 반응한다 |
| `.github/workflows/pre-deploy-readiness.yml` | 선택 ref의 수동 repository readiness validation | GitHub-hosted test/build만 수행하며 deployment와 외부 인프라 접근을 하지 않는다 |
| `docs/00-project/배포-준비도-수동-점검-Runbook.md` | 수동 readiness와 local follow-up 절차 | GitHub test/build와 local secret/env 및 dry-run 점검을 분리한다 |
| `.env.example` | repository root/backend/local compose용 placeholder inventory | secret 값 없이 변수명과 placeholder만 제공한다 |
| `frontend/.env.example` | frontend public API URL placeholder | `NEXT_PUBLIC_API_URL`만 둔다 |
| `scripts/deploy/README.md` | script 사용 전제와 승인 gate 정리 | 실제 배포 실행 전 승인 필요를 명시한다 |
| `scripts/deploy/check-env.sh` | env var 존재 여부 점검 | 값은 출력하지 않고 변수명과 상태만 출력한다 |
| `scripts/deploy/build-local-artifacts.sh` | local build command 묶음 | backend/frontend/docs-site/all group을 지원한다 |
| `scripts/deploy/deploy-backend-ec2.example.sh` | EC2 배포 skeleton | 기본은 dry-run이며 `THREADIA_DEPLOY_EXECUTE=true`일 때만 원격 명령을 실행한다 |

## 5. 승인 필요 follow-up

다음 항목은 이번 준비 범위에서 실행하지 않는다. 실행 전 팀 승인과 별도 작업 기록이 필요하다.

1. AWS resource 생성 또는 수정이다.
2. Route 53 hosted zone 생성, domain purchase, NS delegation, DNS record 변경이다.
3. ACM certificate 발급 또는 DNS validation record 추가다.
4. GitHub Secrets, GitHub Environments, environment protection rule 설정이다.
5. branch protection, required status checks, ruleset 변경이다.
6. deployment workflow 추가와 실제 deployment 실행이다.
7. production DB migration, RDS endpoint 연결, 운영 secret 주입이다.

## 6. Domain 정책

최종 domain은 이번 범위에서 선택하지 않는다. 최종 domain은 `threadia`를 포함해야 하며, 구체적인 root domain과 subdomain naming은 별도 승인 follow-up에서 확정한다.

DNS, OAuth redirect URI, CORS 허용 origin, cookie domain, ACM certificate, GitHub custom domain 설정은 모두 외부 설정이므로 승인 필요 follow-up으로 유지한다.

## 7. CI 운영 기준

| CI | Trigger | 실행 명령 | 배포 여부 |
|---|---|---|---|
| Backend CI | `pull_request`, `push` to `main`/`dev`; `backend/**`, `db/**`, backend workflow 변경 | `./gradlew test` in `backend` | 배포하지 않는다 |
| Frontend CI | `pull_request`, `push` to `main`/`dev`; `frontend/**`, frontend workflow 변경 | `npm ci`, `npm run lint`, `npm run build` in `frontend` | 배포하지 않는다 |
| Docs-site CI | `pull_request`, `push` to `main`/`dev`; `docs-site/**`, canonical docs, docs-site workflow 변경 | `npm ci`, `npm run lint`, `npm run build` in `docs-site` | 배포하지 않는다 |
| Manual readiness validation | `workflow_dispatch`; GitHub UI에서 선택한 ref | backend test, frontend lint/build, docs-site lint/build | 배포하지 않는다 |

Backend CI의 JWT 값은 public test placeholder만 사용한다. 실제 `THREADIA_DB_PASSWORD`, OAuth secret, mail password, AWS credential은 workflow YAML에 넣지 않는다.

## 8. 향후 deployment workflow 설계 메모

향후 GitHub Actions에서 AWS에 배포하려면 OIDC 방식을 우선 검토한다. deploy job에는 `permissions: id-token: write`, `contents: read`가 필요하고, `aws-actions/configure-aws-credentials`에서 `AWS_ROLE_TO_ASSUME`와 `AWS_REGION`을 사용한다.

운영 secret은 GitHub Environment secrets로 두고 production environment approval gate를 적용한다. repository secrets는 non-prod 또는 공통값에 한정한다. 이 설정은 이번 작업에서 생성하지 않는다.

## 9. Sprint 2 Day 6 점검 checklist

- [ ] Backend CI가 backend/db 변경 PR에서 실행된다.
- [ ] Frontend CI가 frontend 변경 PR에서 실행된다.
- [ ] Docs-site CI가 docs-site 또는 canonical docs 변경 PR에서 실행된다.
- [ ] Manual readiness validation workflow가 선택 ref에서 repository test/build만 실행한다.
- [ ] 수동 readiness Runbook이 local secret/env 점검과 EC2 skeleton dry-run follow-up을 분리한다.
- [ ] `.env.example`, `frontend/.env.example`에 secret 값이 없다.
- [ ] `scripts/deploy/check-env.sh`가 variable value를 출력하지 않는다.
- [ ] `deploy-backend-ec2.example.sh`는 dry-run이 기본이다.
- [ ] 실제 AWS, DNS, GitHub settings 변경은 승인 필요 follow-up으로 분리되어 있다.

## 10. 참조 문서

| 문서 | 사용 목적 |
|---|---|
| [`docs/00-project/CI-기준.md`](../CI-기준.md) | CI 기준과 PR 검증 기준 |
| [`docs/00-project/검증-방법.md`](../검증-방법.md) | test/build/API/화면 확인 기준 |
| [`docs/00-project/Definition-of-Done.md`](../Definition-of-Done.md) | 완료 조건 기준 |
| [`docs/05-architecture/배포-호스팅-아키텍처-연구.md`](../../05-architecture/배포-호스팅-아키텍처-연구.md) | 기존 배포 후보 비교 |
| [`docs/05-architecture/AWS-배포-아키텍처-v1.md`](../../05-architecture/AWS-배포-아키텍처-v1.md) | 이번 Sprint 2 준비용 AWS 배포 proposal |
| [`docs/05-architecture/AWS-운영-환경변수-목록.md`](../../05-architecture/AWS-운영-환경변수-목록.md) | 운영 환경 변수 inventory |
| [`docs/05-architecture/Route53-커스텀-도메인-가이드.md`](../../05-architecture/Route53-커스텀-도메인-가이드.md) | Route 53 domain 준비 guide |
