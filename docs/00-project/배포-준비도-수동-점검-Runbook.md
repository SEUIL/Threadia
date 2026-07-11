> **작성자**: Threadia 팀<br>
> **작성일**: 2026-07-11<br>
> **최종 수정**: 2026-07-11<br>
> **버전**: v0.1<br>
> **상태**: 작성 중<br>
> **태그**: deploy, readiness, ci, runbook, manual

# 배포 준비도 수동 점검 Runbook

## 1. 목적과 적용 범위

이 Runbook은 실제 배포 직전에 선택한 Git ref의 repository test/build 상태와 local 사전 점검 절차를 분리해 기록한다. GitHub Actions의 수동 readiness workflow는 repository 내부 검증만 수행하며, 배포 기능이나 외부 인프라 접근 권한을 제공하지 않는다.

기준은 [CI 기준](/00-project/CI-기준), [검증 방법](/00-project/검증-방법), [Definition of Done](/00-project/Definition-of-Done), [Sprint 2 CI/Deploy 준비계획](/00-project/sprint-운영/sprint-2-CI-Deploy-준비계획)을 따른다.

## 2. GitHub Actions 수동 readiness 점검

1. GitHub Actions에서 `pre-deploy-readiness` workflow를 선택한다.
2. GitHub UI의 ref selector에서 점검할 branch, tag 또는 commit을 선택한다. workflow input은 사용하지 않는다.
3. 세 job이 모두 통과하는지 확인하고, 실행 URL과 선택 ref 및 결과를 배포 작업 기록에 남긴다.

성공한 workflow는 선택한 ref가 GitHub-hosted runner에서 clean 상태로 repository test/build를 통과했다는 의미뿐이다. Backend는 `./gradlew test`를, Frontend와 docs-site는 각각 `npm ci`, `npm run lint`, `npm run build`를 실행한다.

remote workflow 실행은 이 변경을 포함한 향후 commit/push가 승인된 뒤에만 가능하다. required status checks 적용은 별도 승인 대상인 GitHub ruleset 설정이며, 이 Runbook의 workflow 성공이나 local 검증으로 변경되지 않는다.

다음 항목은 workflow 성공으로 확인되지 않으며, 이 Runbook의 수동 follow-up 또는 승인된 별도 절차에서 확인해야 한다.

- AWS access와 계정 권한
- secret의 존재 여부와 유효성
- DNS 또는 domain 설정
- runtime health
- rollback 절차
- 실제 deployment

## 3. Local 사전 점검

GitHub workflow와 별도로, 실제 값이 안전한 local session에만 로드된 뒤 다음 명령을 실행한다. 값 자체나 출력은 작업 기록, terminal 공유, repository에 남기지 않는다.

```bash
scripts/deploy/check-env.sh all
```

명령 결과는 변수명과 설정 여부만 확인한다. 누락 항목이 있으면 실제 deployment를 진행하지 않고, 승인된 secret 관리 경로와 환경 설정을 담당자에게 확인한다.

## 4. EC2 skeleton의 local dry-run

EC2 skeleton은 GitHub Actions에서 실행하지 않는다. 필요한 실제 값과 local artifact 및 key path가 안전하게 준비된 local session에서만 아래와 같이 호출한다.

```bash
scripts/deploy/deploy-backend-ec2.example.sh
```

이 skeleton은 기본 동작이 dry-run이다. 이 Runbook에서는 execution opt-in flag를 설정하지 않는다. 출력에서 대상 설정 상태와 dry-run summary만 확인하고, 실제 원격 copy, restart, 배포 실행은 이 Runbook 범위에서 수행하지 않는다. dry-run 실패 또는 누락 변수는 deployment 중단 사유로 기록하고 follow-up으로 처리한다.

## 5. 수동 follow-up 및 완료 기록

실제 deployment 전에는 승인된 담당자가 AWS access, secret 유효성, DNS/domain, runtime health, rollback 계획을 각각 검토한다. 이 항목과 실제 deployment는 workflow 통과로 대체할 수 없으며, 별도 승인과 실행 기록이 필요하다.

배포 준비도 기록에는 아래 항목을 남긴다.

- 선택한 Git ref와 `pre-deploy-readiness` 실행 URL 및 세 job 결과
- local `check-env.sh`의 성공 또는 누락 변수명만 포함한 결과
- local EC2 skeleton dry-run 결과와 미해결 follow-up
- 모든 미해결 항목의 항목명, 담당자, 목표일, 승인 상태와 후속 조치

secret, token, password, private key, host 주소, 실제 환경 변수 값은 기록하지 않는다.
