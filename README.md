# Jiu-Jitsu Lab Backend API

주짓수 커뮤니티 앱을 위한 Spring Boot 기반 REST API 서버입니다.

---

## 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.3.4 |
| ORM | Spring Data JPA |
| DB | PostgreSQL (운영), H2 (로컬) |
| Security | Spring Security + JWT (Access / Refresh Token) |
| HTTP Client | Spring WebFlux (WebClient), OpenFeign |
| Push Notification | Firebase FCM |
| SNS 로그인 | Google OAuth, Apple Sign In |
| CDN | ImageKit |
| API 문서 | SpringDoc OpenAPI (Swagger UI) |
| 빌드 | Gradle |
| 컨테이너 | Docker, GHCR |
| 인프라 | Nginx + Cloudflare Tunnel |
| CI/CD | GitHub Actions |

---

## 프로젝트 구조

```
src/main/java/com/jiujitsu/api/
├── domain/
│   ├── auth/                   # SNS 로그인, 토큰 관리
│   ├── admin/                  # 관리자 인증 (이메일/비밀번호)
│   ├── user/                   # 사용자 관리, 차단
│   ├── community/
│   │   ├── board/              # 커뮤니티 게시글, 숨김
│   │   ├── comment/            # 댓글
│   │   ├── report/             # 신고 (게시글/댓글)
│   │   └── profile/            # 주짓수 커뮤니티 프로필
│   ├── notice/                 # 알림
│   ├── boot_strap/             # 앱 버전 관리
│   ├── file/                   # 이미지 CDN
│   └── log/                    # API 호출 로그
└── global/
    ├── config/                 # Security, Swagger, WebClient 설정
    ├── exception/              # 전역 예외 처리, 에러 코드
    ├── security/               # JWT 필터, 토큰 블랙리스트
    ├── fcm/                    # FCM 푸시 알림
    ├── log/                    # API 로그 서비스
    └── util/                   # 공통 유틸
```

---

## API 목록

> Base URL: `/api`
> Swagger UI: `/api/swagger-ui.html`

### 인증 (`/api/auth`)

| Method | Path | 설명 |
|--------|------|------|
| POST | `/auth/sns-login` | SNS 통합 로그인 (Google / Apple) |
| POST | `/auth/refresh` | 액세스 토큰 갱신 |
| POST | `/auth/logout` | 로그아웃 (토큰 무효화) |

### 관리자 인증 (`/api/admin/auth`)

| Method | Path | 설명 |
|--------|------|------|
| POST | `/admin/auth/signup` | 관리자 계정 생성 (이메일/비밀번호) |
| POST | `/admin/auth/login` | 관리자 로그인 |

### 사용자 (`/api/user`)

| Method | Path | 설명 |
|--------|------|------|
| POST | `/user` | 회원가입 |
| GET | `/user/profile` | 내 프로필 조회 |
| PUT | `/user/profile` | 프로필 수정 (닉네임, 이미지) |
| DELETE | `/user` | 회원 탈퇴 (30일 유예기간) |
| GET | `/user/check/nickname` | 닉네임 중복/유효성 확인 |
| PUT | `/user/owner` | 관장/사범 인증 신청 (심사 대기 중이면 서류 교체) |
| POST | `/user/appInfo` | 앱 정보 등록 (FCM 토큰 등) |
| POST | `/user/block/{id}` | 유저 차단/차단해제 (toggle) |


### 커뮤니티 게시글 (`/api/board`)

| Method | Path | 설명 |
|--------|------|------|
| GET | `/board/category` | 카테고리 목록 조회 |
| GET | `/board` | 게시글 목록 조회 (카테고리 필터, 페이징). 연결된 대회는 `competition` 카드 |
| GET | `/board/{id}` | 게시글 단건 조회 |
| POST | `/board` | 게시글 생성 (`competitionId` 로 공개 대회 연결 선택) |
| PUT | `/board/{id}` | 게시글 수정 (`competitionId` 는 보낸 값으로 교체, null = 연결 해제) |
| DELETE | `/board/{id}` | 게시글 삭제 |
| PUT | `/board/like/{id}` | 게시글 좋아요 등록/취소 |
| PUT | `/board/save/{id}` | 게시글 저장/취소 |
| PUT | `/board/hide/{id}` | 게시글 숨김/숨김해제 (toggle, 본인 게시글 불가) |
| GET | `/board/write` | 내가 작성한 글 목록 |
| GET | `/board/save` | 내가 저장한 글 목록 |

### 커뮤니티 댓글 (`/api/community/comments`)

