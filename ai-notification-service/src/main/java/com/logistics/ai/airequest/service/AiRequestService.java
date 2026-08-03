package com.logistics.ai.airequest.service;

import com.logistics.ai.airequest.dto.requestdto.AiRequestDto;
import com.logistics.ai.airequest.dto.requestdto.AiSearchCondition;
import com.logistics.ai.airequest.dto.responsedto.AiResponseDto;
import com.logistics.ai.airequest.dto.responsedto.AiStatisticsResponseDto;
import com.logistics.ai.airequest.entity.AiRequest;
import com.logistics.ai.airequest.entity.AiRequestStatus;
import com.logistics.ai.airequest.repository.AiRequestRepository;
import com.logistics.ai.airequest.repository.AiRequestSpecification;
import com.logistics.ai.airequest.repository.AiRequestStatisticsProjection;
import com.logistics.ai.common.exception.AiRequestNotFoundException;
import com.logistics.ai.common.exception.AiRequestRetryNotAllowedException;
import com.logistics.ai.common.exception.GeminiProcessingException;
import com.logistics.ai.common.exception.InvalidStatisticsPeriodException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * AI 요청의 생성, 조회, 재처리 및 삭제를 담당하는 서비스입니다.
 *
 * <p>프롬프트 생성, Gemini 호출, AI 요청 상태 변경을 하나의 흐름으로 관리합니다.</p>
 */
@Service
@RequiredArgsConstructor
public class AiRequestService {

    private final AiRequestRepository aiRequestRepository;
    private final AiPromptService aiPromptService;
    private final DispatchDeadlineAiClient dispatchDeadlineAiClient;

    private static final LocalDateTime MIN_STATISTICS_DATE_TIME =
        LocalDateTime.of(1, 1, 1, 0, 0);

    private static final LocalDateTime MAX_STATISTICS_DATE_TIME =
        LocalDateTime.of(9999, 12, 31, 0, 0);

    /**
     * 배송 정보를 이용하여 최종 발송 시한을 계산합니다.
     *
     * <ol>
     *     <li>이벤트 중복 여부를 확인합니다.</li>
     *     <li>Gemini에 전달할 프롬프트를 생성합니다.</li>
     *     <li>AI 요청을 PENDING 상태로 먼저 저장합니다.</li>
     *     <li>Gemini 호출 결과에 따라 SUCCESS 또는 FAILED 상태로 변경합니다.</li>
     * </ol>
     *
     * @param requestDto AI 계산에 필요한 주문 및 배송 정보
     * @return AI 요청 처리 결과
     */
    public AiResponseDto createAiRequest(AiRequestDto requestDto) {
        AiResponseDto existingResponse = findExistingResponse(
            requestDto.eventId()
        );

        if (existingResponse != null) {
            return existingResponse;
        }

        String prompt = aiPromptService.createPrompt(requestDto);

        AiRequest aiRequest = AiRequest.create(
            requestDto.eventId(),
            requestDto.orderId(),
            requestDto.deliveryId(),
            requestDto.requestText(),
            requestDto.requestedArrivalAt(),
            requestDto.estimatedDurationMinutes(),
            requestDto.preparationBufferMinutes(),
            prompt
        );

        // 외부 API 호출 전에 PENDING 상태를 먼저 저장합니다.
        AiRequest savedAiRequest = aiRequestRepository.save(aiRequest);
        long startedAt = System.nanoTime();

        try {
            DispatchDeadlineAiClient.AiExecutionResult executionResult =
                dispatchDeadlineAiClient.calculateDispatchDeadline(prompt);

            savedAiRequest.markSuccess(
                executionResult.rawResponse(),
                executionResult.calculationResult().dispatchDeadline(),
                executionResult.model(),
                executionResult.processingTimeMs()
            );

            AiRequest completedAiRequest =
                aiRequestRepository.save(savedAiRequest);

            return AiResponseDto.from(completedAiRequest);

        } catch (GeminiProcessingException exception) {
            long processingTimeMs = calculateProcessingTime(startedAt);

            savedAiRequest.markFailed(
                exception.getMessage(),
                dispatchDeadlineAiClient.getModel(),
                processingTimeMs
            );

            // 예외를 다시 던지기 전에 FAILED 상태를 먼저 저장합니다.
            aiRequestRepository.save(savedAiRequest);

            throw exception;
        }
    }

    /**
     * 동일한 이벤트가 이미 처리되었다면 기존 결과를 반환합니다.
     *
     * <p>Kafka 메시지가 중복 전달되더라도 Gemini API를 다시 호출하지 않도록
     * 멱등성을 보장합니다.</p>
     */
    private AiResponseDto findExistingResponse(UUID eventId) {
        if (!aiRequestRepository.existsByEventId(eventId)) {
            return null;
        }

        return aiRequestRepository
            .findByEventIdAndDeletedAtIsNull(eventId)
            .map(AiResponseDto::from)
            .orElseThrow(() -> new IllegalStateException(
                "이미 삭제된 AI 요청 이벤트입니다. eventId=" + eventId
            ));
    }

    /**
     * AI 처리 시간을 밀리초 단위로 계산합니다.
     */
    private long calculateProcessingTime(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }

    /**
     * AI 요청 식별자로 삭제되지 않은 요청 이력을 조회합니다.
     *
     * @param aiRequestId AI 요청 식별자
     * @return AI 요청 처리 결과
     * @throws AiRequestNotFoundException 요청 이력이 존재하지 않는 경우
     */
    public AiResponseDto getAiRequest(UUID aiRequestId) {
        AiRequest aiRequest = aiRequestRepository
            .findByAiRequestIdAndDeletedAtIsNull(aiRequestId)
            .orElseThrow(
                () -> new AiRequestNotFoundException(aiRequestId)
            );

        return AiResponseDto.from(aiRequest);
    }

