-- CTF 대회 이력 보존 (#25)
--
-- 지금까지는 대회가 하나뿐이라고 가정했다 (V37 ctf_schedule은 id = 1 한 행).
-- 대회(ctf_contests)를 두고 일정·팀·정답 기록을 대회에 묶은 뒤, 종료 시점에 최종 결과를 스냅샷으로 남긴다.
-- 대회 하나 = 일정 + 팀 + 정답 기록 + 최종 결과. 가장 최근에 시작하는 대회가 "현재 대회"다.

-- 1. 대회. V37 ctf_schedule의 컬럼에 이름과 결과 확정 시각을 더한다.
CREATE TABLE IF NOT EXISTS ctf_contests (
    id BIGINT NOT NULL AUTO_INCREMENT,

    name VARCHAR(100) NOT NULL,

    starts_at DATETIME(6) NOT NULL,
    ends_at DATETIME(6) NOT NULL,

    cleanup_instances_on_end BIT(1) NOT NULL DEFAULT b'1',
    challenges_visible_after_end BIT(1) NOT NULL DEFAULT b'1',
    freeze_scoreboard BIT(1) NOT NULL DEFAULT b'0',
    freeze_minutes_before_end INT NOT NULL DEFAULT 30,

    -- 최종 결과 스냅샷을 만든 시각. NULL이면 아직 이력에 오르지 않은 대회다.
    -- 종료 시각을 미래로 바꿔 대회를 다시 열면 스냅샷을 지우고 NULL로 돌린다.
    finalized_at DATETIME(6) NULL,

    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    updated_by BIGINT NULL,

    PRIMARY KEY (id),
    KEY idx_ctf_contests_starts_at (starts_at),
    CONSTRAINT ck_ctf_contests_range CHECK (ends_at > starts_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. 일정 변경 이력을 대회별로. field 값은 V37 주석과 같고 NAME, REOPENED, FINALIZED가 더해진다.
CREATE TABLE IF NOT EXISTS ctf_contest_change_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    contest_id BIGINT NOT NULL,
    field VARCHAR(20) NOT NULL,
    old_value VARCHAR(100) NULL,
    new_value VARCHAR(100) NULL,
    changed_by BIGINT NULL,
    changed_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_ctf_contest_change_logs_contest (contest_id, changed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. 팀과 정답 기록을 대회에 묶는다. 팀 이름 중복·한 사람 한 팀·배점 감소는 대회 안에서만 따진다.
ALTER TABLE `groups` ADD COLUMN contest_id BIGINT NULL;
CREATE INDEX idx_groups_contest_id ON `groups` (contest_id);

ALTER TABLE solves ADD COLUMN contest_id BIGINT NULL AFTER team_id;
CREATE INDEX idx_solves_contest_challenge ON solves (contest_id, challenge_id);

-- 4. V37 일정이 있으면 첫 대회로 옮기고, 기존 팀과 팀 정답 기록을 그 대회에 묶는다.
INSERT INTO ctf_contests (name, starts_at, ends_at, cleanup_instances_on_end, challenges_visible_after_end,
                          freeze_scoreboard, freeze_minutes_before_end, finalized_at, created_at, updated_at, updated_by)
SELECT CONCAT('NIMDACON ', YEAR(s.starts_at)), s.starts_at, s.ends_at, s.cleanup_instances_on_end,
       s.challenges_visible_after_end, s.freeze_scoreboard, s.freeze_minutes_before_end, NULL,
       s.updated_at, s.updated_at, s.updated_by
FROM ctf_schedule s
WHERE NOT EXISTS (SELECT 1 FROM ctf_contests);

SET @first_contest = (SELECT MIN(id) FROM ctf_contests);

INSERT INTO ctf_contest_change_logs (contest_id, field, old_value, new_value, changed_by, changed_at)
SELECT @first_contest, h.field, h.old_value, h.new_value, h.changed_by, h.changed_at
FROM ctf_schedule_history h
WHERE @first_contest IS NOT NULL;

UPDATE `groups` SET contest_id = @first_contest WHERE contest_id IS NULL AND @first_contest IS NOT NULL;
UPDATE solves SET contest_id = @first_contest
WHERE contest_id IS NULL AND team_id IS NOT NULL AND @first_contest IS NOT NULL;

DROP TABLE IF EXISTS ctf_schedule_history;
DROP TABLE IF EXISTS ctf_schedule;

-- 5. 최종 결과 스냅샷. 종료 후 팀이 지워지거나 멤버가 바뀌어도 과거 결과는 그대로다.
--    team_id·user_id·challenge_id는 원본을 가리키지만 FK는 걸지 않는다 (원본이 지워져도 남아야 한다).

-- 팀 최종 순위. 점수가 0인 팀은 team_rank가 NULL (라이브 스코어보드와 같은 규칙)
CREATE TABLE IF NOT EXISTS ctf_contest_results (
    id BIGINT NOT NULL AUTO_INCREMENT,
    contest_id BIGINT NOT NULL,
    team_id BIGINT NOT NULL,
    team_rank INT NULL,
    team_name VARCHAR(50) NOT NULL,
    member_count INT NOT NULL,
    solve_count INT NOT NULL,
    total_score BIGINT NOT NULL,
    last_solved_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ctf_contest_results_team (contest_id, team_id),
    KEY idx_ctf_contest_results_rank (contest_id, team_rank)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 팀 멤버와 멤버별 기여. "참가한 대회" 필터와 내 팀 탭에 쓴다
CREATE TABLE IF NOT EXISTS ctf_contest_result_members (
    id BIGINT NOT NULL AUTO_INCREMENT,
    contest_id BIGINT NOT NULL,
    team_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    nickname VARCHAR(20) NOT NULL,
    role VARCHAR(20) NOT NULL,
    solve_count INT NOT NULL,
    points BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ctf_contest_result_members_user (contest_id, user_id),
    KEY idx_ctf_contest_result_members_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 문제별 결과. final_point는 종료 시점 배점 (최대 → 종료 시점)
CREATE TABLE IF NOT EXISTS ctf_contest_challenges (
    id BIGINT NOT NULL AUTO_INCREMENT,
    contest_id BIGINT NOT NULL,
    challenge_id BIGINT NOT NULL,
    code VARCHAR(50) NOT NULL,
    title VARCHAR(100) NOT NULL,
    category VARCHAR(20) NOT NULL,
    max_point INT NOT NULL,
    final_point INT NOT NULL,
    solved_teams INT NOT NULL,
    first_blood_team_id BIGINT NULL,
    first_blood_team_name VARCHAR(50) NULL,
    first_blood_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ctf_contest_challenges (contest_id, challenge_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
