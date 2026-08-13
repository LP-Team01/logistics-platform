package com.logistics.user.kafka.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.user.kafka.DeliveryManagerApprovalRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

// 배송담당자 승인 요청 이벤트를 user.approving()과 같은 트랜잭션 안에서 아웃박스에 적재
@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryManagerApprovalOutboxService {

    private final DeliveryManagerApprovalOutboxRepository repository;
    private final ObjectMapper objectMapper;

    public void save(DeliveryManagerApprovalRequestedEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            repository.save(new DeliveryManagerApprovalOutbox(event.eventId(), event.agentId(), payload));
        } catch (JsonProcessingException exception) {
            log.error("승인 요청 이벤트 직렬화 실패로 아웃박스 저장을 건너뜁니다. agentId={}", event.agentId(), exception);
        }
    }
}
