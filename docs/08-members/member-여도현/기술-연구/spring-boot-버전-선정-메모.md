> **작성자**: 여도현
> **작성일**: 2026-07-01
> **최종 수정**: 2026-07-01
> **버전**: v1.0
> **상태**: 검토 중
> **태그**: spring-boot, backend, sprint-1, java, gradle

# Spring Boot 버전 선정 메모

이 문서는 Sprint 1에서 여도현이 Owner로 맡은 `Spring Boot 기본 프로젝트 생성` 작업을 시작하기 전에 사용할 Spring Boot 버전을 정리한다.

## 결론

Threadia Sprint 1 Spring Boot skeleton은 **Spring Boot 3.5.x 최신 patch**로 생성한다.

조사 시점 기준으로는 공식 문서에서 `3.5.15`, GitHub release 목록에서 `3.5.16`까지 확인된다. 실제 프로젝트 생성 시점에는 Spring Initializr 또는 Maven Central에서 제공하는 **3.5.x stable 최신 patch**를 사용한다.

Java 기준은 프로젝트 문서에 맞춰 **Java 17**로 고정한다.

## 선택 근거

### 1. Threadia 확정 문서와 맞다

Threadia Sprint 1 문서는 Backend framework를 `Spring Boot 3.x`로 두고, local development와 CI 기준을 `Java 17`로 확정한다.

관련 문서 근거는 다음과 같다.

| 문서 | 확인한 기준 |
|---|---|
| `docs/00-project/sprint-운영/sprint-1-백엔드-dod.md` | Spring Boot 3.x, MySQL 8.4, Java 17 기준 |
| `docs/00-project/sprint-운영/sprint-1-상세계획.md` | Spring Boot 기반 구축, Java 17 확정 |
| `docs/00-project/CI-기준.md` | GitHub Actions 예시가 `java-version: '17'` 기준 |
| `docs/00-project/검증-방법.md` | Backend 검증 명령은 `./gradlew bootRun`, `./gradlew test`, 안정화 후 `./gradlew build` |

따라서 이번 작업에서 Spring Boot 4.x를 선택하면 공식 문서의 `Spring Boot 3.x` 결정과 충돌한다.

### 2. Spring Boot 3.5.x는 Java 17 기준과 호환된다

Spring Boot 3.5 공식 system requirements는 Java 17 이상을 요구하고 Java 25까지 호환된다고 안내한다. 또한 Spring Framework 6.2.x 이상을 사용한다.

Threadia는 현재 Java 21 이상 전용 기능을 요구하지 않는다. 팀 local/CI 기준도 Java 17로 확정되어 있으므로 Java 17을 baseline으로 두는 편이 재현성이 높다.

### 3. 3.x 라인 안에서는 3.5.x가 가장 합리적이다

Spring Boot 3.4.x 이하를 선택하면 같은 3.x 라인 안에서도 더 오래된 minor line을 쓰게 된다. 신규 skeleton이라면 3.x 제약 안에서 최신 minor line인 3.5.x를 선택하는 것이 patch, dependency, 보안 업데이트 측면에서 낫다.

### 4. Spring Boot 4.x는 지금 선택하지 않는다

Spring Boot 4.x는 공식 stable 라인이고 Java 17 이상을 지원하지만, Spring Framework 7 기반의 major upgrade다. 신규 프로젝트 기준만 보면 후보가 될 수 있으나, Threadia Sprint 1 문서는 이미 `Spring Boot 3.x`를 확정 기준으로 둔다.

이번 작업의 목표는 기술 스택 재결정이 아니라 Sprint 1 문서에 부합하는 기본 프로젝트 생성이다. 따라서 4.x 도입은 별도 ADR 또는 팀 합의가 있을 때 검토한다.

## 적용 기준

Spring Boot 기본 프로젝트 생성 시 아래 기준을 적용한다.

| 항목 | 기준 |
|---|---|
| Spring Boot | 3.5.x stable 최신 patch |
| Java | 17 |
| Build tool | Gradle, Gradle wrapper 포함 |
| 실행 검증 | `./gradlew bootRun` |
| 최소 테스트 | `./gradlew test` |
| 안정화 후 검증 | `./gradlew build` |
| DB | MySQL 8.4 연결은 다음 작업에서 확인 |

## Sprint 1 WBS 연결

WBS에서 여도현이 Owner인 Sprint 1 초반 작업은 다음 순서다.

| WBS ID | 작업 | Owner | Reviewer | 완료 조건 |
|---|---|---|---|---|
| 1.1.1 | Spring Boot 기본 프로젝트 생성 | 여도현 | 황스일 | local에서 서버 실행 가능 |
| 1.1.2 | MySQL 8.4 연결 확인 | 여도현 | 황스일 | test DB 연결 성공 |
| 1.3.2 | 소셜 로그인 API 구현 | 여도현 | 황스일 | OAuth 시작/callback 구현, blind auto-link 금지 |

이번 문서는 `1.1.1 Spring Boot 기본 프로젝트 생성`에 대한 버전 결정 근거로 사용한다.

## 후속 확인

- 실제 skeleton 생성 시 Spring Initializr 또는 Maven Central에서 사용 가능한 3.5.x 최신 patch를 확인한다.
- 생성된 `build.gradle` 또는 `build.gradle.kts`에 Java toolchain `17`을 명시한다.
- Spring Boot skeleton PR에는 `./gradlew bootRun` 실행 결과를 남긴다.
- CI workflow를 만들 때는 `actions/setup-java`의 `java-version`을 `17`로 둔다.

## 참고 URL

- Spring Boot 3.5 system requirements: https://docs.spring.io/spring-boot/3.5/system-requirements.html
- Spring Boot current system requirements: https://docs.spring.io/spring-boot/system-requirements.html
- Spring Boot releases: https://github.com/spring-projects/spring-boot/releases
- Spring Boot 4.0 release notes: https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Release-Notes
