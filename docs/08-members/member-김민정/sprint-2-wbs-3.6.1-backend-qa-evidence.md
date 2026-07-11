> **작성자**: 김민정 (WBS 3.6.1 Owner)
> **작성일**: 2026-07-07
> **최종 수정**: 2026-07-11
> **버전**: v1.2
> **상태**: 작성 중
> **태그**: sprint-2, wbs-3.6.1, backend, qa, ci

# Sprint 2 WBS 3.6.1 Backend test/build/API QA Evidence 정리

> 기준: 구글 시트 WBS 탭 3.6.1 row, [`sprint-2-wbs-3.1.5-company-rbac-검증케이스.md`](./sprint-2-wbs-3.1.5-company-rbac-검증케이스.md), [`sprint-2-wbs-3.3.5-project-backend-검증케이스.md`](./sprint-2-wbs-3.3.5-project-backend-검증케이스.md)
> WBS 3.6.1(Backend test/build/API QA evidence 정리, Owner 김민정)의 착수 가능 조건은 "backend QA 케이스 선작성 가능; 최종 evidence는 backend 구현 결과 후"다. 선행 WBS는 3.1, 3.3 전체다. 완료 조건(시트 원문): "backend test/build와 API 정상/실패 케이스 결과를 reviewer가 확인 가능".
> 3.6.1은 새 검증 케이스를 만드는 문서가 아니라, 이미 만든 **3.1.5(Company/RBAC)**와 **3.3.5(Project Backend)** 결과를 취합하는 상위 roll-up이다. 개별 케이스는 두 문서에서 관리하고, 여기서는 전체 backend 빌드/테스트 상태와 두 문서의 종합 판정만 다룬다.

---

## 0. WBS 3.6.1 진행 현황 (본인 self-report)

