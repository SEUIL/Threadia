# Docs

Threadia 프로젝트 문서 보관 위치다.

문서 작성, 저장 위치, 파일명, 메타정보, AI 작업 규칙의 canonical 기준은 [문서-작성-규칙.md](./00-project/문서-작성-규칙.md)를 따른다.

## 폴더 구조

| Folder | Purpose |
|---|---|
| `00-project/` | 프로젝트 공통 규칙과 운영 기준 |
| `00-project/sprint-운영/` | Sprint/주차 계획, TODO, 리스크, 회고 |
| `01-planning/` | 기획주간 raw archive, 회의 전 자료, 분업 산출물, 검토 기록 |
| `02-requirements/` | 사용자 시나리오, 기능 분해표, 요구사항, 추적표 |
| `03-design/` | 화면정의서, 화면 흐름, ERD |
| `04-api/` | API 명세서, error code, request/response 예시 |
| `05-architecture/` | Architecture, 인증/인가 구조, 인프라, 기술 연구 |
| `06-meetings/` | 실제 회의록과 회의 Action Item |
| `07-decisions/` | ADR, 확정 결정, 보류 기능, 변경 이력 |
| `08-members/` | 팀원별 개인 작업 기록 |
| `assets/` | 공용 이미지, 다이어그램, PDF, PPTX 등 첨부 자료 |

## 운영 기준

- `01-planning/`은 기획주간 원본 archive로 보존한다.
- 기획 종료 후 최종 산출물은 각 canonical 폴더로 복사·승격한다.
- 개발주간 이후의 참조 기준은 `02-requirements/`, `03-design/`, `04-api/`, `00-project/`, `07-decisions/` 등에 있는 canonical copy다.
- 회의록은 `06-meetings/`, Sprint 운영 문서는 `00-project/sprint-운영/`, 확정 결정은 `07-decisions/`로 분리한다.
