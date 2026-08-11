package com.logistics.delivery.infrastructure.client.external.naver.dto;

import java.util.List;

// Geocoding 응답 형태 중 사용하는 필드만 매핑. x=경도, y=위도 (문자열로 내려옴)
public record NaverGeocodingResponseDto(
    String status,
    List<Address> addresses,
    String errorMessage
) {
    public record Address(String roadAddress, String jibunAddress, String x, String y) {
    }
}
