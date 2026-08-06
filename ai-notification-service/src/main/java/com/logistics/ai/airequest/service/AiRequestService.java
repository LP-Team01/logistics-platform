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
import com.logistics.ai.global.exception.BusinessException;
import com.logistics.ai.global.exception.ErrorCode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI 요청의 생성, 조회, 재처리 및 삭제를 담당하는 서비스입니다.
 *
 * <p>프롬프트 생성, Gemini 호출, AI 요청 상태 변경을
 * 하나의 흐름으로 관리합니다.</p>
 */
@Service
@RequiredArgsConstructor
public class AiRequestService {

    private static final LocalDateTime MIN_STATISTICS_DATE_TIME =
        LocalDateTime.of(1, 1, 1, 0, 0);

    private static final LocalDateTime MAX_STATISTICS_DATE_TIME =
        LocalDateTime.of(9999, 12, 31, 0, 0);

    private final AiRequestRepository aiRequestRepository;
    private final AiPromptService aiPromptService;
    private final DispatchDeadlineAiClient dispatchDeadlineAiClient;

    /**
     * 배송 정보를 이용하여 최종 발송 시한을 계산합니다.
     *
     * <ol>
     *     <li>이벤트 중복 여부를 확인합니다.</li>
     *     <li>Gemini에 전달할 프롬프트를 생성합니다.</li>
     *     <li>AI 요청을 PENDING 상태로 먼저 저장합니다.</li>
     *     <li>AI 호출 결과에 따라 SUCCESS 또는 FAILED 상태로 변경합니다.</li>
     * </ol>
     *
     * @param requestDto AI 계산에 필요한 주문 및 배송 정보
     * @return AI 요청 처리 결과
     */
    public AiResponseDto createAiRequest(
        AiRequestDto requestDto
    ) {
        return aiRequestRepository
            .findByEventId(requestDto.eventId())
            .map(AiResponseDto::from)
            .orElseGet(
                () -> createNewAiRequest(requestDto)
            );
    }

    /**
     * 새로운 AI 요청을 생성하고 Gemini 계산을 수행합니다.
     */
    private AiResponseDto createNewAiRequest(
        AiRequestDto requestDto
    ) {
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

        AiRequest savedAiRequest =
            aiRequestRepository.save(aiRequest);

        long startedAt = System.nanoTime();

        try {
            DispatchDeadlineAiClient.AiExecutionResult executionResult =
                dispatchDeadlineAiClient.calculateDispatchDeadline(
                    prompt
                );

            savedAiRequest.markSuccess(
                executionResult.rawResponse(),
                executionResult.calculationResult()
                    .dispatchDeadline(),
                executionResult.model(),
                executionResult.processingTimeMs()
            );

            AiRequest completedAiRequest =
                aiRequestRepository.save(savedAiRequest);

            return AiResponseDto.from(completedAiRequest);

        } catch (BusinessException exception) {
            long processingTimeMs =
                calculateProcessingTime(startedAt);

            savedAiRequest.markFailed(
                exception.getMessage(),
                dispatchDeadlineAiClient.getModel(),
                processingTimeMs
            );

            aiRequestRepository.save(savedAiRequest);

            throw exception;
        }
    }

    /**
     * Kafka 이벤트의 기존 처리 상태에 따라
     * AI 요청을 생성하거나 실패 요청을 재처리합니다.
     *
     * <p>논리 삭제된 요청도 동일 eventId의 처리 이력으로 인정합니다.
     * 삭제된 이력이 존재하는 경우 같은 이벤트를 다시 실행하지 않습니다.</p>
     *
     * @param requestDto AI 계산 요청 정보
     * @return Slack 처리까지 이어갈 AI 응답.
     *         삭제된 이벤트 이력이면 Optional.empty()
     */
    public Optional<AiResponseDto> createOrRetryAiRequest(
        AiRequestDto requestDto
    ) {
        Optional<AiRequest> existingRequest =
            aiRequestRepository.findByEventId(
                requestDto.eventId()
            );

        if (existingRequest.isEmpty()) {
            return Optional.of(
                createNewAiRequest(requestDto)
            );
        }

        AiRequest aiRequest = existingRequest.get();

        /*
         * 삭제된 AI 요청도 과거에 소비된 Kafka 이벤트입니다.
         * 같은 eventId로 Gemini와 Slack을 다시 실행하지 않습니다.
         */
        if (aiRequest.isDeleted()) {
            return Optional.empty();
        }

        if (aiRequest.getStatus() == AiRequestStatus.SUCCESS) {
            return Optional.of(
                AiResponseDto.from(aiRequest)
            );
        }

        if (aiRequest.getStatus() == AiRequestStatus.FAILED) {
            return Optional.of(
                retryAiRequest(aiRequest)
            );
        }

        /*
         * PENDING은 다른 Consumer가 처리 중이거나
         * 이전 처리가 비정상적으로 중단된 상태일 수 있습니다.
         * 정상 종료하지 않고 Kafka 재처리가 가능하도록 예외를 전달합니다.
         */
        throw new BusinessException(
            ErrorCode.AI_REQUEST_RETRY_NOT_ALLOWED
        );
    }

