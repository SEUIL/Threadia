# PR Template v1.0

> **작성자**: Threadia 팀  
> **작성일**: 2026-06-15  
> **최종 수정**: 2026-06-15  
> **버전**: v1.0  
> **상태**: 확정  
> **기준 문서**: PR_Template_체크항목_초안, 4일차 회의 결정, CONTRIBUTING.md

---

## 1. 결정 사항

| 항목 | 결정 |
|---|---|
| PR Template 구성 | 작업 내용, 변경 유형, 검증, 보안/승인 체크 포함 |
| GitHub metadata | collaborator, author, co-author, reviewer, assignee에는 실제 팀원만 표시 |
| 검증 명령 | Backend는 `./gradlew test` 우선, 안정화 후 `./gradlew build` 확대 |
| 승인 필요 작업 | DB, 배포, 인증/인가, 보안 변경은 팀 승인 필요 |
| 구조 변경 | 선조치 후보고 가능하되 test/build 또는 직접 검증 필수 |

---

## 2. PR Template 본문

```md
## 작업 내용

- 

## 변경 유형

- [ ] 기능 추가
- [ ] 버그 수정
- [ ] 문서 수정
- [ ] 설정 변경
- [ ] 리팩터링
- [ ] 테스트 추가/수정

## 관련 문서/이슈

- 

## 검증

- [ ] 관련 기능을 직접 실행했다.
- [ ] Backend 변경이면 `./gradlew test` 또는 `./gradlew build`를 실행했다.
- [ ] Frontend 변경이면 브라우저에서 화면 또는 route를 확인했다.
- [ ] API 변경이면 요청/응답 예시를 확인했다.
- [ ] DB 변경이면 ERD/API 문서와 migration 영향을 확인했다.
- [ ] 실패한 test/build가 있으면 아래에 이유와 후속 처리를 적었다.

## 보안/승인 체크

- [ ] `.env`, API key, token, password, 개인정보가 포함되지 않았다.
- [ ] DB 변경이면 팀 승인 기준을 확인했다.
- [ ] 배포/인프라 변경이면 팀 승인 기준을 확인했다.
- [ ] 인증/인가/보안 변경이면 팀 승인 기준을 확인했다.
- [ ] 대규모 구조 변경이면 변경 이유와 검증 결과를 설명했다.

## 추가 메모

- 
```

---

## 3. 적용 위치

실제 repository 파일 생성 시 위치는 다음과 같다.

```text
.github/PULL_REQUEST_TEMPLATE.md
```

4일차 기준으로는 최종 문서만 확정하고, 실제 `.github/` 생성은 CI 도입 시점 또는 첫 PR 전 작업으로 처리한다.
