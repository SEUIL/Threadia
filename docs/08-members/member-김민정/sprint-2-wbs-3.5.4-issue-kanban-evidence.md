> **작성자**: Threadia 팀  
> **작성일**: 2026-07-13  
> **최종 수정**: 2026-07-13  
> **버전**: v1.3  
> **상태**: 검토 중  
> **태그**: sprint-2, wbs-3.5.4, issue, kanban, evidence

# Sprint 2 WBS 3.5.4 최소 Issue/Kanban 실행 Evidence

> 기준: `docs/08-members/member-김민경/sprint2-wbs-3.5.1-candidate-gate.md`의 WBS 3.5 후보 실행 범위와 2026-07-13 실행 결과다.

## 1. WBS 역할과 기록 범위

| WBS | Owner | Reviewer | 상태 |
|---|---|---|---|
| 3.5.4 | 김민정 | 황스일 | 검토 중 |

이 문서는 최소 Issue/Kanban 구현의 실행 evidence만 기록한다. Owner와 Reviewer의 역할은 구분하며, review, PR, merge 완료 여부는 기록하지 않는다.

## 2. 구현 범위

| 구분 | 구현 내용 |
|---|---|
| 프로젝트 이슈 조회·생성 | 프로젝트 이슈 `GET`/`POST`를 구현했다. |
| 이슈 상태 변경 | 이슈 상태 `PATCH`를 구현했다. |
| 이슈 키 | 프로젝트 범위에서 순차 이슈 키를 생성한다. |
| 상태 이력 | 이슈 상태 변경 이력을 기록한다. |
| 상태 전이 제한 | A 역할의 `DONE` 이동을 서버에서 거부한다. |
| Kanban 보드 | `BACKLOG`, `TODO`, `IN_PROGRESS`, `IN_REVIEW`, `DONE` 고정 5컬럼을 제공한다. |
| 이슈 생성 입력 | 제목만으로 이슈를 생성한다. |
| candidate 응답 | 프로젝트 이슈 `GET`, title-only `POST`, 상태 `PATCH`의 이슈 응답 객체는 `id`, `issueKey`, `title`, `status`만 포함한다. |

## 3. 명시적 제외 범위

- 이슈 상세 수정
- 담당자 지정
- 우선순위
- 마감일
- 휴지통 및 복원
- Sprint 연결
- from-message 연결
- 문서 연결
- thread 연결
- notification 연결

## 4. 2026-07-13 실행 결과

| 구분 | 명령 또는 확인 방법 | 결과 |
|---|---|---|
| Backend build | `cd backend && ./gradlew build` | 성공했다. 전체 332개 test 중 실패 0건, ignored 10건이다. |
| Issue focused test | Issue service/controller focused test 실행 | 26개가 통과했고 1개가 skipped됐다. |
| Frontend lint·build | `cd frontend && npm run lint && npm run build` | 성공했다. |
| Frontend changed-file diagnostics | 변경된 frontend 파일의 diagnostics 확인 | 오류가 없었다. |
| Java diagnostics | Java LSP 초기화 | 시간 초과로 완료되지 않았다. Gradle compile, test, build는 성공했다. |
| MySQL schema validation | `THREADIA_DB_PASSWORD`, `THREADIA_DB_SCHEMA_VALIDATION=true` 환경 | DB 환경이 없어 실행하지 않았다. 통과로 기록하지 않는다. |
| 이슈 키 동시성 test | `THREADIA_DB_PASSWORD` 환경 | DB 환경이 없어 실행하지 않았다. 통과로 기록하지 않는다. |

## 5. Board 브라우저 smoke 검증 결과

| 항목 | 확인 결과 |
|---|---|
| 실행 명령 | `PLAYWRIGHT_BASE_URL=http://localhost:3000 env -u CI npm run test:smoke -- board.spec.ts --workers=1` |
| 실행 결과 | `1 passed` |
| 검증 범위 | mocked API 응답으로 목록 load, title-only `POST` 생성, 허용된 상태 `PATCH` 변경, A 역할의 `DONE` 옵션 미노출을 검증했다. |
| 실행 환경 | 기존 `localhost:3000` 사용자 소유 server를 재사용했으며, 중지하거나 재시작하지 않았다. Playwright Chromium은 프로젝트 cache에 설치됐다. |

이 smoke는 mocked API 응답을 사용했으며, live Backend+Frontend E2E 검증을 의미하지 않는다.

## 6. MySQL 검증 미실행 상태

MySQL schema validation은 `THREADIA_DB_PASSWORD` 및 `THREADIA_DB_SCHEMA_VALIDATION=true` 환경을, 이슈 키 동시성 test는 `THREADIA_DB_PASSWORD` 환경을 사용할 수 없어 실행하지 않았다. 이 문서는 해당 검증을 성공 또는 통과로 기록하지 않는다.
