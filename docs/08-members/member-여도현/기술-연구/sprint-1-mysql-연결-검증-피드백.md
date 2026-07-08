> **작성자**: 여도현
> **작성일**: 2026-07-01
> **최종 수정**: 2026-07-02
> **버전**: v1.2
> **상태**: 작성 중
> **태그**: backend, mysql, spring-boot, sprint-1, validation, security

# Sprint 1 MySQL 연결 검증 피드백

이 문서는 Sprint 1.1.2 `MySQL 8.4 연결 확인` 작업 중 검토한 주의사항과 처리 기준을 정리한다.  
공식 기준은 `docs/05-architecture/백엔드-구조.md`와 Sprint 운영 문서이며, 이 문서는 여도현 개인 기술 연구 기록이다.

---

## 1. 작성 배경

Sprint 1.1.2의 완료 기준은 `test DB 연결 성공`이다. 이번 작업에서는 Docker 기반 MySQL 8.4 container를 실행하고 Spring Boot `JdbcTemplate` 테스트로 `SELECT 1`, `SELECT VERSION()`을 검증했다.

다만 연결 확인 작업은 DB schema, secret 관리, test skip, Docker volume 재사용 같은 후속 위험 요소와 연결된다. PR #11에서는 연결 성공 범위를 명확히 하고, schema 검증과 JPA 정책은 후속 작업으로 분리했다.

---

## 2. 검토한 주의사항 요약

| 번호 | 주의사항 | 판단 | 처리 기준 |
|---|---|---|---|
| 1 | `db/schema.sql` 없음 | 이번 PR blocker 아님 | 연결 확인까지만 완료로 본다 |
| 2 | Hikari `initialization-fail-timeout=0` | 앱 시작 성공만으로 DB 연결 성공 단정 불가 | `MysqlConnectionTests`를 핵심 검증으로 둔다 |
| 3 | 테스트가 env에 의해 skip될 수 있음 | 가장 중요한 검증 리스크 | env 로드 후 skip되지 않았는지 확인한다 |
| 4 | password 공유 방식 | secret commit 위험 | `.env`는 commit하지 않고 placeholder만 문서화한다 |
| 5 | Docker container/volume 재사용 | 설정 변경이 반영되지 않을 수 있음 | 필요 시 test DB volume 재생성을 별도 판단한다 |
| 6 | timezone/UTC 기준 | 연결 테스트 직접 범위는 아님 | token/password reset 구현 전 후속 결정한다 |
| 7 | JPA `ddl-auto` 정책 | 현재는 JPA starter 없음 | Entity 도입 시 `update` 자동 생성 금지 기준을 확인한다 |

---

## 3. 항목별 피드백

### 3.1 `db/schema.sql` 없음

문서상 DB 생성 기준은 `db/schema.sql`이다. 하지만 Sprint 1.1.2의 이번 작업은 Auth table 생성이 아니라 MySQL 8.4 test DB와 Spring Boot datasource 연결 확인이다.

따라서 현재 `db/schema.sql`이 없다는 사실은 이번 연결 확인 PR의 blocker가 아니다. 다만 아래 table 생성 검증은 후속 DB schema 작업 이후 진행해야 한다.

- `users`
- `oauth_accounts`
- `password_reset_tokens`
- `refresh_tokens`

이번 작업의 완료 범위는 다음으로 제한하는 것이 맞다.

```text
MySQL 8.4 container 실행
Spring Boot datasource 연결
SELECT 1 성공
SELECT VERSION() 결과 8.4.x 확인
```

---

### 3.2 Hikari `initialization-fail-timeout=0`

현재 설정은 다음과 같다.

```properties
spring.datasource.hikari.initialization-fail-timeout=0
```

이 설정 때문에 `bootRun`이 성공했다는 사실만으로 DB 연결 성공을 강하게 주장하면 안 된다. 앱 시작 시점의 datasource 초기화와 실제 query 실행은 다른 검증이다.

따라서 이번 PR에서는 `bootRun`이나 actuator health만 완료 기준으로 두지 않고, `MysqlConnectionTests`에서 실제 query를 실행하는 방식을 핵심 검증으로 삼았다.

판단 기준은 다음과 같다.

