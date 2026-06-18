# CONTRIBUTING

이 문서는 Threadia의 Git, commit, PR 기준 문서다.

문서 작성, 저장 위치, 파일명, 메타정보 규칙은 [문서-작성-규칙.md](./docs/00-project/문서-작성-규칙.md)를 따른다.

## Commit Message

Commit message는 아래 형식을 따른다.

```text
type(scope): summary

1. 변경 내용
2. 변경 이유 또는 영향
```

- `type`은 `feat`, `fix`, `docs`, `refactor`, `test`, `chore`, `build`, `ci`, `perf`, `revert` 중 하나를 사용한다.
- `scope`는 선택이며, 필요하면 `auth`, `api`, `ui`, `db`, `infra`, `docs`처럼 변경 영역을 간결하게 적는다.
- 첫 줄에는 `type(scope): summary`를 작성한다.
- 첫 줄 아래에는 빈 줄을 두고 번호 목록으로 변경 내용을 작성한다.
- `summary`와 번호 목록은 한국어를 허용한다.
- `summary`와 번호 목록은 일부러 짧게 줄이지 않는다.
- `summary`에는 변경 대상이 드러나야 한다.
- 번호 목록에는 변경 내용과 변경 이유 또는 영향을 작성한다.
- 한 commit에는 한 가지 논리적 변경만 담는다.
- 의미 없는 `WIP`, `수정`, `작업중` 메시지는 PR merge 전에 정리한다.
- 민감정보, `.env`, token, password는 commit하지 않는다.

예시:

```text
feat(auth): OAuth 로그인과 비밀번호 재설정 API 흐름 추가

1. Google OAuth 로그인과 비밀번호 재설정 token 발급 흐름을 추가한다.
2. Sprint 1 인증 범위에 포함된 소셜 로그인과 계정 복구 시나리오를 구현하기 위한 변경이다.
```

좋지 않은 예시:

```text
docs: 수정
fix: 오류 수정
fix: 작업중
chore: WIP
feat(auth): 로그인 API 추가
```

## Branch and PR

Threadia는 개발단계부터 `feature/*` 작업 branch에서 개발하고, `dev`와 `main`에는 Pull Request로만 반영한다.

### Branch 용도

| Branch | 용도 | Push 기준 |
|---|---|---|
| `main` | 제출, 발표, 안정본 기준 branch | 직접 push 금지. PR merge만 허용 |
| `dev` | Sprint 통합 개발 branch, 기본 branch | 직접 push 금지. PR merge만 허용 |
| `feature/*` | 기능 단위 작업 branch | 담당자가 자유롭게 push 가능 |
| `fix/*` | 버그 수정 작업 branch | 담당자가 자유롭게 push 가능 |
| `docs/*` | 문서 수정 작업 branch | 담당자가 자유롭게 push 가능 |
| `hotfix/*` | 긴급 수정 branch | 필요 시 생성하되 `dev` 또는 `main` 반영은 PR로 처리 |

### Branch naming

작업 branch는 아래 형식을 사용한다.

```text
type/short-topic
```

예시:

```text
feature/auth-login
feature/password-reset
fix/auth-token-refresh
docs/branch-policy
```

### PR 기준

- `feature/*`, `fix/*`, `docs/*`에서 `dev`로 PR을 생성한다.
- `main` 반영은 `dev`에서 `main`으로 PR을 생성한다.
- PR에는 작업 내용, 관련 문서/이슈, 검증 결과, 보안/승인 체크를 작성한다.
- PR 작성자와 reviewer는 분리한다.
- PR merge 전 approving review 1개 이상이 필요하다.
- PR review conversation은 모두 resolve한 뒤 merge한다.
- 승인 후 새 commit을 push하면 기존 approval은 다시 확인한다.
- 마지막 push를 한 사람이 아닌 다른 사람이 최종 승인한다.
- CI workflow가 생성되기 전에는 PR 본문의 수동 검증 기록을 기준으로 review한다.
- CI workflow가 생성된 뒤에는 required status check 통과를 merge 기준에 추가한다.

### Ruleset 기준

`main`과 `dev`에는 GitHub repository ruleset을 적용한다.

| Branch | Ruleset 기준 |
|---|---|
| `main` | PR required, approval 1명, conversation resolution required, stale approval dismiss, last push approval, deletion 금지, non-fast-forward 금지 |
| `dev` | PR required, approval 1명, conversation resolution required, stale approval dismiss, last push approval, deletion 금지, non-fast-forward 금지 |
| `feature/*` | 별도 ruleset 없음. 직접 push 가능 |

`required status checks`는 GitHub Actions workflow가 생성된 뒤 활성화한다.

### 승인 필요 작업

다음 변경은 PR에 승인 필요 항목으로 표시하고 팀 확인 후 merge한다.

- DB schema, migration 변경.
- 인증/인가, JWT, OAuth, password reset 정책 변경.
- GitHub Actions, ruleset, repository 권한 변경.
- AWS, 배포, domain, secret 변경.
- 대규모 구조 변경.
