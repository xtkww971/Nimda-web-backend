/**
 * CTF 게임(game.nimda.kr) 이벤트용 기능: 맵 위 물건(아이템)과 문제 연결, 팀 소유에 따른 이야기 공개,
 * 게임 설정(Stage2 시간 개방, docs/api/CTF 게임 설정 API.md).
 *
 * <h2>이벤트용이라 지우기 쉽게 둔 것</h2>
 * <ul>
 *   <li>코드는 {@code com.nimda.cite} 와 따로 이 최상위 패키지에만 있다. 다른 코드는 이 패키지를 참조하지 않는다
 *       (이 패키지가 judgeServer 의 문제·정답·팀과 cite 의 로그인 사용자·ApiResponse 를 읽기만 한다).</li>
 *   <li>DB 는 {@code ctf_game_items} (V41) 와 {@code ctf_game_settings} (V42, 한 줄). 다른 테이블이 참조하지 않는다.</li>
 *   <li>오류 코드도 공용 ErrorCode 를 늘리지 않고 {@link com.nimda.ctfgame.exception.GameItemException} 으로 따로 둔다.</li>
 *   <li>밖에 걸친 곳은 네 군데: NimdaCiteApplication 의 스캔 범위 3줄({@code "com.nimda.ctfgame"}),
 *       SecurityConfig 의 {@code /api/ctf/game/**} 규칙, application.yml 의 {@code nimda.ctf-game.enabled},
 *       docker-compose.yml 의 {@code CTF_GAME_ENABLED}.</li>
 * </ul>
 *
 * <h2>끄기 (이벤트가 끝났을 때)</h2>
 * 서버 .env 에 {@code CTF_GAME_ENABLED=false} → 다음 배포부터 API 가 404. 데이터는 남는다.
 *
 * <h2>지우기 (다음 학기 등)</h2>
 * <ol>
 *   <li>이 패키지와 테스트(src/test/java/com/nimda/ctfgame) 삭제, 위 네 군데 정리</li>
 *   <li>새 마이그레이션 {@code V<다음 번호>__Drop_ctf_game_tables.sql}: {@code DROP TABLE IF EXISTS ctf_game_items;}
 *       {@code DROP TABLE IF EXISTS ctf_game_settings;} (V41·V42 파일은 이미 적용됐으므로 고치거나 지우지 않는다)</li>
 * </ol>
 */
package com.nimda.ctfgame;
