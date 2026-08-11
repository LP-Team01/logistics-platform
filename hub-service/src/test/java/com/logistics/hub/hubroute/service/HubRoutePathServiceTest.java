package com.logistics.hub.hubroute.service;

import com.logistics.hub.global.exception.BusinessException;
import com.logistics.hub.global.exception.ErrorCode;
import com.logistics.hub.hub.entity.Hub;
import com.logistics.hub.hub.repository.HubRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HubRoutePathServiceTest {

    @Mock
    private HubRepository hubRepository;

    @Mock
    private HubRouteGraphService hubRouteGraphService;

    @InjectMocks
    private HubRoutePathService hubRoutePathService;

    private static final UUID HUB_A = UUID.randomUUID();
    private static final UUID HUB_B = UUID.randomUUID();
    private static final UUID HUB_C = UUID.randomUUID();
    private static final UUID HUB_D = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        lenient().when(hubRepository.findByHubIdAndDeletedAtIsNull(any()))
            .thenAnswer(invocation -> {
                UUID id = invocation.getArgument(0);
                if (id.equals(HUB_A) || id.equals(HUB_B) || id.equals(HUB_C) || id.equals(HUB_D)) {
                    return Optional.of(Mockito.mock(Hub.class));
                }
                return Optional.empty();
            });
    }

    private Map<UUID, List<HubRouteGraphService.Edge>> buildTestGraph() {
        Map<UUID, List<HubRouteGraphService.Edge>> graph = new HashMap<>();
        addBidirectionalEdge(graph, HUB_A, HUB_B, 100.0, 50);
        addBidirectionalEdge(graph, HUB_A, HUB_C, 10.0, 100);
        addBidirectionalEdge(graph, HUB_C, HUB_B, 10.0, 10);
        graph.put(HUB_D, new ArrayList<>());
        return graph;
    }

    private void addBidirectionalEdge(
        Map<UUID, List<HubRouteGraphService.Edge>> graph, UUID from, UUID to, double distance, int duration
    ) {
        graph.computeIfAbsent(from, k -> new ArrayList<>())
            .add(new HubRouteGraphService.Edge(to, distance, duration));
        graph.computeIfAbsent(to, k -> new ArrayList<>())
            .add(new HubRouteGraphService.Edge(from, distance, duration));
    }

    @Test
    @DisplayName("직접 연결보다 경유 경로가 더 짧으면 경유를 선택한다 (distance 기준)")
    void findShortestPath_prefersShorterViaRoute_whenDistanceCriteria() {
        when(hubRouteGraphService.buildGraph()).thenReturn(buildTestGraph());

        HubRoutePathService.PathResult result =
            hubRoutePathService.findShortestPath(HUB_A, HUB_B, "distance");

        assertThat(result.totalDistance()).isEqualTo(20.0);
        assertThat(result.segments()).hasSize(2);
        assertThat(result.segments().get(0).departureHubId()).isEqualTo(HUB_A);
        assertThat(result.segments().get(0).arrivalHubId()).isEqualTo(HUB_C);
        assertThat(result.segments().get(1).departureHubId()).isEqualTo(HUB_C);
        assertThat(result.segments().get(1).arrivalHubId()).isEqualTo(HUB_B);
    }

    @Test
    @DisplayName("distance와 duration 기준의 최적 경로가 다를 수 있다")
    void findShortestPath_differsBetweenDistanceAndDuration() {
        when(hubRouteGraphService.buildGraph()).thenReturn(buildTestGraph());

        HubRoutePathService.PathResult distanceResult =
            hubRoutePathService.findShortestPath(HUB_A, HUB_B, "distance");
        HubRoutePathService.PathResult durationResult =
            hubRoutePathService.findShortestPath(HUB_A, HUB_B, "duration");

        assertThat(distanceResult.segments()).hasSize(2);
        assertThat(durationResult.segments()).hasSize(1);
        assertThat(durationResult.totalDuration()).isEqualTo(50);
    }

    @Test
    @DisplayName("도달 불가능한 허브면 PATH_NOT_FOUND 예외를 던진다")
    void findShortestPath_throwsPathNotFound_whenUnreachable() {
        when(hubRouteGraphService.buildGraph()).thenReturn(buildTestGraph());

        assertThatThrownBy(() -> hubRoutePathService.findShortestPath(HUB_A, HUB_D, "distance"))
            .isInstanceOf(BusinessException.class)
            .extracting(ex -> ((BusinessException) ex).getErrorCode())
            .isEqualTo(ErrorCode.PATH_NOT_FOUND);
    }

    @Test
    @DisplayName("출발 허브와 도착 허브가 같으면 SAME_HUB_ROUTE 예외를 던진다")
    void findShortestPath_throwsSameHubRoute_whenSameHub() {
        assertThatThrownBy(() -> hubRoutePathService.findShortestPath(HUB_A, HUB_A, "distance"))
            .isInstanceOf(BusinessException.class)
            .extracting(ex -> ((BusinessException) ex).getErrorCode())
            .isEqualTo(ErrorCode.SAME_HUB_ROUTE);
    }

    @Test
    @DisplayName("존재하지 않는 허브면 HUB_NOT_FOUND 예외를 던진다")
    void findShortestPath_throwsHubNotFound_whenHubDoesNotExist() {
        UUID nonExistentHub = UUID.randomUUID();

        assertThatThrownBy(() -> hubRoutePathService.findShortestPath(HUB_A, nonExistentHub, "distance"))
            .isInstanceOf(BusinessException.class)
            .extracting(ex -> ((BusinessException) ex).getErrorCode())
            .isEqualTo(ErrorCode.HUB_NOT_FOUND);
    }

    @Test
    @DisplayName("criteria가 distance/duration이 아니면 INVALID_CRITERIA 예외를 던진다")
    void findShortestPath_throwsInvalidCriteria_whenCriteriaIsInvalid() {
        assertThatThrownBy(() -> hubRoutePathService.findShortestPath(HUB_A, HUB_B, "duraton"))
            .isInstanceOf(BusinessException.class)
            .extracting(ex -> ((BusinessException) ex).getErrorCode())
            .isEqualTo(ErrorCode.INVALID_CRITERIA);
    }
}
