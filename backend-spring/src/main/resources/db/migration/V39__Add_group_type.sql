-- 스터디 그룹과 CTF 팀 구분
--
-- groups 테이블을 스터디 그룹과 CTF 팀이 함께 쓰는데 둘을 가를 값이 없었다.
-- contest_id만으로는 안 된다. 대회가 생기기 전에 만든 CTF 팀도 contest_id가 NULL이고,
-- 첫 대회를 만들 때 contest_id가 NULL인 그룹을 전부 그 대회 팀으로 편입하기 때문이다.
--
-- 지금까지 스터디 그룹을 만드는 화면이 없었으므로 기존 행은 모두 CTF 팀으로 본다.
-- 새 스터디 그룹은 애플리케이션이 STUDY로 넣는다.

ALTER TABLE `groups`
    ADD COLUMN group_type VARCHAR(20) NOT NULL DEFAULT 'CTF_TEAM' AFTER description;

CREATE INDEX idx_groups_type_contest ON `groups` (group_type, contest_id);
