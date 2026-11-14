-- Public local-only test accounts. Never import into a production database.
-- admin / 1234 ; test / 1234
-- Seed statements are idempotent and preserve existing passwords/balances.

SET NAMES utf8mb4;

INSERT INTO authority (authority_name) VALUES
    ('ROLE_USER'), ('ROLE_ADMIN'), ('ROLE_DEV'), ('ROLE_CARTEL'), ('ROLE_STUDY_LEADER')
ON DUPLICATE KEY UPDATE authority_name = VALUES(authority_name);

INSERT INTO users
    (user_id, name, nickname, password, student_num, email, major, birth,
     status, email_hide, created_at, updated_at)
VALUES
    ('admin', '로컬관리자', 'local-admin', '$2a$10$/cvt9WvOOBoRDXlUVSVqWeULnoDgRh/0/rBWjwWqlhnpRn6DRP8gq',
     '202600001', 'local-admin@nimda.test', '컴퓨터공학', '20000101',
     'APPROVED', FALSE, NOW(), NOW()),
    ('test', '로컬사용자', 'local-user', '$2a$10$7.nCChMVxxK2sTUHBgFXpulwkfL3idc5JbuzQlF4tMWvm9slxIJHC',
     '202600002', 'local-user@nimda.test', '컴퓨터공학', '20000102',
     'APPROVED', FALSE, NOW(), NOW())
ON DUPLICATE KEY UPDATE user_id = VALUES(user_id);

INSERT INTO user_authority (user_id, authority_id)
SELECT u.id, a.id FROM users u CROSS JOIN authority a
WHERE (u.user_id = 'admin' AND a.authority_name IN ('ROLE_USER', 'ROLE_ADMIN'))
   OR (u.user_id = 'test' AND a.authority_name = 'ROLE_USER')
ON DUPLICATE KEY UPDATE authority_id = VALUES(authority_id);

INSERT INTO user_balance (user_id, total_amount, updated_at)
SELECT id, 0, NOW() FROM users WHERE user_id IN ('admin', 'test')
ON DUPLICATE KEY UPDATE user_id = VALUES(user_id);

INSERT INTO category
    (parent_id, name, is_active, slug, sort_order, post_count, shop_enabled, created_at, updated_at)
VALUES
    (NULL, '새 소식', TRUE, 'news', 1, 0, FALSE, NOW(), NOW()),
    (NULL, '학술 게시판', TRUE, 'academic', 2, 0, FALSE, NOW(), NOW()),
    (NULL, '커뮤니티', TRUE, 'community', 3, 0, FALSE, NOW(), NOW()),
    (NULL, '질문과 답변', TRUE, 'qna', 4, 0, FALSE, NOW(), NOW()),
    (NULL, '자유 게시판', TRUE, 'free', 5, 0, FALSE, NOW(), NOW())
ON DUPLICATE KEY UPDATE slug = VALUES(slug);
