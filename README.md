# Threadia

Threadia는 Jira, Slack, Confluence처럼 분리된 협업 도구의 흐름을 하나의 프로젝트 협업 서비스로 통합하는 것을 목표로 하는 팀 프로젝트입니다.

이슈, 채팅, 쓰레드, 문서, 그래프, 알림을 연결해서 업무 맥락이 흩어지지 않게 하는 것이 핵심 콘셉트입니다.

> **Thread**(모든 논의가 쓰레드 하나로 남는 구조) + **idea**(채팅에서 나온 생각이 이슈·문서로 이어지는 흐름) + **area**(프로젝트·채팅·문서가 분산되지 않는 통합 협업 공간)를 합친 이름입니다.

```mermaid
flowchart LR
    A[알림 수신] --> B[이슈 확인]
    B --> C[쓰레드에서 논의]
    C --> D[채팅 · DM 소통]
    D --> E[문서에 결정사항 정리]
    E --> F[문서 · 이슈 연결]
    F --> G[그래프에서 관계 확인]
    G --> H[변경사항 알림]
    H --> A
```

---

## 팀 구성

| 구성원 | 역할 | 담당 업무 |
|---|---|---|
| 황스일 | 팀장 | 팀 운영 총괄, WBS/일정 관리, 플랫폼 관리자(감사 로그, 회사 생명주기) 백엔드 |
| 정철진 | 부팀장 / Frontend lead | 프로젝트·문서·그래프 화면, 프론트엔드 아키텍처 |
| 김민경 | 팀원 | 이메일 Auth API 구현, 회사/부서 Backend API, 이슈·칸반 최소 API 구현 |
| 김민정 | 팀원 | DB schema 설계, Auth API 구현 지원, API 문서화, 검증 evidence 정리, Sprint QA/CI 총괄 |
| 김태준 | 팀원 | 프로젝트 백엔드(생성/조회/수정/archive), 인증 Entity 구현, API 문서화 |
| 여도현 | 팀원 | 인증 API(OAuth/JWT) 구현, 기술 검토, 회사/RBAC 리뷰 |

---

## 핵심 기능

| 영역 | 기능 |
|---|---|
| 인증 | OAuth 소셜 로그인, 이메일 회원가입/로그인 |
| 회사·조직 | 회사 관리, 부서, 구성원, RBAC |
| 프로젝트 | 생성/조회/수정/보관, 멤버 관리 |
| 이슈·칸반 | 이슈 목록/생성, 5컬럼 칸반, 스프린트/백로그 |
| 문서 | 문서 작성·편집, 문서·이슈 그래프 연결 |
| 메시지 | 프로젝트 채팅(쓰레드 연동), 1:1 DM |
| 검색 | 프로젝트/이슈 통합 검색 |
| 알림 | 실시간 알림, 안읽음 카운트 |
| 플랫폼 관리자 | 회사·관리자 생명주기, 감사 로그, 시스템 설정, 고객센터 |

---

## 트러블슈팅 하이라이트

### Frontend

**"같은 버그가 4번 재발했다" — DM 화면 동시성 버그를 라이브러리 교체로 근절**

<details>
<summary>📖 <strong>▶ 펼쳐서 보기 — 리뷰 5라운드 타임라인 · Before/After 다이어그램 · 정량 지표</strong></summary>

1:1 DM 화면(WBS 4.7.3.2, REST 폴링 기반) 코드 리뷰 5라운드 동안 "배지가 안 바뀐다", "새 대화가 옛 대화로 덮인다", "읽음 처리가 되돌아간다"처럼 겉보기엔 서로 다른 버그가 계속 재발했습니다. 원인은 하나였습니다 — 컴포넌트마다 "이 비동기 응답이 지금도 유효한가?"를 손으로 판정하는 ref를 각자 새로 발명하고 있었던 것입니다(`sequenceRef`, `readGenerationsRef` 등 한 화면에만 7개).

```mermaid
flowchart LR
    subgraph before["Before — 손으로 관리하는 캐시"]
        direction TB
        B1["컴포넌트 A: useState + fetch"] -.같은 데이터.-> B2["컴포넌트 B: useState + fetch"]
        B1 --> BG["ref 7개로 최신성 직접 판정"]
        B2 --> BG
    end
    subgraph after["After — React Query가 관리하는 캐시"]
        direction TB
        A1["컴포넌트 A: useQuery(key)"] --> QC["QueryClient 캐시<br/>(key로 격리)"]
        A2["컴포넌트 B: useQuery(같은 key)"] --> QC
        QC -->|dedup / 최신 fetch만 커밋| A1
        QC -->|dedup / 최신 fetch만 커밋| A2
    end
    before -. "이슈 #217 → 마이그레이션" .-> after
```

