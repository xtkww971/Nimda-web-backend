#!/bin/bash
# CI가 deploy.sh와 docker-compose.yml을 함께 복사한 뒤 실행한다.
# 서버 상태는 이 두 파일과 .env로만 정한다. 서버에서 손으로 compose를 고치거나 네트워크를 만들지 않는다.
set -uo pipefail
cd "$(dirname "$0")"

# 0. .env 필수 키 확인. 빈 값으로 컨테이너가 뜨면 DB·Redis 인증이 깨지므로 시작 전에 멈춘다.
#    목록은 docker-compose.yml이 ${...}로 참조하는 값이다 (.env.example 참고).
REQUIRED_ENV_KEYS=(
    MYSQL_ROOT_PASSWORD MYSQL_DATABASE MYSQL_USER MYSQL_PASSWORD
    REDIS_PASSWORD
    JWT_SECRET
    SPRING_SECURITY_USER_NAME SPRING_SECURITY_USER_PASSWORD
    AWS_S3_BUCKET AWS_S3_REGION AWS_S3_ACCESS_KEY AWS_S3_SECRET_KEY
    MAIL_HOST MAIL_PORT MAIL_USERNAME MAIL_PASSWORD
)
MISSING=()
for key in "${REQUIRED_ENV_KEYS[@]}"; do
    grep -qE "^[[:space:]]*${key}[[:space:]]*=[[:space:]]*[^[:space:]]" .env 2>/dev/null || MISSING+=("$key")