    /**
     * 검색조건과 페이지 정보를 이용하여 AI 요청 목록을 조회합니다.
     *
     * <p>주문 ID, 배송 ID, 처리 상태 중 전달된 조건만 적용하며,
     * 논리적으로 삭제된 요청은 조회하지 않습니다.</p>
     *
     * @param condition AI 요청 검색조건
     * @param pageable 페이지 번호, 크기 및 정렬 정보
     * @return 페이징된 AI 요청 목록
     */
    @Transactional(readOnly = true)
    public Page<AiResponseDto> searchAiRequests(
        AiSearchCondition condition,
        Pageable pageable
    ) {
        return aiRequestRepository.findAll(
            AiRequestSpecification.withCondition(condition),
            pageable
        ).map(AiResponseDto::from);
    }

    /**
     * 실패한 AI 요청을 Gemini에 다시 전달하여 재처리합니다.
     *
     * <p>FAILED 상태의 요청만 재처리할 수 있으며,
     * 성공하면 SUCCESS, 실패하면 다시 FAILED 상태로 저장합니다.</p>
     *
     * @param aiRequestId 재처리할 AI 요청 식별자
     * @return 재처리 결과
     */
    public AiResponseDto retryAiRequest(UUID aiRequestId) {
        AiRequest aiRequest = aiRequestRepository
            .findByAiRequestIdAndDeletedAtIsNull(aiRequestId)
            .orElseThrow(
                () -> new AiRequestNotFoundException(aiRequestId)
            );

        if (aiRequest.getStatus() != AiRequestStatus.FAILED) {
            throw new AiRequestRetryNotAllowedException(
                aiRequestId,
                aiRequest.getStatus()
            );
        }

        // 이전 실패 결과를 초기화하고 PENDING으로 변경합니다.
        aiRequest.prepareRetry();

        AiRequest pendingAiRequest =
            aiRequestRepository.save(aiRequest);

        long startedAt = System.nanoTime();

        try {
            DispatchDeadlineAiClient.AiExecutionResult executionResult =
                dispatchDeadlineAiClient.calculateDispatchDeadline(
                    pendingAiRequest.getPrompt()
                );

            pendingAiRequest.markSuccess(
                executionResult.rawResponse(),
                executionResult.calculationResult()
                    .dispatchDeadline(),
                executionResult.model(),
                executionResult.processingTimeMs()
            );

            AiRequest completedAiRequest =
                aiRequestRepository.save(pendingAiRequest);

            return AiResponseDto.from(completedAiRequest);

        } catch (GeminiProcessingException exception) {
            long processingTimeMs =
                calculateProcessingTime(startedAt);

            pendingAiRequest.markFailed(
                exception.getMessage(),
                dispatchDeadlineAiClient.getModel(),
                processingTimeMs
            );

            aiRequestRepository.save(pendingAiRequest);

            throw exception;
        }
    }

    /**
     * 지정된 기간의 AI 요청 처리 통계를 조회합니다.
     *
     * <p>날짜는 일 단위로 입력받으며 종료일의 데이터까지
     * 포함되도록 다음 날 0시를 조회 종료 시점으로 사용합니다.</p>
     *
     * @param startDate 조회 시작일
     * @param endDate 조회 종료일
     * @return AI 요청 처리 통계
     */
    @Transactional(readOnly = true)
    public AiStatisticsResponseDto getAiStatistics(
        LocalDate startDate,
        LocalDate endDate
    ) {
        validateStatisticsPeriod(startDate, endDate);

        LocalDateTime startDateTime =
            startDate == null
                ? MIN_STATISTICS_DATE_TIME
                : startDate.atStartOfDay();

        LocalDateTime endDateTime =
            endDate == null
                ? MAX_STATISTICS_DATE_TIME
                : endDate.plusDays(1).atStartOfDay();

        AiRequestStatisticsProjection statistics =
            aiRequestRepository.findStatistics(
                AiRequestStatus.SUCCESS,
                AiRequestStatus.FAILED,
                startDateTime,
                endDateTime
            );

        return new AiStatisticsResponseDto(
            statistics.getTotalCount(),
            statistics.getSuccessCount(),
            statistics.getFailedCount(),
            roundToTwoDecimalPlaces(
                statistics.getAverageProcessingTimeMs()
            )
        );
    }

    /**
     * 통계 조회 시작일과 종료일의 순서를 검증합니다.
     */
    private void validateStatisticsPeriod(
        LocalDate startDate,
        LocalDate endDate
    ) {
        if (
            startDate != null
                && endDate != null
                && startDate.isAfter(endDate)
        ) {
            throw new InvalidStatisticsPeriodException(
                startDate,
                endDate
            );
        }
    }

    /**
     * 평균 처리시간을 소수점 둘째 자리까지 반환합니다.
     */
    private double roundToTwoDecimalPlaces(Double value) {
        if (value == null) {
            return 0.0;
        }

        return Math.round(value * 100.0) / 100.0;
    }

    /**
     * AI 요청 이력을 논리 삭제합니다.
     *
     * @param aiRequestId 삭제할 AI 요청 식별자
     * @param deletedBy   삭제를 수행한 사용자 식별자
     */
    @Transactional
    public void deleteAiRequest(UUID aiRequestId, UUID deletedBy) {

        AiRequest aiRequest = aiRequestRepository
            .findByAiRequestIdAndDeletedAtIsNull(aiRequestId)
            .orElseThrow(() -> new AiRequestNotFoundException(aiRequestId));

        // 실제 행을 삭제하지 않고 deletedAt과 deletedBy를 기록합니다.
        aiRequest.softDelete(deletedBy);
    }
}
