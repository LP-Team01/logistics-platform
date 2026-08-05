package com.logistics.ai.slackmessage.service;

import com.logistics.ai.global.exception.BusinessException;
import com.logistics.ai.global.exception.ErrorCode;
import com.logistics.ai.slackmessage.dto.requestdto.SlackMessageRequestDto;
import com.logistics.ai.slackmessage.dto.requestdto.SlackMessageUpdateRequestDto;
import com.logistics.ai.slackmessage.dto.requestdto.SlackTestMessageRequestDto;
import com.logistics.ai.slackmessage.dto.responsedto.SlackMessageResponseDto;
import com.logistics.ai.slackmessage.dto.responsedto.SlackTestMessageResponseDto;
import com.logistics.ai.slackmessage.entity.SlackMessage;
import com.logistics.ai.slackmessage.entity.SlackMessageStatus;
import com.logistics.ai.slackmessage.entity.SlackMessageType;
import com.logistics.ai.slackmessage.repository.SlackMessageRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SlackMessageServiceTest {

    @Mock
    private SlackMessageRepository slackMessageRepository;

    @Mock
    private SlackNotificationClient slackNotificationClient;

    @InjectMocks
    private SlackMessageService slackMessageService;

    @Test
    @DisplayName("Slack 메시지를 생성하고 발송 성공 상태를 저장한다")
    void createSlackMessageSuccess() {
        // given
        SlackMessageRequestDto requestDto =
            createRequestDto();

        SlackNotificationClient.SlackSendResult sendResult =
            mock(SlackNotificationClient.SlackSendResult.class);

        when(
            slackMessageRepository
                .existsByAiRequestIdAndRecipientUserIdAndDeletedAtIsNull(
                    requestDto.aiRequestId(),
                    requestDto.recipientUserId()
                )
        ).thenReturn(false);

        when(
            slackMessageRepository.save(any(SlackMessage.class))
        ).thenAnswer(invocation -> invocation.getArgument(0));

        when(
            slackNotificationClient.sendMessage(
                requestDto.recipientSlackId(),
                requestDto.title(),
                requestDto.content()
            )
        ).thenReturn(sendResult);

        when(sendResult.slackTimestamp())
            .thenReturn("1722844800.123456");

        // when
        SlackMessageResponseDto response =
            slackMessageService.createSlackMessage(requestDto);

        // then
        assertThat(response).isNotNull();

        ArgumentCaptor<SlackMessage> captor =
            ArgumentCaptor.forClass(SlackMessage.class);

        verify(slackMessageRepository, times(2))
            .save(captor.capture());

        SlackMessage completedMessage =
            captor.getAllValues().get(1);

        assertThat(completedMessage.getStatus())
            .isEqualTo(SlackMessageStatus.SENT);

        assertThat(completedMessage.getSlackTimestamp())
            .isEqualTo("1722844800.123456");

        assertThat(completedMessage.getSentAt()).isNotNull();
        assertThat(completedMessage.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("동일한 AI 요청과 수신자의 메시지는 중복 생성할 수 없다")
    void createDuplicateSlackMessage() {
        // given
        UUID aiRequestId = UUID.randomUUID();
        UUID recipientUserId = UUID.randomUUID();

        SlackMessageRequestDto requestDto =
            mock(SlackMessageRequestDto.class);

        when(requestDto.aiRequestId())
            .thenReturn(aiRequestId);

        when(requestDto.recipientUserId())
            .thenReturn(recipientUserId);

        when(
            slackMessageRepository
                .existsByAiRequestIdAndRecipientUserIdAndDeletedAtIsNull(
                    aiRequestId,
                    recipientUserId
                )
        ).thenReturn(true);

        // when
        BusinessException exception =
            catchThrowableOfType(
                () -> slackMessageService
                    .createSlackMessage(requestDto),
                BusinessException.class
            );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(
                ErrorCode.SLACK_MESSAGE_ALREADY_EXISTS
            );

        verify(
            slackMessageRepository,
            never()
        ).save(any(SlackMessage.class));

        verifyNoInteractions(slackNotificationClient);
    }

    @Test
    @DisplayName("Slack 발송 실패 시 FAILED 상태를 저장한다")
    void createSlackMessageFailed() {
        // given
        SlackMessageRequestDto requestDto =
            createRequestDto();

        BusinessException failure =
            new BusinessException(
                ErrorCode.SLACK_NOTIFICATION_FAILED
            );

        when(
            slackMessageRepository
                .existsByAiRequestIdAndRecipientUserIdAndDeletedAtIsNull(
                    requestDto.aiRequestId(),
                    requestDto.recipientUserId()
                )
        ).thenReturn(false);

        when(
            slackMessageRepository.save(any(SlackMessage.class))
        ).thenAnswer(invocation -> invocation.getArgument(0));

        when(
            slackNotificationClient.sendMessage(
                requestDto.recipientSlackId(),
                requestDto.title(),
                requestDto.content()
            )
        ).thenThrow(failure);

        // when
        BusinessException exception =
            catchThrowableOfType(
                () -> slackMessageService
                    .createSlackMessage(requestDto),
                BusinessException.class
            );

        // then
        assertThat(exception).isSameAs(failure);

        ArgumentCaptor<SlackMessage> captor =
            ArgumentCaptor.forClass(SlackMessage.class);

        verify(slackMessageRepository, times(2))
            .save(captor.capture());

        SlackMessage failedMessage =
            captor.getAllValues().get(1);

        assertThat(failedMessage.getStatus())
            .isEqualTo(SlackMessageStatus.FAILED);

        assertThat(failedMessage.getErrorMessage())
            .isEqualTo(failure.getMessage());
    }

    @Test
    @DisplayName("실패한 Slack 메시지를 성공적으로 재발송한다")
    void retrySlackMessageSuccess() {
        // given
        UUID slackMessageId = UUID.randomUUID();
        SlackMessage slackMessage = createTestSlackMessage();

        slackMessage.markFailed("Slack API 호출 실패");

        SlackNotificationClient.SlackSendResult sendResult =
            mock(SlackNotificationClient.SlackSendResult.class);

        when(
            slackMessageRepository
                .findBySlackMessageIdAndDeletedAtIsNull(
                    slackMessageId
                )
        ).thenReturn(Optional.of(slackMessage));

        when(
            slackMessageRepository.save(any(SlackMessage.class))
        ).thenAnswer(invocation -> invocation.getArgument(0));

        when(
            slackNotificationClient.sendMessage(
                slackMessage.getRecipientSlackId(),
                slackMessage.getTitle(),
                slackMessage.getContent()
            )
        ).thenReturn(sendResult);

        when(sendResult.slackTimestamp())
            .thenReturn("1722844800.654321");

        // when
        SlackMessageResponseDto response =
            slackMessageService.retrySlackMessage(
                slackMessageId
            );

        // then
        assertThat(response).isNotNull();

        assertThat(slackMessage.getStatus())
            .isEqualTo(SlackMessageStatus.SENT);

        assertThat(slackMessage.getRetryCount())
            .isEqualTo(1);

        assertThat(slackMessage.getSlackTimestamp())
            .isEqualTo("1722844800.654321");

        verify(slackMessageRepository, times(2))
            .save(slackMessage);
    }

    @Test
    @DisplayName("FAILED 상태가 아닌 Slack 메시지는 재발송할 수 없다")
    void retrySlackMessageNotAllowed() {
        // given
        UUID slackMessageId = UUID.randomUUID();
        SlackMessage slackMessage = createTestSlackMessage();

        when(
            slackMessageRepository
                .findBySlackMessageIdAndDeletedAtIsNull(
                    slackMessageId
                )
        ).thenReturn(Optional.of(slackMessage));

        // when
        BusinessException exception =
            catchThrowableOfType(
                () -> slackMessageService
                    .retrySlackMessage(slackMessageId),
                BusinessException.class
            );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(
                ErrorCode.SLACK_MESSAGE_RETRY_NOT_ALLOWED
            );

        verifyNoInteractions(slackNotificationClient);

        verify(
            slackMessageRepository,
            never()
        ).save(any(SlackMessage.class));
    }

    @Test
    @DisplayName("발송이 완료된 Slack 메시지는 수정할 수 없다")
    void updateSentSlackMessageNotAllowed() {
        // given
        UUID slackMessageId = UUID.randomUUID();
        SlackMessage slackMessage = createTestSlackMessage();

        slackMessage.markSent("1722844800.123456");

        SlackMessageUpdateRequestDto requestDto =
            mock(SlackMessageUpdateRequestDto.class);

        when(
            slackMessageRepository
                .findBySlackMessageIdAndDeletedAtIsNull(
                    slackMessageId
                )
        ).thenReturn(Optional.of(slackMessage));

        // when
        BusinessException exception =
            catchThrowableOfType(
                () -> slackMessageService.updateSlackMessage(
                    slackMessageId,
                    requestDto
                ),
                BusinessException.class
            );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(
                ErrorCode.SLACK_MESSAGE_UPDATE_NOT_ALLOWED
            );
    }

    @Test
    @DisplayName("Slack 메시지를 논리 삭제할 수 있다")
    void deleteSlackMessage() {
        // given
        UUID slackMessageId = UUID.randomUUID();
        UUID deletedBy = UUID.randomUUID();
        SlackMessage slackMessage = createTestSlackMessage();

        when(
            slackMessageRepository
                .findBySlackMessageIdAndDeletedAtIsNull(
                    slackMessageId
                )
        ).thenReturn(Optional.of(slackMessage));

        // when
        slackMessageService.deleteSlackMessage(
            slackMessageId,
            deletedBy
        );

        // then
        assertThat(slackMessage.isDeleted()).isTrue();
        assertThat(slackMessage.getDeletedAt()).isNotNull();

        assertThat(slackMessage.getDeletedBy())
            .isEqualTo(deletedBy);
    }

    @Test
    @DisplayName("테스트 메시지는 DB에 저장하지 않고 Slack으로 발송한다")
    void sendTestMessage() {
        // given
        SlackTestMessageRequestDto requestDto =
            mock(SlackTestMessageRequestDto.class);

        SlackNotificationClient.SlackSendResult sendResult =
            mock(SlackNotificationClient.SlackSendResult.class);

        when(requestDto.recipientSlackId())
            .thenReturn("U0123456789");

        when(requestDto.content())
            .thenReturn("Slack 연동 테스트입니다.");

        when(
            slackNotificationClient.sendMessage(
                "U0123456789",
                "[TEST] Slack 연동 테스트",
                "Slack 연동 테스트입니다."
            )
        ).thenReturn(sendResult);

        when(sendResult.channelId())
            .thenReturn("D0123456789");

        when(sendResult.slackTimestamp())
            .thenReturn("1722844800.123456");

        // when
        SlackTestMessageResponseDto response =
            slackMessageService.sendTestMessage(requestDto);

        // then
        assertThat(response).isNotNull();

        verifyNoInteractions(slackMessageRepository);

        verify(slackNotificationClient).sendMessage(
            "U0123456789",
            "[TEST] Slack 연동 테스트",
            "Slack 연동 테스트입니다."
        );
    }

    /**
     * 테스트에 사용할 Slack 메시지 요청 DTO를 생성합니다.
     */
    private SlackMessageRequestDto createRequestDto() {
        SlackMessageRequestDto requestDto =
            mock(SlackMessageRequestDto.class);

        when(requestDto.aiRequestId())
            .thenReturn(UUID.randomUUID());

        when(requestDto.recipientUserId())
            .thenReturn(UUID.randomUUID());

        when(requestDto.recipientSlackId())
            .thenReturn("U0123456789");

        when(requestDto.messageType())
            .thenReturn(
                SlackMessageType.DISPATCH_DEADLINE
            );

        when(requestDto.title())
            .thenReturn("최종 발송 시한 안내");

        when(requestDto.content())
            .thenReturn(
                "최종 발송 시한은 8월 5일 오전 9시입니다."
            );

        return requestDto;
    }

    /**
     * 테스트에 사용할 Slack 메시지 엔티티를 생성합니다.
     */
    private SlackMessage createTestSlackMessage() {
        return SlackMessage.create(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "U0123456789",
            SlackMessageType.DISPATCH_DEADLINE,
            "최종 발송 시한 안내",
            "최종 발송 시한은 8월 5일 오전 9시입니다."
        );
    }
}
