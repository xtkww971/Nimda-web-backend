# CTF API 명세

CTF 문제, 첨부파일, 문제 인스턴스, 그룹 API 명세입니다. (backend `judgeServer` 패키지 전체)

* 기준일: 2026-10-29 (문제 유형별 접속 방식, 실패 응답 형식 반영)
* Base URL: `https://api.nimda.kr`
* 인증: 로그인하면 `Authorization` 쿠키가 발급됩니다. 모든 요청에 쿠키를 실어 보내야 합니다 (`credentials: 'include'` / axios `withCredentials: true`).

---

## 1. 공통

### 1-1. 성공 응답 형식

모든 성공 응답은 아래 형식입니다. 값이 없는 필드(`message`, `data`)는 응답에서 빠집니다.

```json
{
  "success": true,
  "message": "그룹 목록을 성공적으로 조회하였습니다.",
  "data": { }
}
```

이 문서의 응답 예시는 `data` 안의 내용만 적습니다.

### 1-2. 실패 응답 형식

**대부분의 실패는 공통 형식**입니다. 상태 코드는 그대로 오고, `message`에 사유가 들어 있어서 화면에 그대로 보여줘도 됩니다.

```json
{ "success": false, "message": "인스턴스 문제는 첨부파일 다운로드를 지원하지 않습니다." }
```

**아래 세 경우만 Spring 기본 형식**이라 `message`가 없습니다. 상태 코드로 구분해서 화면 문구를 직접 정합니다.

| 경우 | 상태 코드 | 화면 처리 예시 |
|---|---|---|
| 로그인하지 않음 | 403 | 로그인 페이지로 이동 |
| 관리자 전용 API를 일반 사용자가 호출 | 403 | "권한이 없습니다" |
| 문제 등록(2-5)에서 필수값 누락·형식 오류 | 400 | 폼에서 먼저 막는다 (2-5 필드 표) |

```json
{
  "timestamp": "2026-10-29T05:53:53.284+00:00",
  "status": 403,
  "error": "Forbidden",
  "path": "/api/ctf/challenges"
}
```

> 구분 방법: 응답에 `success` 필드가 있으면 공통 형식, 없으면 Spring 기본 형식입니다.
> 그룹 API는 형식은 공통이지만 거의 모든 실패가 500으로 옵니다(4절).

### 1-3. 권한

| 구분 | 설명 |
|---|---|
| 로그인 필요 | 이 문서의 모든 API. 비로그인이면 **403** (401이 아닙니다) |
| 관리자 전용 | 표에 `ADMIN`으로 표시. 일반 사용자는 403 |

### 1-4. 코드값 (enum)

| 이름 | 값 | 의미 |
|---|---|---|
| category | `WEB` `PWN` | 인스턴스 문제. 참가자가 문제 서버를 띄워서 푼다 (3절) |
| category | `REVERSING` `FORENSICS` `CRYPTO` `MISC` | 정적 문제. 첨부파일을 받아서 푼다 (2-3) |
| isolationType | `NONE` | 인스턴스 없음 (파일만 받는 문제) |
| isolationType | `SHARED` | 모든 참가자가 컨테이너 하나를 같이 씀 |
| isolationType | `PER_USER` | 참가자마다 컨테이너를 따로 띄움 |

문제 유형마다 쓰는 기능이 정해져 있습니다.

| category | 첨부파일 받기 | 인스턴스 | 접속 방법 |
|---|---|---|---|
| WEB | 안 됨 | O (SHARED 또는 PER_USER) | 브라우저 (`accessUrl`) |
| PWN | 안 됨 | O (PER_USER) | 터미널 `nc {host} {port}` |
| REVERSING, FORENSICS, CRYPTO, MISC | O | 안 됨 | 파일 다운로드 |
| flagType | `STATIC` `DYNAMIC` | 고정 플래그 / 인스턴스마다 바뀌는 플래그 |
| 인스턴스 status | `PENDING` `READY` `FAILED` `EXPIRED` | 3-1 참고 |
| 그룹 role | `LEADER` `MEMBER` | 그룹장 / 멤버 |
| 가입 요청 joinType | `APPLY` `INVITE` | 사용자가 신청 / 그룹장이 초대 |
| 가입 요청 status | `PENDING` `ACCEPTED` `REJECTED` `CANCELED` | 대기 / 수락 / 거절 / 취소 |

