# Logistics Platform

Spring Boot 3.5 기반 물류관리 MSA 프로젝트입니다. 기본 기능은 REST 동기 통신으로 구현하고, Redis·AI/RAG·Slack을 단계적으로 연결합니다. Kafka와 AWS ECR/ECS 배포는 기본 기능 완료 후 선택적으로 적용합니다.

## 기술 스택

- Java 17, Spring Boot 3.5.16
- Spring Cloud 2025.0.x
- PostgreSQL 17 + pgvector
- Redis
- Spring AI 1.1.8 + Gemini + pgvector RAG
- Slack API
- 선택: Apache Kafka
- Docker Compose, GitHub Actions
- 선택 배포: AWS ECR, ECS Fargate

## 서비스

| 서비스 | 포트 | 역할 |
|---|---:|---|
| Eureka Server | 8761 | 서비스 등록·탐색 |
| Config Server | 8888 | 중앙 설정 관리 |
| API Gateway | 8080 | 외부 진입점·JWT 검증 |
| User Service | 8081 | 사용자·인증 |
| Hub Service | 8082 | 허브·이동정보 |
| Company Service | 8083 | 업체·상품·재고 |
| Order Service | 8084 | 주문 |
| Delivery Service | 8085 | 배송·배송담당자 |
| AI Notification Service | 8086 | Spring AI·RAG·Slack |

## 로컬 실행

1. `.env.example`을 `.env`로 복사하고 값을 설정합니다.
2. 애플리케이션 JAR을 빌드합니다.
3. Docker Compose를 실행합니다.

```bash
./gradlew clean bootJar
docker compose -f infrastructure/docker-compose.yml up --build
```

Kafka까지 실행할 경우:

```bash
docker compose \
  -f infrastructure/docker-compose.yml \
  -f infrastructure/docker-compose.kafka.yml \
  up --build
```

## 확인 URL

- Gateway health: `http://localhost:8080/actuator/health`
- Eureka: `http://localhost:8761`
- Config Server: `http://localhost:8888/actuator/health`

## 개발 원칙

- 각 서비스는 자신의 DB만 접근합니다.
- 서비스 간 물리 FK와 DB 직접 조인을 금지합니다.
- 기본 기능은 REST로 완성한 후 Kafka를 적용합니다.
- Secret은 `.env` 또는 배포 환경의 Secret Store에서 관리합니다.
- `main`, `develop`에는 Pull Request로만 병합합니다.

자세한 내용은 [`docs/infrastructure.md`](docs/infrastructure.md)를 확인하세요.
