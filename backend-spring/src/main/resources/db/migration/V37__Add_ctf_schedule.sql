-- CTF 대회 일정(시작·종료 시각)과 종료 후 동작 설정, 변경 이력
--
-- 시각은 모두 서버 시간(KST) 기준 DATETIME이다. 참가자 PC 시계와 상관없이 서버가 판정한다.
-- 대회는 하나뿐이라 ctf_schedule은 id = 1 한 행만 쓴다. 행이 없으면 "일정 미등록"으로 보고 제출을 막지 않는다.

CREATE TABLE IF NOT EXISTS ctf_schedule (
    id TINYINT NOT NULL,

    starts_at DATETIME(6) NOT NULL,
    ends_at DATETIME(6) NOT NULL,

    -- 종료 시 실행 중인 WEB·PWN 인스턴스를 내릴지 (정리 동작은 아직 미구현, 설정만 저장)
    cleanup_instances_on_end BIT(1) NOT NULL DEFAULT b'1',

    -- 종료 후에도 문제 목록·상세·첨부파일을 볼 수 있는지 (끄면 종료 후 참가자에게 403)
    challenges_visible_after_end BIT(1) NOT NULL DEFAULT b'1',

    -- 종료 전에 스코어보드 갱신을 멈출지와 그 시점(종료 N분 전). 고정 동작은 정책 확정 후 구현, 설정만 저장
    freeze_scoreboard BIT(1) NOT NULL DEFAULT b'0',
    freeze_minutes_before_end INT NOT NULL DEFAULT 30,

    updated_at DATETIME(6) NOT NULL,
    updated_by BIGINT NULL,

    PRIMARY KEY (id),
    CONSTRAINT ck_ctf_schedule_single_row CHECK (id = 1),
    CONSTRAINT ck_ctf_schedule_range CHECK (ends_at > starts_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 일정 변경 이력 (운영 설정 화면의 CHANGE LOG)
CREATE TABLE IF NOT EXISTS ctf_schedule_history (
    id BIGINT NOT NULL AUTO_INCREMENT,

    -- CREATED            : 일정 최초 등록 (new_value = "시작 ~ 종료")
    -- STARTS_AT, ENDS_AT : 시작·종료 시각 변경 (old_value → new_value, "yyyy-MM-dd HH:mm")
    -- CLEANUP_INSTANCES, CHALLENGES_VISIBLE, FREEZE_SCOREBOARD : 종료 후 동작 켬/끔 (ON/OFF)
    -- FREEZE_MINUTES     : 스코어보드 고정 시점(종료 N분 전) 변경
    field VARCHAR(20) NOT NULL,
    old_value VARCHAR(100) NULL,
    new_value VARCHAR(100) NULL,

    changed_by BIGINT NULL,
    changed_at DATETIME(6) NOT NULL,

    PRIMARY KEY (id),
    KEY idx_ctf_schedule_history_changed_at (changed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
