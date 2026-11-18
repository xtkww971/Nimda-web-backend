-- CTF 게임(game.nimda.kr) 이벤트용: 맵 위 물건(아이템)의 "정보" 쪽.
-- 참가자가 물건에 다가가 상호작용하면 연결된 문제 팝업이 뜨고, 팀이 그 문제를 풀면 물건을 "가진" 것으로 보고 이야기(description)를 보여 준다.
-- 소유는 따로 저장하지 않고 solves(팀 정답 기록)로 계산한다.
-- 물건의 위치·그림·크기("디자인" 쪽)는 여기 두지 않는다. 게임 저장소 client/ctf/objects.json 이 맡고 item_key 로만 이어진다
-- (CTF_INTEGRATION D12: 맵이 바뀌어도 고치는 곳은 게임 하나뿐).
--
-- 이벤트용이라 지우기 쉽게 둔다:
--   - 다른 테이블이 이 테이블을 참조하지 않는다 (이 테이블만 challenges 를 참조)
--   - 문제를 지워도 막히지 않게 ON DELETE SET NULL
--   - 이벤트가 끝나면 CTF_GAME_ENABLED=false 로 API 를 끄고, 정리할 때는 이 파일을 고치지 말고
--     새 마이그레이션(V<다음>__Drop_ctf_game_items.sql: DROP TABLE IF EXISTS ctf_game_items)을 추가한다
-- V40 은 역할 추가(feat/ctf-roles)가 쓴다. out-of-order: true 라 어느 쪽이 먼저 병합돼도 적용된다.
CREATE TABLE IF NOT EXISTS ctf_game_items (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    item_key     VARCHAR(50)  NOT NULL COMMENT '고유 키 (영문 소문자·숫자·-). 게임 objects.json 의 key 와 같다',
    name         VARCHAR(100) NOT NULL COMMENT '화면에 보이는 이름 (예: 낡은 게임기)',
    description  TEXT         NULL     COMMENT '이야기. 팀이 연결 문제를 푼 뒤에만 내려준다',
    challenge_id BIGINT       NULL     COMMENT '상호작용하면 여는 문제',
    sort_order   INT          NOT NULL DEFAULT 0,
    visible      TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '0 이면 참가자에게 보이지 않는다 (배치 준비 중)',
    created_at   DATETIME(6)  NOT NULL,
    updated_at   DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_ctf_game_items_key UNIQUE (item_key),
    KEY idx_ctf_game_items_challenge (challenge_id),
    CONSTRAINT fk_ctf_game_items_challenge FOREIGN KEY (challenge_id) REFERENCES challenges (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
