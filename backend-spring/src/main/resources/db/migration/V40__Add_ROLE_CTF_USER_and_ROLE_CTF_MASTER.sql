-- CTF 게임(game.nimda.kr) 역할.
--   ROLE_CTF_USER   : 대회 참가자. 게임에 들어갈 수 있다
--   ROLE_CTF_MASTER : 대회 운영진·출제자. 게임에 들어가고, 게임 안에서 운영진으로 표시된다
-- 게임 서버는 로그인 토큰(JWT)의 authorities 로 입장과 표시를 정한다. ROLE_ADMIN, ROLE_DEV 도 게임에 들어간다.
-- 백엔드 CTF API 의 권한 검사(ADMIN 또는 DEV)는 이번에 바꾸지 않는다.
-- 관리자 화면(/api/admin/users/roles)이 이 표를 읽으므로 넣기만 하면 부여할 수 있다.
INSERT INTO authority (authority_name)
SELECT 'ROLE_CTF_USER'
WHERE NOT EXISTS (SELECT 1 FROM authority WHERE authority_name = 'ROLE_CTF_USER');

INSERT INTO authority (authority_name)
SELECT 'ROLE_CTF_MASTER'
WHERE NOT EXISTS (SELECT 1 FROM authority WHERE authority_name = 'ROLE_CTF_MASTER');
