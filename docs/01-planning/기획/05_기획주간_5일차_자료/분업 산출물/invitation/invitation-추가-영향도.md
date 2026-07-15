> **작성자**: 여도현  
> **작성일**: 2026-06-16  
> **최종 수정**: 2026-06-16  
> **버전**: v1.0  
> **상태**: 검토 중

# Invitation 추가 영향도

## 1. 목적

이 문서는 Threadia 현재 ERD/API 기준에서 제거된 `Invitation` 테이블을 다시 추가할 경우 Backend 작업 범위와 구현 난이도를 정리한다.

현재 최종 설계는 회사 구성원과 프로젝트 멤버를 초대 상태 없이 즉시 등록하는 방식이다. 따라서 `Invitation` 추가는 단순 테이블 추가가 아니라 ERD, API, Auth, RBAC, Notification, Email, 화면 상태를 함께 바꾸는 설계 변경이다.

## 2. 현재 확정 기준

| 항목 | 현재 기준 |
|---|---|
| 회사 구성원 추가 | 가입된 사용자 이메일이면 즉시 `company_members` 생성 |
| 미가입 이메일 | `400 VALIDATION_ERROR` 반환 |
| 프로젝트 멤버 추가 | 같은 회사의 active 구성원만 `project_members` 추가 |
| 초대 상태 | 없음 |
| 초대 token/status/mail | 최종 설계에서 제외 |

근거 문서:

| 문서 | 근거 |
|---|---|
| `ERD v2.0.md` | `company_members`는 Invitation 없이 가입된 사용자 즉시 등록 |
| `API 명세서 v2.0.md` | 구성원 API는 즉시 등록이며 미가입 이메일은 400 반환 |
| `화면정의서_v3.md` | S14 구성원 추가 정책이 즉시 등록 기준 |
| `기능 분해표 v3.0.md` | MEMBER-02가 가입된 사용자 즉시 등록, `Invitation` 없음으로 정리 |
| `기능-시나리오-API-ERD 추적표 v1.0.md` | 구성원/권한 흐름이 `users`, `company_members`, `project_members` 기준으로 연결됨 |

## 3. Invitation 추가 시 바뀌는 점

`Invitation`을 추가하면 현재 흐름은 아래처럼 바뀐다.

```text
현재:
관리자가 이메일 입력
→ 가입된 사용자 확인
→ 즉시 company_members 또는 project_members 생성
→ 성공/실패 응답

변경 후:
관리자가 이메일 입력
→ invitation 생성(PENDING)
→ 사용자에게 초대 표시 또는 메일 발송
→ 사용자가 수락
→ company_members 또는 project_members 생성
→ invitation 상태 ACCEPTED 변경
```

핵심 차이는 `member 생성 시점`이다. 현재는 관리자 액션 직후 생성하지만, `Invitation` 방식은 사용자가 수락한 뒤 생성한다.

## 4. Backend 작업 범위

| 영역 | 필요한 작업 | 비고 |
|---|---|---|
| DB | `invitations` 테이블 추가 | 회사 초대와 프로젝트 초대를 같은 테이블로 처리할지 결정 필요 |
| Entity | `Invitation` entity, status enum 추가 | `PENDING`, `ACCEPTED`, `EXPIRED`, `CANCELED` 등 |
| Repository | email/company/project/status 기준 조회 | 중복 pending invite 검사용 |
| API | 초대 생성, 목록 조회, 수락, 취소, 재발송 endpoint 추가 | 기존 member 추가 API와 역할 분리 필요 |
| Service | 초대 생성, token 발급/hash 저장, 만료 검사, 수락 처리 | 수락 시 member 생성 |
| Auth 연동 | 미가입 사용자의 초대 링크 진입 처리 | 회원가입 후 자동 수락 여부 결정 필요 |
| RBAC | 초대 생성 권한과 수락 권한 검증 | pending 상태에서는 권한 없음 |
| Notification | 초대 생성/수락/취소 알림 이벤트 | S11 알림과 연결 가능 |
| Email | 실제 메일 발송 여부 결정 | Sprint 1에서는 제외 권장 |
| Test | 중복, 만료, 취소, 이미 멤버, 권한 없음, 미가입 이메일 케이스 | 실패 케이스가 많아짐 |

## 5. 테이블 초안

```text
invitations
- id
- company_id
- project_id nullable
- email
- target_type: COMPANY | PROJECT
- role
- status: PENDING | ACCEPTED | EXPIRED | CANCELED
- token_hash
- expires_at
- accepted_at
- invited_by_user_id
- created_at
- updated_at
```

### 주요 컬럼 설명

