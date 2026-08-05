package com.logistics.ai.slackmessage.service;

import com.logistics.ai.global.exception.BusinessException;
import com.logistics.ai.global.exception.ErrorCode;
import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.methods.response.conversations.ConversationsOpenResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Slack Web API를 이용해 사용자에게 DM을 발송하는 클라이언트입니다.
 */
@Component
public class SlackWebApiClient implements SlackNotificationClient {

    private final MethodsClient methodsClient;

    public SlackWebApiClient(
        @Value("${slack.bot-token:}") String botToken
    ) {
        if (!StringUtils.hasText(botToken)) {
            throw new BusinessException(
                ErrorCode.SLACK_NOTIFICATION_FAILED,
                "Slack Bot Token이 설정되지 않았습니다."
            );
        }

        this.methodsClient = Slack.getInstance().methods(botToken);
    }

    /**
     * 사용자와의 DM 채널을 열고 메시지를 발송합니다.
     */
    @Override
    public SlackSendResult sendMessage(
        String recipientSlackId,
        String title,
        String content
    ) {
        try {
            // Slack 회원 ID를 이용해 DM 채널을 열거나 기존 채널을 조회합니다.
            String channelId = openDirectMessageChannel(
                recipientSlackId
            );

            ChatPostMessageResponse response =
                methodsClient.chatPostMessage(request ->
                    request
                        .channel(channelId)
                        .text("[" + title + "]\n" + content)
                        .mrkdwn(false)
                        .unfurlLinks(false)
                        .unfurlMedia(false)
                );

            if (!response.isOk()) {
                throw slackApiException(response.getError());
            }

            return new SlackSendResult(
                response.getTs(),
                channelId
            );

        } catch (BusinessException exception) {
            throw exception;

        } catch (IOException | SlackApiException exception) {
            throw new BusinessException(
                ErrorCode.SLACK_NOTIFICATION_FAILED,
                "Slack API 호출 중 오류가 발생했습니다.",
                exception
            );
        }
    }

    /**
     * Slack 회원과의 1:1 DM 채널을 생성하거나 기존 채널을 반환합니다.
     */
    private String openDirectMessageChannel(
        String recipientSlackId
    ) throws IOException, SlackApiException {
        ConversationsOpenResponse response =
            methodsClient.conversationsOpen(request ->
                request
                    .users(List.of(recipientSlackId))
                    .returnIm(true)
            );

        if (!response.isOk()) {
            throw slackApiException(response.getError());
        }

        if (response.getChannel() == null
            || !StringUtils.hasText(response.getChannel().getId())) {
            throw new BusinessException(
                ErrorCode.SLACK_NOTIFICATION_FAILED,
                "Slack DM 채널 정보를 확인할 수 없습니다."
            );
        }

        return response.getChannel().getId();
    }

    /**
     * Slack에서 반환한 오류 코드를 비즈니스 예외로 변환합니다.
     */
    private BusinessException slackApiException(String error) {
        return new BusinessException(
            ErrorCode.SLACK_NOTIFICATION_FAILED,
            "Slack API 오류: " + error
        );
    }
}
