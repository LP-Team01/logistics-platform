package com.logistics.delivery.domain.repository;


import com.logistics.delivery.domain.entity.AgentType;
import com.logistics.delivery.domain.entity.DeliveryAgent;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public class DeliveryAgentSpecification {
    public static Specification<DeliveryAgent> withSearchCondition(UUID hubId, AgentType agentType,
                                                                   Boolean isAvailable) {
        return Specification
            .allOf(likeHubId(hubId))
            .and(equalsAgentType(agentType))
            .and(equalsIsAvailable(isAvailable))
            .and(notDeleted());
    }

    private static Specification<DeliveryAgent> likeHubId(UUID hubId) {
        return (root, query, cb) -> hubId == null ? null
            : cb.like(root.get("hubId"), "%" + hubId + "%");
    }

    public static Specification<DeliveryAgent> equalsAgentType(AgentType agentType) {
        return ((root, query, cb) -> agentType == null ? null
            : cb.equal(root.get("agentType"), "%" + agentType + "%"));
    }

    private static Specification<DeliveryAgent> equalsIsAvailable(Boolean isAvailable) {
        return ((root, query, cb) -> isAvailable == null ? null
            : cb.equal(root.get("isAvailable"), "%" + isAvailable + "%"));
    }

    private static Specification<DeliveryAgent> notDeleted() {
        return ((root, query, cb) -> cb.isNull(root.get("deletedAt")));
    }
}
