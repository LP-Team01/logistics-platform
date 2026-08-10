package com.logistics.delivery.infrastructure.client.external.naver;

// 네이버 Directions/Geocoding API는 "경도,위도" 순서를 요구
public final class NaverCoordinateFormatter {

    private NaverCoordinateFormatter() {
    }

    public static String format(double latitude, double longitude) {
        return longitude + "," + latitude;
    }
}
