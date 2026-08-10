package com.logistics.user.kafka;

import com.logistics.user.kafka.consumer.DeliveryApprovalResultEvent;
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

    private final KafkaTemplate<String, DeliveryApprovalEvent> kafkaTemplate;
    private final UserRepository userRepository;

    @Value("${kafka.topic.delivery-agent-approval-requested}")
    private String approvalTopic;


    public void approvalDeliveryAgent(String key, DeliveryApprovalEvent event){
        kafkaTemplate.send(approvalTopic, key, event);
    }


    @CacheEvict(value = "users", key = "#event.userId()")
    @Transactional
    @KafkaListener(
        topics = "${kafka.topic.delivery-agent-approval-result}",
        containerFactory = "deliveryApprovalResultListenerFactory"
    )
    public void listenApprovalResult(DeliveryApprovalResultEvent event){
        User user = userRepository.findByUserIdAndDeletedAtIsNullAndStatus(event.userId(), UserStatus.APPROVING)
            .orElse(null);
        if(user == null){
            log.warn("listenApprovalResult의 User를 찾을 수 없습니다.");
            return;
        }

        if (event.success()) {
            user.approve();
        }else{
            user.revertToPending();
        }
    }
}
