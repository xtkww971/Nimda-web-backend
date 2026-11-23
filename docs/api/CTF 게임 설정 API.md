# CTF 게임 설정 API (Stage2 시간 개방)

CTF 게임(`https://game.nimda.kr`, 저장소 `Nimda-Security/Nimda-ctf-game`)의 이벤트 설정이다. 지금 설정은 **Stage2 시간 개방** 하나다.

- **기본 규칙 (게임 화면이 정한다):** Stage1 기념품 5개를 모은 팀만 Stage1 오른쪽 문으로 Stage2(완벽한 동아리)에 들어간다.
- **시간 개방 (이 API):** 대회 시작 후 **N분**이 지나면 기념품을 다 모으지 못한 팀에게도 Stage2 문을 연다. 기획안 `nimdactf.md` 8장 "개방 보완안"의 "시작 90분 후 미개방 팀에도 Stage2 접근 허용"과, 게임 프로토타입 초안의 "개방 시각이 지나면 Stage2 열림"이다.
- **문만 연다.** 시간 개방은 아이템·점수·해결 처리를 주지 않는다. 남은 Stage1 문제는 계속 풀 수 있다.

```
운영진 (ADMIN·DEV)                       게임 (참가자)
PUT /api/ctf/game/admin/settings         GET /api/ctf/game/settings
{ "stage2OpenMinutes": 90 }    ──저장──▶  { stage2OpenMinutes: 90, stage2OpenAt: "…14:30+09:00", stage2Open: true/false, serverTime }
                                          └ stage2OpenAt = 현재 대회 시작(starts_at) + 90분 (대회 일정을 바꿔도 따라간다)
```

## 위치

| 항목 | 값 |
|---|---|
| 코드 | `backend-spring/src/main/java/com/nimda/ctfgame/` |
| 컨트롤러 | `GameSettingsController`(참가자), `AdminGameSettingsController`(관리) |
| 서비스·엔티티·DTO | `GameSettingsService`, `GameSettings`, `GameSettingsRequest`·`GameSettingsResponse`, `GameSettingsRepository` |
| DB | `ctf_game_settings` 한 줄(id = 1), Flyway `V42__Create_ctf_game_settings.sql`. 마이그레이션이 시간 개방 없음(NULL)으로 넣어 둔다 |
| 시계 | 대회와 같은 서버 시계(KST, `ContestService.now`). 대회 일정은 읽기만 한다 (`judgeServer` 는 고치지 않음) |
| 켜고 끄기 | `CTF_GAME_ENABLED`(기본 `true`). `false` 면 아래 API 가 모두 **404** (게임 물건 API 와 같이) |
| 권한 | `SecurityConfig` 의 기존 규칙 그대로 |
| 테스트 | `src/test/java/com/nimda/ctfgame/service/GameSettingsServiceTest` |

권한 규칙:
- `/api/ctf/game/admin/**`: ADMIN·DEV
- `/api/ctf/game/**`: CTF_USER·CTF_MASTER·ADMIN·DEV

## 참가자(게임) API

### `GET /api/ctf/game/settings`

```json
{
  "success": true,
  "data": {
    "stage2OpenMinutes": 90,
    "stage2OpenAt": "2026-12-05T14:30:00+09:00",
    "stage2Open": false,
    "serverTime": "2026-12-05T14:12:03.512+09:00",
    "updatedAt": null
  }
}
```

| 필드 | 뜻 |
|---|---|
| `stage2OpenMinutes` | 대회 시작 후 몇 분 뒤 여는지. `null` 이면 시간 개방 없음 |
| `stage2OpenAt` | 실제로 여는 시각 = 현재 대회 시작 + 분. 시간 개방이 없거나 대회가 없으면 `null` |
| `stage2Open` | 지금 시간 개방이 된 상태인지 (`serverTime ≥ stage2OpenAt`) |
| `serverTime` | 서버 시각. **남은 시간은 참가자 PC 시계가 아니라 이것으로 센다** (일정 API 와 같은 방식) |
| `updatedAt` | 참가자 응답에서는 늘 `null` |

게임이 쓰는 법:
- Stage2 문 앞에서 E 를 누르면 판단합니다.
- 기념품 5개가 있으면 열립니다.
- 없더라도 `stage2Open` 이 `true` 거나 `serverTime` 기준으로 `stage2OpenAt` 이 지났으면 열고, 안내를 띄웁니다: "후반 구역이 개방되었습니다. 미수집 기념품은 계속 획득할 수 있습니다." (기획안 문구)
- 운영진은 늘 통과합니다.

## 관리 API (ADMIN·DEV)

### `GET /api/ctf/game/admin/settings`
참가자 응답과 같고 `updatedAt`(마지막으로 바꾼 때)이 채워진다.

### `PUT /api/ctf/game/admin/settings`

```json
{ "stage2OpenMinutes": 90 }
```

| 필드 | 필수 | 규칙 |
|---|---|---|
| `stage2OpenMinutes` | | 1~1440 (분). `null` 이거나 빼면 **시간 개방 끄기** |

응답: `"게임 설정을 바꿨습니다."` + 위와 같은 `data`. 바꾼 운영진은 `updated_by` 에 남는다.

| 상태 | 경우 |
|---|---|
| 400 | 0 이하, 1440 초과, 숫자가 아님 (검사 문구 그대로) |
| 403 | ADMIN·DEV 가 아님, 로그인 없음 |
| 404 | `CTF_GAME_ENABLED=false` |

> Windows Git Bash 의 `curl` 로 한글 JSON 을 보내면 깨질 수 있다. 이 요청은 숫자뿐이라 괜찮지만, 시험은 Node `fetch` 나 Postman 을 권한다.

## 운영 메모
- 대회 전에 정한다: 기획안의 "90분"은 **미확정**이다. 사전 풀이 뒤 확정하고 PUT 한다. 정하지 않으면 시간 개방은 없다.
- 대회 시각을 바꿔도 다시 넣을 필요가 없다 (시작 기준 상대 시간).
- 이벤트가 끝나면 `CTF_GAME_ENABLED=false`. 테이블을 지울 때는 V42 를 고치지 말고 새 마이그레이션 `DROP TABLE IF EXISTS ctf_game_settings` 를 더한다 (`package-info.java`).
