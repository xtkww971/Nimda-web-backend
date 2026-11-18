# CTF 게임 물건 ↔ 문제 연결 API

> 상태: **백엔드 구현·로컬 확인 끝 (2026-11-18, 브랜치 `feat/ctf-game-mode-api`, 아직 배포 전)**. Figma·프론트(`nimda-ctf-frontend`)는 이 문서대로 진행한다.
> 확인: 단위 테스트 8개(`GameItemLinkServiceTest`), 로컬 실제 DB 로 2장 API 전부 호출(만들기 201, 이름 없음 400, 한 문제에 여러 개, 요약, 해제, 다시 해제 404, 없는 문제 404, 잘못된 key 400, 참가자 403). 409(다른 문제에 연결됨)는 로컬에 문제가 하나뿐이라 단위 테스트로만 확인.
> nginx `objects.json` CORS 는 임시 nginx 로 헤더까지 확인 (게임이 꺼져 있으면 503 + CORS 없음 → 화면은 key 직접 입력).
> 코드: `com.nimda.ctfgame` 의 `GameItemLinkController`·`GameItemLinkService`, nginx `game.nimda.kr` 블록의 `location = /ctf/objects.json`
> 바꿀 것이 생기면 이 문서를 먼저 고치고 세 쪽에 알린다. 끝의 "정할 것" 은 기본안으로 진행하고, 바뀌면 문서를 고친다.
> 물건 API 전체 명세: [CTF 게임 물건 API.md](CTF%20게임%20물건%20API.md) · 맵 오브젝트 계약: 게임 저장소 `docs/MAP_OBJECTS.md`

## 1. 무엇을 하는가

운영진(DEV)이 **CTF 사이트 문제 관리 화면(G06)** 에서 문제마다 "이 문제를 여는 게임 물건" 을 고른다.

```
게임 맵 오브젝트 (client/ctf/objects.json, AI 가 만듦)      백엔드 게임 물건 (ctf_game_items, 사람이 넣음)
  key: old-arcade, zone 1, hint "Stage1 오른쪽 벽 낡은 게임기"  ──key──▶  key: old-arcade → 문제 rev-crack-me, 이름, 이야기, 공개
```

- 오브젝트(위치·그림)는 게임 저장소 담당, **연결(문제·이름·이야기·공개)은 이 API 로 데이터만** 넣는다 (D12: 백엔드 코드·배포 없음).
- 지금도 `POST/PUT /api/ctf/game/admin/items` 의 `challengeCode` 로 연결할 수 있다. 이 문서는 **문제 쪽에서 key 만으로** 연결·해제하는 편의 API 를 더한다.

### 규칙

| 규칙 | 내용 |
|---|---|
| 개수 | 물건 하나는 **문제 하나까지**. 문제 하나에는 물건 **여러 개** (맵 여러 곳에서 같은 문제를 열 수 있다) |
| 다른 문제에 이미 연결된 key | 기본은 **409** (`이미 다른 문제에 연결된 물건입니다`). `moveFromOtherChallenge: true` 로 보내면 옮긴다 |
| 해제 | 물건은 남기고 연결만 끊는다 (`challengeCode: null`). 게임에서는 장식이 된다. 물건 자체 삭제는 기존 `DELETE /api/ctf/game/admin/items/{id}` |
| 초안 문제 | 연결할 수 있다. 문제가 공개되기 전에는 참가자에게 `challengeCode: null` 로 간다 (기존 규칙) |
| 공개 | 물건 `visible` 기본 `false`. 연결과 함께 켤지 화면에서 정한다 |
| key 형식 | `^[a-z0-9][a-z0-9-]{0,49}$`. **`objects.json` 에 있는 key 를 고르는 것이 기본**, 없는 key 도 넣을 수 있다 (오브젝트가 나중에 생기는 경우) |

## 2. API

권한: **ADMIN·DEV** (기존 `/api/ctf/game/admin/**` 와 같다). 응답은 공용 `ApiResponse` `{success, message, data}`.

