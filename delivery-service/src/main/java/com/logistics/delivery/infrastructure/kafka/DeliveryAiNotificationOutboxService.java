package com.logistics.delivery.infrastructure.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

//DELIVERY_CREATED 이벤트를 아웃박스에 적재
@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryAiNotificationOutboxService {

    private final DeliveryAiNotificationOutboxRepository repository;
    private final ObjectMapper objectMapper;

    public void save(DeliveryAiNotificationEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            repository.save(new DeliveryAiNotificationOutbox(event.eventId(), event.deliveryId(), payload));
        } catch (JsonProcessingException exception) {
            log.warn("DELIVERY_CREATED 이벤트 직렬화 실패로 아웃박스 저장을 건너뜁니다. deliveryId={}",
                event.deliveryId(), exception);
        }
    }
}
