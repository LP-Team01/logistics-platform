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

- AWS: ECR 이미지 저장 후 ECS Fargate 배포

## 통신 원칙

1. 클라이언트는 API Gateway로만 접근합니다.
2. Gateway가 JWT를 검증하고 `X-User-Id`, `X-User-Role`, `X-Hub-Id` , `X-Company-Id`, `X-Request-Id`를 전달합니다.
3. 서비스 간 동기 통신은 REST/OpenFeign을 사용합니다.
4. 현재 비동기 메시지 브로커는 도입하지 않으며 REST 기반으로 구현합니다.
5. 향후 이벤트 기반 전환을 고려하여 서비스 간 데이터 계약과 멱등성 기준을 분리합니다.
6. 서비스는 다른 서비스의 DB를 직접 조회하지 않습니다.

## 향후 이벤트 기반 확장 후보

| 이벤트 | 발행 | 소비 | 목적 |
|---|---|---|---|
| DeliveryStatusChanged | Delivery | AI Notification | 배송 알림 |
| SlackMessageRequested | Domain Services | AI Notification | Slack 비동기 발송 |
| OrderCreated | Order | Delivery | 선택적 배송 생성 비동기화 |

현재는 위 이벤트를 구현하지 않는다. 향후 Kafka 등 메시지 브로커를 도입할 경우 중복 처리, 재시도, Dead Letter Topic 및 Transactional Outbox 적용 여부를 함께 검토한다.
