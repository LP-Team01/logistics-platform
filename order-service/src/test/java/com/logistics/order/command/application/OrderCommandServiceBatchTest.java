package com.logistics.order.command.application;

import com.logistics.order.command.dto.CreateOrderItemRequest;
import com.logistics.order.command.dto.CreateOrderRequest;
import com.logistics.order.config.InternalServiceProperties;
import com.logistics.order.domain.entity.Order;
import com.logistics.order.domain.repository.OrderRepository;
import com.logistics.order.global.auth.OrderOwnershipValidator;
import com.logistics.order.infrastructure.client.CompanyClient;
import com.logistics.order.infrastructure.client.DeliveryClient;
import com.logistics.order.infrastructure.client.ProductClient;
import com.logistics.order.infrastructure.client.UserClient;
import com.logistics.order.infrastructure.client.dto.CompanyResponse;
import com.logistics.order.infrastructure.client.dto.CreateDeliveryRequest;
import com.logistics.order.infrastructure.client.dto.CreateDeliveryResponse;
import com.logistics.order.infrastructure.client.dto.ProductResponse;
import com.logistics.order.infrastructure.client.dto.UserResponse;
import com.logistics.order.outbox.DeliveryCompensationOutboxService;
import feign.FeignException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 주문 생성 시 외부 서비스를 일괄 호출하는지 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
class OrderCommandServiceBatchTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderOwnershipValidator orderOwnershipValidator;

    @Mock
    private CompanyClient companyClient;

    @Mock
    private ProductClient productClient;

    @Mock
    private UserClient userClient;

    @Mock
    private DeliveryClient deliveryClient;

    @Mock
    private InternalServiceProperties internalServiceProperties;

    @Mock
    private DeliveryCompensationOutboxService compensationOutboxService;

    @InjectMocks
    private OrderCommandService orderCommandService;

    /**
     * 상품이 여러 개여도 Product와 Delivery의 Batch API가
     * 각각 한 번만 호출되는지 검증합니다.
     */
    @Test
    void createOrder_callsBatchApisOnlyOnce() {
        UUID userId = UUID.randomUUID();

        UUID receiverCompanyId = UUID.randomUUID();
        UUID receiverHubId = UUID.randomUUID();

        UUID supplierCompanyId = UUID.randomUUID();
        UUID supplierHubId = UUID.randomUUID();

        UUID productId1 = UUID.randomUUID();
        UUID productId2 = UUID.randomUUID();

        UUID orderId = UUID.randomUUID();

        CreateOrderRequest request =
                new CreateOrderRequest(
                        receiverCompanyId,
                        "문 앞에 놓아주세요.",
                        List.of(
                                new CreateOrderItemRequest(
                                        productId1,
                                        2,
                                        Instant.now()
                                                .plusSeconds(3600)
                                ),
                                new CreateOrderItemRequest(
                                        productId2,
                                        3,
                                        Instant.now()
                                                .plusSeconds(3600)
                                )
                        )
                );

        // 수령 업체 정보
        when(companyClient.getCompany(
                receiverCompanyId
        )).thenReturn(
                new CompanyResponse(
                        receiverCompanyId,
                        receiverHubId,
                        "서울시 강남구"
                )
        );

        /*
         * 두 상품의 공급업체를 동일하게 설정하여
         * 공급업체 캐시가 적용되는지 확인
         */
        when(companyClient.getCompany(
                supplierCompanyId
        )).thenReturn(
                new CompanyResponse(
                        supplierCompanyId,
                        supplierHubId,
                        "서울시 송파구"
                )
        );

        // 상품 두 개를 한 번의 Batch 응답으로 반환
        when(productClient.getProducts(
                anyList()
        )).thenReturn(
                List.of(
                        new ProductResponse(
                                productId1,
                                "상품1",
                                10_000L,
                                supplierCompanyId
                        ),
                        new ProductResponse(
                                productId2,
                                "상품2",
                                20_000L,
                                supplierCompanyId
                        )
                )
        );

        // 배송 수령인 정보
        when(userClient.getUser(
                userId
        )).thenReturn(
                new UserResponse(
                        userId,
                        "수령인",
                        "slack-id"
                )
        );

        // 내부 서비스 호출 헤더값
        when(internalServiceProperties.name())
                .thenReturn("order-service");

        when(internalServiceProperties.key())
                .thenReturn("test-key");

        /*
         * 실제 DB를 사용하지 않으므로
         * 저장 시 생성되는 주문과 주문 상품 UUID를 직접 설정
         */
        when(orderRepository.save(
                any(Order.class)
        )).thenAnswer(invocation -> {
            Order savedOrder =
                    invocation.getArgument(0);

            ReflectionTestUtils.setField(
                    savedOrder,
                    "orderId",
                    orderId
            );

            savedOrder.getOrderItems()
                    .forEach(orderItem ->
                            ReflectionTestUtils.setField(
                                    orderItem,
                                    "orderItemId",
                                    UUID.randomUUID()
                            )
                    );

            return savedOrder;
        });

        /*
         * Delivery Batch 요청에 포함된 각 주문 상품 ID에 맞춰
         * 배송 생성 응답을 반환
         */
        when(deliveryClient.createDeliveries(
                eq("order-service"),
                eq("test-key"),
                anyList()
        )).thenAnswer(invocation -> {
            List<CreateDeliveryRequest> deliveryRequests =
                    invocation.getArgument(2);

            return deliveryRequests.stream()
                    .map(deliveryRequest ->
                            new CreateDeliveryResponse(
                                    UUID.randomUUID(),
                                    deliveryRequest
                                            .orderItemId(),
                                    "HUB_WAITING"
                            )
                    )
                    .toList();
        });

        // 상품 두 개로 주문 생성
        orderCommandService.createOrder(
                request,
                userId
        );

        // 상품 두 개를 한 번의 Batch API로 조회했는지 확인
        verify(
                productClient,
                times(1)
        ).getProducts(anyList());

        // 배송 두 개를 한 번의 Batch API로 생성했는지 확인
        verify(
                deliveryClient,
                times(1)
        ).createDeliveries(
                eq("order-service"),
                eq("test-key"),
                anyList()
        );

        // 동일한 공급업체가 한 번만 조회됐는지 확인
        verify(
                companyClient,
                times(1)
        ).getCompany(supplierCompanyId);
    }

    /** 동기 배송 보상 취소 실패 시 Outbox에 재처리 작업을 저장합니다. */
    @Test
    void savesOutboxWhenSynchronousCompensationFails() {
        UUID orderId = UUID.randomUUID();

        when(internalServiceProperties.name()).thenReturn("order-service");
        when(internalServiceProperties.key()).thenReturn("test-key");

        doThrow(mock(FeignException.class))
                .when(deliveryClient)
                .cancelDeliveriesByOrderId("order-service", "test-key", orderId);

        ReflectionTestUtils.invokeMethod(
                orderCommandService,
                "compensateDeliveries",
                orderId
        );

        verify(compensationOutboxService).save(orderId);
    }

    /** 주문 트랜잭션이 커밋되지 않으면 보상 Outbox를 저장합니다. */
    @Test
    void savesOutboxWhenOrderTransactionRollsBack() {
        UUID orderId = UUID.randomUUID();
        TransactionSynchronizationManager.initSynchronization();

        try {
            ReflectionTestUtils.invokeMethod(
                    orderCommandService,
                    "registerRollbackCompensation",
                    orderId
            );

            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(synchronization ->
                            synchronization.afterCompletion(
                                    TransactionSynchronization.STATUS_ROLLED_BACK
                            )
                    );

            verify(compensationOutboxService).save(orderId);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
