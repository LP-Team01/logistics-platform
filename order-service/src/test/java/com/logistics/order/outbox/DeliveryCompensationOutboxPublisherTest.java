package com.logistics.order.outbox;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 배송 보상 Outbox의 Kafka 발행을 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
class DeliveryCompensationOutboxPublisherTest {

    @Mock
    private DeliveryCompensationOutboxRepository repository;

    @Mock
    private KafkaTemplate<
            String,
            DeliveryCompensationEvent
    > kafkaTemplate;

    @InjectMocks
    private DeliveryCompensationOutboxPublisher publisher;

    @BeforeEach
    void setUp() {
        // @Value로 주입되는 Topic 이름을 테스트에서 직접 설정
        ReflectionTestUtils.setField(
                publisher,
                "topic",
                "delivery-compensation"
        );
    }

    /**
     * Kafka 발행 성공 시 Outbox를 발행 완료 처리합니다.
     */
    @Test
    void marksOutboxPublishedWhenKafkaSendSucceeds() {
        UUID orderId = UUID.randomUUID();

        DeliveryCompensationOutbox outbox =
                new DeliveryCompensationOutbox(orderId);

        when(repository
                .findTop100ByPublishedAtIsNullOrderByCreatedAtAsc())
                .thenReturn(List.of(outbox));

        CompletableFuture<
                SendResult<
                        String,
                        DeliveryCompensationEvent
                >
        > completedFuture =
                CompletableFuture.completedFuture(null);

        when(kafkaTemplate.send(
                eq("delivery-compensation"),
                eq(orderId.toString()),
                any(DeliveryCompensationEvent.class)
        )).thenReturn(completedFuture);

        publisher.publishPending();

        // Kafka 전송이 실행됐는지 확인
        verify(kafkaTemplate).send(
                eq("delivery-compensation"),
                eq(orderId.toString()),
                any(DeliveryCompensationEvent.class)
        );

        // 발행 성공 시간이 기록됐는지 확인
        assertNotNull(
                outbox.getPublishedAt()
        );
    }

    /**
     * Kafka 발행 실패 시 Outbox를 미발행 상태로 유지합니다.
     * 의도적으로 만든 에러
     */
    @Test
    void keepsOutboxPendingWhenKafkaSendFails() {
        UUID orderId = UUID.randomUUID();

        DeliveryCompensationOutbox outbox =
                new DeliveryCompensationOutbox(orderId);

        when(repository
                .findTop100ByPublishedAtIsNullOrderByCreatedAtAsc())
                .thenReturn(List.of(outbox));

        // Kafka 전송 실패 상황
        CompletableFuture<
                SendResult<
                        String,
                        DeliveryCompensationEvent
                >
        > failedFuture =
                CompletableFuture.failedFuture(
                        new RuntimeException(
                                "Kafka connection failed"
                        )
                );

        when(kafkaTemplate.send(
                eq("delivery-compensation"),
                eq(orderId.toString()),
                any(DeliveryCompensationEvent.class)
        )).thenReturn(failedFuture);

        publisher.publishPending();

        // 발행 실패 시 publishedAt은 NULL로 유지
        assertNull(
                outbox.getPublishedAt()
        );
    }

}
