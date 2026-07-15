> **작성자**: Threadia 팀
> **작성일**: 2026-06-16
> **최종 수정**: 2026-06-16
> **버전**: v2.0
> **상태**: 검토 중

# test/build/API/화면 확인 기준 v2

## 1. 목적

이 문서는 Threadia Sprint 1 작업 완료 여부를 확인하기 위한 test, build, API, 화면, DB, 문서, PR 검증 방법을 정리한다.

`작업 유형별 완료 조건 v2.md`가 완료 기준을 정의한다면, 이 문서는 그 기준을 어떻게 확인하고 어떤 증빙을 남길지 정의한다.

## 2. 검증 기본 원칙

| 원칙 | 설명 |
|---|---|
| 변경 범위 기준 | 모든 명령을 무조건 실행하지 않고 변경 유형에 맞는 검증을 선택한다 |
| 자동 검증 우선 | 가능한 항목은 test, build, CI 같은 자동 검증으로 확인한다 |
| 수동 검증 기록 | API 응답, 화면 캡처, DB query 결과처럼 자동화가 어려운 항목은 재확인 가능한 기록을 남긴다 |
| 실패 케이스 포함 | 정상 케이스뿐 아니라 주요 실패 케이스를 함께 확인한다 |
| 미확인 항목 공개 | 실행하지 못한 검증은 사유, 영향, 후속 조치를 남긴다 |
| secret 비노출 | token, password, `.env`, DB 접속 정보, AWS key는 검증 기록에 남기지 않는다 |

## 3. Sprint 1 검증 흐름

```mermaid
flowchart TD
    A[변경 유형 확인] --> B{Backend 또는 DB 변경?}
    B -- 예 --> C[./gradlew test]
    B -- 아니오 --> D{Frontend 화면 변경?}
    C --> E{API 변경?}
    E -- 예 --> F[curl 또는 Postman 확인]
    E -- 아니오 --> G[검증 결과 기록]
    D -- 예 --> H[npm run dev 또는 route 확인]
    D -- 아니오 --> I{문서 또는 PR 기준 변경?}
    F --> G
    H --> G
    I -- 예 --> J[링크, 메타정보, 관련 문서 확인]
    I -- 아니오 --> G
    J --> G
```

## 4. Threadia 기준 명령어

| 영역 | 명령/확인 | Sprint 1 적용 기준 |
|---|---|---|
| Backend local 실행 | `./gradlew bootRun` | 서버 실행과 DB 연결 확인이 필요할 때 사용 |
| Backend 최소 test | `./gradlew test` | PR 전 기본 검증. 초기 CI 기준 |
| Backend build | `./gradlew build` | Spring Boot skeleton 안정화 후 merge 기준으로 확대 |
| Backend 확장 검증 | `./gradlew check` | lint/check 도구 도입 후 적용 |
| API 확인 | `curl` 또는 Postman | API 추가/변경, 인증/인가, validation 확인 |
| DB 확인 | MySQL client, application log, 관련 API 응답 | entity/table 관계, migration, local 연결 확인 |
| Frontend 실행 | `npm run dev` | React + Next 구조 생성 후 route와 화면 확인 |
| Frontend script | `npm run lint`, `npm run typecheck`, `npm run test:e2e` | script 확정 후 적용 |
| CI | GitHub Actions 초록 체크 | workflow 생성 이후 PR 자동 검증으로 적용 |

## 5. Test 확인 기준

### 5.1 Backend test

Backend 변경이 있으면 우선 아래 명령을 실행한다.

```bash
./gradlew test
```

| 확인 항목 | 기준 |
|---|---|
| 성공 여부 | test command가 exit code 0으로 종료된다 |
| 대상 | 변경된 service, controller, repository, validation에 관련 test가 포함된다 |
| 실패 케이스 | 필수값 누락, 권한 부족, 리소스 없음 등 주요 실패 조건을 확인한다 |
| DB 연동 | DB 저장/조회/수정/삭제 변경이면 관련 repository 또는 API 흐름을 확인한다 |
| 기록 | 실행 명령과 결과 요약을 PR 또는 작업 기록에 남긴다 |

### 5.2 Frontend test

Sprint 1 초기에는 Frontend skeleton과 npm script가 확정된 뒤 자동 검증을 적용한다.

| 단계 | 적용 기준 |
|---|---|
| 초기 | `npm run dev`로 route와 주요 화면을 브라우저에서 확인 |
| script 확정 후 | `npm run lint`, `npm run typecheck`를 PR 전 검증에 추가 |
| e2e 도입 후 | 로그인, 프로젝트 홈, 이슈 흐름 같은 핵심 flow를 `npm run test:e2e`로 확인 |

## 6. Build 확인 기준

### 6.1 Backend build

Spring Boot skeleton이 안정화되면 아래 명령을 merge 기준으로 확대한다.

