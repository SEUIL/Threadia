> **작성자**: Threadia 팀
> **작성일**: 2026-07-16
> **최종 수정**: 2026-07-16
> **버전**: v1.2
> **상태**: 검토 중
> **태그**: aws, route53, eip, nginx, certbot, tls

# Route 53, EIP, Nginx, Certbot 수동 운영 가이드

## 1. 목적과 책임 경계

이 문서는 Docker Compose backend EC2 앞에 Route 53, Elastic IP, Nginx, Certbot을 연결하는 사용자 운영 절차를 정의한다. AWS resource 생성, domain 구매, DNS 변경, EC2 bootstrap, certificate 발급과 renewal 설정은 모두 사용자가 직접 수행한다. 이 repository의 workflow와 script는 이 작업을 자동화하거나 수행하지 않는다.

## 2. 사전 결정

운영 시작 전 사용자는 아래 결정을 확정한다.

| 결정 | 확인 기준 |
|---|---|
| Domain 구매 또는 소유권 | 사용자가 registrar에서 domain을 구매하거나 기존 소유권을 확인한다. |
| DNS 운영 주체 | Route 53 hosted zone을 사용할지 registrar DNS를 유지할지 정한다. |
| 공개 API FQDN | frontend origin, OAuth callback, CORS, reset URL과 동일한 HTTPS endpoint 정책을 정한다. |
| EC2 Elastic IP | 사용자가 EIP를 할당하고 대상 EC2에 연결한다. private address나 임시 public address를 DNS 대상으로 사용하지 않는다. |
| TLS 책임 | EC2 Nginx가 TLS를 terminate하고 Certbot이 certificate와 renewal을 관리한다. |

## 3. Domain 구매 방법

### 3.1 Route 53에서 직접 구매

1. AWS Console에서 **Route 53 → Registered domains → Register domains**로 이동한다.
2. 원하는 domain 이름과 TLD를 검색하고, 등록 가능한 이름만 선택한다.
3. 등록 기간, 연락처 정보, 자동 갱신 여부를 검토한 뒤 결제를 완료한다.
4. registrar 확인 이메일이 오면 정해진 기한 안에 소유 확인을 완료한다.
5. 등록이 완료되면 Route 53의 public hosted zone이 생성됐는지 확인한다. 이 zone의 NS record는 직접 수정하지 않는다.

### 3.2 다른 registrar에서 구매

1. 원하는 registrar에서 domain을 구매한다.
2. AWS Console에서 **Route 53 → Hosted zones → Create hosted zone**으로 public hosted zone을 생성한다.
3. 생성된 hosted zone의 NS record에 있는 name server 네 개를 registrar의 name server 설정에 그대로 반영한다.
4. NS delegation이 전파된 것을 확인한 뒤 Route 53에서 API FQDN의 A record를 만든다.

Route 53에서 domain을 구매하는 경우에는 registrar와 hosted zone이 같은 AWS 계정에 있어 이후 DNS 관리가 단순하다. 이미 다른 registrar를 선호하거나 domain을 보유했다면 3.2 절차를 사용한다.

## 4. 사용자 수동 설정 순서

1. domain을 구매하거나 소유권을 확인한다.
2. Route 53을 사용할 경우 hosted zone을 생성하고 registrar name server delegation을 완료한다.
3. EC2에 Elastic IP를 수동 할당·연결한다.
4. Route 53 hosted zone에 공개 API FQDN의 A record를 만들고 Elastic IP를 가리키게 한다.
5. DNS propagation이 완료된 뒤 EC2에 Nginx와 Certbot을 수동 설치한다.
6. Nginx는 public 80/443만 listen하고 `127.0.0.1:8080`의 Compose app으로 reverse proxy한다.
7. HTTP challenge가 가능한 상태에서 Certbot으로 certificate를 발급하고 자동 renewal을 사용자 책임으로 설정한다.
8. HTTPS endpoint, Nginx proxy, backend actuator health, browser CORS와 secure cookie를 순서대로 확인한다.

## 5. Nginx reverse proxy 계약

Compose application port는 `127.0.0.1:8080:8080`으로만 bind된다. 보안 group과 Nginx는 container port 8080을 인터넷에 공개하지 않는다.

Nginx virtual host는 사용자가 선택한 API FQDN만 `server_name`으로 설정하고, upstream을 `http://127.0.0.1:8080`으로 고정한다. proxy에는 원래 Host, client IP forwarding header, HTTPS scheme header를 전달한다. `/actuator/health` endpoint를 외부 공개할지 여부는 별도 정책으로 결정하며, deployment host script의 local health check는 계속 localhost를 사용한다.

