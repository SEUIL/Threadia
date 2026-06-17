> **작성일**: 2026-06-17
> **작성자**: 문서 검수
> **상태**: 검토 기록 (팀 공유용)
> **범위**: 6일차 수정본 이후 추가 머지 3커밋(invitation 정밀화) 검수 → 김민정 「Sprint 1.5 invitation 검토안」 연계 → 검토안 메타검수
> **기준 문서**:
> - `06_기획주간_6일차_자료/분업 산출물/5일차 최종본 수정/` (6일차 수정본 v1)
> - `Sprint1.5-invitation-검토안-우선순위분류.md` (김민정, 2026-06-17)

# Sprint 1.5 invitation 검토안 메타검수

## 0. 이 문서의 위치

검수 흐름은 다음과 같다. 이 문서는 **③부터** 기록한다(① 1차 검수 9개 이슈, ② 6일차 수정본의 9개 이슈 클로즈는 이미 저장소 `Threadia-기획문서-검수리포트.md`와 6일차 수정본 `README.md`에 기록되어 있으므로 생략).

```
① 1차 검수 (9개 이슈)           → 저장소 검수리포트
② 6일차 수정본 (9개 이슈 클로즈) → 6일차 수정본 README
③ 추가 머지 3커밋 (invitation 4종 정밀화)   ← 본 문서 §1
④ 김민정 검토안 (A/B/C 우선순위 분류)        ← 본 문서 §2
⑤ 검토안 메타검수 (항목별 판정 + 보완)        ← 본 문서 §3~§5
```

---

## 1. 추가 머지 3커밋 검수 — invitation 4종 정밀화

6일차 수정본(9개 이슈 클로즈) 이후 들어온 3개 커밋을 diff 단위로 확인했다. 모두 invitation 계약을 조이는 보정이며, **구조 변경이 아니라 제약 추가/명시**다.

| 커밋 | 제목 |
|---|---|
| `253a648` | invitation API와 ERD 계약 보정 |
| `56b5049` | Sprint 1.5 invitation 실행 기준 보정 |
| `ee0533b` | invitation 화면과 기능 기준 보정 |

### 1.1 변경 내용 (4종)

| # | 변경 | 적용 문서 | 평가 |
|---|---|---|---|
| M-1 | **invitation role 제약** — 회사 초대는 `COMPANY_ADMIN/MEMBER`, 프로젝트 초대는 `PROJECT_ADMIN/MEMBER`만 허용 (`invitationRoleByScope` enum 신설) | ERD `invitations.role`, API §2 Enum, 화면 S14, 기능분해표, 추적표 | 타당. 역할 오·부여 차단 |
| M-2 | **프로젝트 초대 대상 제한** — 같은 회사 `ACTIVE` company member에게만 생성, 위반 시 `400 VALIDATION_ERROR` | ERD §4.5, API, 화면 S14, DoD, Backlog, 추적표 | 타당. RBAC 오염 방지 |
| M-3 | **invitation 알림 범위 축소** — Sprint 1.5는 내부 `notification_events` 원장 기록까지만. **Push 알림·별도 Invitation notification preference·S11 사용자 알림 UI 필수 구현은 제외** | 전 문서 + Phase B 메모 #1·#3 | 의도는 타당하나 **부작용 있음 → §3 A-3 참조** |
| M-4 | **Sprint 1 login context = 빈 값** — Sprint 1 `login` 응답 `companies: []` 고정, `/auth/me`의 `companyMemberships` 분기는 "Sprint 1 이후 후속 MVP reference"로 이동 | API(login·/auth/me 예시), 화면 S01·S03 | **개선**. 이전 모순 해소(아래) |

### 1.2 검수 verdict

- **정합성 양호, 새 구조 이슈 없음.** 4종이 9개 문서에 **동일 문구로 일관 전파**됐다(ERD·API·화면·기능분해표·추적표·DoD·Backlog·Sprint detail·백엔드 DoD).
- **M-4는 실질 개선.** 직전까지 Sprint 1 `login` 예시가 `companies: [{companyId:10, role:COMPANY_ADMIN}]`을, `/auth/me`가 full membership을 반환해 "Auth-only인데 회사 컨텍스트가 왜 있나"는 모순이 있었다. 이제 `companies: []`로 정리되어 Sprint 1 = 회사/멤버십 없음과 일치한다.
- **M-3은 클로즈가 아니라 새 의존을 만들었다.** "내부 `notification_events` 원장 기록은 1.5 범위"로 못박으면서, MVP/Sprint 2 테이블인 `notification_events`를 Sprint 1.5 의존으로 끌어왔다. → 이 부작용을 검토안 A-3이 정확히 포착했다(§3).

