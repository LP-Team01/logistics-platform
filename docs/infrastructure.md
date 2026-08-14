# 인프라 운영 가이드

![물류관리 시스템 인프라 구성도](infrastructure-diagram.png)

[SVG 원본](infrastructure-diagram.svg)

## 현재 구성

- 애플리케이션: Spring Boot 3.5.16, Java 17, API Gateway, Eureka, Config Server
- 데이터: Amazon RDS for PostgreSQL 17, pgvector(`ai_db`), Redis 7.4
- 메시징: Kafka 3.9.2(KRaft), Outbox, Retry Topic, DLT
- 관측성: Actuator, Prometheus, Grafana, Zipkin
- 실행 환경: Docker Compose on Amazon EC2
- 배포: GitHub Actions OIDC → Amazon ECR → AWS Systems Manager → EC2
- 외부 진입점: Caddy(80/443, 자동 TLS) → API Gateway
- 운영 도메인: `https://api.logistics-platfom.shop`

운영 EC2에서는 로컬 PostgreSQL 컨테이너를 실행하지 않습니다. 서비스별 DB는 사설망의 RDS 인스턴스에 논리적으로 분리하며 `ai_db`에만 `vector` 확장을 사용합니다.

## 네트워크와 보안

1. 외부 요청은 Caddy의 80/443 포트로만 들어오며 API Gateway로 전달됩니다.
2. Gateway가 JWT를 검증하고 신뢰할 수 없는 `X-User-*` 헤더를 제거한 뒤 사용자 정보를 전달합니다.
3. 서비스 간 내부 API는 `INTERNAL_SERVICE_KEY` 또는 `HUB_INTERNAL_SERVICE_KEY`로 검증합니다.
4. RDS는 Public Access를 비활성화하고 EC2 보안 그룹에서 오는 5432 연결만 허용합니다.
5. 운영 서버 관리는 SSH 고정 IP 허용 또는 AWS Session Manager를 사용합니다.
6. `.env`, `.env.prod`, PEM 키와 실제 Secret은 Git에 커밋하지 않습니다.

## 환경 파일

```powershell
# 로컬
Copy-Item .env.example .env
docker compose --env-file .env -f infrastructure/docker-compose.yml up --build -d
```

```bash
# EC2 운영
cp .env.prod.example .env.prod
chmod 600 .env.prod
```

운영 파일에는 RDS endpoint, Redis/JWT/내부 통신 키, 외부 API 키를 실제 값으로 설정합니다. `JWT_SECRET`은 32바이트 이상의 표준 Base64 문자열이어야 합니다.

```bash
openssl rand -base64 32 | tr -d '\n'
```

## 운영 배포

`main` 브랜치의 CI가 성공하면 `.github/workflows/publish-ecr.yml`이 실행됩니다.

```text
main push
  → GitHub Actions CI
  → GitHub OIDC로 AWS IAM Role 임시 권한 획득
  → 서비스 이미지 9개를 ECR에 commit SHA 태그로 push
  → SSM Run Command로 EC2 배포 명령 실행
  → EC2가 비활성 색상의 ECR image pull
  → Eureka, Config Server, 도메인 서비스, Gateway 순차 기동
  → 전체 서비스 Health Check 성공 시 Caddy upstream 전환
  → 진행 중 요청 대기 후 기존 스택 종료
```

EC2에서 수동으로 같은 구성을 확인할 때는 다음 파일을 함께 사용합니다.

```bash
docker compose \
  --env-file .env.prod \
  -f infrastructure/docker-compose.yml \
  -f infrastructure/docker-compose.prod.yml \
  -f infrastructure/docker-compose.ecr.yml \
  ps
```

실제 pull 및 전환은 GitHub Actions가 `infrastructure/deploy-ec2.sh`를 호출해 수행합니다. Redis, Kafka, Zipkin, Caddy와 RDS는 공유하며 Spring 애플리케이션 9개만 `logistics-blue`, `logistics-green` 프로젝트로 교대합니다. 8GiB EC2의 JVM 동시 시작 부하를 줄이기 위해 `Eureka → Config Server → User → Hub → Company → Order → Delivery → AI/Notification → API Gateway` 순서로 기동합니다. 신규 스택이 Healthy가 아니면 Caddy를 변경하지 않고 후보 스택만 정지하므로 기존 버전이 유지됩니다. GitHub Actions의 SSM 완료 대기 시간은 최대 40분입니다.

