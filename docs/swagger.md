# Swagger / OpenAPI 사용 가이드

각 업무 서비스는 Springdoc OpenAPI로 API 명세와 Swagger UI를 제공합니다.

| 서비스 | Swagger UI | OpenAPI JSON |
|---|---|---|
| User | http://localhost:8081/swagger-ui/index.html | http://localhost:8081/v3/api-docs |
| Hub | http://localhost:8082/swagger-ui/index.html | http://localhost:8082/v3/api-docs |
| Company | http://localhost:8083/swagger-ui/index.html | http://localhost:8083/v3/api-docs |
| Order | http://localhost:8084/swagger-ui/index.html | http://localhost:8084/v3/api-docs |
| Delivery | http://localhost:8085/swagger-ui/index.html | http://localhost:8085/v3/api-docs |
| AI / Notification | http://localhost:8086/swagger-ui/index.html | http://localhost:8086/v3/api-docs |

## 작성 규칙

- 컨트롤러에는 `@Tag`로 API 그룹명과 설명을 작성합니다.
- 엔드포인트에는 `@Operation`으로 기능 요약과 상세 설명을 작성합니다.
- 요청/응답 DTO에는 `@Schema`로 필드 설명과 예시를 작성합니다.
- JWT가 필요한 API는 OpenAPI 보안 스키마를 적용합니다.
- 운영 환경에서는 Swagger UI 공개 여부를 환경 설정으로 제어합니다.

## 기본 예시

```java
@Tag(name = "주문", description = "주문 생성 및 조회 API")
@RestController
@RequestMapping("/api/orders")
class OrderController {

    @Operation(summary = "주문 생성", description = "상품과 배송 정보를 받아 주문을 생성합니다.")
    @PostMapping
    ResponseEntity<Void> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
```

서비스가 실행 중이어야 Swagger UI에 접속할 수 있습니다. 아직 컨트롤러가 없는 서비스는 빈 API 목록이 표시될 수 있습니다.