### 1.3 1.2 시점에 남긴 열린 질문 → 검토안 A-1로 승계

이 머지 검수 직후, 다음 열린 질문을 제기했다.

> invitation(Sprint 1.5)은 회사가 이미 있어야 동작하는데, 회사 **생성** API는 "MVP reference(Sprint 2 이후)"다. → Sprint 1.5에서 첫 회사를 어떻게 만들지 팀에서 짚어야 함.

이 질문이 김민정 검토안 **A-1 회사 부트스트랩**으로 정식 승계되어 해결안까지 붙었다. 검수 체인이 끊기지 않고 연결됨을 확인한다.

---

## 2. 김민정 「Sprint 1.5 invitation 검토안」 요약

검토안은 6일차 수정본 + 추가 3커밋 이후 **남은 "착수 시 실제로 막히는 항목"**을 결정 난이도/의존 변경 기준으로 A/B/C로 분류한 것이다.

### A. 착수 전 무조건 정해야 할 것 (블로커 — 범위·의존 테이블 변경)

| 항목 | 문제 | 권장 해결 |
|---|---|---|
| **A-1 회사 부트스트랩** | 회사 초대를 만들려면 회사 + COMPANY_ADMIN inviter가 선행돼야 하나 회사 생성 API는 Sprint 2. 회사가 생기는 경로가 없음 | seed 부트스트랩(마이그레이션으로 데모 회사 1개 + 첫 사용자 COMPANY_ADMIN) vs 최소 회사 생성 API 1.5 포함 |
| **A-2 프로젝트 초대 위치** | 프로젝트 초대는 `projects`+`project_members` 의존 → 회사 초대보다 테이블 2개 더 필요 | 회사 초대만 1.5, 프로젝트 초대는 Sprint 2로 분리 |
| **A-3 notification_events** | 6일차 수정본이 "원장 기록은 1.5"로 못박아 Sprint 2 테이블 의존이 추가됨 | 원장 기록을 optional("필요 시")로 낮춰 의존 제거 vs 최소 테이블 1.5 포함 |

### B. 권고사항 (표준 해법 있음, 코딩 전 합의 권장)

| 항목 | 문제 | 해결 |
|---|---|---|
| **B-1 PENDING 중복 동시성** | "동일 scope·대상 PENDING 1건"인데 MySQL 8.4는 부분 유니크 인덱스 미지원. 단순 UNIQUE는 거절·취소·만료 과거 초대까지 막아 재초대 불가 | 앱 레벨 트랜잭션+행 잠금으로 active pending 검사 (또는 status 포함 generated column + unique 트릭). 단순 unique index 금지 |
| **B-2 타임존 UTC** | ERD `expires_at DATETIME`(TZ 없음) vs API 예시 `"...Z"`(UTC). MySQL DATETIME은 TZ 미저장 → 7일 만료 계산이 서버 로컬 TZ에 흔들림 | 전 시각 UTC 저장(또는 TIMESTAMP), 응답 ISO-8601 UTC. Backend DoD에 1줄 |
| **B-3 이메일 정규화** | `users.email UNIQUE`에 대소문자 정규화 기준 없음 → 중복 가입·lookup 빗나감 | 가입·로그인·lookup 모두 lowercase 정규화. Sprint 1부터 |
| **B-4 인증 보안 3종** | ① OAuth 이메일 자동연결 무검증(계정탈취) ② 비번재설정 후 refresh_tokens revoke 누락 ③ login에서 `users.status` 미검증 | ① 이메일 소유 확인 후 연결/명시적 연결 단계 ② confirm 성공 시 전체 refresh_tokens revoke ③ status != ACTIVE 거부 |

### C. 개발 착수하며 정해도 되는 것

