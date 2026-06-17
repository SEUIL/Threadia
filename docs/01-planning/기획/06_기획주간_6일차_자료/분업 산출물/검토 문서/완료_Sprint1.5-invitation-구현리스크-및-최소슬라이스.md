# Sprint 1.5 invitation 구현 리스크 및 최소 슬라이스 설계

> **작성자**: 김민정
> **작성일**: 2026-06-17
> **상태**: 팀 공유용 검토본
> **기준 문서**: 06_기획주간_6일차_자료/분업 산출물/5일차 최종본 수정 (ERD/API/화면정의서 6일차 수정본 v1)
> **목적**: 6일차 수정본 구조를 확정 전제로, 실제 구현 시 발생할 문제와 개선 방향, 그리고 Sprint 순서 변경 없이 invitation을 진행하는 방법을 정리한다.

---

## 1. 전제

- Sprint 1은 Auth-only로 확정한다. (회원가입/로그인/로그아웃/`/auth/me`/Google OAuth/비밀번호 재설정)
- Sprint 1 DB는 `users`, `oauth_accounts`, `password_reset_tokens`, `refresh_tokens` 4개로 제한한다.
- invitation은 Sprint 1.5에서 등록 사용자 대상 app-internal invitation으로 구현한다.
- 문서 구조(6일차 수정본)는 팀 합의로 확정됐다. 이 문서는 그 구조를 바꾸자는 제안이 아니라, 구현 단계에서 미리 합의해야 할 항목을 정리한 것이다.

---

## 2. 구현 시 발생할 문제

### 🔴 핵심 — invitation은 회사/멤버 테이블을 선행 요구한다

`invitations`는 FK가 `companies.id`, `projects.id`에 걸리고, 수락(accept) 시 `company_members` 또는 `project_members` row를 생성한다. 그런데 이 4개 테이블은 모두 Sprint 2 도메인이다.

- "수락했는데 들어갈 회사/프로젝트가 없는" 초대는 성립하지 않는다.
- 따라서 invitation을 실제로 동작시키려면 최소한의 회사 + 멤버 테이블이 invitation과 함께 있어야 한다.
- 근거: ERD 6일차 수정본 §4.5 (`company_id NOT NULL FK companies.id`, accept 시 membership 생성)

→ 대응은 4장(최소 슬라이스) 참고.

### 🔴 login / `/auth/me` 응답의 `companies[]`

- login 응답 예시는 `companies` 배열에 `COMPANY_ADMIN`을 채워서 반환한다. (API 수정본 라인 517~523)
- 그러나 Sprint 1에는 `company_members` 테이블이 없으므로, Sprint 1 구현체는 이 배열을 항상 빈 배열로 줄 수밖에 없다.
- `/auth/me`에는 "Sprint 1은 minimal context만" 주석이 있으나, login 예시에는 그 주석이 없어 FE가 채워진 값으로 화면 분기를 짜면 어긋난다.

→ 개선: Sprint 1 login 응답은 `companies: []` 고정임을 예시에 명시한다.

### 🟡 PENDING 중복 방지를 단순 unique index로 못 막는다

- 정책: 동일 scope·대상에 대해 PENDING 초대는 1건만 허용(`INVITATION_ALREADY_PENDING`).
- MySQL 8.4는 부분(조건부) 유니크 인덱스를 지원하지 않는다. `UNIQUE(...)`로 처리하면 거절·취소·만료된 과거 초대까지 막혀 재초대가 불가능해진다.
- → 애플리케이션 레벨에서 트랜잭션 + 잠금으로 "active pending 1건"을 검사해야 한다. 단순 unique index 사용 금지.

### 🟡 프로젝트 초대의 선행조건(회사 멤버) 미정의

- "같은 회사 active 구성원만 프로젝트 멤버 가능"이라는 규칙은 있으나, 회사 멤버가 아닌 사람에게 프로젝트 초대를 보낼 수 있는지가 정의되어 있지 않다.
- accept 시점에 회사 멤버가 아니면 어떻게 처리할지(거절 / 회사+프로젝트 동시 가입) 미정.
- → 규칙 추가: "프로젝트 초대는 이미 회사 멤버인 사용자에게만 가능" (또는 프로젝트 초대를 Sprint 2로 분리, 4장 참고).

### 🟡 role 컬럼이 scope와 검증되지 않는다

- `invitations.role`이 `VARCHAR(50)`이고, 생성 body도 자유 문자열(`"role":"MEMBER"`)이다.
- 회사 역할(`COMPANY_ADMIN | MEMBER`)과 프로젝트 역할(`PROJECT_ADMIN | MEMBER`)은 다른 집합인데, COMPANY 초대에 `PROJECT_ADMIN`이 들어가는 잘못된 조합을 막는 검증이 없다.
- → scope_type별 허용 role 검증 명시.

### 🟡 invitation 알림(notification)은 Sprint 1.5에 없다

- `notification_events`/`notification_recipients`는 Sprint 2 도메인이다.
- 화면 S14는 "초대를 S11 알림과 연결할 수 있다"로 optional하게 적혀 있고, 실제 수신 확인은 `GET /api/v1/users/me/invitations` 목록 조회로 동작한다.
- 즉 Sprint 1.5 invitation은 푸시 알림 없이 "받은 초대 목록" 화면만으로 동작한다. 블로커는 아니다.
- → "Sprint 1.5는 푸시 알림 없이 받은 초대 목록 조회만 제공"을 명시해 시나리오의 "알림 수신" 표현과 어긋나지 않게 한다.

### 🟢 타임존 불일치

