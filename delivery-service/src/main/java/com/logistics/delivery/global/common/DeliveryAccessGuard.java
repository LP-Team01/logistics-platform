package com.logistics.delivery.global.common;

import com.logistics.delivery.global.exception.BusinessException;
import com.logistics.delivery.global.exception.ErrorCode;
import java.util.Collection;
import java.util.UUID;

// "본인 담당 배송/경로만" 류의 스코핑 규칙을 한 곳에 모은 순수 검증 유틸.
// Command/Query 서비스 여러 곳에 흩어져 있던 역할/허브/담당자/업체 비교 로직 중복을 제거하기 위해 도입.
public final class DeliveryAccessGuard {

    private DeliveryAccessGuard() {
    }

    public static void requireRole(UserRole userRole, Collection<UserRole> allowedRoles, ErrorCode errorCode) {
        if (!allowedRoles.contains(userRole)) {
            throw new BusinessException(errorCode);
        }
    }

    // HUB_MANAGER/COMPANY_MANAGER 등 스코핑에 필요한 컨텍스트(X-Hub-Id/X-Company-Id) 헤더가 비어있으면
    // "전체 허용"으로 새지 않고 fail-closed 하기 위한 null 가드
    public static void requireNonNull(Object value, ErrorCode errorCode) {
        if (value == null) {
            throw new BusinessException(errorCode);
        }
    }

    // requesterHubId가 hubIds 중 하나와 일치해야 함 (배송/구간의 출발·도착 허브 등 여러 개를 한 번에 비교)
    public static void requireWithinHub(UUID requesterHubId, ErrorCode errorCode, UUID... hubIds) {
        if (!matchesAny(requesterHubId, hubIds)) {
            throw new BusinessException(errorCode);
        }
    }

    public static void requireOwnAgent(UUID requesterId, UUID agentId, ErrorCode errorCode) {
        if (requesterId == null || !requesterId.equals(agentId)) {
            throw new BusinessException(errorCode);
        }
    }

    // 업체배송담당자(companyAgentId)로 배정됐거나, 허브 구간 담당자(routeAgentIds)로 배정된 경우 모두 "본인 담당"으로 인정
    public static void requireAssignedAgent(UUID requesterId, UUID companyAgentId, Collection<UUID> routeAgentIds,
                                             ErrorCode errorCode) {
        boolean assigned = requesterId.equals(companyAgentId) || routeAgentIds.stream().anyMatch(requesterId::equals);
        if (!assigned) {
            throw new BusinessException(errorCode);
        }
    }

    public static void requireOwnCompany(UUID requesterCompanyId, UUID receiverCompanyId, ErrorCode errorCode) {
        if (requesterCompanyId == null || !requesterCompanyId.equals(receiverCompanyId)) {
            throw new BusinessException(errorCode);
        }
    }

    private static boolean matchesAny(UUID requesterHubId, UUID... hubIds) {
        if (requesterHubId == null) {
            return false;
        }
        for (UUID hubId : hubIds) {
            if (requesterHubId.equals(hubId)) {
                return true;
            }
        }
        return false;
    }
}