| 항목 | 성격 |
|---|---|
| **C-1 accept 멱등성 응답** | `membershipCreated` 값 정의 — 구현 중 확정 가능 |
| **C-2 추적표 notification 표기 동기화** | A-3 결정 후 한 줄 수정 — 문서 위생 |
| **C-3 만료 정리 batch/sweeper** | 문제 아님(이미 lazy expiration·batch 없음으로 설계) |
| **C-4 rate limiting** | 배포 단계 관심사, 기획 범위 밖 |

---

## 3. 메타검수 — 항목별 판정

판정 기준: **(가) 문제의 실재성** / **(나) 기술 분석 정확성** / **(다) 분류 적절성** / **(라) 해결안 타당성**. 판정 태그: ✅ 동의 · ⭐ 핵심 catch · ⚠ 동의+주의 · ◐ 보완 필요.

### A-1 회사 부트스트랩 — ✅ 동의

- **(가)(나)** 실재. 회사 초대 inviter는 COMPANY_ADMIN이어야 하고(M-1으로 role 제약까지 추가됨), 회사·company_members가 선행돼야 한다. 회사 생성 API는 ERD 엔티티 목록상 `companies = MVP reference, Sprint 1 구현 제외`이므로 1.5엔 회사 생성 경로가 없다. → 정확.
- **(다)** A(블로커) 분류 타당. "회사가 어떻게 존재하는가"는 의존 테이블·마이그레이션을 바꾸는 결정이다.
- **(라)** seed vs 최소 생성 API 둘 다 유효. **seed 권장이 합리적** — `invitations` + `company_members`만 새로 만들면 되고 Sprint 1/2 순서 불변. 단, seed 채택 시 "데모 회사/관리자 계정"이 운영 데이터에 섞이지 않도록 환경 분리(로컬/데모 전용) 조건을 1줄 덧붙이면 더 안전.

### A-2 프로젝트 초대 위치 — ⚠ 동의 + 주의

