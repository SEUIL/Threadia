# Sprint 1 Day 3 QA - 버그 수정 / 버퍼 검증 결과

## 작업 식별

- WBS: Sprint 1 / Day 3 QA
- 분류: 버퍼
- 작업명: 버그 수정 / 버퍼
- 요구사항 ID: AUTH-01~AUTH-04
- 관련 영역: Auth Backend / Frontend
- 산출물: 통합 테스트 발견 이슈 수정 결과
- 담당자: 김민경
- Reviewer: 여도현
- 진행 상태: 진행중 50%
- 우선순위: P1
- 기준 문서: sprint-1-상세계획 105~113, sprint-1-역할분담 66~72
- 비고: Sprint 1 압축 일정. 치명 이슈만 당일 수정하고 나머지는 backlog/이슈로 이월

## 검증 기준

이번 QA/버퍼 작업은 신규 기능 구현이 아니라, Sprint 1 Auth Backend/Frontend 통합 상태에서 발견되는 이슈를 확인하고 수정 필요 여부를 판단하는 작업이다.

확인 기준은 다음과 같다.

- Auth Backend 전체 테스트가 통과하는지 확인한다.
- Auth Frontend lint/build 상태를 확인한다.
- 통합 테스트 중 발견된 이슈가 있으면 치명도와 수정 필요 여부를 분리한다.
- 당일 수정이 필요한 치명 이슈와 후속 이월 가능한 이슈를 구분한다.
- 실제 코드 수정 여부와 미수정 사유를 기록한다.


## 포함된 주요 범위

현재 `dev`에는 Sprint 1 Auth 관련 주요 PR이 포함된 상태로 확인했다.

- 이메일 signup/login/refresh/logout
- Google OAuth login
- `/api/v1/auth/me`
- `/api/v1/users/lookup`
- password reset backend
- Auth 관련 공통 응답/예외/JWT/CORS/cookie 처리
- Frontend AuthContext 및 로그인/회원가입/비밀번호 찾기/재설정 화면
- Frontend app shell / hub layout polish

## Backend 검증 결과

실행 명령:

```powershell
cd C:\Users\user\Desktop\Threadia\backend
.\gradlew.bat test
```

결과:

- 전체 backend test 통과
- 실패 테스트 없음
- AuthService refresh token 만료 테스트는 최신 dev에서 통과 상태
- Password reset 관련 controller/service test도 전체 테스트 범위에 포함되어 통과

판단:

- Backend 기준으로 Day 3 QA에서 즉시 수정해야 하는 치명 이슈는 발견되지 않았다.
- Backend 후속 수정 없이 공유 가능하다.

## Frontend 검증 결과

실행 명령:

```powershell
cd C:\Users\user\Desktop\Threadia\frontend
npm.cmd run lint
```

결과:

- lint error 없음
- warning 2건 발생

발생 warning:

- `frontend/app/(app)/[companyId]/members/page.tsx`
  - `departmentName` 변수가 선언되었지만 사용되지 않음
- `frontend/app/(app)/[companyId]/notifications/page.tsx`
  - `companyId` 변수가 선언되었지만 사용되지 않음

판단:

- lint warning은 존재하지만 error는 아니므로 즉시 실행 차단 이슈는 아니다.
- 다만 코드 품질 정리 대상으로 후속 수정 가능하다.

실행 명령:

```powershell
cd C:\Users\user\Desktop\Threadia\frontend
npm.cmd run build
```

결과:

- Next.js compile 단계는 성공
- TypeScript type check 단계에서 실패

실패 위치:

```text
frontend/app/(app)/[companyId]/[projectId]/backlog/page.tsx:50:25
Type error: This comparison appears to be unintentional because the types '"C"' and '"P"' have no overlap.
```

관련 코드 흐름:

```tsx
const MOCK_ROLE: Role = "C";
const canManageSprint = MOCK_ROLE === "P" || MOCK_ROLE === "C";
```

판단:

- Frontend production build를 차단하는 이슈가 확인되었다.
- 원인은 mock role 상수가 `"C"` 리터럴 타입으로 좁혀진 상태에서 `"P"`와 비교되어 TypeScript가 의도치 않은 비교로 판단하는 것이다.
- 같은 패턴이 여러 화면에 존재할 가능성이 있으므로 한 파일만 임시 수정하기보다 Frontend 담당자가 동일 패턴을 함께 정리하는 것이 안전하다.

## 발견 이슈 요약

| 구분 | 위치 | 현상 | 영향 | 판단 |
| --- | --- | --- | --- | --- |
| Backend | 전체 test | 실패 없음 | 없음 | 수정 불필요 |
| Frontend lint | members page | unused variable warning | 빌드 차단 아님 | 후속 정리 가능 |
| Frontend lint | notifications page | unused variable warning | 빌드 차단 아님 | 후속 정리 가능 |
| Frontend build | backlog page | TypeScript type check 실패 | production build 차단 | 수정 필요 |



## 후속 처리 제안

Frontend 담당자 또는 해당 PR 작성자가 아래 항목을 확인하는 것을 권장한다.

- `MOCK_ROLE: Role = "C"` 패턴을 사용하는 파일의 타입 좁혀짐 문제 정리
- `departmentName` unused warning 제거 또는 실제 사용
- `notifications/page.tsx`의 unused `companyId` 제거 또는 실제 경로 생성에 사용
- 수정 후 `npm.cmd run lint` 재실행
- 수정 후 `npm.cmd run build` 재실행

## 최종 판단

Sprint 1 Day 3 QA / 버그 수정·버퍼 기준으로 다음과 같이 기록한다.

- Backend Auth 통합 테스트는 통과했다.
- Backend에서 즉시 수정해야 할 이슈는 발견되지 않았다.
- Frontend lint는 warning 2건이 있으나 error는 없다.
- Frontend production build는 TypeScript type check에서 실패했다.
- build 실패는 Day 3 QA에서 확인된 이슈로 기록하고, Frontend 담당 범위에서 수정 후 재검증이 필요하다.
- 현재 시점에는 코드 수정 없이 검증 결과만 문서화했다.
