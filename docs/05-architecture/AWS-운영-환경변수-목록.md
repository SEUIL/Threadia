> **작성자**: Threadia 팀
> **작성일**: 2026-07-11
> **최종 수정**: 2026-07-11
> **버전**: v0.1 운영 환경 변수 inventory 초안
> **상태**: 작성 중
> **태그**: aws, env, secret, operations, deployment

# AWS 운영 환경변수 목록

## 1. 문서 목적

이 문서는 Threadia AWS 배포 준비에 필요한 환경 변수와 secret 후보를 inventory로 정리한다. 실제 값은 기록하지 않으며, `.env.example`, GitHub Secrets, AWS console, PR 본문, 검증 evidence 어디에도 secret 값을 남기지 않는다.

## 2. 관리 원칙

| 원칙 | 설명 |
|---|---|
| placeholder only | 문서는 `<...>`, example 파일은 shell-safe `REPLACE_ME` 또는 local default만 둔다 |
| value 비출력 | script와 CI log는 변수명과 존재 여부만 출력한다 |
| production 분리 | production 값은 GitHub Environment secrets 또는 AWS secret store 후보로 분리한다 |
| local 분리 | local `.env`는 commit하지 않는다 |
| spelling 확인 | `theadia.com` 후보와 기존 `threadia.com` 예시의 최종 spelling을 확정 전까지 혼용하지 않는다 |

## 3. Backend runtime variables

| 변수 | 예시 placeholder | 분류 | 설명 |
|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `<prod>` | config | 운영 profile 활성화 기준이다 |
| `THREADIA_DB_URL` | `<jdbc-url>` | secret-adjacent | RDS MySQL JDBC URL이다 |
| `THREADIA_DB_USERNAME` | `<db-username>` | secret-adjacent | RDS 접속 사용자명이다 |
| `THREADIA_DB_PASSWORD` | `<db-password>` | secret | RDS 접속 password다 |
| `THREADIA_DB_NAME` | `<db-name>` | local config | local compose와 검증 DB 이름 구분용이다 |
| `THREADIA_DB_ROOT_PASSWORD` | `<local-root-password>` | local secret | local compose 전용 root password다. 운영 app에는 주입하지 않는다 |
| `THREADIA_DB_SCHEMA_VALIDATION` | `<true-or-false>` | local config | schema validation test gate다 |
| `THREADIA_JWT_SECRET` | `<jwt-secret>` | secret | JWT signing secret이다 |
| `THREADIA_COOKIE_SECURE` | `<true-or-false>` | config | 운영 HTTPS에서는 `true`가 필수다 |
| `THREADIA_REFRESH_TOKEN_EXPIRATION_DAYS` | `<days>` | config | refresh token 만료 기간이다 |
| `THREADIA_CORS_ALLOWED_ORIGIN` | `<https://app.theadia.com>` | config | credential 포함 요청을 허용할 frontend origin이다 |
| `THREADIA_OAUTH_SUCCESS_REDIRECT_URL` | `<https://app.theadia.com/hub>` | config | OAuth 성공 후 frontend redirect URL이다 |
| `THREADIA_OAUTH_FAILURE_REDIRECT_URL` | `<https://app.theadia.com/login?oauth=failed>` | config | OAuth 실패 후 frontend redirect URL이다 |
| `THREADIA_PASSWORD_RESET_URL_PREFIX` | `<https://app.theadia.com/reset-password?token=>` | config | password reset link 생성 기준 URL prefix다 |
| `THREADIA_PASSWORD_RESET_EXPIRATION_MINUTES` | `<minutes>` | config | password reset token 만료 시간이다 |

## 4. OAuth variables

| 변수 | 예시 placeholder | 분류 | 설명 |
|---|---|---|---|
| `GOOGLE_CLIENT_ID` | `<google-client-id>` | secret-adjacent | Google OAuth client ID다 |
| `GOOGLE_CLIENT_SECRET` | `<google-client-secret>` | secret | Google OAuth client secret이다 |
| `GOOGLE_REDIRECT_URI` | `<https://api.theadia.com/api/v1/auth/oauth2/callback/google>` | config | Google OAuth redirect URI 후보 값이다 |

OAuth console에 redirect URI를 등록하기 전 domain spelling을 확정해야 한다. 기존 문서의 `threadia.com` 예시와 사용자 제시 `theadia.com` 후보가 다르다.

## 5. Mail variables

| 변수 | 예시 placeholder | 분류 | 설명 |
|---|---|---|---|
| `MAIL_HOST` | `<smtp-host>` | config | SMTP host다 |
| `MAIL_PORT` | `<smtp-port>` | config | SMTP port다 |
| `MAIL_USERNAME` | `<smtp-username>` | secret-adjacent | SMTP username이다 |
| `MAIL_PASSWORD` | `<smtp-password>` | secret | SMTP password다 |
| `NOTIFICATION_FROM_EMAIL` | `<no-reply@theadia.com>` | config | 발신자 주소 후보다 |

