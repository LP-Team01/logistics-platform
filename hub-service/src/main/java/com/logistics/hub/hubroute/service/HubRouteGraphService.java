package com.logistics.hub.hubroute.service;

import com.logistics.hub.hubroute.entity.HubRoute;
import com.logistics.hub.hubroute.repository.HubRouteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HubRouteGraphService {
    private final HubRouteRepository hubRouteRepository;

    // 허브 ID를 key로, 그 허브와 연결된 이웃 정보 목록을 value로 갖는 인접 리스트 만든다.
    // 각 HubRoute 한 건을 양방향 간선으로 취급한다.
    @Cacheable(value = "hubRouteGraph", key = "'all'")
    public Map<UUID, List<Edge>> buildGraph() {
        List<HubRoute> routes = hubRouteRepository.findByDeletedAtIsNull();

        Map<UUID, List<Edge>> graph = new HashMap<>();
        for (HubRoute route : routes) {
            graph.computeIfAbsent(route.getDepartureHubId(), k -> new ArrayList<>())
                .add(new Edge(route.getArrivalHubId(), route.getDistance(), route.getDuration()));

            graph.computeIfAbsent(route.getArrivalHubId(), k -> new ArrayList<>())
                .add(new Edge(route.getDepartureHubId(), route.getDistance(), route.getDuration()));
        }
        return graph;
    }

    public record Edge(UUID neighborHubId, double distance, int duration) implements Serializable {
    }
}

