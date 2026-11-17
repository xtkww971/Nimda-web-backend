# [RB-0002] nginx 설정 변경 반영

> 규칙: **nginx 설정(`nginx/nginx.conf`)이 바뀌면 → 서버에 올리고 → 검사하고 → nginx 를 restart 한다.**
> reload 만으로는 반영되지 않는다.

## 왜 restart 인가

`nimda-nginx` 는 `~/app/nginx/nginx.conf` **파일 하나**를 `/etc/nginx/nginx.conf` 로 바인드 마운트한다.
파일 하나짜리 바인드 마운트는 컨테이너가 뜰 때의 파일(inode)에 묶인다.
CI(scp)나 `sed -i`, `cp` 처럼 파일을 새로 만들어 바꾸면 서버 파일은 바뀌어도 **컨테이너는 예전 파일을 계속 본다.**
이 상태에서 `docker exec nimda-nginx nginx -t` / `nginx -s reload` 는 **예전 설정**을 검사·반영하고 "성공"한다.

- 2026-11-17: #34(game.nimda.kr 블록) 배포가 성공했지만 컨테이너 안 설정에는 블록이 없었다 (서버 파일 md5 `611ac3…` ≠ 컨테이너 `433bfd…`).
  restart 뒤 반영됐다.
- 같은 날 확인: 저장소 nginx.conf 의 API 블록이 쓰던 `live/api.nimda.kr` 인증서는 2026-08-01 에 만료돼 있었다.
  그동안 컨테이너가 예전 설정을 보고 있어 드러나지 않았다 → 와일드카드 `live/nimda.kr` 로 바꿈 (이 PR).
  웹사이트는 `nimda.kr/api` → Vercel rewrite → `http://<서버 IP>/api`(80) 경로라 인증서와 무관했다.

`nginx/conf.d/`(active-backend.inc)는 **폴더** 마운트라 바로 보인다. blue/green 전환은 reload 로 충분하다.

## CI 배포(deploy.sh)가 하는 일

main 병합 → CI 가 `deploy.sh, docker-compose.yml, nginx/nginx.conf, …` 를 `~/app` 에 올린 뒤 `deploy.sh`:

1. 새 백엔드 색상 기동·헬스체크, `active-backend.inc` 를 새 색상으로
2. **새 nginx.conf 검사**: 임시 컨테이너(`nginx:alpine`, 같은 마운트·`nimda-network`)로 `nginx -t`.
   실패하면 nginx 를 건드리지 않고 `.inc` 를 되돌리고 새 백엔드를 내린다 (트래픽은 그대로).
3. **인증서 남은 날짜 출력**: nginx.conf 가 쓰는 `live/<이름>/` 인증서마다 `✅ / ⚠️ 곧 만료(14일) / ❌ 만료` (경고만)
4. **반영**: 서버 nginx.conf 와 컨테이너가 보는 nginx.conf 의 md5 가 다르면 → `docker restart nimda-nginx` (1~2초 끊김).
   같으면 → `nginx -s reload` (무중단)
5. 예전 색상 백엔드 중단

CI 로그에서 `📝 nginx.conf 가 바뀌어 nginx 를 다시 시작합니다` 가 보이면 설정 변경이 반영된 것이다.

## 손으로 바꿀 때 (긴급 수정 등)

다음 CI 배포가 저장소 파일로 덮어쓰므로, **저장소에도 같은 변경을 PR 로 올린다.**

```bash
cd ~/app
cp nginx/nginx.conf nginx/nginx.conf.bak-$(date -u +%Y%m%d-%H%M%S)   # 백업
vi nginx/nginx.conf                                                    # 수정

# 1) 검사: 임시 컨테이너로 (떠 있는 컨테이너 안의 nginx -t 는 예전 파일을 볼 수 있다)
docker run --rm --network nimda-network \
  -v $PWD/nginx/nginx.conf:/etc/nginx/nginx.conf:ro -v $PWD/nginx/conf.d:/etc/nginx/conf.d:ro \
  -v $PWD/certbot/conf:/etc/letsencrypt:ro -v $PWD/certbot/www:/var/www/certbot:ro \
  nginx:alpine nginx -t

# 2) 반영: restart (1~2초 끊김)
docker restart nimda-nginx

# 3) 확인: 컨테이너가 새 파일을 보는지, 사이트가 사는지
diff <(md5sum < nginx/nginx.conf) <(docker exec nimda-nginx md5sum < /etc/nginx/nginx.conf) && echo 같음
curl -s -o /dev/null -w '%{http_code}\n' https://nimda.kr/            # 200
curl -s -o /dev/null -w '%{http_code}\n' https://api.nimda.kr/api/auth/me   # 401 (로그인 안 함)
curl -s -o /dev/null -w '%{http_code}\n' https://game.nimda.kr/       # 503(게임 꺼짐) 또는 200
```

끊김 없이 하고 싶으면 파일을 **새로 만들지 말고 내용만 덮어쓴다** (`cat 새파일 > nginx/nginx.conf`). inode 가 그대로라
컨테이너가 바로 새 내용을 보므로 `docker exec nimda-nginx nginx -t && docker exec nimda-nginx nginx -s reload` 로 된다.
(`vi` 는 설정에 따라 새 파일을 만들 수 있으니 위 md5 비교로 확인한다)

## 되돌리기

```bash
cd ~/app
cat nginx/nginx.conf.bak-<시각> > nginx/nginx.conf
docker exec nimda-nginx nginx -t && docker exec nimda-nginx nginx -s reload
```

## 인증서

| 파일 | 덮는 이름 | 쓰는 블록 | 상태 (2026-11-17) |
|---|---|---|---|
| `live/nimda.kr` | `*.nimda.kr` | api, inst-*, game | 2027-02-09 까지 (DNS-01, RB-0001) |
| `live/api.nimda.kr` | `api.nimda.kr` | 없음 (이 PR 부터) | 2026-08-01 만료, 갱신 안 됨. 정리 대상 |