## 6. Frontend variables

| 변수 | 예시 placeholder | 분류 | 설명 |
|---|---|---|---|
| `NEXT_PUBLIC_API_URL` | `<https://api.theadia.com>` | public config | browser bundle에 포함되는 backend API base URL이다 |

`NEXT_PUBLIC_` 변수는 client에 노출되므로 secret을 넣지 않는다.

## 7. Domain, Route 53, ACM placeholders

| 변수 | 예시 placeholder | 분류 | 설명 |
|---|---|---|---|
| `THREADIA_DOMAIN_NAME` | `<theadia.com>` | config | 사용자 제시 domain candidate다 |
| `THREADIA_APP_DOMAIN` | `<app.theadia.com>` | config | frontend custom domain 후보다 |
| `THREADIA_API_DOMAIN` | `<api.theadia.com>` | config | backend API custom domain 후보다 |
| `THREADIA_DOCS_DOMAIN` | `<docs.theadia.com>` | config | docs-site custom domain 후보다 |
| `THREADIA_ROUTE53_HOSTED_ZONE_ID` | `<hosted-zone-id>` | secret-adjacent | hosted zone ID다 |
| `THREADIA_ACM_CERTIFICATE_ARN` | `<certificate-arn>` | secret-adjacent | HTTPS certificate ARN이다 |

CloudFront를 사용하면 ACM certificate는 `us-east-1`에 있어야 한다. ALB나 regional service는 region별 ACM 조건을 별도로 확인한다.

## 8. GitHub Actions AWS OIDC placeholders

| 변수 | 예시 placeholder | 분류 | 설명 |
|---|---|---|---|
| `AWS_REGION` | `<aws-region>` | config | future deploy job에서 사용할 AWS region이다 |
| `AWS_ROLE_TO_ASSUME` | `<iam-role-arn>` | secret-adjacent | GitHub Actions OIDC가 assume할 IAM role ARN이다 |

이번 repository workflow는 CI만 수행하므로 위 변수는 아직 사용하지 않는다. future deploy job에는 `permissions: id-token: write`, `contents: read`가 필요하다.

## 9. EC2 deploy script placeholders

| 변수 | 예시 placeholder | 분류 | 설명 |
|---|---|---|---|
| `THREADIA_DEPLOY_TARGET_HOST` | `<ec2-hostname-or-ip>` | secret-adjacent | SSH target host다 |
| `THREADIA_DEPLOY_TARGET_USER` | `<ec2-user>` | config | SSH user다 |
| `THREADIA_DEPLOY_SSH_KEY_PATH` | `<path-to-private-key>` | local secret path | private key 경로다. key content는 출력하지 않는다 |
| `THREADIA_DEPLOY_REMOTE_DIR` | `<remote-deploy-dir>` | config | artifact를 둘 remote directory다 |
| `THREADIA_DEPLOY_ARTIFACT` | `<local-artifact-path>` | config | 전송할 backend artifact path다 |
| `THREADIA_DEPLOY_RESTART_COMMAND` | `<remote-restart-command>` | sensitive command | remote restart command다 |
| `THREADIA_DEPLOY_EXECUTE` | `<true-or-false>` | safety gate | `true`일 때만 skeleton deploy가 실행된다 |

## 10. Secret 저장 후보

| 저장 위치 | 사용 대상 | 기준 |
|---|---|---|
| Local `.env` | 개발자 PC local 실행 | commit 금지다 |
| GitHub Environment secrets | production deploy | approval gate와 함께 사용한다 |
| GitHub repository secrets | shared non-prod 또는 CI 일부 | production secret에는 기본으로 사용하지 않는다 |
| AWS Secrets Manager/SSM Parameter Store | EC2/RDS 운영 secret | EC2 runtime 주입 방식 확정 후 검토한다 |

## 11. 관련 파일

| 파일 | 목적 |
|---|---|
| [`.env.example`](../../.env.example) | root/backend/local compose placeholder |
| [`../../frontend/.env.example`](../../frontend/.env.example) | frontend public env placeholder |
| [`../../scripts/deploy/check-env.sh`](../../scripts/deploy/check-env.sh) | variable presence check |
| [`AWS-배포-아키텍처-v1.md`](./AWS-배포-아키텍처-v1.md) | AWS 배포 proposal |
| [`Route53-커스텀-도메인-가이드.md`](./Route53-커스텀-도메인-가이드.md) | domain/DNS guide |