| 메서드 | 경로 | 설명 |
|---|---|---|
| GET | `/api/ctf/game/admin/links` | 문제별 연결된 물건 (문제 관리 표의 "게임 물건 key" 칸) |
| GET | `/api/ctf/game/admin/challenges/{code}/items` | 한 문제에 연결된 물건들 (연결 패널) |
| PUT | `/api/ctf/game/admin/challenges/{code}/items/{key}` | 이 key 의 물건을 이 문제에 연결. 물건이 없으면 만든다 |
| DELETE | `/api/ctf/game/admin/challenges/{code}/items/{key}` | 연결 해제 (물건은 남는다) |

### 2-1. 문제별 연결 요약

`GET /api/ctf/game/admin/links`

```json
{
  "success": true,
  "data": {
    "links": [
      { "challengeCode": "rev-crack-me", "items": [ { "id": 1, "key": "old-arcade", "name": "낡은 게임기", "visible": true } ] },
      { "challengeCode": "web-basic-01", "items": [] }
    ],
    "unlinkedItems": [ { "id": 3, "key": "lobby-poster", "name": "동방 포스터", "visible": true } ]
  }
}
```

- `links` 는 **모든 문제**(초안 포함, id 순)를 담는다. 연결이 없으면 `items: []`
- `unlinkedItems`: 어느 문제에도 연결되지 않은 물건 (장식)
- 문제 관리 화면은 `GET /api/ctf/challenges` (DEV, 해결 팀 수 포함)와 이것을 `challengeCode` 로 합친다

### 2-2. 한 문제의 물건

`GET /api/ctf/game/admin/challenges/{code}/items`

```json
{ "success": true, "data": [
  { "id": 1, "key": "old-arcade", "name": "낡은 게임기", "description": "이야기…", "challengeCode": "rev-crack-me",
    "challengeTitle": "회장님 최애 게임", "sortOrder": 1, "visible": true, "updatedAt": "2026-11-18T11:37:12" }
] }
```

항목 모양은 기존 관리 응답(`AdminGameItemResponse`)과 같다. 없는 문제 코드면 404.

### 2-3. 연결 (만들거나 고치기)

`PUT /api/ctf/game/admin/challenges/{code}/items/{key}`

```json
{ "name": "낡은 게임기", "description": "회장님이 아끼던 게임팩이 꽂혀 있다.", "visible": true, "sortOrder": 1,
  "moveFromOtherChallenge": false }
```

| 필드 | 필수 | 설명 |
|---|---|---|
| `name` | 물건이 없을 때 O | 100자 이하. 물건이 이미 있으면 비워 두면 그대로 둔다 |
| `description` | | 5000자 이하. 비워 두면 그대로 (새로 만들 때는 없음) |
| `visible` | | 비워 두면 그대로 (새로 만들 때 `false`) |
| `sortOrder` | | 비워 두면 그대로 (새로 만들 때 0) |
| `moveFromOtherChallenge` | | `true` 면 다른 문제에 연결돼 있어도 이 문제로 옮긴다. 기본 `false` → 409 |

응답: `200` 고친 물건 / `201` 새로 만든 물건 (항목 모양은 2-2).

### 2-4. 연결 해제

`DELETE /api/ctf/game/admin/challenges/{code}/items/{key}` → `200`, 물건의 `challengeCode` 가 `null` 이 된다.
그 key 가 이 문제에 연결돼 있지 않으면 404.

### 오류

| 상태 | 메시지 | 언제 |
|---|---|---|
| 400 | 검사 문구 | key 형식, 길이 |
| 400 | `새 물건은 이름이 필요합니다.` | 없는 key 를 연결하면서 `name` 이 없음 |
| 404 | `연결할 문제를 찾을 수 없습니다.` | 없는 문제 코드 |
| 404 | `이 문제에 연결된 물건이 아닙니다.` | 해제할 때 key 가 이 문제에 없음 |
| 409 | `이미 다른 문제에 연결된 물건입니다.` | 다른 문제에 연결된 key, `moveFromOtherChallenge` 없음. `data: { "challengeCode": "다른 문제" }` 로 어디 연결됐는지 준다 |
| 403 | | ADMIN·DEV 아님 |
| 404 | | `CTF_GAME_ENABLED=false` |