```bash
./gradlew build
```

| 확인 항목 | 기준 |
|---|---|
| compile | Java compile error가 없다 |
| test | build 과정에서 test가 통과한다 |
| packaging | 실행 가능한 artifact 생성이 가능하다 |
| 설정 | 환경 변수 누락이나 profile 오류로 build가 실패하지 않는다 |
| warning | warning이 있으면 Sprint 1 진행에 영향이 있는지 기록한다 |

### 6.2 Frontend build

Frontend build는 Next 구조와 package manager, script가 확정된 뒤 적용한다.

```bash
npm run build
```

| 확인 항목 | 기준 |
|---|---|
| compile | TypeScript 또는 bundling 오류가 없다 |
| route | 주요 route가 build 과정에서 실패하지 않는다 |
| resource | 이미지, font, 환경 변수 참조가 누락되지 않는다 |
| 적용 시점 | script 준비 전에는 필수 기준으로 두지 않고 후속 항목으로 남긴다 |

## 7. API 확인 기준

### 7.1 확인 대상

다음 변경이 있으면 API 확인을 수행한다.

| 변경 | 확인 이유 |
|---|---|
| 신규 API 추가 | request/response 형식이 합의와 맞는지 확인 |
| 인증/인가 변경 | token 없음, 권한 부족 요청이 차단되는지 확인 |
| validation 변경 | 필수값, 형식 오류가 일관된 응답으로 반환되는지 확인 |
| DB 저장/수정/삭제 | API 성공 후 DB 반영 여부 확인 |
| 검색/필터/정렬/페이징 | 조건에 맞는 결과가 반환되는지 확인 |

### 7.2 Threadia API 확인 예시

아래 예시는 형식 예시이며 실제 endpoint와 request body는 API 명세 초안에 맞춰 조정한다.

```bash
curl -i -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password"}'

curl -i -X GET "http://localhost:8080/api/projects" \
  -H "Authorization: Bearer <access-token>"

curl -i -X GET "http://localhost:8080/api/projects/{projectId}/issues" \
  -H "Authorization: Bearer <access-token>"
```

### 7.3 API 확인 항목

| 항목 | 기준 |
|---|---|
| 정상 응답 | 기대한 status code와 response body가 반환된다 |
| validation | 필수값 누락, 잘못된 형식, 길이 초과가 적절한 오류로 반환된다 |
| 인증 | token이 없거나 만료된 요청이 차단된다 |
| 권한 | 접근 권한이 없는 project, issue 요청이 차단된다 |
| 리소스 없음 | 존재하지 않는 id 요청이 404 또는 합의된 오류로 처리된다 |
| 민감 정보 | password, token 원문, 내부 stack trace가 응답에 포함되지 않는다 |
| DB 반영 | 생성, 수정, 삭제 API는 DB 또는 후속 조회 API로 결과를 확인한다 |

## 8. DB 확인 기준

DB 검증은 ERD와 application 동작 사이의 정합성을 확인하는 절차다.

| 확인 항목 | 방법 | 기준 |
|---|---|---|
| local 연결 | `./gradlew bootRun`, application log, health check | MySQL 8.4 local DB에 연결된다 |
| table/entity 정합성 | ERD v2.0, entity class, migration 비교 | 이름, 관계, null, unique, foreign key 기준이 충돌하지 않는다 |
| 저장 결과 | 생성 API 후 조회 API 또는 query 확인 | 기대한 table에 데이터가 저장된다 |
| 수정 결과 | 수정 API 후 조회 API 또는 query 확인 | 변경된 field만 의도대로 바뀐다 |
| 삭제 결과 | 삭제 API 후 조회 API 또는 query 확인 | hard delete 또는 soft delete 기준이 문서와 일치한다 |
| migration | migration script 적용 결과 | local 또는 test DB에서 재현 가능하다 |

DB 접속 정보, password, connection string은 검증 기록에 남기지 않는다.

## 9. 화면 확인 기준

### 9.1 실행 전 준비

Frontend 구조가 생성된 뒤 아래 명령으로 화면을 확인한다.

```bash
npm run dev
```

확인 전에 아래 항목을 준비한다.

| 항목 | 예시 |
|---|---|
| 확인 URL | `/login`, `/projects`, `/projects/{projectId}` |
| Backend 상태 | API server 실행 여부 |
| 테스트 계정 | 권한 조건만 기록하고 password는 기록하지 않음 |
| 테스트 데이터 | project, issue, member 등 화면 확인에 필요한 데이터 |
| 화면 크기 | Desktop 우선. 필요 시 Mobile 375px 추가 |

### 9.2 화면 확인 항목

