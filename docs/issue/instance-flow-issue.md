# [web] CTF 인스턴스 조회 API 복구 — 생성부터 접속 주소까지 흐름 연결

> 선행: `docs/issue/instance-bugfix-issue.md`(TTL·재사용·사용자 매핑). 이 이슈는 그 수정 위에서 동작한다.
> 해당 이슈는 **backend API**만 다룬다. 프론트(CTF 문제 화면)는 아래 API 명세로 따로 진행한다.

### 배경

지금 인스턴스 API는 생성(`POST /api/ctf/instance/{code}` → uuid) 하나만 살아 있다.
상태 조회, 내 인스턴스 조회, 접속 주소(`accessUrl`)는 당시 뒤쪽 기능(결과 소비, 서브도메인 프록시)이 없어서 주석 처리돼 있었다.
그래서 지금은 인스턴스를 만들어도 **언제 준비됐는지, 어디로 접속하는지** 알 방법이 없다.

네트워크·프록시·배포 쪽은 끝났으므로(`docs/ctf-instance-network-work.md`, RB-0001), 조회 API를 지금 구조에 맞게 되살린다.

```
POST /api/ctf/instance/{code}        ──▶ uuid           (생성, 이미 있으면 재사용)
GET  /api/ctf/instance/status/{uuid} ──▶ PENDING / READY + accessUrl / FAILED   (생성 직후 폴링)
GET  /api/ctf/instance/{code}        ──▶ 이 문제로 내가 띄운 인스턴스          (페이지 진입·새로고침)
브라우저 ──▶ accessUrl (https://inst-{uuid}.nimda.kr) ──▶ SubdomainInstanceProxyFilter ──▶ 컨테이너
```

### 설계

**1. 상태 판단**

결과(`ctf:result:{uuid}`)와 생성 중 마커(`ctf:instance:status{uuid}`)로 정한다.

| 결과 | 마커 | 응답 status |
|---|---|---|
| READY | - | `READY` + `accessUrl`, `expiresAt` |
| FAILED | - | `FAILED` + `message`(Judge가 준 사유) |
| 없음 | 있음 | `PENDING` |
| 없음 | 없음 | `EXPIRED` — 결과가 만료됐다. 다시 생성(POST)하면 같은 uuid로 이어 붙는다 |

**2. 소유권**

uuid를 아는 사람이 남의 상태·주소를 보면 안 된다. 결과 메시지에는 userId가 없으므로
발행 때 남긴 소유자 매핑(`ctf:request:owner:{uuid}`)으로 확인한다. 매핑이 없으면 막는다(다운로드 API와 같은 규칙).

* 결과가 아직 없을 때(PENDING)도 소유자를 먼저 확인한다. 주석 속 코드는 결과가 없으면 확인 없이 PENDING을 돌려줬다.

**3. 응답에서 빼는 것**

`host`/`port`는 내부 컨테이너 이름·EXPOSE 포트라 응답에 싣지 않는다. 참가자는 `accessUrl`만 쓴다.

**4. accessUrl**

`InstanceService.accessUrl`을 그대로 쓴다. nginx 뒤에서 scheme이 https로 잡히는 건 RB-0001 R8(`forward-headers-strategy`)이 맡는다.
`CTF_INSTANCE_BASE_DOMAIN`이 비어 있으면 `accessUrl`은 null이다(서브도메인 프록시가 꺼진 상태).

### API 명세 (프론트용)

모든 응답은 `ApiResponse` 형식(`{ success, data, message }`)이다. 로그인(쿠키) 필수 — 비로그인은 Spring Security가 컨트롤러 전에 **403**으로 막는다(앱 전체 동작).

**POST `/api/ctf/instance/{code}`** — 생성 또는 재사용

```json
{ "uuid": "0e3f7c1a-...", "status": "PENDING" }
```

```json
// 이미 살아 있으면 (같은 uuid)
{ "uuid": "0e3f7c1a-...", "status": "READY", "accessUrl": "https://inst-....nimda.kr", "expiresAt": "..." }
```

* 404: 없는 문제, 비공개 문제(관리자 제외) / 400: WEB 문제가 아님
* 이미 READY면 `status: "READY"`가 바로 온다. 이어서 status 폴링을 하면 된다.

**GET `/api/ctf/instance/status/{uuid}`** — 폴링 (1~2초 간격 권장)

```json
{ "uuid": "...", "status": "READY", "accessUrl": "https://inst-....nimda.kr", "expiresAt": "2026-10-28T10:00:00Z" }
{ "uuid": "...", "status": "PENDING" }
{ "uuid": "...", "status": "FAILED", "message": "컨테이너 실행 실패: ..." }
{ "uuid": "...", "status": "EXPIRED" }
```

* 403: 본인 인스턴스가 아님(또는 소유자 매핑 만료)

**GET `/api/ctf/instance/{code}`** — 이 문제의 내 인스턴스

```json
{ "hasInstance": false }
{ "hasInstance": true, "uuid": "...", "status": "READY", "accessUrl": "...", "expiresAt": "..." }
```

* status 값은 위와 같다. 문제 페이지에 들어올 때 한 번 부르고, PENDING이면 status 폴링으로 넘어간다.

### 작업

* [x] `InstanceService`: 상태 조회(`findView(uuid)`), 내 인스턴스 uuid 조회
* [x] `InstanceController`: `GET /status/{uuid}`, `GET /{code}` 복구, 소유자 매핑으로 소유권 확인, host/port 제외
* [x] `POST /{code}` 응답에 status 포함
* [x] Deprecated 경로 접두사 프록시(`/{requestId}/app/**`) 주석 블록 제거 — 서브도메인 프록시로 대체됨
* [x] 테스트: 생성 → PENDING → (결과 주입) READY + accessUrl → accessUrl로 접속 200, 남의 uuid 403, FAILED, EXPIRED, 재진입(`GET /{code}`)

### 완료 조건

* [x] 생성 → 폴링 → `accessUrl` 접속까지 API만으로 된다
* [x] 남의 uuid로 status를 부르면 403이다 (결과가 없을 때도)
* [x] 새로고침 후 `GET /{code}`로 같은 인스턴스를 다시 찾는다
* [x] 응답에 `host`/`port`가 없다
* [x] nginx 뒤(`X-Forwarded-Proto: https`)에서 `accessUrl`이 `https://`다 (R8 검증)

* 비공개 문제 404: 다운로드 API와 같은 규칙으로 막았다. 로컬 문제가 전부 공개라 데이터로는 확인하지 않았다.

### 이번에 하지 않는 것

* **프론트 CTF 문제 화면.** 프론트에 CTF 문제 목록·상세가 아직 없다. 위 명세로 별도 진행한다.
* **인스턴스 연장·종료 버튼.** Judge 프로토콜이 먼저 필요하다.
* **SSE·WebSocket 상태 알림.** 이번엔 폴링으로 둔다.