done
if [ ${#MISSING[@]} -gt 0 ]; then
    echo "❌ .env에 값이 없는 키: ${MISSING[*]} — 배포를 중단합니다."
    exit 1
fi

# compose는 서비스 정의가 바뀔 때만 재생성하고, 망 정의(name:)만 바뀐 건 감지하지 못한다.
# 그래서 컨테이너가 compose가 정한 망에 붙어 있는지 직접 보고, 아니면 강제로 재생성한다.
APP_NETWORK=nimda-network

# 서비스를 compose 정의에 맞춘다. 정의가 그대로이고 망도 맞으면 아무것도 안 한다.
#   --no-deps: nginx나 다른 색상 backend를 같이 띄우지 않는다.
reconcile() {
    local service=$1 container=$2
    shift 2
    local recreate=()
    if ! docker inspect "$container" --format '{{range $k, $v := .NetworkSettings.Networks}}{{$k}} {{end}}' 2>/dev/null \
            | grep -qw "$APP_NETWORK"; then
        echo "🔁 $container 가 $APP_NETWORK 에 없어 재생성합니다."
        recreate=(--force-recreate)
    fi
    docker compose up -d --no-deps "${recreate[@]}" "$@" "$service"
}

# 1. 공용 인프라(mysql, redis)
if ! reconcile mysql nimda-mysql --wait || ! reconcile redis nimda-redis --wait; then
    echo "❌ mysql/redis가 healthy가 되지 않았습니다. 배포를 중단합니다."
    exit 1
fi

if [ ! -f "./nginx/conf.d/active-backend.inc" ]; then
    echo "server backend-blue:8080;" > ./nginx/conf.d/active-backend.inc
    echo "active-backend.inc 파일이 생성되었습니다. (초기 타겟: blue)"
fi

# 2. 현재 Nginx 뒷단에서 구동 중인 서비스가 'blue'인지 'green'인지 헬스체크로 알아냅니다.
if grep -q "backend-blue" "./nginx/conf.d/active-backend.inc"; then
    ACTIVE_COLOR="blue"
    TARGET_COLOR="green"
    TARGET_PORT=8082
else
    ACTIVE_COLOR="green"
    TARGET_COLOR="blue"
    TARGET_PORT=8081
fi

echo "현재 가동 중인 서비스: $ACTIVE_COLOR ➡️ 새로 배포할 타겟: $TARGET_COLOR"

# 4. 새로 배포할 색상의 최신 이미지를 도커허브에서 땡겨옵니다.
docker compose pull backend-$TARGET_COLOR

# 새로운 버전 컨테이너 실행. 의존 서비스는 1단계에서 맞췄다.
docker compose up -d --no-deps --force-recreate backend-$TARGET_COLOR

echo "⏳ 새 버전($TARGET_COLOR) 부팅 및 헬스체크 대기 중..."
# 새 버전 정상 작동 확인
HEALTHY=false
for retry in {1..24}
do
    status_code=$(curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:$TARGET_PORT/error)
    if [ "$status_code" != "000" ]; then
        echo "✅ $TARGET_COLOR 서버가 정상 응답합니다. (Status: $status_code)"
        HEALTHY=true
        break
    fi
    sleep 5
done

# 헬스체크 실패 시 트래픽 전환 없이 새 버전만 내리고 중단
if [ "$HEALTHY" != true ]; then
    echo "❌ $TARGET_COLOR 서버가 응답하지 않습니다. 배포를 중단하고 $ACTIVE_COLOR 를 유지합니다."
    docker compose stop backend-$TARGET_COLOR
    exit 1
fi

# 정상 작동 시 구동되어야 할 서버를 .inc에 저장
echo "server backend-$TARGET_COLOR:8080;" > ./nginx/conf.d/active-backend.inc

# nginx도 compose 정의에 맞춘다(네트워크가 바뀌었으면 재생성되어 새 .inc를 읽는다). 그대로면 아무것도 안 한다.
reconcile nginx nimda-nginx

# ── nginx 설정 반영 ──────────────────────────────────────────────
# nginx.conf 는 "파일 하나"를 바인드 마운트한다. CI(scp)가 파일을 새로 만들어 바꾸면(새 inode) 떠 있는 컨테이너는
# 예전 파일을 계속 보고, 그 안에서 하는 nginx -t 와 reload 도 예전 설정으로 돈다.
# (2026-11-17: #34 의 game.nimda.kr 블록이 배포 "성공" 뒤에도 반영되지 않았다.) 그래서 규칙은:
#   1) CI 가 올린 서버의 새 nginx.conf 를 임시 컨테이너(같은 마운트·망)로 검사한다. 실패하면 nginx 를 건드리지 않는다.
#   2) 컨테이너가 보는 nginx.conf 와 내용이 다르면(= 설정이 바뀌었으면) nginx 를 restart 한다 (1~2초 끊김).
#      같으면(= 설정 그대로, active-backend.inc 만 바뀜) reload 만 한다. (conf.d 는 폴더 마운트라 바로 보인다)
# 손으로 nginx.conf 를 고칠 때도 같다: 고친 뒤 검사하고 restart (RUNBOOK 참고).
NGINX_MOUNTS=(-v "$PWD/nginx/nginx.conf:/etc/nginx/nginx.conf:ro" -v "$PWD/nginx/conf.d:/etc/nginx/conf.d:ro"
              -v "$PWD/certbot/conf:/etc/letsencrypt:ro" -v "$PWD/certbot/www:/var/www/certbot:ro")

nginx_failed() {
    echo "❌ $1 — nginx 를 건드리지 않습니다. 트래픽은 $ACTIVE_COLOR 에 그대로 둡니다."
    echo "server backend-$ACTIVE_COLOR:8080;" > ./nginx/conf.d/active-backend.inc
    docker compose stop backend-$TARGET_COLOR
    exit 1
}

if ! docker run --rm --network "$APP_NETWORK" "${NGINX_MOUNTS[@]}" nginx:alpine nginx -t; then
    nginx_failed "새 nginx.conf 검사 실패 (예: 인증서 파일 없음, 문법 오류)"
fi

# nginx.conf 가 쓰는 인증서의 남은 날짜 (경고만, 배포는 막지 않는다). nginx -t 는 만료를 보지 않는다.
# certbot 이미지에 든 python/cryptography 로 파일만 읽는다 (네트워크 없음, 읽기 전용)
CERTBOT_IMAGE=$(docker image ls --format '{{.Repository}}:{{.Tag}}' certbot/certbot | head -1)
CERT_NAMES=$(grep -oE "live/[^/]+/fullchain.pem" ./nginx/nginx.conf | cut -d/ -f2 | sort -u)
if [ -n "$CERTBOT_IMAGE" ] && [ -n "$CERT_NAMES" ]; then
    # shellcheck disable=SC2086
    docker run --rm --network none --entrypoint python3 -v "$PWD/certbot/conf:/le:ro" "$CERTBOT_IMAGE" -c '
import sys, datetime
from cryptography import x509
now = datetime.datetime.now(datetime.timezone.utc)
for name in sys.argv[1:]:
    cert = x509.load_pem_x509_certificate(open(f"/le/live/{name}/fullchain.pem", "rb").read())
    left = (cert.not_valid_after_utc - now).days
    mark = "❌ 만료" if left < 0 else ("⚠️  곧 만료" if left < 14 else "✅")
    print(f"{mark} 인증서 {name}: {cert.not_valid_after_utc.date()} 까지 ({left}일)")
' $CERT_NAMES || echo "⚠️  인증서 날짜를 읽지 못했습니다."
fi

host_conf=$(md5sum < ./nginx/nginx.conf | cut -d' ' -f1)
live_conf=$(docker exec nimda-nginx md5sum /etc/nginx/nginx.conf | cut -d' ' -f1)
if [ "$host_conf" != "$live_conf" ]; then
    echo "📝 nginx.conf 가 바뀌어 nginx 를 다시 시작합니다 (새 파일을 읽도록)."
    docker restart nimda-nginx >/dev/null
    for retry in {1..10}; do
        [ "$(docker inspect -f '{{.State.Running}}' nimda-nginx)" = "true" ] && break
        sleep 1
    done
    if [ "$(docker exec nimda-nginx md5sum /etc/nginx/nginx.conf | cut -d' ' -f1)" != "$host_conf" ]; then
        echo "⚠️  재시작 뒤에도 컨테이너가 새 nginx.conf 를 보지 않습니다. docker compose up -d --force-recreate nginx 로 확인하세요."
    fi
else
    # Nginx 프록시 서버 변경 (설정은 그대로, active-backend.inc 만 바뀜)
    docker exec nimda-nginx nginx -s reload
fi
echo "🔄 Nginx 트래픽을 backend-$TARGET_COLOR 로 전환했습니다."
# 기존 서버 중단
docker compose stop backend-$ACTIVE_COLOR
echo "🧹 구버전 서버(backend-$ACTIVE_COLOR)를 안전하게 중단했습니다."