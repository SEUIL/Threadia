---
title: "PR Template 체크항목 초안"
type: pre-meeting-draft
status: draft
created: 2026-06-14
updated: 2026-06-14
tags:
  - project
  - team-project
  - acorn
  - planning
  - pr
related:
  - "[[04_기획주간_4일차_협업하네스_문서운영]]"
context: "4일차 회의 전 PR Template 체크 항목 검토 초안"
---

# PR Template 체크항목 초안

## 목적

PR을 올릴 때 팀원이 빠뜨리기 쉬운 작업 내용, 검증, AI 사용, 보안/승인 항목을 미리 정리한다. 이 문서는 검토용 초안이며, `.github/PULL_REQUEST_TEMPLATE.md`를 생성한 것이 아니다.

## 초안 내용

```md
## 작업 내용

-

## 변경 유형

- [ ] 기능 추가
- [ ] 버그 수정
- [ ] 문서 수정
- [ ] 설정 변경
- [ ] 리팩터링

## 검증

- [ ] 관련 기능을 직접 실행했다.
- [ ] Backend 변경이면 `./gradlew test` 또는 `./gradlew build`를 실행했다.
- [ ] Frontend 변경이면 브라우저에서 화면 또는 route를 확인했다.
- [ ] 실패한 test/build가 있으면 아래에 이유와 후속 처리를 적었다.

## AI 사용 여부

- [ ] 이번 PR에서 AI를 사용했다.
- [ ] AI 사용 범위를 아래에 적었다.
- [ ] AI가 만든 코드나 문서를 사람이 직접 이해하고 검토했다.

## 보안/승인 체크

- [ ] `.env`, API key, token, password, 개인정보가 포함되지 않았다.
- [ ] DB, 배포, 인증/인가, 보안, 대규모 구조 변경이면 승인자 검토를 요청했다.
- [ ] GitHub collaborator, author, co-author, reviewer, assignee에 AI 이름을 남기지 않았다.
```

## 팀이 리뷰하고 결정할 것

| 결정 항목 | 확인할 내용 | 회의 결과 |
|---|---|---|
| 필수 체크 항목 | 너무 많거나 빠진 항목이 있는지 |  |
| 검증 명령 | 초기 기준을 `./gradlew test`로 둘지, `./gradlew build`까지 요구할지 |  |
| AI 사용 표시 | PR에 어떤 수준으로 적을지 |  |
| 승인 필요 작업 | DB, 배포, 인증/인가, 보안, 대규모 구조 변경 외 추가 항목이 있는지 |  |
| 실제 파일 생성 시점 | 4일차 후 / 5일차 후 / 첫 PR 전 |  |
