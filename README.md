# Threadia

Threadia는 Jira, Slack, Confluence처럼 분리된 협업 도구의 흐름을 하나의 프로젝트 협업 서비스로 통합하는 것을 목표로 하는 팀 프로젝트다.

이슈, 채팅, 쓰레드, 문서, 그래프, 알림을 연결해 업무 맥락이 흩어지지 않게 하는 것이 핵심 흐름이다.

```text
알림 수신
→ 이슈 확인
→ 이슈 또는 문서의 쓰레드에서 논의
→ 필요 시 프로젝트 채팅 또는 1:1 DM으로 소통
→ 문서에 결정사항 정리
→ 문서와 이슈를 연결
→ 그래프에서 연결 관계 확인
→ 변경사항을 알림으로 회수
```

---

## 기술 스택

| 영역 | 구성 |
|---|---|
| Backend | Java 17, Spring Boot 3.5, Spring Data JPA, Spring Security(JWT + OAuth2), MySQL 8.4 |
| Frontend | Next.js 16(App Router), React 19, TypeScript, TanStack Query, Tailwind CSS 4, React Three Fiber(3D 허브) |
| 파일 저장 | AWS S3 호환 스토리지(로컬 개발은 MinIO) |
| 테스트 | JUnit 5 + Testcontainers(Backend), Playwright(Frontend smoke test) |
| 인프라 | AWS(ECR/SSM 배포), Docker Compose(로컬 개발) |

---

## 프로젝트 구조

```text
Threadia/
├── backend/     Spring Boot API 서버
├── frontend/    Next.js 프론트엔드
├── docs/        기획/설계/API/아키텍처/회의록 등 전체 문서
├── docs-site/   docs/ 렌더링·검색용 사이트
├── db/          DB 스키마(schema.sql)
├── infra/       인프라 구성
├── scripts/     배포·운영 스크립트
└── compose.*.yml  로컬/배포용 Docker Compose 정의
```

기능/화면/API/DB 설계의 canonical 기준 문서는 [docs/02-requirements/요구사항-정의서.md](docs/02-requirements/요구사항-정의서.md)에서 각 영역별 문서로 안내한다.

| 구분 | 기준 문서 |
|---|---|
| 기능 요구사항 | `docs/02-requirements/기능-분해표.md` |
| 화면 기준 | `docs/03-design/화면정의서.md` |
| API 기준 | `docs/04-api/API-명세서.md` |
| DB 기준 | `docs/03-design/ERD.md`, `db/schema.sql` |
| Architecture | `docs/05-architecture/` |

---

## 로컬 개발 환경 실행

### 1. 환경 변수

```bash
cp .env.example .env
```

`.env`의 `REPLACE_ME_*` 값을 로컬 값으로 채운다. `SPRING_PROFILES_ACTIVE=local`을 유지하면 배포용 설정 검증을 건너뛴다.

### 2. 로컬 인프라(MySQL, MinIO) 기동

```bash
docker compose -f compose.mysql.yml -f compose.minio.yml up -d
```

### 3. Backend 실행

```bash
cd backend
./gradlew bootRun
```

### 4. Frontend 실행

```bash
cd frontend
npm install
npm run dev
```

프론트엔드는 기본적으로 `http://localhost:3000`, 백엔드 API는 `http://localhost:8080`에서 뜬다.

---

## 테스트

```bash
# Backend
cd backend
./gradlew test

# Frontend 타입/린트
cd frontend
npx tsc --noEmit
npm run lint

# Frontend smoke test(Playwright)
cd frontend
npm run test:smoke
```

---

## 문서

프로젝트 문서는 전부 [docs/](docs/) 아래에 있으며, 작성 규칙은 [docs/00-project/문서-작성-규칙.md](docs/00-project/문서-작성-규칙.md)를 따른다. 폴더별 용도는 [docs/README.md](docs/README.md)에 정리돼 있다.

---

## 기여 규칙

Git, commit message, PR 기준은 [CONTRIBUTING.md](CONTRIBUTING.md)를 따른다. 코드/문서/Git 작업 전 공통 작업 규칙은 [AGENTS.md](AGENTS.md)(또는 Claude Code 사용자는 [CLAUDE.md](CLAUDE.md))를 참고한다.
