package com.logistics.ai.airequest.service;

import com.logistics.ai.airequest.dto.requestdto.AiRequestDto;
import com.logistics.ai.airequest.dto.responsedto.AiResponseDto;
import com.logistics.ai.airequest.entity.AiRequest;
import com.logistics.ai.airequest.repository.AiRequestRepository;
import com.logistics.ai.common.exception.GeminiProcessingException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
    private final GeminiClient geminiClient;

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
            GeminiClient.GeminiExecutionResult executionResult =
                geminiClient.calculateDispatchDeadline(prompt);

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
                geminiClient.getModel(),
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
}
