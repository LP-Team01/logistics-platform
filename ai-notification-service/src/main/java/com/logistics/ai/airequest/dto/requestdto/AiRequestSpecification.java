package com.logistics.ai.airequest.dto.requestdto;

import com.logistics.ai.airequest.entity.AiRequest;
import com.logistics.ai.airequest.entity.AiRequestStatus;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

/**
 * AI 요청 목록의 동적 검색조건을 생성하는 클래스입니다.
 *
 * <p>주문 ID, 배송 ID, 처리 상태 중 실제로 전달된 조건만
 * SQL WHERE 절에 포함합니다.</p>
 */
public final class AiRequestSpecification {

    // 문자열 오타를 줄이기 위한 엔티티 필드명 상수입니다.
    private static final String DELETED_AT = "deletedAt";
    private static final String ORDER_ID = "orderId";
    private static final String DELIVERY_ID = "deliveryId";
    private static final String STATUS = "status";

    /**
     * 객체 생성을 막기 위한 private 생성자입니다.
     *
     * <p>이 클래스는 정적 메서드만 제공하므로 객체를 생성할 필요가 없습니다.</p>
     */
    private AiRequestSpecification() {
    }

    /**
     * AI 요청 목록에 적용할 전체 검색조건을 생성합니다.
     *
     * @param condition 목록 검색조건
     * @return 조합된 JPA Specification
     */
    public static Specification<AiRequest> search(
        AiSearchCondition condition
    ) {
        // 검색조건이 전달되지 않아도 삭제된 데이터는 제외합니다.
        if (condition == null) {
            return notDeleted();
        }

        return notDeleted()
            .and(hasOrderId(condition.orderId()))
            .and(hasDeliveryId(condition.deliveryId()))
            .and(hasStatus(condition.status()));
    }

    /**
     * Soft Delete되지 않은 데이터만 조회합니다.
     */
    private static Specification<AiRequest> notDeleted() {
        return (root, query, criteriaBuilder) ->
            criteriaBuilder.isNull(root.get(DELETED_AT));
    }

    /**
     * 주문 ID가 전달된 경우에만 주문 조건을 적용합니다.
     */
    private static Specification<AiRequest> hasOrderId(UUID orderId) {
        return (root, query, criteriaBuilder) -> {
            if (orderId == null) {
                // 조건이 없으면 항상 참인 조건을 반환합니다.
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.equal(
                root.get(ORDER_ID),
                orderId
            );
        };
    }

    /**
     * 배송 ID가 전달된 경우에만 배송 조건을 적용합니다.
     */
    private static Specification<AiRequest> hasDeliveryId(
        UUID deliveryId
    ) {
        return (root, query, criteriaBuilder) -> {
            if (deliveryId == null) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.equal(
                root.get(DELIVERY_ID),
                deliveryId
            );
        };
    }

    /**
     * 처리 상태가 전달된 경우에만 상태 조건을 적용합니다.
     */
    private static Specification<AiRequest> hasStatus(
        AiRequestStatus status
    ) {
        return (root, query, criteriaBuilder) -> {
            if (status == null) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.equal(
                root.get(STATUS),
                status
            );
        };
    }
}
