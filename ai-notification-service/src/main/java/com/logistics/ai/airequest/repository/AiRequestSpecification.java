package com.logistics.ai.airequest.repository;

import com.logistics.ai.airequest.dto.requestdto.AiSearchCondition;
import com.logistics.ai.airequest.entity.AiRequest;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/**
 * AI 요청 목록의 동적 검색조건을 생성하는 클래스입니다.
 *
 * <p>전달된 검색조건만 WHERE 절에 추가하며,
 * 논리적으로 삭제된 요청은 항상 조회 대상에서 제외합니다.</p>
 */
public final class AiRequestSpecification {

    /**
     * 인스턴스 생성을 막습니다.
     */
    private AiRequestSpecification() {
    }

    /**
     * AI 요청 검색조건을 Specification으로 변환합니다.
     *
     * @param condition 주문 ID, 배송 ID, 처리 상태 검색조건
     * @return 동적으로 생성된 검색조건
     */
    public static Specification<AiRequest> withCondition(
        AiSearchCondition condition
    ) {
        List<Specification<AiRequest>> specifications =
            new ArrayList<>();

        // 논리적으로 삭제되지 않은 요청만 조회합니다.
        specifications.add(notDeleted());

        if (condition == null) {
            return Specification.allOf(specifications);
        }

        if (condition.orderId() != null) {
            specifications.add(
                hasOrderId(condition.orderId())
            );
        }

        if (condition.deliveryId() != null) {
            specifications.add(
                hasDeliveryId(condition.deliveryId())
            );
        }

        if (condition.status() != null) {
            specifications.add(
                hasStatus(condition.status())
            );
        }

        return Specification.allOf(specifications);
    }

    /**
     * 논리적으로 삭제되지 않은 요청만 조회합니다.
     */
    private static Specification<AiRequest> notDeleted() {
        return (root, query, criteriaBuilder) ->
            criteriaBuilder.isNull(root.get("deletedAt"));
    }

    /**
     * 특정 주문의 AI 요청만 조회합니다.
     */
    private static Specification<AiRequest> hasOrderId(
        java.util.UUID orderId
    ) {
        return (root, query, criteriaBuilder) ->
            criteriaBuilder.equal(root.get("orderId"), orderId);
    }

    /**
     * 특정 배송의 AI 요청만 조회합니다.
     */
    private static Specification<AiRequest> hasDeliveryId(
        java.util.UUID deliveryId
    ) {
        return (root, query, criteriaBuilder) ->
            criteriaBuilder.equal(root.get("deliveryId"), deliveryId);
    }

    /**
     * 특정 처리 상태의 AI 요청만 조회합니다.
     */
    private static Specification<AiRequest> hasStatus(
        com.logistics.ai.airequest.entity.AiRequestStatus status
    ) {
        return (root, query, criteriaBuilder) ->
            criteriaBuilder.equal(root.get("status"), status);
    }
}