| Method | Path | 설명 |
|--------|------|------|
| GET | `/community/comments` | 댓글 목록 조회 |
| POST | `/community/comments` | 댓글 작성 |
| POST | `/community/comments/like` | 댓글 좋아요 등록/취소 |
| DELETE | `/community/comments/{id}` | 댓글 삭제 |

### 커뮤니티 프로필 (`/api/community/profile`)

| Method | Path | 설명 |
|--------|------|------|
| GET | `/community/profile` | 내 커뮤니티 프로필 조회 |
| GET | `/community/profile/{userId}` | 다른 회원 공개 프로필 조회 (비로그인 가능, 체급·특기 제외) |
| POST | `/community/profile` | 커뮤니티 프로필 생성/수정 (항목별: 도장, 벨트·체급, 벨트만, 스타일, 대회, 지도 정보) |

> 커뮤니티 프로필 포함 정보: 벨트 등급, 스트라이프, 성별, 선호 포지션/기술/서브미션, 대회 출전 이력

### 신고 (`/api/reports`)

| Method | Path | 설명 |
|--------|------|------|
| POST | `/reports` | 게시글/댓글 신고 (중복 신고 불가, 본인 신고 불가) |

> 동일 대상 3회 이상 신고 시 전체 유저에게 자동 숨김 처리

### 관리자 신고 관리 (`/api/admin/reports`)

| Method | Path | 설명 |
|--------|------|------|
| GET | `/admin/reports` | 신고 목록 조회 (페이징) |
| PATCH | `/admin/reports/board/{contentId}/unhide` | 게시글 숨김 해제 |
| PATCH | `/admin/reports/comment/{commentId}/unhide` | 댓글 숨김 해제 |

### 대회 (`/api/competitions`)

| Method | Path | 설명 |
|--------|------|------|
| GET | `/competitions` | 대회 탭 목록 (공개 대회, 대회일순). 기준일 `date` + 양방향 커서(`nextCursor`·`previousCursor`), 필터 `region`·`status`·`interested`·`keyword`. 항목의 `popular` = 관심 많은 대회(같은 달 끝나지 않은 대회 중 관심 상위 `competition.popular.top`개, 관심 `min-interests`개 이상). 비로그인 가능 |
| GET | `/competitions/{competitionId}` | 대회 상세 (공개 대회, 일정·참가비 단계·관심 수·조회수). 비로그인 가능, 조회수는 오르지 않음 |
| POST | `/competitions/{competitionId}/views` | 대회 조회수 +1 (앱이 상세를 열 때, 중복 방지 없음) |
| GET | `/competitions/regions` | 대회가 있는 지역 (대회 탭 지역 필터, 지난 대회 포함 공개 대회 전체, 지역 enum 순서). 비로그인 가능 |
| GET | `/competitions/picker?keyword=` | 글쓰기 대회 고르기 (공개 대회, 다가오는 대회 → 끝난 대회, 최대 20개). 비로그인 가능 |
| GET | `/competitions/{competitionId}/stories` | 대회 이야기 (이 대회를 고른 글 최대 5개: 반응 많은 2개 + 최신). 비로그인 가능 |
| GET | `/competitions/autocomplete?keyword=` | 등록 대회 검색 (프로필 대회 기록용 대회명 자동완성, 지난 대회까지) |
| PUT | `/competitions/{competitionId}/interest` | 대회 관심 등록 (멱등, 처리 후 관심 상태·관심 수) |
| DELETE | `/competitions/{competitionId}/interest` | 대회 관심 해제 (멱등, 미공개로 바뀐 대회도 해제 가능) |
| PUT | `/competitions/{competitionId}/notice` | 이 대회 마감 알림 켜기 (관심 없으면 관심도 등록) |
| DELETE | `/competitions/{competitionId}/notice` | 이 대회 마감 알림 끄기 (관심 유지) |
| POST | `/competitions/{competitionId}/info-reports` | 대회 정보 제보 (정보가 달라요, 공개 대회. 같은 항목은 하루 3번까지) |

### 관리자 대회 (`/api/admin/competitions`)

