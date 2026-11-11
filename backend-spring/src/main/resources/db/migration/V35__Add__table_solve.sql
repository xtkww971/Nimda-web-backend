-- 기존 테이블이 존재할 경우 삭제
DROP TABLE IF EXISTS `solves`;

-- solves 테이블 생성
CREATE TABLE `solves` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `challenge_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `awarded_points` INT NOT NULL,
    `solved_at` DATETIME(6) NOT NULL,
    PRIMARY KEY (`id`),
    -- challenge_id를 앞에 둬서 문제별 조회(해결자 수, 삭제)도 이 키로 처리한다.
    CONSTRAINT `uk_solves_challenge_user` UNIQUE (`challenge_id`, `user_id`),
    -- 사용자별 정답 조회용
    KEY `idx_solves_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;