package com.logistics.delivery.command.application;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

// 업체배송담당자의 당일 방문 순서 1차 계산 전담
// 회사 간 실제 도로 거리가 아닌 좌표 간 직선거리(Haversine)로 상대적 순서만 정하면 충분하므로 외부 API 호출 없이 계산한다.
public final class NearestNeighborRouteSequencer {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private NearestNeighborRouteSequencer() {
    }

    public record Stop(UUID recordId, Double latitude, Double longitude) {
        boolean hasCoordinate() {
            return latitude != null && longitude != null;
        }
    }

    // 좌표가 없는 stop(지오코딩 실패)은 순서를 매길 수 없으므로 원래 순서 그대로 결과 맨 뒤에 붙인다.
    public static List<UUID> order(double startLat, double startLng, List<Stop> stops) {
        List<Stop> remaining = new ArrayList<>();
        List<UUID> unresolved = new ArrayList<>();
        for (Stop stop : stops) {
            if (stop.hasCoordinate()) {
                remaining.add(stop);
            } else {
                unresolved.add(stop.recordId());
            }
        }

        List<UUID> ordered = new ArrayList<>();
        double currentLat = startLat;
        double currentLng = startLng;
        while (!remaining.isEmpty()) {
            double fromLat = currentLat;
            double fromLng = currentLng;
            Stop nearest = remaining.stream()
                .min(Comparator.comparingDouble(stop -> haversineKm(fromLat, fromLng, stop.latitude(), stop.longitude())))
                .orElseThrow();
            ordered.add(nearest.recordId());
            currentLat = nearest.latitude();
            currentLng = nearest.longitude();
            remaining.remove(nearest);
        }

        ordered.addAll(unresolved);
        return ordered;
    }

    private static double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
            * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }
}
