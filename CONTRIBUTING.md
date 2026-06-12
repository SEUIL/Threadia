# CONTRIBUTING

## Commit Message

Commit message는 아래 형식을 따른다.

```text
type(scope): summary
```

- `type`은 `feat`, `fix`, `docs`, `refactor`, `test`, `chore`, `build`, `ci`, `perf`, `revert` 중 하나를 사용한다.
- `scope`는 선택이며, 필요하면 `auth`, `api`, `ui`, `db`, `infra`, `docs`처럼 짧게 적는다.
- `summary`는 한 줄로 작성하고, 한국어를 허용한다.
- 한 commit에는 한 가지 논리적 변경만 담는다.
- 의미 없는 `WIP`, `수정`, `작업중` 메시지는 PR merge 전에 정리한다.
- 민감정보, `.env`, token, password는 commit하지 않는다.

예시:

```text
feat(auth): 로그인 API 추가
fix(issue): 이슈 상태 변경 오류 수정
docs: README 실행 방법 보강
ci: GitHub Actions test workflow 추가
chore: Gradle wrapper 실행 권한 수정
```


## GitHub 표시 원칙

- GitHub collaborator, reviewer, assignee에는 실제 팀원 계정만 사용한다.
- AI 도구나 agent 이름을 collaborator로 추가하지 않는다.
- commit author, co-author, PR author에 AI 도구나 agent 이름을 남기지 않는다.
- AI를 사용했다면 GitHub 계정 표시가 아니라 PR 설명의 AI 사용 여부/범위로만 기록한다.

## Branch and PR

Branch naming, PR Template, CI 기준은 협업 규칙 확정 후 이 문서에 추가한다.
