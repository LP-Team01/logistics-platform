package com.logistics.user.user.repository;

import com.logistics.user.user.dto.request.UserSearchCondition;
import com.logistics.user.user.entity.User;
import com.logistics.user.user.entity.UserRole;
import com.logistics.user.user.entity.UserStatus;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.logistics.user.user.entity.QUser.user;

@RequiredArgsConstructor
public class UserRepositoryCustomImpl implements UserRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<User> search(UserSearchCondition condition, Pageable pageable) {
        List<User> content = queryFactory
            .selectFrom(user)
            .where(
                user.deletedAt.isNull(),
                roleEq(condition.role()),
                statusEq(condition.status()),
                hubIdEq(condition.hubId()),
                companyIdEq(condition.companyId()),
                usernameContains(condition.username())
            )
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .orderBy(resolveOrders(pageable.getSort()))
            .fetch();

        Long total = queryFactory
            .select(user.count())
            .from(user)
            .where(
                user.deletedAt.isNull(),
                roleEq(condition.role()),
                statusEq(condition.status()),
                hubIdEq(condition.hubId()),
                companyIdEq(condition.companyId()),
                usernameContains(condition.username())
            )
            .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    @Override
    public Page<User> findAllByHubIdAndDeletedAtIsNullAndStatus(UUID hubId, UserStatus status, Pageable pageable){
        List<User> content = queryFactory
            .selectFrom(user)
            .where(
                user.deletedAt.isNull(),
                statusEq(status),
                hubIdEq(hubId)
            )
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .orderBy(resolveOrders(pageable.getSort()))
            .fetch();

        Long count = queryFactory
            .select(user.count())
            .from(user)
            .where(
                user.deletedAt.isNull(),
                statusEq(status),
                hubIdEq(hubId)
            )
            .fetchOne();

        return new PageImpl<>(content, pageable, count == null ? 0 : count);
    }

    private BooleanExpression roleEq(UserRole role) {
        return role != null ? user.role.eq(role) : null;
    }

    private BooleanExpression statusEq(UserStatus status) {
        return status != null ? user.status.eq(status) : null;
    }

    private BooleanExpression hubIdEq(UUID hubId) {
        return hubId != null ? user.hubId.eq(hubId) : null;
    }

    private BooleanExpression companyIdEq(UUID companyId) {
        return companyId != null ? user.companyId.eq(companyId) : null;
    }

    private BooleanExpression usernameContains(String username) {
        return StringUtils.hasText(username) ? user.username.containsIgnoreCase(username) : null;
    }

    private OrderSpecifier<?>[] resolveOrders(Sort sort){
        List<? extends OrderSpecifier<?>> orders = sort.stream()
            .map(this::toOrderSpecifier)
            .filter(Objects::nonNull)
            .toList();

        return orders.isEmpty()
            ? new OrderSpecifier[]{user.createdAt.desc()}
            : orders.toArray(new OrderSpecifier[0]);
    }

    private OrderSpecifier<?> toOrderSpecifier(Sort.Order order){
        Order direction = order.isAscending() ? Order.ASC : Order.DESC;
        return switch (order.getProperty()){
            case "createdAt" -> new OrderSpecifier<>(direction, user.createdAt);
            case "updatedAt" -> new OrderSpecifier<>(direction, user.updatedAt);
            default -> null;
        };
    }
}
