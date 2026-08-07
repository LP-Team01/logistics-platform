package com.logistics.delivery.command.application;

import com.logistics.delivery.infrastructure.client.HubServiceClient;
import com.logistics.delivery.infrastructure.client.dto.HubServiceHubResponseDto;
import com.logistics.delivery.infrastructure.client.external.naver.NaverCoordinateFormatter;
import com.logistics.delivery.infrastructure.client.external.naver.NaverDirectionsClient;
import com.logistics.delivery.infrastructure.client.external.naver.NaverGeocodingClient;
import com.logistics.delivery.infrastructure.client.external.naver.dto.NaverDirectionsResponseDto;
import com.logistics.delivery.infrastructure.client.external.naver.dto.NaverGeocodingResponseDto;
import feign.FeignException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

// 업체 배송지 좌표(Geocoding) + 목적지 허브→업체 구간 예상 경로 산출 전담.
@Slf4j
@Component
@RequiredArgsConstructor
public class CompanyRouteEstimator {

    private final HubServiceClient hubServiceClient;
    private final NaverGeocodingClient naverGeocodingClient;
    private final NaverDirectionsClient naverDirectionsClient;

    // 배송 용도에 맞는 옵션 값(실시간 빠른 길) 선택
    private static final String NAVER_DIRECTIONS_OPTION = "trafast";

    // Naver 호출 실패 시 정책: 최대 3회 재시도, 3회 모두 실패하면 null로 남기고 수동 등록 API로 보정
    private static final int MAX_ATTEMPTS = 3;

    public CompanyRouteEstimate estimate(UUID destinationHubId, String deliveryAddress) {
        Coordinate companyCoordinate = geocodeCompanyAddress(deliveryAddress);
        if (companyCoordinate == null) {
            return new CompanyRouteEstimate(null, null, null, null);
        }
        RouteEstimate routeEstimate = estimateCompanyRoute(destinationHubId, companyCoordinate);
        return new CompanyRouteEstimate(
            companyCoordinate.latitude(),
            companyCoordinate.longitude(),
            routeEstimate == null ? null : routeEstimate.distanceKm(),
            routeEstimate == null ? null : routeEstimate.durationMin()
        );
    }

    private Coordinate geocodeCompanyAddress(String address) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                NaverGeocodingResponseDto response = naverGeocodingClient.geocode(address);
                if (response.addresses() == null || response.addresses().isEmpty()) {
                    log.warn("Naver Geocoding: 주소를 인식하지 못했습니다 - address={}", address);
                    return null;
                }
                NaverGeocodingResponseDto.Address geocoded = response.addresses().get(0);
                return new Coordinate(Double.parseDouble(geocoded.y()), Double.parseDouble(geocoded.x()));
            } catch (FeignException e) {
                log.warn("Naver Geocoding 호출 실패 ({}/{}회) - address={}, status={}",
                    attempt, MAX_ATTEMPTS, address, e.status(), e);
            }
        }
        return null;
    }

    private RouteEstimate estimateCompanyRoute(UUID destinationHubId, Coordinate companyCoordinate) {
        HubServiceHubResponseDto hub;
        try {
            hub = hubServiceClient.getHub(destinationHubId);
        } catch (FeignException e) {
            log.warn("허브 조회 실패로 Naver Directions 호출을 건너뜁니다 - destinationHubId={}, status={}",
                destinationHubId, e.status(), e);
            return null;
        }
        if (hub.latitude() == null || hub.longitude() == null) {
            return null;
        }
        String start = NaverCoordinateFormatter.format(hub.latitude(), hub.longitude());
        String goal = NaverCoordinateFormatter.format(companyCoordinate.latitude(), companyCoordinate.longitude());

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                NaverDirectionsResponseDto response =
                    naverDirectionsClient.getDirections(start, goal, null, NAVER_DIRECTIONS_OPTION);
                List<NaverDirectionsResponseDto.Route> routes =
                    response.route() == null ? null : response.route().get(NAVER_DIRECTIONS_OPTION);
                if (routes == null || routes.isEmpty()) {
                    return null;
                }
                NaverDirectionsResponseDto.Summary summary = routes.get(0).summary();
                // m -> km, ms -> 분
                return new RouteEstimate(
                    (int) Math.round(summary.distance() / 1000.0),
                    (int) Math.round(summary.duration() / 60000.0)
                );
            } catch (FeignException e) {
                log.warn("Naver Directions 호출 실패 ({}/{}회) - destinationHubId={}, status={}",
                    attempt, MAX_ATTEMPTS, destinationHubId, e.status(), e);
            }
        }
        return null;
    }

    private record Coordinate(double latitude, double longitude) {
    }

    private record RouteEstimate(Integer distanceKm, Integer durationMin) {
    }

    public record CompanyRouteEstimate(Double latitude, Double longitude, Integer distanceKm, Integer durationMin) {
    }
}
