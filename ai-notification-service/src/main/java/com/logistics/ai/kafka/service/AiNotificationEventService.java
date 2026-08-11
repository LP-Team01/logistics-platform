package com.logistics.ai.kafka.service;

import com.logistics.ai.airequest.dto.responsedto.AiResponseDto;
import com.logistics.ai.airequest.service.AiRequestService;
import com.logistics.ai.global.exception.BusinessException;
import com.logistics.ai.global.exception.ErrorCode;
import com.logistics.ai.kafka.event.DeliveryAiNotificationEvent;
import com.logistics.ai.kafka.mapper.AiNotificationEventMapper;
import com.logistics.ai.slackmessage.service.SlackMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Kafka 배송 이벤트를 받아 AI 계산과 Slack 발송을 연결합니다.
 *
 * <p>Consumer는 메시지 수신만 담당하고,
 * 실제 비즈니스 처리 순서는 이 서비스에서 관리합니다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiNotificationEventService {

    private final AiNotificationEventMapper eventMapper;
    private final AiRequestService aiRequestService;
    private final SlackMessageService slackMessageService;

    /**
     * 배송 이벤트를 이용하여 AI 최종 발송 시한을 계산하고
     * 계산 결과를 Slack으로 발송합니다.
     *
     * @param event Kafka로 전달받은 배송 AI 알림 이벤트
     */
    public void process(
        DeliveryAiNotificationEvent event
    ) {
        log.info(
            "AI 알림 이벤트 처리를 시작합니다. "
                + "eventId={}, orderId={}, deliveryId={}",
            event.eventId(),
            event.orderId(),
            event.deliveryId()
        );

        Optional<AiResponseDto> aiResponseOptional =
            aiRequestService.createOrRetryAiRequest(
                eventMapper.toAiRequestDto(event)
            );

        /*
         * 논리 삭제된 AI 요청도 이미 소비된 Kafka 이벤트입니다.
         * 같은 eventId로 Gemini와 Slack을 다시 실행하지 않습니다.
         */
        if (aiResponseOptional.isEmpty()) {
            log.info(
                "이미 처리 후 삭제된 Kafka 이벤트이므로 "
                    + "재처리를 생략합니다. eventId={}",
                event.eventId()
            );

            return;
        }

        AiResponseDto aiResponse = aiResponseOptional.get();

        try {
            slackMessageService.createOrRetrySlackMessage(
                eventMapper.toSlackMessageRequestDto(
                    event,
                    aiResponse
                )
            );

        } catch (BusinessException exception) {
            /*
             * 같은 Kafka 이벤트가 중복 전달되었고
             * 해당 AI 요청의 Slack 메시지가 이미 존재한다면
             * 정상적으로 처리된 중복 이벤트로 판단합니다.
             */
            if (
                exception.getErrorCode()
                    == ErrorCode.SLACK_MESSAGE_ALREADY_EXISTS
            ) {
                log.info(
                    "이미 Slack 메시지가 생성된 이벤트입니다. "
                        + "중복 처리를 생략합니다. "
                        + "eventId={}, aiRequestId={}",
                    event.eventId(),
                    aiResponse.aiRequestId()
                );

                return;
            }

            throw exception;
        }

        log.info(
            "AI 알림 이벤트 처리가 완료되었습니다. "
                + "eventId={}, aiRequestId={}, "
                + "dispatchDeadline={}",
            event.eventId(),
            aiResponse.aiRequestId(),
            aiResponse.dispatchDeadline()
        );
    }
}
