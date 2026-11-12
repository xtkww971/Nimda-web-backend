-- CTF 팀전 전환: 정답 기록을 팀 단위로 바꾸고 팀 점수판(ctf_team_scores)을 만든다.
--
-- 팀은 group 도메인(`groups`, group_members)을 그대로 쓴다. Activity가 CTF 하나뿐이라 그룹 = CTF 팀이다.

-- 1. solves에 팀을 남긴다.
--    같은 팀원이 같은 문제를 또 맞혀도 점수를 다시 주지 않도록 (문제, 팀) 유니크로 막는다.
--    기존 (문제, 유저) 유니크는 지운다. 팀 유니크가 문제별 조회(challenge_id 선두) 인덱스 역할도 한다.
--    팀전 이전에 쌓인 기록은 team_id가 NULL로 남는다 (팀 점수에 들어가지 않음, 문제 배점 감소에는 계속 반영).
ALTER TABLE solves ADD COLUMN team_id BIGINT NULL AFTER user_id;
ALTER TABLE solves ADD CONSTRAINT uk_solves_challenge_team UNIQUE (challenge_id, team_id);
ALTER TABLE solves DROP INDEX uk_solves_challenge_user;
-- 내 팀 최근 해결·멤버별 기여 조회용
CREATE INDEX idx_solves_team_solved_at ON solves (team_id, solved_at);

-- 2. 팀 점수판. 점수의 원본은 solves.awarded_points이고 이 테이블은 순위 조회용 집계본이다.
--    정답 기록 트랜잭션 안에서 같이 갱신하고, 어긋나면 solves로부터 다시 계산한다 (CtfTeamScoreRepository.recalculate).
--    행은 운영자가 팀을 만들 때 같이 만든다. 팀이 지워지면 함께 지워진다.
CREATE TABLE IF NOT EXISTS ctf_team_scores (
    team_id BIGINT NOT NULL,

    -- 팀이 얻은 점수 합
    total_score BIGINT NOT NULL DEFAULT 0,

    -- 팀이 푼 문제 수
    solve_count INT NOT NULL DEFAULT 0,

    -- 마지막으로 점수를 얻은 시각. 동점이면 이 값이 이른 팀이 위다. 아직 못 풀었으면 NULL
    last_solved_at DATETIME(6) NULL,

    updated_at DATETIME(6) NOT NULL,

    PRIMARY KEY (team_id),
    -- 순위 조회(ORDER BY total_score DESC, last_solved_at ASC)용
    KEY idx_ctf_team_scores_rank (total_score DESC, last_solved_at ASC),
    CONSTRAINT fk_ctf_team_scores_team FOREIGN KEY (team_id) REFERENCES `groups` (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. 이미 만들어진 팀도 점수판 행을 갖게 한다 (팀 기록이 아직 없으므로 0점에서 시작).
INSERT INTO ctf_team_scores (team_id, total_score, solve_count, last_solved_at, updated_at)
SELECT g.id, 0, 0, NULL, NOW(6)
FROM `groups` g
WHERE g.id NOT IN (SELECT team_id FROM ctf_team_scores);
