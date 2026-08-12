package com.logistics.delivery.command.application;

import com.logistics.delivery.domain.entity.CompanyDeliveryRouteRecord;
import com.logistics.delivery.domain.entity.CompanyRouteRecordStatus;
import com.logistics.delivery.domain.repository.CompanyDeliveryRouteRecordRepository;
import com.logistics.delivery.domain.repository.DeliveryAgentRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// CompanyRouteSequencingService가 트랜잭션 밖에서 계산(Gemini/Naver 호출)을 마친 결과를 짧은 트랜잭션으로 DB에 반영하는 전담 컴포넌트.
@Slf4j
@Component
@RequiredArgsConstructor
public class CompanyRouteResequenceWriter {

    private final CompanyDeliveryRouteRecordRepository companyDeliveryRouteRecordRepository;
    private final DeliveryAgentRepository deliveryAgentRepository;

    private static final String RESEQUENCE_LOCK_PREFIX = "company-route-resequence:";

    public record RouteTotal(int totalDistanceKm, int totalDurationMinutes) {
    }

    // 담당자 기준 advisory lock으로 동시 반영을 직렬화하고, 계산을 시작한 시점 이후 WAITING 목록이 바뀌지 않았을 때만 결과를 반영한다.
    // 목록이 바뀌었다면 그 변경을 유발한 새 resequence() 호출이 곧 최신 결과로 다시 반영할 것이므로 이번 결과는 버린다.
    @Transactional
    public void apply(UUID agentId, Set<UUID> expectedRecordIds, List<UUID> finalOrder, RouteTotal routeTotal) {
        deliveryAgentRepository.lockAgentGroup(RESEQUENCE_LOCK_PREFIX + agentId);

        List<CompanyDeliveryRouteRecord> currentWaiting = companyDeliveryRouteRecordRepository
            .findByAgentIdAndStatusAndDeletedAtIsNull(agentId, CompanyRouteRecordStatus.WAITING);
        Set<UUID> currentIds = currentWaiting.stream()
            .map(CompanyDeliveryRouteRecord::getId)
            .collect(Collectors.toSet());

        if (!currentIds.equals(expectedRecordIds)) {
            log.info("재계산 도중 WAITING 목록이 변경되어 이전 계산 결과 반영을 건너뜁니다 - agentId={}", agentId);
            return;
        }

        Map<UUID, CompanyDeliveryRouteRecord> currentById = new HashMap<>();
        currentWaiting.forEach(record -> currentById.put(record.getId(), record));
        for (int i = 0; i < finalOrder.size(); i++) {
            currentById.get(finalOrder.get(i)).updateSequence(i + 1);
        }

        if (routeTotal != null) {
            deliveryAgentRepository.findByIdAndDeletedAtIsNull(agentId).ifPresent(agent -> agent.updateRouteSummary(
                routeTotal.totalDistanceKm(), routeTotal.totalDurationMinutes(), Instant.now()));
        }
    }
}
