# Threadia 6일차 수정본 v1 최종 검토

> 검토 대상: `5일차 최종본 수정` 폴더의 6일차 수정본 v1  
> 추가 참고: `Sprint1.5-invitation-검토안-메타검수.md`  
> 검토일: 2026-06-17

## 1. 결론

6일차 수정본 v1은 1차 검수에서 지적된 핵심 정합성 문제를 대부분 해결했다.

특히 Sprint 1을 Auth-only로 유지하고, invitation을 Sprint 1.5로 분리한 방향은 문서 전반에 비교적 일관되게 반영됐다. `refresh_tokens`, `invitations`, invitation API, S14 구성원 추가 화면 정책도 주요 문서에 반영되어 5일차 최종본보다 정합성이 좋아졌다.

다만 팀원 메타검수안 기준으로 보면 Sprint 1.5 착수 전 반드시 합의해야 할 설계 공백이 남아 있다. 현재 상태는 “제출용 정합성 보정본”으로는 충분히 개선됐지만, “Sprint 1.5 바로 구현 가능” 상태로 보기는 어렵다.

## 2. 잘 반영된 항목

| 항목 | 검토 결과 |
|---|---|
| Sprint 1 Auth-only 정리 | 반영됨. 회사/프로젝트/이슈 등 도메인 기능을 Sprint 1에서 분리하고 Auth 중심으로 정리함 |
| `refresh_tokens` ERD 반영 | 반영됨. Entity 목록, Mermaid ERD, 상세 table에 추가됨 |
| invitation Sprint 1.5 분리 | 반영됨. API/ERD/화면/추적표/Backlog/Backend DoD에 Sprint 1.5 흐름 추가됨 |
| invitation 상태값 | `PENDING`, `ACCEPTED`, `DECLINED`, `CANCELED`, `EXPIRED` 기준 반영됨 |
| invitation 만료 정책 | 7일 만료와 lazy expiration 기준 반영됨 |
| 즉시 membership 생성 제거 | 반영됨. invitation accept 시점에 membership 생성하는 흐름으로 보정됨 |
| S14 구성원 추가 화면 | 즉시 등록 화면이 아니라 invitation 생성/상태 표시/accept 후 membership 생성 흐름으로 수정됨 |
| project invitation 대상 제한 | 해당 프로젝트가 속한 회사의 `ACTIVE` company member 조건이 명시됨 |
| login 응답 | Sprint 1 기준 `companies: []` 고정 정책 반영됨 |
| `/auth/me` 응답 | Sprint 1 minimal `data.user` 기준 유지됨 |

## 3. 남은 주요 이슈

| 심각도 | 이슈 | 설명 | 권장 조치 |
|---|---|---|---|
| 높음 | 회사 부트스트랩 부재 | Sprint 1.5에서 회사 invitation을 하려면 선행 company와 company admin이 필요하지만 seed/minimal creation 정책이 명확하지 않음 | demo/local seed 회사와 첫 관리자 계정을 둘지, 최소 회사 생성 API를 둘지 결정 |
| 높음 | project invitation의 Sprint 1.5 포함 여부 | project invitation은 `projects`, `project_members`, 회사 membership에 의존함. Sprint 1.5 범위가 커질 수 있음 | Sprint 1.5는 company invitation만 두고 project invitation은 Sprint 2로 분리 권장 |
| 높음 | `notification_events` 의존성 | invitation 내부 원장 기록이 Sprint 1.5에 포함되면 notification domain까지 선행 구현해야 함 | 1.5에서는 `GET /users/me/invitations` 조회로 닫고, `notification_events`는 optional 또는 Sprint 2로 이동 |
| 중간 | UTC/timezone 기준 부족 | `expiresAt` 예시는 `Z`가 있으나 DB type은 `DATETIME`이고 reset/refresh token에도 공통 시간 기준이 필요함 | 모든 token/invitation timestamp는 UTC 기준으로 명시 |
| 중간 | email lowercase 정규화 기준 없음 | signup, login, invitation lookup에서 email 비교 기준이 흔들릴 수 있음 | 저장·조회 모두 lowercase normalized email 기준 명시 |
| 중간 | OAuth account linking 보안 기준 미정 | 같은 이메일이면 자동 연결하는 흐름은 이메일 소유 확인 기준이 필요함 | provider verified email 또는 명시적 linking 정책 추가 |
| 중간 | password reset 후 refresh token revoke 없음 | 비밀번호 재설정 후 기존 refresh token 유지 여부가 보안상 중요함 | password reset 성공 시 기존 refresh token 전체 revoke 권장 |
| 중간 | `users.status != ACTIVE` 처리 기준 없음 | `SUSPENDED`, `DELETED` 사용자의 login/lookup/invitation accept 차단 기준 필요 | Auth와 invitation 공통으로 inactive user 차단 규칙 추가 |
| 낮음 | metadata 상태값 일부 잔존 | 상단 metadata는 `6일차 수정본`으로 통일됐지만, 하단 Phase B 메모에 `공식 결정 반영 완료`가 남은 문서가 있음 | 제출 전 하단 상태 메모까지 정리 |
| 낮음 | README 경로 불일치 | README 기준 자료 경로가 `/home/ubuntu/dev/...`로 되어 있어 현재 Mac workspace 경로와 다름 | 제출 패키지에서는 상대경로 또는 실제 경로로 정리 |

## 4. Sprint 1.5 착수 전 결정 필요 항목

### 4.1 회사 부트스트랩 방식

Sprint 1.5 invitation은 company membership을 전제로 한다. 따라서 최소한 다음 중 하나를 결정해야 한다.