## 3. 맵 오브젝트 목록 (key 고르기)

백엔드는 맵을 모른다 (D12). 연결 화면의 key 목록은 **프론트가 게임에서 직접** 받는다.

`GET https://game.nimda.kr/ctf/objects.json` (정적 파일, 게임 저장소 `client/ctf/objects.json`)

```json
{ "version": 1, "objects": [ { "key": "old-arcade", "zone": 1, "x": 600, "y": 380, "w": 40, "h": 56, "hint": "Stage1 오른쪽 벽, 낡은 아케이드 게임기" } ] }
```

- 화면은 `key` + `hint` + 구역(0 동방 로비, 1 Stage1, 2 Stage2)을 보여 주고, 2-1 의 연결 상태(연결 없음 / 이 문제 / 다른 문제)를 key 로 붙인다
- **받지 못하면**(게임이 꺼져 있어 503 등) key 를 직접 입력하게 한다
- CTF 사이트(`https://ctf.nimda.kr`)에서 다른 출처라 **CORS 가 필요**하다 → nginx `game.nimda.kr` 블록에 `/ctf/objects.json` 만 `Access-Control-Allow-Origin` 추가 (백엔드 저장소 nginx, 아래 할 일 B3)

## 4. 화면 (Figma 에 그릴 것)

G06 문제 관리에 붙인다.

| 화면 | 내용 |
|---|---|
| 표 "게임 물건 key" 칸 | 2-1 의 key 들. 없으면 "— (연결 없음)" (이미 시안에 있음). 누르면 연결 패널 |
| 연결 패널 (문제 하나) | 위: 연결된 물건 목록 (key·이름·공개·이야기 일부, [해제]) / 아래: 맵 오브젝트 고르기 (key·hint·구역·상태). 고르면 이름·이야기·공개 입력 → [연결] |
| 다른 문제에 연결된 key 를 골랐을 때 | "rev-01 에 연결돼 있습니다. 이 문제로 옮길까요?" → 확인하면 `moveFromOtherChallenge: true` |
| 오브젝트 목록을 못 받을 때 | key 직접 입력 칸 |
| 장식 물건 | 2-1 의 `unlinkedItems` 를 따로 보여 줄지 (선택) |

## 5. 할 일

| # | 쪽 | 할 일 |
|---|---|---|
| B1 | 백엔드 | 2장 API (`com.nimda.ctfgame`, 새 테이블·마이그레이션 없음) + 테스트 |
| B2 | 백엔드 | 이 문서를 확정본으로, `CTF 게임 물건 API.md` 에 링크 |
| B3 | 백엔드(nginx) | `game.nimda.kr` 의 `/ctf/objects.json` 에 CORS (`https://ctf.nimda.kr`) — nginx 변경이라 배포 때 nginx 재시작 1회 (RB-0002) |
| D1 | Figma | 4장 화면 |
| F1 | 프론트 | 문제 관리 표 + 연결 패널 (`/api/ctf/challenges` + 2-1 + `objects.json`) |

## 6. 정할 것 (기본안으로 진행)

| # | 질문 | 기본안 |
|---|---|---|
| Q1 | 문제 하나에 물건 여러 개를 허용할지 | 허용 |
| Q2 | 다른 문제에 연결된 key 를 고르면 | 409 + 확인 뒤 옮기기 |
| Q3 | 해제하면 물건을 지울지 | 지우지 않고 연결만 끊음 (장식) |
| Q4 | 이 화면을 쓸 수 있는 역할 | ADMIN·DEV (물건 관리 API 와 같게). 문제 관리 화면 자체는 DEV 만이므로 실제로는 DEV |
| Q5 | 연결 패널에서 이름·이야기까지 고칠지 | 고친다 (물건 정보 입력을 한 화면에서) |
