package com.logistics.order.infrastructure.persistence;

import com.logistics.order.domain.entity.Order;
import com.logistics.order.query.dto.OrderSummaryResponse;
import com.logistics.order.query.repository.OrderQueryRepository;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.logistics.order.domain.entity.QOrder.order;
import static com.logistics.order.domain.entity.QOrderItem.orderItem;

@Repository
@RequiredArgsConstructor
public class OrderQueryRepositoryImpl
        implements OrderQueryRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * Order와 OrderItem을 한 번에 조회합니다.
     *
     * fetchJoin을 사용하지 않으면 상품 목록을 사용할 때
     * 추가 SQL이 실행될 수 있습니다.
     */
    @Override
    public Optional<Order> findOrderDetail(UUID orderId) {
        Order result = queryFactory
                .selectFrom(order)
                .leftJoin(order.orderItems, orderItem)
                .fetchJoin()
                .where(
                        order.orderId.eq(orderId),  // 주문 ID
                        order.deletedAt.isNull()    // 소프트 삭제된 주문은 조회하지 않습니다.
                )
                .distinct()
                .fetchOne();

        return Optional.ofNullable(result);
    }

    /**
     * 삭제되지 않은 주문 목록을 최신순으로 조회합니다.
     *
     * Pageable에 담긴 페이지 번호와 페이지 크기를 이용해
     * 현재 페이지에 필요한 주문만 조회합니다.
     */
    @Override
    public Page<OrderSummaryResponse> findOrders(
        String keyword,
        UUID receiverCompanyId,
        UUID createdBy,
        UUID hubId,
        Pageable pageable
    ) {
        // 1. 현재 페이지에 보여줄 주문 목록을 조회합니다.
        List<OrderSummaryResponse> content = queryFactory
            .select(Projections.constructor(
                    OrderSummaryResponse.class,
                    order.orderId,              // 주문 ID
                    order.receiverCompanyId,    // 주문 상품을 받을 업체 ID
                    order.status,               // 주문 진행 상태
                    order.totalAmount,          // 주문 총액
                    order.createdAt             // 주문 생성 시간
            ))
            .from(order)
            .leftJoin(order.orderItems, orderItem)
            .on(orderItem.deletedAt.isNull())
            .where(
                order.deletedAt.isNull(),       // 소프트 삭제된 주문은 조회하지 않습니다.
                keywordCondition(keyword),                      // 상품명 또는 배송 요청사항 키워드 검색
                createdByCondition(createdBy),
                hubCondition(hubId)
            )
            .distinct()
            .orderBy(order.createdAt.desc())    // 최근에 생성된 주문부터 조회합니다.
            .offset(pageable.getOffset())       // 조회를 시작할 위치를 지정합니다. 1페이지 2페이지
            .limit(pageable.getPageSize())      // 한 페이지에서 조회할 주문 개수를 지정합니다.
            .fetch();                           // 지금까지 작성한 QueryDSL 쿼리를 실행합니다.

        // 2. 삭제되지 않은 전체 주문 개수를 조회합니다.
        Long total = queryFactory
            .select(order.countDistinct())
            .from(order)
            .leftJoin(order.orderItems, orderItem)
            .on(orderItem.deletedAt.isNull())
            .where(
                    order.deletedAt.isNull(),
                    keywordCondition(keyword),
                    createdByCondition(createdBy),
                    hubCondition(hubId)
            )
            .fetchOne();                        // COUNT 쿼리는 결과가 한 개이므로 fetchOne()을 사용합니다.

        // 3. 조회 결과와 전체 개수를 Page 객체로 묶어서 반환합니다.
        return new PageImpl<>(
                content,
                pageable,
                total == null ? 0L : total
        );
    }

    /**
     * 상품명 또는 배송 요청사항 키워드 검색
     */
    private BooleanExpression keywordCondition(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }

        return orderItem.productName.containsIgnoreCase(keyword)
                .or(order.deliveryRequest.containsIgnoreCase(keyword));
    }

    /**
     * 본인이 생성한 주문 조건
     */
    private BooleanExpression createdByCondition(UUID createdBy) {
        return createdBy == null
                ? null
                : order.createdBy.eq(createdBy);
    }

    /**
     * 담당 허브 주문 조건
     */
    private BooleanExpression hubCondition(UUID hubId) {
        return hubId == null
                ? null
                : order.receiverHubId.eq(hubId)
                        .or(orderItem.supplierHubId.eq(hubId));
    }
}
