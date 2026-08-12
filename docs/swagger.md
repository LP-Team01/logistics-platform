# Swagger / OpenAPI 사용 가이드

## 통합 Swagger

API Gateway가 각 서비스의 OpenAPI 문서를 한 화면에 모아 제공합니다.

| 환경 | Swagger UI |
|---|---|
| 로컬 | <http://localhost:8080/swagger-ui/index.html> |
| 운영 | <https://api.logistics-platfom.shop/swagger-ui/index.html> |

오른쪽 위 `Select a definition`에서 User, Hub, Company, Order, Delivery, AI/Notification 서비스를 선택합니다. 문서 조회 경로는 Gateway의 `/docs/{service}/v3/api-docs`이며 Eureka에 해당 서비스가 등록되어 있어야 정상 응답합니다.

Swagger에서 인증 API를 호출할 때 `Authorize`에 로그인 API에서 받은 JWT access token을 입력합니다. 브라우저에서 내부 서비스 전용 키를 직접 입력하거나 노출하지 않습니다.

## 서비스 직접 접속(로컬 디버깅)

| 서비스 | Swagger UI | OpenAPI JSON |
|---|---|---|
| User | <http://localhost:8081/swagger-ui/index.html> | <http://localhost:8081/v3/api-docs> |
| Hub | <http://localhost:8082/swagger-ui/index.html> | <http://localhost:8082/v3/api-docs> |
| Company | <http://localhost:8083/swagger-ui/index.html> | <http://localhost:8083/v3/api-docs> |
| Order | <http://localhost:8084/swagger-ui/index.html> | <http://localhost:8084/v3/api-docs> |
| Delivery | <http://localhost:8085/swagger-ui/index.html> | <http://localhost:8085/v3/api-docs> |
| AI / Notification | <http://localhost:8086/swagger-ui/index.html> | <http://localhost:8086/v3/api-docs> |

Docker Compose 운영 구성은 서비스 포트를 호스트에 공개하지 않으므로 운영에서는 Gateway 통합 Swagger를 사용합니다.

## 503 오류 확인

`/docs/{service}/v3/api-docs`가 503이면 대부분 대상 서비스가 Eureka에 등록되지 않았거나 아직 healthy 상태가 아닙니다.

```bash
docker compose --env-file .env.prod \
  -f infrastructure/docker-compose.yml \
  -f infrastructure/docker-compose.prod.yml \
  -f infrastructure/docker-compose.ecr.yml \
  ps

docker logs --tail=200 logistics-platform-delivery-service-1
```

서비스의 DB/Flyway/환경변수 오류를 먼저 해결한 뒤 Eureka 등록 상태와 Gateway 문서 URL을 다시 확인합니다.

## 작성 규칙

- Controller에는 `@Tag`로 API 그룹과 설명을 작성합니다.
- Endpoint에는 `@Operation`으로 요약과 상세 설명을 작성합니다.
- 요청·응답 DTO에는 `@Schema`로 필드 설명과 예시를 작성합니다.
- JWT가 필요한 API에는 OpenAPI 보안 스키마를 적용합니다.
- `SWAGGER_ENABLED=false`이면 공통 Config Server 설정을 통해 각 서비스의 API 문서와 UI가 함께 비활성화됩니다.
