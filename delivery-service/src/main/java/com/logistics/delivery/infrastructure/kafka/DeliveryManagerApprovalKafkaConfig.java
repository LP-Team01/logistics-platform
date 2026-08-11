package com.logistics.delivery.infrastructure.kafka;

import java.util.Map;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;


@Configuration
public class DeliveryManagerApprovalKafkaConfig {

    @Bean
    public ConsumerFactory<String, DeliveryManagerApprovalRequestedEvent> deliveryManagerApprovalConsumerFactory(
        KafkaProperties kafkaProperties
    ) {
        Map<String, Object> props = kafkaProperties.buildConsumerProperties(null);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, DeliveryManagerApprovalRequestedEvent.class.getName());
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, DeliveryManagerApprovalRequestedEvent>
        deliveryManagerApprovalKafkaListenerContainerFactory(
            KafkaProperties kafkaProperties,
            ConsumerFactory<String, DeliveryManagerApprovalRequestedEvent> deliveryManagerApprovalConsumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, DeliveryManagerApprovalRequestedEvent> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(deliveryManagerApprovalConsumerFactory);
        factory.getContainerProperties().setAckMode(kafkaProperties.getListener().getAckMode());
        return factory;
    }
}
