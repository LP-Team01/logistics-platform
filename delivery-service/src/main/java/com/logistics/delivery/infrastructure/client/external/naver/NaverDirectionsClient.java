package com.logistics.delivery.infrastructure.client.external.naver;

import com.logistics.delivery.infrastructure.client.external.naver.config.NaverFeignConfig;
import com.logistics.delivery.infrastructure.client.external.naver.dto.NaverDirectionsResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
    name = "naver-directions",
    url = "${naver.map.directions-base-url}",
    configuration = NaverFeignConfig.class
)
public interface NaverDirectionsClient {

    // start/goal/waypoints는 "경도,위도" 순서 문자열 - NaverCoordinateFormatter로만 생성할 것
    @GetMapping("/map-direction/v1/driving")
    NaverDirectionsResponseDto getDirections(
        @RequestParam("start") String start,
        @RequestParam("goal") String goal,
        @RequestParam(value = "waypoints", required = false) String waypoints,
        @RequestParam("option") String option
    );
}
