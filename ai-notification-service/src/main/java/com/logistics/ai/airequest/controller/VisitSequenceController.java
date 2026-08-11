package com.logistics.ai.airequest.controller;

import com.logistics.ai.airequest.dto.requestdto.VisitSequenceRequestDto;
import com.logistics.ai.airequest.dto.responsedto.VisitSequenceResponseDto;
import com.logistics.ai.airequest.service.VisitSequenceService;
import com.logistics.ai.global.common.InternalServiceValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * delivery-service가 1차 계산한 방문 순서를 AI로 미세 조정하는 API를 제공하는 컨트롤러입니다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai-requests")
@Tag(
    name = "AI 방문 순서 다듬기",
    description = "배송담당자의 1차(최근접 이웃) 방문 순서를 AI로 미세 조정하는 API"
)
public class VisitSequenceController {

    private final VisitSequenceService visitSequenceService;
    private final InternalServiceValidator internalServiceValidator;

    /**
     * 1차 방문 순서를 Gemini에 전달하여 미세 조정합니다.
     *
     * @param requestDto 1차 순서가 매겨진 방문지 목록 및 허브 좌표
     * @return AI가 미세 조정한 최종 방문 순서
     */
    @PostMapping("/visit-sequence")
    @Operation(
        summary = "방문 순서 미세 조정",
        description = "delivery-service가 최근접 이웃 알고리즘으로 계산한 1차 방문 순서를 AI로 미세 조정합니다. delivery-service 내부 호출 전용입니다."
    )
    public ResponseEntity<VisitSequenceResponseDto> refineVisitSequence(
        @RequestHeader("X-Internal-Service") String serviceName,
        @RequestHeader("X-Internal-Service-Key") String serviceKey,
        @Valid @RequestBody VisitSequenceRequestDto requestDto
    ) {
        internalServiceValidator.validateInternalService(serviceName, serviceKey);
        VisitSequenceResponseDto response = visitSequenceService.refine(requestDto);

        return ResponseEntity.ok(response);
    }
}