Blue와 Green은 같은 RDS를 공유합니다. Flyway 변경은 두 애플리케이션 버전이 동시에 사용할 수 있는 확장형 마이그레이션으로 작성하고, 컬럼 삭제·이름 변경 같은 파괴적 변경은 이전 색상 종료 후 별도 배포로 분리합니다.

```bash
# 현재 트래픽을 받는 색상
cat ~/.logistics-platform-deploy/active-color

# Blue/Green 컨테이너 상태
docker ps --format 'table {{.Names}}\t{{.Status}}' \
  | grep -E 'logistics-(blue|green)'

# 배포 중 RAM, Swap 및 컨테이너 메모리 확인
free -h
docker stats --no-stream
```

최초 도입 시에는 동적 upstream 파일 마운트를 적용하기 위해 Caddy가 한 번 재생성됩니다. 이후에는 `caddy reload`로 연결을 유지한 채 upstream만 전환합니다.

2026-08-14 운영 검증에서 `legacy → blue` 전환, Blue 애플리케이션 9개 Health Check, Caddy upstream 변경 및 Legacy 스택 종료를 확인했습니다.

## 서비스 통신 원칙

- 외부 요청은 API Gateway를 통과합니다.
- 동기 서비스 통신은 REST/OpenFeign을 사용합니다.
- 서비스는 다른 서비스의 DB를 직접 조회하지 않습니다.
- Config Server는 `config-repository`의 공통·서비스별 설정을 제공합니다.
- Eureka는 Docker 네트워크에서 서비스 등록과 탐색을 담당합니다.

## Kafka

`kafka-init`이 다음 Topic을 생성합니다.

| Topic | Producer | Consumer | 목적 |
|---|---|---|---|
| `delivery-compensation` | Order | Delivery | 배송 보상 취소 재처리 |
| `delivery-ai-notification` | Delivery | AI Notification | 배송 이벤트 알림 |

Order Service는 동기 보상 취소가 실패하면 Outbox에 기록하고 Kafka로 발행합니다. Delivery Consumer는 Retry Topic을 거쳐 재시도하고 최종 실패 메시지는 DLT로 보냅니다.

```powershell
# DLT 상태
.\infrastructure\kafka-dlt.ps1 status

# 원인 해결 후 최대 10건 재처리
.\infrastructure\kafka-dlt.ps1 replay -MaxMessages 10
```

## 상태 확인

```powershell
docker compose --env-file .env -f infrastructure/docker-compose.yml ps
docker compose --env-file .env -f infrastructure/docker-compose.yml exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:29092 --list
```

- Gateway Health(컨테이너 내부): `http://api-gateway:9091/actuator/health`
- Eureka(로컬): `http://localhost:8761`
- Config Server(로컬): `http://localhost:8888/order-service/local`
- Zipkin(로컬): `http://localhost:9411`
- Prometheus/Grafana: `monitoring` profile 사용 시 `http://localhost:9090`, `http://localhost:3000`

운영에서는 9091 관리 포트와 Zipkin 포트를 인터넷에 공개하지 않습니다.

## RDS와 Flyway

- DB: `user_db`, `hub_db`, `company_db`, `order_db`, `delivery_db`, `ai_db`
- 초기 DB 생성: `infrastructure/postgres/create-rds-databases.sql`
- 연결/확장 검증: `infrastructure/postgres/verify-rds.sql`
- `ai_db`: `CREATE EXTENSION IF NOT EXISTS vector;`

이미 적용된 Flyway 파일은 이름이나 내용을 수정하지 않습니다. 스키마 변경은 항상 다음 버전의 새 마이그레이션으로 추가해야 checksum validation 오류를 피할 수 있습니다.

## CI 검증 범위

Pull Request와 `main`, `dev` push에서 다음 항목을 검증합니다.

1. 전체 Gradle 테스트 및 Boot JAR 생성
2. 기본/RDS/운영 Docker Compose 문법
3. 전체 서비스 Docker 이미지 빌드
4. Compose 기동 및 Gateway, Config Server, Eureka, Kafka smoke test
