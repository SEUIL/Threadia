# Threadia deploy script skeletons

> **작성자**: Threadia 팀  
> **작성일**: 2026-07-11  
> **최종 수정**: 2026-07-11  
> **버전**: v0.1 safe deploy skeleton  
> **상태**: 작성 중  
> **태그**: deploy, aws, ec2, script, safety

## 1. 목적

이 디렉터리는 Threadia Sprint 2 Day 6, 2026-07-21 CI/Deploy/QA gate를 준비하기 위한 안전한 local script skeleton을 둔다. 실제 AWS resource 생성, DNS 변경, GitHub Secrets/Environments 설정, branch protection 변경, deployment workflow 추가, production deploy 실행은 이 script 범위가 아니며 별도 승인이 필요하다.

## 2. Script 목록

| 파일 | 목적 | 기본 동작 |
|---|---|---|
| `check-env.sh` | backend, frontend, aws, deploy 환경 변수 존재 여부 확인 | 변수명과 상태만 출력하고 값은 출력하지 않는다 |
| `build-local-artifacts.sh` | backend/frontend/docs-site local build command 실행 | 지정 group만 build한다 |
| `deploy-backend-ec2.example.sh` | EC2 backend deploy 절차 skeleton | `THREADIA_DEPLOY_EXECUTE=true`가 아니면 dry-run만 한다 |

## 3. 사용 예시

```bash
scripts/deploy/check-env.sh backend
scripts/deploy/check-env.sh all
scripts/deploy/build-local-artifacts.sh backend
scripts/deploy/build-local-artifacts.sh all
scripts/deploy/deploy-backend-ec2.example.sh
```

## 4. 안전 기준

1. `.env` 실제 값은 repository에 commit하지 않는다.
2. script는 secret value를 출력하지 않는다.
3. `deploy-backend-ec2.example.sh`는 dry-run이 기본이다.
4. 실제 원격 copy/restart는 `THREADIA_DEPLOY_EXECUTE=true`가 있을 때만 실행한다.
5. 실제 실행 전 팀 승인, 대상 host 확인, rollback 방법, DB migration 영향, health check 기준을 별도 기록한다.

## 5. 관련 문서

| 문서 | 목적 |
|---|---|
| `docs/00-project/sprint-운영/sprint-2-CI-Deploy-준비계획.md` | Sprint 2 CI/Deploy 준비 범위 |
| `docs/05-architecture/AWS-배포-아키텍처-v1.md` | AWS 배포 proposal |
| `docs/05-architecture/AWS-운영-환경변수-목록.md` | 환경 변수 inventory |
| `docs/05-architecture/Route53-커스텀-도메인-가이드.md` | domain/DNS 준비 guide |
