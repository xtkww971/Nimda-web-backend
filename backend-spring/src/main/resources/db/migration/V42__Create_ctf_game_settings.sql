-- CTF 게임(game.nimda.kr) 이벤트용 설정. 한 줄(id = 1)만 쓴다.
-- stage2_open_minutes: 대회 시작 후 이 분이 지나면 Stage1 기념품을 다 모으지 못한 팀에게도 게임의 Stage2 문을 연다
--   (기획안 8장 "개방 보완안": 시작 90분 후 미개방 팀에도 Stage2 접근 허용. 시간 개방은 아이템·점수·해결 처리를 주지 않는다).
--   NULL 이면 시간 개방 없음 (Stage1 기념품 5개를 모아야만 열린다).
-- 대회 시작 기준의 상대 시간이라 대회 일정을 바꿔도 다시 맞출 필요가 없다. 실제 시각은 API 가 현재 대회의 starts_at 으로 계산한다.
--
-- 이벤트용이라 지우기 쉽게 둔다: 다른 테이블을 참조하지 않고, 다른 테이블도 이것을 참조하지 않는다.
-- 정리할 때는 이 파일을 고치지 말고 새 마이그레이션(DROP TABLE IF EXISTS ctf_game_settings)을 추가한다.
-- out-of-order: true 라 다른 브랜치의 번호와 병합 순서가 바뀌어도 적용된다 (번호 예약: 게임 저장소 밖 docs/WORKSTREAMS.md 4장).
CREATE TABLE IF NOT EXISTS ctf_game_settings (
    id                  TINYINT     NOT NULL COMMENT '늘 1',
    stage2_open_minutes INT         NULL     COMMENT '대회 시작 후 몇 분 뒤 Stage2 를 모두에게 여는지. NULL 이면 시간 개방 없음',
    updated_at          DATETIME(6) NOT NULL,
    updated_by          BIGINT      NULL     COMMENT '마지막으로 바꾼 운영진 users.id',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO ctf_game_settings (id, stage2_open_minutes, updated_at, updated_by) VALUES (1, NULL, NOW(6), NULL);
