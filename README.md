# KnowWhoHow Backend

노후하우(KnowWhoHow)의 메인 백엔드 서버입니다.

사용자의 자산과 재무 목표를 바탕으로 포트폴리오를 진단하고, 금융 상품 시뮬레이션·일자리 탐색·MyData 연동·디지털 유산(영상 편지) 관리를 제공하는 REST API 서버입니다.

## 주요 기능

| 도메인 | 제공 기능 |
| --- | --- |
| 인증 | 일반 회원가입/로그인, Kakao OAuth2 로그인, JWT 재발급·로그아웃, CoolSMS 휴대폰 인증 |
| 사용자 | 프로필, 관심 키워드, 투자 성향, 보유 자산·연금 자산 관리, 회원 탈퇴 |
| 자산 관리 | 재무 설문 저장, 포트폴리오 진단, 예금·적금 만기 금액 시뮬레이션, 금융 상품 조회 |
| MyData | Authorization Code 기반 연동, 토큰 관리, 외부 Resource Server의 자산 데이터 조회 |
| 일자리 | 공공 Open API 기반 채용 공고 검색·상세 조회, Redis 캐시 |
| 디지털 유산 | 상속 계획, S3 영상 멀티파트 업로드, 수신인 등록, 예약 이메일 발송 및 영상 열람 |

## 기술 스택

### Application

- Java 17
- Spring Boot 3.2.5
- Spring Web MVC, Validation
- Spring Data JPA
- Spring Security
- Spring WebFlux `WebClient`
- Gradle
- Lombok

### Data & Infrastructure

- MySQL: 서비스 영속 데이터
- Redis: 인증 코드, 임시 OAuth 정보, MyData Access Token, 채용 API 캐시
- AWS S3: 영상 편지 저장 및 Presigned URL 기반 멀티파트 업로드
- SMTP: 예약 영상 편지 링크 발송
- CoolSMS(Solapi): 휴대폰 본인 인증
- Docker, Docker Compose, Nginx

### Authentication & API

- JJWT 0.12.5
- Kakao OAuth2
- springdoc-openapi / Swagger UI

### Test

- JUnit 5
- Spring Boot Test
- Spring Security Test
- H2
- JaCoCo

## 아키텍처

프로젝트는 도메인 단위 패키지와 계층형 구조를 함께 사용합니다.

```text
Client
  │
  ▼
Spring Security / JWT Filter
  │
  ▼
Controller ── DTO / Validation
  │
  ▼
Service ──── Transaction / Business Logic
  │
  ├── Repository ── JPA ── MySQL
  ├── Redis
  └── External Services
      ├── Kakao / CoolSMS
      ├── MyData AS·RS
      ├── 공공 일자리 Open API
      ├── AWS S3
      └── SMTP
```

### 요청과 응답

1. 공개 API를 제외한 요청은 `JwtAuthFilter`에서 Bearer Access Token을 검증합니다.
2. 인증된 사용자 엔티티는 `@AuthenticationPrincipal`을 통해 컨트롤러에 전달됩니다.
3. 컨트롤러는 입력 검증과 HTTP 응답을 담당하고, 서비스가 트랜잭션과 비즈니스 규칙을 처리합니다.
4. 일반 응답은 아래의 공통 포맷을 사용합니다.

```json
{
  "isSuccess": true,
  "data": {},
  "error": null
}
```

실패 시에는 `GlobalExceptionHandler`가 HTTP 상태와 서비스 에러 코드를 포함한 응답으로 변환합니다.

```json
{
  "isSuccess": false,
  "data": null,
  "error": {
    "code": "AUTH_015",
    "message": "사용자를 찾을 수 없습니다."
  }
}
```

## 핵심 동작

### JWT 인증

- Access Token과 Refresh Token은 서로 다른 Secret과 만료 시간을 사용합니다.
- 비밀번호는 BCrypt로 단방향 암호화합니다.
- 서버 세션을 생성하지 않는 Stateless 방식입니다.
- Refresh Token은 DB에 저장하며, 로그아웃 시 토큰을 무효화합니다.
- 로그인·회원가입·토큰 재발급·OAuth 콜백·Swagger·영상 편지 공개 링크 등만 인증 없이 접근할 수 있습니다.

