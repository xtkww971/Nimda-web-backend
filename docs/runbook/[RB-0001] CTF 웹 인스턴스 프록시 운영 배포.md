# [RB-0001] CTF 웹 인스턴스 프록시 운영 배포

* **최초 작성일:** 2026-10-28
* **최종 수정일:** 2026-10-28
* **작성자:** 이도현
* **대상 시스템:** Web Backend (backend-blue/green), Nginx, Infra (DNS·인증서), Judge Server

---

## 1. 개요 및 트리거

참가자가 `inst-{uuid}.nimda.kr`로 web 문제 인스턴스에 붙는 기능을 **운영에서 처음 켤 때** 한 번 실행한다.
서버를 새로 구축하거나 호스트를 옮길 때도 다시 한다.

짝 작업은 Judge Server `#35`(web 인스턴스를 `ctf-instances`에 띄우고 host=컨테이너 이름, port=EXPOSE 포트로 응답)다.
레포 작업 정리는 `docs/ctf-instance-network-work.md`, 설계 근거는 Judge Server ADR-0017 2-2에 있다.

코드는 로컬 기준으로 만들어져 있었다. 운영에서 깨지는 지점 중 **레포에서 고칠 수 있는 것은 3절에서 고쳤다**
(Redis 접속 정보, 네트워크 이름, nginx 리다이렉트·인스턴스 블록, 업로드 크기, forwarded 헤더).
남은 것은 서버·인프라 작업(4절)과 Judge Server 작업(3절 하단)이다.

* `inst-*.nimda.kr`이 DNS에 없고, 인증서가 `api.nimda.kr` 하나다 → 단계 4·5
* `ctf-instances`가 없고, `nimda-network` 이름이 운영에서 다르다 → 단계 2·3
* CI가 `deploy.sh`만 보내서 compose·nginx 변경이 서버에 안 간다 → 단계 6

3절 표의 Judge 항목(J1·J2)이 끝나야 실제 접속이 된다.

## 2. 사전 준비 사항 및 리스크

* **필요 권한:** 운영 서버 SSH + `sudo`. `nimda.kr` DNS 관리 권한. 서버 `.env` 수정 권한.
* **주의사항:**

  > ⚠️ `ctf-instances` 네트워크가 없으면 backend 컨테이너가 아예 안 뜬다
  > (`network ctf-instances declared as external, but could not be found`).
  > `deploy.sh`는 새 색상 기동에 실패하면 트래픽을 안 넘기고 멈추지만, **네트워크 생성(4절 단계 2)을 배포보다 먼저** 한다.

  > ⚠️ `nimda-network` 이름을 고정하면(3절 R5) 네트워크가 새로 만들어진다. mysql·redis·backend·nginx를
  > 전부 내렸다 올려야 하므로 **짧은 다운타임이 생긴다.** 사용자가 적은 시간에 한다. 볼륨은 그대로 남는다(`down -v` 금지).

  > ⚠️ `AUTH_COOKIE_DOMAIN=nimda.kr`을 걸면 로그인 쿠키가 모든 `*.nimda.kr`로 간다.
  > 프록시 필터가 문제 컨테이너로 넘기기 전에 `Authorization` 쿠키를 지운다(`SubdomainInstanceProxyFilter.stripPlatformCookies`).
  > 근본 해결은 도메인 분리(ADR-0017 2-1)이고 이번 범위 밖이다.

## 3. 배포 전에 레포에서 고쳐야 하는 것

