# CTF 게임 물건(아이템) API

CTF 게임(`https://game.nimda.kr`, 저장소 `Nimda-Security/Nimda-ctf-game`)의 맵 위에 놓는 **물건**을 다룬다.
참가자가 물건에 다가가 상호작용하면 연결된 **문제 팝업**이 뜨고, 팀이 그 문제를 풀면 물건을 **가진** 것으로 보고 **이야기**를 보여 준다.

- 코드: `backend-spring/src/main/java/com/nimda/ctfgame/` (이벤트용, 이 패키지에만 모여 있음)
- DB: `ctf_game_items` (Flyway `V41__Create_ctf_game_items.sql`)
- 켜고 끄기: `CTF_GAME_ENABLED` (docker-compose, 기본 `true`). `false` 면 아래 API 가 모두 **404**

```
com/nimda/ctfgame/          (com.nimda.cite 와 따로 둔 최상위 패키지. 안쪽은 다른 domain 과 같은 구성)
├── controller/  GameItemController (참가자), AdminGameItemController (관리)
├── dto/         GameItemResponse, AdminGameItemResponse, GameItemRequest
├── entity/      GameItem
├── exception/   GameItemException, GameItemExceptionHandler (이 패키지 컨트롤러에만 적용)
├── repository/  GameItemRepository (소유 계산용 solves 조회 포함)
├── service/     GameItemService, SolvedChallenges (소유 기준), TeamSolvedChallenges (팀 공유 구현)
└── package-info.java   이벤트 끄기·지우기 절차
테스트: src/test/java/com/nimda/ctfgame/service/GameItemServiceTest
```

## 규칙

| 항목 | 규칙 |
|---|---|
| 소유 | **팀 공유**. 현재 대회에서 팀원 누구라도 연결 문제를 풀었으면 팀 전체가 가진다. 저장하지 않고 `solves` 로 계산한다. 팀이 없으면 본인 풀이만 본다 |
| 이야기 공개 | **가진 뒤에만**. 못 가진 물건은 `description: null` (서버가 보내지 않음) |
| 공개 전 문제 | 연결 문제가 비공개(초안)면 참가자에게 `challengeCode: null`. ADMIN·DEV 는 코드를 받는다 |
| 숨긴 물건 | `visible: false` 면 참가자 목록에 없다 (관리 목록에는 있다) |
| 소비 | 없다. 틀려도 잃지 않는다 (기획안) |

## 참가자(게임) API

### `GET /api/ctf/game/items`

권한: `ROLE_CTF_USER`, `ROLE_CTF_MASTER`, `ROLE_ADMIN`, `ROLE_DEV` (게임 서버 입장 역할과 같다). 그 밖은 403.
인증: `Authorization` 쿠키 (게임은 `https://nimda.kr/api/...` 로 부른다, `credentials: 'include'`).

```json
{
  "success": true,
  "data": [
    {
      "key": "old-arcade",
      "name": "낡은 게임기",
      "zone": 1, "x": 600, "y": 380,
      "sortOrder": 1,
      "challengeCode": "rev-crack-me",
      "challengeTitle": "회장님 최애 게임",
      "owned": true,
      "description": "회장님이 아끼던 게임팩이 꽂혀 있다."
    },
    {
      "key": "poster", "name": "포스터",
      "zone": 0, "x": 100, "y": 100, "sortOrder": 0,
      "challengeCode": null, "challengeTitle": null,
      "owned": false, "description": null
    }
  ]
}
```

| 필드 | 뜻 |
|---|---|
| `key` | 물건 고유 키. 게임이 물건을 구분한다 (바뀌지 않음) |
| `zone` | 게임 구역 번호: `0` 동방 로비, `1` Stage1, `2` Stage2 |
| `x`, `y` | 구역 안 좌표 (px). 물건의 바닥 가운데 |
| `challengeCode` | 상호작용하면 열 문제 → `GET /api/ctf/challenges/{code}` (상세), 파일 받기·플래그 제출은 기존 CTF API |
| `owned` | 팀이 연결 문제를 풀었는지. 문제가 없는 물건은 늘 `false` |
| `description` | 이야기. `owned` 일 때만 값이 있다 |

정렬: `sortOrder` 오름차순, 같으면 만든 순.

## 관리 API

권한: `ROLE_ADMIN`, `ROLE_DEV` (CTF 문제 관리와 같다). 그 밖은 403.

| 메서드 | 경로 | 설명 |
|---|---|---|
| GET | `/api/ctf/game/admin/items` | 전부 (숨긴 것, 이야기 포함) |
| POST | `/api/ctf/game/admin/items` | 등록 → 201 |
| PUT | `/api/ctf/game/admin/items/{id}` | 수정. `key` 는 바꿀 수 없다 |
| DELETE | `/api/ctf/game/admin/items/{id}` | 삭제 |