- API 예시는 `"expiresAt": "2026-06-24T10:00:00Z"` (UTC `Z` 표기)인데, ERD 컬럼은 `expires_at DATETIME`(타임존 없음)이다.
- MySQL `DATETIME`은 타임존을 저장하지 않아 7일 만료 계산이 서버 로컬 타임존에 흔들릴 수 있다.
- → 전 시각을 UTC 저장 기준으로 통일하거나 `TIMESTAMP`를 사용한다.

### 🟢 accept 응답 멱등성 미정의

- accept 응답에 `membershipCreated: true`가 있으나, 이미 멤버인 사용자가 수락하면 어떤 값/상태를 반환할지 정의되어 있지 않다.
- → 이미 멤버일 때의 응답(예: `membershipCreated: false` + ACCEPTED)을 정의한다.

### 🟢 인증 보안 보강 항목 (Sprint 1)

- OAuth 자동 계정 연결: provider 이메일이 기존 로컬 계정과 일치하면 무검증 자동 연결 → 계정 탈취 경로. 이메일 소유 확인 정책 필요.
- 비밀번호 재설정 후 기존 `refresh_tokens` revoke 누락 → 비밀번호를 바꿔도 탈취 세션 유지.
- 이메일 대소문자 정규화 미정의 → 중복 가입 우회 및 exact email lookup 빗나감. lowercase 정규화 기준 필요.
- `users.status`(SUSPENDED/DELETED)를 login에서 거르는 규칙 없음.

---

## 3. Sprint 순서 변경 없이 invitation을 진행하는 방법

### 결론

Sprint 1은 그대로 두고 진행할 수 있다. 단 invitation의 본질이 "수락 → 회사/프로젝트 멤버가 됨"이므로, **최소한의 회사 + 멤버 테이블은 invitation과 함께** 만들어야 한다.

중요한 구분: Sprint 2의 전체 도메인을 앞당기는 것이 아니라, invitation이 소유하는 얇은 한 조각(vertical slice)만 먼저 만든다. Sprint 2의 순서는 바뀌지 않는다.

### 최소 수직 슬라이스

```text
Sprint 1   : (변경 없음) Auth only
Sprint 1.5 : invitation + 회사 최소 슬라이스
             - companies        (id, name, created_at)              ← 최소 컬럼만
             - company_members  (id, company_id, user_id, role, status) ← 최소
             - invitations      (전체)
             - 회사 invitation API + accept/decline/cancel
Sprint 2   : (순서 유지) departments, stats, logo, projects, issues,
             문서, 채팅, 알림 ... 그리고 project invitation
```

`departments`, `stats`, `로고`, `projects`, `issues` 등 풍부한 기능은 전부 Sprint 2에 그대로 남긴다. invitation이 쓰는 컬럼만 가진 `companies`/`company_members`를 먼저 만든다.

### 의존성을 더 줄이는 3가지 레버

| 방법 | 효과 | 트레이드오프 |
|---|---|---|
| 회사 초대만 1.5, 프로젝트 초대는 Sprint 2로 | `projects`/`project_members` 의존 제거. 필요 테이블 절반 | 1.5에선 프로젝트 초대 데모 불가 |
| 첫 회사를 seed로 부트스트랩 | 회사 생성 API도 Sprint 2로 미룰 수 있음 | 데모는 미리 심은 회사 1개 기준 |
| schema-first, API-later | 멤버 테이블 풀 스키마는 두되 invitation 경로만 INSERT | 테이블만 먼저, 나머지 CRUD는 나중 |

세 레버를 합치면 Sprint 1.5에 새로 만드는 것은 사실상 `invitations` + `company_members` 2개 테이블 + 초대 API뿐이다. (companies는 seed)

### 불가능한 방향 (참고)

- 멤버 테이블 없이 invitation만 구현 → accept가 쓸 곳이 없어 "권한 없는 초대"가 됨. 의미 없음.
- `company_id`를 FK 없는 BIGINT로 두고 회피 → accept가 멤버를 못 만들어 핵심 가치 상실.

---

## 4. 팀 합의가 필요한 항목 요약

| 우선순위 | 항목 | 정해야 할 것 | 담당 후보 |
|---|---|---|---|
| 1 | Sprint 1.5 실제 범위 | 회사 최소 슬라이스 포함 여부, 프로젝트 초대 1.5 포함/2 분리 | 팀 전체 |
| 2 | companies 부트스트랩 방식 | seed vs 회사 생성 API 포함 | BE |
| 3 | login `companies[]` | Sprint 1 응답 `companies: []` 고정 명시 | BE + FE |
| 4 | PENDING 중복 방지 | 앱 레벨 트랜잭션 검사 기준 | BE |
| 5 | 프로젝트 초대 선행조건 | 회사 멤버 한정 규칙 | BE |
| 6 | role-scope 검증 | scope별 허용 role 정의 | BE |
| 7 | 타임존 / accept 멱등성 | UTC 저장 기준, 이미 멤버 시 응답 | BE |
| 8 | 인증 보안 보강 | OAuth 자동연결 / 비번재설정 세션무효화 / 이메일 정규화 / status 처리 | BE |

---

## 5. 비고

- 이 문서는 6일차 수정본 구조를 변경하자는 제안이 아니라, 구현 착수 전 합의가 필요한 항목 목록이다.
- 실제 schema migration, Backend/Frontend 구현은 포함하지 않는다.
- invitation 입력 방식(email vs userId)은 6일차 확정 문서 기준으로 진행하기로 했으므로 이 문서에서 다루지 않는다.