| Method | Path | 설명 |
|--------|----------|------|
| POST | `/admin/competitions/catalog` | 자동완성용 대회 일괄 등록 (회차 = 대회명 + 연도, 대회일 없는 미공개, 중복은 건너뜀) |
| GET | `/admin/competitions` | 관리자 대회 목록 (대회일 있는 대회, 공개·미공개 모두. 공개 여부·대회명·대회일 기간 필터, 대회일 순 페이지, 비어 있는 공개 필수 칸) |
| GET | `/admin/competitions/{competitionId}` | 관리자 대회 상세 (공개 여부 무관, 입력 화면 전체 칸 + 조회수·관심 수·비어 있는 공개 필수 칸 + 접수 사이트·상태 링크(지금 보임·보이는 기간)) |
| POST | `/admin/competitions` | 관리자 대회 만들기 (미공개, 대회명·연도·대회일 필수, 여러 날 대회는 종료일, 접수 사이트. 같은 이름·연도의 자동완성용 대회가 있으면 그 행에 채움) |
| PUT | `/admin/competitions/{competitionId}` | 관리자 대회 고치기 (전체 교체 — 종료일·접수 사이트도 빼면 비움, 공개 여부·확인일 제외. 대회명·연도를 바꾸면 연결된 프로필 대회 기록도 맞춤) |
| PATCH | `/admin/competitions/{competitionId}/verify` | 관리자 대회 확인 완료 (마지막 확인일 = 오늘) |
| PATCH | `/admin/competitions/{competitionId}/publish` | 관리자 대회 공개 (필수 칸 검사, 마지막 확인일 = 오늘. 이미 공개면 그대로) |
| PATCH | `/admin/competitions/{competitionId}/unpublish` | 관리자 대회 비공개 (관심·알림 설정·게시글 연결은 유지) |
| PUT | `/admin/competitions/{competitionId}/links/{type}` | 관리자 대회 상태 링크 넣기·바꾸기 (신청·참가자 현황·대진표·실시간 매트·결과, 종류별 하나, 새로 넣거나 바뀌면 승인 전) |
| DELETE | `/admin/competitions/{competitionId}/links/{type}` | 상태 링크 지우기 |
| PATCH | `/admin/competitions/{competitionId}/links/{type}/approve` · `/hide` | 상태 링크 승인(켜기)·숨기기(끄기). 보이는 기간은 서버 계산(`CompetitionLinkWindow`) |
| GET | `/admin/competitions/info-reports` | 대회 정보 제보 목록 (최신순 페이지) |

### 관리자 관장/사범 인증 (`/api/admin/owner-requests`)

| Method | Path | 설명 |
|--------|------|------|
| GET | `/admin/owner-requests?status=PENDING` | 인증 신청 목록 조회 (페이징, 신청 최신순) |
| PATCH | `/admin/owner-requests/{requestId}/approve` | 승인 (관장/사범 권한 부여, 커뮤니티 프로필 필요) |
| PATCH | `/admin/owner-requests/{requestId}/reject` | 거절 (사유 선택, 재신청 가능) |

### 알림 (`/api/notice`)

| Method | Path | 설명 |
|--------|------|------|
| GET | `/notice` | 알림 목록 조회 (커뮤니티 알림 + 관심 대회 마감 알림) |
| PUT | `/notice` | 모든 알림 읽음 처리 |
| PUT | `/notice/{id}` | 개별 알림 읽음 처리 |
| PUT | `/notice/setting/competition` | 처음 관심 등록 때 관심 대회 알림 질문 답 저장 |

관심 대회 마감 알림은 매일 18시(`competition.deadline-notice.cron`) 내일 끝나는 얼리버드(할인 단계)·접수 마감을 관심 등록한 유저에게 보낸다 (`CompetitionDeadlineNoticeRunService`). 놓친 날은 같은 날 자정 전에 관리자 `POST /admin/competitions/deadline-notices`로 다시 돌린다 (이미 받은 유저는 건너뜀, 자정이 지나면 다음 날 마감을 보내게 됨)

### 앱 버전 (`/api/bootstrap`)

| Method | Path | 설명 |
|--------|------|------|
| GET | `/bootstrap/info` | 앱 버전 체크 (강제/선택 업데이트 여부) |

### 신고 (`/api/reports`)

| Method | Path | 설명 |
|--------|------|------|
| POST | `/reports` | 게시물/댓글 신고 (중복 신고 불가, 자기 자신 신고 불가) |

### 관리자 신고 관리 (`/api/admin/reports`)

| Method | Path | 설명 |
|--------|------|------|
| GET | `/admin/reports` | 전체 신고 목록 조회 (페이징) |
| PATCH | `/admin/reports/board/{contentId}/unhide` | 게시물 숨김 해제 |
| PATCH | `/admin/reports/comment/{commentId}/unhide` | 댓글 숨김 해제 |

### 이미지 (`/api/image`)

| Method | Path | 설명 |
|--------|------|------|
| GET | `/image/auth` | ImageKit CDN 업로드용 서명 발급 |

### API 로그 (`/api/log`) - 관리자 전용

| Method | Path | 설명 |
|--------|------|------|
| GET | `/log` | API 호출 로그 목록 조회 (날짜, 성공 여부 필터링, 페이징) |

---

## 주요 도메인 모델

