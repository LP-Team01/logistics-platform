package com.logistics.user.kafka;

import com.logistics.user.kafka.consumer.DeliveryAgentApprovalResultEvent;
import com.logistics.user.kafka.consumer.DeliveryAgentApprovalResultEventType;
import com.logistics.user.user.entity.User;
import com.logistics.user.user.entity.UserStatus;
import com.logistics.user.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaService {

    private final UserRepository userRepository;

    @RetryableTopic(
        attempts = "5",
        backoff = @Backoff(delay = 5_000, multiplier = 2),
        listenerContainerFactory = "deliveryApprovalResultListenerFactory"
    )
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

    @DltHandler
    public void handleApprovalResultDlt(DeliveryAgentApprovalResultEvent event){
        log.error("배송담당자 승인 결과 처리 최종 실패(DLT). agentId={}, eventType={}",
            event.agentId(), event.eventType());
    }
}
