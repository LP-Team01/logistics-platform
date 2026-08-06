package com.logistics.order.global.auth;

import com.logistics.order.domain.entity.Order;
import com.logistics.order.global.exception.BusinessException;
import com.logistics.order.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 주문 접근 권한 검사
 */
@Component
public class OrderOwnershipValidator {

    /**
     * 주문 조회 권한 검사
     */
    public void validateReadAccess(
            Order order,
            UUID userId,
            String userRole,
            UUID companyId,
            UUID hubId
    ) {
        if (order == null || userRole == null) {
            deny();
        }

        boolean allowed = switch (userRole) {
            case "MASTER" -> true;

            case "HUB_MANAGER" ->
                    order.belongsToHub(hubId);

            case "COMPANY_MANAGER" ->
                    order.belongsToCompany(companyId);

            case "DELIVERY_MANAGER" ->
                    order.wasCreatedBy(userId);

            default -> false;
        };

        if (!allowed) {
            deny();
        }
    }

    /**
     * 주문 수정 권한 검사
     */
    public void validateManageAccess(
            Order order,
            String userRole,
            UUID hubId
    ) {
        if (order == null || userRole == null) {
            deny();
        }

        boolean allowed = switch (userRole) {
            case "MASTER" -> true;

            case "HUB_MANAGER" ->
                    order.belongsToHub(hubId);

            default -> false;
        };

        if (!allowed) {
            deny();
        }
    }

    /**
     * 주문 취소 권한 검사
     */
    public void validateCancelAccess(
            Order order,
            String userRole,
            UUID hubId
    ) {
        if (order == null || userRole == null) {
            deny();
        }

        boolean allowed = switch (userRole) {
            case "MASTER" -> true;

            case "HUB_MANAGER" ->
                    order.belongsToHub(hubId);

            default -> false;
        };

        if (!allowed) {
            deny();
        }
    }

    /**
     * 접근 거부
     */
    private void deny() {
        throw new BusinessException(
                ErrorCode.ACCESS_DENIED
        );
    }
}