| # | 항목 | 파일 | 왜 필요한가 | 상태 |
|---|---|---|---|---|
| R1 | backend-blue/green을 `ctf-instances`(external)에 연결 | `docker-compose.yml` | 컨테이너 이름을 embedded DNS로 풀려면 같은 망에 있어야 한다 | 완료 (단계 1) |
| R2 | `CTF_INSTANCE_BASE_DOMAIN`, `AUTH_COOKIE_DOMAIN` 전달 | `docker-compose.yml` | `environment`에 없으면 `.env`에 넣어도 컨테이너에 안 들어간다. 비어 있으면 프록시가 꺼진다 | 완료 (단계 2) |
| R3 | backend에 Redis 접속 정보 전달 | `docker-compose.yml` | 운영 compose에 Redis 설정이 없어서 `localhost:6379`, 비밀번호 없음으로 붙었다. 운영 redis는 `--requirepass`라 결과·소유자 조회가 전부 실패한다. host는 서비스 이름 `redis`로 고정(`.env`의 `localhost`는 IDE용), 비밀번호는 redis 서버와 같은 `REDIS_PASSWORD`를 쓴다. 로컬(비밀번호 없음)은 기본값이 빈 값이라 그대로 된다 | 완료 |
| R4 | 운영 프로필 지정 | `application.yml` | 기본값 `spring.profiles.active: local`. 코드를 올릴 때 `local`을 지워서 올리는 것으로 운영한다 | 해당 없음 |
| R5 | `nimda-network` 이름 고정 (`name: nimda-network`) | `docker-compose.yml` | Judge provisioner가 `nimda-network`를 external로 찾는다. 이름을 안 고정하면 실제 이름이 `{프로젝트}_nimda-network`가 된다. **반영하면 서버에서 4절 단계 3(전체 재기동)이 필요하다** | 완료 |
| R6 | 80 포트에서 `*.nimda.kr` 전부 HTTPS로 리다이렉트 | `nginx/nginx.conf` | 전엔 `api.nimda.kr`만 리다이렉트하고 나머지 `*.nimda.kr`은 http로 필터 없이 backend에 넘겼다. 인증 쿠키가 `Secure`라 http에선 안 실려서 **항상 401**이었다. IP(`43.200.36.32`) http 접근은 그대로 둔다 | 완료 |
| R7 | 443에 인스턴스 전용 `server` 블록 분리 | `nginx/nginx.conf` | 아래 상세 | 완료 |
| R8 | `server.forward-headers-strategy: native` | `application.yml` | `accessUrl`이 `request.getScheme()`을 쓴다. nginx 뒤에선 `http`로 잡혀 `http://inst-...`가 만들어졌다. 해당 엔드포인트는 지금 주석 처리라 직접 검증은 못 했다 | 완료 (미검증) |
| R9 | `client_max_body_size 10m` | `nginx/nginx.conf` | `application.yml` multipart 주석은 "nginx와 일치"인데 nginx엔 설정이 없어서 기본 1MB였다. 1MB 넘는 업로드가 API·인스턴스 모두 413 | 완료 |
| R10 | JVM DNS 캐시 끄기 (`networkaddress.cache.ttl`/`negative.ttl` = 0) | `NimdaCiteApplication.java` | JVM이 이름 조회를 30초(없음은 10초) 캐시한다. 인스턴스를 재생성하면 IP가 바뀌는데, **옛 IP를 다른 컨테이너가 받으면 30초 동안 요청이 그 컨테이너로 갔다**(로컬 재현: 소유권 검사 통과 후 남의 컨테이너가 응답). 재생성 직후엔 10초간 502도 났다 | 완료 |

### R7 상세 — 전에 443 블록이 인스턴스에 하던 일

`server_name api.nimda.kr *.nimda.kr;` 한 블록이라, 인스턴스 요청에도 아래가 그대로 걸렸다.

| 설정 | 인스턴스에 미치는 영향 |
|---|---|
| `if ($query_string ~* "union.*select") { return 403; }` | **SQL injection 문제를 풀 수 없다.** 페이로드가 문제에 닿기 전에 403 |
| `if ($query_string ~* "(<\|%3C).*script.*(>\|%3E)") { return 403; }` | **XSS 문제를 풀 수 없다** |
| `location ~* \.(env\|log\|git\|sh\|bak\|php\|sql)$ { deny all; }` | `.php` 문제, `.git`/`.bak` 노출 문제가 안 열린다 |
| `location /api { limit_req ... }` | 문제 안의 `/api/...` 경로가 플랫폼 rate limit을 탄다 |
| `ssl_certificate .../live/api.nimda.kr/...` | 인증서에 `*.nimda.kr`이 없으면 브라우저가 경고하고 막는다 |

블록을 둘로 나눴다.

* **API 블록** `server_name api.nimda.kr;` — 기존 필터·rate limit(10r/s) 그대로. 첫 443 블록이라 **기본 서버**다.
* **인스턴스 블록** `server_name ~^inst-[^.]+\.nimda\.kr$;` — 필터 없이 `location /`만 backend로 넘긴다.
  rate limit은 별도 zone(30r/s, burst 100). 인증서는 `live/nimda.kr/`(단계 5의 와일드카드).

