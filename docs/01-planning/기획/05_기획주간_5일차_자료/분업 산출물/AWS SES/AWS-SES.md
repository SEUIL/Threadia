> **작성자**: Threadia 팀  
> **작성일**: 2026-06-16  
> **최종 수정**: 2026-06-16  
> **버전**: v1.0  
> **상태**: 검토 중

# ADR-001 Email Delivery Provider 선택

## 배경

Threadia는 Spring Boot, React + Next, MySQL 8.4, AWS EC2/RDS/S3 기반의 경량 애자일 협업 플랫폼이다. 현재 외부 email 발송이 직접 필요한 확정 기능은 `AUTH-04` 비밀번호 찾기/재설정이다.

알림 기능은 email이 아니라 MySQL 기반 인앱 알림으로 설계되어 있다. `notification_events`, `notification_recipients`, `notification_preferences`를 사용하며, 이슈 할당, 이슈 상태 변경, 쓰레드 멘션, 문서 수정, 채팅/DM, 권한 변경 이벤트를 앱 안에서 조회하고 읽음 처리한다.

구성원 추가도 email invitation 방식이 아니다. 현재 정책은 가입된 사용자 email이면 즉시 구성원으로 등록하고, 가입되지 않은 email이면 `400 Bad Request`를 반환한다. 따라서 Sprint 1 기준 email 발송 범위는 대량 알림이 아니라 비밀번호 재설정 link 전달 중심이다.

## 결정

Threadia의 email delivery provider는 **AWS SES(Simple Email Service)** 를 우선 채택한다.

단, Sprint 1에서는 SES 설정을 직접 강제하지 않고 `EmailSender` interface를 먼저 둔다. local 환경은 mock 또는 console sender로 처리하고, 실제 발송이 필요한 시점에 AWS SES SMTP 구현체를 연결한다. 운영 고도화 단계에서는 SES API와 SNS/EventBridge 기반 bounce/complaint 처리를 검토한다.

```text
PasswordResetService
  -> EmailSender interface
      -> ConsoleEmailSender local
      -> SesSmtpEmailSender production
      -> SesApiEmailSender future
```

## 이유

### 1. AWS 배포 방향과 일관성이 높다

Threadia는 이미 AWS EC2/RDS/S3 사용을 확정했다. email 발송도 AWS SES로 맞추면 운영 계정, IAM, CloudWatch, EventBridge, billing 관리 흐름을 AWS 안에서 통일할 수 있다.

### 2. Transactional email 비용이 낮다

AWS SES는 일반적으로 대량 transactional email에서 비용이 낮다. 비밀번호 재설정, 가입 확인, 향후 초대 email, 중요 알림 email로 확장하더라도 비용 부담이 작다.

### 3. 운영 확장성이 좋다

SES는 domain verification, DKIM, SPF, DMARC, bounce/complaint event publishing, suppression 관리와 연결할 수 있다. 초기에는 단순 SMTP로 시작하고, 운영 요구가 생기면 API/event 기반으로 확장할 수 있다.

### 4. 일반 SMTP 계정 사용보다 운영 리스크가 낮다

Gmail, Naver, ISP SMTP 같은 일반 계정 기반 SMTP는 rate limit, 계정 잠금, 약한 deliverability visibility, 약한 bounce 처리 문제가 있다. 운영용 password reset email에는 부적합하다.

## 결과

### 긍정적 결과

| 항목 | 결과 |
|---|---|
| 운영 일관성 | AWS 기반 배포 구조와 email provider가 맞는다. |
| 비용 | 소량/대량 transactional email 모두 비용 부담이 낮다. |
| 확장성 | password reset 이후 invite, verification, 중요 알림 email로 확장 가능하다. |
| 보안 | IAM, least privilege, Secrets Manager 연동 등 AWS 보안 모델을 적용할 수 있다. |

### 부정적 결과와 대응

| 리스크 | 영향 | 대응 |
|---|---|---|
| SES sandbox | production access 전에는 verified recipient 중심으로만 테스트 가능하다. | Sprint 1에서는 mock/console sender로 개발하고, 운영 준비 단계에서 production access를 신청한다. |
| DNS 설정 필요 | domain verification, DKIM, SPF, DMARC 설정이 필요하다. | 배포 준비 checklist에 DNS 설정을 포함한다. |
| bounce/complaint 처리 구현 필요 | 운영 품질을 위해 event consumer가 필요하다. | 초기에는 password reset 중심으로 시작하고, email volume 증가 시 SNS/EventBridge 처리로 확장한다. |
| AWS 종속성 | provider 전환 비용이 생긴다. | `EmailSender` interface로 application service와 provider 구현을 분리한다. |

## 대안 검토

