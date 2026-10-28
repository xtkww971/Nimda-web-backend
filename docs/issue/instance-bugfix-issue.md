> 선행: `docs/ctf-instance-network-work.md`(ctf-instances 네트워크 연결), `docs/runbook/[RB-0001]`.
> 같이 고려해야 하는 이슈: #2  이슈의 TTL·재사용 수정이 그 이슈의 전제다.
> 해당 이슈는 **이미 있는 코드의 잘못된 동작**만 다룬다.

### 배경

네트워크 작업을 로컬에서 통합 테스트하면서,
그리고 과거 CTF 관련 기능이 미완성일 때 주석처리 했던 조회 API를 켜기 위해 코드를 읽으면서
인스턴스 생성·프록시 경로의 버그가 나왔다. 조회 API를 켜기 전에 고쳐야 하는 것과, 켜지 않아도 지금 드러나는 것이 섞여 있다.

### 버그

**B1. JVM DNS 캐시로 재생성 후 요청이 남의 컨테이너로 간다 — 수정 완료**

JVM이 이름 조회를 30초(실패는 10초) 캐시한다. 인스턴스를 재생성하면 IP가 바뀌는데, 옛 IP를 다른 컨테이너가 받으면
소유권 검사를 통과한 요청이 30초간 그 컨테이너로 갔다. 재생성 직후엔 10초간 502도 났다.

* 재현: 인스턴스 삭제 → 다른 컨테이너가 옛 IP 점유 → 같은 이름으로 재생성 → 요청이 옛 IP 컨테이너로 감
* 수정: `NimdaCiteApplication.main` 맨 앞에서 `networkaddress.cache.ttl`/`negative.ttl` = 0 (RB-0001 R10)

**B2. 결과·소유자 보관 시간이 인스턴스 수명보다 짧다** - CTF 서버 TTL 기능 미구현 당시 넣은 임시 값에 의해 발생

| 키 | 보관 | 만료되면 |
|---|---|---|
| `ctf:result:{uuid}` | 10분 (`CTF_TTL_RESULT`) | 프록시가 503 "준비되지 않았습니다" |
| `ctf:request:owner:{uuid}` | 30분 (`CTF_TTL_OWNER`) | 프록시가 403 "본인 인스턴스가 아닙니다" |
| 컨테이너 (Judge) | **1시간** (`perUserTTL`, SHARED는 lease 1시간) | reaper가 회수 |

컨테이너는 살아 있는데 **10분 뒤부터 참가자가 못 붙는다.**
결과에는 Judge가 정한 `expiresAt`이 실려 오므로, web 인스턴스 READY 결과를 받으면 결과·소유자·사용자 매핑을
**`expiresAt`까지** 보관한다. `expiresAt`이 없으면 지금 기본값을 쓴다.

**B3. 생성 버튼을 누를 때마다 새 컨테이너가 뜬다**

`InstanceService.createInstance`가 요청마다 `UUID.randomUUID()`를 만든다.
Judge는 이름(`ctf-{code}-{uuid}`)로 사용자와 컨테이너를 매핑하기에 같은 사용자·같은 문제라도 누를 때마다 컨테이너가 하나씩 늘어난다

따라서 같은 사용자·같은 문제에 처리 중인 요청이 있으면 uuid를 재사용해야 한다.

| 기존 상태 | 동작 |
|---|---|
| 없음 / FAILED / 소유자 매핑 만료 | 새 uuid 발급, 발행 |
| 생성 중 (상태 마커 있음, 결과 없음) | 같은 uuid 반환, **재발행 안 함** (같은 이름으로 두 번 빌드되지 않게) |
| READY | 같은 uuid 반환, 재발행 안 함 |
| 결과 만료 (마커·결과 없음, 매핑 있음) | 같은 uuid로 **재발행** → Judge가 떠 있으면 `Existing`으로 돌려주고, 없으면 같은 이름으로 다시 만든다 |

"생성 중" 판단에 지금 쓰기만 하고 읽지 않는 `InstanceStatusStore`(`ctf:instance:status{uuid}`, PENDING)를 쓴다.

**B4. 사용자 → 인스턴스 매핑이 저장되지 않는다**

`InstanceResultStore.saveUserChallengeMapping`을 부르는 곳이 없다. `ctf:instance:user-map:{userId}:{challengeId}`가 항상 비어 있어서
"이 문제로 내가 띄운 인스턴스"를 찾을 수 없다. B3와 짝 이슈의 `GET /{code}`가 이 매핑을 쓴다. 생성할 때 저장한다.

**B5. 502 응답에 내부 예외 메시지가 그대로 나간다**

`InstanceProxy`가 `"인스턴스에 연결할 수 없습니다: " + e.getMessage()`를 본문에 넣는다.
로컬 테스트에서 `...: null`이 나왔고, 경우에 따라 내부 호스트명·IP가 담긴다. 고정 문구로 바꾸고 원인은 로그로만 남긴다.

**B6. 잘못된 URI면 500이 난다**

`URI.create`가 `IllegalArgumentException`을 던지면 `IOException`만 잡는 `catch`를 지나 500이 된다.
(host에 `_`가 들어가거나, 경로에 URI에 못 쓰는 문자가 남은 경우.) 502로 같이 처리한다.

### 작업

* [x] `NimdaCiteApplication`: JVM DNS 캐시 끄기 (B1)
* [ ] `RedisCtfRequestProducer`: 소유자 매핑에 `challengeId`도 남긴다 (B2에서 사용자 매핑을 연장하려면 필요)
* [ ] `CtfResultConsumer`: web 인스턴스 READY 결과면 결과·소유자·사용자 매핑을 `expiresAt`까지 연장 (B2)
* [ ] `InstanceService.createInstance`: 사용자 매핑 저장, 살아 있는 요청 재사용 (B3, B4)
* [ ] `InstanceProxy`: 502 고정 문구 + 로그, `IllegalArgumentException` 처리 (B5, B6)
* [ ] 테스트: 재생성(B1), 10분 경과 후 접속 유지(B2), 연속 생성 시 컨테이너 하나(B3), 502 본문(B5)

### 이번에 하지 않는 것

* **Judge `Existing`의 `expiresAt`.** 이미 떠 있는 인스턴스를 돌려줄 때 Judge가 `expiresAt`을 안 싣는다.
  그러면 재사용으로 받은 결과는 web 기본 TTL(10분)로 떨어진다. Judge 쪽에서 registry의 만료 시각을 실어야 한다(RB-0001 J5).
* **인스턴스 연장·종료 API.** 수명을 늘리거나 직접 끄는 기능은 Judge 프로토콜부터 필요하다.
* **매 요청 CTF 조회(ADR-0017 2-3).** 여전히 결과 캐시를 쓴다. 이번엔 보관 시간만 수명에 맞춘다.
