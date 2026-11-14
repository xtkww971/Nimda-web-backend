## 로컬에서 백엔드 + 인프라 돌리는법

1. 개인컴일경우 docker desktop 실행
2. .env.local.example 참고해서 .env.local 만들기
3. 루트에서 docker compose -f docker-compose.local.yml up -d 실행
4. vs code 디버거로 스프링 띄우기

API문서: 이때 로컬 docker에서 Swagger도 같이뜸. localhost:18080 로 확인가능

