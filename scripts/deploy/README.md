# Threadia local build artifacts

> **작성자**: Threadia 팀  
> **작성일**: 2026-07-11  
> **최종 수정**: 2026-07-28  
> **버전**: v1.0  
> **상태**: 확정  
> **태그**: local, build, artifacts

## 1. 목적

이 디렉터리는 local build artifacts 검증에 필요한 script만 둔다. retired deployment script와 AWS 운영 절차는 [프로젝트 배포 종료 기록](../../docs/00-project/프로젝트-배포-종료-기록.md)으로 정리했다.

## 2. Script 목록

| 파일 | 목적 | 기본 동작 |
|---|---|---|
| `build-local-artifacts.sh` | backend/frontend/docs-site local build command 실행 | 지정 group만 build한다 |

## 3. 사용 예시

```bash
scripts/deploy/build-local-artifacts.sh backend
scripts/deploy/build-local-artifacts.sh all
```

## 4. 안전 기준

1. local 환경에서만 실행한다.
2. `.env` 실제 값은 repository에 commit하지 않는다.
3. build 실패는 수정 전에 원인을 확인한다.

## 5. 관련 문서

| 문서 | 목적 |
|---|---|
| `docs/00-project/검증-방법.md` | local test/build 검증 기준 |
| `docs/00-project/프로젝트-배포-종료-기록.md` | retired deployment source와 resource 종료 기록 |
