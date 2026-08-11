package com.logistics.ai.routenotification.service;

import com.logistics.ai.routenotification.client.DeliveryServiceClient;
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
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 업체 배송 담당자의 당일 경로를 조회하고
 * AI 메시지를 생성하여 Slack으로 발송합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DailyRouteNotificationService {

    private static final String COMPANY_DELIVERY =
        "COMPANY_DELIVERY";

    private static final String MESSAGE_TITLE =
        "[오늘의 배송 경로]";

    private static final int PAGE_SIZE = 100;

    private static final ZoneId SEOUL_ZONE =
        ZoneId.of("Asia/Seoul");

    private final DeliveryServiceClient deliveryServiceClient;
    private final DailyRoutePromptService dailyRoutePromptService;
    private final DailyRouteAiClient dailyRouteAiClient;
    private final SlackMessageService slackMessageService;

    /**
     * 현재 날짜를 기준으로 모든 업체 배송 담당자에게
     * 당일 배송 경로 알림을 발송합니다.
     */
    public void sendDailyRouteNotifications() {
        sendDailyRouteNotifications(
            LocalDate.now(SEOUL_ZONE)
        );
    }

    /**
     * 지정된 날짜를 기준으로 알림을 발송합니다.
     *
     * <p>날짜를 매개변수로 분리하여 단위 테스트에서
     * 실행 날짜를 고정할 수 있도록 합니다.</p>
     */
    void sendDailyRouteNotifications(
        LocalDate notificationDate
    ) {
        int page = 0;

        while (true) {
            DeliveryAgentPageResponseDto agentPage =
                deliveryServiceClient.getCompanyDeliveryAgents(
                    COMPANY_DELIVERY,
                    true,
                    page,
                    PAGE_SIZE
                );

            validateAgentPage(agentPage);

            List<DeliveryAgentSummary> agents =
                agentPage.content() == null
                    ? List.of()
                    : agentPage.content();

            for (DeliveryAgentSummary agent : agents) {
                processAgent(agent, notificationDate);
            }

            if (isLastPage(agentPage, page)) {
                break;
            }

            page++;
        }
    }

    /**
     * 배송 담당자 한 명의 당일 경로 알림을 처리합니다.
     *
     * <p>한 담당자의 처리에 실패하더라도 다른 담당자의
     * 알림 처리는 계속 진행합니다.</p>
     */
    private void processAgent(
        DeliveryAgentSummary agent,
        LocalDate notificationDate
    ) {
        if (
            agent == null
                || agent.agentId() == null
                || !StringUtils.hasText(agent.slackId())
        ) {
            log.warn(
                "Slack 알림에 필요한 담당자 정보가 없어 건너뜁니다. agent={}",
                agent
            );
            return;
        }

        try {
            UUID notificationId = createNotificationId(
                notificationDate,
                agent.agentId()
            );

            Optional<SlackMessageResponseDto> existingMessage =
                slackMessageService
                    .findSlackMessageByAiRequestAndRecipient(
                        notificationId,
                        agent.agentId()
                    );

            if (existingMessage.isPresent()) {
                handleExistingMessage(
                    existingMessage.get()
                );
                return;
            }

            TodayRouteResponseDto todayRoute =
                deliveryServiceClient.getTodayRoute(
                    agent.agentId()
                );

            validateTodayRoute(
                agent.agentId(),
                todayRoute
            );

            String prompt =
                dailyRoutePromptService.createPrompt(
                    todayRoute
                );

            DailyRouteAiClient.AiExecutionResult aiResult =
                dailyRouteAiClient
                    .generateDailyRouteMessage(prompt);

            SlackMessageRequestDto slackRequest =
                new SlackMessageRequestDto(
                    notificationId,
                    agent.agentId(),
                    agent.slackId(),
                    SlackMessageType.DAILY_ROUTE,
                    MESSAGE_TITLE + " " + notificationDate,
                    aiResult.result().message()
                );

            slackMessageService.createOrRetrySlackMessage(
                slackRequest
            );

            log.info(
                "당일 배송 경로 Slack 알림 처리가 완료되었습니다. "
                    + "agentId={}, notificationDate={}",
                agent.agentId(),
                notificationDate
            );

        } catch (Exception exception) {
            log.error(
                "당일 배송 경로 Slack 알림 처리에 실패했습니다. "
                    + "agentId={}, notificationDate={}",
                agent.agentId(),
                notificationDate,
                exception
            );
        }
    }

    /**
     * 이미 생성된 메시지의 상태에 따라
     * 중복 발송을 건너뛰거나 재발송합니다.
     */
    private void handleExistingMessage(
        SlackMessageResponseDto existingMessage
    ) {
        if (
            existingMessage.status()
                == SlackMessageStatus.SENT
        ) {
            log.info(
                "이미 발송된 당일 배송 경로 알림입니다. "
                    + "slackMessageId={}",
                existingMessage.slackMessageId()
            );
            return;
        }

        SlackMessageRequestDto retryRequest =
            new SlackMessageRequestDto(
                existingMessage.aiRequestId(),
                existingMessage.recipientUserId(),
                existingMessage.recipientSlackId(),
                existingMessage.messageType(),
                existingMessage.title(),
                existingMessage.content()
            );

        slackMessageService.createOrRetrySlackMessage(
            retryRequest
        );
    }

    /**
     * Delivery Service의 담당자 목록 응답을 검증합니다.
     */
    private void validateAgentPage(
        DeliveryAgentPageResponseDto agentPage
    ) {
        if (agentPage == null) {
            throw new IllegalStateException(
                "Delivery Service가 담당자 목록을 반환하지 않았습니다."
            );
        }
    }

    /**
     * 요청한 담당자와 조회된 경로의 담당자가 일치하는지 확인합니다.
     */
    private void validateTodayRoute(
        UUID requestedAgentId,
        TodayRouteResponseDto todayRoute
    ) {
        if (
            todayRoute == null
                || todayRoute.agentId() == null
                || !requestedAgentId.equals(
                todayRoute.agentId()
            )
        ) {
            throw new IllegalStateException(
                "Delivery Service의 당일 경로 응답이 올바르지 않습니다."
            );
        }
    }

    /**
     * 날짜와 담당자 ID를 이용해 매번 동일한 알림 ID를 만듭니다.
     *
     * <p>같은 날짜와 담당자로 다시 실행하면 같은 UUID가 생성되므로
     * 중복 Gemini 호출과 Slack 발송을 방지할 수 있습니다.</p>
     */
    private UUID createNotificationId(
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

    /**
     * 현재 응답이 마지막 페이지인지 확인합니다.
     */
    private boolean isLastPage(
        DeliveryAgentPageResponseDto agentPage,
        int currentPage
    ) {
        Integer totalPages = agentPage.totalPages();

        return totalPages == null
            || currentPage + 1 >= totalPages;
    }
}