인스턴스 블록을 `*.nimda.kr` 전체로 열지 않은 이유: 그러면 `foo.nimda.kr` 같은 아무 Host로 API 필터와 rate limit을 건너뛰고
backend API에 닿는다. 정규식에 안 맞는 호스트(`foo.nimda.kr`, 중첩된 `inst-a.b.nimda.kr`)는 기본 서버인 API 블록으로 떨어져 필터를 탄다.
`inst-{토큰}` 호스트는 backend 프록시 필터가 항상 가로채므로 API로 새지 않는다.

WebSocket 헤더는 넣지 않았다. backend의 `InstanceProxy`가 Java `HttpClient`로 요청 단위만 전달해서 WS를 지원하지 않는다.

> ⚠️ 인스턴스 블록이 `live/nimda.kr/` 인증서를 참조한다. **파일이 없으면 nginx가 설정을 거부하고 안 뜬다.**
> 서버에서 이 nginx.conf를 받기(단계 6) 전에 인증서(단계 5)가 있어야 한다. `restart: always`라 재부팅 때도 그대로 읽는다.

### Judge Server 쪽 (이 레포 밖)

| # | 항목 | 왜 필요한가 |
|---|---|---|
| J1 | `#35` 구현·배포 | 지금 agent는 web 인스턴스를 `CTF_INSTANCE_HOST=127.0.0.1` + host 포트로 띄운다. 2026-10-28 기준 어느 브랜치에도 `ctf-instances` 코드가 없다 |
| J2 | provisioner의 `REDIS_ADDR=nimda-redis:6379`에 비밀번호 | 운영 nimda-redis는 `--requirepass`다. 비밀번호 없이 붙으면 요청 스트림을 못 읽는다 |
| J3 | 같은 호스트에서 실행 | 도커 네트워크는 호스트 단위다. backend와 agent가 만든 인스턴스가 **같은 도커 호스트**에 있어야 `ctf-instances`로 묶인다 |
| J4 | 응답 host를 **63자 이하**로 | 컨테이너 이름 `ctf-{code}-{uuid}`가 그대로 DNS 라벨 하나가 된다. DNS 라벨은 최대 63바이트라, `ctf-`(4) + `-`(1) + uuid(36) = 41이므로 **code가 22자를 넘으면 이름 해석이 실패한다**(컨테이너는 뜨지만 backend가 502). code는 50자까지 허용된다. 로컬에서 63자 200, 64자 resolve 실패 확인. `ctf-instances`에 붙일 때 짧은 네트워크 alias(예: `i-{uuid}`, 38자)를 주고 그걸 host로 돌려주면 된다 |
| J5 | `Existing` 응답에 `expiresAt` 싣기 | web은 READY 결과를 `expiresAt`까지 보관한다(`docs/issue/instance-bugfix-issue.md` B2). 이미 떠 있는 인스턴스를 돌려주는 `Existing`은 `expiresAt`을 안 실어서, 재사용으로 받은 결과는 web 기본 TTL(10분)로 떨어진다 |

## 4. 단계별 실행 절차 (운영 서버)

서버 작업 디렉터리는 `/home/ubuntu/app`이다(CI `ci.yml` 기준).

### 단계 1. 현재 상태 기록

되돌릴 때 비교하려고 지금 상태를 적어둔다.

```bash
cd /home/ubuntu/app
docker ps --format '{{.Names}}\t{{.Networks}}\t{{.Status}}' | tee ~/rb0001-before.txt
docker network ls | tee -a ~/rb0001-before.txt
cat nginx/conf.d/active-backend.inc | tee -a ~/rb0001-before.txt
cp .env ~/rb0001-env.bak
cp nginx/nginx.conf ~/rb0001-nginx.conf.bak
```

### 단계 2. ctf-instances 네트워크 생성

인터넷으로 못 나가는(`--internal`) bridge 네트워크를 만든다. Judge `#35`의 runbook과 같은 명령이다. 이미 있으면 "already exists"가 나고, 그대로 두면 된다.

```bash
docker network create --internal ctf-instances
docker network inspect ctf-instances -f '{{.Internal}}'   # true 여야 한다
```

### 단계 3. nimda-network 이름 확인

```bash
docker network ls --filter name=nimda-network --format '{{.Name}}'
```

