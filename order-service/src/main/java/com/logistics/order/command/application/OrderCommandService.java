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
import com.logistics.order.outbox.DeliveryCompensationOutboxService;
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
    private final DeliveryCompensationOutboxService compensationOutboxService;

    /**
     * 새로운 주문 과 배송 생성을 생성합니다.
     *
     */
    public OrderCommandResponse createOrder(
            CreateOrderRequest request,
            UUID userId
    ) {
        // 수령 업체 정보 조회
        CompanyResponse receiverCompany =
                getCompany(request.receiverCompanyId());

        // 배송 수령인 정보 조회
        UserResponse receiverUser =
                getUser(userId);

        // 주문 기본 정보 생성
        Order order = Order.create(
                receiverCompany.companyId(),
                receiverCompany.hubId(),
                request.deliveryRequest(),
                userId
        );

        // 요청에 포함된 상품 ID 목록 추출
        List<UUID> requestedProductIds = request.items().stream()
                .map(CreateOrderItemRequest::productId)
                .toList();

        // 동일한 상품이 주문에 중복 포함됐는지 검사
        Set<UUID> uniqueProductIds =
                new HashSet<>(requestedProductIds);

        if (uniqueProductIds.size()
                != requestedProductIds.size()) {
            throw new BusinessException(
                    ErrorCode.DUPLICATE_ORDER_PRODUCT
            );
        }

        // 상품 서비스에 한 번만 요청하여 전체 상품 조회
        List<ProductResponse> products =
                getProducts(requestedProductIds);

        // 반복문에서 상품 ID로 빠르게 조회할 수 있도록 Map으로 변환
        Map<UUID, ProductResponse> productMap =
                new HashMap<>();

        for (ProductResponse product : products) {
            /*
             * 요청하지 않은 상품이 응답되거나
             * 동일한 상품이 중복 응답되면 잘못된 응답으로 처리
             */
            if (!uniqueProductIds.contains(product.productId())
                    || productMap.put(
                            product.productId(),
                            product
                    ) != null) {
                throw new BusinessException(
                        ErrorCode.INVALID_PRODUCT_RESPONSE
                );
            }
        }

        // 요청한 상품 중 응답에서 누락된 상품이 있는지 검사
        if (productMap.size() != requestedProductIds.size()) {
            throw new BusinessException(
                    ErrorCode.INVALID_PRODUCT_RESPONSE
            );
        }

        // 같은 공급업체를 반복 조회하지 않기 위한 요청 범위 캐시
        Map<UUID, CompanyResponse> supplierCompanyCache =
                new HashMap<>();

        // 조회한 상품 정보를 이용하여 주문 상품 생성
        for (CreateOrderItemRequest itemRequest : request.items()) {
            // 일괄 조회 결과에서 현재 주문 상품 조회
            ProductResponse product =
                    productMap.get(itemRequest.productId());

            if (product == null) {
                throw new BusinessException(
                        ErrorCode.PRODUCT_NOT_FOUND
                );
            }

            // 같은 공급업체는 최초 한 번만 조회하고 이후에는 캐시 사용
            CompanyResponse supplierCompany =
                    supplierCompanyCache.computeIfAbsent(
                            product.supplierCompanyId(),
                            this::getCompany
                    );

            // 주문 당시의 상품명과 단가를 주문 상품에 저장
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

        // 주문 및 주문 상품을 저장하고 UUID를 확정
        Order savedOrder =
                orderRepository.saveAndFlush(order);

        // 배송 생성 요청 목록
        List<CreateDeliveryRequest> deliveryRequests =
                new ArrayList<>();

        // 배송 응답 검증과 실패 보상에 사용할 주문 상품 ID
        Set<UUID> requestedOrderItemIds =
                new HashSet<>();

        // 주문 상품별 배송 요청 정보 생성
        for (OrderItem orderItem : savedOrder.getOrderItems()) {
            requestedOrderItemIds.add(
                    orderItem.getOrderItemId()
            );

            deliveryRequests.add(
                    new CreateDeliveryRequest(
                            savedOrder.getOrderId(),
                            orderItem.getOrderItemId(),
                            orderItem.getSupplierHubId(),
                            savedOrder.getReceiverHubId(),
                            receiverCompany.address(),
                            receiverUser.username(),
                            receiverUser.slackId(),
                            savedOrder.getReceiverCompanyId()
                    )
            );
        }

        try {
            // 모든 주문 상품의 배송을 한 번의 API 요청으로 생성
            List<CreateDeliveryResponse> deliveryResponses =
                    createDeliveries(deliveryRequests);

            // 주문 상품 ID로 배송 응답을 찾기 위한 Map
            Map<UUID, CreateDeliveryResponse> deliveryResponseMap =
                    new HashMap<>();

            for (CreateDeliveryResponse response : deliveryResponses) {
                /*
                 * 요청하지 않은 주문 상품이 응답되거나
                 * 동일한 주문 상품 응답이 중복되면 잘못된 응답으로 처리
                 */
                if (!requestedOrderItemIds.contains(
                            response.orderItemId()
                        )
                        || deliveryResponseMap.put(
                            response.orderItemId(),
                            response
                        ) != null) {
                    throw new BusinessException(
                            ErrorCode.INVALID_DELIVERY_RESPONSE
                    );
                }
            }

            // 각 주문 상품에 생성된 배송 ID 연결
            for (OrderItem orderItem : savedOrder.getOrderItems()) {
                CreateDeliveryResponse deliveryResponse =
                        deliveryResponseMap.get(
                                orderItem.getOrderItemId()
                        );

                if (deliveryResponse == null) {
                    throw new BusinessException(
                            ErrorCode.INVALID_DELIVERY_RESPONSE
                    );
                }

                orderItem.assignDelivery(
                        deliveryResponse.deliveryId()
                );
            }

            // 모든 배송이 정상적으로 연결되면 주문 확정
            savedOrder.completeDeliveryCreation();

        } catch (RuntimeException exception) {
            // 일괄 배송 생성 실패 시 주문 단위로 한 번만 보상 취소
            compensateDeliveries(savedOrder.getOrderId());

            // 예외를 다시 전달하여 주문 DB 트랜잭션 롤백
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

        // 주문에 포함된 배송을 한 번에 취소
        cancelDeliveriesByOrderId(orderId);

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
     * 주문 생성 실패 시 해당 주문의 배송을 한 번에 보상 취소합니다.
     */
    private void compensateDeliveries(UUID orderId) {
        try {
            deliveryClient.cancelDeliveriesByOrderId(
                    internalServiceProperties.name(),
                    internalServiceProperties.key(),
                    orderId
            );
        } catch (FeignException exception) {
            compensationOutboxService.save(orderId);
            log.error(
                    "배송 보상 취소 실패. orderId={}",
                    orderId,
                    exception
            );
        }
    }

    /**
     * 주문 전체 삭제
     */
    private void cancelDeliveriesByOrderId(UUID orderId) {
        try {
            deliveryClient.cancelDeliveriesByOrderId(
                    internalServiceProperties.name(),
                    internalServiceProperties.key(),
                    orderId
            );
        } catch (FeignException exception) {
            throw new BusinessException(
                    ErrorCode.DELIVERY_CANCELLATION_FAILED
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

    /**
     * 주문에 포함된 상품 정보를 한 번의 API 요청으로 조회합니다.
     */
    private List<ProductResponse> getProducts(
            List<UUID> productIds
    ) {
        if (productIds == null
                || productIds.isEmpty()
                || productIds.stream().anyMatch(Objects::isNull)) {
            throw new BusinessException(
                    ErrorCode.INVALID_ORDER_REQUEST
            );
        }

        try {
            List<ProductResponse> responses =
                    productClient.getProducts(productIds);

            // 응답이 없거나 요청한 상품 수와 다르면 잘못된 응답으로 처리
            if (responses == null
                    || responses.size() != productIds.size()) {
                throw new BusinessException(
                        ErrorCode.INVALID_PRODUCT_RESPONSE
                );
            }

            // 상품별 필수 응답값 검증
            boolean invalidResponse = responses.stream()
                    .anyMatch(product ->
                            product == null
                                    || product.productId() == null
                                    || product.productName() == null
                                    || product.productName().isBlank()
                                    || product.unitPrice() == null
                                    || product.unitPrice() < 0
                                    || product.supplierCompanyId() == null
                    );

            if (invalidResponse) {
                throw new BusinessException(
                        ErrorCode.INVALID_PRODUCT_RESPONSE
                );
            }

            return responses;
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
     * 주문 상품들의 배송을 한 번의 내부 API 요청으로 생성합니다.
     */
    private List<CreateDeliveryResponse> createDeliveries(
            List<CreateDeliveryRequest> requests
    ) {
        if (requests == null || requests.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.INVALID_ORDER_REQUEST
            );
        }

        try {
            List<CreateDeliveryResponse> responses =
                    deliveryClient.createDeliveries(
                            internalServiceProperties.name(),
                            internalServiceProperties.key(),
                            requests
                    );

            // 응답이 없거나 요청한 배송 수와 다르면 잘못된 응답으로 처리
            if (responses == null
                    || responses.size() != requests.size()) {
                throw new BusinessException(
                        ErrorCode.INVALID_DELIVERY_RESPONSE
                );
            }

            // 배송 ID와 주문 상품 ID가 모두 반환됐는지 검사
            boolean invalidResponse = responses.stream()
                    .anyMatch(response ->
                            response == null
                                    || response.deliveryId() == null
                                    || response.orderItemId() == null
                    );

            if (invalidResponse) {
                throw new BusinessException(
                        ErrorCode.INVALID_DELIVERY_RESPONSE
                );
            }

            return responses;

        } catch (FeignException.Conflict exception) {
            // 주문 상품 중 하나라도 이미 배송이 생성된 경우
            throw new BusinessException(
                    ErrorCode.DELIVERY_ALREADY_ASSIGNED
            );

        } catch (FeignException exception) {
            throw new BusinessException(
                    ErrorCode.DELIVERY_CREATION_FAILED
            );
        }
    }

}