TanStack Query를 도입해 손으로 짠 방어 코드를 걷어내고 캐시 최신성 판정 자체를 라이브러리에 위임했습니다. 다만 라이브러리가 mutation/cache lifecycle은 관리해도 `router.push()` 같은 side effect는 자동으로 취소해주지 않는다는 것도 함께 드러나, 그 경계에 남은 gap(로그아웃 시 캐시 미분리, 언마운트 후 남은 라우팅)은 별도로 막았습니다. 이 원리를 검색·활동 피드 화면에도 감사해서 같은 방식으로 정리했습니다.

| 지표 | Before → After |
|---|---|
| 손으로 짠 방어 코드(DM+검색+활동 피드) | 9개 → 0개 |
| 코드 리뷰 라운드(승인까지) | 6회 |
| DM 회귀 테스트 | 0개 → 12개 |
| 적용 범위 | DM 1개 화면 → DM·검색·활동 피드 3개 화면 |

과정 전체는 [dm-react-query-마이그레이션-트러블슈팅.md](docs/08-members/member-정철진/기술-연구/dm-react-query-마이그레이션-트러블슈팅.md)에 리뷰 라운드별 타임라인, 시퀀스 다이어그램과 함께 정리해뒀습니다.

</details>

**AI는 3D 좌표와 방향 감각이 약하다 — 월드 좌표 실측 + 임시 방향지시물로 해결**

<details>
<summary>📖 <strong>▶ 펼쳐서 보기 — 방향 마커 다이어그램 · 실측 원칙 · 관련 코드</strong></summary>

3D 허브(S03 메인 허브) 작업 초반에는 문제가 없었지만, 책상·의자 같은 가구를 세부적으로 배치하면서부터 AI에게 3차원 좌표(x, y, z)와 방향(동서남북)을 그대로 맡기면 자꾸 어긋났습니다. 텍스트 기반으로 좌표를 계산해 배치를 지시하다 보니, 실제 씬에서는 물체가 반대 방향을 보거나 의도한 자리에서 벗어나는 일이 반복됐습니다.

```mermaid
graph TD
    C["중앙 C<br/>오피스 기준점 (2.5, -3)"]
    N["N 북<br/>-Z"]
    S["S 남<br/>+Z"]
    E["E 동<br/>+X"]
    W["W 서<br/>-X"]
    C --- N
    C --- S
    C --- E
    C --- W
```

"손 계산 대신 항상 실측"으로 원칙을 바꿔서 해결했습니다.

1. 씬 중앙을 기준으로 동서남북에 색이 다른 기둥과 좌표 라벨을 세우는 `DebugDirections` 컴포넌트를 임시로 만들어, 방향과 좌표를 눈으로 직접 확인할 수 있게 했습니다.
2. 이후 가구 배치는 계산값을 그대로 믿지 않고, `getWorldPosition()`으로 실제 렌더링된 월드 좌표를 읽어와 확정하는 방식으로 바꿨습니다(예: 책상 8석 좌표를 전부 이 방식으로 재확정).

이 원칙은 이후 프로젝트의 3D 작업 컨벤션으로 굳어져, 지금도 씬에 새로 뭔가를 배치할 때는 `SHOW_DEBUG_DIRECTIONS` 플래그로 방향지시물을 잠깐 켜서 기준으로 삼습니다(`frontend/components/hub/DebugDirections.tsx`). 회전이 섞인 배치에서 위치가 어긋나거나 모델 pivot이 반대라 벽을 뚫었던 구체적인 사고 사례는 [3d-배치-ai-공간지각-한계-실측-트러블슈팅.md](docs/08-members/member-정철진/기술-연구/3d-배치-ai-공간지각-한계-실측-트러블슈팅.md)에 정리해뒀습니다.

</details>

그 밖에도 아래 버그를 직접 찾아 고쳤습니다.

- **그래프 화면 노드 겹침**: 문서 카드가 6개를 넘어 두 번째 서브열이 생기면, 이슈 열의 시작 x좌표가 고정값이라 그 서브열과 겹쳐 보이는 버그가 있었습니다. 이슈 열 시작 위치를 "문서 열이 실제로 차지하는 폭 + 여백" 기준으로 다시 계산하도록 고치고, 회귀 테스트를 추가했습니다.
- **로그인 실패가 세션 만료로 오분류되는 문제**: axios 인터셉터가 모든 401을 "세션 만료"로 보고 refresh 실패 시 무조건 `/login`으로 리다이렉트해서, 로그인 자격증명 오류 같은 화면 자체의 인라인 오류 메시지가 사용자에게 보일 새도 없이 덮여버렸습니다. "401이 곧 비즈니스 로직 실패"인 endpoint를 인터셉터 예외 목록으로 분리해 해결했습니다.

