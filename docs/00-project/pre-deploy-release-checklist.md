> **작성자**: Threadia 팀  
> **작성일**: 2026-07-13  
> **최종 수정**: 2026-07-13  
> **버전**: v1.0  
> **상태**: 검토 중  
> **태그**: pre-deploy, release, readiness, rollback, approval

# Pre-deploy Release Checklist

## 1. 목적과 판정 원칙

이 문서는 선택한 release ref를 실제 배포 대상으로 승인할지 판단하는 사전 점검 기준이다. 수동 readiness 실행 절차는 [배포 준비도 수동 점검 Runbook](/00-project/배포-준비도-수동-점검-Runbook)을 따르며, 이 문서는 그 결과와 수동 검토를 바탕으로 한 Go 또는 No-Go 판정을 담당한다.

CI 성공은 repository 검증 결과일 뿐 배포 권한, 운영 환경 접근 권한, 실제 배포 승인을 뜻하지 않는다. 아래 모든 필수 게이트가 충족되고 release owner와 승인자가 기록으로 승인한 경우에만 Go로 판정한다.

## 2. 역할과 승인 체크포인트

| 시점 | 책임자 | 확인 사항 | 기록 |
|---|---|---|---|
| 점검 시작 | Release owner | 배포 대상의 immutable commit 또는 tag, 변경 범위, 점검 담당자를 확정한다 | release ref와 변경 요약 |
| CI 검토 | QA 또는 CI owner | 해당 ref의 CI-only 결과와 미실행 항목을 확인한다 | workflow URL, job별 결과 |
| 수동 게이트 | 영역 owner | 기능, 환경, migration, rollback 준비도를 각자 확인한다 | 게이트별 PASS, FAIL, BLOCKED |
| Go/No-Go | 승인자 | 미해결 blocker, 예외 승인, rollback 준비도를 검토한다 | 판정, 승인 시각, 승인자 |

Release owner와 최종 승인자는 분리한다. 승인자가 없거나 승인 기록이 없으면 No-Go다.

## 3. CI-only checks

`pre-deploy-readiness`는 선택한 ref에서 repository 내부 검증만 수행한다. 이 job set은 배포를 실행하지 않으며 외부 인프라, 운영 secret, 실제 환경 상태를 확인하지 않는다.

| Job | 확인 범위 | 필수화 조건 | 판정 |
|---|---|---|---|
| `backend-test` | Backend test | 현재 workflow에 포함된 job이 성공해야 한다 | 실패 또는 미실행이면 No-Go |
| `frontend-lint-and-build` | Frontend dependency install, lint, build | 현재 workflow에 포함된 job이 성공해야 한다 | 실패 또는 미실행이면 No-Go |
| `docs-site-lint-and-build` | Docs-site dependency install, lint, build | 현재 workflow에 포함된 job이 성공해야 한다 | 실패 또는 미실행이면 No-Go |
| `frontend-smoke` | MySQL 준비, Spring Boot health 확인, managed Chromium 기반 Frontend smoke | 해당 CI PR이 merge되어 선택 ref에서 실행 가능한 뒤에만 필수 gate가 된다 | merge 전에는 결과를 요구하거나 통과로 주장하지 않는다 |

`frontend-smoke` CI PR이 merge된 뒤에는 위 네 job이 같은 선택 ref에서 성공해야 한다. merge 전에는 기존 세 job의 성공만 CI-only 필수 조건이며, smoke 검증이 필요하면 수동 release gate에서 결과와 한계를 기록한다.

## 4. 수동 release gates

| ID | Gate | Go 조건 | No-Go 또는 BLOCKED 조건 | Owner |
|---|---|---|---|---|
| R-01 | Release identity | 선택 ref가 기록된 immutable commit 또는 tag와 일치하고, CI evidence도 같은 ref를 가리킨다 | ref, artifact, evidence의 대상이 다르거나 확인할 수 없다 | Release owner |
| R-02 | 변경 범위와 QA | 변경 영향 영역의 정상 및 주요 실패 흐름이 확인되고, 미확인 항목의 영향과 후속 조치가 기록된다 | 중대 회귀, 미확인 핵심 흐름, owner 없는 미해결 항목이 있다 | QA owner, 영역 owner |
| R-03 | 환경 계약과 설정 | 승인된 환경 계약, 환경 변수 inventory, CORS, cookie, OAuth redirect 등 배포 영향 설정의 이름과 적용 책임이 정합하다. 값은 기록하지 않는다 | secret, 실제 host, domain, credential, 또는 설정값의 불일치를 발견했다 | Environment owner |
| R-04 | Migration 호환성 | schema, application mapping, 기존 데이터와의 호환성, 복구 가능성을 검토하고 영향이 기록된다 | 호환성 검토가 없거나 기존 데이터 손상 가능성을 해소하지 못했다 | DB owner |
| R-05 | 비가역 migration 승인 | 데이터 삭제, 형식 축소, 되돌릴 수 없는 변환처럼 비가역 migration이 있으면 영향 범위, backup 또는 복구 계획, 실행 창을 별도로 승인받는다 | 비가역 migration의 별도 승인이 없거나 복구 계획이 없다 | DB owner, 최종 승인자 |
| R-06 | Rollback 준비 | rollback 책임자, 중단 기준, 이전 안정 ref, 데이터 복구 판단 기준이 기록되어 있다 | rollback 판단자 또는 이전 안정 기준이 없다 | Release owner, 운영 owner |
| R-07 | 최종 승인 | 모든 필수 gate가 PASS이고 예외는 영향, 만료 시점, 승인자를 포함해 기록된다 | FAIL, BLOCKED, 또는 미승인 예외가 있다 | 최종 승인자 |