| 선택지 | 설명 | 판단 |
|---|---|---|
| seed company + seed admin | demo/local 환경에 기본 회사와 관리자 계정을 미리 생성 | 가장 단순하고 Sprint 1.5 범위 통제에 유리 |
| minimal company creation API | 회사 생성 API를 Sprint 1.5에 포함 | 실제 서비스 흐름에는 자연스럽지만 Sprint 1.5 범위 증가 |
| 수동 DB insert | 개발 중 임시로 DB에 회사/관리자 삽입 | 문서화된 구현 흐름으로는 부적합 |

권장안은 `seed company + seed admin`이다.

### 4.2 Sprint 1.5 invitation 범위

현재 문서는 company invitation과 project invitation을 모두 Sprint 1.5에 포함하는 방향이다. 그러나 project invitation은 project domain에 강하게 의존한다.

권장 범위는 다음과 같다.

| Sprint | 권장 범위 |
|---|---|
| Sprint 1 | Auth-only, refresh token, password reset, OAuth |
| Sprint 1.5 | company invitation only |
| Sprint 2 | project invitation, project members, issue/document/chat domain 연동 |

### 4.3 notification_events 처리

Sprint 1.5에서 notification_events까지 포함하면 invitation 구현이 notification domain에 의존한다.

권장안:

- Sprint 1.5에서는 invitation 생성/조회/수락/거절/취소 API까지만 구현한다.
- 사용자의 받은 초대 목록은 `GET /users/me/invitations`로 제공한다.
- `notification_events` 기록은 optional 또는 Sprint 2로 분리한다.

### 4.4 Auth 보안 기준

Sprint 1 문서에도 다음 기준은 명시하는 편이 안전하다.

- email은 저장·조회 모두 lowercase normalized 기준으로 처리한다.
- `users.status != ACTIVE`인 사용자는 login, token refresh, invitation accept를 차단한다.
- password reset 성공 시 기존 refresh token을 모두 revoke한다.
- OAuth 자동 연결은 provider가 검증한 email일 때만 허용하거나, 명시적 account linking 절차를 둔다.
- token, invitation, password reset 관련 timestamp는 UTC 기준으로 통일한다.

## 5. 발표/제출용 메시지

발표에서는 다음처럼 정리하는 것이 안전하다.

```text
1차 검수에서 나온 문서 정합성 문제는 6일차 수정본에서 대부분 보정했다.
Sprint 1은 Auth-only로 유지하고, invitation은 Sprint 1.5로 분리했다.
ERD/API/화면/추적표에도 refresh token과 invitation 흐름을 반영했다.
다만 Sprint 1.5 구현 전에는 회사 부트스트랩, project invitation 범위, notification_events 의존성, Auth 보안 기준을 추가 합의해야 한다.
```

## 6. 최종 판정

| 구분 | 판정 |
|---|---|
| 1차 검수 이슈 반영도 | 높음 |
| Sprint 1 Auth-only 정합성 | 양호 |
| Sprint 1.5 invitation 문서 반영 | 양호 |
| Sprint 1.5 즉시 구현 가능성 | 보완 필요 |
| 발표/제출 가능 여부 | 가능. 단, 잔여 리스크를 같이 언급하는 조건 |

최종적으로, 이 수정본은 제출용 문서 묶음으로는 충분히 개선됐다. 다만 구현 착수 기준으로는 위의 Sprint 1.5 선행 결정 항목을 닫은 뒤 진행하는 것이 안전하다.

## 7. 병렬 검토 결과 보강

백그라운드 검토 결과와 대조했을 때, 기존 최종 판정은 유지된다. 다만 아래 2개 항목은 리뷰 문서에 더 명확히 남기는 것이 좋다.

| 심각도 | 추가 확인 항목 | 설명 | 권장 조치 |
|---|---|---|---|
| 높음 | OAuth account-linking 정책 | 문서에는 같은 email이면 같은 `users` 계정에 연결하는 방향이 있으나, provider email 소유 검증 또는 명시적 연결 단계가 부족하다. 계정 탈취 리스크가 될 수 있다. | provider verified email 조건 또는 별도 account-link flow를 명시 |
| 중간 | `/auth/me`와 S03 화면 context 범위 | API는 Sprint 1 minimal auth context인데, 화면 S03은 회사/프로젝트/unread count 등 후속 context까지 포함하는 것처럼 읽힐 수 있다. | S03 또는 API 설명에 “Sprint 1 이후 확장 context”임을 명확히 표기 |

메타검수안 기준으로는 다음처럼 판정한다.

| 메타검수 항목 | 수정본 반영 상태 | 판정 |
|---|---|---|
| registered-user-only invitation | 반영됨 | 해소 |
| 실제 email/token link/unregistered onboarding 제외 | 반영됨 | 해소 |
| 7일 만료 + lazy expiration | 반영됨 | 해소 |
| accept 시 membership 생성 | 반영됨 | 해소 |
| API error code 정리 | 대체로 반영됨 | 해소 |
| 회사 bootstrap 방식 | 문서상 신규 해법 없음 | 미해소 |
| project invitation Sprint 1.5 포함 여부 | 수정본은 여전히 Sprint 1.5에 포함 | 의사결정 필요 |
| `notification_events` Sprint 1.5 의존 | 수정본은 여전히 1.5 내부 원장으로 포함 | 의사결정 필요 |

따라서 수정본 내부 정합성은 좋아졌지만, 메타검수안이 요구한 범위 축소와 의존성 제거가 모두 채택된 것은 아니다. 이 차이는 단순 오탈자나 문서 누락이 아니라 팀 차원의 scope 결정 문제로 보는 것이 맞다.