- **(가)(나)** 실재·정확. 프로젝트 초대는 `projects`+`project_members` 의존. M-2가 "같은 회사 ACTIVE member 대상"까지 더해 사실상 회사+프로젝트 멤버십 모델을 함께 끌고 온다.
- **(다)(라)** "회사 초대만 1.5, 프로젝트 초대 Sprint 2" 권장은 의존 절감 논거로 타당.
- **⚠ 주의(보완점 #1)**: 이 권장은 **방금 머지된 6일차 확정 문서를 되돌리는(reversal) 결정**이다. 현재 API(`POST /projects/{projectId}/invitations` = Sprint 1.5), ERD, 추적표 INVITE row, 화면 S12, DoD, Backlog가 **모두 프로젝트 초대를 1.5로 명시**하고 있고, 직전 커밋(`253a648`/`ee0533b`)이 오히려 이를 강화(ACTIVE member 제약 추가)했다. 따라서 단순 "정하면 됨"이 아니라 **확정 범위 축소 결정**으로 다뤄야 하며, 수용 시 다수 문서 재편집이 따른다(§4 영향 매트릭스).

### A-3 notification_events — ⭐ 핵심 catch (+ ◐ 보완)

- **(가)(나)** 가장 날카로운 지적. **직전 머지 M-3이 만든 회귀를 정확히 포착**했다. M-3은 "push/UI는 제외하되 원장 기록은 1.5"로 정리했는데, `notification_events`는 ERD 엔티티 목록상 `MVP` 테이블(Sprint 2 권역)이다. → 1.5가 이 테이블에 새로 의존하게 됨. 정확.
- **(다)** A(블로커) 분류 타당 — 테이블 의존을 바꾼다.
- **(라)** "원장 기록 optional로 낮춰 의존 제거" 권장 합리적. 1.5는 `GET /users/me/invitations` 목록 조회만으로도 닫힌다.
- **◐ 보완(보완점 #2)**: 현재 문구가 이미 "`notification_events`에 **필요 시(if needed)** 추가한다"로 반쯤 열려 있다. 즉 의존이 이미 soft하다. 다만 **"테이블 자체를 1.5에 만드느냐"**는 여전히 미결이므로 A 유지는 타당. 결정 질문을 다음 한 문장으로 좁히면 빨라진다 → **"1.5는 내 초대 목록 조회(GET /users/me/invitations)만으로 닫히는가? `notification_events` 테이블 없이?"**

### B-1 PENDING 중복 동시성 — ✅ 동의

- **(나)** 정확. MySQL(InnoDB)은 PostgreSQL식 부분/조건부 유니크 인덱스를 지원하지 않는다. 단순 `UNIQUE(company_id, invitee_user_id)`로 막으면 `DECLINED/CANCELED/EXPIRED` 과거 행까지 충돌시켜 재초대를 차단한다.
- **(라)** 앱 레벨 트랜잭션 + 행 잠금이 표준. 보조로 제시한 "status 포함 generated column + unique index" 트릭도 유효(예: `PENDING`일 때만 키 값을 갖고 아니면 NULL → MySQL의 다중 NULL 허용으로 부분 유니크 효과). 정확.
- **(다)** B 분류 타당(표준 해법 존재, 범위 불변).

### B-2 타임존 UTC — ✅ 동의 + 적용 범위 확장

- **(나)** 정확. ERD `expires_at DATETIME`은 TZ를 저장하지 않는다. API 예시의 `Z`(UTC)와 어긋나 만료 계산이 서버 로컬 TZ에 흔들린다.
- **◐ 보완(보완점 #3)**: 이 문제는 invitation만이 아니라 **Sprint 1 토큰 테이블**에도 동일하게 적용된다 — `refresh_tokens.expires_at`(14일), `password_reset_tokens.expires_at`. 따라서 **Sprint 1 사안**으로 올려 잡아야 한다. "전 시각 UTC 저장, 응답 `Z` 통일"을 Backend DoD 공통 기준으로 두는 것을 권장.

### B-3 이메일 정규화 — ✅ 동의

- **(나)(라)** 정확. `users.email UNIQUE`에 정규화 기준이 없으면 `User@x.com`/`user@x.com` 중복 가입이 뚫리고 invitation exact email lookup이 빗나간다. 가입·로그인·lookup lowercase 정규화는 표준이며 **Sprint 1 인증부터** 적용이 맞다.

### B-4 인증 보안 3종 — ✅ 동의 + 격상 권고

- **(가)(나)** 세 가지 모두 실재하는 명세 공백:
  - **OAuth 자동연결**: ERD §4.2가 "같은 이메일의 로컬 계정이 있으면 **계정 연결 정책을 적용한다**"고 **언급만 하고 정책을 정의하지 않는다**. 무검증 자동연결이면 provider 이메일을 위조/탈취한 공격자가 기존 로컬 계정을 흡수할 수 있다(전형적 OAuth account-linking 취약점).
  - **비번재설정 후 세션**: ERD §4.4 `refresh_tokens.revoked_at`은 있으나, `password-reset/confirm` 성공 시 해당 user의 refresh_tokens를 revoke하라는 규칙이 어느 문서에도 없다. → 비번을 바꿔도 탈취된 세션이 유지된다.
  - **status 미검증**: ERD `users.status ENUM('ACTIVE','SUSPENDED','DELETED')`는 있으나 login에서 이를 거르는 규칙이 없다.
- **◐ 보완(보완점 #4)**: B(권고)에 있으나 **세 항목 모두 Sprint 1(인증) 범위이며 보안 critical**이다. 특히 ②(세션 revoke)·③(status 차단)은 "권고"보다 **must**에 가깝다. "B = 나중"으로 읽히지 않도록 **적용 시점 = Sprint 1**을 명시하고, 가능하면 ②③은 A에 준해 다루기를 권장.

### C-1~C-4 — ✅ 동의

- **C-1** accept 멱등성: "이미 멤버면 `membershipCreated:false` + status ACCEPTED"는 응답 디테일이라 구현 중 확정 타당.
- **C-2** 추적표 동기화: A-3 결정에 종속된 문서 위생. 타당.
- **C-3** 만료 정리: 이미 lazy expiration·batch 없음으로 설계됨 → "문제 아님" 판단 정확.
- **C-4** rate limiting: 배포 단계 관심사로 분류 적절.

---

## 4. A-2·A-3 수용 시 영향 문서 매트릭스 (보완점 #1 상세)

검토안 요약표에는 영향 범위가 드러나지 않는다. A-2·A-3은 확정 문서 reversal이므로, 수용 시 아래 문서를 함께 수정해야 한다. **C-2(추적표 한 줄)보다 큰 작업**임을 명시한다.

| 결정 | 수정해야 할 문서 | 변경 내용 |
|---|---|---|
| **A-2 채택** (프로젝트 초대 → Sprint 2) | API 명세서 v2.3 | `POST /projects/{projectId}/invitations` 상태를 1.5 → Sprint 2 |
| | ERD v2.0 §4.5 / 엔티티 목록 | `invitations` 설명에서 프로젝트 초대 1.5 표현 제거 |
| | 화면정의서 S12/S14 | 프로젝트 초대 흐름 Sprint 2 표기 |
| | 기능 분해표 (INVITE row) | 프로젝트 초대 Sprint 구분 |
| | 추적표 (INVITE-0x) | 프로젝트 초대 row 시점 |
| | Sprint1_Backlog / Sprint 1_detail / 백엔드 DoD | "프로젝트 invitation API" 작업을 1.5에서 제거 |
| **A-3 채택** (원장 기록 optional/제외) | ERD §4.5 | "내부 notification_events 원장 기록" 문구 완화 |
| | API 명세서 §9 / invitation 섹션 | 동일 |
| | DoD / 화면 S14 / 기능분해표 | "invitation notification = 내부 원장" 문구 walk-back |
| | 추적표 (C-2) | ERD 칸 동기화 |

> 권고: 검토안 요약표에 **"수용 시 영향 문서 수"** 칸을 추가하면 결정 비용이 한눈에 보인다. (A-1=낮음, A-2=높음, A-3=중간)

---

## 5. 종합 결론

### 5.1 검토안 자체 평가

- **완성도 높음. 그대로 팀 합의 안건으로 상정 가능한 수준.** A/B/C 분류 기준("결정 난이도 × 의존 테이블 변경")이 타당하고, 기술 분석(MySQL 부분 유니크 미지원, DATETIME TZ, OAuth 자동연결, generated column 트릭 등)이 정확하다.
- **A-3은 직전 머지가 만든 회귀를 잡은 핵심 기여.** 검수 체인이 실제로 회귀를 거르고 있음을 보여준다.

### 5.2 본 메타검수가 더한 4개 보완

| # | 보완 | 대상 |
|---|---|---|
| 1 | A-2·A-3은 "결정"이 아니라 **확정 문서 reversal** → 영향 문서 매트릭스(§4)로 결정 비용 명시 | A-2, A-3 |
| 2 | A-3은 이미 "필요 시"로 soft함 → 결정 질문을 **"GET /users/me/invitations 조회만으로 1.5가 닫히는가"**로 좁힘 | A-3 |
| 3 | B-2(타임존)는 invitation만이 아니라 **Sprint 1 토큰 테이블**(refresh/reset)에도 적용 → Sprint 1 공통 DoD로 | B-2 |
| 4 | B-3·B-4는 **Sprint 1 적용 시점** 명시. 특히 B-4 ②③(세션 revoke·status 차단)은 must에 준함 | B-3, B-4 |

### 5.3 권고 처리 순서

1. **A 그룹(블로커) 3건을 1.5 착수 전 합의** — A-1(seed 권장), A-2(회사 초대만 1.5 권장), A-3(원장 optional 권장). 셋을 권장안으로 정하면 Sprint 1.5 신규 산출물은 사실상 `invitations` + `company_members` 2개 테이블 + 회사 초대 API로 최소화되고 Sprint 1/2 순서는 불변.
2. **A-2·A-3 채택 시 §4 영향 문서를 같은 PR로 동기화** (문서 정합성 회귀 방지).
3. **B-2·B-3·B-4를 Sprint 1 DoD에 반영** — invitation(1.5)이 아니라 인증(Sprint 1)에서 지금 막아야 할 보안·정합성 항목.
4. C 그룹은 현행 분류대로 구현 중/배포 단계 처리.

### 5.4 남은 caveat

- 본 문서는 **기획 정합성·착수 전 합의 목록 검수**다. 실제 schema migration·Backend/Frontend 구현·일정 확정은 포함하지 않는다.
- A 그룹은 "무엇을 만들지/어떤 테이블에 의존할지"를 바꾸는 결정이므로, 합의 없이 코딩에 들어가면 1.5 착수 직후 막힌다.
