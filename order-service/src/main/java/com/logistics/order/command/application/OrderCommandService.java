package com.logistics.order.command.application;

import com.logistics.order.command.dto.*;
import com.logistics.order.config.InternalServiceProperties;
import com.logistics.order.domain.entity.Order;
import com.logistics.order.domain.entity.OrderItem;
import com.logistics.order.domain.entity.OrderItemStatus;
import com.logistics.order.domain.repository.OrderRepository;

import com.logistics.order.global.auth.OrderOwnershipValidator;
import com.logistics.order.global.exception.BusinessException;
import com.logistics.order.global.exception.ErrorCode;
import com.logistics.order.infrastructure.client.CompanyClient;
import com.logistics.order.infrastructure.client.DeliveryClient;
import com.logistics.order.infrastructure.client.ProductClient;
import com.logistics.order.infrastructure.client.UserClient;
import com.logistics.order.infrastructure.client.dto.*;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class OrderCommandService {

    private final OrderRepository orderRepository;
    private final OrderOwnershipValidator orderOwnershipValidator;
    private final CompanyClient companyClient;
    private final ProductClient productClient;
    private final UserClient userClient;
    private final DeliveryClient deliveryClient;
    private final InternalServiceProperties internalServiceProperties;

    /**
     * 새로운 주문 과 배송 생성을 생성합니다.
     *
     */
    public OrderCommandResponse createOrder(
        CreateOrderRequest request,
        UUID userId
    ) {
        // 수령업체와 주문자 조회
        CompanyResponse receiverCompany =
                getCompany(request.receiverCompanyId());

        UserResponse receiverUser =
                getUser(userId);

        Order order = Order.create(
                receiverCompany.companyId(),
                receiverCompany.hubId(),
                request.deliveryRequest(),
                userId
        );

        Set<UUID> productIds = new HashSet<>();

        for (CreateOrderItemRequest itemRequest : request.items()) {
            // 상품 중복 방지
            if (!productIds.add(itemRequest.productId())) {
                throw new BusinessException(
                        ErrorCode.DUPLICATE_ORDER_PRODUCT
                );
            }

            ProductResponse product =
                    getProduct(itemRequest.productId());

            CompanyResponse supplierCompany =
                    getCompany(product.supplierCompanyId());

            OrderItem orderItem = OrderItem.create(
                    product.productId(),
                    product.productName(),
                    product.unitPrice(),
                    itemRequest.quantity(),
                    supplierCompany.companyId(),
                    supplierCompany.hubId(),
                    itemRequest.requestedDeadline(),
                    userId
            );

            order.addItem(orderItem);
        }

        // 배송 요청에 사용할 주문·상품 UUID 생성
        Order savedOrder =
                orderRepository.saveAndFlush(order);

        // 배송 생성이 요청된 상품 ID 기록
        List<UUID> requestedOrderItemIds =
                new ArrayList<>();

        // 주문 상품별 배송 생성
        try {
            for (OrderItem orderItem : savedOrder.getOrderItems()) {
                /*
                 * 응답을 받기 전에 통신이 끊길 수도 있으므로
                 * 배송 요청 전에 상품 ID를 기록합니다.
                 */
                requestedOrderItemIds.add(
                    orderItem.getOrderItemId()
                );
                CreateDeliveryRequest deliveryRequest =
                    new CreateDeliveryRequest(
                        savedOrder.getOrderId(),
                        orderItem.getOrderItemId(),
                        orderItem.getSupplierHubId(),
                        savedOrder.getReceiverHubId(),
                        receiverCompany.address(),
                        receiverUser.username(),
                        receiverUser.slackId(),
                        savedOrder.getReceiverCompanyId()
                    );

                CreateDeliveryResponse deliveryResponse =
                    createDelivery(deliveryRequest);

                // 반환받은 배송 ID 연결
                orderItem.assignDelivery(
                    deliveryResponse.deliveryId()
                );
            }

            // 모든 배송 연결 확인 후 주문 확정
            savedOrder.completeDeliveryCreation();
        } catch (RuntimeException exception) {
            // 생성됐을 가능성이 있는 배송 모두 취소
            requestedOrderItemIds.forEach(
                    this::compensateDelivery
            );
            // 주문 DB 트랜잭션 롤백
            throw exception;
        }

        return OrderCommandResponse.from(savedOrder);
    }

    /**
     * 업체 정보 조회
     */
    private CompanyResponse getCompany(UUID companyId) {
        if (companyId == null) {
            throw new BusinessException(
                    ErrorCode.INVALID_ORDER_REQUEST
            );
        }

        try {
            CompanyResponse response =
                    companyClient.getCompany(companyId);

            // 필수 응답값 검증
            if (response == null
                    || !companyId.equals(response.companyId())
                    || response.hubId() == null
                    || response.address() == null
                    || response.address().isBlank()) {
                throw new BusinessException(
                        ErrorCode.INVALID_COMPANY_RESPONSE
                );
            }

            return response;
        } catch (FeignException.NotFound exception) {
            throw new BusinessException(
                    ErrorCode.COMPANY_NOT_FOUND
            );
        } catch (FeignException exception) {
            throw new BusinessException(
                    ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE
            );
        }
    }

    /**
     * 상품 정보 조회
     */
    private ProductResponse getProduct(UUID productId) {
        if (productId == null) {
            throw new BusinessException(
                    ErrorCode.INVALID_ORDER_REQUEST
            );
        }

        try {
            ProductResponse response =
                    productClient.getProduct(productId);

            // 필수 응답값 검증
            if (response == null
                    || !productId.equals(response.productId())
                    || response.productName() == null
                    || response.productName().isBlank()
                    || response.unitPrice() == null
                    || response.unitPrice() < 0
                    || response.supplierCompanyId() == null) {
                throw new BusinessException(
                        ErrorCode.INVALID_PRODUCT_RESPONSE
                );
            }

            return response;
        } catch (FeignException.NotFound exception) {
            throw new BusinessException(
                    ErrorCode.PRODUCT_NOT_FOUND
            );
        } catch (FeignException exception) {
            throw new BusinessException(
                    ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE
            );
        }
    }

    /**
     * 주문 상품 전체 취소
     */
    public OrderCommandResponse cancelOrder(
            UUID orderId,
            CancelOrderRequest request,
            UUID userId,
            String userRole,
            UUID hubId
    ) {
        // 삭제되지 않은 주문 조회
        Order order = orderRepository
                .findByOrderIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode.ORDER_NOT_FOUND
                        )
                );

        // 사용자별 주문 취소 권한 검사
        orderOwnershipValidator.validateCancelAccess(
                order,
                userRole,
                hubId
        );

        // 외부 배송 API 호출 전에 전체 취소 가능 여부 검사
        order.validateCancellation();

        // 취소 대상 상품의 배송부터 취소
        order.getOrderItems().stream()
                .filter(item -> !item.isDeleted())
                .filter(item ->
                        item.getStatus()
                                != OrderItemStatus.CANCELLED
                )
                .forEach(this::cancelDelivery);

        // 배송 취소 성공 후 주문과 상품 상태 변경
        order.cancel(
                userId,
                request.cancelReason()
        );

        // JPA 변경 감지로 자동 UPDATE
        return OrderCommandResponse.from(order);
    }

    /**
     * 주문에 포함된 상품 하나를 취소합니다.
     */
    public OrderCommandResponse cancelOrderItem(
            UUID orderId,
            UUID orderItemId,
            CancelOrderRequest request,
            UUID userId,
            String userRole,
            UUID hubId
    ) {
        // 삭제되지 않은 주문 조회
        Order order = orderRepository
                .findByOrderIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.ORDER_NOT_FOUND)
                );

        // MASTER 또는 담당 HUB_MANAGER 권한 검사
        orderOwnershipValidator.validateCancelAccess(
                order,
                userRole,
                hubId
        );

        // 취소할 주문 상품 조회
        OrderItem orderItem =
                order.getOrderItem(orderItemId);

        // 생성된 배송 먼저 취소
        cancelDelivery(orderItem);

        // 배송 취소 성공 후 주문 상품 취소
        order.cancelItem(
                orderItemId,
                userId,
                request.cancelReason()
        );

        // JPA 변경 감지로 자동 UPDATE
        return OrderCommandResponse.from(order);
    }

    /**
     * 주문 배송 요청사항 수정
     */
    public OrderCommandResponse updateOrder(
            UUID orderId,
            UpdateOrderRequest request,
            String userRole,
            UUID hubId
    ) {
        // 삭제되지 않은 주문 조회
        Order order = orderRepository
                .findByOrderIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.ORDER_NOT_FOUND)
                );

        // 마스터 또는 담당 허브 관리자 검사
        orderOwnershipValidator.validateManageAccess(
                order,
                userRole,
                hubId
        );

        // 배송 요청사항 수정
        order.updateDeliveryRequest(
                request.deliveryRequest()
        );

        // JPA 변경 감지로 자동 UPDATE
        return OrderCommandResponse.from(order);
    }

    /**
     * 사용자 정보 조회
     */
    private UserResponse getUser(UUID userId) {
        if (userId == null) {
            throw new BusinessException(
                    ErrorCode.INVALID_ORDER_REQUEST
            );
        }

        try {
            UserResponse response =
                    userClient.getUser(userId);

            // 필수 응답값 검증
            if (response == null
                    || !userId.equals(response.userId())
                    || response.username() == null
                    || response.username().isBlank()) {
                throw new BusinessException(
                        ErrorCode.INVALID_USER_RESPONSE
                );
            }

            return response;
        } catch (FeignException.NotFound exception) {
            throw new BusinessException(
                    ErrorCode.USER_NOT_FOUND
            );
        } catch (FeignException exception) {
            throw new BusinessException(
                    ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE
            );
        }
    }

    /**
     * 배송 생성 요청
     */
    private CreateDeliveryResponse createDelivery(
            CreateDeliveryRequest request
    ) {
        // 필수 요청값 검증
        if (request == null
                || request.orderId() == null
                || request.orderItemId() == null
                || request.departureHubId() == null
                || request.destinationHubId() == null
                || request.deliveryAddress() == null
                || request.deliveryAddress().isBlank()
                || request.receiver() == null
                || request.receiver().isBlank()) {
            throw new BusinessException(
                    ErrorCode.INVALID_ORDER_REQUEST
            );
        }

        try {
            CreateDeliveryResponse response =
                    deliveryClient.createDelivery(
                            internalServiceProperties.name(),
                            internalServiceProperties.key(),
                            request
                    );

            // 배송 ID 응답 검증
            if (response == null
                    || response.deliveryId() == null) {
                throw new BusinessException(
                        ErrorCode.INVALID_DELIVERY_RESPONSE
                );
            }

            return response;
        } catch (FeignException.Conflict exception) {
            // 같은 주문 상품의 배송이 이미 생성된 경우
            throw new BusinessException(
                    ErrorCode.DELIVERY_ALREADY_ASSIGNED
            );
        } catch (FeignException exception) {
            throw new BusinessException(
                    ErrorCode.DELIVERY_CREATION_FAILED
            );
        }
    }

    /**
     * 생성된 배송 보상 취소
     */
    private void compensateDelivery(UUID orderItemId) {
        try {
            deliveryClient.cancelDeliveryByOrderItemId(
                    internalServiceProperties.name(),
                    internalServiceProperties.key(),
                    orderItemId
            );
        } catch (FeignException exception) {
            // 원래 주문 생성 오류를 덮어쓰지 않고 기록
            log.error(
                    "배송 보상 처리 실패. orderItemId={}",
                    orderItemId,
                    exception
            );
        }
    }

    /**
     * 주문 취소 전 배송 취소
     */
    private void cancelDelivery(OrderItem orderItem) {
        if (orderItem == null) {
            throw new BusinessException(
                    ErrorCode.ORDER_ITEM_NOT_FOUND
            );
        }

        // 아직 배송이 생성되지 않은 상품
        if (orderItem.getDeliveryId() == null) {
            return;
        }

        try {
            deliveryClient.cancelDeliveryByOrderItemId(
                    internalServiceProperties.name(),
                    internalServiceProperties.key(),
                    orderItem.getOrderItemId()
            );
        } catch (FeignException exception) {
            // 배송 취소 실패 시 주문 상태는 변경하지 않음
            throw new BusinessException(
                    ErrorCode.DELIVERY_CANCELLATION_FAILED
            );
        }
    }

    /**
     * 주문 논리 삭제
     */
    public void deleteOrder(
            UUID orderId,
            UUID userId,
            String userRole,
            UUID hubId
    ) {
        // 삭제되지 않은 주문 조회
        Order order = orderRepository
                .findByOrderIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode.ORDER_NOT_FOUND
                        )
                );

        // MASTER 또는 담당 HUB_MANAGER만 삭제 가능
        orderOwnershipValidator.validateManageAccess(
                order,
                userRole,
                hubId
        );

        // 주문과 주문 상품 논리 삭제
        order.delete(userId);
    }

    /**
     * 배송 완료된 주문 상품 상태 변경
     */
    public void completeOrderItem(
            UUID orderId,
            UUID orderItemId
    ) {
        // 삭제되지 않은 주문 조회
        Order order = orderRepository
                .findByOrderIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode.ORDER_NOT_FOUND
                        )
                );

        // 상품 완료 및 주문 완료 상태 반영
        order.completeItem(orderItemId);
    }

}
