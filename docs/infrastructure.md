# 인프라 구성

## 필수 구성

- Spring Boot 3.5.16 / Java 17
- API Gateway, Eureka, Config Server
- PostgreSQL 17 + pgvector
- Redis 7.4
- Kafka 3.9.2 (KRaft)
- Prometheus / Grafana (선택형 monitoring 프로필)
- Docker Compose
- GitHub Actions

AWS ECR, ECS Fargate, RDS 배포는 선택 사항이며 로컬 환경은 Docker Compose만으로 실행할 수 있습니다.

## 환경 분리

Docker Compose가 지정된 env 파일을 읽어 각 컨테이너에 환경 변수를 전달합니다.

```powershell
# 로컬
docker compose --env-file .env -f infrastructure/docker-compose.yml up --build -d

# 운영/RDS
docker compose --env-file .env.prod -f infrastructure/docker-compose.yml -f infrastructure/docker-compose.rds.yml up --build -d
```

- `.env`: 로컬 환경, `SPRING_PROFILES_ACTIVE=local`
- `.env.prod`: 운영 환경, `SPRING_PROFILES_ACTIVE=prod`
- 실제 env 파일은 Git에 커밋하지 않습니다.
- 운영 Secret은 가능하면 AWS Secrets Manager 또는 ECS Secret으로 주입합니다.
- Config Server는 `native` 프로필로 `/config`에 마운트된 `config-repository`를 제공합니다.
- 운영 Profile에는 graceful shutdown, Health 상세정보 비공개, INFO 로그 레벨을 적용합니다.

## 서비스 통신 원칙

1. 외부 요청은 API Gateway를 통해서만 접근합니다.
2. Gateway가 JWT를 검증하고 사용자 및 요청 식별 헤더를 전달합니다.
3. 서비스 간 동기 통신은 REST/OpenFeign을 사용합니다.
4. 서비스는 다른 서비스의 DB를 직접 조회하지 않습니다.
5. 내부 API는 `INTERNAL_SERVICE_KEY`와 `HUB_INTERNAL_SERVICE_KEY`로 보호합니다.

## Kafka

`kafka-init`이 다음 Topic을 명시적으로 생성합니다.

| Topic | Producer | Consumer | 목적 |
|---|---|---|---|
| `delivery-compensation` | Order | Delivery | 배송 보상 취소 재시도 |
| `delivery-ai-notification` | Delivery | AI Notification | 배송 이벤트 알림 |

배송 보상 이벤트는 Order Outbox에 먼저 저장한 뒤 Kafka로 발행합니다. Delivery Consumer는 재시도 후에도 실패하면 DLT로 이동시킵니다. DLT는 운영 모니터링과 재처리 절차가 필요합니다.

DLT 파티션별 적재 Offset 확인:

```powershell
.\infrastructure\kafka-dlt.ps1 status
```

장애 원인을 해결한 뒤 DLT 메시지를 원본 Topic으로 재전송합니다. 한 번에 최대 100건으로 제한하며 전용 Consumer Group Offset을 사용해 같은 메시지의 반복 재전송을 방지합니다.

```powershell
.\infrastructure\kafka-dlt.ps1 replay -MaxMessages 10
```

재전송 후 Delivery Service 로그와 DLT 상태를 다시 확인합니다. 자동 무한 재처리는 장애 확산을 막기 위해 사용하지 않습니다.

## 로컬 실행 및 확인

```powershell
.\gradlew.bat clean bootJar
docker compose --env-file .env -f infrastructure/docker-compose.yml up --build -d
docker compose --env-file .env -f infrastructure/docker-compose.yml ps
```

Prometheus와 Grafana를 포함해 실행:

```powershell
docker compose --profile monitoring --env-file .env -f infrastructure/docker-compose.yml up --build -d
```

- Prometheus: <http://localhost:9090>
- Grafana: <http://localhost:3000>
- Grafana에는 `http://prometheus:9090` Data Source가 자동 등록됩니다.
- 운영에서는 `GRAFANA_ADMIN_PASSWORD`를 반드시 별도 Secret으로 설정합니다.

정상 상태:

- PostgreSQL, Redis, Kafka와 모든 애플리케이션: `healthy`
- `kafka-init`: `Exited (0)`

주요 확인 주소:

- Gateway Health: Docker 내부 `http://api-gateway:9091/actuator/health`
- Gateway Metrics: Docker 내부 `http://api-gateway:9091/actuator/prometheus`
- Eureka: <http://localhost:8761>
- Config Server: <http://localhost:8888/order-service/local>

Kafka Topic 확인:

```powershell
docker compose --env-file .env -f infrastructure/docker-compose.yml exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:29092 --list
```

Flyway 적용 확인 예시:

```powershell
docker compose --env-file .env -f infrastructure/docker-compose.yml exec postgres psql -U logistics -d order_db -c "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;"
```

## CI

Pull Request와 `main`, `dev` 브랜치 Push에서 다음을 검증합니다.

1. 전체 Gradle 테스트
2. 실행 JAR 생성
3. Docker Compose 설정 유효성
4. 전체 서비스 Docker 이미지 빌드
5. 전체 Compose 기동 및 Gateway, Config Server, Eureka, Kafka Smoke Test
