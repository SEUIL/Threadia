> **작성자**: Threadia 팀
> **작성일**: 2026-07-13
> **최종 수정**: 2026-07-28
> **버전**: v1.2
> **상태**: 검토 중
> **태그**: ci, qa, approval, rollback, governance

# Pre-release Validation Checklist

## 1. 목적과 현재 적용 범위

이 문서는 코드 변경 전후의 CI, QA, 승인, rollback 판단을 일관되게 검토하기 위한 재사용 가능한 governance checklist다. Threadia의 application deployment는 종료되었으며, 이 문서는 active deployment 또는 release authorization을 제공하거나 선언하지 않는다.

현재 `pre-deploy-readiness` workflow는 repository test/build와 frontend smoke만 수행하는 CI-only 수동 검증이다. cloud credential, infrastructure access, artifact publish, deployment 실행은 포함하지 않는다.

## 2. CI-only 검증

| Job | 확인 범위 | 실패 시 처리 |
|---|---|---|
| `backend-test` | Backend test와 test MySQL 정합성 | 원인을 확인하고 변경을 수정하거나 명시적으로 제외한다. |
| `frontend-lint-and-build` | Frontend dependency, lint, build | lint 또는 build 오류를 해결한다. |
| `docs-site-lint-and-build` | Docs-site lint, build, 문서 렌더링 | 문서 또는 사이트 build 오류를 해결한다. |
| `frontend-smoke` | mock 기반 browser smoke | 재현 조건과 영향 범위를 기록한다. |

CI 성공은 repository 검증 결과일 뿐, 운영 환경 접근, release 승인 또는 배포 가능 상태를 뜻하지 않는다.

## 3. QA 검토

| 항목 | 확인 기준 |
|---|---|
| 변경 범위 | 구현, test, 문서가 의도한 변경 범위와 일치한다. |
| 주요 사용자 흐름 | 정상 흐름과 주요 오류·권한 경계를 확인한다. |
| 문서 정합성 | 문서 링크, 명령어, 상태와 실제 source가 일치한다. |
| 민감정보 | secret, credential, account/resource identifier, host 정보가 source와 evidence에 없다. |

## 4. 승인 및 rollback 판단

변경이 되돌리기 어려운 데이터·권한·공개 API 영향을 가지면 영향 범위, 검토자, 복구 판단 기준을 별도로 기록한다. CI-only workflow는 rollback을 실행하지 않으며, 현재 decommissioned portfolio에는 active release 또는 rollback 대상이 없다.

향후 별도 환경을 다시 구성하는 경우에도, 실제 resource 변경과 release authorization은 이 checklist와 분리된 승인 작업으로 다룬다.

## 5. 기록 원칙

검증 기록에는 실행한 CI job, 결과, 미확인 범위와 후속 조치만 남긴다. secret, credential, account ID, resource ID, domain, host, raw infrastructure command는 기록하지 않는다.

## 6. 관련 문서

| 문서 | 목적 |
|---|---|
| [CI 기준](./CI-기준.md) | test/build 중심 CI 기준 |
| [검증 방법](./검증-방법.md) | local test/build/API/화면 검증 기준 |
| [프로젝트 배포 종료 기록](./프로젝트-배포-종료-기록.md) | application resource 종료와 후속 GitHub metadata 정리 경계 |
