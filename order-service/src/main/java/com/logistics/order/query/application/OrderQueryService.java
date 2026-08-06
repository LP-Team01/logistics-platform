package com.logistics.order.query.application;

import com.logistics.order.domain.entity.Order;
import com.logistics.order.global.auth.OrderOwnershipValidator;
import com.logistics.order.global.exception.BusinessException;
import com.logistics.order.global.exception.ErrorCode;
import com.logistics.order.query.dto.OrderDetailResponse;
import com.logistics.order.query.dto.OrderSummaryResponse;
import com.logistics.order.query.repository.OrderQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OrderQueryService {

    private final OrderQueryRepository orderQueryRepository;
    private final OrderOwnershipValidator orderOwnershipValidator;

    /**
     * 주문 ID로 주문과 주문 상품 목록을 조회합니다.
     */
    public OrderDetailResponse getOrderDetail(
        UUID orderId,
        UUID userId,
        String userRole,
        UUID companyId,
        UUID hubId
    ) {
        Order order = orderQueryRepository.findOrderDetail(orderId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.ORDER_NOT_FOUND)
                );

        // 업체 관리자의 주문 소유권 검사
        orderOwnershipValidator.validateReadAccess(
                order,
                userId,
                userRole,
                companyId,
                hubId
        );

        // Entity를 외부 응답용 DTO로 변환합니다.
        return OrderDetailResponse.from(order);
    }

    /**
     * 권한별 주문 목록 조회
     */
    public Page<OrderSummaryResponse> getOrders(
            String keyword,
            Pageable pageable,
            UUID userId,
            String userRole,
            UUID companyId,
            UUID hubId
    ) {
        UUID receiverCompanyId = null;
        UUID createdBy = null;
        UUID assignedHubId = null;

        if ("COMPANY_MANAGER".equals(userRole)) {
        if (companyId == null) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        receiverCompanyId = companyId;

        } else if ("DELIVERY_MANAGER".equals(userRole)) {
            if (userId == null) {
                throw new BusinessException(ErrorCode.ACCESS_DENIED);
            }

            createdBy = userId;

        } else if ("HUB_MANAGER".equals(userRole)) {
            if (hubId == null) {
                throw new BusinessException(ErrorCode.ACCESS_DENIED);
            }

            assignedHubId = hubId;

        } else if (!"MASTER".equals(userRole)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        return orderQueryRepository.findOrders(
                keyword,
                receiverCompanyId,
                createdBy,
                assignedHubId,
                pageable
        );
    }
}
