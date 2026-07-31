# 인프라 개요

## 필수 구성

- Spring Boot 3.5.16 / Java 17
- Spring Cloud 2025.0.x
- Gateway, Eureka, Config Server
- PostgreSQL + pgvector
- Redis
- Docker Compose
- GitHub Actions

## 선택 구성

- Kafka: 기본 기능 완료 후 배송 상태 알림부터 적용
- AWS: ECR 이미지 저장 후 ECS Fargate 배포

## 통신 원칙

1. 클라이언트는 API Gateway로만 접근합니다.
2. Gateway가 JWT를 검증하고 `X-User-Id`, `X-User-Role`, `X-Request-Id`를 전달합니다.
3. 서비스 간 동기 통신은 REST/OpenFeign을 사용합니다.
4. Kafka는 알림과 후속 처리부터 점진적으로 적용합니다.
5. 서비스는 다른 서비스의 DB를 직접 조회하지 않습니다.

## Kafka 적용 후보

| 이벤트 | 발행 | 소비 | 목적 |
|---|---|---|---|
| DeliveryStatusChanged | Delivery | AI Notification | 배송 알림 |
| SlackMessageRequested | Domain Services | AI Notification | Slack 비동기 발송 |
| OrderCreated | Order | Delivery | 선택적 배송 생성 비동기화 |

중요 이벤트에는 중복 처리, 재시도, DLT 및 필요시 Transactional Outbox를 적용합니다.