* `nimda-network`가 나오면 넘어간다.
* `app_nimda-network`처럼 접두사가 붙어 있으면, R5가 반영된 compose로 **전체 스택을 한 번 내렸다 올린다.**
  볼륨을 지우는 `-v`는 절대 붙이지 않는다.

```bash
git pull                       # R5가 반영된 docker-compose.yml을 받는다 (단계 6 참고)
docker compose down            # -v 금지. 다운타임 시작
docker compose up -d mysql redis
docker compose up -d backend-$(grep -q blue nginx/conf.d/active-backend.inc && echo blue || echo green) nginx
docker network ls --filter name=nimda-network --format '{{.Name}}'   # nimda-network
```

### 단계 4. 와일드카드 DNS

`nimda.kr` DNS에 레코드를 추가한다. 값은 `api.nimda.kr`과 같은 서버 IP(`43.200.36.32`)다.

```
Type: A | Name: *.nimda.kr | Value: 43.200.36.32
```

```bash
dig +short inst-dnscheck.nimda.kr      # 43.200.36.32 가 나와야 한다
```

### 단계 5. 와일드카드 인증서

와일드카드 인증서는 HTTP-01(웹루트)로는 못 받는다. **DNS-01**로 받는다.
아래는 수동 방식이고, 안내되는 TXT 레코드를 DNS에 넣고 진행한다.

```bash
docker run --rm -it \
  -v "$PWD/certbot/conf:/etc/letsencrypt" \
  certbot/certbot certonly --manual --preferred-challenges dns \
  -d nimda.kr -d '*.nimda.kr'
```

> 수동 DNS-01은 **자동 갱신이 안 된다.** 90일마다 다시 하거나, DNS 제공자의 certbot 플러그인으로 바꾼다.
> 발급된 경로(`live/nimda.kr/`)는 R7의 인스턴스 블록 `ssl_certificate`에 쓴다.

### 단계 6. 서버 레포·설정 파일 반영

**CI는 `deploy.sh`만 서버로 복사한다.** `docker-compose.yml`과 `nginx/nginx.conf` 변경은 자동으로 안 간다.
서버에서 직접 최신으로 맞춘다.

```bash
cd /home/ubuntu/app
git pull
git log --oneline -1
```

### 단계 7. .env 값 추가

```bash
# CTF 인스턴스 서브도메인 프록시 (R2)
CTF_INSTANCE_BASE_DOMAIN=nimda.kr
# 로그인 쿠키를 inst-*.nimda.kr에도 보낸다 (R2)
AUTH_COOKIE_DOMAIN=nimda.kr
```

backend의 Redis 비밀번호는 redis 서버와 같은 `REDIS_PASSWORD`를 쓴다(R3). 이미 있는 값이라 추가할 것은 없다.

### 단계 8. nginx 반영

설정을 검사하고 무중단으로 다시 읽는다.

```bash
docker exec nimda-nginx nginx -t
docker exec nimda-nginx nginx -s reload
```

`nginx -t`가 실패하면 reload하지 않고 단계 1의 백업으로 되돌린다.

### 단계 9. backend 배포

평소처럼 blue/green 배포한다. 새 색상 컨테이너가 `ctf-instances`에 붙은 채로 뜬다.

```bash
./deploy.sh
docker inspect backend-blue  -f '{{range $k,$v := .NetworkSettings.Networks}}{{$k}} {{end}}'
docker inspect backend-green -f '{{range $k,$v := .NetworkSettings.Networks}}{{$k}} {{end}}'
# 둘 다 ctf-instances nimda-network
docker exec backend-blue sh -c 'echo $CTF_INSTANCE_BASE_DOMAIN $AUTH_COOKIE_DOMAIN'   # nimda.kr nimda.kr
```

backend는 Judge `#35`보다 먼저 배포해도 된다. 네트워크에 붙기만 하고, 인스턴스가 아직 `127.0.0.1`로 오면 지금처럼 접속이 안 될 뿐이다.

### 단계 10. Judge Server #35 배포

Judge Server 레포에서 J1·J2를 반영해 배포한다. 절차는 Judge 쪽 runbook을 따른다.

## 5. 검증

**① 망 연결** — 테스트 컨테이너를 `ctf-instances`에 띄우고 backend에서 이름으로 붙는다.

```bash
docker run -d --rm --name ctf-rb0001-check --network ctf-instances nginx:alpine
docker exec backend-blue curl -s -o /dev/null -w '%{http_code}\n' http://ctf-rb0001-check:80/   # 200
docker exec ctf-rb0001-check wget -q -T 5 -O /dev/null https://example.com && echo BAD || echo OK   # OK (인터넷 막힘)
docker stop ctf-rb0001-check
```

