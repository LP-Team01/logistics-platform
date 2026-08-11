package com.logistics.user.kafka;

import com.logistics.user.kafka.consumer.DeliveryAgentApprovalResultEvent;
import com.logistics.user.kafka.consumer.DeliveryAgentApprovalResultEventType;
import com.logistics.user.user.entity.User;
import com.logistics.user.user.entity.UserStatus;
import com.logistics.user.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaService {

    private final KafkaTemplate<String, DeliveryManagerApprovalRequestedEvent> kafkaTemplate;
    private final UserRepository userRepository;

    @Value("${kafka.topic.delivery-manager-approval-requested}")
    private String approvalTopic;


    public void approvalDeliveryAgent(String key, DeliveryManagerApprovalRequestedEvent event){
        kafkaTemplate.send(approvalTopic, key, event);
    }


    @CacheEvict(value = "users", key = "#event.agentId()")
    @Transactional
    @KafkaListener(
        topics = "${kafka.topic.delivery-agent-approval-result}",
        containerFactory = "deliveryApprovalResultListenerFactory"
    )
    public void listenApprovalResult(DeliveryAgentApprovalResultEvent event){
        User user = userRepository.findByUserIdAndDeletedAtIsNullAndStatus(event.agentId(), UserStatus.APPROVING)
            .orElse(null);
        if(user == null){
            log.warn("listenApprovalResult의 User를 찾을 수 없습니다. agentId={}", event.agentId());
            return;
        }

        if (event.eventType() == DeliveryAgentApprovalResultEventType.APPROVED) {
            user.approve();
        }else{
            log.warn("배송담당자 승인 실패. agentId={}, reasonCode={}, reasonMessage={}",
                event.agentId(), event.failureReasonCode(), event.failureReasonMessage());
            user.revertToPending();
        }
    }
}
