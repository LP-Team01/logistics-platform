package com.logistics.hub.hubroute.service;

import com.logistics.hub.global.exception.BusinessException;
import com.logistics.hub.global.exception.ErrorCode;
import com.logistics.hub.hub.repository.HubRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class HubRoutePathService {

    private static final Set<String> VALID_CRITERIA = Set.of("distance", "duration");

    private final HubRepository hubRepository;
    private final HubRouteGraphService hubRouteGraphService;

    private DijkstraResult dijkstra(Map<UUID, List<HubRouteGraphService.Edge>> graph, UUID startHubId, String criteria) {
        Map<UUID, Double> shortestDistance = new HashMap<>();
        Map<UUID, UUID> previousHub = new HashMap<>();
        Set<UUID> confirmed = new HashSet<>(); // 확정된 허브 ID들

        for (UUID hubId : graph.keySet()) {
            shortestDistance.put(hubId, Double.MAX_VALUE);
        }
        shortestDistance.put(startHubId, 0.0); // 출발지 0으로 덮어쓰기

        while (confirmed.size() < graph.size()) { // 모든 허브 확정될 때까지 반복
            UUID current = findClosestUnconfirmedHub(shortestDistance, confirmed);
            if (current == null) { // 그래프 끊겨있거나 더 이상 갈 수 있는 곳이 없을 경우
                break;
            }
            confirmed.add(current);

            for (HubRouteGraphService.Edge edge : graph.getOrDefault(current, List.of())) {
                if (confirmed.contains(edge.neighborHubId())) { // 이미 확정 허브라면 건너뛰기
                    continue;
                }

                double weight = "duration".equals(criteria) ? edge.duration() : edge.distance(); // 가중치 기준 결정
                double candidateDistance = shortestDistance.get(current) + weight;

                if (candidateDistance < shortestDistance.get(edge.neighborHubId())) {
                    shortestDistance.put(edge.neighborHubId(), candidateDistance); // 후보값으로 갱신
                    previousHub.put(edge.neighborHubId(), current);
                }
            }
        }

        return new DijkstraResult(shortestDistance, previousHub);
    }

    // 확정 안 된 허브들 중에서, 지금까지 알려진 거리가 제일 짧은 게 어디인지 찾는 메서드
    private UUID findClosestUnconfirmedHub(Map<UUID, Double> shortestDistance, Set<UUID> confirmed) {
        UUID closest = null;
        double minDistance = Double.MAX_VALUE;

        for (Map.Entry<UUID, Double> entry : shortestDistance.entrySet()) {
            if (!confirmed.contains(entry.getKey()) && entry.getValue() < minDistance) {
                closest = entry.getKey();
                minDistance = entry.getValue();
            }
        }
        return closest;
    }

    // 뒤에서부터 경로를 거꾸로 추적하는 메서드 (경로 복원)
    private List<UUID> reconstructPath(Map<UUID, UUID> previousHub, UUID startHubId, UUID endHubId) {
        LinkedList<UUID> path = new LinkedList<>();
        UUID current = endHubId; // 도착지부터 시작

        while (current != null) {
            path.addFirst(current); // 리스트 맨 앞에 끼워넣음
            current = previousHub.get(current);
        }

        // 경로 없음 (도착지 도달 불가능)
        if (path.isEmpty() || !path.getFirst().equals(startHubId)) {
            return List.of(); // 빈 리스트 반환
        } // todo: 나중에 에러 처리랑 연결

        return path;
    }

    // 각 구간의 원본 거리/시간 정보를 다시 찾아 채우는 메서드
    private List<PathSegment> buildSegments(Map<UUID, List<HubRouteGraphService.Edge>> graph, List<UUID> hubIdOrder) {
        List<PathSegment> segments = new ArrayList<>();
        for (int i = 0; i < hubIdOrder.size() - 1; i++) {
            UUID from = hubIdOrder.get(i);
            UUID to = hubIdOrder.get(i + 1); // 경로를 두 허브씩 짝지어 순회

            HubRouteGraphService.Edge edge = graph.get(from).stream()
                .filter(e -> e.neighborHubId().equals(to))
                .findFirst()
                .orElseThrow(); // from-to는 이미 연결이 확인된 구간이라 못 찾으면 버그 발생

            segments.add(new PathSegment(from, to, edge.distance(), edge.duration())); // 거리, 시간 정보 둘 다 추가
        }
        return segments;
    }

    // 최단 경로 계산해서 결과 돌려주는 메서드
    public PathResult findShortestPath(UUID startHubId, UUID endHubId, String criteria) {
        if (!VALID_CRITERIA.contains(criteria)) {
            throw new BusinessException(ErrorCode.INVALID_CRITERIA);
        }
        if (startHubId.equals(endHubId)) {
            throw new BusinessException(ErrorCode.SAME_HUB_ROUTE);
        }

        hubRepository.findByHubIdAndDeletedAtIsNull(startHubId)
            .orElseThrow(() -> new BusinessException(ErrorCode.HUB_NOT_FOUND));
        hubRepository.findByHubIdAndDeletedAtIsNull(endHubId)
            .orElseThrow(() -> new BusinessException(ErrorCode.HUB_NOT_FOUND));

        Map<UUID, List<HubRouteGraphService.Edge>> graph = hubRouteGraphService.buildGraph();
        DijkstraResult result = dijkstra(graph, startHubId, criteria);
        List<UUID> hubIdOrder = reconstructPath(result.previousHub(), startHubId, endHubId);

        if (hubIdOrder.isEmpty()) {
            throw new BusinessException(ErrorCode.PATH_NOT_FOUND);
        }

        List<PathSegment> segments = buildSegments(graph, hubIdOrder);

        double totalDistance = segments.stream().mapToDouble(PathSegment::distance).sum();
        int totalDuration = segments.stream().mapToInt(PathSegment::duration).sum();

        return new PathResult(segments, totalDistance, totalDuration);
    }



    private record DijkstraResult(Map<UUID, Double> shortestDistance, Map<UUID, UUID> previousHub) {
    }

    public record PathResult(List<PathSegment> segments, double totalDistance, int totalDuration) {
    }

    public record PathSegment(UUID departureHubId, UUID arrivalHubId, double distance, int duration) {
    }

}
