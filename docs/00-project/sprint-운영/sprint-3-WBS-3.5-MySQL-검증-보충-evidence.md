> **작성자**: Threadia 팀
> **작성일**: 2026-07-13
> **최종 수정**: 2026-07-13
> **버전**: v1.0 MySQL post-merge validation evidence
> **상태**: 검토 중
> **태그**: sprint-3, wbs-3.5, mysql, validation, evidence, post-merge

# WBS 3.5 MySQL 검증 보충 evidence

## 1. 문서 목적

이 문서는 병합된 PR #90의 WBS 3.5 범위에 대해 2026-07-13에 수행한 MySQL 검증 결과를 보충 기록한다.

이 검증은 병합 후 검증이다. 기존에 DB test가 BLOCKED로 기록된 문서의 소유권이나 내용을 변경하지 않고, 이후 WBS 상태 동기화에 참고할 수 있는 팀 소유 evidence만 남긴다.

## 2. 검증 대상과 환경

| 항목 | 기록 |
|---|---|
| 대상 병합 SHA | `af41c281c7df128022e53cf3be59dfbd48298672` |
| DB | MySQL 8.4 |
| 검증 DB | 일회성 `threadia_evidence_*` 패턴의 disposable DB |
| schema 준비 | canonical schema를 적용했다 |
| 총 소요 시간 | 149초 |

검증용 DB는 실제 DB 이름, URL, 사용자 정보 또는 credential을 기록하지 않는다.

## 3. 테스트 결과

모든 아래 test class는 failures 0, errors 0, skips 0으로 완료됐다.

| Test class | Test 수 | Failures | Errors | Skips |
|---|---:|---:|---:|---:|
| `MysqlSchemaValidationTests` | 6 | 0 | 0 | 0 |
| `MysqlConnectionTests` | 1 | 0 | 0 | 0 |
| `JpaSchemaValidationTests` | 1 | 0 | 0 | 0 |
| `IssueMysqlConcurrencyTests` | 1 | 0 | 0 | 0 |
| `IssueRepositoryLockingTests` | 2 | 0 | 0 | 0 |

## 4. 정리 결과

canonical schema 적용과 테스트 완료 후 disposable MySQL 8.4 DB를 성공적으로 drop했다. 검증 종료 시점에 이 evidence를 위해 생성한 DB는 남기지 않았다.

## 5. 범위와 제한 사항

### 포함 범위

- 병합 SHA 기준 MySQL schema, connection, JPA schema validation 결과 확인
- MySQL 기반 issue concurrency와 repository locking test 결과 확인
- disposable DB 생성, canonical schema 적용, test 실행, drop 완료 확인

### 제외 범위

- 기존 멤버 소유 evidence 문서의 수정
- 기존 canonical 문서의 수정
- Google Sheets WBS의 상태 또는 진행률 수정
- PR 상태, commit, push, merge 변경
- 운영 DB 또는 운영 credential 사용
- 독립적인 GitHub approval 취득 여부 판단

Google Sheets WBS의 상태 및 진행률 동기화는 사용자가 수행하기로 한 범위이며, 이 문서에서는 의도적으로 수정하지 않는다. 이 evidence는 WBS 동기화 시 병합 후 MySQL 검증이 통과했다는 사실만 참조할 수 있게 한다.

## 6. 판정

병합 SHA `af41c281c7df128022e53cf3be59dfbd48298672`에 대한 기록된 MySQL 검증 범위는 PASS다. 이 판정은 본 문서에 기록한 disposable MySQL 8.4 환경과 test class 결과에만 적용한다.
