package com.logistics.ai.kafka.service;

import com.logistics.ai.airequest.dto.requestdto.AiRequestDto;
import com.logistics.ai.airequest.dto.responsedto.AiResponseDto;
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

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.Mockito.*;

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
            mock(DeliveryAiNotificationEvent.class);

        AiRequestDto aiRequestDto =
            mock(AiRequestDto.class);

        AiResponseDto aiResponse =
            mock(AiResponseDto.class);

        SlackMessageRequestDto slackRequest =
            mock(SlackMessageRequestDto.class);

        when(eventMapper.toAiRequestDto(event))
            .thenReturn(aiRequestDto);

        when(
            aiRequestService.createOrRetryAiRequest(aiRequestDto)
        ).thenReturn(
            Optional.of(aiResponse)
        );

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
            .createOrRetryAiRequest(aiRequestDto);

        verify(eventMapper)
            .toSlackMessageRequestDto(
                event,
                aiResponse
            );

        verify(slackMessageService)
            .createOrRetrySlackMessage(slackRequest);
    }

    @Test
    @DisplayName("삭제된 AI 요청의 Kafka 이벤트는 다시 처리하지 않는다")
    void skipDeletedAiRequestEvent() {
        // given
        DeliveryAiNotificationEvent event =
            mock(DeliveryAiNotificationEvent.class);

        AiRequestDto aiRequestDto =
            mock(AiRequestDto.class);

        when(eventMapper.toAiRequestDto(event))
            .thenReturn(aiRequestDto);

        when(
            aiRequestService.createOrRetryAiRequest(aiRequestDto)
        ).thenReturn(Optional.empty());

        // when
        eventService.process(event);

        // then
        verify(aiRequestService)
            .createOrRetryAiRequest(aiRequestDto);

        verify(
            eventMapper,
            never()
        ).toSlackMessageRequestDto(
            eq(event),
            any(AiResponseDto.class)
        );

        verifyNoInteractions(slackMessageService);
    }

    @Test
    @DisplayName("Slack 발송 오류는 호출자에게 전달한다")
    void propagateSlackFailure() {
        // given
        DeliveryAiNotificationEvent event =
            mock(DeliveryAiNotificationEvent.class);

        AiRequestDto aiRequestDto =
            mock(AiRequestDto.class);

        AiResponseDto aiResponse =
            mock(AiResponseDto.class);

        SlackMessageRequestDto slackRequest =
            mock(SlackMessageRequestDto.class);

        BusinessException slackFailure =
            new BusinessException(
                ErrorCode.SLACK_NOTIFICATION_FAILED
            );

        when(eventMapper.toAiRequestDto(event))
            .thenReturn(aiRequestDto);

        when(
            aiRequestService.createOrRetryAiRequest(aiRequestDto)
        ).thenReturn(
            Optional.of(aiResponse)
        );

        when(
            eventMapper.toSlackMessageRequestDto(
                event,
                aiResponse
            )
        ).thenReturn(slackRequest);

        when(
            slackMessageService.createOrRetrySlackMessage(
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
