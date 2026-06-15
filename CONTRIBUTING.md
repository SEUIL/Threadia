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

Branch naming, PR Template, CI 기준은 협업 규칙 확정 후 이 문서에 추가한다.
