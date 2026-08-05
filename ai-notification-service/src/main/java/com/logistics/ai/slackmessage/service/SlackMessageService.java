package com.logistics.ai.slackmessage.service;

import com.logistics.ai.global.exception.BusinessException;
import com.logistics.ai.global.exception.ErrorCode;
import com.logistics.ai.slackmessage.dto.requestdto.*;
import com.logistics.ai.slackmessage.dto.responsedto.SlackBulkRetryResponseDto;
import com.logistics.ai.slackmessage.dto.responsedto.SlackMessageResponseDto;
import com.logistics.ai.slackmessage.dto.responsedto.SlackTestMessageResponseDto;
import com.logistics.ai.slackmessage.entity.SlackMessage;
import com.logistics.ai.slackmessage.entity.SlackMessageStatus;
import com.logistics.ai.slackmessage.repository.SlackMessageRepository;
import com.logistics.ai.slackmessage.repository.SlackMessageSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Slack 메시지 발송과 발송 이력 저장을 처리하는 서비스입니다.
 */
@Service
@RequiredArgsConstructor
public class SlackMessageService {

    private static final String TEST_MESSAGE_TITLE =
        "[TEST] Slack 연동 테스트";

    private final SlackMessageRepository slackMessageRepository;
    private final SlackNotificationClient slackNotificationClient;

    /**
     * Slack 메시지 이력을 생성한 후 실제 Slack 메시지를 발송합니다.
     *
     * @param requestDto Slack 메시지 발송 요청
     * @return Slack 메시지 처리 결과
     */
    public SlackMessageResponseDto createSlackMessage(
        SlackMessageRequestDto requestDto
    ) {
        validateDuplicateMessage(requestDto);

        // Slack 발송 전 PENDING 상태의 이력을 먼저 저장합니다.
        SlackMessage slackMessage = SlackMessage.create(
            requestDto.aiRequestId(),
            requestDto.recipientUserId(),
            requestDto.recipientSlackId(),
            requestDto.messageType(),
            requestDto.title(),
            requestDto.content()
        );

        SlackMessage savedSlackMessage =
            slackMessageRepository.save(slackMessage);

        try {
            SlackNotificationClient.SlackSendResult result =
                slackNotificationClient.sendMessage(
                    requestDto.recipientSlackId(),
                    requestDto.title(),
                    requestDto.content()
                );

            savedSlackMessage.markSent(result.slackTimestamp());

            SlackMessage completedMessage =
                slackMessageRepository.save(savedSlackMessage);

            return SlackMessageResponseDto.from(completedMessage);

        } catch (BusinessException exception) {
            // Slack 발송 실패 이력도 DB에 남깁니다.
            savedSlackMessage.markFailed(exception.getMessage());
            slackMessageRepository.save(savedSlackMessage);

            throw exception;
        }
    }

    /**
     * Slack 메시지 발송 이력을 단건 조회합니다.
     *
     * @param slackMessageId Slack 메시지 식별자
     * @return Slack 메시지 발송 이력
     */
    @Transactional(readOnly = true)
    public SlackMessageResponseDto getSlackMessage(
        UUID slackMessageId
    ) {
        SlackMessage slackMessage = slackMessageRepository
            .findBySlackMessageIdAndDeletedAtIsNull(slackMessageId)
            .orElseThrow(
                () -> new BusinessException(
                    ErrorCode.SLACK_MESSAGE_NOT_FOUND
                )
            );

        return SlackMessageResponseDto.from(slackMessage);
    }

    /**
     * Slack 메시지 목록을 검색하고 페이징하여 조회합니다.
     *
     * @param condition Slack 메시지 검색 조건
     * @param pageable 페이지 번호, 크기 및 정렬 조건
     * @return 페이징된 Slack 메시지 목록
     */
    @Transactional(readOnly = true)
    public Page<SlackMessageResponseDto> getSlackMessages(
        SlackMessageSearchCondition condition,
        Pageable pageable
    ) {
        return slackMessageRepository.findAll(
            SlackMessageSpecification.withCondition(condition),
            pageable
        ).map(SlackMessageResponseDto::from);
    }