## 6. JAR fallback과 Compose의 제어된 전환

legacy JAR runtime과 Compose `app`은 별도 EC2에서 실행한다. 각 host는 자신의 `127.0.0.1:8080`을 사용할 수 있으므로 port binding, systemd unit, release lock을 공유하지 않는다. Compose host의 Nginx는 Compose host의 localhost upstream으로만 reverse proxy한다.

JAR fallback에서 Compose로 최초 전환할 때 사용자는 Compose host에 image를 배포하고 local actuator health와 Nginx proxy health를 확인한 뒤, DNS A record를 Compose public host의 EIP로 변경하거나 public Nginx upstream을 Compose host로 전환한다. 전환 후 실제 public endpoint와 주요 요청을 확인한다.

legacy JAR host는 private host이므로 Compose host에 장애가 있어도 DNS A record 또는 public Nginx upstream을 legacy JAR host로 변경하지 않는다. public traffic fallback은 별도로 승인된 public edge route가 설계·구축되기 전까지 범위 밖이다. Compose host의 digest rollback은 Compose runtime 내부에서만 수행하며, legacy JAR host의 service를 stop, start, enable 또는 disable하지 않는다. legacy JAR/S3/SSM release 경로는 deployment fallback으로 유지한다.

## 7. TLS와 application runtime 정합성

TLS가 정상적으로 terminate된 뒤 사용자는 runtime env의 public config를 실제 HTTPS 정책에 맞춰 설정한다.

- `THREADIA_COOKIE_SECURE`는 HTTPS 운영에서 true여야 한다.
- `THREADIA_CORS_ALLOWED_ORIGIN`은 확정된 frontend HTTPS origin만 허용해야 한다.
- OAuth redirect URI와 password reset URL은 같은 확정 HTTPS domain 정책을 사용해야 한다.
- certificate private key, OAuth client secret, DB password, JWT secret, mail credential은 Nginx 설정, repository, workflow log에 기록하지 않는다.

## 8. 검증과 장애 대응

| 검증 대상 | 사용자 확인 방법 |
|---|---|
| DNS | 공개 API FQDN이 의도한 Elastic IP로 해석되는지 확인한다. |
| HTTP challenge | Certbot이 HTTP challenge endpoint에 도달하는지 확인한다. |
| TLS | certificate chain, hostname, renewal timer를 확인한다. |
| Reverse proxy | HTTPS request가 Nginx를 거쳐 localhost Compose app으로 전달되는지 확인한다. |
| Backend | EC2 localhost에서 `/actuator/health`가 성공하는지 확인한다. |
| Exposure | 8080이 외부 security group, Nginx public listener, DNS record에 노출되지 않았는지 확인한다. |

EIP 연결, DNS, Nginx 또는 certificate 변경이 실패하면 사용자는 최근에 적용한 해당 계층의 수동 변경을 되돌린다. ECR image rollback은 DNS/TLS rollback과 분리되며 `ECR-Compose-SSM-수동-배포-가이드.md`의 digest rollback 절차를 사용한다.

## 9. Network 및 host security checklist

- [ ] RDS는 private subnet에 있고 public access가 disabled다.
- [ ] RDS security group inbound는 application EC2 security group만 source로 허용한다.
- [ ] EC2 security group inbound는 Nginx용 80/443만 허용한다.
- [ ] 8080은 public inbound, DNS record, Nginx public listener에 노출하지 않는다.
- [ ] broad SSH inbound를 허용하지 않고, 운영 접근은 승인된 최소 경로로 제한한다.
- [ ] Docker daemon access와 `docker` group membership이 root-equivalent privilege임을 인지하고 최소 인원만 부여한다.

## 10. 금지사항

- workflow가 Route 53, EIP, EC2, Nginx, Certbot을 생성하거나 변경하게 하지 않는다.
- Docker Compose에 MySQL을 추가하거나 external RDS를 container DB로 대체하지 않는다.
- EIP, domain, certificate, credential, secret을 tracked file 또는 CI log에 기록하지 않는다.
- HTTPS 적용 전 `THREADIA_COOKIE_SECURE`를 production 안전값으로 간주하지 않는다.
- DNS 또는 Nginx upstream 전환 검증 전에는 public traffic을 Compose host로 변경하지 않는다.
- private legacy JAR host를 public DNS 또는 public Nginx upstream 대상으로 지정하지 않는다.