---

## 2. 문제 (Challenge)

### API 목록

> **게임 모드 (2026-11-18, Figma "CTF 대시보드 MMO Game v1" G07)**: 문제는 게임(game.nimda.kr) 맵에서 찾고 게임 안 문제 창에서 제출한다.
> CTF 사이트(ctf.nimda.kr)는 **우리 팀이 푼 문제 다시 보기 + 팀 현황 + 순위** 화면이다. 그래서
> - 참가자에게 **전체 문제 목록·개수·코드를 주지 않는다** → `GET /api/ctf/challenges/solved` (우리 팀이 푼 문제)
> - 전체 목록·초안·등록·공개 전환·삭제는 **ROLE_DEV 만** (ADMIN 도 안 됨)
> - 상세는 게임이 안 푼 문제를 열 때도 쓰므로 참가자에게 열려 있다. 응답의 `solvedByMyTeam` 으로 사이트가 "해결함"/"아직 풀지 않은 문제" 화면을 고른다
> - **게임에 나온 문제만 연다 (11-18 결정)**: 참가자(CTF_USER·CTF_MASTER 등 ADMIN·DEV 가 아닌 사람)에게 상세·첨부파일·인스턴스는 **공개 문제이면서 `visible` 게임 물건에 연결된 문제**, 또는 **우리 팀이 이미 푼 문제**만 열린다. 그 밖은 없는 문제와 같은 404. ADMIN 은 공개 문제 전부, DEV 는 초안까지 전부. 게임 물건 기능이 꺼져 있으면(`CTF_GAME_ENABLED=false`) 이 제한은 없다 (공개 문제 전부). 아래 "노출 규칙" 표
> - 순위 비공개 중(6절)에는 해결 목록·상세의 문제별 점수도 참가자에게 `null` (더하면 팀 점수가 드러나므로)
> - 이미 푼 WEB·PWN 문제도 인스턴스를 다시 띄울 수 있다 (3절, 점수 변화 없음)

| 메서드 | 경로 | 권한 | 설명 |
|---|---|---|---|
| GET | `/api/ctf/challenges/solved` | 로그인 (팀 필요, ADMIN·DEV 는 팀 없이도) | **우리 팀이 푼 문제** |
| GET | `/api/ctf/challenges/{code}` | 로그인 (팀 필요, ADMIN·DEV 는 팀 없이도) | 문제 상세 + 우리 팀 해결 여부 |
| GET | `/api/ctf/challenges` | **DEV** | 전체 문제 목록 (초안 포함, 해결 팀 수) |
| POST | `/api/ctf/challenge/{code}/download` | 로그인 | 첨부파일 링크 발급 |
| GET | `/api/ctf/challenge/download/{requestId}` | 로그인 | 첨부파일 링크 발급 결과 조회 |
| POST | `/api/ctf/challenge` | **DEV** | 문제 등록 |
| POST | `/api/ctf/challenge/toggle-public/{id}` | **DEV** | 공개/비공개 전환 |
| DELETE | `/api/ctf/challenge/{id}` | **DEV** | 문제 삭제 |

> 경로 주의: 조회는 `challenges`(복수), 등록·첨부파일은 `challenge`(단수)입니다.

**노출 규칙** (상세 2-2, 첨부파일 2-3, 인스턴스 3-2·3-3 공통)

| 누구 | 열리는 문제 | 그 밖 |
|---|---|---|
| DEV | 전부 (초안 포함) | - |
| ADMIN | 공개 문제 전부 | 404 |
| 참가자 (CTF_USER·CTF_MASTER·그 밖) | 공개 + `visible` 게임 물건에 연결된 문제, 또는 우리 팀이 푼 문제 | 404 |
| 참가자, 게임 물건 기능 꺼짐 | 공개 문제 전부 | 404 |