| 검증 | 신뢰도 | 비고 |
|---|---|---|
| `bootRun` 성공 | 낮음 | 앱이 뜬 것만 확인 |
| `/actuator/health` 확인 | 중간 | runtime 상태 확인에 도움 |
| `JdbcTemplate SELECT 1` | 높음 | 실제 DB query 실행 |
| `JdbcTemplate SELECT VERSION()` | 높음 | MySQL 8.4 연결 여부 확인 |

---

### 3.3 테스트가 환경변수에 의해 skip될 수 있음

`MysqlConnectionTests`는 아래 조건이 있을 때만 실행된다.

```java
@EnabledIfEnvironmentVariable(named = "THREADIA_DB_PASSWORD", matches = ".+")
```

따라서 `THREADIA_DB_PASSWORD`가 없는 상태에서 `./gradlew test`가 성공해도 DB 연결 테스트가 실행됐다고 볼 수 없다. 이 점이 이번 작업에서 가장 조심해야 하는 검증 리스크다.

검증 시에는 다음 순서가 필요하다.

```bash
cd backend

set -a
source ../.env
set +a

./gradlew test --rerun-tasks
```

성공 기준은 다음과 같다.

```text
MysqlConnectionTests가 skip되지 않고 실행됨
JdbcTemplate으로 SELECT 1 성공
JdbcTemplate으로 SELECT VERSION() 성공
VERSION() 결과가 8.4.x
BUILD SUCCESSFUL
```

---

### 3.4 password 공유 방식

`compose.mysql.yml`은 DB password와 root password를 env로 받는다.

```yaml
MYSQL_PASSWORD: ${THREADIA_DB_PASSWORD:?THREADIA_DB_PASSWORD is required}
MYSQL_ROOT_PASSWORD: ${THREADIA_DB_ROOT_PASSWORD:?THREADIA_DB_ROOT_PASSWORD is required}
```

이 방식은 password가 없으면 container 실행이 실패하도록 하므로 secret 누락을 빠르게 드러낸다. 반면 실제 password 값을 repository에 넣으면 안 된다.

처리 기준은 다음과 같다.

- `.env`는 commit하지 않는다.
- PR 본문과 문서에는 실제 password를 쓰지 않는다.
- 예시는 `<local-db-password>`, `<local-root-password>` 같은 placeholder만 사용한다.
- 팀 공유가 필요하면 `.env.example` 추가 여부를 별도 합의한다.

---

### 3.5 Docker container/volume 재사용 이슈

MySQL Docker image는 최초 초기화 시점에 database, user, password를 만든다. 이미 생성된 container나 volume이 남아 있으면 `.env` 값을 바꿔도 기대한 대로 반영되지 않을 수 있다.

예를 들어 아래 값을 바꿨는데 접속이 계속 실패할 수 있다.

- `THREADIA_DB_NAME`
- `THREADIA_DB_USERNAME`
- `THREADIA_DB_PASSWORD`
- `THREADIA_DB_ROOT_PASSWORD`

이 경우 test DB volume 재생성이 필요할 수 있다. 다만 `down -v`는 DB volume을 삭제하는 destructive 작업이므로 실행 전 목적을 분명히 해야 한다.

```bash
docker compose --env-file .env -f compose.mysql.yml down -v
docker compose --env-file .env -f compose.mysql.yml up -d mysql
```

이번 단계에서는 local test DB라면 재생성이 가능하지만, shared DB나 실제 데이터가 있는 환경에서는 사용하면 안 된다.

---

### 3.6 timezone/UTC 기준

Threadia 문서에는 서버 생성·만료 시각을 UTC로 저장하고 API 응답은 ISO-8601 UTC `Z` 형식으로 반환한다는 기준이 있다.

단순 DB 연결 테스트에서는 timezone 설정이 직접 blocker는 아니다. 하지만 아래 기능을 구현할 때는 중요하다.

- access token 만료
- refresh token 만료
- password reset token 만료
- OAuth account 연결 시각
- user 생성·수정 시각

따라서 이번 PR에서 timezone 설정을 무리하게 확정하기보다, Auth token과 JPA Entity 작업 전에 아래 항목을 결정하는 편이 낫다.

- JVM timezone 기준
- MySQL session timezone 기준
- JDBC URL의 timezone option 사용 여부
- `Instant`, `OffsetDateTime`, `LocalDateTime` 중 어떤 Java type을 쓸지

---

### 3.7 JPA `ddl-auto` 정책

현재 backend에는 JPA starter가 없다. 따라서 `spring.jpa.hibernate.ddl-auto`는 아직 적용 대상이 아니다.

