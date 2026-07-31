# Kafka 이벤트 명세

Kafka는 기본값으로 비활성화합니다. 필수 기능 완료 후 `DeliveryStatusChanged`부터 적용합니다.

## 공통 Envelope

```json
{
  "eventId": "uuid",
  "eventType": "DeliveryStatusChanged",
  "eventVersion": 1,
  "aggregateId": "uuid",
  "occurredAt": "2026-07-31T00:00:00Z",
  "requestId": "uuid",
  "payload": {}
}
```

## Topic

- `order-events`
- `delivery-events`
- `notification-events`
- `document-events`

## 규칙

- JPA Entity를 직접 전송하지 않습니다.
- JWT와 Secret을 이벤트에 포함하지 않습니다.
- Consumer는 `eventId`를 기준으로 멱등성을 보장합니다.
- 재시도 종료 후 `*-dlt` Topic으로 이동합니다.
