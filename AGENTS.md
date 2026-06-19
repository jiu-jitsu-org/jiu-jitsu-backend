# Jiu-Jitsu-Backend AGENTS Guide

## 목적

이 문서는 이 저장소에서 작업하는 사람이 프로젝트 구조와 작업 기준을 빠르게 이해하도록 돕기 위한 운영 문서다.
특히 아래 목적에 맞춰 작성한다.

1. 도메인 레이어 구조와 패키지 책임을 일관되게 이해하도록 돕기
2. 새 기능 추가 시 어디에 무엇을 넣어야 하는지 빠르게 판단하게 하기
3. 공통 응답/에러 규칙이 작업 중 무너지지 않게 하기

## 프로젝트 성격

이 저장소는 `Spring Boot 3.3.4 + Java 17 + JPA` 기반 REST API 서버다.

핵심 목표:

1. 도메인 중심 패키지 구조 유지
2. 공통 응답 래핑과 에러 코드 체계 일관 적용
3. JWT 기반 무상태 인증 운영

## 문서 우선순위

작업 중 문서를 해석할 때는 아래 우선순위를 따른다.

1. 실제 코드 구조
2. 이 루트 `AGENTS.md`
3. 루트 `README.md`

## 기술 스택

| 항목 | 내용 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.3.4 |
| ORM | Spring Data JPA + Hibernate |
| DB | PostgreSQL (운영), H2 (테스트) |
| Security | Spring Security + JWT (jjwt 0.12.3) |
| API 문서 | springdoc-openapi 2.6.0 (Swagger UI) |
| Push | Firebase Admin SDK (FCM) |
| HTTP Client | Spring WebFlux WebClient, OpenFeign |
| SQL 로깅 | P6Spy |
| 빌드 | Gradle |

## 저장소 구조

```txt
.
├── AGENTS.md
├── README.md
├── build.gradle
├── settings.gradle
├── Dockerfile
└── src/
    ├── main/
    │   ├── java/com/jiujitsu/api/
    │   │   ├── domain/
    │   │   └── global/
    │   └── resources/
    │       ├── application.yml
    │       ├── application-local.yml
    │       └── application-dev.yml
    └── test/
```

## 패키지 구조 원칙

### 1. `domain/`은 기능의 본체다

도메인별 코드는 `domain/<도메인>` 아래에 모은다.

```txt
domain/
├── admin/            - 관리자 인증 및 신고 처리
├── boot_strap/       - 앱 버전 및 OS별 부트스트랩
├── community/        - 커뮤니티 (하위 도메인 존재)
│   ├── board/        - 게시글
│   ├── comment/      - 댓글
│   ├── content/      - 컨텐츠 공통 (좋아요, 저장)
│   ├── profile/      - 커뮤니티 프로필
│   └── report/       - 신고
├── file/             - 이미지 파일
├── notice/           - 알림
└── user/             - 유저, 인증, 차단
```

각 도메인 내부 레이어:

```txt
domain/<도메인>/
├── controller/   - HTTP 진입점 (@RestController)
├── service/      - 비즈니스 로직 (@Service, @Transactional)
├── repository/   - 데이터 접근 (JPA Repository)
├── entity/       - JPA 엔티티, Enum
├── dto/          - 요청/응답 DTO (record 우선)
├── factory/      - 엔티티 생성 로직
└── mapper/       - 엔티티 → DTO 변환
```

### 2. `global/`은 도메인 횡단 공통 코드다

```txt
global/
├── config/       - Spring 설정 (Security, Swagger, WebClient 등)
├── exception/    - ErrorCode 열거형, ErrorException, GlobalExceptionHandler, ApiResponse
├── fcm/          - Firebase 설정, 이벤트, 서비스
├── security/     - JWT 필터, JwtTokenProvider, CustomUserPrincipal
├── util/         - AuthenticationUtil 등 공통 유틸
├── properties/   - @ConfigurationProperties 클래스
└── entity/       - BaseEntity (공통 감사 필드)
```

## 공통 API 응답 규칙

모든 응답은 `ApiResponse<T>` 로 자동 래핑된다 (`ApiResponseWrapperAdvice`).

