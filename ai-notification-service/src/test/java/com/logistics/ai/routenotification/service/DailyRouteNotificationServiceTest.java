package com.logistics.ai.routenotification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.logistics.ai.routenotification.client.DeliveryServiceClient;
import com.logistics.ai.routenotification.dto.responsedto.DailyRouteAiResult;
import com.logistics.ai.routenotification.dto.responsedto.DeliveryAgentPageResponseDto;
import com.logistics.ai.routenotification.dto.responsedto.DeliveryAgentPageResponseDto.DeliveryAgentSummary;
import com.logistics.ai.routenotification.dto.responsedto.TodayRouteResponseDto;
import com.logistics.ai.slackmessage.dto.requestdto.SlackMessageRequestDto;
import com.logistics.ai.slackmessage.dto.responsedto.SlackMessageResponseDto;
import com.logistics.ai.slackmessage.entity.SlackMessageStatus;
import com.logistics.ai.slackmessage.entity.SlackMessageType;
import com.logistics.ai.slackmessage.service.SlackMessageService;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DailyRouteNotificationServiceTest {

    @Mock
    private DeliveryServiceClient deliveryServiceClient;

    @Mock
    private DailyRoutePromptService dailyRoutePromptService;

    @Mock
    private DailyRouteAiClient dailyRouteAiClient;

    @Mock
    private SlackMessageService slackMessageService;

    @InjectMocks
    private DailyRouteNotificationService notificationService;

    @Test
    @DisplayName("한 담당자의 처리가 실패해도 다음 담당자의 알림은 계속 처리한다")
    void continuesWithNextAgentWhenOneAgentFails() {
        // given
        LocalDate notificationDate =
            LocalDate.of(2026, 8, 11);

        UUID failedAgentId = UUID.randomUUID();
        UUID successfulAgentId = UUID.randomUUID();

        DeliveryAgentSummary failedAgent =
            createAgent(
                failedAgentId,
                "U_FAILED"
            );

        DeliveryAgentSummary successfulAgent =
            createAgent(
                successfulAgentId,
                "U_SUCCESS"
            );

        DeliveryAgentPageResponseDto agentPage =
            org.mockito.Mockito.mock(
                DeliveryAgentPageResponseDto.class
            );

        when(agentPage.content())
            .thenReturn(
                List.of(
                    failedAgent,
                    successfulAgent
                )
            );

        when(agentPage.totalPages())
            .thenReturn(1);

        when(
            deliveryServiceClient.getCompanyDeliveryAgents(
                "COMPANY_DELIVERY",
                true,
                0,
                100
            )
        ).thenReturn(agentPage);

        when(
            slackMessageService
                .findSlackMessageByAiRequestAndRecipient(
                    any(UUID.class),
                    any(UUID.class)
                )
        ).thenReturn(Optional.empty());

        when(
            deliveryServiceClient.getTodayRoute(
                failedAgentId
            )
        ).thenThrow(
            new RuntimeException(
                "Delivery Service 호출 실패"
            )
        );

        TodayRouteResponseDto successfulRoute =
            org.mockito.Mockito.mock(
                TodayRouteResponseDto.class
            );

        when(successfulRoute.agentId())
            .thenReturn(successfulAgentId);

        when(
            deliveryServiceClient.getTodayRoute(
                successfulAgentId
            )
        ).thenReturn(successfulRoute);

        when(
            dailyRoutePromptService.createPrompt(
                successfulRoute
            )
        ).thenReturn("정상 담당자 프롬프트");

        when(
            dailyRouteAiClient.generateDailyRouteMessage(
                "정상 담당자 프롬프트"
            )
        ).thenReturn(
            new DailyRouteAiClient.AiExecutionResult(
                """
                {"message":"정상 담당자의 경로 안내입니다."}
                """,
                new DailyRouteAiResult(
                    "정상 담당자의 경로 안내입니다."
                ),
                "gemini-test",
                100L
            )
        );

        ArgumentCaptor<SlackMessageRequestDto> requestCaptor =
            ArgumentCaptor.forClass(
                SlackMessageRequestDto.class
            );

        // when
        notificationService.sendDailyRouteNotifications(
            notificationDate
        );

        // then
        verify(
            deliveryServiceClient
        ).getTodayRoute(failedAgentId);

        verify(
            deliveryServiceClient
        ).getTodayRoute(successfulAgentId);

        verify(
            slackMessageService
        ).createOrRetrySlackMessage(
            requestCaptor.capture()
        );

        SlackMessageRequestDto sentRequest =
            requestCaptor.getValue();

        assertThat(sentRequest.recipientUserId())
            .isEqualTo(successfulAgentId);

        assertThat(sentRequest.recipientSlackId())
            .isEqualTo("U_SUCCESS");

        assertThat(sentRequest.content())
            .isEqualTo(
                "정상 담당자의 경로 안내입니다."
            );
    }

    @Test
    @DisplayName("기존 알림이 실패 상태이면 저장된 메시지를 재발송한다")
    void retriesStoredMessageWhenExistingNotificationFailed() {
        // given
        LocalDate notificationDate =
            LocalDate.of(2026, 8, 11);

        UUID agentId = UUID.randomUUID();

        UUID notificationId =
            createExpectedNotificationId(
                notificationDate,
                agentId
            );

        DeliveryAgentSummary agent =
            createAgent(
                agentId,
                "U0123456789"
            );

        DeliveryAgentPageResponseDto agentPage =
            createSingleAgentPage(agent);

        SlackMessageResponseDto failedMessage =
            org.mockito.Mockito.mock(
                SlackMessageResponseDto.class
            );

        when(failedMessage.status())
            .thenReturn(SlackMessageStatus.FAILED);

        when(failedMessage.aiRequestId())
            .thenReturn(notificationId);

        when(failedMessage.recipientUserId())
            .thenReturn(agentId);

        when(failedMessage.recipientSlackId())
            .thenReturn("U0123456789");

        when(failedMessage.messageType())
            .thenReturn(SlackMessageType.DAILY_ROUTE);

        when(failedMessage.title())
            .thenReturn(
                "[오늘의 배송 경로] 2026-08-11"
            );

        when(failedMessage.content())
            .thenReturn(
                "기존에 생성된 배송 경로 안내입니다."
            );

        when(
            deliveryServiceClient.getCompanyDeliveryAgents(
                "COMPANY_DELIVERY",
                true,
                0,
                100
            )
        ).thenReturn(agentPage);

        when(
            slackMessageService
                .findSlackMessageByAiRequestAndRecipient(
                    notificationId,
                    agentId
                )
        ).thenReturn(
            Optional.of(failedMessage)
        );

        ArgumentCaptor<SlackMessageRequestDto> requestCaptor =
            ArgumentCaptor.forClass(
                SlackMessageRequestDto.class
            );

        // when
        notificationService.sendDailyRouteNotifications(
            notificationDate
        );

        // then
        verify(
            slackMessageService
        ).createOrRetrySlackMessage(
            requestCaptor.capture()
        );

        SlackMessageRequestDto retryRequest =
            requestCaptor.getValue();

        assertThat(retryRequest.aiRequestId())
            .isEqualTo(notificationId);

        assertThat(retryRequest.content())
            .isEqualTo(
                "기존에 생성된 배송 경로 안내입니다."
            );

        verify(
            deliveryServiceClient,
            never()
        ).getTodayRoute(any(UUID.class));

        verify(
            dailyRoutePromptService,
            never()
        ).createPrompt(any());

        verify(
            dailyRouteAiClient,
            never()
        ).generateDailyRouteMessage(any());
    }

    @Test
    @DisplayName("당일 경로를 AI 메시지로 생성하여 Slack으로 발송한다")
    void sendsAiGeneratedDailyRouteMessageToSlack() {
        // given
        LocalDate notificationDate =
            LocalDate.of(2026, 8, 11);

        UUID agentId = UUID.randomUUID();
        String slackId = "U0123456789";

        DeliveryAgentSummary agent =
            createAgent(agentId, slackId);

        DeliveryAgentPageResponseDto agentPage =
            createSingleAgentPage(agent);

        TodayRouteResponseDto todayRoute =
            org.mockito.Mockito.mock(
                TodayRouteResponseDto.class
            );

        when(todayRoute.agentId())
            .thenReturn(agentId);

        when(
            deliveryServiceClient.getCompanyDeliveryAgents(
                "COMPANY_DELIVERY",
                true,
                0,
                100
            )
        ).thenReturn(agentPage);

        when(
            slackMessageService
                .findSlackMessageByAiRequestAndRecipient(
                    any(UUID.class),
                    eq(agentId)
                )
        ).thenReturn(Optional.empty());

        when(
            deliveryServiceClient.getTodayRoute(agentId)
        ).thenReturn(todayRoute);

        when(
            dailyRoutePromptService.createPrompt(todayRoute)
        ).thenReturn("당일 경로 안내 프롬프트");

        when(
            dailyRouteAiClient.generateDailyRouteMessage(
                "당일 경로 안내 프롬프트"
            )
        ).thenReturn(
            new DailyRouteAiClient.AiExecutionResult(
                """
                {"message":"오늘의 배송 경로입니다."}
                """,
                new DailyRouteAiResult(
                    "오늘의 배송 경로입니다."
                ),
                "gemini-test",
                100L
            )
        );

        ArgumentCaptor<SlackMessageRequestDto> requestCaptor =
            ArgumentCaptor.forClass(
                SlackMessageRequestDto.class
            );

        // when
        notificationService.sendDailyRouteNotifications(
            notificationDate
        );

        // then
        verify(
            slackMessageService
        ).createOrRetrySlackMessage(
            requestCaptor.capture()
        );

        SlackMessageRequestDto request =
            requestCaptor.getValue();

        UUID expectedNotificationId =
            createExpectedNotificationId(
                notificationDate,
                agentId
            );

        assertThat(request.aiRequestId())
            .isEqualTo(expectedNotificationId);

        assertThat(request.recipientUserId())
            .isEqualTo(agentId);

        assertThat(request.recipientSlackId())
            .isEqualTo(slackId);

        assertThat(request.messageType())
            .isEqualTo(SlackMessageType.DAILY_ROUTE);

        assertThat(request.title())
            .isEqualTo(
                "[오늘의 배송 경로] 2026-08-11"
            );

        assertThat(request.content())
            .isEqualTo("오늘의 배송 경로입니다.");
    }

    @Test
    @DisplayName("이미 발송된 알림이면 Delivery와 Gemini를 다시 호출하지 않는다")
    void skipsExternalCallsWhenNotificationWasAlreadySent() {
        // given
        LocalDate notificationDate =
            LocalDate.of(2026, 8, 11);

        UUID agentId = UUID.randomUUID();

        DeliveryAgentSummary agent =
            createAgent(
                agentId,
                "U0123456789"
            );

        DeliveryAgentPageResponseDto agentPage =
            createSingleAgentPage(agent);

        SlackMessageResponseDto existingMessage =
            org.mockito.Mockito.mock(
                SlackMessageResponseDto.class
            );

        when(existingMessage.status())
            .thenReturn(SlackMessageStatus.SENT);

        when(existingMessage.slackMessageId())
            .thenReturn(UUID.randomUUID());

        when(
            deliveryServiceClient.getCompanyDeliveryAgents(
                "COMPANY_DELIVERY",
                true,
                0,
                100
            )
        ).thenReturn(agentPage);

        when(
            slackMessageService
                .findSlackMessageByAiRequestAndRecipient(
                    any(UUID.class),
                    eq(agentId)
                )
        ).thenReturn(
            Optional.of(existingMessage)
        );

        // when
        notificationService.sendDailyRouteNotifications(
            notificationDate
        );

        // then
        verify(
            deliveryServiceClient,
            never()
        ).getTodayRoute(any(UUID.class));

        verify(
            dailyRoutePromptService,
            never()
        ).createPrompt(any());

        verify(
            dailyRouteAiClient,
            never()
        ).generateDailyRouteMessage(any());

        verify(
            slackMessageService,
            never()
        ).createOrRetrySlackMessage(any());
    }

    private DeliveryAgentSummary createAgent(
        UUID agentId,
        String slackId
    ) {
        DeliveryAgentSummary agent =
            org.mockito.Mockito.mock(
                DeliveryAgentSummary.class
            );

        when(agent.agentId())
            .thenReturn(agentId);

        when(agent.slackId())
            .thenReturn(slackId);

        return agent;
    }

    private DeliveryAgentPageResponseDto createSingleAgentPage(
        DeliveryAgentSummary agent
    ) {
        DeliveryAgentPageResponseDto agentPage =
            org.mockito.Mockito.mock(
                DeliveryAgentPageResponseDto.class
            );

        when(agentPage.content())
            .thenReturn(List.of(agent));

        when(agentPage.totalPages())
            .thenReturn(1);

        return agentPage;
    }

    private UUID createExpectedNotificationId(
        LocalDate notificationDate,
        UUID agentId
    ) {
        String source =
            "daily-route-notification:"
                + notificationDate
                + ":"
                + agentId;

        return UUID.nameUUIDFromBytes(
            source.getBytes(StandardCharsets.UTF_8)
        );
    }
}
