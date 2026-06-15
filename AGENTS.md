# AGENTS.md

## 프로젝트 작업 규칙

이 저장소는 사람과 AI가 같은 기준으로 작업하기 위해 공통 규칙을 사용한다.

코드, 문서, Git, PR 작업을 하기 전에 아래 문서를 읽고 따른다.

- [CONTRIBUTING.md](./CONTRIBUTING.md)
- [문서-작성-규칙.md](./docs/00-project/문서-작성-규칙.md)
- [docs/AGENTS.md](./docs/AGENTS.md)
- [docs/CLAUDE.md](./docs/CLAUDE.md)

## 역할 분리

- `AGENTS.md`: 저장소 공통 작업 규칙과 최소 금지사항을 둔다.
- `CONTRIBUTING.md`: Git, commit, PR 기준 문서다.
- `docs/00-project/문서-작성-규칙.md`: 문서 작성, 저장 위치, 파일명, 메타정보의 기준 문서다.
- `docs/AGENTS.md`: `docs/` 하위 문서 작업 시 적용되는 AI 작업 규칙이다.
- `docs/CLAUDE.md`: `docs/AGENTS.md`와 동일한 Claude Code용 문서 작업 규칙이다.
- `CLAUDE.md`: Claude Code 사용자를 위한 동일 규칙 진입점이다.

## Git Commit Message

commit message는 [CONTRIBUTING.md](./CONTRIBUTING.md#commit-message)의 규칙을 따른다.

기본 형식:

```text
type(scope): summary

1. 변경 내용
2. 변경 이유 또는 영향
```

예시:

```text
feat(auth): OAuth 로그인과 비밀번호 재설정 API 흐름 추가

1. Google OAuth 로그인과 비밀번호 재설정 token 발급 흐름을 추가한다.
2. Sprint 1 인증 범위에 포함된 소셜 로그인과 계정 복구 시나리오를 구현하기 위한 변경이다.
```

첫 줄에는 type, scope, summary를 적고, 그 아래에는 번호 목록으로 변경 내용과 이유를 작성한다. summary와 본문은 일부러 짧게 줄이지 않는다.

## 최소 작업 원칙

- secret, `.env`, token, password, 개인정보는 commit하지 않는다.
- 한 commit에는 한 가지 논리적 변경만 담는다.
- PR에 포함될 commit에는 `WIP`, `수정`, `작업중`처럼 의미가 모호한 message를 남기지 않는다.
- branch, PR Template, CI 기준은 [CONTRIBUTING.md](./CONTRIBUTING.md)에 추가된 규칙을 따른다.
- 문서를 작성하거나 수정할 때는 [문서-작성-규칙.md](./docs/00-project/문서-작성-규칙.md)를 따른다.
