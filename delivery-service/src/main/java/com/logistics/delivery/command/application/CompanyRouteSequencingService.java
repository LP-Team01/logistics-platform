package com.logistics.delivery.command.application;

import com.logistics.delivery.domain.entity.CompanyDeliveryRouteRecord;
import com.logistics.delivery.domain.entity.CompanyRouteRecordStatus;
import com.logistics.delivery.domain.repository.CompanyDeliveryRouteRecordRepository;
import com.logistics.delivery.infrastructure.client.AiNotificationServiceClient;
import com.logistics.delivery.infrastructure.client.HubQueryService;
import com.logistics.delivery.infrastructure.client.dto.HubServiceHubResponseDto;
import com.logistics.delivery.infrastructure.client.dto.VisitSequenceRefinementRequestDto;
import com.logistics.delivery.infrastructure.client.dto.VisitSequenceRefinementResponseDto;
import com.logistics.delivery.infrastructure.client.external.naver.NaverCoordinateFormatter;
import com.logistics.delivery.infrastructure.client.external.naver.NaverDirectionsClient;
import com.logistics.delivery.infrastructure.client.external.naver.dto.NaverDirectionsResponseDto;
import com.logistics.delivery.global.config.InternalServiceProperties;
import feign.FeignException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

// 업체배송담당자에게 새 구간이 배정될 때마다 그 담당자의 당일 방문 순서(1차 NN)를 재계산
// 허브 좌표 조회 실패 등은 배송 처리 자체를 막지 않도록 조용히 건너뛴다
//
// Gemini(최대 30초) / Naver(구간당 최대 3회 재시도) 외부 호출을 트랜잭션 밖에서 모두 마친 뒤,
// DB 반영만 CompanyRouteResequenceWriter의 짧은 트랜잭션(advisory lock + 변경분 재검증 포함)으로 위임한다.
// 같은 클래스 안에 @Transactional 메서드를 두면 self-invocation으로 트랜잭션이 적용되지 않으므로 별도 빈으로 분리했다.
@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyRouteSequencingService {

    private final CompanyDeliveryRouteRecordRepository companyDeliveryRouteRecordRepository;
    private final HubQueryService hubQueryService;
    private final NaverDirectionsClient naverDirectionsClient;
    private final AiNotificationServiceClient aiNotificationServiceClient;
    private final InternalServiceProperties internalServiceProperties;
    private final CompanyRouteResequenceWriter resequenceWriter;

    // 배송 용도에 맞는 옵션 값(실시간 빠른 길)
    private static final String NAVER_DIRECTIONS_OPTION = "trafast";
    // Naver 호출 실패 시 정책: 구간(leg)당 최대 3회 재시도, 3회 모두 실패하면 총 동선 갱신을 건너뜀
    private static final int MAX_ATTEMPTS = 3;

    public void resequence(UUID agentId, UUID hubId) {
        List<CompanyDeliveryRouteRecord> waiting = companyDeliveryRouteRecordRepository
            .findByAgentIdAndStatusAndDeletedAtIsNull(agentId, CompanyRouteRecordStatus.WAITING);
        if (waiting.isEmpty()) {
            return;
        }

        HubServiceHubResponseDto hub = fetchHub(hubId);
        if (hub == null || hub.latitude() == null || hub.longitude() == null) {
            log.warn("허브 좌표를 확인할 수 없어 방문 순서 자동 계산을 건너뜁니다 - agentId={}, hubId={}", agentId, hubId);
            return;
        }

        List<NearestNeighborRouteSequencer.Stop> stops = waiting.stream()
            .map(record -> new NearestNeighborRouteSequencer.Stop(record.getId(), record.getLatitude(), record.getLongitude()))
            .toList();
        List<UUID> ordered = NearestNeighborRouteSequencer.order(hub.latitude(), hub.longitude(), stops);

        Map<UUID, CompanyDeliveryRouteRecord> recordsById = new HashMap<>();
        waiting.forEach(record -> recordsById.put(record.getId(), record));
        Set<UUID> expectedRecordIds = new HashSet<>(recordsById.keySet());

        // AI 미세조정 결과가 있으면 그걸로, 없으면 NN 순서 그대로 총 동선 계산에 사용
        List<UUID> finalOrder = refineWithAi(agentId, hub, recordsById, ordered);
        CompanyRouteResequenceWriter.RouteTotal routeTotal = calculateRouteTotal(agentId, hub, recordsById, finalOrder);

        resequenceWriter.apply(agentId, expectedRecordIds, finalOrder, routeTotal);
    }

    // 1차 NN 결과를 ai-notification-service에 보내 미세조정을 요청
    // 실패 시 재시도 없이 NN 순서를 그대로 유지
    private List<UUID> refineWithAi(UUID agentId, HubServiceHubResponseDto hub,
                                     Map<UUID, CompanyDeliveryRouteRecord> recordsById, List<UUID> ordered) {
        VisitSequenceRefinementRequestDto request = buildRefinementRequest(agentId, hub, recordsById, ordered);
        try {
            VisitSequenceRefinementResponseDto response = aiNotificationServiceClient.refineVisitSequence(
                internalServiceProperties.name(), internalServiceProperties.key(), request);
            List<UUID> refined = response.orderedRecordIds();
            if (refined == null || !recordsById.keySet().equals(new HashSet<>(refined))) {
                log.warn("AI 방문순서 다듬기 응답이 유효하지 않아 NN 순서를 유지합니다 - agentId={}", agentId);
                return ordered;
            }
            return refined;
        } catch (FeignException e) {
            log.warn("AI 방문순서 다듬기 호출 실패 - NN 순서를 유지합니다 - agentId={}, status={}",
                agentId, e.status());
            return ordered;
        }
    }

    private VisitSequenceRefinementRequestDto buildRefinementRequest(UUID agentId, HubServiceHubResponseDto hub,
                                                                       Map<UUID, CompanyDeliveryRouteRecord> recordsById,
                                                                       List<UUID> ordered) {
        List<VisitSequenceRefinementRequestDto.Stop> stops = new ArrayList<>();
        for (int i = 0; i < ordered.size(); i++) {
            CompanyDeliveryRouteRecord record = recordsById.get(ordered.get(i));
            stops.add(new VisitSequenceRefinementRequestDto.Stop(
                record.getId(),
                record.getReceiverCompanyId(),
                record.getLatitude(),
                record.getLongitude(),
                record.getEstimatedDistance(),
                record.getEstimatedDuration(),
                i + 1
            ));
        }
        return new VisitSequenceRefinementRequestDto(agentId, hub.hubId(), hub.latitude(), hub.longitude(), stops);
    }

    // NN(혹은 AI가 다듬은) 순서 그대로 "허브 → 업체1 → 업체2 → ..." 각 구간을 구간별로 나눠 조회한 뒤 합산해 당일 총 거리/시간을 계산한다.
    // waypoints 한 번 호출 대신 구간(leg)마다 start/goal만 있는 단순 호출을 반복 - Directions의 경유지 개수 상한과 무관해짐.
    // DB에는 쓰지 않고 결과만 반환 - 실제 반영은 CompanyRouteResequenceWriter의 짧은 트랜잭션에서 처리한다.
    private CompanyRouteResequenceWriter.RouteTotal calculateRouteTotal(UUID agentId, HubServiceHubResponseDto hub,
                                   Map<UUID, CompanyDeliveryRouteRecord> recordsById, List<UUID> ordered) {
        List<String> points = new ArrayList<>();
        points.add(NaverCoordinateFormatter.format(hub.latitude(), hub.longitude()));
        for (UUID recordId : ordered) {
            CompanyDeliveryRouteRecord record = recordsById.get(recordId);
            if (record.getLatitude() == null || record.getLongitude() == null) {
                // 좌표 없는 stop이 섞여 있으면 전체 경로 좌표열이 끊기므로 총 동선 계산은 건너뜀 (개별 sequence는 그대로 반영됨)
                return null;
            }
            points.add(NaverCoordinateFormatter.format(record.getLatitude(), record.getLongitude()));
        }

        int totalDistanceMeters = 0;
        long totalDurationMillis = 0;
        for (int i = 0; i < points.size() - 1; i++) {
            NaverDirectionsResponseDto.Summary legSummary = fetchLegSummary(agentId, points.get(i), points.get(i + 1));
            if (legSummary == null) {
                // 구간 하나라도 실패하면 부분 합산은 의미가 없으므로 총 동선 계산 자체를 건너뜀 (개별 sequence는 그대로 반영됨)
                return null;
            }
            totalDistanceMeters += legSummary.distance();
            totalDurationMillis += legSummary.duration();
        }

        return new CompanyRouteResequenceWriter.RouteTotal(
            (int) Math.round(totalDistanceMeters / 1000.0),
            (int) Math.round(totalDurationMillis / 60000.0)
        );
    }

    // 구간 하나(start→goal, 경유지 없음)를 최대 3회 재시도로 조회 - 계속 실패하면 null
    private NaverDirectionsResponseDto.Summary fetchLegSummary(UUID agentId, String start, String goal) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                NaverDirectionsResponseDto response =
                    naverDirectionsClient.getDirections(start, goal, null, NAVER_DIRECTIONS_OPTION);
                List<NaverDirectionsResponseDto.Route> routes =
                    response.route() == null ? null : response.route().get(NAVER_DIRECTIONS_OPTION);
                if (routes == null || routes.isEmpty()) {
                    return null;
                }
                return routes.get(0).summary();
            } catch (FeignException e) {
                log.warn("Naver Directions(구간별) 호출 실패 ({}/{}회) - agentId={}, start={}, goal={}, status={}",
                    attempt, MAX_ATTEMPTS, agentId, start, goal, e.status(), e);
            }
        }
        return null;
    }

    private HubServiceHubResponseDto fetchHub(UUID hubId) {
        try {
            return hubQueryService.getHub(hubId);
        } catch (FeignException e) {
            log.warn("허브 좌표 조회 실패로 방문 순서 자동 계산을 건너뜁니다 - hubId={}, status={}", hubId, e.status(), e);
            return null;
        }
    }
}
