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
public class HubEventsKafkaConfig {

    @Bean
    public ConsumerFactory<String, HubDeletedEvent> hubEventsConsumerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> props = kafkaProperties.buildConsumerProperties(null);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, HubDeletedEvent.class.getName());
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, HubDeletedEvent> hubEventsKafkaListenerContainerFactory(
        KafkaProperties kafkaProperties,
        ConsumerFactory<String, HubDeletedEvent> hubEventsConsumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, HubDeletedEvent> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(hubEventsConsumerFactory);
        factory.getContainerProperties().setAckMode(kafkaProperties.getListener().getAckMode());
        return factory;
    }
}