| 항목 | 기준 |
|---|---|
| 진입 | 대상 route에 정상 진입할 수 있다 |
| runtime | 흰 화면, 무한 loading, console fatal error가 없다 |
| 데이터 표시 | API 또는 mock data가 요구된 위치와 형식으로 표시된다 |
| 사용자 action | 클릭, 입력, 저장, 수정, 삭제 등 변경 범위의 action이 동작한다 |
| 오류 상태 | API 실패, validation 실패, 권한 없음 상태가 사용자에게 전달된다 |
| 화면 깨짐 | Desktop 기준으로 요소 겹침, 잘림, 불필요한 가로 scroll이 없다 |
| 증빙 | route, 브라우저, 화면 크기, 캡처 또는 확인 결과를 기록한다 |

## 10. 문서 확인 기준

| 항목 | 확인 방법 |
|---|---|
| 메타정보 | 작성자, 작성일, 최종 수정, 버전, 상태가 있는지 확인 |
| 문체 | 한국어 `~다` 체와 기술 용어 표기가 일관적인지 확인 |
| 링크 | 상대 경로와 관련 문서명이 실제 존재하는지 확인 |
| 명령어 | 문서에 적은 명령어가 현재 프로젝트 기준과 맞는지 확인 |
| 충돌 | API, ERD, 화면 정의서, Backlog 문서와 내용이 충돌하지 않는지 확인 |
| 미확정 | 미확정 항목에 owner, 결정 시점, 후속 조치가 있는지 확인 |

## 11. PR/CI 확인 기준

| 항목 | 확인 기준 |
|---|---|
| PR 설명 | 변경 목적, 주요 변경, 제외 범위가 적혀 있다 |
| 검증 결과 | 실행한 명령과 결과, API/화면 확인 결과가 적혀 있다 |
| CI | workflow 생성 이후 GitHub Actions가 초록 체크인지 확인한다 |
| AI 사용 | AI 사용 여부와 범위가 적혀 있다 |
| 승인 필요 | DB schema, 인증/인가, CI, AWS, secret 변경 여부가 표시되어 있다 |
| reviewer | owner와 reviewer가 분리되어 있다 |

## 12. 작업 유형별 필수 검증 요약

| 작업 유형 | 필수 검증 | 보조 검증 |
|---|---|---|
| Backend | `./gradlew test`, API 정상/실패 케이스 | `./gradlew build` |
| Frontend | `npm run dev`, 브라우저 route와 주요 flow 확인 | `npm run lint`, `npm run typecheck`, `npm run build` |
| DB | DB 연결, ERD/entity/table 정합성, 관련 API 확인 | migration 재실행, 검증 query |
| Infra/AWS | 사용 범위, 비용/계정, 배포 시점, secret 관리 문서 확인 | 실제 배포는 Sprint 1 이후 |
| 문서 | 메타정보, 링크, 관련 문서 충돌 확인 | 명령어 직접 실행 |
| PR | 검증 결과, 영향 범위, 보안/승인 체크 | CI 초록 체크 |
| AI-assisted | owner 직접 검증, AI 사용 범위 기록 | reviewer 추가 검토 |

## 13. 검증 결과 작성 예시

PR 또는 작업 기록에는 아래 형식으로 검증 결과를 남긴다.

```markdown
## 검증 결과

- Backend test: `./gradlew test` 통과
- Backend build: skeleton 안정화 전이라 미실행. 후속 기준은 `./gradlew build`
- API:
  - `POST /api/auth/login` 정상 응답 확인
  - 필수값 누락 시 400 계열 오류 응답 확인
  - token 없는 `GET /api/projects` 요청 차단 확인
- DB:
  - MySQL 8.4 local 연결 확인
  - project 생성 후 조회 API에서 데이터 반영 확인
- 화면:
  - Chrome Desktop에서 `/login`, `/projects` 진입 확인
  - 화면 캡처 첨부
- 문서:
  - 관련 API/ERD 문서 링크 확인
- AI 사용:
  - Controller 초안 생성에 사용. owner가 수정 내용 확인 후 직접 test 실행
- 미확인 항목:
  - Frontend `npm run typecheck`는 script 확정 후 적용
```

## 14. 완료로 보지 않는 경우

| 경우 | 처리 |
|---|---|
| test 또는 build 결과가 없다 | 실행하거나 미실행 사유와 후속 조치를 기록한다 |
| API 변경인데 요청/응답 예시가 없다 | curl, Postman, test 중 하나로 재확인한다 |
| 화면 변경인데 화면 확인 기록이 없다 | route, 브라우저, 화면 크기, 캡처를 남긴다 |
| DB 변경인데 DB 반영 확인이 없다 | query, log, 조회 API 중 하나로 확인한다 |
| 정상 케이스만 확인했다 | 실패 케이스를 추가 확인한다 |
| secret이 검증 기록에 노출되었다 | 즉시 제거하고 노출 범위와 후속 조치를 기록한다 |
| 미확인 항목의 owner와 처리 시점이 없다 | Backlog 또는 PR TODO로 추적 가능하게 만든다 |
