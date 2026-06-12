# AGENTS.md

## 프로젝트 작업 규칙

이 저장소는 사람과 AI가 같은 기준으로 작업하기 위해 공통 규칙을 사용한다.

코드, 문서, Git, PR 작업을 하기 전에 아래 문서를 읽고 따른다.

- [CONTRIBUTING.md](./CONTRIBUTING.md)

## Git Commit Message

commit message는 [CONTRIBUTING.md](./CONTRIBUTING.md#commit-message)의 규칙을 따른다.

형식:

```text
type(scope): summary
```

예시:

```text
feat(auth): 로그인 API 추가
fix(issue): 이슈 상태 변경 오류 수정
docs: README 실행 방법 보강
ci: GitHub Actions test workflow 추가
```

## 최소 작업 원칙

- secret, `.env`, token, password, 개인정보는 commit하지 않는다.
- 한 commit에는 한 가지 논리적 변경만 담는다.
- PR에 포함될 commit에는 `WIP`, `수정`, `작업중`처럼 의미가 모호한 message를 남기지 않는다.
- branch, PR Template, CI 기준은 확정 후 [CONTRIBUTING.md](./CONTRIBUTING.md)에 추가된 규칙을 따른다.
- AI 도구나 agent 이름을 GitHub collaborator, commit author/co-author, reviewer, assignee로 남기지 않는다. GitHub에는 실제 팀원 계정만 표시한다.
