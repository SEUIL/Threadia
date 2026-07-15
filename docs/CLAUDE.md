# CLAUDE.md

## Docs 작업 규칙

`docs/` 아래에서 문서를 작성하거나 수정할 때는 [문서-작성-규칙.md](./00-project/문서-작성-규칙.md)를 따른다.

`문서-작성-규칙.md`가 `docs/` 구조와 문서 작성 규칙의 canonical 기준이다. 이 파일은 Claude Code 작업자가 빠르게 확인하기 위한 요약이다.

## 핵심 원칙

- 대상 파일을 먼저 읽고 수정한다.
- 기존 문서는 필요한 최소 범위만 수정한다.
- `01-planning/`은 기획주간 raw archive로 보존한다.
- 최종 산출물 승격은 원본 이동보다 canonical 폴더로 복사하는 방식을 우선한다.
- 상태가 `확정`인 문서는 사용자 확인 없이 수정하지 않는다.
- 새 문서 생성 전 같은 이름의 파일이 이미 있는지 확인한다.
- 문서 변경 후 가능한 diagnostics 또는 Markdown 검사를 실행한다.

## 폴더 사용 요약

| Folder | Purpose |
|---|---|
| `00-project/` | 프로젝트 공통 규칙과 운영 기준 |
| `00-project/sprint-운영/` | Sprint/주차 계획, TODO, 리스크, 회고 |
| `01-planning/` | 기획주간 raw archive |
| `02-requirements/` | 사용자 시나리오, 기능 분해표, 요구사항, 추적표 |
| `03-design/` | 화면정의서, 화면 흐름, ERD |
| `04-api/` | API 명세서와 API 계약 |
| `05-architecture/` | Architecture와 기술 연구 |
| `06-meetings/` | 실제 회의록과 Action Item |
| `07-decisions/` | ADR, 확정 결정, 보류 기능, 변경 이력 |
| `08-members/` | 팀원별 개인 작업 기록 |
| `assets/` | 이미지, 다이어그램, PDF, PPTX 등 첨부 자료 |

## 금지사항

- `01-planning/` 원본 archive를 명시 요청 없이 수정하지 않는다.
- 회의록이 아닌 Sprint TODO를 `06-meetings/`에 넣지 않는다.
- 확정 결정이 아닌 논의 메모를 `07-decisions/`에 넣지 않는다.
- `08-members/member-XX/` 문서는 해당 멤버 본인의 요청 없이 수정하지 않는다.
- 문서 내용 없이 폴더만 생성하지 않는다. 단, `.gitkeep`은 예외다.
- 한 문서에 여러 주제를 섞지 않는다.