| 컬럼 | 설명 |
|---|---|
| `company_id` | 회사 초대와 프로젝트 초대 모두 회사 범위 안에서 처리하기 위한 기준 |
| `project_id` | 프로젝트 멤버 초대일 때만 사용 |
| `target_type` | 회사 구성원 초대와 프로젝트 멤버 초대를 구분 |
| `email` | 초대 대상 이메일. 미가입 사용자도 저장 가능 |
| `role` | 수락 후 부여할 회사 또는 프로젝트 역할 |
| `status` | 초대 진행 상태 |
| `token_hash` | 초대 수락 token 원문을 저장하지 않기 위한 hash |
| `expires_at` | 초대 만료 시각 |
| `accepted_at` | 수락 완료 시각 |
| `invited_by_user_id` | 초대한 관리자 |

## 6. API 초안

### 6.1 회사 구성원 초대

```http
POST /api/v1/companies/{companyId}/invitations
```

```json
{
  "email": "user@example.com",
  "role": "MEMBER"
}
```

### 6.2 프로젝트 멤버 초대

```http
POST /api/v1/projects/{projectId}/invitations
```

```json
{
  "email": "user@example.com",
  "role": "MEMBER"
}
```

### 6.3 초대 목록 조회

```http
GET /api/v1/companies/{companyId}/invitations?status=PENDING
```

### 6.4 초대 수락

```http
POST /api/v1/invitations/{token}/accept
```

### 6.5 초대 취소

```http
POST /api/v1/invitations/{invitationId}/cancel
```

### 6.6 초대 재발송

```http
POST /api/v1/invitations/{invitationId}/resend
```

`resend`는 실제 email 발송을 Sprint 1에 넣지 않는다면 후속 범위로 둔다.

## 7. 정책 결정 필요 사항

| 결정 항목 | 선택지 | 추천 |
|---|---|---|
| 회사 초대와 프로젝트 초대 | 테이블 분리 / 공통 `invitations` 테이블 | 공통 테이블 |
| 미가입 이메일 | 400 유지 / pending invitation 생성 | pending invitation 생성 시 invitation 의미가 살아남 |
| 수락 방식 | 로그인 필요 / token만으로 수락 | 로그인 필요 |
| 회원가입 연동 | 수동 수락 / 회원가입 후 자동 수락 | 후속 Sprint에서 자동 수락 검토 |
| 메일 발송 | Sprint 1 포함 / 후속 처리 | 후속 처리 |
| 만료 정책 | 만료 없음 / 7일 또는 14일 | 7일 또는 14일 |
| 중복 초대 | 무조건 생성 / pending 있으면 409 | pending 있으면 409 |

## 8. 난이도 평가

| 구현 범위 | 난이도 | 설명 |
|---|---:|---|
| 회사 구성원 pending 상태만 추가, 메일 없음 | 5/10 | 테이블, 생성 API, 목록 조회, 상태 표시 중심 |
| 회사 + 프로젝트 초대 공통 처리 | 6.5/10 | 수락 시 생성 대상이 `company_members`와 `project_members`로 갈라짐 |
| token 수락 + 만료 + 취소 + 재발송 | 7/10 | token hash, 만료, 상태 전이, 중복 처리가 필요 |
| 미가입 사용자 onboarding + 실제 email 발송 | 8/10 이상 | Auth, Email, 비동기 발송, 실패 처리까지 포함 |

## 9. Sprint 적용 판단

Sprint 1에 전체 invitation flow를 넣는 것은 범위가 크다. 현재 Sprint 1 목표가 skeleton, 기본 인증, 기본 구성원/권한 흐름이라면 `Invitation` 전체 구현은 후속 Sprint로 분리하는 것이 안전하다.

추천 우선순위:

| 단계 | 권장 범위 |
|---|---|
| Sprint 1 | 즉시 등록 유지, 성공/실패 피드백 명확화 |
| Sprint 1.5 | 메일 없는 pending invitation 추가 |
| Sprint 2 | email 발송, token 수락, 미가입 사용자 onboarding 추가 |

## 10. 최종 의견

현재 문제는 “초대 상태 피드백이 없다”는 UX 문제다. 이 문제를 해결하기 위해 반드시 full invitation flow가 필요한 것은 아니다.

최소 대응은 다음과 같다.

1. 가입된 사용자를 추가하면 즉시 등록 성공 메시지를 명확히 표시한다.
2. 미가입 이메일이면 “가입된 사용자만 추가할 수 있다”는 실패 메시지를 표시한다.
3. 초대 대기 상태가 꼭 필요하면 `invitations`를 추가하되, Sprint 1에서는 메일 발송 없이 pending 상태까지만 구현한다.

따라서 지금 바로 넣는다면 “메일 없는 pending invitation”까지만 현실적이다. 실제 이메일 초대와 미가입 사용자 onboarding은 Sprint 2로 분리하는 것이 좋다.
