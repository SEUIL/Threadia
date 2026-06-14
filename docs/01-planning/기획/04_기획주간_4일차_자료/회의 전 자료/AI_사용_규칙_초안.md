---
title: "AI 사용 규칙 초안"
type: pre-meeting-draft
status: draft
created: 2026-06-14
updated: 2026-06-14
tags:
  - project
  - team-project
  - acorn
  - planning
  - ai
related:
  - "[[04_기획주간_4일차_협업하네스_문서운영]]"
context: "4일차 회의 전 팀 AI 사용 규칙 검토 초안"
---

# AI 사용 규칙 초안

## 목적

팀원이 AI를 사용해도 되는 범위와 책임 기준을 회의 전에 정리한다. 이 문서는 검토용 초안이며, 최종 규칙은 팀 리뷰 후 `CONTRIBUTING.md`나 PR Template에 반영할지 결정한다.

## 초안 내용

| 규칙 | 초안 문장 |
|---|---|
| AI 사용 공개 | PR에서 AI를 사용했다면 사용 여부와 범위를 적는다. 예: 문서 초안, test 보조, 코드 초안 작성. |
| secret 입력 금지 | `.env`, API key, token, password, 개인정보, 비공개 계정 정보는 AI 도구에 입력하지 않는다. |
| 사람 책임 | AI가 만든 코드, 문서, 설정은 작성자가 직접 이해하고 검토한 뒤 제출한다. |
| 검증 책임 | AI가 만들었더라도 test, build, 화면 확인, API 확인 같은 검증 책임은 PR 작성자에게 있다. |
| GitHub metadata | AI 도구나 agent 이름을 GitHub collaborator, commit author, co-author, reviewer, assignee로 남기지 않는다. |
| 승인 필요 작업 | DB, 배포, 인증/인가, 보안, 대규모 구조 변경은 AI 제안만으로 진행하지 않고 팀 승인 기준을 따른다. |

## PR에 적을 수 있는 예시

```md
## AI 사용 여부

- 사용 여부: 예
- 사용 범위: PR 설명 초안 정리, test case 후보 정리
- 사람 검토: 작성자가 코드와 문서를 직접 확인함
```

## 팀이 리뷰하고 결정할 것

| 결정 항목 | 확인할 내용 | 회의 결과 |
|---|---|---|
| AI 사용 공개 범위 | 모든 사용을 적을지, 코드와 문서 변경에 한정할지 |  |
| PR Template 반영 | 체크박스로 둘지, 자유 서술로 둘지 |  |
| 금지 입력 범위 | secret 외 수업 자료, 팀원 개인정보, 외부 서비스 계정 정보 포함 여부 |  |
| GitHub metadata 문장 | collaborator, author, co-author, reviewer, assignee 전체를 명시할지 |  |
| 최종 반영 위치 | `CONTRIBUTING.md` / PR Template / 둘 다 |  |