```json
{
  "success": true,
  "code": "OK",
  "message": "Success",
  "data": { ... }
}
```

에러 응답:

```json
{
  "success": false,
  "code": "C0002",
  "message": "존재하지 않는 게시글입니다.",
  "data": null
}
```

규칙:
- 컨트롤러는 항상 실제 비즈니스 타입을 리턴한다 (`void` 포함).
- `ApiResponse`를 컨트롤러에서 직접 리턴하지 않는다 (래퍼가 자동 처리).
- 예외는 `ErrorException(ErrorCode)` 를 throw하고, `GlobalExceptionHandler`가 처리한다.

## 에러 코드 규칙

에러 코드는 `ErrorCode` 열거형 (`global/exception/ErrorCode.java`)에서 중앙 관리한다.

접두사 기준:

| 접두사 | 의미 |
|--------|------|
| R | Bad Request (입력값 오류) |
| C | 컨텐츠/게시물 관련 |
| P | 처리 오류 (서명, CDN 등) |
| U | 유저/권한 관련 |
| A | 인증 토큰 관련 |
| V | 앱 버전 관련 |
| SY | 서버 내부 오류 |

새 에러 코드를 추가할 때는 해당 접두사 그룹에 맞춰 추가한다.

## JWT 인증 규칙

토큰 종류:

| 종류 | 유효기간 | 용도 |
|------|----------|------|
| access | 1시간 | API 인증 |
| refresh | 7일 | access 재발급 |
| temporary | 5분 | SNS 가입 중간 단계 |

공통 규칙:
- 모든 토큰은 `Authorization: Bearer <token>` 헤더로 전달한다.
- `JwtAuthenticationFilter`가 필터 체인에서 토큰을 파싱한다.
- 로그인 필요 API에서 인증 실패 시 `ErrorCode.LOGIN_NOT_ACCESS` 를 리턴한다.
- `/api/auth/**`, `/api/admin/auth/**`, Swagger UI 경로는 인증 없이 허용된다.

## Swagger 어노테이션 규칙

컨트롤러에서 아래 어노테이션을 일관되게 사용한다.

```java
@Tag(name = "...", description = "...")        // 컨트롤러 태그
@CommonApiResponses                             // 공통 응답 코드 자동 추가
@Operation(summary = "...", description = "...") // 각 엔드포인트 설명
@LoginErrorExamples                             // 인증 필요 API에 추가
@ApiErrorCodeExamples({ErrorCode.XXX, ...})     // 발생 가능한 에러 코드 명시
```

Swagger UI 주소: `/api/swagger-ui.html`
API 스펙 주소: `/api/api-docs`

## API 기본 경로

`application.yml`에 서블릿 경로가 `/api`로 설정되어 있다.

```yaml
spring:
  mvc:
    servlet:
      path: /api
```

실제 요청 경로 예시: `POST /api/board`, `GET /api/auth/login`

## 환경 설정 파일

```txt
src/main/resources/
├── application.yml       - 공통 설정 (JWT, Swagger, FCM, 서버 포트 등)
├── application-local.yml - 로컬 DB, CORS 등 로컬 전용 설정
└── application-dev.yml   - 개발 서버 설정
```

규칙:
- 공통 값은 `application.yml`에 둔다.
- 환경별로 달라지는 값만 `application-local.yml`, `application-dev.yml`에 Override한다.
- 새 설정 키를 추가하면 관련 환경 파일도 함께 갱신한다.

## 새 도메인/기능 추가 규칙

새 기능을 추가할 때는 아래 질문부터 판단한다.

1. 이 기능은 어느 도메인에 속하는가?
2. 기존 도메인 내부에 추가인가, 새 도메인인가?
3. 공통 레이어(`global/`)를 재사용할 수 있는가?
4. 새 에러 코드가 필요한가?

권장 순서:

1. `entity/`에 JPA 엔티티 및 열거형 정의
2. `repository/`에 JPA Repository 작성
3. `dto/`에 요청/응답 DTO 작성 (record 우선)
4. `factory/`에 엔티티 생성 로직 분리 (복잡한 경우)
5. `service/`에 비즈니스 로직 작성
6. `mapper/`에 엔티티 → DTO 변환 로직 작성
7. `controller/`에 엔드포인트 추가
8. 필요 시 `ErrorCode`에 에러 코드 추가