요청 본문 (등록·수정 같음):

```json
{
  "key": "old-arcade",
  "name": "낡은 게임기",
  "description": "회장님이 아끼던 게임팩이 꽂혀 있다.",
  "challengeCode": "rev-crack-me",
  "zone": 1, "x": 600, "y": 380,
  "sortOrder": 1,
  "visible": true
}
```

| 필드 | 필수 | 검사 |
|---|---|---|
| `key` | O | `^[a-z0-9][a-z0-9-]{0,49}$` |
| `name` | O | 100자 이하 (앞뒤 공백 제거) |
| `description` | | 5000자 이하. 빈 문자열은 `null` 로 저장 |
| `challengeCode` | | 있는 문제 코드. 비우면 연결 없음 |
| `zone` | O | 0~50 |
| `x`, `y` | O | 0~10000 |
| `sortOrder` | | -1000~1000, 기본 0 |
| `visible` | | 기본 `false` (만든 직후엔 참가자에게 안 보임) |

관리 응답 항목: `id, key, name, description, challengeCode, challengeTitle, zone, x, y, sortOrder, visible, updatedAt`.

## 오류

응답은 공용 `ApiResponse` 실패 모양 `{"success": false, "message": "..."}`.

| 상태 | 메시지 | 언제 |
|---|---|---|
| 400 | `잘못된 요청입니다.` 또는 검사 문구 | 본문 검사 실패 (`key` 형식 등) |
| 400 | `연결할 문제를 찾을 수 없습니다.` | 없는 `challengeCode` |
| 400 | `key 는 바꿀 수 없습니다. 지우고 다시 만들어 주세요.` | 수정 때 `key` 가 다름 |
| 403 | | 역할 없음 / 로그인 안 함 |
| 404 | `존재하지 않는 게임 물건입니다.` | 없는 `id` |
| 404 | | `CTF_GAME_ENABLED=false` |
| 409 | `이미 있는 key 입니다.` | 등록 때 `key` 중복 |

## 이벤트 수명 (버전 관리)

| 단계 | 할 일 |
|---|---|
| 지금 | V41 이 테이블을 만든다. V40 은 역할 추가(`feat/ctf-roles`, #35)가 쓴다. `out-of-order: true` 라 어느 쪽이 먼저 병합돼도 적용된다 |
| 다음 마이그레이션 | **V42 부터** 쓴다 (V40·V41 은 예약됨) |
| 이벤트 끝 | 서버 `.env` 에 `CTF_GAME_ENABLED=false` → 다음 배포부터 API 404. 데이터는 남는다 |
| 정리 | `com.nimda.ctfgame` 패키지·테스트 삭제, NimdaCiteApplication 스캔 범위의 `"com.nimda.ctfgame"` 3곳, SecurityConfig 의 `/api/ctf/game/**` 두 줄, application.yml 의 `nimda.ctf-game`, compose 의 `CTF_GAME_ENABLED` 정리. DB 는 **새 마이그레이션** `V<다음>__Drop_ctf_game_items.sql` (`DROP TABLE IF EXISTS ctf_game_items;`). 이미 적용된 V41 파일은 고치거나 지우지 않는다 |

다른 테이블이 `ctf_game_items` 를 참조하지 않고, 이 테이블은 `challenges` 를 `ON DELETE SET NULL` 로만 참조한다 (문제를 지워도 막히지 않음).

## 확인한 것 (2026-11-18, 로컬: 운영 스키마 + nimda-mysql)

- Flyway: `Successfully applied 1 migration ... now at version v41`
- 단위 테스트 `GameItemServiceTest` 8개, 백엔드 전체 테스트 50개 (Skipped 2) 통과
- 실제 호출: 등록 201, 중복 409, 잘못된 key 400, 없는 문제 400, key 변경 400, 없는 id 404, 참가자→관리 403, 로그인 없음 403
- 참가자 목록: 숨긴 물건 빠짐, 못 푼 물건 이야기 `null`, 풀이 기록을 넣으면 그 물건만 `owned: true` + 이야기
- `CTF_GAME_ENABLED=false` → 두 경로 모두 404

확인하지 못한 것: **팀이 있는 경우**의 소유(팀원 풀이 공유)는 단위 테스트(람다)로만 확인했다. 로컬에 대회·팀이 없어 `TeamSolvedChallenges` 의 팀 쿼리는 실제 DB 로 돌려 보지 않았다.

> Windows Git Bash 의 `curl` 로 한글 JSON 을 보내면 깨져서 400 이 난다. 시험할 때는 Node `fetch` 나 Postman 을 쓴다.
