# README

## 폴더 구조

| 구분 | 설명 | 링크 |
|---|---|---|
| 핵심 기획/설계 문서 | 사용자 흐름, 기능 범위, 화면, API, ERD, 추적성, 보류 범위를 정리한 최종 산출물 | 아래 `핵심 기획/설계 문서` 참고 |
| Sprint 관리 | Sprint 1 실행 계획, Backlog, Backend/Frontend DoD, 담당 그룹을 정리한 하위 폴더 | [Sprint-관리](<./Sprint-관리/>) |
| Harness | AI 사용 규칙, CI 기준, PR Template 등 협업 하네스 문서 하위 폴더 | [Harness](<./Harness />) |
| 변경 이력 | 6일차 최종 산출물의 버전 관리, 검수 반영, 파일명 정리, 이관 계획 기록 | [기획문서-변경이력.md](<./기획문서-변경이력.md>) |
| 발표 자료 | 6일차 최종 발표용 PPTX 파일 | [threadia.pptx](<./threadia.pptx>) |

## 핵심 기획/설계 문서

| 문서 | 설명 |
|---|---|
| [사용자 시나리오](<./사용자-시나리오.md>) | Threadia의 주요 사용자 역할과 시나리오 흐름, Sprint 1 Auth-only 기준, Sprint 1.5 invitation 분리를 정리한 문서다. |
| [기능 분해표](<./기능-분해표.md>) | MVP 기능, 후속 Sprint 구현 범위, 보류/선택 기능을 기능 단위로 분해한 기준 문서다. |
| [화면정의서](<./화면정의서.md>) | 화면별 목적, 사용자 액션, 화면 흐름, invitation 반영 기준을 정리한 화면 설계 문서다. |
| [API 명세서](<./API-명세서.md>) | Threadia API 공통 규칙, Auth API, 도메인 API, invitation API 계약과 error code를 정리한 API 기준 문서다. |
| [ERD](<./ERD.md>) | users, refresh_tokens, invitations 등 주요 entity와 관계, table detail을 정리한 DB 설계 문서다. |
| [기능-시나리오-API-ERD 추적표](<./기능-시나리오-API-ERD-추적표.md>) | 기능, 사용자 시나리오, API, ERD, 화면 간 연결 상태를 검증하기 위한 추적표다. |
| [보류 기능 최종표](<./보류-기능-최종표.md>) | Sprint 1과 Sprint 1.5에서 제외하거나 후속 검토로 분리한 기능과 제외 사유를 정리한 문서다. |

## Sprint 관리 문서

| 문서 | 설명 |
|---|---|
| [Sprint 1 상세계획](<./Sprint-관리/Sprint-1-상세계획.md>) | Sprint 1 Auth-only 구현 범위, Sprint 1.5 invitation 계획, 검증 기준을 상세화한 실행 계획 문서다. |
| [Sprint 1 Backlog](<./Sprint-관리/Sprint-1-Backlog.md>) | Sprint 1 작업 항목, 우선순위, 완료 기준, 후속 Sprint 분리 기준을 정리한 Backlog 문서다. |
| [Sprint 1 역할분담](<./Sprint-관리/Sprint-1-역할분담.md>) | Sprint 1 작업별 Owner, Reviewer, 지원 역할과 문서 정합성 확인 역할을 정리한 문서다. |
| [Sprint 1 Backend DoD](<./Sprint-관리/Sprint-1-백엔드-DoD.md>) | Backend 구현 범위, 기술 스택, DB/API 의존성, 완료 조건과 검증 기준을 정리한 문서다. |
| [Sprint 1 Frontend DoD](<./Sprint-관리/Sprint-1-프론트엔드-DoD.md>) | Frontend 구현 범위, 화면/API 연동 의존성, 상태 관리 기준, 완료 조건을 정리한 문서다. |

## 검증/품질 기준 문서

| 문서 | 설명 |
|---|---|
| [Definition of Done](<./Definition-of-Done.md>) | Backend, Frontend, DB, 문서, PR 등 작업 유형별 완료 조건과 Sprint 1.5 기준을 정리한 DoD 문서다. |
| [검증 방법](<./검증-방법.md>) | test, build, API, 화면, DB, 문서 검증 방법과 확인 기준을 정리한 품질 검증 문서다. |

## Harness 문서

| 문서 | 설명 |
|---|---|
| [AI 사용 규칙](<./Harness /AI-사용-규칙.md>) | AI 사용 책임, PR 기재 기준, 검증 책임, 문서/코드 작성 시 주의사항을 정리한 규칙 문서다. |
| [CI 기준](<./Harness /CI-기준.md>) | Local 검증과 CI 검증의 차이, PR 전 필수 확인 항목, Auth/invitation 변경 검증 기준을 정리한 문서다. |
| [PR Template](<./Harness /PR-Template.md>) | PR 작성 시 포함해야 할 변경 유형, 검증 체크리스트, Auth/Invitation 관련 확인 항목을 정리한 Template 문서다. |

## 관리/발표 자료

| 문서 | 설명 |
|---|---|
| [기획문서 변경이력](<./기획문서-변경이력.md>) | 6일차 최종 산출물의 검수 반영 내역, 파일명 정리 내역, caveat, 향후 `docs/07-decisions` 이관 계획을 기록한 문서다. |
| [threadia.pptx](<./threadia.pptx>) | 6일차 최종 산출물을 바탕으로 공유하거나 발표할 때 사용하는 PPTX 자료다. |