### 대안 1. 일반 SMTP provider 사용

| 장점 | 단점 |
|---|---|
| Spring Boot `MailSender`로 빠르게 구현할 수 있다. | 운영용 계정 제한, rate limit, 계정 잠금 리스크가 있다. |
| local 또는 demo 발송 테스트가 쉽다. | bounce/complaint 추적과 suppression 관리가 약하다. |
| 초기 설정이 SES보다 단순하다. | AWS 배포 구조와 운영 계정이 분리된다. |

일반 SMTP는 local 개발 또는 임시 demo에는 적합하지만, Threadia의 운영 email provider로는 권장하지 않는다.

### 대안 2. SendGrid, Mailgun, Postmark 같은 transactional email provider 사용

| 장점 | 단점 |
|---|---|
| onboarding이 SES보다 단순한 편이다. | 월정액 또는 plan 기반 비용이 발생할 수 있다. |
| dashboard, webhook, suppression UI가 제품화되어 있다. | AWS 운영 흐름과 별도 계정/권한/비용 관리가 필요하다. |
| 운영 support와 analytics가 강하다. | Threadia의 현재 email volume과 기능 범위에는 과할 수 있다. |

빠른 상용 출시와 email 운영 UI가 중요하면 좋은 선택이다. 그러나 Threadia는 현재 Sprint 1과 MVP 범위에서 password reset 중심이므로 AWS SES가 더 균형적이다.

### 대안 3. Email 발송 없이 reset token을 화면에 노출

| 장점 | 단점 |
|---|---|
| 구현과 테스트가 가장 쉽다. | 실제 사용자 흐름과 다르고 보안상 부적절하다. |
| 외부 provider 설정이 필요 없다. | password reset 기능의 완성도가 낮아진다. |

local 개발용 mock으로만 허용하고, 실제 운영 또는 시연 환경의 기본 방식으로는 사용하지 않는다.

## 구현 가이드

### Sprint 1

- `EmailSender` interface를 정의한다.
- `ConsoleEmailSender` 또는 `MockEmailSender`로 password reset link를 local log에 남긴다.
- `PasswordResetService`는 email provider를 직접 알지 않도록 한다.
- `.env.example`에는 실제 secret이 아니라 필요한 변수명만 둔다.

예시 변수:

```text
MAIL_PROVIDER=console
MAIL_FROM=no-reply@threadia.com
THREADIA_APP_BASE_URL=http://localhost:3000
```

### 운영 준비

- SES domain identity를 생성한다.
- DKIM, SPF, DMARC DNS record를 설정한다.
- SES production access를 신청한다.
- `MAIL_PROVIDER=ses-smtp`로 전환한다.
- SMTP credential 또는 IAM credential은 secret manager 또는 배포 환경 secret으로 관리한다.

예시 변수:

```text
MAIL_PROVIDER=ses-smtp
MAIL_FROM=no-reply@threadia.com
AWS_REGION=ap-northeast-2
SES_SMTP_HOST=email-smtp.ap-northeast-2.amazonaws.com
SES_SMTP_PORT=587
SES_SMTP_USERNAME=<secret>
SES_SMTP_PASSWORD=<secret>
THREADIA_APP_BASE_URL=https://threadia.com
```

### 운영 고도화

- SES API 기반 발송으로 전환할지 검토한다.
- SNS 또는 EventBridge로 bounce/complaint event를 수집한다.
- suppression list와 user email status 정책을 설계한다.
- reset email 외에 중요 보안 알림, 초대 email, digest email을 별도 정책으로 분리한다.

## 참고 자료

- AWS SES Pricing: <https://aws.amazon.com/ses/pricing/>
- AWS SES sandbox와 production access: <https://docs.aws.amazon.com/ses/latest/dg/request-production-access.html>
- AWS SES quotas: <https://docs.aws.amazon.com/ses/latest/dg/quotas.html>
- AWS SES identity와 DKIM: <https://docs.aws.amazon.com/ses/latest/dg/creating-identities.html>
- AWS SES event publishing: <https://docs.aws.amazon.com/ses/latest/dg/monitor-using-event-publishing.html>
- AWS SES SMTP: <https://docs.aws.amazon.com/ses/latest/dg/send-email-smtp.html>
- Threadia API 명세서 v2.0: `docs/01-planning/기획/04_기획주간_4일차_자료/최종 산출물/API 명세서 v2.0.md`
- Threadia ERD v2.0: `docs/01-planning/기획/04_기획주간_4일차_자료/최종 산출물/ERD v2.0.md`
- Threadia 기능 분해표 v3.0: `docs/01-planning/기획/04_기획주간_4일차_자료/최종 산출물/기능 분해표 v3.0.md`