    /**
     * 실패한 Slack 메시지를 다시 발송합니다.
     *
     * @param slackMessageId 재발송할 Slack 메시지 식별자
     * @return 재발송 처리 결과
     */
    public SlackMessageResponseDto retrySlackMessage(
        UUID slackMessageId
    ) {
        SlackMessage slackMessage = slackMessageRepository
            .findBySlackMessageIdAndDeletedAtIsNull(slackMessageId)
            .orElseThrow(
                () -> new BusinessException(
                    ErrorCode.SLACK_MESSAGE_NOT_FOUND
                )
            );

        // 이미 성공했거나 아직 처리 중인 메시지는 재발송할 수 없습니다.
        if (slackMessage.getStatus() != SlackMessageStatus.FAILED) {
            throw new BusinessException(
                ErrorCode.SLACK_MESSAGE_RETRY_NOT_ALLOWED
            );
        }

        // 실패 정보를 초기화하고 PENDING 상태로 변경합니다.
        slackMessage.prepareRetry();
        SlackMessage pendingMessage =
            slackMessageRepository.save(slackMessage);

        try {
            SlackNotificationClient.SlackSendResult sendResult =
                slackNotificationClient.sendMessage(
                    pendingMessage.getRecipientSlackId(),
                    pendingMessage.getTitle(),
                    pendingMessage.getContent()
                );

            pendingMessage.markSent(
                sendResult.slackTimestamp()
            );

            SlackMessage sentMessage =
                slackMessageRepository.save(pendingMessage);

            return SlackMessageResponseDto.from(sentMessage);

        } catch (BusinessException exception) {
            // 외부 Slack 호출이 실패해도 실패 이력은 DB에 남깁니다.
            pendingMessage.markFailed(exception.getMessage());
            slackMessageRepository.save(pendingMessage);

            throw exception;
        }
    }

    /**
     * 발송 전 또는 발송에 실패한 Slack 메시지를 수정합니다.
     *
     * <p>PATCH 요청이므로 요청 DTO에서 값이 전달된 필드만 수정합니다.
     * 이미 발송이 완료된 메시지는 수정할 수 없습니다.</p>
     *
     * @param slackMessageId 수정할 Slack 메시지 식별자
     * @param requestDto Slack 메시지 수정 요청
     * @return 수정된 Slack 메시지
     */
    @Transactional
    public SlackMessageResponseDto updateSlackMessage(
        UUID slackMessageId,
        SlackMessageUpdateRequestDto requestDto
    ) {
        SlackMessage slackMessage = slackMessageRepository
            .findBySlackMessageIdAndDeletedAtIsNull(slackMessageId)
            .orElseThrow(
                () -> new BusinessException(
                    ErrorCode.SLACK_MESSAGE_NOT_FOUND
                )
            );

        // 이미 Slack 발송이 완료된 메시지는 과거 이력이므로 수정하지 않습니다.
        if (slackMessage.getStatus() == SlackMessageStatus.SENT) {
            throw new BusinessException(
                ErrorCode.SLACK_MESSAGE_UPDATE_NOT_ALLOWED
            );
        }

        // 엔티티 내부에서 null이 아닌 필드만 변경합니다.
        slackMessage.update(
            requestDto.recipientUserId(),
            requestDto.recipientSlackId(),
            requestDto.messageType(),
            requestDto.title(),
            requestDto.content()
        );

        /*
         * @Transactional 범위 안에서 조회한 엔티티이므로
         * JPA 변경 감지(Dirty Checking)를 통해 UPDATE 쿼리가 실행됩니다.
         */
        return SlackMessageResponseDto.from(slackMessage);
    }

    /**
     * Slack 메시지 발송 이력을 논리 삭제합니다.
     *
     * <p>DB에서 실제 행을 삭제하지 않고 deletedAt과 deletedBy를
     * 기록하여 일반 조회 결과에서 제외합니다.</p>
     *
     * @param slackMessageId 삭제할 Slack 메시지 식별자
     * @param deletedBy 삭제를 수행한 사용자 식별자
     */
    @Transactional
    public void deleteSlackMessage(
        UUID slackMessageId,
        UUID deletedBy
    ) {
        SlackMessage slackMessage = slackMessageRepository
            .findBySlackMessageIdAndDeletedAtIsNull(slackMessageId)
            .orElseThrow(
                () -> new BusinessException(
                    ErrorCode.SLACK_MESSAGE_NOT_FOUND
                )
            );

        slackMessage.softDelete(deletedBy);

        /*
         * @Transactional 범위에서 조회한 엔티티이므로
         * 별도의 save() 없이 변경 감지로 UPDATE 쿼리가 실행됩니다.
         */
    }