### Backend

- **존재확인·권한확인 순서 버그(반복 패턴)**: Company → Department → Project → CompanyMember 4개 도메인에서 같은 클래스의 버그(리소스 존재 확인보다 권한 확인이 먼저 이뤄져야 하는데 순서가 뒤바뀐 문제)가 반복 발견됐습니다. 전 도메인을 동일 기준으로 수정했고, 이후 신규 도메인은 같은 체크리스트로 셀프 리뷰를 거치도록 프로세스화했습니다.
- **Cross-project thread 접근 노출**: `threadId`의 `projectId` 소속 검증이 누락되어, 다른 프로젝트의 권한·문서·이슈·메시지 링크가 노출되던 문제를 찾아 수정했습니다.
- **User enumeration 취약점 제거**: 초대 후보 조회 API의 이메일 조회 순서 문제로 `PROJECT_ADMIN`이 임의 이메일의 서비스 가입 여부를 알아낼 수 있던 문제를 수정했습니다.

---

## 품질 관리

- **5단계 검증 체계**: Test · Build · API · DB · 화면 영역별 확인 기준을 문서화했습니다.
- **구현자 자체검증 + QA 교차검증**: 구현 담당자가 `./gradlew test` + curl로 1차 확인하고, QA 담당이 Apidog collection으로 43개 케이스를 교차 검증합니다.
- **"증거 없으면 미완료" 원칙**: 정상 케이스만 확인하거나 증빙 없는 작업은 완료로 인정하지 않습니다.
- **실제 커버리지**: 134개 테스트 · 20개 도메인 · PR마다 CI가 실 MySQL에 연결해 자동 검증합니다.

---

## 기술 스택

| 영역 | 구성 |
|---|---|
| Backend | Java 17, Spring Boot 3.5, Spring Data JPA, Spring Security(JWT + OAuth2), MySQL 8.4 |
| Frontend | Next.js 16(App Router), React 19, TypeScript, TanStack Query, Tailwind CSS 4, React Three Fiber(3D 허브) |
| 파일 저장 | AWS S3 호환 스토리지(로컬 개발은 MinIO) |
| 테스트 | JUnit 5 + Testcontainers(Backend), Playwright(Frontend smoke test) |
| 인프라 | AWS(EC2, RDS, S3, ECR, SSM 배포), Docker Compose(로컬 개발) |

---

## 아키텍처

<details>
<summary>🏗️ <strong>▶ 펼쳐서 보기 — 런타임 구성 · 배포 파이프라인 다이어그램</strong></summary>

### 런타임 구성

```mermaid
graph LR
    U["사용자<br/>HTTPS"] --> R53["Route 53<br/>threadia.click / api.threadia.click"]
    R53 --> NGINX["EIP + Nginx<br/>TLS 종단(Certbot) · :443 → 내부 proxy"]
    subgraph EC2["EC2 · Docker Compose(dual image)"]
        FE["frontend 컨테이너<br/>Next.js SSR · 127.0.0.1:3000"]
        BE["backend 컨테이너<br/>Spring Boot · 127.0.0.1:8080"]
    end
    NGINX --> FE
    NGINX --> BE
    BE --> RDS[("RDS MySQL<br/>private subnet · EC2 SG만 inbound")]
```

프론트엔드와 백엔드를 같은 EC2 위에서 서로 다른 ECR 이미지로 독립 실행하고, Docker Compose는 public port를 열지 않고 Nginx만 외부에 노출합니다.

### 배포 파이프라인

```mermaid
flowchart LR
    GH["GitHub Actions<br/>OIDC · 장기 key 없음"] --> ECR["ECR push<br/>digest 고정 이미지"]
    ECR --> S3M["S3 manifest<br/>versioned release"]
    S3M --> SSM["SSM Command<br/>SSH 없이 전달"]
    SSM --> EC2D["EC2 배포 스크립트<br/>양쪽 health check 후 원자적 전환"]
```

health check가 실패하면 기존 버전을 그대로 유지합니다(rollback 안전). secret은 저장소에 두지 않고 root-only runtime env로만 주입합니다.

</details>

---

## 핵심 도메인 모델

회사(workspace) 아래 프로젝트가 있고, 프로젝트 아래에서 이슈·문서·채팅·쓰레드가 서로 연결되는 구조입니다. 전체 ERD는 [docs/03-design/ERD.md](docs/03-design/ERD.md)에서 확인할 수 있습니다.