환경 계약 문서는 별도 docs branch에서 준비 중이다. 해당 문서가 merge되어 canonical 위치가 확정되면 R-03의 필수 참조로 추가한다. merge 전에는 존재하지 않는 문서를 충족 근거로 사용하지 않는다.

## 5. Go, No-Go, Stop 판정

### Go

다음 조건을 모두 충족하면 Go다.

- 선택 ref와 CI evidence가 일치한다.
- 적용 시점의 모든 CI-only 필수 job이 성공한다.
- R-01부터 R-07까지 PASS이며 최종 승인 기록이 있다.
- 미해결 항목은 배포 안전성에 영향이 없고, 예외 승인과 추적 책임자가 기록되어 있다.

### No-Go

다음 중 하나라도 있으면 배포를 승인하지 않는다.

- CI-only 필수 job 실패, 취소, 미실행, 또는 대상 ref 불일치가 있다.
- 중대 기능 회귀, 인증 또는 권한 이상, 데이터 정합성 우려가 확인됐다.
- 환경 설정, migration 호환성, 비가역 migration 승인, rollback 준비 중 하나가 PASS가 아니다.
- approval checkpoint가 누락됐거나 근거 없는 예외만 남아 있다.

### Stop 및 rollback 개시 기준

배포 중 또는 배포 직후 아래 상황이 확인되면 새 변경의 진행을 멈추고 승인된 rollback 절차의 개시 여부를 rollback 책임자가 판단한다.

- health 확인 실패 또는 핵심 사용자 흐름 불가다.
- 인증, 권한, 데이터 손상, 데이터 유실 가능성이 확인됐다.
- 이전 안정 ref로 복구하지 않으면 서비스 영향이 계속될 것으로 판단된다.
- migration 결과가 사전 호환성 검토와 다르거나 복구 계획을 실행해야 한다.

Stop 또는 rollback 결정에는 시각, 감지 근거, 영향 범위, 결정자, 이전 안정 ref, 후속 점검 결과를 남긴다. 실제 rollback 실행 방법은 승인된 운영 절차에서만 다룬다.

## 6. 기록할 evidence

release 기록에는 아래의 결과만 남긴다. secret, token, password, private key, 실제 host, domain, connection string, 환경 변수 값은 기록하지 않는다.

| 항목 | 기록 내용 |
|---|---|
| Release identity | release ref, 변경 요약, 점검 시작 시각 |
| CI-only checks | `pre-deploy-readiness` 실행 URL, 선택 ref, job별 PASS 또는 FAIL, `frontend-smoke` 필수화 여부 |
| 수동 gate | R-01부터 R-07의 결과, 확인자, BLOCKED 사유와 후속 조치 |
| Migration | 호환성 검토 결과, 비가역 여부, 별도 승인 여부, 복구 판단 기준 |
| Rollback | 이전 안정 ref, 책임자, stop 또는 rollback 개시 기준, 결정 결과 |
| Approval | Go 또는 No-Go, 승인자, 승인 시각, 승인된 예외와 만료 시점 |

## 7. 금지 및 범위 밖 작업

이 체크리스트를 수행하거나 기록하는 과정에서 다음 작업을 하지 않는다.

- CI 성공만으로 배포를 승인하거나 실제 배포를 실행하지 않는다.
- CI workflow, deploy script, GitHub settings, AWS resource, DNS, secret, `.env`를 변경하지 않는다.
- 실제 배포 명령, 원격 접속 정보, 운영 credential, 실환경 endpoint를 문서나 evidence에 적지 않는다.
- 별도 승인이 없는 migration, 특히 비가역 migration을 실행하지 않는다.
- 기존 Runbook의 절차나 내용은 이 문서로 대체하거나 수정하지 않는다.

## 8. 관련 문서

| 문서 | 사용 목적 |
|---|---|
| [배포 준비도 수동 점검 Runbook](/00-project/배포-준비도-수동-점검-Runbook) | readiness workflow와 local follow-up 절차 |
| [CI 기준](/00-project/CI-기준) | CI와 PR 검증 기준 |
| [검증 방법](/00-project/검증-방법) | test, build, API, 화면 검증과 evidence 기준 |
| [Sprint 2 CI/Deploy 준비계획](/00-project/sprint-운영/sprint-2-CI-Deploy-준비계획) | CI-only 범위와 승인 필요 follow-up |
| [AWS 배포 아키텍처 v1](/05-architecture/AWS-배포-아키텍처-v1) | 배포 proposal과 환경 영향 항목 |
| [DB schema 관리 정책](/07-decisions/DB-schema-관리-정책) | schema 기준과 migration 재검토 조건 |