    /**
     * Slack 연동 상태를 확인하기 위한 테스트 메시지를 발송합니다.
     *
     * <p>테스트 메시지는 실제 Slack API를 호출하지만
     * p_slack_message 테이블에는 발송 이력을 저장하지 않습니다.</p>
     *
     * @param requestDto Slack 테스트 메시지 발송 요청
     * @return Slack 테스트 메시지 발송 결과
     */
    public SlackTestMessageResponseDto sendTestMessage(
        SlackTestMessageRequestDto requestDto
    ) {
        SlackNotificationClient.SlackSendResult sendResult =
            slackNotificationClient.sendMessage(
                requestDto.recipientSlackId(),
                TEST_MESSAGE_TITLE,
                requestDto.content()
            );

        return new SlackTestMessageResponseDto(
            true,
            requestDto.recipientSlackId(),
            sendResult.channelId(),
            sendResult.slackTimestamp(),
            LocalDateTime.now()
        );
    }

    /**
     * 실패한 Slack 메시지를 여러 건 일괄 재발송합니다.
     *
     * <p>한 메시지의 재발송이 실패하더라도 나머지 메시지는 계속 처리합니다.
     * 동일한 메시지 ID가 여러 번 전달되면 한 번만 처리합니다.</p>
     *
     * @param requestDto 일괄 재발송 요청
     * @return 메시지별 재발송 처리 결과
     */
    public SlackBulkRetryResponseDto retrySlackMessages(
        SlackBulkRetryRequestDto requestDto
    ) {
        // 입력 순서를 유지하면서 중복된 메시지 ID를 제거합니다.
        Set<UUID> uniqueMessageIds =
            new LinkedHashSet<>(requestDto.slackMessageIds());

        List<SlackBulkRetryResponseDto.RetryResult> results =
            new ArrayList<>();

        for (UUID slackMessageId : uniqueMessageIds) {
            try {
                // 기존 단건 재발송 기능을 재사용합니다.
                retrySlackMessage(slackMessageId);

                results.add(
                    new SlackBulkRetryResponseDto.RetryResult(
                        slackMessageId,
                        true,
                        SlackMessageStatus.SENT,
                        null,
                        null
                    )
                );

            } catch (BusinessException exception) {
                // 재발송 실패 후 현재 DB 상태를 확인합니다.
                SlackMessageStatus currentStatus =
                    slackMessageRepository
                        .findBySlackMessageIdAndDeletedAtIsNull(
                            slackMessageId
                        )
                        .map(SlackMessage::getStatus)
                        .orElse(null);

                results.add(
                    new SlackBulkRetryResponseDto.RetryResult(
                        slackMessageId,
                        false,
                        currentStatus,
                        exception.getErrorCode().getCode(),
                        exception.getMessage()
                    )
                );
            }
        }

        int successCount = (int) results.stream()
            .filter(SlackBulkRetryResponseDto.RetryResult::success)
            .count();

        int requestedCount = results.size();
        int failureCount = requestedCount - successCount;

        return new SlackBulkRetryResponseDto(
            requestedCount,
            successCount,
            failureCount,
            List.copyOf(results)
        );
    }

    /**
     * 동일 AI 요청과 수신자에 대한 중복 메시지 생성을 방지합니다.
     */
    private void validateDuplicateMessage(
        SlackMessageRequestDto requestDto
    ) {
        boolean alreadyExists =
            slackMessageRepository
                .existsByAiRequestIdAndRecipientUserIdAndDeletedAtIsNull(
                    requestDto.aiRequestId(),
                    requestDto.recipientUserId()
                );

        if (alreadyExists) {
            throw new BusinessException(
                ErrorCode.SLACK_MESSAGE_ALREADY_EXISTS
            );
        }
    }
}
