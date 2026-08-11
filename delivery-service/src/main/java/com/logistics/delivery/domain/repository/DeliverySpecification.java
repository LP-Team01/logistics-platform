package com.logistics.delivery.domain.repository;


import com.logistics.delivery.domain.entity.CompanyDeliveryRouteRecord;
import com.logistics.delivery.domain.entity.Delivery;
import com.logistics.delivery.domain.entity.DeliveryRouteRecord;
import com.logistics.delivery.domain.entity.DeliveryStatus;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public class DeliverySpecification {
    public static Specification<Delivery> withSearchCondition(DeliveryStatus status, UUID orderId,
                                                              UUID orderItemId, UUID companyAgentId,
                                                              UUID assignedAgentId, UUID hubId, UUID companyId) {
        return Specification
            .allOf(equalsDeliveryStatus(status),
                equalsOrderId(orderId),
                equalsOrderItemId(orderItemId),
                equalsCompanyAgentId(companyAgentId),
                assignedToAgent(assignedAgentId),
                withinHub(hubId),
                forReceiverCompany(companyId),
                notDeleted()
            );
    }

    public static Specification<Delivery> equalsDeliveryStatus(DeliveryStatus status) {
        return ((root, query, cb) -> status == null ? null
            : cb.equal(root.get("status"), status));
    }

    private static Specification<Delivery> equalsOrderId(UUID orderId) {
        return (root, query, cb) -> orderId == null ? null
            : cb.equal(root.get("orderId"), orderId);
    }

    private static Specification<Delivery> equalsOrderItemId(UUID orderItemId) {
        return (root, query, cb) -> orderItemId == null ? null
            : cb.equal(root.get("orderItemId"), orderItemId);
    }

    private static Specification<Delivery> equalsCompanyAgentId(UUID companyAgentId) {
        return (root, query, cb) -> companyAgentId == null ? null
            : cb.equal(root.get("companyAgentId"), companyAgentId);
    }

    // DELIVERY_MANAGER 스코핑용: 업체배송담당자(companyAgentId)로 배정되었거나,
    // 허브배송담당자로 배송 경로 기록(DeliveryRouteRecord.agentId)에 배정된 배송을 모두 포함
    private static Specification<Delivery> assignedToAgent(UUID agentId) {
        return (root, query, cb) -> {
            if (agentId == null) {
                return null;
            }
            var subquery = query.subquery(UUID.class);
            var routeRoot = subquery.from(DeliveryRouteRecord.class);
            subquery.select(routeRoot.get("deliveryId"))
                .where(cb.equal(routeRoot.get("agentId"), agentId), cb.isNull(routeRoot.get("deletedAt")));
            return cb.or(
                cb.equal(root.get("companyAgentId"), agentId),
                root.get("id").in(subquery)
            );
        };
    }

    // HUB_MANAGER 스코핑용: 담당 허브가 출발 허브 또는 도착 허브인 배송
    private static Specification<Delivery> withinHub(UUID hubId) {
        return (root, query, cb) -> hubId == null ? null
            : cb.or(cb.equal(root.get("departureHubId"), hubId), cb.equal(root.get("destinationHubId"), hubId));
    }

    // COMPANY_MANAGER 스코핑용: 요청자 소속 업체가 수령업체(receiverCompanyId)인 배송
    private static Specification<Delivery> forReceiverCompany(UUID companyId) {
        return (root, query, cb) -> {
            if (companyId == null) {
                return null;
            }
            var subquery = query.subquery(UUID.class);
            var companyRouteRoot = subquery.from(CompanyDeliveryRouteRecord.class);
            subquery.select(companyRouteRoot.get("deliveryId"))
                .where(cb.equal(companyRouteRoot.get("receiverCompanyId"), companyId),
                    cb.isNull(companyRouteRoot.get("deletedAt")));
            return root.get("id").in(subquery);
        };
    }

    private static Specification<Delivery> notDeleted() {
        return ((root, query, cb) -> cb.isNull(root.get("deletedAt")));
    }
}