    /**
     * AI 처리시간을 밀리초 단위로 계산합니다.
     *
     * @param startedAt 처리 시작 시각
     * @return 처리시간(ms)
     */
    private long calculateProcessingTime(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }

    /**
     * AI 요청 식별자로 삭제되지 않은 요청 이력을 조회합니다.
     *
     * @param aiRequestId AI 요청 식별자
     * @return AI 요청 처리 결과
     * @throws BusinessException 요청 이력이 존재하지 않는 경우
     */
    @Transactional(readOnly = true)
    public AiResponseDto getAiRequest(UUID aiRequestId) {
        AiRequest aiRequest = aiRequestRepository
            .findByAiRequestIdAndDeletedAtIsNull(aiRequestId)
            .orElseThrow(
                () -> new BusinessException(
                    ErrorCode.AI_REQUEST_NOT_FOUND
                )
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
     * 실패한 AI 요청을 다시 처리합니다.
     *
     * <p>FAILED 상태의 요청만 재처리할 수 있으며,
     * 성공하면 SUCCESS, 실패하면 다시 FAILED 상태로 저장합니다.</p>
     *
     * @param aiRequestId 재처리할 AI 요청 식별자
     * @return 재처리 결과
     * @throws BusinessException 요청이 없거나 재처리가 불가능한 경우
     */
    public AiResponseDto retryAiRequest(
        UUID aiRequestId
    ) {
        AiRequest aiRequest = aiRequestRepository
            .findByAiRequestIdAndDeletedAtIsNull(aiRequestId)
            .orElseThrow(
                () -> new BusinessException(
                    ErrorCode.AI_REQUEST_NOT_FOUND
                )
            );

        return retryAiRequest(aiRequest);
    }

    /**
     * 조회된 FAILED 상태의 AI 요청을 다시 처리합니다.
     *
     * @param aiRequest 재처리할 AI 요청
     * @return AI 재처리 결과
     */
    private AiResponseDto retryAiRequest(
        AiRequest aiRequest
    ) {
        /*
         * 재처리가 가능한 상태인지에 대한 검증은
         * AiRequest.prepareRetry()에서 수행합니다.
         */
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

        } catch (BusinessException exception) {
            long processingTimeMs =
                calculateProcessingTime(startedAt);

            pendingAiRequest.markFailed(
                exception.getMessage(),
                dispatchDeadlineAiClient.getModel(),
                processingTimeMs
            );

            // 실패 상태를 저장한 뒤 예외를 다시 전달합니다.
            aiRequestRepository.save(pendingAiRequest);

            throw exception;
        }
    }

    /**
     * 지정된 기간의 AI 요청 처리 통계를 조회합니다.
     *
     * <p>날짜는 일 단위로 입력받으며 종료일의 데이터까지 포함되도록
     * 다음 날 0시를 조회 종료 시점으로 사용합니다.</p>
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
     *
     * @param startDate 조회 시작일
     * @param endDate 조회 종료일
     * @throws BusinessException 시작일이 종료일보다 늦은 경우
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
            throw new BusinessException(
                ErrorCode.INVALID_STATISTICS_PERIOD
            );
        }
    }

    /**
     * 평균 처리시간을 소수점 둘째 자리까지 반환합니다.
     *
     * @param value 평균 처리시간
     * @return 소수점 둘째 자리까지 반올림한 값
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
     * @param deletedBy 삭제를 수행한 사용자 식별자
     * @throws BusinessException 요청 이력이 존재하지 않는 경우
     */
    @Transactional
    public void deleteAiRequest(
        UUID aiRequestId,
        UUID deletedBy
    ) {
        AiRequest aiRequest = aiRequestRepository
            .findByAiRequestIdAndDeletedAtIsNull(aiRequestId)
            .orElseThrow(
                () -> new BusinessException(
                    ErrorCode.AI_REQUEST_NOT_FOUND
                )
            );

        aiRequest.softDelete(deletedBy);
    }
}
