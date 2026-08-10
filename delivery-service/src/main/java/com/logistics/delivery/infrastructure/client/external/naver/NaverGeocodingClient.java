package com.logistics.delivery.infrastructure.client.external.naver;

import com.logistics.delivery.infrastructure.client.external.naver.config.NaverFeignConfig;
import com.logistics.delivery.infrastructure.client.external.naver.dto.NaverGeocodingResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
    name = "naver-geocoding",
    url = "${naver.map.geocoding-base-url}",
    configuration = NaverFeignConfig.class
)
public interface NaverGeocodingClient {

    @GetMapping("/map-geocode/v2/geocode")
    NaverGeocodingResponseDto geocode(@RequestParam("query") String query);
}