### User
- SNS 로그인 (Google, Apple) 기반 회원 관리
- 역할: `USER`, `OWNER` (관장/사범)
- 상태: `ACTIVE`, `DELETED` (30일 유예 후 영구 삭제)
- 소프트 딜리트 적용 (`deletedAt` 기준 30일 유예)

### Community Profile
- 벨트 등급 (`BeltRank`) / 스트라이프 (`BeltStripe`)
- 선호 포지션 (`PositionType`) / 기술 (`TechniqueType`) / 서브미션 (`SubmissionType`)
- 대회 출전 이력 (`CompetitionRecord`: 연도, 대회명, 대표 고정·순서, 등록 대회 연결(`Competition`) / `CompetitionDivision`: 출전 부문별 메달, 도복/노기, 띠, 체급 또는 앱솔루트)

### Board / Content
- 게시글(`Board`)은 컨텐츠(`Content`)와 1:1 관계
- 컨텐츠에 댓글, 이미지, 좋아요 포함
- 카테고리(`BoardCategory`) 기반 분류
- `BoardHide`: 사용자별 게시글 숨김 (toggle, 본인 게시글 제외)

### Report
- 게시글(`BOARD`) / 댓글(`COMMENT`) 신고
- 동일 대상 3회 이상 신고 시 `hiddenAt` 자동 설정 → 전체 숨김
- 신고자 본인에게는 즉시 숨김 처리
- 관리자가 숨김 해제 가능

### Owner Profile
- 관장/사범 전용 프로필 (지도 철학, 경력 시작일, 경력 상세)

### Report
- 게시물(`BOARD`) 또는 댓글(`COMMENT`) 단위 신고
- 동일 대상 중복 신고 방지, 자기 자신 신고 불가
- 신고 누적 시 자동 숨김 처리 (관리자 해제 가능)

### UserBlock
- 특정 유저 차단 (토글 방식: 재요청 시 차단 해제)

---

## 인증 방식

- **SNS 로그인**: Google OAuth 또는 Apple Sign In 토큰으로 로그인
- **JWT**: Access Token (1시간) + Refresh Token (7일) 발급
- **토큰 블랙리스트**: 로그아웃 시 Access Token 무효화
- **요청 헤더**: `Authorization: Bearer {accessToken}`

---

## 환경 설정

### 프로파일

| 프로파일 | 설명                   |
|----------|----------------------|
| `local` | H2 인메모리 DB, 로컬 개발 환경 |
| `dev` | PostgreSQL, 개발 서버 환경 |

### 주요 환경 변수 (dev)

```
SPRING_PROFILES_ACTIVE=dev
IMAGEKIT_PRIVATE_KEY=<ImageKit Private Key>
WEBHOOK_SECRET=<Webhook Secret>
GHCR_USERNAME=<GitHub Container Registry Username>
GHCR_PAT=<GitHub Personal Access Token>
# 선택: 관심 많은 대회 기준 (기본은 application.yml competition.popular — 상위 3개, 관심 5개 이상). 바꾸면 재시작
COMPETITION_POPULAR_TOP=3
COMPETITION_POPULAR_MININTERESTS=5
```

### application.yml 주요 설정

```yaml
server:
  port: 8080

spring:
  mvc:
    servlet:
      path: /api   # 모든 API 경로 prefix

jwt:
  access-token-validity: 3600000   # 1시간
  refresh-token-validity: 604800000 # 7일
```

---

## 로컬 실행

```bash
# 빌드
./gradlew build

# 로컬 환경 실행
./gradlew bootRun --args='--spring.profiles.active=local'
```

---

## 배포

GitHub Actions를 통해 `develop` 브랜치 push 시 자동으로 빌드 및 GHCR 배포됩니다.

```
develop 브랜치 push
  -> GitHub Actions: Build & Push to GHCR
  -> Docker Image: ghcr.io/jiu-jitsu-org/jiu-jitsu-backend-api:latest-develop
  -> Webhook 수신 -> docker compose pull & up
```

### Docker Compose 구성 (`deploy/docker-compose.yml`)

| 서비스 | 설명 |
|--------|------|
| `api` | Spring Boot 애플리케이션 (포트 8080, 내부 노출) |
| `nginx` | Reverse Proxy (Cloudflare Tunnel 연결) |
| `webhook` | GitHub Webhook 수신 후 자동 배포 트리거 |

### 헬스체크

```
GET /actuator/health
```

---

## Swagger UI

서버 실행 후 아래 URL에서 API 문서를 확인할 수 있습니다.

- **로컬**: `http://localhost:8080/api/swagger-ui.html`
- **개발**: `https://dev-api.bjj-oss.kr/api/swagger-ui.html`
