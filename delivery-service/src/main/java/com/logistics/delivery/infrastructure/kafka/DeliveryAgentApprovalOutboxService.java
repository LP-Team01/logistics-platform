package com.logistics.delivery.infrastructure.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


 //유저 승인 사가의 결과 이벤트(APPROVED/APPROVAL_FAILED)를 아웃박스에 적재
 //결과 이벤트가 실제 처리 결과와 항상 같은 커밋 단위로 — user-service가 APPROVING에서
 //영원히 멈추지 않으려면 실패 케이스도 반드시 결과 이벤트가 발행
@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryAgentApprovalOutboxService {

    private final DeliveryAgentApprovalOutboxRepository repository;
    private final ObjectMapper objectMapper;

    public void save(DeliveryAgentApprovalResultEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            repository.save(new DeliveryAgentApprovalOutbox(event.eventId(), event.agentId(), payload));
        } catch (JsonProcessingException exception) {
            log.error("승인 결과 이벤트 직렬화 실패로 아웃박스 저장을 건너뜁니다. agentId={}, eventType={}",
                event.agentId(), event.eventType(), exception);
        }
    }
}
