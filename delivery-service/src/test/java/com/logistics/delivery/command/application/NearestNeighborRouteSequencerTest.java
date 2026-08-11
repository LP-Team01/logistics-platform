package com.logistics.delivery.command.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.logistics.delivery.command.application.NearestNeighborRouteSequencer.Stop;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NearestNeighborRouteSequencerTest {

    @Test
    @DisplayName("출발점에서 가까운 정류지부터 순서대로 방문 순서를 매긴다")
    void ordersStopsByNearestFirst() {
        UUID near = UUID.randomUUID();
        UUID mid = UUID.randomUUID();
        UUID far = UUID.randomUUID();
        // 출발점(37.50, 127.00) 기준 위도 차이만큼 거리 차가 생기도록 구성
        List<Stop> stops = List.of(
            new Stop(far, 37.80, 127.00),
            new Stop(near, 37.51, 127.00),
            new Stop(mid, 37.60, 127.00)
        );

        List<UUID> ordered = NearestNeighborRouteSequencer.order(37.50, 127.00, stops);

        assertEquals(List.of(near, mid, far), ordered);
    }

    @Test
    @DisplayName("가장 가까운 정류지를 방문한 뒤에는 그 지점을 기준으로 다음 가장 가까운 정류지를 고른다(탐욕적 선택)")
    void greedilyPicksNextNearestFromCurrentPosition() {
        UUID r1 = UUID.randomUUID();
        UUID l1 = UUID.randomUUID();
        UUID r2 = UUID.randomUUID();
        List<Stop> stops = List.of(
            new Stop(l1, 37.47, 127.00), // 출발점 대비 위도차 0.03
            new Stop(r1, 37.52, 127.00), // 출발점 대비 위도차 0.02 (출발점 기준 최근접)
            new Stop(r2, 37.55, 127.00)  // 출발점 대비 위도차 0.05
        );

        List<UUID> ordered = NearestNeighborRouteSequencer.order(37.50, 127.00, stops);

        // 출발점 기준 거리로만 정렬하면 [r1, l1, r2]이지만, r1(37.52)을 먼저 방문하고 나면
        // 그 위치에서는 l1(거리 0.05)보다 r2(거리 0.03)가 더 가까워 탐욕적으로 r2를 먼저 방문한다
        assertEquals(List.of(r1, r2, l1), ordered);
    }

    @Test
    @DisplayName("좌표가 없는(지오코딩 실패) 정류지는 순서를 매길 수 없어 결과 맨 뒤에 원래 순서대로 붙는다")
    void appendsStopsWithoutCoordinatesAtTheEnd() {
        UUID resolved = UUID.randomUUID();
        UUID unresolvedFirst = UUID.randomUUID();
        UUID unresolvedSecond = UUID.randomUUID();
        List<Stop> stops = List.of(
            new Stop(unresolvedFirst, null, null),
            new Stop(resolved, 37.51, 127.00),
            new Stop(unresolvedSecond, null, null)
        );

        List<UUID> ordered = NearestNeighborRouteSequencer.order(37.50, 127.00, stops);

        assertEquals(List.of(resolved, unresolvedFirst, unresolvedSecond), ordered);
    }

    @Test
    @DisplayName("정류지가 없으면 빈 결과를 반환한다")
    void returnsEmptyListForNoStops() {
        List<UUID> ordered = NearestNeighborRouteSequencer.order(37.50, 127.00, List.of());

        assertTrue(ordered.isEmpty());
    }
}