**② 실제 흐름** — 참가자 계정으로 web 문제 인스턴스를 만들고 브라우저에서 `https://inst-{uuid}.nimda.kr`에 접속한다.

| 경우 | 기대 |
|---|---|
| 본인 인스턴스 | 문제 페이지 (200) |
| 다른 계정으로 같은 주소 | 403 |
| 로그아웃 상태 | 401 |
| 만료·없는 uuid | 503 |
| `http://inst-...` | `https://`로 301 (R6) |
| 문제에 `?q=1' union select 1--` | nginx 403이 아니라 **문제 응답** (R7) |
| 인증서 | 경고 없음, SAN에 `*.nimda.kr` |

**③ 재생성** — 같은 문제 인스턴스를 재생성한 뒤 같은 주소로 다시 붙는다(이름·EXPOSE 포트는 안 바뀐다).

**④ 쿠키 유출** — 문제 컨테이너 로그나 요청 헤더에 `Authorization` 쿠키가 없어야 한다.

## 6. 완료 조건

- [ ] 3절 R1~R10이 서버에 반영됐고 Judge J1·J2·J4가 배포됐다
- [ ] `ctf-instances`가 있고 `Internal=true`다
- [ ] `nimda-network`가 접두사 없는 이름이고 Judge provisioner가 붙어 있다
- [ ] backend-blue/green이 `ctf-instances`·`nimda-network` 둘 다에 붙어 있다
- [ ] `*.nimda.kr`이 서버 IP로 풀리고, 와일드카드 인증서로 HTTPS가 된다
- [ ] 5절 ②의 표가 전부 기대대로 나온다
- [ ] 재생성 후에도 접속이 유지된다
- [ ] 문제 컨테이너에 `Authorization` 쿠키가 안 넘어간다

## 7. 원복

**프록시만 끄기 (가장 빠름)** — `.env`에서 두 값을 비우고 backend를 다시 배포한다. 서브도메인 요청을 아무것도 가로채지 않는다.

```bash
sed -i 's/^CTF_INSTANCE_BASE_DOMAIN=.*/CTF_INSTANCE_BASE_DOMAIN=/; s/^AUTH_COOKIE_DOMAIN=.*/AUTH_COOKIE_DOMAIN=/' .env
./deploy.sh
```

> 쿠키 도메인을 비우면 기존 `Domain=nimda.kr` 쿠키는 만료 전까지 브라우저에 남는다. 새로 로그인하면 host-only로 다시 받는다.

**nginx 되돌리기**

```bash
cp ~/rb0001-nginx.conf.bak nginx/nginx.conf
docker exec nimda-nginx nginx -t && docker exec nimda-nginx nginx -s reload
```

**네트워크 제거** — backend가 `ctf-instances`를 참조하는 compose로 떠 있으면 지울 수 없다. compose에서 R1을 되돌린 뒤 지운다.

```bash
docker network rm ctf-instances
```

## 8. 이번에 하지 않는 것

* **도메인 분리(ADR-0017 2-1).** `inst-{uuid}.nimda-ctf.kr` 별도 도메인. 이게 되면 `AUTH_COOKIE_DOMAIN`을 넓힐 필요가 없어진다.
  지금 CORS가 `https://*.nimda.kr`을 credentials와 함께 허용하므로 인스턴스 출처도 플랫폼 API를 부를 수 있는데, 이것도 도메인 분리로 같이 닫힌다.
* **응답 Set-Cookie 제거.** 문제 컨테이너가 `Domain=.nimda.kr` 쿠키를 심는 것 막기.
* **문제 응답에 붙는 Spring Security 헤더.** 프록시 응답에 backend가 `X-Frame-Options: DENY` 등을 덧붙인다.
  iframe·clickjacking 계열 문제는 이것 때문에 의도대로 안 될 수 있다(로컬 테스트에서 확인).
* **매 요청 CTF 조회.** 지금은 `InstanceResultStore` 캐시(`CTF_TTL_RESULT`, 10분)를 쓴다.
* **인스턴스별 네트워크.** `ctf-instances` 하나라 web 인스턴스끼리는 서로 보인다(ADR-0017 현상 유지).
