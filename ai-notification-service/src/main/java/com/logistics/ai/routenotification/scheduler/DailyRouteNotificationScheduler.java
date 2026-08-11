package com.logistics.ai.routenotification.scheduler;

import com.logistics.ai.routenotification.service.DailyRouteNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 설정된 시각에 당일 배송 경로 Slack 알림을 실행합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "daily-route-notification",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class DailyRouteNotificationScheduler {

    private final DailyRouteNotificationService
        dailyRouteNotificationService;

    /**
     * 기본값은 매일 한국 시간 오전 6시입니다.
     */
    @Scheduled(
        cron = "${daily-route-notification.cron:0 0 6 * * *}",
        zone = "${daily-route-notification.zone:Asia/Seoul}"
    )
    public void sendDailyRouteNotifications() {
        log.info(
            "당일 배송 경로 Slack 알림 스케줄을 시작합니다."
        );

        dailyRouteNotificationService
            .sendDailyRouteNotifications();

        log.info(
            "당일 배송 경로 Slack 알림 스케줄을 종료합니다."
        );
    }
}
