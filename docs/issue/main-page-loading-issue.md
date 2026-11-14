> 작성자: 최도일
> 브랜치: `perf/main-page-loading`
> 관련 코드: `backend-spring/src/main/java/com/nimda/cite/common/s3/` (`S3Service`, `S3Config`, `s3readme.md`)

### 배경

로그인한 사용자가 NIMDA 커뮤니티(nimda.kr) 메인 페이지에 들어가면 배너와 사진첩 이미지가 뜨기까지 체감상 매우 오래 걸린다.
서버 API 응답은 요청당 20~40ms로 빠르기 때문에, 원인은 서버 처리 시간이 아니라 **이미지 전달 방식**에 있다고 판단했다.

### 측정

#### 지표 기준

| 지표 | 설명 | Good | Needs Improvement | Poor |
|---|---|---|---|---|
| FCP (First Contentful Paint) | 텍스트·이미지 등 첫 콘텐츠가 화면에 나타나는 시점 | 1.8s 이하 | 1.8s ~ 3.0s | 3.0s 이상 |
| LCP (Largest Contentful Paint) | 뷰포트에서 가장 큰 이미지·텍스트 블록이 그려지는 시점 | 2.5s 이하 | 2.5s ~ 4.0s | 4.0s 이상 |

참고: https://web.dev/articles/lcp?hl=ko

#### Test 1. 자체 측정 스크립트

실제 Chrome으로 메인 페이지를 열어 지표와 받은 용량을 기록하는 스크립트(`perf-home.mjs`)로 측정했다.

```bash
NIMDA_ID=아이디 NIMDA_PW=비밀번호 node perf-home.mjs \
  --mode login --runs 3 --throttle lte --out after_login_lte.json
```

| 옵션 | 설명 |
|---|---|
| `--mode login` | API로 로그인해 받은 인증 쿠키를 Chrome에 넣고 측정한다 |
| `--runs 3` | 3회 반복하고 중앙값을 쓴다. 회차마다 빈 캐시로 첫 방문을 측정하고, 재방문도 따로 측정한다 |
| `--throttle lte` | 다운로드 10Mbps, 업로드 5Mbps, 지연 40ms로 제한한다 |

**개선 이전 결과 (3회 중앙값)**

| 조건 | FCP | LCP | 완료 | 용량 | 캐싱 |
|---|---|---|---|---|---|
| login, LTE, 첫 방문 | 0.68s | 6.06s | 8.70s | 10,141KB | 40(0) |
| login, LTE, 재방문 | 0.19s | 5.46s | 7.96s | 9,712KB | 39(2) |

* 받은 용량 10,141KB 중 **이미지가 9,722KB(약 96%)** 를 차지한다.
* 재방문에도 이미지 9,712KB를 다시 받는다. **브라우저 캐시가 동작하지 않는다.**
* FCP는 양호하지만 LCP가 Google 기준 **Poor**다.

#### Test 2. Chrome Lighthouse

| 설정 | 값 |
|---|---|
| Mode | Navigation |
| Device | Mobile (4G, RTT 150ms, 1.6Mbps, CPU 4배 감속) |
| Categories | Performance |
| 횟수 | 3회, 중앙값 |

| 항목 | 값 | 판정 |
|---|---|---|
| Performance score | 66 | Needs Improvement (50~66) |
| FCP | 3.4s | Bad |
| LCP | 47.4s | Bad (`home-banner_img`, S3) |
| TBT | 10ms | Good |
| CLS | 0.095 | Good |
| Speed Index | 3.4s | Good |

### 원인

Lighthouse 개선 항목과 코드 확인 결과, 원인은 세 가지다.

**C1. 이미지 캐싱이 되지 않는다 (Use efficient cache lifetimes)**

* 공개 이미지도 S3 Presigned URL로 내려주고 있다.
* Presigned URL의 서명에 생성 시각이 들어가서 같은 파일도 요청마다 주소가 바뀐다. 주소가 매번 다르니 브라우저 캐시가 동작하지 않는다.
* S3 객체에도 `Cache-Control`이 없다.

**C2. 썸네일·WebP가 없다 (Improve image delivery)**

* 업로드 시 리사이징 단계가 없어서 아바타 같은 작은 자리에도 원본을 그대로 표시한다.
* 원본 용량이 그대로 전달돼 이미지가 전체 용량의 96%를 차지한다.

**C3. S3를 HTTP/1.1로 직접 받는다 (Modern HTTP)**

* 여러 장을 동시에 받는 효율이 크게 떨어진다.
* CloudFront를 앞에 두어 HTTP/2 이상으로 전달해야 한다.

### 해결방안

**S1. 목록 응답으로 이미지를 바로 요청하고, 공개 이미지 URL을 재사용한다**

* 목록 응답의 `thumbnailAttachmentId`로 이미지를 바로 요청하고, 이미지마다 하던 상세 호출을 제거한다.
* 공개 이미지는 같은 URL을 Redis에 캐시해 재사용한다.
* Presigned URL 생성 시 `response-cache-control`을 지정한다.
* 기대효과: 배너 병렬 로딩, 재방문 시 이미지 캐시 적중

**S2. 업로드 시 썸네일을 WebP로 생성하고, 기존 이미지를 일괄 변환한다**

* 업로드 단계에 리사이징·WebP 변환을 추가한다.
* 이미 올라가 있는 이미지는 일괄 변환 작업으로 처리한다.
* 기대효과: 이미지 전송 용량 감소 (현재 약 9.7MB)

**S3. CloudFront + 고정 URL + 장기 캐시**

* 공개 이미지는 CloudFront의 고정 URL로 내려주고 장기 캐시를 건다.
* Presigned URL은 **비공개 첨부파일에만** 유지한다.
* 기대효과: HTTP/2 이상 전달, 서명 없는 고정 주소로 캐시 적중

### 작업 목록

* [ ] S1: 목록 응답에 `thumbnailAttachmentId` 포함, 프론트의 상세 호출 제거
* [ ] S1: 공개 이미지 URL Redis 캐시 및 `response-cache-control` 지정
* [ ] S2: 업로드 시 썸네일 생성(WebP)
* [ ] S2: 기존 이미지 일괄 변환
* [ ] S3: CloudFront 배포 및 고정 URL 적용, 공개/비공개 첨부파일 URL 분기

### 완료 조건

* [ ] 같은 조건(login, LTE)에서 `perf-home.mjs`를 다시 측정해 LCP가 **2.5s 이하(Good)** 로 내려간다
* [ ] 재방문 시 이미지가 캐시에서 제공된다 (캐시 적중 수 증가, 재방문 용량 대폭 감소)
* [ ] Lighthouse(Mobile, 3회 중앙값) Performance 점수가 개선 전(66)보다 오른다
* [ ] 비공개 첨부파일은 여전히 Presigned URL로만 접근된다
