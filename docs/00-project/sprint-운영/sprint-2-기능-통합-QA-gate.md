> **작성자**: Threadia 팀
> **작성일**: 2026-07-11
> **최종 수정**: 2026-07-11
> **버전**: v0.4 Sprint 2 Functional QA gate
> **상태**: 검토 중
> **태그**: sprint-2, qa, gate, evidence, verification

# Sprint 2 기능 통합 QA gate

## 1. 문서 목적

이 문서는 Sprint 2 기능 통합 QA gate의 검증 범위, 결과, 증빙 형식을 기록하는 작업용 초안이다.

이 문서는 배포 준비 완료를 확정하지 않는다. 실제 판정은 결과를 review한 뒤 별도로 확정한다.

## 2. 적용 범위

### 포함 범위

- Sprint 2에서 변경된 backend 기능 API
- Sprint 2에서 변경된 frontend route와 화면 상태
- Sprint 2에서 영향을 받은 DB schema 또는 mapping
- Sprint 2 관련 문서 정합성 확인
- Sprint 2 CI/QA 증빙 정리

### 제외 범위

- WBS 원문 수정
- 실제 배포 또는 운영 인프라 변경
- 외부 시스템에 대한 실접속 검증
- 승인되지 않은 secret 또는 운영 credential 사용
- 이 문서와 무관한 Sprint 1 또는 후속 sprint 결과 단정

## 3. 판정 정의

| 판정 | 정의 |
|---|---|
| PASS | 검증 항목과 기대 결과가 모두 충족된 상태다 |
| FAIL | 기대 결과와 다르거나 회귀가 확인된 상태다 |
| BLOCKED | 검증에 필요한 선행 조건, 환경, 권한, 데이터가 부족한 상태다 |
| OUT OF SCOPE | 이번 gate의 검증 범위에 포함되지 않는 항목이다 |

## 4. 결과 표 템플릿

| ID | 검증 항목 | 실행 명령 | 기대 assertion | 결과 | 증빙 위치 | 상태 |
|---|---|---|---|---|---|---|
| QA-01 | backend test | `./gradlew --no-daemon test` | backend test suite passes | PASS (78s) | `backend/build/reports/tests/test/index.html` | 완료 |
| QA-02 | backend build | `./gradlew --no-daemon build` | backend build completes successfully | PASS (11s) | `backend/build/libs/` | 완료 |
| QA-03 | auth/API preflight | `./scripts/auth-e2e-preflight.sh` | health, signup, login, refresh rotation, logout, me, password-reset request contract pass | PASS | `backend/scripts/auth-e2e-preflight.sh` | 완료 |
| QA-04 | DB schema/JPA | `THREADIA_DB_SCHEMA_VALIDATION=true ./gradlew --no-daemon test --tests com.threadia.backend.MysqlConnectionTests --tests com.threadia.backend.MysqlSchemaValidationTests --tests com.threadia.backend.JpaSchemaValidationTests` | schema/JPA validation can run without exposing secrets | PASS (28s) | `backend/src/test/java/com/threadia/backend/MysqlSchemaValidationTests.java`, `backend/src/test/java/com/threadia/backend/JpaSchemaValidationTests.java` | 완료 |
| QA-05 | frontend browser | `npm run test:smoke` | managed Chromium에서 anonymous redirect·auth form·invalid reset-token 흐름이 통과한다 | PASS (3 tests, 18.7s) | `frontend/playwright.config.ts`, `frontend/tests/smoke/anonymous-auth.spec.ts` | 완료 |
| QA-06 | external dependency tracks | user-controlled Google OAuth and reset-email check | Google OAuth success redirect와 reset email 수신을 사용자 browser·mailbox에서 확인한다 | PASS: 사용자 수동 확인 완료 | user-controlled browser and mailbox confirmation; no credentials or tokens recorded | 완료 |

## 5. evidence schema

각 검증 항목은 아래 형식으로 기록한다.

| 필드 | 내용 |
|---|---|
| command | 실행한 명령 또는 확인 절차다 |
| expected_assertion | 성공 또는 실패를 판단하는 기준이다 |
| source_path | 근거가 되는 문서, 테스트, 코드, 로그 경로다 |
| local_runnable | 로컬에서 재현 가능한지 여부다 |
| external_blocked | 외부 시스템 의존으로 인해 로컬에서만 재현 가능한지 여부다 |
| note | 추가 설명, 전제, 제한 사항이다 |

### 기록 예시 형태

| command | expected_assertion | source_path | local_runnable | external_blocked | note |
|---|---|---|---|---|---|
|  |  |  | yes/no | yes/no |  |

## 6. 외부 시스템 제외 원칙

다음 항목은 이번 템플릿의 기본 검증 대상으로 두지 않는다.

- AWS 운영 자원 변경
- GitHub Actions 결과 자체의 운영 상태 확인
- 외부 이메일 전송 성공 여부
- 외부 OAuth provider 실연동 확인
- 운영 DB 또는 운영 secret 접근
- 배포된 production 환경 직접 점검

외부 시스템이 필요한 항목은 `external_blocked`로 표시하고, 로컬 재현 방법 또는 대체 확인 수단을 함께 적는다.

## 7. no-secret 규칙

- DB password를 적지 않는다.
- access token, refresh token, cookie value를 적지 않는다.
- API key, OAuth secret, AWS credential을 적지 않는다.
- `.env` 원문이나 connection string을 적지 않는다.
- 로그에 민감값이 포함되면 마스킹된 형태로만 남긴다.

## 8. 작성 상태 메모

- 이 파일은 검토 중 상태로 유지한다.
- 결과는 sanitized outcome만 기록했으며, 배포 준비 완료로 판단하지 않는다.
- WBS 문서는 수정하지 않았다.
- 후속 조치:

| owner | follow-up | target condition |
|---|---|---|
| qa owner | SMTP runtime configuration source 정리 | reset-email delivery 설정을 재현 가능하게 확인한다 |
