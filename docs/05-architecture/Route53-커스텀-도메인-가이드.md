> **작성자**: Threadia 팀
> **작성일**: 2026-07-11
> **최종 수정**: 2026-07-11
> **버전**: v0.1 Route 53 custom domain 준비 guide
> **상태**: 작성 중
> **태그**: aws, route53, domain, dns, acm, https

# Route 53 커스텀 도메인 가이드

## 1. 문서 목적

이 문서는 Threadia의 custom domain을 Route 53으로 운영하기 전에 확인해야 하는 domain registration, hosted zone, NS delegation, DNS record, ACM certificate, HTTPS 설정 기준을 정리한다. 실제 domain 구매, hosted zone 생성, DNS record 변경, certificate 발급은 승인 필요 follow-up이다.

## 2. Domain spelling 기준

사용자 제시 domain candidate는 `theadia.com`이다. 기존 문서에는 `threadia.com` 예시가 남아 있다. 최종 spelling을 확정하기 전에는 다음 작업을 실행하지 않는다.

1. domain purchase 또는 transfer다.
2. Route 53 hosted zone 생성이다.
3. OAuth redirect URI 등록이다.
4. CORS allowed origin 변경이다.
5. cookie domain 또는 HTTPS certificate 설정이다.

이 문서의 예시는 candidate 기준으로 `theadia.com`을 사용한다.

## 3. Domain registration과 hosted zone의 차이

| 항목 | 의미 | Threadia에서의 작업 |
|---|---|---|
| Domain registration | `theadia.com` 이름을 registrar를 통해 소유하는 절차 | 구매 또는 기존 소유 여부 확인이 필요하다 |
| Hosted zone | Route 53이 `theadia.com` DNS record를 관리하는 zone | AWS에서 hosted zone을 만들고 NS delegation을 연결해야 한다 |
| NS delegation | registrar에 Route 53 name server를 등록하는 절차 | Route 53 hosted zone의 NS 값을 registrar에 반영해야 한다 |

Domain을 AWS Route 53에서 직접 등록하면 hosted zone 연결이 단순하다. 다른 registrar에서 구매하면 registrar의 name server 설정을 Route 53 NS 값으로 바꿔야 한다.

## 4. 권장 subdomain 구조

| 도메인 | 대상 | DNS record 후보 |
|---|---|---|
| `theadia.com` | landing 또는 redirect 후보 | apex A/AAAA alias 또는 redirect service 후보 |
| `app.theadia.com` | Threadia frontend | Amplify custom domain 또는 CloudFront/hosting alias |
| `api.theadia.com` | Spring Boot backend API | EC2 public IP/EIP, ALB, 또는 CloudFront/API routing 후보 |
| `docs.theadia.com` | docs-site 선택 항목 | Amplify, CloudFront, 또는 후속 docs hosting 후보 |

Frontend와 backend를 subdomain으로 분리하면 `NEXT_PUBLIC_API_URL=https://api.theadia.com`, `THREADIA_CORS_ALLOWED_ORIGIN=https://app.theadia.com` 기준으로 설정한다.

## 5. Apex와 subdomain record 구분

| 구분 | 예시 | 주의점 |
|---|---|---|
| Apex/root domain | `theadia.com` | CNAME을 직접 쓰기 어렵다. AWS alias record 또는 A/AAAA record 후보를 검토한다 |
| Subdomain | `app.theadia.com`, `api.theadia.com` | CNAME 또는 alias record 후보를 사용할 수 있다 |

AWS managed endpoint가 있으면 Route 53 alias record를 우선 검토한다. EC2 단일 인스턴스에 직접 연결하면 Elastic IP를 사용해 A record를 연결하는 방식을 검토한다.

## 6. ACM certificate와 DNS validation

ACM certificate는 HTTPS 적용을 위한 인증서다. DNS validation을 사용하면 ACM이 제공하는 CNAME validation record를 Route 53 hosted zone에 추가해 domain 소유를 증명한다.

| 사용 위치 | ACM region 주의 |
|---|---|
| CloudFront | certificate는 `us-east-1`에 있어야 한다 |
| ALB, API Gateway, regional service | service가 있는 region의 ACM certificate를 사용한다 |
| EC2 Nginx 직접 TLS | ACM certificate를 직접 EC2에 내려받아 쓰는 방식은 일반적이지 않으므로 ALB/CloudFront 또는 별도 certificate 관리 방식을 검토한다 |

이번 pass에서는 certificate를 발급하지 않는다. DNS validation record도 추가하지 않는다.

## 7. HTTPS 운영 메모

| 항목 | 기준 |
|---|---|
| Backend cookie | 운영에서는 `THREADIA_COOKIE_SECURE=true`가 필요하다 |
| CORS | frontend origin인 `https://app.theadia.com`만 허용하는 방향을 우선한다 |
| OAuth | redirect URI는 `https://api.theadia.com/api/v1/auth/oauth2/callback/google` 후보를 등록한다 |
| HSTS | production 안정화 후 적용 여부를 검토한다 |
| Mixed content | frontend가 `https://api.theadia.com`만 호출하도록 `NEXT_PUBLIC_API_URL`을 설정한다 |

## 8. 실행 순서 후보

아래 순서는 승인 후 실행할 후보 절차다.

1. `theadia.com`과 `threadia.com` 중 최종 spelling을 확정한다.
2. domain registration 또는 소유권을 확인한다.
3. Route 53 hosted zone을 생성한다.
4. registrar의 NS를 Route 53 hosted zone NS로 delegation한다.
5. `app`, `api`, 선택적으로 `docs` subdomain record 설계를 확정한다.
6. HTTPS termination 위치를 확정한다.
7. ACM certificate를 요청하고 DNS validation record를 추가한다.
8. Frontend hosting, backend endpoint, docs hosting record를 연결한다.
9. CORS, OAuth redirect URI, cookie secure 설정을 반영한다.
10. 실제 browser/API smoke test를 수행한다.

## 9. 승인 필요 checklist

- [ ] 최종 domain spelling 확정이다.
- [ ] domain purchase 또는 registrar 설정 변경 승인이다.
- [ ] Route 53 hosted zone 생성 승인이다.
- [ ] NS delegation 변경 승인이다.
- [ ] DNS record 추가 또는 수정 승인이다.
- [ ] ACM certificate 발급 승인이다.
- [ ] OAuth console redirect URI 변경 승인이다.
- [ ] backend CORS/cookie 운영 설정 변경 승인이다.

## 10. 참조 문서

| 문서 | 목적 |
|---|---|
| [`AWS-배포-아키텍처-v1.md`](./AWS-배포-아키텍처-v1.md) | AWS 배포 구조 proposal |
| [`AWS-운영-환경변수-목록.md`](./AWS-운영-환경변수-목록.md) | domain, ACM, GitHub Actions env inventory |
| [`../00-project/sprint-운영/sprint-2-CI-Deploy-준비계획.md`](../00-project/sprint-운영/sprint-2-CI-Deploy-준비계획.md) | Sprint 2 Day 6 CI/Deploy 준비 계획 |
