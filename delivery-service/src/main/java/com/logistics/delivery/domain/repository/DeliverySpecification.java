package com.logistics.delivery.domain.repository;


import com.logistics.delivery.domain.entity.Delivery;
import com.logistics.delivery.domain.entity.DeliveryStatus;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public class DeliverySpecification {
    public static Specification<Delivery> withSearchCondition(DeliveryStatus status, UUID orderId,
                                                              UUID companyAgentId) {
        return Specification
            .allOf(equalsDeliveryStatus(status),
                equalsOrderId(orderId),
                equalsCompanyAgentId(companyAgentId),
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

    private static Specification<Delivery> equalsCompanyAgentId(UUID companyAgentId) {
        return (root, query, cb) -> companyAgentId == null ? null
            : cb.equal(root.get("companyAgentId"), companyAgentId);
    }

    private static Specification<Delivery> notDeleted() {
        return ((root, query, cb) -> cb.isNull(root.get("deletedAt")));
    }
}
