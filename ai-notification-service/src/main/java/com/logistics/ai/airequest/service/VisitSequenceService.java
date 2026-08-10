package com.logistics.ai.airequest.service;

import com.logistics.ai.airequest.dto.requestdto.VisitSequenceRequestDto;
import com.logistics.ai.airequest.dto.responsedto.VisitSequenceResponseDto;
import com.logistics.ai.global.exception.GeminiProcessingException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 1차(최근접 이웃) 방문 순서를 Gemini로 미세 조정하는 흐름을 담당합니다.
 */
@Service
@RequiredArgsConstructor
public class VisitSequenceService {

    private final VisitSequencePromptService visitSequencePromptService;
    private final VisitSequenceAiClient visitSequenceAiClient;

    /**
     * 1차 방문 순서를 AI로 미세 조정합니다.
     *
     * <p>Gemini에게도 입력받은 recordId 전체를 정확히 한 번씩만 포함하도록 프롬프트로
     * 지시하지만, 그 지시를 어긴 응답이 그대로 반영되지 않도록 서버에서도 동일하게 검증합니다.</p>
     *
     * @param request 1차 순서가 매겨진 방문지 목록 및 허브 좌표
     * @return AI가 미세 조정한 최종 방문 순서
     * @throws GeminiProcessingException Gemini 응답이 유효하지 않거나 recordId 집합이 요청과 다른 경우
     */
    public VisitSequenceResponseDto refine(VisitSequenceRequestDto request) {
        String prompt = visitSequencePromptService.createPrompt(request);

        VisitSequenceAiClient.AiExecutionResult executionResult =
            visitSequenceAiClient.refineVisitSequence(prompt);

        List<UUID> orderedRecordIds = executionResult.result().orderedRecordIds();

        validateSameRecordIds(request, orderedRecordIds);

        return new VisitSequenceResponseDto(request.agentId(), orderedRecordIds);
    }

    /**
     * AI 응답의 recordId 집합이 요청받은 방문지의 recordId 집합과 정확히 일치하는지 검증합니다.
     */
    private void validateSameRecordIds(
        VisitSequenceRequestDto request,
        List<UUID> orderedRecordIds
    ) {
        Set<UUID> requested = new HashSet<>();
        request.stops().forEach(stop -> requested.add(stop.recordId()));

        Set<UUID> responded = new HashSet<>(orderedRecordIds);

        if (!requested.equals(responded) || requested.size() != orderedRecordIds.size()) {
            throw new GeminiProcessingException(
                "Gemini가 반환한 방문 순서의 recordId 집합이 요청과 일치하지 않습니다."
            );
        }
    }
}