예시:

```txt
domain/community/board/
├── controller/BoardController.java
├── service/BoardService.java
├── repository/BoardRepository.java
├── entity/Board.java
├── dto/BoardCreateRequest.java
├── dto/BoardResponse.java
├── factory/BoardFactory.java
└── mapper/BoardMapper.java
```

## 구현 시 지켜야 할 실무 원칙

- 컨트롤러는 최대한 얇게 유지한다 (파라미터 받기 + 서비스 위임 + 리턴).
- 비즈니스 로직은 서비스에 둔다. 엔티티 검증 로직은 엔티티 내부 메서드로 분리한다.
- `@Transactional`은 서비스 클래스에 기본 적용하고, 조회 전용 메서드에는 `@Transactional(readOnly = true)` 를 붙인다.
- 엔티티 직접 수정은 JPA Dirty Checking 방식을 활용한다 (save 재호출 불필요).
- DTO는 Java record를 우선 사용한다.
- 인증 유저 조회는 `AuthenticationFacade`를 통한다 (`AuthenticationUtil`에 직접 접근하지 않는다).
- 재사용이 확인되기 전에는 공통화하지 않는다.

## 현재 구현된 도메인 요약

| 도메인 | 주요 기능 |
|--------|-----------|
| user | SNS 로그인(Google/Apple), JWT 발급, 프로필, 탈퇴, 차단 |
| admin | 관리자 회원가입/로그인, 신고 처리 |
| boot_strap | OS별 앱 버전 정보 조회 |
| community/board | 게시글 CRUD, 카테고리, 좋아요, 저장, 숨김 |
| community/comment | 댓글 CRUD, 좋아요 |
| community/content | 좋아요/저장 공통 처리 |
| community/profile | 커뮤니티 프로필(벨트, 경기 이력 등) |
| community/report | 게시글/댓글 신고, 자동 숨김 |
| file | CDN 이미지 파일 등록/삭제 |
| notice | 알림 설정, FCM 푸시 전송 |

## 협업 시 리뷰 기준

리뷰할 때는 아래를 우선 본다.

1. 기능 책임이 올바른 레이어(controller/service/repository)에 배치되었는가
2. 서비스가 너무 많은 책임을 가지고 있지 않은가
3. `ApiResponse` 직접 리턴 또는 에러 처리 누락은 없는가
4. 새 에러 상황에 `ErrorCode`가 추가되었는가
5. 인증 필요 API에 `@LoginErrorExamples`가 붙어 있는가
6. 문서와 실제 구조가 일치하는가

## 문서 갱신 규칙

아래 변화가 있으면 이 문서 갱신을 함께 고려한다.

- 새 도메인 또는 하위 도메인 추가
- `global/` 공통 레이어 추가 또는 변경
- 환경 설정 키 추가 또는 변경
- 인증 방식 변경
- 협업 규칙 변경

## 금지에 가까운 패턴

- 컨트롤러에서 비즈니스 로직 직접 구현
- 도메인 전용 코드를 이유 없이 `global/`로 이동
- `ErrorCode` 없이 임의 문자열로 에러 응답 리턴
- 서비스에서 `SecurityContextHolder` 직접 접근 (`AuthenticationFacade` 대신)
- `ApiResponse`를 컨트롤러에서 직접 생성하여 리턴 (래퍼 어드바이스가 중복 처리함)
- 구조 설명 없이 큰 패키지 이동 수행

## 작업 전 체크

이 저장소에서 새 작업을 시작할 때는 아래 순서를 따른다.

1. 변경 대상 도메인을 정한다.
2. 기존 레이어와 서비스 재사용 가능성을 확인한다.
3. 에러 케이스가 있다면 `ErrorCode`에 먼저 추가한다.
4. 구조 변경이 있으면 이 문서 수정 범위까지 같이 본다.
5. 작업 후 빌드(`./gradlew build`) 및 Swagger UI에서 엔드포인트를 확인한다.
