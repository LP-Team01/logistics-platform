# 🚚 물류관리 플랫폼 (Logistics Platform)

> Spring Boot 기반 MSA 구조로 구현하는 물류·주문·배송 관리 플랫폼

![Java](https://img.shields.io/badge/Java-17-007396?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.16-6DB33F?logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-4169E1?logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-7.4-DC382D?logo=redis&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)
![GitHub Actions](https://img.shields.io/badge/GitHub_Actions-CI-2088FF?logo=githubactions&logoColor=white)

## 📌 프로젝트 소개

허브, 업체, 상품, 주문, 배송 정보를 하나의 흐름으로 관리하는 물류관리 백엔드 플랫폼입니다.

서비스별 책임과 데이터베이스를 분리한 MSA 구조를 사용하며, API Gateway를 통해 외부 요청을 단일 진입점으로 관리합니다. 기본 기능은 REST 기반 동기 통신으로 구현하고, AI 알림 서비스에서는 Spring AI와 Gemini, pgvector 기반 RAG 및 Slack 알림 연동을 지원할 예정입니다.

Kafka와 AWS 배포는 기본 기능 완료 후 일정에 따라 적용하는 선택 사항입니다.

## 🎯 주요 목표

- 서비스별 책임과 데이터 저장소가 분리된 MSA 구성
- API Gateway와 JWT를 이용한 인증·인가 및 공통 요청 처리
- Eureka를 이용한 서비스 등록·탐색
- Config Server를 이용한 서비스 설정 중앙 관리
- PostgreSQL 및 pgvector를 이용한 서비스 데이터와 RAG 데이터 관리
- Redis를 이용한 캐시 및 인증 보조 데이터 관리
- Spring AI와 Gemini를 이용한 AI 기능 구현
- Slack을 이용한 주요 업무 알림 전송
- Docker Compose를 이용한 동일한 로컬 개발 환경 제공
- GitHub Actions를 이용한 테스트 및 빌드 자동화

## 🏗️ 인프라 구성도

![물류관리 시스템 인프라 구성도](docs/infrastructure-diagram.png)

현재는 Docker Compose 기반의 로컬 개발 환경을 사용합니다. 기본 기능 구현 후 시간이 허용되면 Docker 이미지를 AWS ECR에 저장하고 ECS Fargate로 배포할 예정입니다.

## 🧩 서비스 구성

| 애플리케이션 | 포트 | 역할 |
|---|---:|---|
| API Gateway | `8080` | 외부 요청 진입점, 라우팅, JWT 검증, 공통 필터 |
| Eureka Server | `8761` | 마이크로서비스 등록 및 탐색 |
| Config Server | `8888` | 서비스별 설정 중앙 관리 |
| User Service | `8081` | 사용자, 인증 및 권한 관리 |
| Hub Service | `8082` | 허브 및 허브 이동 정보 관리 |
| Company Service | `8083` | 업체, 상품 및 재고 정보 관리 |
| Order Service | `8084` | 주문 생성 및 상태 관리 |
| Delivery Service | `8085` | 배송 및 배송 담당자 관리 |
| AI Notification Service | `8086` | Spring AI, RAG, Gemini 및 Slack 알림 연동 |

## 🛠️ 기술 스택

### Backend

- Java 17
- Spring Boot 3.5.16
- Spring Cloud 2025.0.0
- Spring Cloud Gateway
- Netflix Eureka
- Spring Cloud Config
- Spring Security, JWT
- Spring Data JPA
- Springdoc OpenAPI / Swagger UI

### Data & AI

- PostgreSQL 17
- pgvector
- Redis 7.4
- Spring AI 1.1.8
- Google Gemini
- Slack API

### Infrastructure & Collaboration

- Docker, Docker Compose
- Git, GitHub
- GitHub Actions
- AWS ECR, ECS Fargate — 향후 선택 적용
- Kafka — 확장성만 고려하며 현재 구현 범위에서 제외

## 🔄 요청 흐름

```text
Client
  └─ HTTPS / REST
      └─ API Gateway
          ├─ JWT 검증 및 사용자 정보 전달
          ├─ Eureka 기반 서비스 탐색
          └─ 각 도메인 서비스 호출
               ├─ PostgreSQL / pgvector
               ├─ Redis
               └─ AI Notification Service
                    ├─ Gemini
                    └─ Slack
```

## 🗂️ 프로젝트 구조

```text
logistics-platform/
├─ api-gateway/
├─ eureka-server/
├─ config-server/
├─ config-repository/
├─ user-service/
├─ hub-service/
├─ company-service/
├─ order-service/
├─ delivery-service/
├─ ai-notification-service/
├─ infrastructure/
│  ├─ docker-compose.yml
│  ├─ Dockerfile
│  └─ postgres/init.sql
├─ docs/
├─ .github/workflows/ci.yml
├─ build.gradle
└─ settings.gradle
```

## 🚀 시작하기

### 1. 저장소 복제

```bash
git clone https://github.com/LP-Team01/logistics-platform.git
cd logistics-platform
```

### 2. 환경 변수 설정

PowerShell:

```powershell
Copy-Item .env.example .env
```

macOS / Linux:

```bash
cp .env.example .env
```

`.env` 파일에서 다음 값을 환경에 맞게 변경합니다.

```dotenv
POSTGRES_USER=logistics
POSTGRES_PASSWORD=change-me
JWT_SECRET=replace-with-at-least-32-byte-secret

GEMINI_API_KEY=
GEMINI_CHAT_MODEL=gemini-2.5-flash
GEMINI_EMBEDDING_MODEL=gemini-embedding-001

SLACK_BOT_TOKEN=
```

> `.env`와 실제 Secret은 Git에 커밋하지 않습니다.

### 3. 테스트 및 JAR 빌드

Windows:

```powershell
.\gradlew.bat clean test bootJar
```

macOS / Linux:

```bash
./gradlew clean test bootJar
```

### 4. Docker Compose 실행

```bash
docker compose --env-file .env -f infrastructure/docker-compose.yml up --build -d
```

실행 상태 확인:

```bash
docker compose --env-file .env -f infrastructure/docker-compose.yml ps
```

종료:

```bash
docker compose --env-file .env -f infrastructure/docker-compose.yml down
```

데이터 볼륨까지 삭제해야 할 때만 다음 명령을 사용합니다.

```bash
docker compose --env-file .env -f infrastructure/docker-compose.yml down -v
```

## 🔎 접속 URL

| 항목 | URL |
|---|---|
| API Gateway | <http://localhost:8080> |
| Gateway Health Check | <http://localhost:8080/actuator/health> |
| Eureka Dashboard | <http://localhost:8761> |
| Config Server Health Check | <http://localhost:8888/actuator/health> |
| PostgreSQL | `localhost:5432` |
| Redis | `localhost:6379` |

## 📖 API 문서

도메인 서비스는 Springdoc OpenAPI를 이용해 API 명세를 제공합니다. 현재 Gateway 통합 Swagger는 구성하지 않았으므로, 로컬에서 각 서비스의 포트로 직접 접속합니다.

### Swagger 접속 전 준비

먼저 Docker Compose로 전체 서비스를 실행합니다.

```bash
docker compose -f infrastructure/docker-compose.yml up --build -d
```

컨테이너가 정상적으로 실행 중인지 확인합니다.

```bash
docker compose -f infrastructure/docker-compose.yml ps
```

브라우저에서 담당 서비스의 Swagger UI 주소를 엽니다.

| 서비스 | Swagger UI | OpenAPI JSON |
|---|---|---|
| User Service | <http://localhost:8081/swagger-ui/index.html> | <http://localhost:8081/v3/api-docs> |
| Hub Service | <http://localhost:8082/swagger-ui/index.html> | <http://localhost:8082/v3/api-docs> |
| Company Service | <http://localhost:8083/swagger-ui/index.html> | <http://localhost:8083/v3/api-docs> |
| Order Service | <http://localhost:8084/swagger-ui/index.html> | <http://localhost:8084/v3/api-docs> |
| Delivery Service | <http://localhost:8085/swagger-ui/index.html> | <http://localhost:8085/v3/api-docs> |
| AI Notification Service | <http://localhost:8086/swagger-ui/index.html> | <http://localhost:8086/v3/api-docs> |

> API Gateway(`8080`), Eureka Server(`8761`), Config Server(`8888`)는 현재 Swagger UI 제공 대상이 아닙니다.

### Swagger에서 JWT 인증하기

인증이 필요한 API는 다음 순서로 테스트합니다.

1. 회원가입 및 로그인 API를 호출해 Access Token을 발급받습니다.
2. Swagger UI 오른쪽 위의 **Authorize** 버튼을 누릅니다.
3. 인증 입력란에 Access Token을 입력합니다.
4. **Authorize**를 누른 뒤 창을 닫습니다.
5. 테스트할 API에서 **Try it out → Execute**를 선택합니다.

Swagger 설정에 따라 입력 형식이 다를 수 있습니다.

```text
Bearer 인증 방식으로 설정된 경우: 발급받은 토큰만 입력
일반 Authorization 헤더 방식인 경우: Bearer {발급받은_토큰}
```

요청 결과가 `401 Unauthorized`라면 토큰 만료 여부와 `JWT_SECRET` 설정을 확인합니다. `403 Forbidden`이라면 로그인한 사용자의 역할이 해당 API의 요구 권한과 일치하는지 확인합니다.

### Swagger가 열리지 않을 때

```bash
docker compose --env-file .env -f infrastructure/docker-compose.yml logs -f user-service
```

- 해당 서비스 컨테이너가 실행 중인지 확인합니다.
- 서비스 포트가 다른 프로그램에서 사용 중인지 확인합니다.
- `/swagger-ui.html` 대신 `/swagger-ui/index.html`로 접속합니다.
- `/v3/api-docs`가 정상 JSON을 반환하는지 먼저 확인합니다.
- Config Server와 Eureka Server가 먼저 정상 기동됐는지 확인합니다.

## 🧪 테스트 및 CI

전체 테스트 실행:

```bash
./gradlew clean test
```

GitHub Actions는 `main`, `dev` 브랜치에 대한 Push 및 Pull Request에서 다음 작업을 수행합니다.

1. Java 17 환경 구성
2. 전체 Gradle 테스트 실행
3. 서비스별 Boot JAR 빌드

## 🌿 Git 협업 규칙

- 기본 개발 브랜치: `dev`
- 배포 기준 브랜치: `main`
- 기능 개발: `feature/{기능명}`
- 오류 수정: `fix/{기능명}`
- 문서 작업: `docs/{작업명}`
- `main`, `dev`에는 직접 Push하지 않고 Pull Request로 병합합니다.
- Pull Request는 최소 1명의 승인을 받은 후 병합합니다.
- 변경된 기능과 관련된 테스트 및 CI가 통과해야 합니다.

## 📐 개발 원칙

- 각 서비스는 자신의 데이터베이스만 직접 조회합니다.
- 다른 서비스의 테이블을 직접 조인하거나 수정하지 않습니다.
- 현재 서비스 간 통신은 REST/OpenFeign 기반으로 구성합니다.
- API 응답 형식과 예외 처리 규칙을 공통화합니다.
- 환경 변수와 인증 정보는 저장소에 커밋하지 않습니다.
- 이벤트 전환을 고려해 서비스 간 계약과 도메인 이벤트 후보를 문서화합니다.

## 🗓️ 향후 확장 계획

### AWS 배포

기본 기능과 로컬 통합 테스트가 완료된 후 일정에 따라 적용합니다.

```text
Feature Branch → Pull Request → GitHub Actions → AWS ECR → AWS ECS Fargate
```

### Kafka 도입 검토

Kafka는 현재 구현하지 않습니다. 주문·배송·알림 기능의 기본 구현이 완료되고 이벤트 분리 필요성이 확인될 때 도입 여부를 결정합니다.

도입 후보 이벤트:

| 이벤트 | 발행 서비스 | 구독 서비스 | 목적 |
|---|---|---|---|
| `OrderCreated` | Order | Delivery | 주문 이후 배송 생성 비동기화 |
| `DeliveryStatusChanged` | Delivery | AI Notification | 배송 상태 변경 알림 |
| `SlackMessageRequested` | Domain Services | AI Notification | Slack 발송 책임 분리 |

Kafka 도입 시 중복 처리 방지, 재시도, Dead Letter Topic 및 Transactional Outbox 적용 여부를 함께 검토합니다.

## 📚 관련 문서

- [인프라 설계서](docs/infrastructure.md)
- [인프라 구성도](docs/infrastructure-diagram.png)