| WBS | 진척률 | 비고 |
|---|---|---|
| 3.6.1 | 90% | 취합 완료(2026-07-11). §2.1 백엔드 빌드 성공, §2.2 종합 판정 = **통과**(Company/RBAC·Project 두 라인 모두 실패(버그) 0건). **롤업 대상 QA 문서 둘 다 dev merge 완료**: 3.1.5(PR #71), 3.3.5(PR [#74](https://github.com/SEUIL/Threadia/pull/74), 2026-07-11 황스일 merge — #20 재검증 반영). 3.3.5 Finding F-1은 PR [#76](https://github.com/SEUIL/Threadia/pull/76)(김민경 수정·여도현 review+merge)으로 해결·재검증(없는 companyId→404) 통과. 남은 것: 3.6.1 문서 자체 Reviewer(황스일) 확인+머지 |

---

## 1. 착수 조건 확인

| WBS | 상태(2026-07-11 기준) | 비고 |
|---|---|---|
| 3.1 (Company/RBAC Backend, 3.1.1~3.1.4) | **완료** (3.1.1~3.1.4 전부 dev merge) | 상세는 [`sprint-2-wbs-3.1.5-company-rbac-검증케이스.md`](./sprint-2-wbs-3.1.5-company-rbac-검증케이스.md) §1 참고. QA(3.1.5)는 PR #71로 dev 반영 완료. 실행 중 발견된 버그는 PR #66(Company)·#68(Department)로 수정·재검증 완료 |
| 3.3 (Project Backend, 3.3.1~3.3.4) | **완료** (3.3.1~3.3.4 전부 dev merge) | 상세는 [`sprint-2-wbs-3.3.5-project-backend-검증케이스.md`](./sprint-2-wbs-3.3.5-project-backend-검증케이스.md) §1 참고. QA(3.3.5)는 PR [#74](https://github.com/SEUIL/Threadia/pull/74) **dev merge 완료(2026-07-11, 황스일)** — #20 재검증(비활성 회사 구성원 승격 → 409) 포함. 코드 리뷰 Finding F-1은 이슈 [#73](https://github.com/SEUIL/Threadia/issues/73) → PR [#76](https://github.com/SEUIL/Threadia/pull/76)(김민경 수정·여도현 review+merge)으로 **해결·재검증 통과** |

3.1·3.3 구현은 모두 dev에 merge됐고, 롤업 대상 QA 문서(3.1.5 PR #71, 3.3.5 PR #74)도 dev에 merge되어 §2 종합 판정을 실제 결과로 확정했다. 3.3.5 Finding F-1(#73)은 PR #76으로 해결·재검증 통과, #20은 PR #74 리뷰 반영해 409로 재검증 → Project 라인도 "통과", **종합 판정 = 통과**. 최종 완료는 3.6.1 문서 자체의 Reviewer(황스일) 확인+머지만 남았다.

---

## 2. Backend 전체 QA 판정 기준

### 2.1 빌드/테스트 공통 확인

| 항목 | 확인 방법 | 확인 결과 | 비고 |
|---|---|---|---|
| Backend 컴파일/빌드 | `./gradlew build -x test` | **정상 통과** | 2026-07-11 dev 기준 `BUILD SUCCESSFUL`(19s), 컴파일 에러 0. 팀 방향대로 단위/통합 테스트는 `-x test`로 제외하고 컴파일·빌드 성공만 확인(API 검증은 Apidog=§2.2) |
| DB 스키마 정합성 | ERD ↔ 실제 schema 대조 | **확인(간접)** | 별도 스키마 검증 테스트 미실행. 대신 3.1.5 §2.4 #20(`DESCRIBE users`로 role 컬럼 부재 확인)·3.3.5 §3(`projects`/`project_members` 관계, soft-delete, company_id NOT NULL, PROJECT_ADMIN 최소 1명 쿼리 확인)에서 DB 직접 조회로 ERD 정합성 확인 |
| 마이그레이션/시드 | company/invitation 시드 데이터 정상 동작 | **확인(간접)** | 3.1.5 검증 중 seed 회사(companyId=6)·구성원·invitation 데이터로 정상 동작 확인(§2.3-A 19-A/19-B 등 membership 상태 전이 포함) |

> **팀 방향(2026-07-07 확인, canonical 문서 미반영)**: Backend 단위/통합 테스트는 `./gradlew test`가 아니라 **Apidog collection 실행**으로 진행한다. 위 표의 "빌드"는 컴파일 성공 여부만 확인하고, API 정상/실패 케이스 검증은 §2.2의 Apidog 결과로 판정한다.

### 2.2 API 정상/실패 케이스 종합 판정

| 라인 | 문서 | 케이스 수 | 종합 결과 | 비고 |
|---|---|---|---|---|
| Company/RBAC (3.1.5) | [`sprint-2-wbs-3.1.5-company-rbac-검증케이스.md`](./sprint-2-wbs-3.1.5-company-rbac-검증케이스.md) | 23개(19-A, 19-B 포함) | **통과** | 3.1.5 범위 케이스 전부 정상 통과(실패 0건). 실행 중 발견된 #4/#7 버그는 PR #66으로 수정·Apidog 재검증 완료, 동일 패턴 Department 건은 #67/PR #68로 해결. #19(프로젝트 관리자 지정)는 3.1.5 범위 밖 3.3.x라 확인 불가·3.3.5로 이관 |
| Project Backend (3.3.5) | [`sprint-2-wbs-3.3.5-project-backend-검증케이스.md`](./sprint-2-wbs-3.3.5-project-backend-검증케이스.md) | 20개 | **통과** | 20개 전부 정상 통과. #20(회사 구성원만 프로젝트 멤버)은 PR #74 리뷰(황스일) 반영해 "비활성 회사 구성원 승격 시도 → 409 CONFLICT"로 재정의·재검증. 코드 리뷰 Finding F-1(list/create 존재확인·권한확인 순서)은 이슈 [#73](https://github.com/SEUIL/Threadia/issues/73) → PR [#76](https://github.com/SEUIL/Threadia/pull/76)(김민경 수정·여도현 review+merge)으로 해결, 재검증(없는 companyId→404) 통과(2026-07-11) |

`종합 결과`는 `통과 / 부분 통과(실패 케이스 있음) / 확인 불가` 중 하나로 표기한다. `부분 통과`면 실패 케이스 번호와 관련 PR을 비고에 남긴다.

---

## 3. Apidog Collection 구성 (팀 방향 반영)

- Company/RBAC(3.1.5)와 Project Backend(3.3.5)의 request를 하나의 Apidog project 안에 **폴더 단위로 분리**해서 구성한다 (예: `Company-RBAC`, `Project`).
- 각 request는 3.1.5/3.3.5 문서의 `#` 번호를 request 이름에 포함해 문서-request 매핑을 유지한다 (예: `#3 GET company detail`).
- 환경 변수(baseUrl, 테스트 계정 토큰 등)는 Apidog environment 기능으로 관리하고, secret/DB password/token 원문은 문서에 기록하지 않는다.
- 실행 결과(pass/fail)는 Apidog 자체 기록에 의존하지 않고, 이 문서 §2.2와 3.1.5/3.3.5 문서의 `확인 결과` 컬럼에 별도로 옮겨 적어 문서 기준 추적성을 유지한다.

---

## 4. 검증 방법

1. 3.1, 3.3이 완료된 시점에 §1을 갱신한다.
2. 3.1.5, 3.3.5 문서의 §2 케이스를 Apidog collection으로 실행하고 각 문서의 `확인 결과`를 채운다.
3. §2.1 빌드/DB 확인을 실행한다.
4. §2.2 종합 판정표를 채운다.
5. 결과를 Reviewer(황스일)에게 재확인 요청한다.
6. 실패 케이스가 있으면 3.6 전체 완료 처리를 보류하고 해당 라인 owner에게 후속 조치를 요청한다.

---

## 5. 남은 작업

- 이 3.6.1 문서를 Reviewer(황스일)가 확인+머지하면 완료 (종합 판정 = 통과, 하위 QA 문서 3.1.5·3.3.5는 dev merge 완료)
