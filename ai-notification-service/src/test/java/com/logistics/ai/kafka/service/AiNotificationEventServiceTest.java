package com.logistics.ai.kafka.service;

import com.logistics.ai.airequest.dto.requestdto.AiRequestDto;
import com.logistics.ai.airequest.dto.responsedto.AiResponseDto;
import com.logistics.ai.airequest.entity.AiRequestStatus;
import com.logistics.ai.airequest.service.AiRequestService;
import com.logistics.ai.global.exception.BusinessException;
import com.logistics.ai.global.exception.ErrorCode;
import com.logistics.ai.kafka.event.DeliveryAiNotificationEvent;
import com.logistics.ai.kafka.mapper.AiNotificationEventMapper;
import com.logistics.ai.slackmessage.dto.requestdto.SlackMessageRequestDto;
import com.logistics.ai.slackmessage.service.SlackMessageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Kafka 이벤트 기반 AI 계산 및 Slack 발송 흐름을 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
class AiNotificationEventServiceTest {

    @Mock
    private AiNotificationEventMapper eventMapper;

    @Mock
    private AiRequestService aiRequestService;

    @Mock
    private SlackMessageService slackMessageService;

    @InjectMocks
    private AiNotificationEventService eventService;

    @Test
    @DisplayName("AI 계산 성공 시 Slack 메시지를 발송한다")
    void processSuccess() {
        // given
        DeliveryAiNotificationEvent event =
            org.mockito.Mockito.mock(
                DeliveryAiNotificationEvent.class
            );

        AiRequestDto aiRequestDto =
            org.mockito.Mockito.mock(AiRequestDto.class);

        AiResponseDto aiResponse =
            org.mockito.Mockito.mock(AiResponseDto.class);

        SlackMessageRequestDto slackRequest =
            org.mockito.Mockito.mock(
                SlackMessageRequestDto.class
            );

        when(eventMapper.toAiRequestDto(event))
            .thenReturn(aiRequestDto);

        when(aiRequestService.createAiRequest(aiRequestDto))
            .thenReturn(aiResponse);

        when(aiResponse.status())
            .thenReturn(AiRequestStatus.SUCCESS);

        when(
            eventMapper.toSlackMessageRequestDto(
                event,
                aiResponse
            )
        ).thenReturn(slackRequest);

        // when
        eventService.process(event);

        // then
        verify(eventMapper)
            .toAiRequestDto(event);

        verify(aiRequestService)
            .createAiRequest(aiRequestDto);

        verify(eventMapper)
            .toSlackMessageRequestDto(
                event,
                aiResponse
            );

        verify(slackMessageService)
            .createSlackMessage(slackRequest);
    }

    @Test
    @DisplayName("AI 계산 결과가 실패 상태이면 Slack 발송을 생략한다")
    void skipSlackWhenAiRequestFailed() {
        // given
        DeliveryAiNotificationEvent event =
            org.mockito.Mockito.mock(
                DeliveryAiNotificationEvent.class
            );

        AiRequestDto aiRequestDto =
            org.mockito.Mockito.mock(AiRequestDto.class);

        AiResponseDto aiResponse =
            org.mockito.Mockito.mock(AiResponseDto.class);

        when(eventMapper.toAiRequestDto(event))
            .thenReturn(aiRequestDto);

        when(aiRequestService.createAiRequest(aiRequestDto))
            .thenReturn(aiResponse);

        when(aiResponse.status())
            .thenReturn(AiRequestStatus.FAILED);

        // when
        eventService.process(event);

        // then
        verify(
            eventMapper,
            never()
        ).toSlackMessageRequestDto(
            event,
            aiResponse
        );

        verify(
            slackMessageService,
            never()
        ).createSlackMessage(
            org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    @DisplayName("이미 Slack 메시지가 존재하면 중복 이벤트로 판단하고 정상 종료한다")
    void ignoreExistingSlackMessage() {
        // given
        DeliveryAiNotificationEvent event =
            org.mockito.Mockito.mock(
                DeliveryAiNotificationEvent.class
            );

        AiRequestDto aiRequestDto =
            org.mockito.Mockito.mock(AiRequestDto.class);

        AiResponseDto aiResponse =
            org.mockito.Mockito.mock(AiResponseDto.class);

        SlackMessageRequestDto slackRequest =
            org.mockito.Mockito.mock(
                SlackMessageRequestDto.class
            );

        BusinessException duplicateException =
            new BusinessException(
                ErrorCode.SLACK_MESSAGE_ALREADY_EXISTS
            );

        when(eventMapper.toAiRequestDto(event))
            .thenReturn(aiRequestDto);

        when(aiRequestService.createAiRequest(aiRequestDto))
            .thenReturn(aiResponse);

        when(aiResponse.status())
            .thenReturn(AiRequestStatus.SUCCESS);

        when(
            eventMapper.toSlackMessageRequestDto(
                event,
                aiResponse
            )
        ).thenReturn(slackRequest);

        when(
            slackMessageService.createSlackMessage(
                slackRequest
            )
        ).thenThrow(duplicateException);

        // when
        Throwable throwable =
            org.assertj.core.api.Assertions
                .catchThrowable(
                    () -> eventService.process(event)
                );

        // then
        assertThat(throwable).isNull();

        verify(slackMessageService)
            .createSlackMessage(slackRequest);
    }

    @Test
    @DisplayName("Slack 중복 이외의 발송 오류는 호출자에게 전달한다")
    void propagateSlackFailure() {
        // given
        DeliveryAiNotificationEvent event =
            org.mockito.Mockito.mock(
                DeliveryAiNotificationEvent.class
            );

        AiRequestDto aiRequestDto =
            org.mockito.Mockito.mock(AiRequestDto.class);

        AiResponseDto aiResponse =
            org.mockito.Mockito.mock(AiResponseDto.class);

        SlackMessageRequestDto slackRequest =
            org.mockito.Mockito.mock(
                SlackMessageRequestDto.class
            );

        BusinessException slackFailure =
            new BusinessException(
                ErrorCode.SLACK_NOTIFICATION_FAILED
            );

        when(eventMapper.toAiRequestDto(event))
            .thenReturn(aiRequestDto);

        when(aiRequestService.createAiRequest(aiRequestDto))
            .thenReturn(aiResponse);

        when(aiResponse.status())
            .thenReturn(AiRequestStatus.SUCCESS);

        when(
            eventMapper.toSlackMessageRequestDto(
                event,
                aiResponse
            )
        ).thenReturn(slackRequest);

        when(
            slackMessageService.createSlackMessage(
                slackRequest
            )
        ).thenThrow(slackFailure);

        // when & then
        assertThatExceptionOfType(BusinessException.class)
            .isThrownBy(
                () -> eventService.process(event)
            )
            .isSameAs(slackFailure);
    }
}