하지만 후속 Entity 작업에서 JPA를 도입할 때 `ddl-auto=update`로 schema를 자동 생성하면 문서 기준과 충돌할 수 있다. Threadia의 DB schema 기준은 `db/schema.sql`이므로, Entity 도입 시에는 schema 자동 생성보다 검증 중심 정책을 검토해야 한다.

후속 기준은 다음 방향이 적절하다.

| 항목 | 권장 방향 |
|---|---|
| local schema 생성 | `db/schema.sql` 기준 |
| JPA Entity 검증 | `ddl-auto=validate` 또는 명시적 검증 |
| 운영 DB 변경 | 수동 SQL 또는 migration tool 합의 후 적용 |
| `ddl-auto=update` | 문서 기준과 충돌 가능성이 있어 사용 금지 방향 |

---

## 4. 이번 작업의 적정 완료 기준

Sprint 1.1.2 `MySQL 8.4 연결 확인`의 적정 완료 기준은 다음과 같다.

1. Docker Compose로 `mysql:8.4` container를 실행한다.
2. Spring datasource가 `localhost:3307/threadia_test`에 연결된다.
3. `MysqlConnectionTests`가 skip되지 않고 실행된다.
4. `SELECT 1`이 성공한다.
5. `SELECT VERSION()` 결과가 `8.4.x`다.
6. 실제 secret, password, root password는 기록하거나 commit하지 않는다.
7. `db/schema.sql` 부재로 Auth table 검증은 후속 작업으로 명시한다.

이번 PR #11은 위 기준 중 schema 검증을 제외한 연결 확인 범위를 충족했다. Auth table 생성 검증은 DB schema 작업 이후 별도 PR에서 다루는 것이 맞다.

---

## 5. schema.sql 적용 검증 테스트 방법

`db/schema.sql`에는 `DROP TABLE` 구문이 포함되어 있으므로 기존 `threadia_test` DB가 아니라 임시 DB `threadia_schema_validation`을 만들어 검증한다. 이 임시 DB는 schema 적용 검증 후 삭제한다.

### 5.1 MySQL 8.4 container 확인

repo root에서 MySQL 8.4 container를 실행한다.

```bash
docker compose --env-file .env -f compose.mysql.yml up -d mysql
```

container 상태는 `threadia-mysql84`가 `healthy`인지 확인한다.

```bash
docker ps --filter name=threadia-mysql84
```

### 5.2 임시 검증 DB 생성

repo root에서 `.env`를 로드한 뒤 임시 DB를 생성한다. 실제 password 값은 명령 기록이나 문서에 남기지 않는다.

```bash
set -a
source .env
set +a

docker exec -e MYSQL_PWD="$THREADIA_DB_ROOT_PASSWORD" threadia-mysql84 \
  mysql -uroot \
  -e "DROP DATABASE IF EXISTS threadia_schema_validation; CREATE DATABASE threadia_schema_validation CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; GRANT ALL PRIVILEGES ON threadia_schema_validation.* TO '$THREADIA_DB_USERNAME'@'%'; FLUSH PRIVILEGES;"
```

### 5.3 Spring Boot 연결 테스트

먼저 Spring Boot가 임시 DB에 연결되는지 확인한다. 이 단계는 `MysqlConnectionTests`로 `SELECT 1`, `SELECT VERSION()`을 실행한다.

```bash
cd backend

set -a
source ../.env
set +a

THREADIA_DB_URL="jdbc:mysql://localhost:3307/threadia_schema_validation" \
./gradlew test --tests '*MysqlConnectionTests' --rerun-tasks
```

성공 기준은 다음과 같다.

```text
JdbcTemplate으로 SELECT 1 성공
JdbcTemplate으로 SELECT VERSION() 성공
VERSION() 결과가 8.4.x
BUILD SUCCESSFUL
```

### 5.4 schema.sql 적용 검증 테스트

`MysqlSchemaValidationTests`는 Spring Boot test context에서 `db/schema.sql`을 적용하고, Sprint 1 Auth 4개 table이 생성됐는지 확인한다. `schema.sql`이 destructive DDL을 포함하므로 `THREADIA_DB_SCHEMA_VALIDATION=true`를 명시한 경우에만 실행한다.

```bash
THREADIA_DB_URL="jdbc:mysql://localhost:3307/threadia_schema_validation" \
THREADIA_DB_SCHEMA_VALIDATION=true \
./gradlew test --tests '*MysqlSchemaValidationTests' --rerun-tasks
```

