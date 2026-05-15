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
│   ├── user/                   # 사용자 관리
│   ├── community/
│   │   ├── board/              # 커뮤니티 게시글
│   │   ├── comment/            # 댓글
│   │   └── profile/            # 주짓수 커뮤니티 프로필
│   ├── notice/                 # 알림
│   ├── boot_strap/             # 앱 버전 관리
│   └── file/                   # 이미지 CDN
└── global/
    ├── config/                 # Security, Swagger, WebClient 설정
    ├── exception/              # 전역 예외 처리, 에러 코드
    ├── security/               # JWT 필터, 토큰 블랙리스트
    ├── fcm/                    # FCM 푸시 알림
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

### 사용자 (`/api/user`)

| Method | Path | 설명 |
|--------|------|------|
| POST | `/user` | 회원가입 |
| GET | `/user/profile` | 내 프로필 조회 |
| PUT | `/user/profile` | 프로필 수정 (닉네임, 이미지) |
| DELETE | `/user` | 회원 탈퇴 (30일 유예기간) |
| GET | `/user/check/nickname` | 닉네임 중복/유효성 확인 |
| PUT | `/user/grant/owner-role` | 관장/사범 권한 부여 |
| POST | `/user/appInfo` | 앱 정보 등록 (FCM 토큰 등) |

### 커뮤니티 게시글 (`/api/board`)

| Method | Path | 설명 |
|--------|------|------|
| GET | `/board/category` | 카테고리 목록 조회 |
| GET | `/board` | 게시글 목록 조회 (카테고리 필터, 페이징) |
| GET | `/board/{id}` | 게시글 단건 조회 |
| POST | `/board` | 게시글 생성 |
| PUT | `/board/{id}` | 게시글 수정 |
| DELETE | `/board/{id}` | 게시글 삭제 |
| PUT | `/board/like/{id}` | 게시글 좋아요 등록/취소 |
| PUT | `/board/save/{id}` | 게시글 저장/취소 |
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
| POST | `/community/profile` | 커뮤니티 프로필 생성/수정 |

> 커뮤니티 프로필 포함 정보: 벨트 등급, 스트라이프, 성별, 선호 포지션/기술/서브미션, 대회 출전 이력

### 알림 (`/api/notice`)

| Method | Path | 설명 |
|--------|------|------|
| GET | `/notice` | 알림 목록 조회 |
| PUT | `/notice` | 모든 알림 읽음 처리 |
| PUT | `/notice/{id}` | 개별 알림 읽음 처리 |

### 앱 버전 (`/api/bootstrap`)

| Method | Path | 설명 |
|--------|------|------|
| GET | `/bootstrap/info` | 앱 버전 체크 (강제/선택 업데이트 여부) |

### 이미지 (`/api/image`)

| Method | Path | 설명 |
|--------|------|------|
| GET | `/image/auth` | ImageKit CDN 업로드용 서명 발급 |

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
- 대회 출전 이력 (`CompetitionInfo`: 년도, 월, 대회명, 수상 정보)

### Board / Content
- 게시글(`Board`)은 컨텐츠(`Content`)와 1:1 관계
- 컨텐츠에 댓글, 이미지, 좋아요 포함
- 카테고리(`BoardCategory`) 기반 분류

### Owner Profile
- 관장/사범 전용 프로필 (지도 철학, 경력 시작일, 경력 상세)

---

## 인증 방식

- **SNS 로그인**: Google OAuth 또는 Apple Sign In 토큰으로 로그인
- **JWT**: Access Token (1시간) + Refresh Token (7일) 발급
- **토큰 블랙리스트**: 로그아웃 시 Access Token 무효화
- **요청 헤더**: `Authorization: Bearer {accessToken}`

---

## 환경 설정

### 프로파일

| 프로파일 | 설명 |
|----------|------|
| `local` | H2 인메모리 DB, 로컬 개발 환경 |
| `dev` | PostgreSQL, 운영 서버 환경 |

### 주요 환경 변수 (dev)

```
SPRING_PROFILES_ACTIVE=dev
IMAGEKIT_PRIVATE_KEY=<ImageKit Private Key>
WEBHOOK_SECRET=<Webhook Secret>
GHCR_USERNAME=<GitHub Container Registry Username>
GHCR_PAT=<GitHub Personal Access Token>
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
- **운영**: `https://api.developer-chanq.xyz/api/swagger-ui.html`
