#!/bin/sh
# certbot --manual-auth-hook (DNS-01). RB-0001 단계 5에서 와일드카드 인증서(*.nimda.kr)를 받을 때 쓴다.
#
# nimda.kr DNS(호스팅케이알)는 API가 없어서 TXT 레코드를 사람이 넣는다. 이 hook은
#   1) 넣어야 할 TXT 값을 /hooks/state/txt-records 에 남기고
#   2) 사람이 DNS에 넣은 뒤 /hooks/state/go 파일을 만들 때까지 기다린다.
# 그래서 certbot을 터미널 없이(-d, --non-interactive) 띄워 둘 수 있다.
#
# 실행 예 (서버 ~/app):
#   mkdir -p ~/certbot-dns && rm -f ~/certbot-dns/*
#   docker run -d --name certbot-wildcard \
#     -v "$PWD/certbot/conf:/etc/letsencrypt" \
#     -v "$PWD/scripts:/hooks/bin:ro" \
#     -v "$HOME/certbot-dns:/hooks/state" \
#     certbot/certbot:v5.5.0 certonly --manual --preferred-challenges dns \
#     --manual-auth-hook /hooks/bin/certbot-dns-hook.sh \
#     --non-interactive --agree-tos --key-type ecdsa \
#     --cert-name nimda.kr -d '*.nimda.kr'
#   cat ~/certbot-dns/txt-records       # 이 값을 DNS에 TXT로 넣는다
#   touch ~/certbot-dns/go              # DNS 반영을 확인한 뒤
#   docker logs -f certbot-wildcard
set -eu

state=/hooks/state
mkdir -p "$state"
echo "_acme-challenge.${CERTBOT_DOMAIN} TXT ${CERTBOT_VALIDATION}" >> "$state/txt-records"

# 같은 이름(_acme-challenge.nimda.kr)에 값이 여러 개일 수 있다(-d nimda.kr -d '*.nimda.kr').
# 마지막 challenge에서만 기다려야 TXT 값을 한 번에 다 받는다.
if [ "${CERTBOT_REMAINING_CHALLENGES:-0}" -gt 0 ]; then
    exit 0
fi

left=${DNS_WAIT_SECONDS:-3600}
while [ ! -f "$state/go" ]; do
    if [ "$left" -le 0 ]; then
        echo "DNS TXT 승인(go 파일)을 기다리다 시간이 다 됐다" >&2
        exit 1
    fi
    sleep 5
    left=$((left - 5))
done