성공 기준은 다음과 같다.

```text
db/schema.sql 적용 성공
users table 생성 확인
oauth_accounts table 생성 확인
password_reset_tokens table 생성 확인
refresh_tokens table 생성 확인
BUILD SUCCESSFUL
```

### 5.5 Auth 4개 table 직접 확인

repo root로 돌아와서 MySQL client로 Auth 4개 table을 직접 확인할 수 있다.

```bash
cd ..

set -a
source .env
set +a

docker exec -e MYSQL_PWD="$THREADIA_DB_PASSWORD" threadia-mysql84 \
  mysql -u"$THREADIA_DB_USERNAME" threadia_schema_validation -N \
  -e "SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name IN ('users', 'oauth_accounts', 'password_reset_tokens', 'refresh_tokens') ORDER BY table_name;"
```

예상 결과는 다음과 같다.

```text
oauth_accounts
password_reset_tokens
refresh_tokens
users
```

### 5.6 전체 build 확인

```bash
cd backend
./gradlew build --rerun-tasks
```

성공 기준은 다음과 같다.

```text
BUILD SUCCESSFUL
```

### 5.7 임시 DB 정리

검증이 끝나면 repo root에서 임시 DB를 삭제한다.

```bash
cd ..

set -a
source .env
set +a

docker exec -e MYSQL_PWD="$THREADIA_DB_ROOT_PASSWORD" threadia-mysql84 \
  mysql -uroot \
  -e "DROP DATABASE IF EXISTS threadia_schema_validation;"
```

주의할 점은 다음과 같다.

- `threadia_schema_validation`은 검증 전용 임시 DB다.
- 기존 `threadia_test`에 직접 `db/schema.sql`을 적용하지 않는다.
- `THREADIA_DB_SCHEMA_VALIDATION=true` 없이 실행하면 schema validation test는 skip된다.
- 실제 password, root password, token 값은 문서와 PR 본문에 기록하지 않는다.

---

## 6. 후속 작업 체크리스트

| 후속 작업 | 확인할 내용 |
|---|---|
| `db/schema.sql` 적용 검증 | Auth 4개 table 생성 확인 기준 유지 |
| JPA starter 도입 | `ddl-auto` 정책 확정 |
| Entity 작성 | `db/schema.sql`과 mapping 일치 확인 |
| Auth token 구현 | UTC 저장·만료 기준 확정 |
| CI DB test | GitHub Actions MySQL service container 여부 결정 |
| `.env.example` | 팀 공유용 placeholder 추가 여부 합의 |

---

## 7. 관련 문서와 PR

- `docs/05-architecture/백엔드-구조.md`
- `docs/00-project/sprint-운영/sprint-1-backlog.md`
- `docs/00-project/sprint-운영/sprint-1-백엔드-dod.md`
- `docs/00-project/검증-방법.md`
- PR #11 `feat(db): MySQL 8.4 연결 확인 구성 추가`

---

## 8. 변경 이력

| 버전 | 날짜 | 내용 |
|---|---|---|
| v1.0 | 2026-07-01 | 최초 작성 |
| v1.1 | 2026-07-02 | schema.sql 적용 검증 테스트 방법 추가 |
| v1.2 | 2026-07-07 | 최신 dev 기준 schema/JPA/company invitation 반영 상태 메모 추가 |

---

## 9. 2026-07-07 최신 반영 메모

이 문서는 WBS `1.1.2` MySQL 8.4 연결 확인 당시의 feedback 기록이다. 최신 `dev` 기준으로는 아래 후속 항목이 반영됐다.

| 항목 | 최신 상태 |
|---|---|
| `db/schema.sql` | 생성됨 |
| JPA starter | 도입됨 |
| Auth Entity mapping | `users`, `oauth_accounts`, `password_reset_tokens`, `refresh_tokens` 기준 검증됨 |
| Sprint 1.5 company/invitation 기반 | `companies`, `company_members`, `invitations` Entity와 schema 검증 추가됨 |
| schema 검증 범위 | Auth 4개 table에서 company/invitation 선행 table까지 확장됨 |

따라서 본문 중 “후속 작업”과 “Auth 4개 table” 표현은 작성 당시 범위로 읽어야 한다. 최신 backend 구조 기준은 `docs/05-architecture/백엔드-구조.md`를 따른다.
