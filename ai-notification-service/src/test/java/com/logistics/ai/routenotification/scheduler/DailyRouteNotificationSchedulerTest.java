package com.logistics.ai.routenotification.scheduler;

import static org.mockito.Mockito.verify;

import com.logistics.ai.routenotification.service.DailyRouteNotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DailyRouteNotificationSchedulerTest {

    @Mock
    private DailyRouteNotificationService
        dailyRouteNotificationService;

    @InjectMocks
    private DailyRouteNotificationScheduler scheduler;

    @Test
    @DisplayName("스케줄러가 실행되면 당일 배송 경로 알림 서비스를 호출한다")
    void invokesDailyRouteNotificationService() {
        // when
        scheduler.sendDailyRouteNotifications();

        // then
        verify(
            dailyRouteNotificationService
        ).sendDailyRouteNotifications();
    }
}
