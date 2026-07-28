> **작성자**: Threadia 팀
> **작성일**: 2026-07-22
> **최종 수정**: 2026-07-22
> **버전**: v1.0
> **상태**: 검토 중
> **태그**: release, verification, compose, dual-image, evidence

# Public dual-image release 검증기록

## 1. 기록 목적

이 문서는 2026-07-22에 완료된 public dual-image Compose 릴리스의 운영 증빙만 기록한다. 이 릴리스는 dual-image host가 이미 public route에 연결된 상태에서 수행한 in-place presentation release이며, 신규 target 생성이나 edge route 전환은 포함하지 않았다.

## 2. 확인된 릴리스 증빙

main SHA `316254949b350e267b7b646072b07f50aaada03f`를 기준으로 CI readiness run `29890297199`와 release workflow `29892340670`가 모두 통과했다.

일회성 additive DB migration postflight에서 attachments table은 1개, reference column은 2개, index는 7개, foreign key는 4개, check는 2개로 확인됐다. active transaction은 0개였고 target metadata lock도 0개였다.

host의 frontend와 backend는 모두 healthy 상태였다. public root는 `/login`으로 redirect됐고, login은 HTTP 200으로 확인됐으며 API health는 `UP` 상태였다.

## 3. 열려 있는 attachment transfer smoke gate

test credential이 없어 authenticated upload/presign smoke를 수행하지 못했다. 따라서 attachment transfer smoke gate는 열려 있으며, 이 릴리스가 attachment rollout 전체를 통과했다는 결론을 내리지 않는다.