> 물건을 숨기면(`visible=false`) 그 문제는 아직 안 푼 팀에게 바로 404 가 된다. 이미 푼 팀은 계속 다시 본다.

### 2-0. 우리 팀이 푼 문제

`GET /api/ctf/challenges/solved`

* 현재 대회에서 **팀원 누구든** 푼 문제. 팀이 없는 ADMIN·DEV 는 본인이 푼 문제. 팀이 없는 참가자는 403 (`NO_TEAM`)
* 푼 순서(오름차순)입니다. 카테고리별 묶음은 화면에서 합니다
* **11-18 변경**: 배열이 아니라 `{ total, solved }` 입니다 (대시보드 "팀이 푼 문제 3 / 12", "아직 찾지 못한 문제 9개")

```json
{
  "total": 12,
  "solved": [
    {
      "code": "rev-basic-01",
      "title": "패킹된 바이너리",
      "category": "REVERSING",
      "isolationType": "NONE",
      "hasAttachment": true,
      "awardedPoints": 100,
      "solvedAt": "2026-03-21T12:41:00+09:00",
      "solvedBy": "blue1"
    }
  ]
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| total | number | 참가자가 찾을 수 있는 문제 수 = 공개 + `visible` 게임 물건에 연결된 문제 (물건 기능이 꺼져 있으면 공개 문제 수). 누가 불러도 같은 값. 코드·제목은 주지 않는다 |
| solved | array | 아래 필드의 목록 |
| awardedPoints | number \| null | 이 풀이로 받은 점수. **순위 비공개 중에는 참가자에게 null** (ADMIN·DEV 는 값) |
| solvedAt | string | 푼 시각 (KST, ISO-8601) |
| solvedBy | string | 푼 팀원 닉네임 |

### 2-1. 전체 문제 목록 (DEV)

`GET /api/ctf/challenges` — **ROLE_DEV 만** (그 밖은 403). 문제 관리 화면용

* 초안까지 전부, id 오름차순입니다

```json
[
  {
    "id": 1,
    "code": "web-basic-01",
    "title": "첫 번째 웹 문제",
    "category": "WEB",
    "points": 100,
    "isolationType": "SHARED",
    "hasAttachment": false,
    "isPublic": true,
    "solveCount": 5
  }
]
```

| 필드 | 타입 | 설명 |
|---|---|---|
| code | string | 문제 식별자. 상세·첨부파일·인스턴스 API 경로에 씁니다 |
| hasAttachment | boolean | 받을 파일이 있는지. **정적 문제(REVERSING, FORENSICS, CRYPTO, MISC)이고** true일 때만 [파일 받기] 버튼을 보여줍니다 |
| category | string | `WEB` `PWN`이면 인스턴스 영역을 보여줍니다 (3절) |
| isPublic | boolean | 초안 표시용 |
| solveCount | number | 현재 대회에서 이 문제를 푼 팀 수 |

게임 물건 연결(문제 관리의 "게임 물건 key")은 `GET /api/ctf/game/admin/items` (ADMIN·DEV, `docs/api/CTF 게임 물건 API.md`)의 `challengeCode` 로 화면에서 맞춘다.

### 2-2. 문제 상세

`GET /api/ctf/challenges/{code}`

```json
{
  "id": 1,
  "code": "web-basic-01",
  "title": "첫 번째 웹 문제",
  "description": "문제 설명 본문",
  "category": "WEB",
  "points": 100,
  "flagType": "STATIC",
  "isolationType": "SHARED",
  "hasAttachment": false,
  "isPublic": true,
  "solvedByMyTeam": true,
  "mySolve": {
    "code": "web-basic-01", "title": "첫 번째 웹 문제", "category": "WEB", "isolationType": "SHARED",
    "hasAttachment": false, "awardedPoints": 100, "solvedAt": "2026-03-21T12:41:00+09:00", "solvedBy": "blue1"
  }
}
```

| 필드 | 설명 |
|---|---|
| points | 지금 풀면 받는 점수. **순위 비공개 중 참가자에게는 null** (maxPoint·decreasePoint 와 함께 보면 해결 팀 수가 드러나므로). ADMIN·DEV 는 값 |
| solvedByMyTeam | 우리 팀(팀이 없으면 본인)이 현재 대회에서 풀었는지. CTF 사이트는 false 면 "아직 풀지 않은 문제" 화면 |
| mySolve | 풀었으면 그 기록 (2-0 과 같은 모양), 아니면 null |

| 상태 코드 | 경우 |
|---|---|
| 404 | 없는 문제, 또는 위 "노출 규칙" 밖의 문제 (초안은 ADMIN 도 404, 게임에 안 나온 공개 문제는 참가자에게 404) |

### 2-3. 첨부파일 링크 발급

`POST /api/ctf/challenge/{code}/download` (body 없음)

서버가 최대 3초 기다립니다. 그 안에 링크가 나오면 바로 주고, 아니면 `requestId`를 줍니다.

**200 — 바로 발급됨**

```json
{
  "status": "READY",
  "url": "https://...(서명된 다운로드 링크)",
  "expiresAt": "2026-10-29T02:00:00Z",
  "cached": true
}
```

**202 — 아직 발급 중** → `requestId`로 2-4를 폴링합니다.

```json
{ "status": "PENDING", "requestId": "0e3f7c1a-..." }
```

| 상태 코드 | 경우 |
|---|---|
| 400 | 인스턴스 문제(WEB, PWN). 이 문제들은 첨부파일 받기를 지원하지 않습니다 |
| 404 | 없는 문제, "노출 규칙" 밖의 문제, 첨부파일이 없는 문제 |
| 503 | 발급 실패. 공통 형식이라 `message`에 사유가 있습니다 |

> `url`은 서명된 링크라 그 자체가 권한입니다. 받으면 바로 이동하고, 화면에 오래 두거나 로그에 남기지 않습니다. `expiresAt`이 지나면 다시 발급받습니다.

### 2-4. 첨부파일 발급 결과 조회

`GET /api/ctf/challenge/download/{requestId}`

* 아직이면 `{ "status": "PENDING", "requestId": "..." }` (200)
* 끝나면 2-3의 200 응답과 같습니다 (`cached: false`)
* 403: 본인이 요청한 것이 아님 / 503: 발급 실패

1초 간격으로 몇 번 폴링하면 충분합니다.

### 2-5. 문제 등록 (DEV)

`POST /api/ctf/challenge` — `multipart/form-data`

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| code | string | O | 영문, 숫자, `-`, `_`만. 1~50자. 중복이면 409 |
| title | string | O | |
| description | string | O | |
| category | enum | O | 1-4 참고 |
| points | number | | 기본 100 |
| flagType | enum | | 기본 `STATIC` |
| flag | string | STATIC이면 O | 원문은 저장하지 않고 해시만 저장합니다 |
| isolationType | enum | | 기본 `NONE`. WEB·PWN은 꼭 지정해야 합니다 (아래 표) |
| zipFile | file | | 문제 파일 묶음 (zip). 최대 10MB |
| isPublic | boolean | O | false면 초안 |

허용되는 조합 (이 밖의 조합은 400):

| category | isolationType | flagType |
|---|---|---|
| WEB | `SHARED`, `PER_USER` | SHARED면 `STATIC`만 |
| PWN | `PER_USER` | `STATIC`, `DYNAMIC` |
| REVERSING, FORENSICS, CRYPTO, MISC | `NONE` | `STATIC` |

응답 `data`: 만든 문제의 `id` (number)

| 상태 코드 | 경우 |
|---|---|
| 400 | 필수값 누락, 형식 오류, 허용되지 않는 조합 |
| 409 | code 중복 |

### 2-6. 공개/비공개 전환 (DEV)

`POST /api/ctf/challenge/toggle-public/{id}`

응답 `data`: 바뀐 뒤의 공개 여부 (boolean). 404: 없는 문제.

### 2-7. 문제 삭제 (DEV)

`DELETE /api/ctf/challenge/{id}`

응답: `{ "success": true, "message": "삭제가 완료되었습니다." }`

---

## 3. 문제 인스턴스 (WEB, PWN 문제)

category가 `WEB` 또는 `PWN`인 문제는 참가자가 버튼을 눌러 문제 서버(인스턴스)를 띄우고, 받은 접속 정보로 붙어서 풉니다.

| category | READY일 때 받는 것 | 참가자가 하는 일 |
|---|---|---|
| WEB | `accessUrl` | [접속하기] → 새 탭으로 열기 |
| PWN | `host`, `port` | 터미널에서 `nc {host} {port}` (화면에 복사 버튼을 두면 좋습니다) |

### API 목록

| 메서드 | 경로 | 권한 | 설명 |
|---|---|---|---|
| POST | `/api/ctf/instance/{code}` | 로그인 | 인스턴스 시작 (이미 있으면 그대로 돌려줌) |
| GET | `/api/ctf/instance/{code}` | 로그인 | 이 문제의 내 인스턴스 |
| GET | `/api/ctf/instance/status/{uuid}` | 로그인 | 인스턴스 상태 (폴링) |
| GET | `/api/ctf/server-status` | 로그인 | CTF 서버가 인스턴스 요청을 받을 수 있는지 |

### 3-0. CTF 서버 상태

`GET /api/ctf/server-status`

```json
{ "available": true }
```

* `false`면 [인스턴스 시작] 버튼 대신 "CTF 서버가 아직 작동중이지 않습니다."를 보여줍니다.
* 이미 띄운 인스턴스(PENDING, READY)는 이 값과 상관없이 그대로 보여줍니다.
* CTF 서버(provisioner)가 agent까지 살아 있을 때 10초마다 TTL 30초로 갱신하는 키(`ctf:server:heartbeat`)를 봅니다. 꺼진 뒤 최대 30초 늦게 반영됩니다. 설계는 CTF 레포 ADR-0022입니다.

### 3-1. 상태값과 화면

| status | 의미 | 함께 오는 필드 | 화면 |
|---|---|---|---|
| (없음) | 아직 시작 안 함 | `hasInstance: false` | [인스턴스 시작] 버튼 |
| `PENDING` | 만드는 중 | `uuid` | "준비 중..." + 1~2초마다 3-4 폴링 |
| `READY` | 접속 가능 | `uuid` `expiresAt` + WEB은 `accessUrl`, PWN은 `host` `port` | WEB: [접속하기] / PWN: `nc host port` + 복사 버튼. 둘 다 남은 시간 |
| `FAILED` | 만들기 실패 | `uuid` `message` | 실패 사유 + [다시 시도] |
| `EXPIRED` | 만료됨 | `uuid` | "만료되었습니다" + [다시 시작] |

* [다시 시도], [다시 시작]은 3-2(시작)를 다시 부르면 됩니다.
* 인스턴스는 약 1시간 유지됩니다. `expiresAt`(UTC)으로 남은 시간을 보여줍니다.

### 3-2. 인스턴스 시작

`POST /api/ctf/instance/{code}` (body 없음)

같은 문제로 이미 만들고 있거나 떠 있으면 **새로 만들지 않고 같은 인스턴스를 돌려줍니다.** 두 번 눌러도 안전합니다.

```json
{ "uuid": "0e3f7c1a-...", "status": "PENDING" }
```

이미 떠 있으면:

```json
{
  "uuid": "0e3f7c1a-...",
  "status": "READY",
  "accessUrl": "https://inst-0e3f7c1a-....nimda.kr",
  "expiresAt": "2026-10-29T02:00:00Z"
}
```

| 상태 코드 | 경우 |
|---|---|
| 400 | 인스턴스 문제(WEB, PWN)가 아님, 또는 문제 설정이 잘못됨 |
| 404 | 없는 문제, 2절 "노출 규칙" 밖의 문제 (이미 푼 문제는 다시 띄울 수 있다) |
| 503 | CTF 서버가 꺼져 있음. `message`: "CTF 서버가 아직 작동중이지 않습니다." (3-0이 `false`일 때) |

### 3-3. 이 문제의 내 인스턴스

`GET /api/ctf/instance/{code}`

문제 상세 페이지에 들어올 때 한 번 부릅니다. 새로고침하거나 다른 기기에서 들어와도 이어서 볼 수 있습니다.

```json
{ "hasInstance": false }
```

```json
{
  "hasInstance": true,
  "uuid": "0e3f7c1a-...",
  "status": "READY",
  "accessUrl": "https://inst-0e3f7c1a-....nimda.kr",
  "expiresAt": "2026-10-29T02:00:00Z"
}
```

`status`가 `PENDING`이면 3-4 폴링을 시작합니다.

### 3-4. 인스턴스 상태 (폴링)

`GET /api/ctf/instance/status/{uuid}`

```json
{ "uuid": "...", "status": "PENDING" }
```

```json
{ "uuid": "...", "status": "READY", "accessUrl": "https://inst-....nimda.kr", "expiresAt": "2026-10-29T02:00:00Z" }
```

PWN 문제일 때:

```json
{ "uuid": "...", "status": "READY", "host": "43.200.36.32", "port": 30001, "expiresAt": "2026-10-29T02:00:00Z" }
```

```json
{ "uuid": "...", "status": "FAILED", "message": "컨테이너 실행 실패: ..." }
```

```json
{ "uuid": "...", "status": "EXPIRED" }
```

| 상태 코드 | 경우 |
|---|---|
| 403 | 본인 인스턴스가 아님 |

폴링 규칙:

* 1~2초 간격
* `PENDING`이 아니면 멈춥니다
* 페이지를 떠나면 멈춥니다
* 3분 넘게 `PENDING`이면 멈추고 "잠시 후 다시 시도" 안내

### 3-5. 접속

**WEB (`accessUrl`)**

* `accessUrl`은 **새 탭**으로 엽니다. 문제 페이지는 참가자가 공격하는 대상이라 플랫폼 화면 안(iframe)에 넣지 않습니다.
* 같은 브라우저에 로그인돼 있어야 열립니다. 다른 계정이면 403, 로그아웃 상태면 401 페이지가 나옵니다.
* 인스턴스가 아직 준비 안 됐거나 만료됐으면 503 페이지가 나옵니다.

**PWN (`host`, `port`)**

* 브라우저로 여는 주소가 아닙니다. 화면에 `nc {host} {port}` 명령을 보여주고 복사 버튼을 둡니다.
* 3-3, 3-4 응답의 `host`, `port`를 그대로 씁니다.

---

## 4. 그룹

> 그룹 API는 대부분 **모든 오류를 500 + 일반 메시지**로 돌려줍니다 (예: 권한 없음, 정원 초과도 500). 상태 코드로 원인을 구분할 수 없으니, 실패 시 `message`를 보여주고 목록을 다시 불러오는 식으로 처리합니다. 원인별 코드가 따로 오는 API는 표에 적었습니다.

### API 목록

| 메서드 | 경로 | 권한 | 설명 |
|---|---|---|---|
| GET | `/api/groups` | 로그인 | 그룹 목록 / 검색 |
| GET | `/api/groups/{groupId}` | 로그인 | 그룹 상세 (멤버 포함) |
| POST | `/api/groups` | ADMIN | 그룹 생성 |
| PATCH | `/api/groups/{groupId}` | 그룹장 | 그룹 수정 |
| DELETE | `/api/groups/{groupId}` | 그룹장 | 그룹 삭제 |
| GET | `/api/groups/{groupId}/members` | 로그인 | 멤버 목록 |
| DELETE | `/api/groups/{groupId}/members/me` | 멤버 | 그룹 탈퇴 |
| DELETE | `/api/groups/{groupId}/members/{targetUserId}` | 그룹장 | 멤버 강퇴 |
| PATCH | `/api/groups/{groupId}/members/{targetUserId}/leader` | 그룹장 | 그룹장 위임 |
| POST | `/api/groups/{groupId}/join-requests/apply` | 로그인 | 가입 신청 |
| POST | `/api/groups/{groupId}/join-requests/invite?inviteeId={userId}` | 그룹장 | 초대 |
| GET | `/api/groups/{groupId}/join-requests` | 그룹장 | 대기 중인 요청 목록 |
| POST | `/api/groups/{groupId}/join-requests/{requestId}/accept` | 그룹장 또는 초대받은 사람 | 수락 |
| POST | `/api/groups/{groupId}/join-requests/{requestId}/reject` | 그룹장 또는 초대받은 사람 | 거절 |
| DELETE | `/api/groups/{groupId}/join-requests/{requestId}` | 요청한 본인 | 취소 |

### 4-1. 그룹 목록 / 검색

`GET /api/groups` · `GET /api/groups?keyword=검색어`

```json
[
  { "id": 1, "name": "nimda-A", "capacity": 15, "currentMemberCount": 3, "description": "그룹 설명" }
]
```

### 4-2. 그룹 상세

`GET /api/groups/{groupId}`

```json
{
  "id": 1,
  "name": "nimda-A",
  "capacity": 15,
  "currentMemberCount": 3,
  "description": "그룹 설명",
  "members": [
    { "id": 10, "userId": 1, "nickname": "admin", "role": "LEADER" },
    { "id": 11, "userId": 2, "nickname": "shareA", "role": "MEMBER" }
  ]
}
```

`members[].id`는 멤버십 id이고, 강퇴·위임에는 `userId`를 씁니다.

### 4-3. 그룹 생성 (ADMIN) / 수정 (그룹장)

`POST /api/groups` · `PATCH /api/groups/{groupId}` — JSON body

```json
{ "name": "nimda-A", "capacity": 15, "description": "그룹 설명" }
```

| 필드 | 필수 | 규칙 |
|---|---|---|
| name | O | 50자 이내, 다른 그룹과 중복 불가 |
| capacity | O | 1~15 |
| description | | 200자 이내 |

* 생성: 201, `data`는 4-2와 같은 그룹 상세. 만든 사람이 자동으로 그룹장(`LEADER`)이 됩니다.
* 수정: 200, `data` 없음.

### 4-4. 그룹 삭제 (그룹장)

`DELETE /api/groups/{groupId}` — 멤버도 함께 삭제됩니다.

### 4-5. 멤버 목록

`GET /api/groups/{groupId}/members` — 4-2의 `members`와 같은 배열.

### 4-6. 탈퇴 / 강퇴 / 위임

| API | 규칙 |
|---|---|
| `DELETE .../members/me` | 그룹장은 위임한 뒤에만 탈퇴할 수 있습니다 |
| `DELETE .../members/{targetUserId}` | 그룹장만. 본인은 강퇴할 수 없습니다 |
| `PATCH .../members/{targetUserId}/leader` | 그룹장만. 본인에게는 위임할 수 없습니다. 위임하면 기존 그룹장은 `MEMBER`가 됩니다 |

위임 API만 원인별 코드가 옵니다: 404(없는 그룹·멤버), 403(그룹장 아님), 400(본인에게 위임 등). `message`에 사유가 있습니다.

### 4-7. 가입 신청 / 초대

* 신청: `POST .../join-requests/apply` (body 없음)
* 초대: `POST .../join-requests/invite?inviteeId={userId}` — **body가 아니라 쿼리 파라미터**입니다

둘 다 201, `data`:

```json
{ "requestId": 5 }
```

실패하는 경우 (모두 500 + 일반 메시지):

* 이미 그 그룹 멤버임
* 같은 그룹에 처리 안 된 요청이 이미 있음
* 초대: 그룹장이 아님

### 4-8. 대기 중인 요청 목록 (그룹장)

`GET /api/groups/{groupId}/join-requests`

```json
[
  {
    "id": 5,
    "groupId": 1,
    "groupName": "nimda-A",
    "userId": 3,
    "nickname": "newbie",
    "joinType": "APPLY",
    "status": "PENDING"
  }
]
```

원인별 코드가 옵니다: 404(없는 그룹), 403(그룹장 아님).

### 4-9. 수락 / 거절 / 취소

| API | 누가 |
|---|---|
| `POST .../{requestId}/accept` | `APPLY`는 그룹장, `INVITE`는 초대받은 사람 |
| `POST .../{requestId}/reject` | 위와 같음 |
| `DELETE .../{requestId}` | 요청한 본인 |

수락이 실패하는 경우: 이미 처리된 요청, 이미 멤버, 정원 초과.

---

## 5. 아직 없는 기능 (화면 설계 시 참고)

| 기능 | 상태 |
|---|---|
| 플래그 제출·채점 | API 없음. 설계 중 (`docs/issue/ctf-flag-submit-issue.md`) |
| CTF 점수판 | 플래그 제출 API가 생긴 뒤 |
| 인스턴스 연장·직접 종료 | API 없음 |
| 내가 받은 초대 목록 | 서버 로직은 있으나 API로 열려 있지 않음 |
| 문제 수정 | API 없음 (삭제 후 다시 등록). 문제 관리 화면(G06)의 "수정" 은 아직 연결할 API 가 없다 |

> 위 표의 "플래그 제출·채점", "CTF 점수판" 은 그 뒤 생겼다 (`POST /api/ctf/challenge/{challengeId}`, `GET /api/ctf/scoreboard`). 점수판은 6절.

---

## 6. 점수판·일정 — 순위 비공개 (게임 모드)

대회 설정의 **순위 비공개**(예전 이름 "스코어보드 고정", 필드 `freezeScoreboard`·`freezeMinutesBeforeEnd`)를 켜면
**종료 N분 전부터 종료까지** 참가자에게 순위를 통째로 감춘다. 모든 팀의 순위·점수·시상대·팀 기여, **우리 팀 점수·등수까지 전부.**
해결한 문제 목록은 계속 보이지만 문제별 점수는 `null` 이다 (2-0). 종료와 함께 최종 순위가 공개된다. **ADMIN·DEV 는 언제나 전체.**
(ROLE_CTF_MASTER 는 순위에서는 참가자와 같다.)

| 설정 | 값 |
|---|---|
| `freezeScoreboard` | 켜기/끄기. 새 대회 기본 꺼짐 |
| `freezeMinutesBeforeEnd` | 종료 몇 분 전부터 (1~600, **기본 120**) |

### 6-1. 일정

`GET /api/ctf/schedule` — 화면의 "곧 순위가 가려집니다" 카운트다운에 쓴다

```json
{
  "contestId": 1, "name": "NIMDACON 2026",
  "startsAt": "2026-03-21T10:00:00+09:00", "endsAt": "2026-03-22T18:00:00+09:00",
  "status": "RUNNING", "serverTime": "2026-03-22T14:35:52+09:00",
  "scoreboardHiddenFrom": "2026-03-22T16:00:00+09:00",
  "scoreboardHidden": false
}
```

| 필드 | 설명 |
|---|---|
| scoreboardHiddenFrom | 비공개가 시작되는 시각 (= endsAt - N분). 비공개를 껐거나 대회가 없으면 null |
| scoreboardHidden | 지금 참가자에게 가려져 있는지 (`scoreboardHiddenFrom ≤ serverTime < endsAt`) |

### 6-2. 점수판

`GET /api/ctf/scoreboard?page=0&size=50`, `GET /api/ctf/scoreboard/me`

비공개 중인 참가자에게는 두 경로 모두 **이것만** 준다 (평소 응답 모양과 다르므로 `hidden` 으로 먼저 가른다):

```json
{ "hidden": true, "hiddenFrom": "2026-03-22T16:00:00+09:00", "revealAt": "2026-03-22T18:00:00+09:00" }
```

평소(비공개 전·종료 뒤·비공개 꺼짐) 와 ADMIN·DEV 는 기존 응답 그대로다 (`hidden` 필드 없음).
