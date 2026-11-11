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
    CONSTRAINT `uk_solves_user_challenge` UNIQUE (`user_id`, `challenge_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;