인증이 필요한 API는 다음 헤더를 사용합니다.

```http
Authorization: Bearer <access-token>
```

### MyData 연동

1. 서버가 MyData Authorization Server의 인가 URL을 생성합니다.
2. 사용자가 동의하면 Callback의 Authorization Code를 토큰으로 교환합니다.
3. 짧게 유지되는 Access Token은 TTL과 함께 Redis에 저장합니다.
4. Refresh Token과 Scope는 MySQL의 `my_data` 테이블에 저장합니다.
5. 자산 조회 시 Access Token이 없거나 만료되었다면 Refresh Token으로 갱신한 뒤 Resource Server를 호출합니다.


### 채용 정보

`JobOpenApiClient`가 공공 Open API를 호출하고 응답을 내부 DTO로 변환합니다. 목록 조회 결과의 부가 정보는 Redis에 캐시하며, 상세 조회 시 목록 정보와 상세 응답을 조합합니다.

### 영상 편지

- 대용량 영상은 서버를 경유하지 않고 S3 Presigned URL로 멀티파트 업로드합니다.
- 업로드 초기화 → Part URL 발급 → 업로드 완료 순서로 동작합니다.
- 수신인별 일회성 접근 링크와 발송 예정 시간을 저장합니다.
- 매분 실행되는 `InheritanceScheduler`가 발송 대상자를 조회해 SMTP로 영상 링크를 전송합니다.
- 공개 영상 링크는 접근 토큰을 검증한 뒤 제한된 시간의 S3 조회 URL로 연결됩니다.

## 프로젝트 구조

```text
src
├── main
│   ├── java/com/know_who_how/main_server
│   │   ├── auth              # 회원가입, 로그인, SMS, Kakao OAuth2
│   │   ├── user              # 사용자·자산·키워드 관리
│   │   ├── asset_management  # 포트폴리오와 금융 시뮬레이션
│   │   ├── mydata            # MyData 인증 및 자산 조회
│   │   ├── job               # 일자리 Open API 연동
│   │   ├── inheritance       # 상속 계획, 영상, 수신인, 예약 발송
│   │   └── global
│   │       ├── config        # Security, Redis, AWS, WebClient 설정
│   │       ├── dto           # 공통 API 응답
│   │       ├── entity        # JPA 엔티티
│   │       ├── exception     # 전역 예외 처리와 에러 코드
│   │       ├── jwt           # JWT 생성·검증·필터
│   │       └── util          # Redis, Cookie 유틸리티
│   └── resources
│       └── data.sql          # 초기 기준 데이터
└── test                     # 도메인별 단위·통합 테스트
```

## 시작하기

### 사전 요구사항

- JDK 17
- MySQL
- Redis
- 사용할 외부 서비스의 인증 정보
  - Kakao OAuth2
  - CoolSMS
  - 공공 일자리 Open API
  - MyData Authorization/Resource Server
  - AWS S3
  - SMTP


    

### 로컬 실행

Windows:

```shell
gradlew.bat bootRun --args="--spring.profiles.active=local"
```

macOS/Linux:

```shell
./gradlew bootRun --args='--spring.profiles.active=local'
```

빌드한 JAR 실행:

```shell
./gradlew clean build
java -jar build/libs/main-server-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

## API 문서

애플리케이션 실행 후 Swagger UI에서 요청·응답 스키마와 각 API의 상세 설명을 확인할 수 있습니다.

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

주요 엔드포인트는 다음과 같습니다.

| Method | Endpoint | 설명 | 인증 |
| --- | --- | --- | --- |
| `POST` | `/api/v1/auth/signup/submit` | 회원가입 | 불필요 |
| `POST` | `/api/v1/auth/login` | 일반 로그인 | 불필요 |
| `POST` | `/api/v1/auth/reissue` | Access Token 재발급 | 불필요 |
| `GET` | `/login/oauth2/code/{registrationId}` | 소셜 로그인 Callback | 불필요 |
| `GET/PATCH` | `/api/v1/user/...` | 사용자·자산·키워드 관리 | 필요 |
| `GET` | `/api/v1/asset-management/portfolio` | 포트폴리오 진단 | 필요 |
| `POST` | `/api/v1/asset-management/simulate/saving` | 적금 시뮬레이션 | 필요 |
| `POST` | `/api/v1/asset-management/simulate/deposit` | 예금 시뮬레이션 | 필요 |
| `GET` | `/api/v1/my-data/authorize` | MyData 연동 시작 | 필요 |
| `GET` | `/api/v1/resource/my-data` | MyData 자산 조회 | 필요 |
| `GET` | `/api/v1/jobs` | 채용 공고 검색 | 필요 |
| `GET` | `/api/v1/jobs/{jobId}` | 채용 공고 상세 | 필요 |
| `POST/GET` | `/api/v1/inheritance/plan` | 상속 계획 저장·조회 | 필요 |
| `POST` | `/api/v1/inheritance/{id}/video/upload/init` | 영상 업로드 초기화 | 필요 |
| `GET` | `/api/v1/inheritance/video-letter` | 수신인 영상 열람 | 접근 토큰 |

## 테스트와 커버리지

전체 테스트:

```shell
./gradlew test
```

테스트 완료 후 JaCoCo HTML 보고서는 다음 위치에 생성됩니다.

```text
build/reports/jacoco/test/html/index.html
```

테스트는 `application-test.yml`과 H2의 MySQL 호환 모드를 사용합니다. 외부 인프라에 의존하는 객체는 테스트별 Mock 구성이 필요합니다.

## Docker

JAR 이미지 빌드:

```shell
./gradlew clean bootJar
docker build -f docker/Dockerfile -t knowwhohow-main .
```

저장소의 `docker-compose.yml`은 다음 구성을 전제로 합니다.

- `${DOCKER_USERNAME}/knowwhohow-main:latest` 백엔드 이미지
- 비밀번호가 설정된 Redis
- 외부 MySQL 및 외부 서비스 설정
- `./nginx/conf`에 준비된 Nginx 설정

```shell
docker compose up -d
```

> 현재 Compose 파일은 MySQL 컨테이너를 생성하지 않으며, 저장소에도 `nginx/conf`가 포함되어 있지 않습니다. 배포 환경에서 DB 연결 정보와 Nginx 설정을 별도로 주입해야 합니다.

## 데이터 모델 개요

- `users`를 중심으로 `user_info`, `assets`, `pension`, `my_data`, `refresh_token`이 사용자 정보를 구성합니다.
- `keyword`와 `term`은 각각 연결 엔티티를 통해 사용자와 다대다 관계를 표현합니다.
- `inheritance`는 사용자별 상속 계획이며 `inheritance_videos`, `inheritance_recipients`와 연결됩니다.
- `financial_product`는 예금·적금 상품과 구간별 금리 계산에 필요한 정보를 보관합니다.

## 운영 시 확인 사항

- `ddl-auto`는 운영 DB 마이그레이션 정책에 맞게 설정하고, 가능하면 Flyway/Liquibase 도입을 권장합니다.
- MyData OAuth2 `state` 검증을 구현해야 합니다.
- S3 버킷은 공개하지 않고 Presigned URL과 최소 권한 IAM 정책을 사용해야 합니다.
- JWT, DB, Redis, AWS, SMTP 및 외부 API Secret은 설정 파일에 커밋하지 말고 Secret Manager나 환경변수로 주입해야 합니다.
- 예약 작업은 인스턴스마다 실행됩니다. 서버를 여러 대 운영한다면 중복 발송 방지를 위한 분산 락이 필요합니다.
- 애플리케이션 로그에 Authorization Code, Token, 개인정보가 기록되지 않도록 운영 로깅 정책을 점검해야 합니다.