<details>
<summary>🗂️ <strong>▶ 펼쳐서 보기 — 협업 도메인 · 운영·관리자 도메인 ERD</strong></summary>

### 협업 도메인

```mermaid
erDiagram
    users ||--o{ oauth_accounts : "소셜계정"
    users ||--o{ company_members : "소속"
    companies ||--o{ company_members : "보유"
    companies ||--o{ departments : "보유"
    companies ||--o{ projects : "소유"
    companies ||--o{ invitations : "회사초대"

    users ||--o{ project_members : "참여"
    projects ||--o{ project_members : "보유"
    projects ||--o{ invitations : "프로젝트초대"
    projects ||--o{ sprints : "보유"
    projects ||--o{ issues : "보유"
    projects ||--o{ documents : "보유"
    projects ||--o{ messages : "보유"
    projects ||--o{ threads : "보유"
    projects ||--o{ activity_events : "기록"

    threads |o--o{ messages : "연결"
    sprints ||--o{ issues : "포함"
    issues ||--o{ issue_status_history : "이력"
    messages ||--o{ issues : "원본"
    issues ||--o{ document_issue_links : "연결"
    documents ||--o{ document_issue_links : "연결"

    threads ||--o{ thread_comments : "보유"
    users ||--o{ thread_comments : "작성"

    companies ||--o{ direct_message_rooms : "범위"
    direct_message_rooms ||--o{ direct_messages : "보유"
    direct_message_rooms ||--o{ dm_room_user_states : "읽음추적"

    activity_events ||--o{ notification_events : "필요시알림생성"
    notification_events ||--o{ notification_recipients : "수신"
    users ||--o{ notification_recipients : "수신"
    users ||--o{ notification_preferences : "설정"

    users ||--o{ invitations : "초대대상"
    invitations ||--o| company_members : "수락시회사소속"
    invitations ||--o| project_members : "수락시프로젝트소속"
```

### 운영·관리자 도메인

```mermaid
erDiagram
    users ||--o| platform_admins : "플랫폼권한"
    users ||--o{ support_cases : "신고자"
    support_cases ||--o{ support_case_public_replies : "공개답변"
    support_cases ||--o{ support_case_internal_notes : "내부메모"
    support_cases ||--o{ admin_action_audits : "감사연결"
    platform_admins ||--o{ support_case_public_replies : "작성"
    platform_admins ||--o{ support_case_internal_notes : "작성"
    platform_admins ||--o{ admin_action_audits : "관리자행위"
    system_settings ||--o{ admin_action_audits : "설정변경감사"
```

</details>

---

## 문서

기능/화면/API/DB 설계의 canonical 기준 문서는 [docs/02-requirements/요구사항-정의서.md](docs/02-requirements/요구사항-정의서.md)에서 각 영역별 문서로 안내합니다.

| 구분 | 기준 문서 |
|---|---|
| 기능 요구사항 | `docs/02-requirements/기능-분해표.md` |
| 화면 기준 | `docs/03-design/화면정의서.md` |
| API 기준 | `docs/04-api/API-명세서.md` |
| DB 기준 | `docs/03-design/ERD.md`, `db/schema.sql` |
| Architecture | `docs/05-architecture/` |

전체 문서는 [docs/](docs/) 아래에 있으며, 작성 규칙은 [docs/00-project/문서-작성-규칙.md](docs/00-project/문서-작성-규칙.md)를 따릅니다. 폴더별 용도는 [docs/README.md](docs/README.md)에 정리되어 있습니다.

---

## 기여 규칙

Git, commit message, PR 기준은 [CONTRIBUTING.md](CONTRIBUTING.md)를 따릅니다. 코드/문서/Git 작업 전 공통 작업 규칙은 [AGENTS.md](AGENTS.md)(또는 Claude Code 사용자는 [CLAUDE.md](CLAUDE.md))를 참고해 주세요.

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

---

## 로컬 개발 환경 실행

### 1. 환경 변수

```bash
cp .env.example .env
```

`.env`의 `REPLACE_ME_*` 값을 로컬 값으로 채워주세요. `SPRING_PROFILES_ACTIVE=local`은 로컬 개발에서 `S3StorageConfig`의 production 검증만 우회하는 선택 키입니다.

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

프론트엔드는 기본적으로 `http://localhost:3000`, 백엔드 API는 `http://localhost:8080`에서 실행됩니다.

---

## 테스트

### Backend

```bash
cd backend
./gradlew test
```

### Frontend

```bash
cd frontend
npx tsc --noEmit
npm run lint

npm run test:smoke
```
