package com.logistics.ai.routenotification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.logistics.ai.routenotification.dto.responsedto.TodayRouteResponseDto;
import com.logistics.ai.routenotification.dto.responsedto.TodayRouteResponseDto.Stop;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DailyRoutePromptServiceTest {

    private final DailyRoutePromptService promptService =
        new DailyRoutePromptService();

    @Test
    @DisplayName("배송 순서대로 당일 경로 프롬프트를 생성한다")
    void createsDailyRoutePromptInDeliverySequenceOrder() {
        // given
        UUID agentId = UUID.randomUUID();

        UUID firstDeliveryId = UUID.randomUUID();
        UUID secondDeliveryId = UUID.randomUUID();

        Stop secondStop = new Stop(
            UUID.randomUUID(),
            secondDeliveryId,
            UUID.randomUUID(),
            37.502,
            127.002,
            15,
            40,
            2,
            "WAITING"
        );

        Stop firstStop = new Stop(
            UUID.randomUUID(),
            firstDeliveryId,
            UUID.randomUUID(),
            37.501,
            127.001,
            10,
            30,
            1,
            "WAITING"
        );

        TodayRouteResponseDto route =
            new TodayRouteResponseDto(
                agentId,
                25,
                70,
                Instant.parse("2026-08-11T20:00:00Z"),
                List.of(secondStop, firstStop)
            );

        // when
        String prompt =
            promptService.createPrompt(route);

        // then
        assertThat(prompt)
            .contains("총 방문지 수: 2곳")
            .contains("총 예상 거리: 25 km")
            .contains("총 예상 시간: 70분")
            .contains("\"message\"");

        int firstStopPosition = prompt.indexOf(
            "1. 배송 ID=" + firstDeliveryId
        );

        int secondStopPosition = prompt.indexOf(
            "2. 배송 ID=" + secondDeliveryId
        );

        assertThat(firstStopPosition).isGreaterThanOrEqualTo(0);
        assertThat(secondStopPosition).isGreaterThan(
            firstStopPosition
        );
    }

    @Test
    @DisplayName("당일 경로가 null이면 예외가 발생한다")
    void throwsExceptionWhenTodayRouteIsNull() {
        assertThatThrownBy(
            () -> promptService.createPrompt(null)
        )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("당일 배송 경로 정보는 필수입니다.");
    }
}
